package com.lenovo.exfat.core.fs;


/**
 * 文件分配表项
 *
 * 每一项大小固定为4字节
 *
 * @auther xiehui
 * @create 2019-10-10 下午5:11
 */
public class FatEntry {
    private static final long BAD = 0xFFFFFFF7L;
    private static final long END = 0xFFFFFFFFL;
    private static final long UNDEFINED = 0x00000000L;
    private static final long MEDIA = 0xFFFFFFF8L; // 首字节 F8 表明是硬盘
    private long nexCluster; // 下一个簇
    public FatEntry(long nexCluster) {
        this.nexCluster = nexCluster;
    }
    public String toString(){
        return "FatEntry :"+Long.toHexString(nexCluster);
    }
    public long getNextCluster(){
        return nexCluster;
    }
}
