package com.lenovo.exfat.core.fs;


import com.lenovo.exfat.core.fs.directory.ExFatFileEntry;

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
    private boolean isDirectory;
    private boolean isRoot;
    private String name;
    private String absolutePath;
    private long length;
    private ExFatFileEntry entry;
    private Map<String,ExFatFile> cache = new HashMap<>();  // 缓存子文件
    private List<ExFatFile> children = new ArrayList<ExFatFile>();
    private long fileCluster;
    public ExFatFile(){

    }
    public ExFatFile(String name){
        this.name = name;
    }

    public ExFatFile findChild(String path) {
        String[] paths = path.split("/");
        if(paths.length ==0 ){ //
            return this;
        }if(paths.length ==2 ){ //
            return cache.get(paths[1]);
        }else {
            return cache.get(paths[1]).findChild(path.substring(paths[1].length()+1,path.length()));
        }
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
}
