package com.lenovo.exfat.core.util;


import android.util.Log;

import com.lenovo.exfat.core.fs.Cluster;
import com.lenovo.exfat.core.fs.DeviceAccess;
import com.lenovo.exfat.core.fs.UpCaseTable;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * exFAT 工具类
 *
 * @auther xiehui
 * @create 2019-10-11 上午11:11
 */
public class ExFatUtil {
    private static final String TAG = ExFatUtil.class.getSimpleName();
    // 计算簇对应扇区位置
    public static long clusterToBlock(long cluster){
        return Constants.CLUSTER_BLOCK_START +
                ((cluster - Cluster.FIRST_DATA_CLUSTER) <<
                        Constants.CLUSTER_BLOCKS);
    }

    // 扇区对应字节偏移
    public static long blockToOffset(long block) {
        return (block << Constants.BLOCK_BITS);
    }
    // 簇对应字节偏移
    public static long clusterToOffset(long cluster) {
        return blockToOffset(clusterToBlock(cluster));
    }

    public static void readCluster(DeviceAccess da, ByteBuffer dest, long cluster) throws IOException {
        da.read(dest, clusterToOffset(cluster));
    }

    public static void writeCluster(DeviceAccess da,ByteBuffer dest, long cluster) throws IOException{
        da.write(dest, clusterToOffset(cluster));
    }
    // 每扇区字节数 2^n
    public static int getBlockSize() {
        return (1 << Constants.BLOCK_BITS);
    }

    // 每簇扇区数 2^n
    public static int getBlocksPerCluster() {
        return (1 << Constants.CLUSTER_BLOCKS);
    }

    // 每簇字节数
    public static int getBytesPerCluster() {
        return (getBlockSize() << Constants.CLUSTER_BLOCKS);
    }

    public static  int startChecksum(ByteBuffer buffer) {
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

    public static  int addChecksum(int sum,ByteBuffer buffer) {
        buffer.flip();
        for (int i = 0; i < Constants.DIR_ENTRY_SIZE ; i++) {
            final int b = DeviceAccess.getUint8(buffer);
            sum = ((sum << 15) | (sum >> 1)) + b;
            sum &= 0xffff;
        }
        return sum;
    }

    /**
     * 文件名 HASH
     * @param name
     * @return
     * @throws IOException
     */
    public static int hashName(String name) throws IOException {
        int hash = 0;

        for (int i = 0; i < name.length(); i++) {
            final int c =  UpCaseTable.toUpperCase(name.charAt(i));

            hash = ((hash << 15) | (hash >> 1)) + (c & 0xff);
            hash &= 0xffff;
            hash = ((hash << 15) | (hash >> 1)) + (c >> 8);
            hash &= 0xffff;
        }

        return (hash & 0xffff);
    }

    public static int[] toUnicode(String s) {
        int[] s1 = new int[s.length()];
        for (int i = 0; i < s.length(); i++) {
            s1[i] = s.charAt(i) & 0xffff;
        }
        return s1;
    }
}
