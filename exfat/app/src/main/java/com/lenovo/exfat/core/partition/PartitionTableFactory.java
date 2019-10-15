package com.lenovo.exfat.core.partition;


import android.util.Log;


import com.lenovo.exfat.core.partition.mbr.MasterBootRecord;
import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;

/**
 * 创建分区表
 *
 * @auther xiehui
 * @create 2019-10-10 上午11:11
 */
public class PartitionTableFactory {
    public static PartitionTable createPartitionTable(BlockDeviceDriver blockDevice) throws IOException {
        MasterBootRecord mbr = new MasterBootRecord();
        return mbr.read(blockDevice);
    }
}
