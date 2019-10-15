package com.lenovo.exfat.core.fs;


import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;

/**
 * 簇： 多个扇区组成一个簇
 *
 * http://www.ntfs.com/exfat-fat-cluster-heap.htm#cluster-heap
 * @auther xiehui
 * @create 2019-10-10 下午3:23
 */
public final class Cluster {

    // 簇中包含错误扇区
    private static final long BAD = 0xfffffff7l;

    // 簇结束标志
    private static final long END = 0xffffffffl;

    // 因为0，1号FAT项有特殊用途，只能从2号FAT项与2号簇形成映射，所以第一个簇是编号为2的簇
    public static final long FIRST_DATA_CLUSTER = 2;

    // 簇中包含扇区数，应该根据DBR的结果进行调整
    public static int SIZE = 64;

    public static boolean invalid(long cluster) {
        return ((cluster == END) || (cluster == BAD));
    }

    public static void checkValid(long cluster) throws IOException {
        if (cluster < FIRST_DATA_CLUSTER || invalid(cluster)) {
            throw new IOException("bad cluster number " + cluster);
        }
    }

    public static void checkValid(
            long cluster, DosBootRecord dbr) throws IOException {
        checkValid(cluster);
        final long maxCluster = Constants.CLUSTER_COUNT + 1;
        if (cluster > maxCluster) {
            final StringBuilder sb = new StringBuilder();
            sb.append("cluster ").append(cluster);
            sb.append(" exceeds maximum of ").append(maxCluster);
            throw new IOException(sb.toString());
        }
    }

    private Cluster() {
    }
}
