package com.lenovo.exfat.core.fs.directory;


import java.nio.ByteBuffer;

/**
 * 文件目录项信息，存放文件元信息
 *
 * @auther xiehui
 * @create 2019-10-11 下午5:00
 */
public class ExFatFileEntry {

    private int conts;          // 目录项数量 属性2+属性3
    private int checkSum;       // 校验码
    private int attrib;         // 属性
    private EntryTimes times;   // 时间

    private int flag;           // 碎片标志
    private int nameLen;        // 名称长度
    private int nameHash;       // 名称哈希
    private long length;        // 文件大小
    private long fileCluster;   // 文件簇号
    private String name;

    private long offset;         // 文件目录项在簇中偏移
    private long cluster;       // 文件目录项簇号
    private boolean end;        // 是否结束


    private StringBuilder sb =new StringBuilder();        // 文件名

    public void addChar(char c){
        sb.append(c);
    }

    public String getName(){
        if(name != null){
            return name;
        }else{
            name = sb.substring(0,nameLen);
            return name;
        }
    }

    public void setName(String name){
        this.name = name;
    }

    public boolean isEnd(){
        return sb.length() >= nameLen;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public int getNameLen() {
        return nameLen;
    }

    public void setNameLen(int nameLen) {
        this.nameLen = nameLen;
    }

    public int getNameHash() {
        return nameHash;
    }

    public void setNameHash(int nameHash) {
        this.nameHash = nameHash;
    }

    public long getLength() {
        return length;
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

    public int getConts() {
        return conts;
    }

    public void setConts(int conts) {
        this.conts = conts;
    }

    public int getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(int checkSum) {
        this.checkSum = checkSum;
    }

    public int getAttrib() {
        return attrib;
    }

    public void setAttrib(int attrib) {
        this.attrib = attrib;
    }

    public EntryTimes getTimes() {
        return times;
    }

    public void setTimes(EntryTimes times) {
        this.times = times;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getCluster() {
        return cluster;
    }

    public void setCluster(long cluster) {
        this.cluster = cluster;
    }
}
