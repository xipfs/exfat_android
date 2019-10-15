package com.lenovo.exfat.core.fs.directory;


import android.util.Log;

import com.lenovo.exfat.core.fs.DeviceAccess;
import com.lenovo.exfat.core.fs.ExFatFile;
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

    private static final int DIR_ENTRY_SIZE = 32;               // 目录项大小
    private static final int ENAME_MAX_LEN = 15;
    private static final int VALID = 0x80;
    private static final int CONTINUED = 0x40;
    private static final int IMPORTANCE_MASK = 0x20;

    //Directory Entry Type
    private static final int EOD =      0x00;
    private static final int BITMAP =   0x81;
    private static final int UPCASE =   0x82;
    private static final int LABEL =    0x83;
    private static final int FILE =     0x85;
    private static final int GUID =     0xA0;
    private static final int TexFATPadding      = 0xA1;
    private static final int AccessControlTable = 0xA2;
    private static final int StreamExtension    = 0xC0;
    private static final int FileName           = 0xC1;


    private static final int FILE_DEL = 0x05;
    private static final int StreamExtension_DEL = 0x40;
    private static final int FileName_DEL = 0x41;

    private static final int FILE_INFO = (0x00 | CONTINUED);
    private static final int FILE_NAME = (0x01 | CONTINUED);
    private static final int FLAG_FRAGMENTED = 1;
    private static final int FLAG_CONTIGUOUS = 3;

    //文件属性
    public static final int ATTRIB_RO = 0x01;		// 00000001 只读
    public static final int ATTRIB_HIDDEN = 0x02;	// 00000010 隐藏
    public static final int ATTRIB_SYSTEM = 0x04;	// 00000100 系统
    public static final int ATTRIB_VOLUME = 0x08;   // 00001000 卷簇
    public static final int ATTRIB_DIR = 0x10;      // 00010000 子目录
    public static final int ATTRIB_ARCH = 0x20;     // 00100000 存档

    private DeviceAccess da;

    private ByteBuffer buffer;

    private long offset; // 偏移位置

    private Fat fat;
    private FatEntry fatEntry;

    private ExFatFileEntry fileEntry;  // 文件目录项信息
    private ExFatFile  exFatFile;      // 文件

    public ChildDirectoryParser(DeviceAccess da, Fat fat) {
        this.da = da;
        this.fat = fat;
        buffer = ByteBuffer.allocate(DIR_ENTRY_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    public void build(ExFatFile exFatFile) throws IOException {
        fatEntry = fat.getFatEntryByCluster(exFatFile.getFileCluster());
        offset =  ExFatUtil.clusterToOffset(exFatFile.getFileCluster());
        long oldOffset = offset;  // 保存原来的偏移
        while(true){
            // 如果处理完一个簇,需要判断簇是否连续,如果不连续需要跳到下一个簇进行处理
            if(offset - oldOffset == ExFatUtil.getBytesPerCluster()){
                long nextCluster = fatEntry.getNextCluster();
                if(nextCluster == 0){
                    Log.i(TAG,"处理连续簇根目录信息");
                    oldOffset = offset;
                }else{
                    Log.i(TAG,"处理非连续簇根目录信息 next_cluster："+Long.toHexString(nextCluster));
                    offset = ExFatUtil.clusterToOffset(fatEntry.getNextCluster());
                    fatEntry = fat.getFatEntryByCluster(fatEntry.getNextCluster());
                    oldOffset = offset;
                }
            }
            // 读取根目录项
            buffer.clear();
            da.read(buffer,offset);
            buffer.flip();
            final int entryType = DeviceAccess.getUint8(buffer);
            Log.i(TAG,"Entry type :"+Integer.toHexString(entryType));
            if (entryType == LABEL) {
            } else if (entryType == BITMAP) {
            } else if (entryType == UPCASE) {
            } else if (entryType == FILE) {
                Log.i(TAG,"parse child file ");
                parseFile(exFatFile);
            }else if (entryType == StreamExtension) {
                parseStreamExtension(); // 属性2
            }else if (entryType == FileName) {
                parseFileName();       // 属性3
            }else if (entryType == EOD) {
                break;
            }else if (entryType == FILE_DEL) {
                Log.i(TAG,"start parse file del ");
            }else if (entryType == StreamExtension_DEL) {
                Log.i(TAG,"start parse StreamExtension_DEL ");
            }else if (entryType == FileName_DEL) {
                Log.i(TAG,"start parse FileName_DEL ");
            }else if (entryType == GUID) {
            }else if (entryType == TexFATPadding) {
            }else if (entryType == AccessControlTable) {
            }else {
                Log.i(TAG,"unknown entry type 0x" + Integer.toHexString(entryType));
            }
            // 下一个目录项
            offset += DIR_ENTRY_SIZE;
            buffer.clear();
        }
        exFatFile.updateCache();
        for(ExFatFile child:exFatFile.getChildren()){
            if(child.isDirectory()){
                build(child);
            }else{
                Log.i(TAG,child.getName()+" , cluster :"+child.getFileCluster());
            }
        }

    }
    private void parseFile(ExFatFile file) throws IOException{

        fileEntry = new ExFatFileEntry();
        exFatFile = new ExFatFile();
        exFatFile.setEntry(fileEntry);
        exFatFile.setRoot(false);
        file.addFile(exFatFile);

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

        fileEntry.setConts(conts);
        fileEntry.setAttrib(attrib);
        fileEntry.setCheckSum(checkSum);
        fileEntry.setTimes(times);

    }

    /**
     * 处理属性2
     */
    public void parseStreamExtension(){
        final int flag = DeviceAccess.getUint8(buffer);  // 碎片标志
        skip(1); /* unknown */
        int nameLen = DeviceAccess.getUint8(buffer);
        final int nameHash = DeviceAccess.getUint16(buffer);
        skip(2); /* unknown */
        final long realSize = DeviceAccess.getUint64(buffer);
        skip(4); /* unknown */
        final long fileCluster = DeviceAccess.getUint32(buffer);
        final long size = DeviceAccess.getUint64(buffer);
        fileEntry.setFlag(flag);
        fileEntry.setNameLen(nameLen);
        fileEntry.setNameHash(nameHash);
        fileEntry.setLength(realSize);
        fileEntry.setFileCluster(fileCluster);
        exFatFile.setFileCluster(fileCluster);

    }

    /**
     * 处理属性3
     */
    public void parseFileName(){
        // 处理属性3, 属性3存在多个，需要根据 conts 多次处理
        /* read file name */
        skip(1); /* unknown */
        for (int i = 0; i < ENAME_MAX_LEN; i++) {
            fileEntry.addChar(DeviceAccess.getChar(buffer));
        }
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
