package com.lenovo.exfat.fs.exfat;

import com.lenovo.exfat.fs.UsbFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExFatUsbFile implements UsbFile {
    private boolean isDirectory;
    private String name;
    private String absolutePath;
    private long createTime;
    private long modifiedTime;
    private long accessedTime;
    private UsbFile parent;
    private boolean isRoot;
    private boolean isLeaf;
    private long length;
    private ExFatFileSystem fs;
    private List<ExFatUsbFile> children = new ArrayList<>();

    public ExFatUsbFile(ExFatFileSystem fs){
        this.fs = fs;
    }


    public void addChild(ExFatUsbFile exFatFile) {
        children.add(exFatFile);
    }

    @Override
    public UsbFile search(String path) throws IOException {
        return fs.files.get(path);
    }

    @Override
    public boolean delete() throws IOException {
        NodeEntry entry = fs.entryMap.get(absolutePath);
        NodeFile file = entry.getFile();
        return file.delete();
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    public void isDirectory(boolean isDirectory){
        this.isDirectory = isDirectory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath){
        this.absolutePath = absolutePath;
    }

    @Override
    public void setName(String newName) throws IOException {
        this.name = newName;
    }

    @Override
    public long createdAt() {
        return createTime;
    }
    public void setCreateTime(long createTime){
        this.createTime = createTime;
    }
    @Override
    public long lastModified() {
        return modifiedTime;
    }
    public void setModifiedTime(long modifiedTime){
        this.modifiedTime = modifiedTime;
    }

    @Override
    public long lastAccessed() {
        return accessedTime;
    }
    public void setAccessedTime(long accessedTime){
        this.accessedTime = accessedTime;
    }

    @Override
    public UsbFile getParent() {
        return parent;
    }

    @Override
    public String[] list() throws IOException {
       return null;
    }

    @Override
    public UsbFile[] listFiles() throws IOException {
        return null;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public void setLength(long newLength) throws IOException {
        this.length = newLength;
    }

    @Override
    public void read(long offset, ByteBuffer destination) throws IOException {
        NodeEntry entry = fs.entryMap.get(absolutePath);
        NodeFile file = entry.getFile();
        file.read(offset,destination);
    }

    @Override
    public void write(long offset, ByteBuffer source) throws IOException {

    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public UsbFile createDirectory(String name) throws IOException {
        ExFatUsbFile dir = new ExFatUsbFile(fs);
        dir.isDirectory(true);
        dir.setLength(0);
        dir.setName(name);
        return dir;
    }

    @Override
    public UsbFile createFile(String name) throws IOException {
        ExFatUsbFile file = new ExFatUsbFile(fs);
        file.isDirectory(false);
        file.setLength(0);
        file.setName(name);
        return file;
    }


    @Override
    public void moveTo(UsbFile destination) throws IOException {

    }



    @Override
    public boolean isRoot() {
        return false;
    }

    public void isRoot(boolean isRoot){
        this.isRoot = isRoot;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(absolutePath).append("\t");
        if(isDirectory){
            sb.append("\t directory");
        }else{
            sb.append("\t file");
        }
        sb.append("\t");
        sb.append(length).append("\n");
        for(UsbFile file : fs.files.values()){
            sb.append("\t");
            sb.append(file.getAbsolutePath());
            if(file.isDirectory()){
                sb.append("\t directory");
            }else{
                sb.append("\t file");
            }
            sb.append("\t");
            sb.append(file.getLength()).append("\n");
        }
        return sb.toString();

    }

}