package com.lenovo.exfat.core.fs;


import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * DOS 引导记录
 * @auther xiehui
 * @create 2019-10-10 下午3:10
 */
public class DosBootRecord {

    public static final String TAG = DosBootRecord.class.getSimpleName();
    private static final int SIZE = 512;
    private static final String OEM_NAME = "EXFAT   ";

    private final DeviceAccess da;

    private long partitionOffset;		// 隐藏扇区数
    private long totalBlocks;			// 总扇区数
    private long fatBlockStart;			// FAT 起始扇区号
    private int fatBlockCount;			// FAT 扇区数
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

    public DosBootRecord(DeviceAccess da){
        this.da = da;
    }
    public void build() throws IOException {
        final ByteBuffer b = ByteBuffer.allocate(SIZE); // 设置 512 字节缓冲区
        b.order(ByteOrder.LITTLE_ENDIAN); // 小端序
        da.read(b,0l);
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
        this.percentInUse = b.get(0x70);            // 使用百分比

        // 保存进常量.
        Constants.TOTAL_BLOCKS = totalBlocks;
        Constants.FAT_BLOCK_START = fatBlockStart;
        Constants.FAT_BLOCK_COUNT = fatBlockCount;
        Constants.CLUSTER_BLOCK_START = clusterBlockStart;
        Constants.CLUSTER_COUNT = clusterCount;
        Constants.ROOT_DIRECTORY_CLUSTER = rootDirCluster;
        Constants.VOLUME_SERIAL = volumeSerial;
        Constants.MINOR_VERSION = fsVersionMinor;
        Constants.MAJOR_VERSION = fsVersionMajor;
        Constants.VOLUME_STATE = volumeState;
        Constants.BLOCK_BITS = blockBits;
        Constants.CLUSTER_BLOCKS = blocksPerClusterBits;
        Constants.PERCENT_IN_USE = percentInUse;

        // 设置簇对应的扇区数目
        Cluster.SIZE = ExFatUtil.getBlocksPerCluster();

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
        sb.append("每扇区字节数: "+ExFatUtil.getBlockSize()+"\n");
        sb.append("每簇扇区数: "+ExFatUtil.getBlocksPerCluster()+"\n");
        sb.append("每簇字节数: "+ExFatUtil.getBytesPerCluster()+"\n");
        sb.append("使用百分比: "+percentInUse+"\n");
        return sb.toString();
    }
    public DeviceAccess getDeviceAccess() {
        return da;
    }
}
