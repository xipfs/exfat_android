package com.lenovo.exfat.fs.exfat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 簇位图文件。簇位图文件是ExFAT文件系统中的一个元文件，类似于 NTFS 文件系统中的元文件$BitMap，用来管理分区中簇的使用情况
 * 簇位图文件中的每一个位，映射到数据区中的每一个簇。
 * 如果某个簇分配给了文件，该簇在簇位图文件中对应的位就会被填入 1 ，表示该簇已经占用；
 * 如果没有使用的空簇，它们在簇位图文件中对应的位就是 0
 */
public final class ClusterBitMap {

    public static ClusterBitMap read(ExFatSuperBlock sb, long startCluster, long size) throws IOException {
        Cluster.checkValid(startCluster);
        final ClusterBitMap result = new ClusterBitMap(sb, startCluster, size);
        if (size < ((result.clusterCount + 7) / 8)) {
            throw new IOException("cluster bitmap too small");
        }
        return result;
    }

    private final ExFatSuperBlock sb;
    private final long startCluster; // 开始簇
    private final long size;         // BitMap 大小
    private final long clusterCount; // 簇数量
    private final long devOffset;
    private final DeviceAccess da;

    private ClusterBitMap(
        ExFatSuperBlock sb, long startCluster, long size)
        throws IOException {
        this.sb = sb;
        this.da = sb.getDeviceAccess();
        this.startCluster = startCluster;
        this.size = size;
        this.clusterCount = sb.getClusterCount() - Cluster.FIRST_DATA_CLUSTER;
        this.devOffset = sb.clusterToOffset(startCluster);
    }

    public boolean isClusterFree(long cluster) throws IOException {
        Cluster.checkValid(cluster, this.sb);

        final long bitNum = cluster - Cluster.FIRST_DATA_CLUSTER;
        final long offset = bitNum / 8;
        final int bits = this.da.getUint8(offset + this.devOffset);
        return (bits & (1 << (bitNum % 8))) == 0;
    }

    public void freeCluster(long cluster) throws IOException{
        Cluster.checkValid(cluster, this.sb);
        final long bitNum = cluster - Cluster.FIRST_DATA_CLUSTER;
        final long offset = bitNum / 8;
        final int bits = this.da.getUint8(offset + this.devOffset);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) (1 << (bitNum % 8)));
        this.da.write(buffer,offset + this.devOffset);

    }

    public long getStartCluster() {
        return startCluster;
    }

    public long getClusterCount() {
        return clusterCount;
    }

    public long getUsedClusterCount() throws IOException {
        long result = 0;
        for (long i = 0; i < size; i++) {
            final int bits = this.da.getUint8(this.devOffset + i);
            result += Integer.bitCount(bits);
        }
        return result;
    }


    public String toString(){
        final StringBuilder result = new StringBuilder();
        result.append(ClusterBitMap.class.getSimpleName());
        result.append(" [开始簇 =");
        result.append(this.startCluster);
        result.append(", BitMap 大小 = ");
        result.append(this.size);
        result.append(", 簇数量 = ");
        result.append(this.clusterCount);
        result.append("]");

        return result.toString();
    }
}
