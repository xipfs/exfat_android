package com.lenovo.exfat.core.fs;


import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

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
    private static long upcaseTableCluster;
    private static long size;
    private static long offset;
    private static long chars;

    public static void build(long upcaseTableCluster, long size) {
        UpCaseTable.upcaseTableCluster = upcaseTableCluster;
        UpCaseTable.size = size;
        UpCaseTable.chars = size / 2;
        offset = ExFatUtil.clusterToOffset(upcaseTableCluster);
    }

    public static char toUpperCase(char c) throws IOException {
        if (c > chars) {
            return c;
        } else {
            return ExFatFileSystem.da.getChar(offset + (c * 2));
        }
    }

    public static String toUpperCase(String s) throws IOException {
        final StringBuilder result = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            result.append(toUpperCase(c));
        }
        return result.toString();
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
