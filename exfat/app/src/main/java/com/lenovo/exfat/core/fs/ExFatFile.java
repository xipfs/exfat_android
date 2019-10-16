package com.lenovo.exfat.core.fs;


import android.util.Log;

import com.lenovo.exfat.core.fs.directory.EntryTimes;
import com.lenovo.exfat.core.fs.directory.ExFatFileEntry;
import com.lenovo.exfat.core.fs.directory.FileNameEntry;
import com.lenovo.exfat.core.fs.directory.StreamExtensionEntry;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
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
    private long fileCluster;

    private ExFatFileEntry entry;
    private StreamExtensionEntry secondEntry;
    private List<FileNameEntry>  thirdsEntry = new ArrayList<>();

    private Map<String,ExFatFile> cache = new HashMap<>();  // 缓存子文件
    private List<ExFatFile> children = new ArrayList<ExFatFile>();

    private long root_end_offset;  // 根目录项结尾偏移
    private long root_end_cluster; // 根目录项结尾出口就

    public ExFatFile(){

    }
    public ExFatFile(String name){
        this.name = name;
    }


    public boolean createFile() throws IOException{
        ExFatFile file = find(name);
        if(file !=null ){
            throw new IOException("文件已经存在 !");
        }
        String[] paths = name.split("/");
        if(paths.length == 2){
            return createRootFile();
        }else if(paths.length > 2){
            return createChildFile();
        }
        throw  new IOException("错误的文件路径 !");
    }


    public boolean mkdir() throws IOException{
        ExFatFile file = find(name);
        if(file !=null ){
            throw new IOException("目录已经存在 !");
        }
        String[] paths = name.split("/");
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
        String[] paths = name.split("/");
        String fileName = name.substring(name.lastIndexOf("/")+1, name.length());
        long root_end_offset = ExFatFileSystem.root.root_end_offset;
        Log.i(TAG,"filename :"+fileName+" , root_end_offset : "+ Long.toHexString(root_end_offset)+" , cluster :"+ExFatFileSystem.root.root_end_cluster);

        int cons = fileName.length()/Constants.ENAME_MAX_LEN + 2;
        ExFatFile file = new ExFatFile(fileName);

        // 遍历根目录，找到空闲位置写入
        ByteBuffer buffer = ByteBuffer.allocate(Constants.DIR_ENTRY_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 属性1 标志
        buffer.put((byte)0x85);

        // 目录项 数目
        buffer.put((byte)cons);

        long checksum_offset = root_end_offset + 2; // 记录下校验码偏移

        // 校验和
        buffer.put((byte)0x0);
        buffer.put((byte)0x0);

        // 文件属性
        buffer.put(Constants.ATTRIB_DIR);
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
        // 时区
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

        int startCheckSum = startChecksum(buffer);

        buffer.flip();
        ExFatFileSystem.da.write(buffer,root_end_offset);
        root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
        buffer.clear();

        // 标志位
        buffer.put((byte)0xc0);
        buffer.put((byte)0x03);
        buffer.put((byte)0);
        buffer.put((byte)0x05);
        buffer.put((byte)0x34);
        buffer.put((byte)0x0e);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0x80);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0x08);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0x80);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        int addCheckSum = addChecksum(startCheckSum,buffer);
        buffer.flip();
        ExFatFileSystem.da.write(buffer,root_end_offset);
        root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
        buffer.clear();

        // 属性3
        buffer.put((byte)0xc1);
        buffer.put((byte)0);
        buffer.put((byte)0x68);
        buffer.put((byte)0);
        buffer.put((byte)0x65);
        buffer.put((byte)0);
        buffer.put((byte)0x6c);
        buffer.put((byte)0);
        buffer.put((byte)0x6c);
        buffer.put((byte)0);
        buffer.put((byte)0x6f);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)0);
        addCheckSum = addChecksum(addCheckSum,buffer);
        buffer.flip();
        ExFatFileSystem.da.write(buffer,root_end_offset);

        // 写入校验码
        ByteBuffer checkSumBuffer = ByteBuffer.allocate(2);
        checkSumBuffer.order(ByteOrder.LITTLE_ENDIAN);
        checkSumBuffer.putShort((short)addCheckSum);
        checkSumBuffer.flip();
        ExFatFileSystem.da.write(checkSumBuffer,checksum_offset);

        AllocationBitmap.useCluster(0x8);
        ExFatFileSystem.da.flush(); // 最后刷新下文件系统

        root_end_offset  = root_end_offset+Constants.DIR_ENTRY_SIZE;
        ExFatFileSystem.root.root_end_offset = root_end_offset;
        ExFatFileSystem.root.addFile(file);
        ExFatFileSystem.root.addCache(file.getName(),file);

        return true;
    }

    /**
     * 创建根文件
     * @return
     */
    private boolean createRootFile(){
        ExFatFile file = new ExFatFile();

        return true;
    }

    /**
     * 创建子目录
     * @return
     */
    private boolean mkChildDir(){
        return true;
    }

    /**
     * 创建子文件
     * @return
     */
    private boolean createChildFile(){
        return true;
    }

    public ExFatFile find(String path) {
        String[] paths = path.split("/");
        if(paths.length ==0 ){ //
            return this;
        }if(paths.length ==2 ){ //
            return cache.get(paths[1]);
        }else {
            return cache.get(paths[1]).find(path.substring(paths[1].length()+1,path.length()));
        }
    }


    public boolean delete() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Log.i(TAG,"attr1 offset => "+ Long.toHexString(entry.getOffset()));
        // 属性1标志置为删除
        buffer.put((byte)Constants.FILE_DEL);
        buffer.flip();
        ExFatFileSystem.da.write(buffer,entry.getOffset());

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


    private int startChecksum(ByteBuffer buffer) {
        buffer.flip();
        int result = 0;
        for (int i = 0; i < Constants.DIR_ENTRY_SIZE; i++) {
            final int b = DeviceAccess.getUint8(buffer);
            if ((i == 2) || (i == 3)) {
                continue;
            }
            result = ((result << 15) | (result >> 1)) + b;
            result &= 0xffff;
        }
        return result;
    }

    private int addChecksum(int sum,ByteBuffer buffer) {
        buffer.flip();
        for (int i = 0; i < Constants.DIR_ENTRY_SIZE ; i++) {
            final int b = DeviceAccess.getUint8(buffer);
            sum = ((sum << 15) | (sum >> 1)) + b;
            sum &= 0xffff;
        }
        return sum;
    }

    public List<ExFatFile> getChildren(){
        return children;
    }

    public void addFile(ExFatFile file){
        children.add(file);
    }

    public void updateCache() {
        for(ExFatFile file : children){
            file.name = file.entry.getName();
            addCache(file.name,file);
        }
    }
    private void addCache(String name , ExFatFile exFatFile){
        cache.put(name,exFatFile);
    }

    public ExFatFile getCache(String name){
        return cache.get(name);
    }

    public void setEntry(ExFatFileEntry entry){
        this.entry = entry;
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

    public long length() {
        return entry.getLength();
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

    public ExFatFileEntry getEntry() {
        return entry;
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

    public long getRoot_end_offset() {
        return root_end_offset;
    }

    public void setRoot_end_offset(long root_end_offset) {
        this.root_end_offset = root_end_offset;
    }

    public long getRoot_end_cluster() {
        return root_end_cluster;
    }

    public void setRoot_end_cluster(long root_end_cluster) {
        this.root_end_cluster = root_end_cluster;
    }
}
