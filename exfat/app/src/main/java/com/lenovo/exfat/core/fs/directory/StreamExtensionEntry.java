package com.lenovo.exfat.core.fs.directory;


import java.nio.ByteBuffer;

/**
 *
 * 属性2
 *
 * @auther xiehui
 * @create 2019-10-15 下午1:52
 */
public class StreamExtensionEntry {

    private int flag;           // 碎片标志
    private int nameLen;        // 名称长度
    private int nameHash;       // 名称哈希
    private long length;        // 文件大小


    private long offset;        // 文件目录项在簇中偏移
    private long cluster;       // 文件目录项簇号
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
}
