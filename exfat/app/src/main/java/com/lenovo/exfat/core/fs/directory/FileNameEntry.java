package com.lenovo.exfat.core.fs.directory;


import java.nio.ByteBuffer;

/**
 * 属性3
 * @auther xiehui
 * @create 2019-10-15 下午1:53
 */
public class FileNameEntry {
    private int offset;        // 文件目录项在簇中偏移
    private long cluster;       // 文件目录项簇号
    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public long getCluster() {
        return cluster;
    }
    public void setCluster(long cluster){
        this.cluster=cluster;
    }
}
