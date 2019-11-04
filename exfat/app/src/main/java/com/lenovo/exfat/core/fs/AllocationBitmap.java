package com.lenovo.exfat.core.fs;



import android.util.Log;

import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Allocation Bitmap 簇分配位图
 * Allocation Bitmap keeps track of the allocation status of clusters.
 * FAT does not serve this purpose as in FAT16/FAT32 file system.Allocation Bitmap consists of a number of 8 bit bytes which can be treated as a sequence of bits. Each bit in bitmap corresponds to a data cluster.
 *
 * If it has a value of 1, the cluster is occupied,
 * if 0 - the cluster is free.
 * The least significant bit of bitmap table refers to the first cluster, i.e. cluster 2.
 *
 * @auther xiehui
 * @create 2019-10-10 下午5:02
 */
public class AllocationBitmap {
    private static final String TAG = "exfat : "+AllocationBitmap.class.getSimpleName();

    public static final long FIRST_DATA_CLUSTER = 2; // 起始簇，前两个簇有特殊用途

    private  long bitmapCluster;          // BitMap 所在簇号
    private  long size;                   // BitMap 大小
    private  long clusterCount;           // 簇数量
    private  long offset;                 // 偏移位置
    private  ByteBuffer buffer;           // 簇位图
    private ExFatFileSystem exFatFileSystem;
    public AllocationBitmap(ExFatFileSystem exFatFileSystem) {
        this.exFatFileSystem = exFatFileSystem;
    }

    public void build(long bitmapCluster, long size) throws IOException{
        this.bitmapCluster = bitmapCluster;
        this.size = size;
        this.clusterCount = Constants.CLUSTER_COUNT - Cluster.FIRST_DATA_CLUSTER;
        this.offset = ExFatUtil.clusterToOffset(bitmapCluster); // 定位到簇位图所在位置

        buffer = ByteBuffer.allocate(ExFatUtil.getBytesPerCluster());
        buffer.order(ByteOrder.LITTLE_ENDIAN);

    }

    /**
     * 判断此簇有没有被使用
     * @param cluster
     * @return
     * @throws IOException
     */

    public boolean isClusterFree(long cluster) throws IOException {
        final long bitNum = cluster - Cluster.FIRST_DATA_CLUSTER;
        final long bitOffset = bitNum / 8;
        final int bits = exFatFileSystem.da.getUint8(offset + bitOffset);
        return (bits & (1 << (bitNum % 8))) == 0;
    }

    /**
     * 找到一个空闲的簇
     * @return
     */
    public long getNextFreeCluster() throws IOException{
        for(long i = 20 ; i<Constants.CLUSTER_COUNT; i++){
            if(isClusterFree(i)){
                return i;
            }
        }
        throw new IOException("未找到空闲的簇");
    }

    public long getNextFreeCluster(long current) throws IOException{
        long next = current+1;
        for(; next<Constants.CLUSTER_COUNT; next++){
            if(isClusterFree(next)){
                return next;
            }
        }
        throw new IOException("未找到空闲的簇");
    }

    public long getUsedClusterCount() throws IOException{
        int count = 0;
        exFatFileSystem.da.read(buffer, offset);
        buffer.clear();
        while(buffer.remaining() > 0) {
            final long bits = DeviceAccess.getUint64(buffer);
            count += Long.bitCount(bits);
        }
        buffer.clear();
        Log.i(TAG,"bitmap offset : "+offset+" , count : "+count);
        return count;
    }

    /**
     * 将此簇置为未使用
     *
     * @param cluster
     * @throws IOException
     */
    public void freeCluster(long cluster) throws IOException{
        final long bitNum = cluster - Cluster.FIRST_DATA_CLUSTER;
        final long bitOffset = bitNum / 8;
        int bits = exFatFileSystem.da.getUint8(offset + bitOffset);
        bits = bits & (~(1 << (bitNum % 8)));
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put((byte)bits);
        buffer.flip();
        exFatFileSystem.da.write(buffer,offset + bitOffset);
    }
    /**
     * 将此簇置为使用
     *
     * @param cluster
     * @throws IOException
     */
    public void useCluster(long cluster) throws IOException{
        final long bitNum = cluster - Cluster.FIRST_DATA_CLUSTER;
        final long bitOffset = bitNum / 8;
        int bits = exFatFileSystem.da.getUint8(offset + bitOffset);
        bits = bits | (1 << (bitNum % 8));
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put((byte)bits);
        buffer.flip();
        exFatFileSystem.da.write(buffer,offset + bitOffset);
    }

    public String toString(){
        final StringBuilder result = new StringBuilder();
        result.append(" [开始簇 =");
        result.append(this.bitmapCluster);
        result.append(", BitMap 大小 = ");
        result.append(this.size);
        result.append(", 簇数量 = ");
        result.append(this.clusterCount);
        result.append("]");
        return result.toString();
    }
}
