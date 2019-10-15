package com.lenovo.exfat.core.partition;


import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;

/**
 * @auther xiehui
 * @create 2019-10-10 下午1:47
 */
public class Partition {
    private BlockDeviceDriver blockDevice;
    private int logicalOffsetToAdd;
    private int blockSize;
    private long blocks;

    public Partition(BlockDeviceDriver blockDevice, int logicalOffsetToAdd) {
        this.blockDevice = blockDevice;
        this.logicalOffsetToAdd = logicalOffsetToAdd;
        this.blockSize = blockDevice.getBlockSize();
        this.blocks = blockDevice.getBlocks();
    }

    public static Partition createPartition(BlockDeviceDriver blockDevice,int logicalOffsetToAdd) throws IOException {
        // 创建分区信息 , entry 中包含磁盘的逻辑偏移地址
        Partition partition = new Partition(blockDevice,logicalOffsetToAdd);
        return partition;

    }

    public BlockDeviceDriver getBlockDevice() {
        return blockDevice;
    }

    public void setBlockDevice(BlockDeviceDriver blockDevice) {
        this.blockDevice = blockDevice;
    }

    public int getLogicalOffsetToAdd() {
        return logicalOffsetToAdd;
    }

    public void setLogicalOffsetToAdd(int logicalOffsetToAdd) {
        this.logicalOffsetToAdd = logicalOffsetToAdd;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public long getBlocks() {
        return blocks;
    }

    public void setBlocks(long blocks) {
        this.blocks = blocks;
    }
}
