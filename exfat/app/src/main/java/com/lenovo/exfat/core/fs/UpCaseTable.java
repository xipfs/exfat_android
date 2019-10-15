package com.lenovo.exfat.core.fs;


import com.lenovo.exfat.core.util.ExFatUtil;

/**
 * 大写字符文件
 *
 * http://www.ntfs.com/exfat-upcase-table.htm
 *
 * exFAT 文件系统格式
 *
 * ---------------------------------------------------------
 * | DBR 及其保留扇区| FAT | 簇位图文件 | 大写字符文件 | 用户数据区|
 * ---------------------------------------------------------
 *
 * @auther xiehui
 * @create 2019-10-10 下午5:04
 */
public class UpCaseTable {
    private final DeviceAccess da;
    private long upcaseTableCluster;
    private long size;
    private long offset;

    public UpCaseTable(DeviceAccess da) {
        this.da = da;
    }

    public void build(long upcaseTableCluster, long size) {
        this.upcaseTableCluster = upcaseTableCluster;
        this.size = size;
        offset = ExFatUtil.clusterToOffset(upcaseTableCluster);
    }


    public String toString(){
        final StringBuilder result = new StringBuilder();
        result.append(" [偏移位置 = ");
        result.append(this.offset);
        result.append(", table 大小 = ");
        result.append(this.size);
        result.append("]");
        return result.toString();
    }
}
