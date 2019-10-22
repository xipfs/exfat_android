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

    public void setCluster(long cluster) {
        this.cluster = cluster;
    }
}
