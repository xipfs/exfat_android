package com.lenovo.exfat.core.fs;


import android.util.Log;

import com.lenovo.exfat.core.fs.directory.ChildDirectoryParser;
import com.lenovo.exfat.core.fs.directory.EntryTimes;
import com.lenovo.exfat.core.fs.directory.ExFatFileEntry;
import com.lenovo.exfat.core.fs.directory.FileNameEntry;
import com.lenovo.exfat.core.fs.directory.StreamExtensionEntry;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatCache;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * exFAT 文件
 *
 * @auther xiehui
 * @create 2019-10-11 下午4:57
 */
public class ExFatFile{
    private static final String TAG =  ExFatFile.class.getSimpleName();

    private boolean isDirectory;
    private boolean isRoot;
    private String name;
    private String absolutePath;
    private long length;

    private long fileInfoCluster; // 文件元信息对应簇

    private long fileCluster;     // 文件内容对应簇

    private ExFatFileEntry firstEntry;
    private StreamExtensionEntry secondEntry;
    private List<FileNameEntry>  thirdsEntry = new ArrayList<>();

    private Map<String,ExFatFile> cache = new HashMap<>();  // 缓存子文件
    private List<ExFatFile> children = new ArrayList<>();

    private long endOffset;  // 目录项结尾偏移
    private long endCluster; // 目录项结尾簇

    public ExFatFile(){

    }
    public ExFatFile(String absolutePath){
        this.absolutePath = absolutePath;
        String[] paths = absolutePath.split("/");
        String name ="";
        if(paths.length == 0){
            name = "/";
        }else if(paths.length > 1){
            name = paths[paths.length-1];
        }
        this.name =name;
    }

    public boolean exists() throws IOException{
        String[] paths = absolutePath.split("/");
        if(paths.length < 2){
            return true;
        }else if(paths.length == 2){
            return ExFatFileSystem.root.cache.get(absolutePath) != null;
        }else{
            boolean flag = false;
            ExFatFile temp = ExFatFileSystem.root.cache.get(paths[1]);
            for(int i = 2; i<paths.length ; i++){
                flag = false;
                List<ExFatFile>  files = temp.listFiles();
                for(ExFatFile file : files){
                    if(file.getName().equals(paths[i])){
                        temp = file;
                        flag = true;
                        break;
                    }
                }
                if(flag){
                    continue;
                }else{
                    return flag;
                }
            }
            return flag;
        }
    }

    public List<ExFatFile> listFiles() throws IOException{
        if(!isDirectory){
            return null;
        }
        if(isRoot){
            return children;
        }
        ChildDirectoryParser childParser = new ChildDirectoryParser();
        childParser.build(this);
        return children;
    }

    public boolean createNewFile() throws IOException{
        if(exists()){
           return false;
        }
        String[] paths = absolutePath.split("/");
        if(paths.length == 2){
            return createRootFile();
        }else if(paths.length > 2){
            return createChildFile();
        }
        throw  new IOException("错误的文件路径 !");
    }


    public boolean mkdir() throws IOException{
        if(exists()){
            return false;
        }
        String[] paths = absolutePath.split("/");
        if(paths.length == 2){
            return mkRootDir();
        }else if(paths.length > 2){
            return mkChildDir();
        }
        throw  new IOException("错误的文件路径 !");
    }

    /**
     * 创建根目录
     * @return
     */
    private boolean mkRootDir() throws IOException{
        return createRootFileOrDir(true);
    }

    /**
     * 创建根文件
     * @return
     */
    private boolean createRootFile() throws IOException{
        return createRootFileOrDir(false);
    }

    private boolean createRootFileOrDir(boolean isDirectory) throws IOException{
        String[] paths  = absolutePath.split("/");
        String fileName = absolutePath.substring(absolutePath.lastIndexOf("/")+1, absolutePath.length());
        long root_end_offset = ExFatFileSystem.root.endOffset;
        Log.i(TAG,"filename :"+fileName+" , endOffset : "+ Long.toHexString(root_end_offset)+" , cluster :"+ExFatFileSystem.root.endCluster);

        int cons = fileName.length()/Constants.ENAME_MAX_LEN + 2;
        ExFatFile file = new ExFatFile(absolutePath);
        file.setName(fileName);

        // 遍历根目录，找到空闲位置写入
        ByteBuffer buffer = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 属性1 标志
        buffer.put((byte)Constants.FILE);

        // 目录项 数目
        buffer.put((byte)cons);

        long checksum_offset = root_end_offset + 2; // 记录下校验码偏移

        // 校验和 先填空
        buffer.put((byte)0x0);
        buffer.put((byte)0x0);

        // 文件属性
        if(isDirectory){
            buffer.put(Constants.ATTRIB_DIR);
        }else{
            buffer.put(Constants.ATTRIB_ARCH);
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
        ExFatFileSystem.da.write(buffer,root_end_offset);
        root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
        buffer.clear();

        // 标志位
        buffer.put((byte)Constants.StreamExtension);

        // 文件碎片标志 03H 连续存放 没有碎片 01H 有碎片
        buffer.put((byte)0x03);

        // 保留
        buffer.put((byte)0);

        // 文件字符数
        buffer.put((byte)fileName.length());

        // 文件名 HASH
        buffer.putInt(ExFatUtil.hashName(fileName));

        // 文件大小
        if(isDirectory){
            buffer.putLong(ExFatUtil.getBytesPerCluster()); // 分配一个簇
        }else{
            buffer.putLong(0L);                             // 新文件大小为0
        }

        // 保留
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);

        // 起始簇号

        long freeCluster = AllocationBitmap.getNextFreeCluster();
        buffer.putInt((int)freeCluster);

        // 文件大小
        buffer.putLong(ExFatUtil.getBytesPerCluster()); // 分配一个簇

        int addCheckSum = ExFatUtil.addChecksum(startCheckSum,buffer);
        buffer.flip();
        ExFatFileSystem.da.write(buffer,root_end_offset);
        root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
        buffer.clear();

        // 属性3 如果文件名称太长,则存在多个属性3
        int count = fileName.length()/Constants.ENAME_MAX_LEN +1;
        int index = 0;
        for(int i = 0  ; i < count ; i++){
            // 属性3 标志
            buffer.put((byte)Constants.FileName);
            // 保留
            buffer.put((byte)0);
            for(int j = 0 ; j<Constants.ENAME_MAX_LEN ; j++){
                if( index < fileName.length()){
                    buffer.putShort((short)(fileName.charAt(index) & 0xffff));
                }else{
                    buffer.putShort((short)0);
                }
                index++;

            }
            addCheckSum = ExFatUtil.addChecksum(addCheckSum,buffer);
            buffer.flip();
            ExFatFileSystem.da.write(buffer,root_end_offset);
            root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
            buffer.clear();
        }


        // 写入校验码
        ByteBuffer checkSumBuffer = ByteBuffer.allocate(2);
        checkSumBuffer.order(ByteOrder.LITTLE_ENDIAN);
        checkSumBuffer.putShort((short)addCheckSum);
        checkSumBuffer.flip();
        ExFatFileSystem.da.write(checkSumBuffer,checksum_offset);

        AllocationBitmap.useCluster(freeCluster); // 将空闲簇置为已使用
        ExFatFileSystem.da.flush(); // 最后刷新下文件系统

        ExFatFileSystem.root.endOffset = root_end_offset;
        ExFatFileSystem.root.addFile(file);
        ExFatFileSystem.root.addCache(file.getName(),file);

        return true;
    }

    /**
     * 创建子目录
     * @return
     */
    private boolean mkChildDir() throws IOException{
        return createChildFileOrDir(true);
    }

    public ExFatFile findFile(String absolutePath) throws IOException{
        String[] paths = absolutePath.split("/");
        if(paths.length ==0 ){ //
            return ExFatFileSystem.root;
        }if(paths.length ==2 ){ //
            return ExFatFileSystem.root.getCache(paths[1]);
        }else {
            boolean flag = false;
            ExFatFile temp = ExFatFileSystem.root.getCache(paths[1]);
            for(int i = 2; i<paths.length ; i++){
                flag = false;
                Log.i(TAG,"path => "+ paths[i]);
                List<ExFatFile>  files = temp.listFiles();
                for(ExFatFile file : files){
                    if(file.getName().equals(paths[i])){
                        Log.i(TAG,"file => "+ paths[i]+" checkSum =>"+Integer.toHexString(file.getFirstEntry().getCheckSum()));
                        temp = file;
                        flag = true;
                        break;
                    }
                }
                if(flag){
                    continue;
                }else{
                    return  null;
                }
            }
            if(flag){
                return temp;
            }else{
                return null;
            }
        }
    }

    /**
     * 创建子文件
     * @return
     */
    private boolean createChildFile() throws IOException{
        return createChildFileOrDir(false);
    }

    private boolean createChildFileOrDir(boolean isDirectory) throws IOException{
        String[] paths = absolutePath.split("/");
        if(exists()){
            return false;
        }
        ExFatFile parentFile = findFile(absolutePath.substring(0,absolutePath.lastIndexOf("/")));
        if(parentFile == null ){
            throw new IOException("parent file is not exist ");
        }else{
            String fileName = absolutePath.substring(absolutePath.lastIndexOf("/")+1, absolutePath.length());
            long root_end_offset = parentFile.endOffset;
            Log.i(TAG,"filename :"+fileName+" , endOffset : "+ Long.toHexString(root_end_offset)+" , cluster :"+parentFile.endCluster);

            int cons = fileName.length()/Constants.ENAME_MAX_LEN + 2;
            ExFatFile file = new ExFatFile(absolutePath);
            file.setName(fileName);

            // 遍历根目录，找到空闲位置写入
            ByteBuffer buffer = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // 属性1 标志
            buffer.put((byte)Constants.FILE);

            // 目录项 数目
            buffer.put((byte)cons);

            long checksum_offset = root_end_offset + 2; // 记录下校验码偏移

            // 校验和 先填空
            buffer.put((byte)0x0);
            buffer.put((byte)0x0);

            // 文件属性
            if(isDirectory){
                buffer.put(Constants.ATTRIB_DIR);
            }else{
                buffer.put(Constants.ATTRIB_ARCH);
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
            ExFatFileSystem.da.write(buffer,root_end_offset);
            root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
            buffer.clear();

            // 标志位
            buffer.put((byte)Constants.StreamExtension);

            // 文件碎片标志 03H 连续存放 没有碎片 01H 有碎片
            buffer.put((byte)0x03);

            // 保留
            buffer.put((byte)0);

            // 文件字符数
            buffer.put((byte)fileName.length());

            // 文件名 HASH
            buffer.putInt(ExFatUtil.hashName(fileName));

            // 文件大小
            if(isDirectory){
                buffer.putLong(ExFatUtil.getBytesPerCluster()); // 分配一个簇
            }else{
                buffer.putLong(0L);                             // 新文件大小为0
            }

            // 保留
            buffer.put((byte)0);
            buffer.put((byte)0);
            buffer.put((byte)0);
            buffer.put((byte)0);

            // 起始簇号

            long freeCluster = AllocationBitmap.getNextFreeCluster();
            buffer.putInt((int)freeCluster);

            // 文件大小
            buffer.putLong(ExFatUtil.getBytesPerCluster()); // 分配一个簇

            int addCheckSum = ExFatUtil.addChecksum(startCheckSum,buffer);
            buffer.flip();
            ExFatFileSystem.da.write(buffer,root_end_offset);
            root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
            buffer.clear();

            // 属性3 如果文件名称太长,则存在多个属性3
            int count = fileName.length()/Constants.ENAME_MAX_LEN +1;
            int index = 0;
            for(int i = 0  ; i < count ; i++){
                // 属性3 标志
                buffer.put((byte)Constants.FileName);
                // 保留
                buffer.put((byte)0);
                for(int j = 0 ; j<Constants.ENAME_MAX_LEN ; j++){
                    if( index < fileName.length()){
                        buffer.putShort((short)(fileName.charAt(index) & 0xffff));
                    }else{
                        buffer.putShort((short)0);
                    }
                    index++;

                }
                addCheckSum = ExFatUtil.addChecksum(addCheckSum,buffer);
                buffer.flip();
                ExFatFileSystem.da.write(buffer,root_end_offset);
                root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
                buffer.clear();
            }


            // 写入校验码
            ByteBuffer checkSumBuffer = ByteBuffer.allocate(2);
            checkSumBuffer.order(ByteOrder.LITTLE_ENDIAN);
            checkSumBuffer.putShort((short)addCheckSum);
            checkSumBuffer.flip();
            ExFatFileSystem.da.write(checkSumBuffer,checksum_offset);

            AllocationBitmap.useCluster(freeCluster); // 将空闲簇置为已使用
            ExFatFileSystem.da.flush(); // 最后刷新下文件系统

            parentFile.endOffset = root_end_offset;
            parentFile.addFile(file);
            parentFile.addCache(file.getName(),file);
        }
        return true;
    }


    public boolean delete() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Log.i(TAG,"attr1 offset => "+ Long.toHexString(firstEntry.getOffset()));
        // 属性1标志置为删除
        buffer.put((byte)Constants.FILE_DEL);
        buffer.flip();
        ExFatFileSystem.da.write(buffer, firstEntry.getOffset());

        // 属性2标志置为删除
        Log.i(TAG,"attr2 offset => "+ Long.toHexString(secondEntry.getOffset()));
        buffer.clear();
        buffer.put((byte)Constants.StreamExtension_DEL);
        buffer.flip();
        ExFatFileSystem.da.write(buffer,secondEntry.getOffset());

        // 属性3标志置为删除
        for(FileNameEntry nameEntry:thirdsEntry){
            Log.i(TAG,"attr3 offset => "+ Long.toHexString(nameEntry.getOffset()));
            buffer.clear();
            buffer.put((byte)Constants.FileName_DEL);
            buffer.flip();
            ExFatFileSystem.da.write(buffer,nameEntry.getOffset());
        }
        return true;
    }



    public List<ExFatFile> getChildren(){
        return children;
    }

    public void addFile(ExFatFile file){
        children.add(file);
    }

    public void updateCache() {
        for(ExFatFile file : children){
            file.name = file.firstEntry.getName();
            addCache(file.name,file);
            ExFatCache.extFatFileCache.put("/"+file.name,file);
        }
    }
    private void addCache(String name , ExFatFile exFatFile){
        cache.put(name,exFatFile);
    }

    public ExFatFile getCache(String name){
        return cache.get(name);
    }

    public void setFirstEntry(ExFatFileEntry firstEntry){
        this.firstEntry = firstEntry;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("[root=").append(getName()).append(" , children = ");
        for(ExFatFile child : children){
            sb.append(child.getName()+";");
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean isRoot() {
        return isRoot;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getFileInfoCluster() {
        return fileInfoCluster;
    }

    public void setFileInfoCluster(long fileInfoCluster) {
        this.fileInfoCluster = fileInfoCluster;
    }

    public long length() {
        return firstEntry.getLength();
    }

    public String getName(){
        return name;
    }

    public String getAbsolutePath(){
        return absolutePath;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public void setLength(long length) {
        this.length = length;
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
}
