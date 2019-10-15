package com.lenovo.exfat.core.util;


import com.lenovo.exfat.core.fs.Cluster;
import com.lenovo.exfat.core.fs.DeviceAccess;

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
}
