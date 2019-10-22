package com.lenovo.exfat.core.fs.directory;


import android.util.Log;

import com.lenovo.exfat.core.fs.DeviceAccess;
import com.lenovo.exfat.core.fs.ExFatFile;
import com.lenovo.exfat.core.fs.ExFatFileSystem;
import com.lenovo.exfat.core.fs.Fat;
import com.lenovo.exfat.core.fs.FatEntry;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @auther xiehui
 * @create 2019-10-14 下午5:20
 */
public class ChildDirectoryParser {
    private static final String TAG = ChildDirectoryParser.class.getSimpleName();

    private ByteBuffer buffer;

    private long offset;         // 磁盘偏移位置
    private int clusterOffset; //  簇中偏移位置

    private FatEntry fatEntry;

    private ExFatFileEntry fileEntry;  // 文件目录项信息
    private ExFatFile  exFatFile;      // 文件
    private long end_cluster;


    public ChildDirectoryParser() {
        buffer = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    public void build(ExFatFile root) throws IOException {
        fatEntry = Fat.getFatEntryByCluster(root.getFileCluster());
        offset =  ExFatUtil.clusterToOffset(root.getFileCluster());
        clusterOffset = 0;
        end_cluster = root.getFileCluster();
        long oldOffset = offset;  // 保存原来的偏移
        while(true){
            // 如果处理完一个簇,需要判断簇是否连续,如果不连续需要跳到下一个簇进行处理
            if(offset - oldOffset == ExFatUtil.getBytesPerCluster()){
                long nextCluster = fatEntry.getNextCluster();
                if(nextCluster == 0){
                    Log.i(TAG,"处理连续簇根目录信息");
                    end_cluster++;
                    oldOffset = offset;
                    clusterOffset=0;
                }else{
                    Log.i(TAG,"处理非连续簇根目录信息 next_cluster："+Long.toHexString(nextCluster));
                    offset = ExFatUtil.clusterToOffset(fatEntry.getNextCluster());
                    fatEntry = Fat.getFatEntryByCluster(fatEntry.getNextCluster());
                    oldOffset = offset;
                    end_cluster = nextCluster;
                    clusterOffset=0;
                }
            }
            // 读取根目录项
            buffer.clear();
            ExFatFileSystem.da.read(buffer,offset);
            buffer.flip();
            final int entryType = DeviceAccess.getUint8(buffer);
            if (entryType == Constants.LABEL) {
            } else if (entryType == Constants.BITMAP) {
            } else if (entryType == Constants.UPCASE) {
            } else if (entryType == Constants.FILE) {
                Log.i(TAG,"parse child file ");
                parseFile(root);
            }else if (entryType == Constants.StreamExtension) {
                parseStreamExtension(); // 属性2
            }else if (entryType == Constants.FileName) {
                parseFileName();       // 属性3
            }else if (entryType == Constants.EOD) {
                root.setEndCluster(end_cluster);
                root.setEndOffset(offset);
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
            offset += Constants.DIR_ENTRY_SIZE;
            clusterOffset +=Constants.DIR_ENTRY_SIZE;;
            buffer.clear();
        }
        root.updateCache();
    }

    private void parseFile(ExFatFile root) throws IOException{

        fileEntry = new ExFatFileEntry();
        exFatFile = new ExFatFile();
        exFatFile.setFirstEntry(fileEntry);
        exFatFile.setRoot(false);
        exFatFile.setFileInfoCluster(end_cluster);  // 设置目录项所在的簇

        root.addFile(exFatFile);

        // 处理属性1
        int conts = DeviceAccess.getUint8(buffer);          // conts 如果为2，表明后面还有2个目录项，分别是属性2 和 属性3
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
    }

    /**
     * 处理属性2
     */
    public void parseStreamExtension(){

        StreamExtensionEntry entry = new StreamExtensionEntry();
        entry.setCluster(end_cluster);
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

    }

    /**
     * 处理属性3
     */
    public void parseFileName(){
        FileNameEntry entry = new FileNameEntry();
        entry.setOffset(clusterOffset);
        entry.setCluster(end_cluster);
        // 处理属性3, 属性3存在多个，需要根据 conts 多次处理
        /* read file name */
        skip(1); /* unknown */
        for (int i = 0; i < Constants.ENAME_MAX_LEN; i++) {
            fileEntry.addChar(DeviceAccess.getChar(buffer));
        }
        exFatFile.getThirdsEntry().add(entry);
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
