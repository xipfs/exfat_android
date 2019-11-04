package com.lenovo.exfat.core.fs.directory;


import android.util.Log;

import com.lenovo.exfat.core.fs.AllocationBitmap;
import com.lenovo.exfat.core.fs.DeviceAccess;
import com.lenovo.exfat.core.fs.ExFatFile;
import com.lenovo.exfat.core.fs.ExFatFileSystem;
import com.lenovo.exfat.core.fs.FatEntry;
import com.lenovo.exfat.core.fs.UpCaseTable;
import com.lenovo.exfat.core.util.ByteBufferUtils;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * exFAT 根目录解析器
 *
 * exFAT uses tree structure to describe relationship between files and directories.
 * The root of the directory tree is defined by directory located at RootDirectoryCluster.
 * Subdirectories are single-linked to there parents.
 * There is no special (.) and (..) directories pointing to itself and to parent like in FAT16/FAT32.
 *
 * @auther xiehui
 * @create 2019-10-11 上午10:18
 */
public class RootDirectoryParser {
    private static final String TAG ="exfat : "+RootDirectoryParser.class.getSimpleName();

    private ByteBuffer buffer;
    private long rootDirStartCluster;          // 根目录首簇号
    private long logicOffset;                  // 根目录偏移位置
    private int  clusterOffset;                // 簇中偏移
    private int conts = 0;
    private ExFatFile root;
    private FatEntry fatEntry;

    private ExFatFileEntry fileEntry;  // 文件目录项信息
    private ExFatFile  exFatFile;      // 文件
    private List<ExFatFile> exFatFileList;
    private long currentCluster;

    private ExFatFileSystem exFatFileSystem;
    public RootDirectoryParser(ExFatFileSystem exFatFileSystem) throws IOException{
        this.exFatFileSystem =exFatFileSystem;
        // 解析exFAT目录树状目录结构
        // 首先跳转到根目录首簇号, 目录项大小固定为32字节
        rootDirStartCluster = Constants.ROOT_DIRECTORY_CLUSTER;
        // 定位根目录磁盘偏移
        logicOffset = ExFatUtil.clusterToOffset(Constants.ROOT_DIRECTORY_CLUSTER);
        // 初始化根目录
        root = new ExFatFile("/",exFatFileSystem);
        root.setAbsolutePath("/");
        root.setDirectory(true);
        root.setRoot(true);
        root.setLength(0);
        root.setFileCluster(0);

        fatEntry = exFatFileSystem.fat.getFatEntryByCluster(Constants.ROOT_DIRECTORY_CLUSTER);
        currentCluster = Constants.ROOT_DIRECTORY_CLUSTER;
    }

    public ExFatFile getRoot(){
        return root;
    }
    /**
     * 返回根目录
     *
     * @return
     * @throws IOException
     */
    public List<ExFatFile> build() throws IOException {
        Log.i(TAG,"start build root dirs !");
        Log.i(TAG,"根目录首簇号："+Constants.ROOT_DIRECTORY_CLUSTER+" , 偏移："+Long.toHexString(ExFatUtil.clusterToOffset(Constants.ROOT_DIRECTORY_CLUSTER)));
        buffer = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        exFatFileList = new ArrayList<>();
        long currentOffset = logicOffset; // 当前偏移
        long oldOffset = logicOffset;  // 保存原来的偏移
        currentCluster = Constants.ROOT_DIRECTORY_CLUSTER;
        fatEntry = exFatFileSystem.fat.getFatEntryByCluster(Constants.ROOT_DIRECTORY_CLUSTER);
        clusterOffset = 0;
        while(true){
            // 如果处理完一个簇,需要判断簇是否连续,如果不连续需要跳到下一个簇进行处理
            if(currentOffset - oldOffset == ExFatUtil.getBytesPerCluster()){
                long nextCluster = fatEntry.getNextCluster();
                if(nextCluster == 0){
                    Log.i(TAG,"处理连续簇根目录信息");
                    currentCluster++;
                    oldOffset = logicOffset;
                    clusterOffset =0;
                }else{
                    Log.i(TAG,"处理非连续簇根目录信息 next_cluster："+Long.toHexString(nextCluster));
                    currentOffset = ExFatUtil.clusterToOffset(fatEntry.getNextCluster());
                    fatEntry = exFatFileSystem.fat.getFatEntryByCluster(fatEntry.getNextCluster());
                    oldOffset = currentOffset;
                    currentCluster = nextCluster;
                    clusterOffset =0;
                }
            }
            Log.i(TAG,"root current offset : "+ Long.toHexString(currentOffset));
            // 读取根目录项
            exFatFileSystem.da.read(buffer, currentOffset);
            buffer.flip();
            final int entryType = DeviceAccess.getUint8(buffer);
            if (entryType == Constants.LABEL) {
                Log.i(TAG,"start parse label ");
                parseLabel();
            } else if (entryType == Constants.BITMAP) {
                Log.i(TAG,"start parse bitmap ");
                if(exFatFileSystem.bitmap == null ){
                    parseBitmap();
                }
            } else if (entryType == Constants.UPCASE) {
                Log.i(TAG,"start parse upcase ");
                if(exFatFileSystem.upCaseTable == null){
                    parseUpcaseTable();
                }
            } else if (entryType == Constants.FILE) {
                Log.i(TAG,"start parse root file ");
                parseFile();
            } else if (entryType == Constants.EOD) {
                root.setEndCluster(currentCluster);
                root.setEndOffset(currentOffset);
                Log.i(TAG,"start parse end ");
                break;
            }else if (entryType == Constants.NO_LABEL) {
                Log.i(TAG,"start parse NO_LABEL ");
            }else if (entryType == Constants.GUID) {
                Log.i(TAG,"start parse GUID ");
            }else if (entryType == Constants.TexFATPadding) {
                Log.i(TAG,"start parse TexFATPadding ");
            }else if (entryType == Constants.AccessControlTable) {
                Log.i(TAG,"start parse AccessControlTable ");
            }else if (entryType == Constants.StreamExtension) {
                Log.i(TAG,"start parse StreamExtensionEntry ");
                parseStreamExtension(); // 属性2
            }else if (entryType == Constants.FileName) {
                Log.i(TAG,"start parse FileName ");
                parseFileName();       // 属性3
            }else if (entryType == Constants.FILE_DEL) {
                Log.i(TAG,"start parse file del ");
            }else if (entryType == Constants.StreamExtension_DEL) {
                Log.i(TAG,"start parse StreamExtension_DEL ");
            }else if (entryType == Constants.FileName_DEL) {
                Log.i(TAG,"start parse FileName_DEL ");
            }else {
                Log.i(TAG,"unknown entry type 0x" + Integer.toHexString(entryType));
            }
            // 下一个目录项
            currentOffset += Constants.DIR_ENTRY_SIZE;
            clusterOffset += Constants.DIR_ENTRY_SIZE;
            buffer.clear();
        }
        return exFatFileList;
    }

    /**
     * Volume Label Directory Entry
     *
     * 字节偏移   字节长度     描述
     *  0x00      1       EntryType
     *  0x01      1       CharacterCount  Length in Unicode characters (max 11)
     *  0x02      22      VolumeLabel
     *  0x18      8          保留
     */
    private void parseLabel() {
        final int len = DeviceAccess.getUint8(buffer);
        final StringBuilder labelBuilder = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            labelBuilder.append(DeviceAccess.getChar(buffer));
        }
        Constants.DISK_LABEL = labelBuilder.toString();
        Log.i(TAG,"label => "+labelBuilder.toString());
    }

    /**
     *  字节偏移    字节长度      内容及含义
     *  0x00         1         目录项类型
     *  0x01         1          保留
     *  0x02         18         保留
     *  0x14         4         起始簇号
     *  0x18         8         文件大小
     */
    private void parseBitmap() throws IOException{
        skip(19); /* unknown content */
        final long bitmapCluster = DeviceAccess.getUint32(buffer); // 起始簇号
        final long size = DeviceAccess.getUint64(buffer);         // 文件大小
        Log.i(TAG,"Bitmap Cluster : "+bitmapCluster+" , logicOffset : "+Long.toHexString(ExFatUtil.clusterToOffset(bitmapCluster)));
        AllocationBitmap bitmap = new AllocationBitmap(exFatFileSystem);
        exFatFileSystem.bitmap = bitmap;
        bitmap.build(bitmapCluster,size);
    }

    /**
     *字节偏移      字节大小      描述
     * 0x00           1       EntryType
     * 0x01           3       Reserved1
     * 0x04           4    TableChecksum
     * 0x08          12       Reserved2
     * 0x14           4       FirstCluster
     * 0x18           8        DataLength
     */

    private void parseUpcaseTable() {
        skip(3); /* unknown */
        final long checksum = DeviceAccess.getUint32(buffer);
        assert (checksum >= 0);
        skip(12); /* unknown */
        final long upcaseTableCluster = DeviceAccess.getUint32(buffer);
        final long size = DeviceAccess.getUint64(buffer);
        UpCaseTable upCaseTable = new UpCaseTable(exFatFileSystem);
        exFatFileSystem.upCaseTable = upCaseTable;
        upCaseTable.build(upcaseTableCluster,size);
    }

    /**
     * File Directory Entry
     * 属性 1
     * 字节偏移   字节大小       描述
     * 0x00        1        EntryType
     * 0x01        1        SecondaryCount  Must be from 2 to 18
     * 0x02        2        SetChecksum
     * 0x04        2        FileAttributes
     * 0x06        2        Reserved1
     * 0x08        4        CreateTimestamp
     * 0x0C        4      LastModifiedTimestamp
     * 0x10        4      LastAccessedTimestamp
     * 0x14        1      Create10msIncrement
     * 0x15        1      LastModified10msIncrement
     * 0x16        1      CreateTimezoneOffset
     * 0x17        1     LastModifiedTimezoneOffset
     * 0x18        1    LastAccessedTimezoneOffset
     * 0x19        7        Reserved2
     *
     *-------------------------------------------------------------
     * 属性2
     * 字节偏移   字节大小       描述
     * 0x00       1           目录项类型
     * 0x01       1           文件碎片标志
     * 0x02       1            保留
     * 0x03       1            文件名字符数N
     * 0x04       2            文件名 Hash 值
     * 0x06       2            保留
     * 0x08       2            文件大小1
     * 0x10       4             保留
     * 0x14       4             起始簇号
     * 0x18       8             文件大小2
     *--------------------------------------------------
     * 属性3
     * 字节偏移   字节大小          描述
     * 0x00         1           目录项类型
     * 0x01        1            保留
     * 0x02        2N           文件名
     */
    private void parseFile() throws IOException{

        fileEntry = new ExFatFileEntry();
        exFatFile = new ExFatFile(exFatFileSystem);
        exFatFile.setParent(root);
        exFatFile.setFirstEntry(fileEntry);
        exFatFile.setRoot(false);
        exFatFile.setFileMetadataCluster(currentCluster);  // 保存文件元信息所在的簇
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
        fileEntry.setOffset(clusterOffset);   // 记录簇中目录项偏移
        fileEntry.setConts(conts);
        fileEntry.setAttrib(attrib);
        fileEntry.setCheckSum(checkSum);
        fileEntry.setTimes(times);
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
        conts--;
        StreamExtensionEntry entry = new StreamExtensionEntry();
        entry.setOffset(clusterOffset);                   // 簇中偏移
        entry.setCluster(currentCluster);
        final int flag = DeviceAccess.getUint8(buffer);   // 碎片标志
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
        Log.i(TAG,"文件分配簇："+fileCluster+"，文件内容偏移地址： "+ Long.toHexString(ExFatUtil.clusterToOffset(fileCluster)));
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
        FileNameEntry entry = new FileNameEntry();
        entry.setOffset(clusterOffset);                 // 簇中偏移
        entry.setCluster(currentCluster);
        // 处理属性3, 属性3存在多个，需要根据 conts 多次处理
        /* read file name */
        skip(1); /* unknown */
        for (int i = 0; i < Constants.ENAME_MAX_LEN; i++) {
                fileEntry.addChar(DeviceAccess.getChar(buffer));
        }
        exFatFile.getThirdsEntry().add(entry);
        conts--;
        if(conts == 0 ){
            exFatFile.setAbsolutePath(root.getAbsolutePath()+exFatFile.getName());
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
