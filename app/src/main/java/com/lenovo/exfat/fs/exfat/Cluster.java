package com.lenovo.exfat.fs.exfat;

import java.io.IOException;

/**
 * http://www.ntfs.com/exfat-fat-cluster-heap.htm#cluster-heap
 * FAT Entry 文件分配表表项  
 * 每一个文件分配表项(FAT Entry)代表一个簇(Cluster)
 * In exFAT, FAT is not used for tracking an allocation; an Allocation Bitmap is used for this purpose. 
 * FAT is only used for keeping chains of clusters of fragmented files. 
 * If a file is not fragmented, FAT table does not need to be updated. 
 * A Stream Extensions Directory Entry should be consulted to determine if the FAT chain is valid or not. 
 * If FAT chain is not valid, it does not need to be zeroed.
 */
final class Cluster {

    /**
     * Marks a cluster containing a bad block.
     */
    private static final long BAD = 0xfffffff7l;

    /**
     * Marks the final cluster of a file or directory.
     */
    private static final long END = 0xffffffffl;

    /**
     * The first data cluster that can be used on exFAT file systems.
     * ClusterCount +1 (max 0xFFFFFFF6) — next cluster in the chain
     */
    public static final long FIRST_DATA_CLUSTER = 2;

    /**
     * The size of an exFAT cluster in blocks.
     */
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
        long cluster, ExFatSuperBlock sblk) throws IOException {
        checkValid(cluster);
        final long maxCluster = sblk.getClusterCount() + 1;
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
