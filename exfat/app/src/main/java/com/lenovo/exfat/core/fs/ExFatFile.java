package com.lenovo.exfat.core.fs;


import android.util.Log;

import com.lenovo.exfat.core.AbstractUsbFile;
import com.lenovo.exfat.core.UsbFile;
import com.lenovo.exfat.core.fs.directory.EntryTimes;
import com.lenovo.exfat.core.fs.directory.ExFatFileEntry;
import com.lenovo.exfat.core.fs.directory.FileNameEntry;
import com.lenovo.exfat.core.fs.directory.StreamExtensionEntry;
import com.lenovo.exfat.core.util.ByteBufferUtils;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExFatFile extends AbstractUsbFile {
    private static final String TAG ="exfat : "+ExFatFile.class.getSimpleName();

    private ExFatFile parent;
    private boolean isDirectory;
    private boolean isRoot;
    private String name;
    private String absolutePath;
    private long length;


    private long fileMetadataCluster; // 文件元信息对应簇
    private long fileCluster;         // 文件内容对应簇

    private ExFatFileEntry firstEntry;                                      // 属性1
    private StreamExtensionEntry secondEntry;                               // 属性2
    private List<FileNameEntry> thirdsEntry = new ArrayList<>();           // 属性3

    private long endOffset;  // 目录项结尾偏移
    private long endCluster; // 目录项结尾簇

    public ByteBuffer firstBuffer;
    public ByteBuffer secondBuffer;
    public List<ByteBuffer> thirdBuffer = new ArrayList<>();

    private ExFatFileSystem exFatFileSystem;
    public ExFatFile(String name,ExFatFileSystem exFatFileSystem) {
        this.exFatFileSystem = exFatFileSystem;
        this.name = name;

    }

    public ExFatFile(ExFatFileSystem exFatFileSystem) {
       this.exFatFileSystem = exFatFileSystem;
    }

    @Override
    public String[] list() throws IOException {
        UsbFile[] files = listFiles();
        String[] strs = new String[files.length];
        for(int i=0 ; i < files.length;i++){
            strs[i] = files[i].getName();
        }
        return strs;
    }

    @Override
    public UsbFile[] listFiles() throws IOException {
        Log.i(TAG,"list file : "+name);
        List<ExFatFile> files = new ArrayList<>();
        if(isRoot){
            files = exFatFileSystem.rootParser.build();
        }else{
            files = exFatFileSystem.childParser.build(this);
        }
        UsbFile[] usbFiles = new UsbFile[files.size()];
        usbFiles = files.toArray(usbFiles);
        return  usbFiles;
    }

    @Override
    public void read(long offset, ByteBuffer destination) throws IOException {
        long clusterNum = offset/ Constants.BytesPerCluster; // 计算偏移是否超过一个簇
        if(clusterNum == 0){//就在当前簇
            exFatFileSystem.da.read(destination, ExFatUtil.clusterToOffset(fileCluster)+offset);
        }else{//需要查找当前簇
            FatEntry entry = exFatFileSystem.fat.getFatEntryByCluster(fileCluster);
            long newFileCluster = fileCluster;
            for(int i = 0 ; i < clusterNum ; i++){
                if(entry.getNextCluster() == FatEntry.UNDEFINED){
                    newFileCluster +=1;
                }else{
                    newFileCluster = entry.getNextCluster();
                }
                entry = exFatFileSystem.fat.getFatEntryByCluster(newFileCluster);
            }
            offset = offset % Constants.BytesPerCluster;
            exFatFileSystem.da.read(destination, ExFatUtil.clusterToOffset(newFileCluster)+offset);
        }

    }

    private FatEntry lastEntry;
    private long lastCluster;

    @Override
    public synchronized void write(long offset, ByteBuffer source) throws IOException {
        Log.i(TAG,"==============================Write file start=========================");
        try {
            long writeLength = offset + source.limit();
            // 占据多少个簇
            long clusterNum = offset / Constants.BytesPerCluster;
            long newFileCluster = fileCluster;
            boolean isContinue = true;
            int length = source.remaining();

            while(length >0) {
                if (clusterNum == 0) {//就在当前簇
                    long writeOffset = ExFatUtil.clusterToOffset(newFileCluster);
                    Log.i(TAG, "write cluster  :" + newFileCluster + ",offset :" + Long.toHexString(writeOffset));
                    lastCluster = fileCluster;
                    lastEntry = exFatFileSystem.fat.getFatEntryByCluster(fileCluster);
                    int size = (int) Math.min(Constants.BytesPerCluster, length);
                    source.limit(source.position() + size);
                    exFatFileSystem.da.write(source,writeOffset);
                    length -= size;
                    writeOffset+=size;
                    exFatFileSystem.bitmap.useCluster(newFileCluster);
                } else {
                    long emptyCluster = exFatFileSystem.bitmap.getNextFreeCluster(newFileCluster);
                    long writeOffset = ExFatUtil.clusterToOffset(emptyCluster);
                    Log.i(TAG, "empty cluster  :" + emptyCluster + ",offset :" + Long.toHexString(writeOffset));
                    int size = (int) Math.min(Constants.BytesPerCluster, length);
                    source.limit(source.position() + size);
                    exFatFileSystem.da.write(source,writeOffset);
                    length -= size;
                    writeOffset+=size;
                    exFatFileSystem.bitmap.useCluster(emptyCluster);
                    if (lastCluster == emptyCluster - 1) {
                    } else {//不连续需要记录新的簇
                        isContinue = false;
                        ByteBuffer fatBuffer = ByteBuffer.allocate(Constants.FAT_ENTRY_SIZE);
                        fatBuffer.order(ByteOrder.LITTLE_ENDIAN);
                        fatBuffer.putInt((int) lastCluster);
                        exFatFileSystem.da.write(fatBuffer, lastEntry.getOffset()); // 将fat项置为空
                    }

                    lastEntry = exFatFileSystem.fat.getFatEntryByCluster(emptyCluster);
                }
            }
            // 计算校验码
            if(!isContinue){//不连续
                secondBuffer.position(1);
                secondBuffer.put((byte)Constants.FLAG_FRAGMENTED);
            }
            secondBuffer.position(8);
            secondBuffer.putLong(writeLength);
            secondBuffer.position(24);
            secondBuffer.putLong(writeLength);
            secondBuffer.clear();

            exFatFileSystem.da.write(secondBuffer, ExFatUtil.clusterToOffset(secondEntry.getCluster()) + secondEntry.getOffset());
            int checkSum = ExFatUtil.startChecksum(firstBuffer);
            checkSum = ExFatUtil.addChecksum(checkSum, secondBuffer);
            for (ByteBuffer buffer : thirdBuffer) {
                checkSum = ExFatUtil.addChecksum(checkSum, buffer);
            }
            firstBuffer.position(2);
            firstBuffer.putShort((short) checkSum);
            firstBuffer.clear();
            exFatFileSystem.da.write(firstBuffer, ExFatUtil.clusterToOffset(firstEntry.getCluster()) + firstEntry.getOffset());
        }catch(Exception e){
            e.printStackTrace();
        }
        Log.i(TAG,"==============================Write file over=========================");
    }

    @Override
    public void flush() throws IOException {
        Log.i(TAG,"flush");
    }


    @Override
    public void close() throws IOException {
        Log.i(TAG,"close file");
    }

    @Override
    public UsbFile createDirectory(String createName) throws IOException {
        Log.i(TAG,"create directory "+createName);
        UsbFile[] files = listFiles();
        if(files !=null){
            for(int i = 0 ; i<files.length;i++){
                if(createName.equals(files[i].getName())){
                    throw  new IOException("已经存在相同文件名!");
                }
            }
        }
        if(isRoot){
            return createFileOrDir(createName,true,true);
        }else{
            return createFileOrDir(createName,true,false);
        }
    }

    @Override
    public UsbFile createFile(String createName) throws IOException {
        Log.i(TAG,"create file "+createName);
        UsbFile[] files = listFiles();
        if(files !=null){
            for(int i = 0 ; i<files.length;i++){
                if(createName.equals(files[i].getName())){
                    throw  new IOException("已经存在相同文件名!");
                }
            }
        }
        if(isRoot){
            return createFileOrDir(createName,false,true);
        }else{
            return createFileOrDir(createName,false,false);
        }
    }

    @Override
    public void moveTo(UsbFile destination) throws IOException {

    }

    @Override
    public void delete() throws IOException {
        if(isDirectory){
            UsbFile[] files = listFiles();
            if(files !=null && files.length>0){
                for(UsbFile file : files){
                    file.delete();
                }
            }
        }
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        // 属性1标志置为删除
        buffer.put((byte)Constants.FILE_DEL);
        buffer.flip();
        Log.i(TAG,"first offset : "+Long.toHexString(ExFatUtil.clusterToOffset(firstEntry.getCluster())+firstEntry.getOffset()));
        exFatFileSystem.da.write(buffer, ExFatUtil.clusterToOffset(firstEntry.getCluster())+firstEntry.getOffset());

        // 属性2标志置为删除
        buffer.clear();
        buffer.put((byte) Constants.StreamExtension_DEL);
        buffer.flip();
        Log.i(TAG,"second offset : "+Long.toHexString(ExFatUtil.clusterToOffset(secondEntry.getCluster())+secondEntry.getOffset()));
        exFatFileSystem.da.write(buffer,ExFatUtil.clusterToOffset(secondEntry.getCluster())+secondEntry.getOffset());

        // 属性3标志置为删除
        for(FileNameEntry nameEntry:thirdsEntry){
            buffer.clear();
            buffer.put((byte)Constants.FileName_DEL);
            buffer.flip();
            Log.i(TAG,"third offset : "+ Long.toHexString(ExFatUtil.clusterToOffset(nameEntry.getCluster())+nameEntry.getOffset()));
            exFatFileSystem.da.write(buffer,ExFatUtil.clusterToOffset(nameEntry.getCluster())+nameEntry.getOffset());
        }

        long clusterUsed = length / Constants.BytesPerCluster+1; // 文件占用簇的数量
        // 判断该文件是否有碎片
        if(secondEntry.getFlag() == Constants.FLAG_CONTIGUOUS){ //连续存放,释放连续占用的簇位图  fat表不用管
            long newFileCluster = fileCluster;
            for(int i = 0 ; i< clusterUsed;i++){
                exFatFileSystem.bitmap.freeCluster(newFileCluster);
                newFileCluster++;
            }
        }else{ //不是连续存放，需要清空fat表，释放簇位图
            FatEntry entry = exFatFileSystem.fat.getFatEntryByCluster(fileCluster);
            long newFileCluster = fileCluster;
            for(int i = 0 ; i < clusterUsed ; i++){
                if(entry.getNextCluster() == FatEntry.UNDEFINED){
                    exFatFileSystem.bitmap.freeCluster(newFileCluster);
                    newFileCluster++;
                }else{
                    exFatFileSystem.bitmap.freeCluster(newFileCluster);
                    newFileCluster = entry.getNextCluster();
                }
                //原来的 fat表需要设置为空
                ByteBuffer fatBuffer = ByteBuffer.allocate(Constants.FAT_ENTRY_SIZE);
                fatBuffer.order(ByteOrder.LITTLE_ENDIAN);
                exFatFileSystem.da.write(fatBuffer,entry.getOffset()); // 将fat项置为空
                entry = exFatFileSystem.fat.getFatEntryByCluster(newFileCluster);
            }
        }
    }

    @Override
    public boolean isRoot() {
        return isRoot;
    }

    @Override
    public String getAbsolutePath() {
        if(this.absolutePath==null){
            this.absolutePath = parent.getAbsolutePath()+"/"+getName();
        }
        return this.absolutePath;
    }


    private ExFatFile createFileOrDir(String createName,boolean isDirectory,boolean isRoot) throws IOException{
        Log.i(TAG,"filename :"+createName+" , isDir :"+isDirectory+" , isRoot :"+isRoot);
        long root_end_offset = exFatFileSystem.root.endOffset;
        long root_end_cluster = exFatFileSystem.root.endCluster;
        if(isRoot){
            root_end_offset = exFatFileSystem.root.endOffset;
            root_end_cluster = exFatFileSystem.root.endCluster;
        }else{
             root_end_offset = endOffset;
             root_end_cluster = endCluster;
        }

        Log.i(TAG,"root_end_offset : "+Long.toHexString(root_end_offset));
        int cons = createName.length()/Constants.ENAME_MAX_LEN + 2;

        ExFatFile file = new ExFatFile(createName,exFatFileSystem);
        file.setDirectory(isDirectory);
        file.setAbsolutePath(this.absolutePath+"/"+createName);
        file.setParent(this);
        file.setRoot(false);

        // 遍历根目录，找到空闲位置写入
        ByteBuffer buffer = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ExFatFileEntry firstEntry = new ExFatFileEntry();
        file.firstEntry = firstEntry;
        firstEntry.setName(createName);

        // 属性1 标志
        buffer.put((byte)Constants.FILE);

        // 目录项 数目
        buffer.put((byte)cons);
        firstEntry.setConts(cons);
        long checksum_offset = root_end_offset + 2; // 记录下校验码偏移
        // 校验和 先填空
        buffer.put((byte)0x0);
        buffer.put((byte)0x0);

        // 文件属性
        if(isDirectory){
            buffer.put(Constants.ATTRIB_DIR);
            firstEntry.setAttrib(Constants.ATTRIB_DIR);

        }else{
            buffer.put(Constants.ATTRIB_ARCH);
            firstEntry.setAttrib(Constants.ATTRIB_ARCH);
        }

        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);

        // 创建时间
        buffer.putInt(EntryTimes.UnixToExfat());
        // 修改时间
        buffer.putInt(EntryTimes.UnixToExfat());
        // 最后访问时间
        buffer.putInt(EntryTimes.UnixToExfat());
        firstEntry.setTimes(new EntryTimes(new Date(),new Date(),new Date()));

        // c/m centiseconds
        buffer.put((byte)0x0);
        buffer.put((byte)0x0);
        // 时区 上海
        buffer.put((byte)0xa0);
        buffer.put((byte)0xa0);
        buffer.put((byte)0xa0);
        // byte7 保留位
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);

        int startCheckSum = ExFatUtil.startChecksum(buffer);

        buffer.flip();
        Log.i(TAG,"写入属性1 ："+Long.toHexString(root_end_offset));
        exFatFileSystem.da.write(buffer,root_end_offset);
        firstEntry.setCluster(root_end_cluster);
        firstEntry.setOffset(root_end_offset%Constants.BytesPerCluster);

        root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;

        ByteBuffer copy = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        copy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBufferUtils.buffercopy(buffer,0,copy,0,Constants.DIR_ENTRY_SIZE);
        file.firstBuffer = copy;
        buffer.clear();
        if(root_end_offset%Constants.BytesPerCluster == 0){
            //这个簇写完了，需要写入下一个簇
            long freeCluster = exFatFileSystem.bitmap.getNextFreeCluster(root_end_cluster);
            root_end_offset = ExFatUtil.clusterToOffset(freeCluster);
            exFatFileSystem.bitmap.useCluster(freeCluster);
            if(root_end_cluster+1 == freeCluster){ // 连续就不管

            }else{ // 不连续
                FatEntry entry = exFatFileSystem.fat.getFatEntryByCluster(root_end_cluster);
                //原来的 fat表需要设置为空
                ByteBuffer fatBuffer = ByteBuffer.allocate(Constants.FAT_ENTRY_SIZE);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(freeCluster);
                buffer.flip();
                exFatFileSystem.da.write(fatBuffer,entry.getOffset()); // 将fat项置为空
                exFatFileSystem.bitmap.useCluster(freeCluster);
            }
            root_end_cluster = freeCluster;
            firstEntry.setCluster(root_end_cluster);
            firstEntry.setOffset(0);
        }


        StreamExtensionEntry secondEntry = new StreamExtensionEntry();
        file.secondEntry =secondEntry;

        // 标志位
        buffer.put((byte)Constants.StreamExtension);

        // 文件碎片标志 03H 连续存放 没有碎片 01H 有碎片
        buffer.put((byte)0x03);
        secondEntry.setFlag(0x03);
        // 保留
        buffer.put((byte)0);

        // 文件字符数
        buffer.put((byte)createName.length());
        secondEntry.setNameLen(createName.length());
        // 文件名 HASH
        buffer.putInt(ExFatUtil.hashName(exFatFileSystem.upCaseTable,createName));

        // 文件大小
        if(isDirectory){
            buffer.putLong(ExFatUtil.getBytesPerCluster()); // 分配一个簇
        }else{
            buffer.putLong(0L);                             // 新文件大小为0
        }
        secondEntry.setLength(0);
        // 保留
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);

        // 起始簇号

        long freeCluster = exFatFileSystem.bitmap.getNextFreeCluster();
        buffer.putInt((int)freeCluster);
        firstEntry.setFileCluster(freeCluster);
        firstEntry.setCluster(root_end_cluster);
        file.setFileCluster(freeCluster);
        Log.i(TAG,"文件分配簇："+freeCluster+"，文件内容偏移地址： "+ Long.toHexString(ExFatUtil.clusterToOffset(freeCluster)));

        // 文件大小
        buffer.putLong(0l); // 分配一个簇

        int addCheckSum = ExFatUtil.addChecksum(startCheckSum,buffer);
        buffer.flip();

        StringBuilder sb = new StringBuilder();
        byte[] data = buffer.array();
        for(byte b: data){
            sb.append(Integer.toHexString(b)+" ");
        }
        Log.i(TAG,"属性2 "+sb.toString());
        Log.i(TAG,"写入属性2 ："+Long.toHexString(root_end_offset)+" buffer "+buffer.position()+"  "+buffer.limit());
        exFatFileSystem.da.write(buffer,root_end_offset);
        secondEntry.setCluster(root_end_cluster);
        secondEntry.setOffset(root_end_offset%Constants.BytesPerCluster);

        root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
        ByteBuffer copy2 = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        copy2.order(ByteOrder.LITTLE_ENDIAN);
        ByteBufferUtils.buffercopy(buffer,0,copy2,0,Constants.DIR_ENTRY_SIZE);
        file.secondBuffer = copy2;
        buffer.clear();

        if(root_end_offset%Constants.BytesPerCluster == 0){
            //这个簇写完了，需要写入下一个簇
            freeCluster = exFatFileSystem.bitmap.getNextFreeCluster(root_end_cluster);
            root_end_offset = ExFatUtil.clusterToOffset(freeCluster);
            exFatFileSystem.bitmap.useCluster(freeCluster);
            if(root_end_cluster+1 == freeCluster){ // 连续就不管

            }else{ // 不连续
                FatEntry entry = exFatFileSystem.fat.getFatEntryByCluster(root_end_cluster);
                //原来的 fat表需要设置为空
                ByteBuffer fatBuffer = ByteBuffer.allocate(Constants.FAT_ENTRY_SIZE);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(freeCluster);
                buffer.flip();
                exFatFileSystem.da.write(fatBuffer,entry.getOffset()); // 将fat项置为空
                exFatFileSystem.bitmap.useCluster(freeCluster);
            }
            root_end_cluster = freeCluster;
            secondEntry.setCluster(root_end_cluster);
            secondEntry.setOffset(0);
        }

        // 属性3 如果文件名称太长,则存在多个属性3
        int count = createName.length()/Constants.ENAME_MAX_LEN +1;
        int index = 0;
        for(int i = 0  ; i < count ; i++){
            FileNameEntry thirdEntry = new FileNameEntry();
            file.thirdsEntry.add(thirdEntry);
            // 属性3 标志
            buffer.put((byte)Constants.FileName);
            // 保留
            buffer.put((byte)0);
            for(int j = 0 ; j<Constants.ENAME_MAX_LEN ; j++){
                if( index < createName.length()){
                    buffer.putShort((short)(createName.charAt(index) & 0xffff));
                }else{
                    buffer.putShort((short)0);
                }
                index++;
            }
            addCheckSum = ExFatUtil.addChecksum(addCheckSum,buffer);
            buffer.flip();
            Log.i(TAG,"写入属性3 ："+Long.toHexString(root_end_offset));
            exFatFileSystem.da.write(buffer,root_end_offset);
            thirdEntry.setCluster(root_end_cluster);
            thirdEntry.setOffset(root_end_offset%Constants.BytesPerCluster);

            root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
            ByteBuffer copy3 = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
            copy3.order(ByteOrder.LITTLE_ENDIAN);
            ByteBufferUtils.buffercopy(buffer,0,copy3,0,Constants.DIR_ENTRY_SIZE);
            file.thirdBuffer.add(copy3);
            buffer.clear();

            if(root_end_offset%Constants.BytesPerCluster == 0){
                //这个簇写完了，需要写入下一个簇
                freeCluster = exFatFileSystem.bitmap.getNextFreeCluster(root_end_cluster);
                root_end_offset = ExFatUtil.clusterToOffset(freeCluster);
                exFatFileSystem.bitmap.useCluster(freeCluster);
                if(root_end_cluster+1 == freeCluster){ // 连续就不管

                }else{ // 不连续
                    FatEntry entry = exFatFileSystem.fat.getFatEntryByCluster(root_end_cluster);
                    //原来的 fat表需要设置为空
                    ByteBuffer fatBuffer = ByteBuffer.allocate(Constants.FAT_ENTRY_SIZE);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    buffer.putLong(freeCluster);
                    buffer.flip();
                    exFatFileSystem.da.write(fatBuffer,entry.getOffset()); // 将fat项置为空
                    exFatFileSystem.bitmap.useCluster(freeCluster);
                }
                root_end_cluster = freeCluster;
                thirdEntry.setCluster(root_end_cluster);
                thirdEntry.setOffset(0);
            }

        }

        // 写入校验码
        ByteBuffer checkSumBuffer = ByteBuffer.allocate(2);
        checkSumBuffer.order(ByteOrder.LITTLE_ENDIAN);
        checkSumBuffer.putShort((short)addCheckSum);
        checkSumBuffer.flip();
        exFatFileSystem.da.write(checkSumBuffer,checksum_offset);
        copy.position(2);
        copy.putShort((short)addCheckSum);
        copy.clear();
        exFatFileSystem.bitmap.useCluster(freeCluster); // 将空闲簇置为已使用
        Log.i(TAG,"===================================create file  over ==============================");
        return file;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public long getFileMetadataCluster() {
        return fileMetadataCluster;
    }

    public void setFileMetadataCluster(long fileMetadataCluster) {
        this.fileMetadataCluster = fileMetadataCluster;
    }

    public long getFileCluster() {
        return fileCluster;
    }

    public void setFileCluster(long fileCluster) {
        this.fileCluster = fileCluster;
    }

    public ExFatFileEntry getFirstEntry() {
        return firstEntry;
    }

    public void setFirstEntry(ExFatFileEntry firstEntry) {
        this.firstEntry = firstEntry;
    }

    public StreamExtensionEntry getSecondEntry() {
        return secondEntry;
    }

    public void setSecondEntry(StreamExtensionEntry secondEntry) {
        this.secondEntry = secondEntry;
    }

    public List<FileNameEntry> getThirdsEntry() {
        return thirdsEntry;
    }

    public void setThirdsEntry(List<FileNameEntry> thirdsEntry) {
        this.thirdsEntry = thirdsEntry;
    }

    public long getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(long endOffset) {
        this.endOffset = endOffset;
    }

    public long getEndCluster() {
        return endCluster;
    }

    public void setEndCluster(long endCluster) {
        this.endCluster = endCluster;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public void setParent(ExFatFile parent) {
        this.parent = parent;
    }


    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public String getName() {
        if(name !=null){
            return name;
        }else{
            name = firstEntry.getName();
            return name;
        }
    }

    @Override
    public void setName(String newName) throws IOException {
        this.name = newName;
    }

    @Override
    public long createdAt() {
        return firstEntry.getTimes().getCreated().getTime();
    }

    @Override
    public long lastModified() {
        return firstEntry.getTimes().getModified().getTime();
    }

    @Override
    public long lastAccessed() {
        return firstEntry.getTimes().getModified().getTime();
    }

    @Override
    public UsbFile getParent() {
        return parent;
    }

    @Override
    public long getLength() {
        if(firstEntry != null){
            return firstEntry.getLength();
        }else{
            return 0;
        }

    }

    @Override
    public void setLength(long newLength) throws IOException {
        this.length = newLength;
    }


}
