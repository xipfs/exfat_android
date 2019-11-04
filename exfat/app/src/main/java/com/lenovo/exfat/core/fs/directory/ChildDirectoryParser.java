package com.lenovo.exfat.core.fs.directory;


import android.util.Log;


import com.lenovo.exfat.core.fs.DeviceAccess;
import com.lenovo.exfat.core.fs.ExFatFile;
import com.lenovo.exfat.core.fs.ExFatFileSystem;
import com.lenovo.exfat.core.fs.FatEntry;
import com.lenovo.exfat.core.util.ByteBufferUtils;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * @auther xiehui
 * @create 2019-10-14 下午5:20
 */
public class ChildDirectoryParser {
    private static final String TAG = "exfat : "+ChildDirectoryParser.class.getSimpleName();

    private ByteBuffer buffer;

    private long logicOffset;         // 磁盘中偏移位置
    private int  clusterOffset;       // 簇中偏移位置

    private FatEntry fatEntry;

    private ExFatFileEntry fileEntry;  // 文件目录项信息
    private ExFatFile exFatFile;      // 文件
    private long currentCluster;
    private int conts;

    private List<ExFatFile> exFatFileList;
    private ExFatFile parent;

    private ExFatFileSystem exFatFileSystem;
    public ChildDirectoryParser(ExFatFileSystem exFatFileSystem) {
        this.exFatFileSystem = exFatFileSystem;
    }
    public List<ExFatFile> build(ExFatFile parent) throws IOException {
        Log.i(TAG,"start build child dir !");
        buffer = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        exFatFileList = new ArrayList<>();
        this.parent = parent;
        fatEntry = exFatFileSystem.fat.getFatEntryByCluster(parent.getFileCluster());
        logicOffset =  ExFatUtil.clusterToOffset(parent.getFileCluster());
        Log.i(TAG,"child logic offset : "+ Long.toHexString(logicOffset));
        clusterOffset = 0;
        currentCluster = parent.getFileCluster();       // 子目录对应的簇 存放的就是子目录下的元数据
        long currentOffset = logicOffset; // 当前偏移
        long oldOffset = logicOffset;                   // 保存原来的偏移
        while(true){
            // 如果处理完一个簇,需要判断簇是否连续,如果不连续需要跳到下一个簇进行处理
            if(currentOffset - oldOffset == ExFatUtil.getBytesPerCluster()){
                long nextCluster = fatEntry.getNextCluster();
                if(nextCluster == 0){
                    Log.i(TAG,"处理连续簇根目录信息");
                    currentCluster++;
                    oldOffset = currentOffset;
                    clusterOffset=0;
                }else{
                    Log.i(TAG,"处理非连续簇根目录信息 next_cluster："+Long.toHexString(nextCluster));
                    currentOffset = ExFatUtil.clusterToOffset(fatEntry.getNextCluster());
                    fatEntry = exFatFileSystem.fat.getFatEntryByCluster(fatEntry.getNextCluster());
                    oldOffset = currentOffset;
                    currentCluster = nextCluster;
                    clusterOffset=0;
                }
            }
            // 读取根目录项
            buffer.clear();
            exFatFileSystem.da.read(buffer, currentOffset);
            buffer.flip();
            final int entryType = DeviceAccess.getUint8(buffer);
            if (entryType == Constants.LABEL) {
            } else if (entryType == Constants.BITMAP) {
            } else if (entryType == Constants.UPCASE) {
            } else if (entryType == Constants.FILE) {
                Log.i(TAG,"parse child file ");
                parseFile();
            }else if (entryType == Constants.StreamExtension) {
                parseStreamExtension(); // 属性2
            }else if (entryType == Constants.FileName) {
                parseFileName();       // 属性3
            }else if (entryType == Constants.EOD) {
                parent.setEndCluster(currentCluster);
                parent.setEndOffset(currentOffset);
                break;
            }else if (entryType == Constants.FILE_DEL) {
                Log.i(TAG,"start parse file del ");
            }else if (entryType == Constants.StreamExtension_DEL) {
                Log.i(TAG,"start parse StreamExtension_DEL ");
            }else if (entryType == Constants.FileName_DEL) {
                Log.i(TAG,"start parse FileName_DEL ");
            }else if (entryType == Constants.GUID) {
                Log.i(TAG,"start parse GUID ");
            }else if (entryType == Constants.NO_LABEL) {
                Log.i(TAG,"start parse NO_LABEL ");
            }else if (entryType == Constants.TexFATPadding) {
                Log.i(TAG,"start parse TexFATPadding ");
            }else if (entryType == Constants.AccessControlTable) {
                Log.i(TAG,"start parse AccessControlTable ");
            }else {
                Log.i(TAG,"unknown entry type 0x" + Integer.toHexString(entryType));
            }
            // 下一个目录项
            currentOffset   += Constants.DIR_ENTRY_SIZE;
            clusterOffset += Constants.DIR_ENTRY_SIZE;;
            buffer.clear();
        }
        return exFatFileList;
    }

    private void parseFile() throws IOException{

        fileEntry = new ExFatFileEntry();
        exFatFile = new ExFatFile(exFatFileSystem);

        exFatFile.setParent(parent);
        exFatFile.setFirstEntry(fileEntry);
        exFatFile.setRoot(false);
        exFatFile.setFileMetadataCluster(currentCluster);  // 设置目录项所在的簇

        exFatFileList.add(exFatFile);

        // 处理属性1
        conts = DeviceAccess.getUint8(buffer);          // conts 如果为2，表明后面还有2个目录项，分别是属性2 和 属性3
        int checkSum = DeviceAccess.getUint16(buffer);
        final int attrib = DeviceAccess.getUint16(buffer);
        if(attrib == 0x20){
            exFatFile.setDirectory(false);
        }else{
            exFatFile.setDirectory(true);
        }
        skip(2); /* unknown */
        EntryTimes times = EntryTimes.read(buffer);
        skip(7); /* unknown */
        Log.i(TAG,"parse check sum :"+Integer.toHexString(checkSum));
        fileEntry.setConts(conts);
        fileEntry.setAttrib(attrib);
        fileEntry.setCheckSum(checkSum);
        fileEntry.setTimes(times);
        fileEntry.setOffset(clusterOffset); // 簇中的偏移
        fileEntry.setCluster(currentCluster);

        ByteBuffer copy = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        copy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBufferUtils.buffercopy(buffer,0,copy,0,Constants.DIR_ENTRY_SIZE);
        exFatFile.firstBuffer = copy;
    }

    /**
     * 处理属性2
     */
    public void parseStreamExtension() throws IOException{
        conts --;
        StreamExtensionEntry entry = new StreamExtensionEntry();
        entry.setCluster(currentCluster);
        entry.setOffset(clusterOffset);

        final int flag = DeviceAccess.getUint8(buffer);  // 碎片标志
        skip(1); /* unknown */
        int nameLen = DeviceAccess.getUint8(buffer);
        final int nameHash = DeviceAccess.getUint16(buffer);
        skip(2); /* unknown */
        Log.i(TAG,"position : "+buffer.position());
        final long realSize = DeviceAccess.getUint64(buffer);
        skip(4); /* unknown */
        final long fileCluster = DeviceAccess.getUint32(buffer);
        Log.i(TAG,"position : "+buffer.position());
        final long size = DeviceAccess.getUint64(buffer);
        fileEntry.setFlag(flag);
        fileEntry.setNameLen(nameLen);
        fileEntry.setNameHash(nameHash);
        fileEntry.setLength(realSize);
        fileEntry.setFileCluster(fileCluster);

        exFatFile.setFileCluster(fileCluster);
        exFatFile.setLength(realSize);
        exFatFile.setSecondEntry(entry);

        ByteBuffer copy = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        copy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBufferUtils.buffercopy(buffer,0,copy,0,Constants.DIR_ENTRY_SIZE);
        exFatFile.secondBuffer = copy;

    }

    /**
     * 处理属性3
     */
    public void parseFileName(){
        conts --;
        FileNameEntry entry = new FileNameEntry();
        entry.setOffset(clusterOffset);
        entry.setCluster(currentCluster);
        // 处理属性3, 属性3存在多个，需要根据 conts 多次处理
        /* read file name */
        skip(1); /* unknown */
        for (int i = 0; i < Constants.ENAME_MAX_LEN; i++) {
            fileEntry.addChar(DeviceAccess.getChar(buffer));
        }
        exFatFile.getThirdsEntry().add(entry);
        if(conts == 0 ){
            exFatFile.setAbsolutePath(parent.getAbsolutePath()+"/"+exFatFile.getName());
        }
        ByteBuffer copy = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        copy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBufferUtils.buffercopy(buffer,0,copy,0,Constants.DIR_ENTRY_SIZE);
        exFatFile.thirdBuffer.add(copy);
    }
    /**
     * 跳过输入字节
     *
     * @param bytes
     */
    private void skip(int bytes) {
        buffer.position(buffer.position() + bytes);
    }
}
