package com.lenovo.exfat.fs.exfat;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/**
 * DBR(Dos Boot Record)
 */
public final class ExFatSuperBlock{
    private static final String TAG = ExFatFileSystem.class.getSimpleName();
    private static final int SIZE = 512;
    private static final String OEM_NAME = "EXFAT   ";

    private final DeviceAccess da;

    private long partitionOffset;		// 隐藏扇区数
    private long totalBlocks;			// 总扇区数
    private long fatBlockStart;			// FAT 起始扇区号
    private long fatBlockCount;			// FAT 扇区数
    private long clusterBlockStart;		// 首簇起始扇区号
    private long clusterCount;			// 总簇数
    private long rootDirCluster;		// 根目录首簇号
    private int  volumeSerial;			// 卷序列号
    private byte fsVersionMinor;		// 版本号
    private byte fsVersionMajor;  
    private short volumeState;  		// 卷状态
    private byte blockBits;				// 每扇区字节数 2^n
    private byte blocksPerClusterBits;	// 每簇扇区数 2^n
    private byte percentInUse;			// 使用百分比

    private BlockDeviceDriver device;
    public ExFatSuperBlock(BlockDeviceDriver device) {
        this.device = device;
        this.da = new DeviceAccess(device);
    }

    public void read() throws IOException {
        final ByteBuffer b = ByteBuffer.allocate(SIZE); // 设置 512 字节缓冲区
        b.order(ByteOrder.LITTLE_ENDIAN); // 小端序
        device.read(0l, b);
        /*
        StringBuilder sb = new StringBuilder();
        byte[] bs = b.array();
        for(byte bb: bs) {
            sb.append(bb).append(" ");
        }
        Log.d("ExFatSuperBlock", sb.toString());*/
        // 前3个字节为跳转指令 EB 76 90(JMP 76 NOP)
        final byte[] oemBytes = new byte[OEM_NAME.length()];
        b.position(0x03); // 跳过前3字节 检测 OEM 字符串
        b.get(oemBytes);
        final String oemString = new String(oemBytes);
        if (!OEM_NAME.equals(oemString)) {
            throw new IOException("OEM name mismatch");
        }
        /* FAT表个数 */
        if ((b.get(0x6e) & 0xff) != 1) {
            throw new IOException("invalid FAT count");
        }
        /* 驱动标记 */
        if ((b.get(0x6f) & 0xff) != 0x80) {
            throw new IOException("invalid drive number");
        }
        /* 末尾以 55 AA 作为特征结束  */
        if ((b.get(510) & 0xff) != 0x55 || (b.get(511) & 0xff) != 0xaa)
            throw new IOException("missing boot sector signature");
        this.partitionOffset = b.getLong(0x40); 	// 隐藏扇区数
        this.totalBlocks = b.getLong(0x48); 		// 总扇区数
        this.fatBlockStart = b.getInt(0x50);		// FAT 起始扇区号
        this.fatBlockCount = b.getInt(0x54);		// FAT 扇区数
        this.clusterBlockStart = b.getInt(0x58);	// 首簇起始扇区号 也就是2号簇的起始扇区，对应簇位图文件起始扇区。
        this.clusterCount = b.getInt(0x5c);		// 总簇数
        this.rootDirCluster = b.getInt(0x60);	// 根目录首簇号
        this.volumeSerial = b.getInt(0x64);		// 卷序列号
        this.fsVersionMinor = b.get(0x68);		    // 版本号
        this.fsVersionMajor = b.get(0x69);		    // 版本号
        this.volumeState = b.getShort(0x6a);		// 卷状态
        this.blockBits = b.get(0x6c);				// 每扇区字节数 2^n
        this.blocksPerClusterBits = b.get(0x6d);    // 每簇扇区数 2^n
        // 设置簇对应的扇区数目
        Cluster.SIZE = getBlocksPerCluster();
        this.percentInUse = b.get(0x70);			// 使用百分比

        /* check version */
        if (this.fsVersionMajor != 1) {
            throw new IOException("unsupported version major " +
                    this.fsVersionMajor);
        }

        if (this.fsVersionMinor != 0) {
            throw new IOException("unsupported version minor " +
                    this.fsVersionMinor);
        }
    }

    // 簇对应扇区位置
    public long clusterToBlock(long cluster) throws IOException {
        Cluster.checkValid(cluster);
        return this.clusterBlockStart +
            ((cluster - Cluster.FIRST_DATA_CLUSTER) <<
                this.blocksPerClusterBits);
    }

    // 扇区对应字节偏移
    public long blockToOffset(long block) {
        return (block << this.blockBits);
    }
    // 簇对应字节偏移
    public long clusterToOffset(long cluster) throws IOException {
        return blockToOffset(clusterToBlock(cluster));
    }

    public void readCluster(ByteBuffer dest, long cluster) throws IOException {
        assert (dest.remaining() <= this.getBytesPerCluster())
            : "read over cluster bundary";
        da.read(dest, clusterToOffset(cluster));
    }

    public void writeCluster(ByteBuffer dest, long cluster) throws IOException{
        da.write(dest, clusterToOffset(cluster));
    }
    // 每扇区字节数 2^n
    public int getBlockSize() {
        return (1 << blockBits);
    }

    // 每簇扇区数 2^n
    public int getBlocksPerCluster() {
        return (1 << blocksPerClusterBits);
    }

    // 每簇字节数
    public int getBytesPerCluster() {
        return (getBlockSize() << this.blocksPerClusterBits);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" \n");
        sb.append("隐藏扇区数: "+ partitionOffset +"\n");
        sb.append("总扇区数目: "+ totalBlocks +"\n");
        sb.append("exFAT 起始扇区号: "+fatBlockStart+"\n");
        sb.append("exFAT 扇区数: "+fatBlockCount+"\n");
        sb.append("首簇起始扇区号: "+clusterBlockStart+"\n");
        sb.append("总簇数: "+clusterCount+"\n");
        sb.append("根目录首簇号: "+rootDirCluster+"\n");
        sb.append("卷序列号: "+volumeSerial+"\n");
        sb.append("卷状态: "+volumeState+"\n");
        sb.append("每扇区字节数: "+getBlockSize()+"\n");
        sb.append("每簇扇区数: "+getBlocksPerCluster()+"\n");
        sb.append("每簇字节数: "+getBytesPerCluster()+"\n");
        sb.append("使用百分比: "+percentInUse+"\n");
        return sb.toString();
    }

    public DeviceAccess getDeviceAccess() {
        return da;
    }

    public long getPartitionOffset() {
        return partitionOffset;
    }

    public long getTotalBlocks() {
        return totalBlocks;
    }

    public long getFatBlockStart() {
        return fatBlockStart;
    }

    public long getFatBlockCount() {
        return fatBlockCount;
    }

    public long getClusterBlockStart() {
        return clusterBlockStart;
    }

    public long getClusterCount() {
        return clusterCount;
    }

    public long getRootDirCluster() {
        return rootDirCluster;
    }

    public int getVolumeSerial() {
        return volumeSerial;
    }

    public byte getFsVersionMajor() {
        return fsVersionMajor;
    }

    public byte getFsVersionMinor() {
        return fsVersionMinor;
    }

    public short getVolumeState() {
        return volumeState;
    }

    public byte getPercentInUse() {
        return percentInUse;
    }

}
