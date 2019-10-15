package com.lenovo.exfat.core;

import android.util.Log;

import com.lenovo.exfat.core.fs.ExFatFileSystem;
import com.lenovo.exfat.core.partition.Partition;
import com.lenovo.exfat.core.partition.PartitionTable;
import com.lenovo.exfat.core.partition.PartitionTableEntry;
import com.lenovo.exfat.core.partition.PartitionTableFactory;
import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.driver.BlockDeviceDriverFactory;
import com.lenovo.exfat.driver.ByteBlockDevice;
import com.lenovo.exfat.driver.scsi.ScsiBlockDevice;
import com.lenovo.exfat.usb.UsbCommunicationInfo;

import java.io.IOException;
import java.util.List;

/**
 * exFAT 文件系统格式
 *
 * -------------------------------------------------------------------------
 * | MBR(512字节)| DBR 及其保留扇区| FAT | 簇位图文件 | 大写字符文件 | 用户数据区|
 * -------------------------------------------------------------------------
 *
 */
public class FileSystemFactory {
    private static final String TAG = FileSystemFactory.class.getSimpleName();
    public static FileSystem createFileSystem(UsbCommunicationInfo usbCommunicationInfo) throws IOException {
        // 开始读取USB设备时，偏移量为0，直接用scsi的方式读取
        ScsiBlockDevice scsiBlockDevice = BlockDeviceDriverFactory.createBlockDevice(usbCommunicationInfo);
        // 1. 读取磁盘前512字节,获取到 MBR 信息, 根据 MBR 信息, 获取分区表.
        Log.i(TAG,"4. 建立分区表");
        PartitionTable partitionTable = PartitionTableFactory.createPartitionTable(scsiBlockDevice);

        // 2. 根据分区表，获取分区信息.
        Log.i(TAG,"5. 建立分区");
        Partition partition = initPartitions(partitionTable,scsiBlockDevice);

        // 3. 根据分区信息，建立文件系统
        Log.i(TAG,"6. 建立文件系统");
        // 找到分区后读取USB设备需要加上偏移量
        ByteBlockDevice byteBlockDevice  = new ByteBlockDevice(scsiBlockDevice,partition.getLogicalOffsetToAdd());
        FileSystem fs = new ExFatFileSystem(byteBlockDevice);

        // 4. 初始化文件系统
        fs.init();


        return fs;
    }

    private static Partition initPartitions(PartitionTable partitionTable, BlockDeviceDriver blockDevice) throws IOException{
        List<PartitionTableEntry> entrys = partitionTable.getPartitionTableEntries();
        for(PartitionTableEntry entry : entrys){
            Partition partition = Partition.createPartition(blockDevice,entry.getLogicalBlockAddress());
            if(partition != null){
                return partition;
            }
        }
        throw new IOException("未获取到分区信息！");
    }
}
