package com.lenovo.exfat.partition;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.driver.ByteBlockDevice;
import com.lenovo.exfat.fs.FileSystem;
import com.lenovo.exfat.fs.FileSystemFactory;

import java.io.IOException;

public class Partition extends ByteBlockDevice implements BlockDeviceDriver{
    public Partition(BlockDeviceDriver blockDevice,PartitionTableEntry entry){
        super(blockDevice,entry.getLogicalBlockAddress());

    }
    private FileSystem fileSystem;
    private String volumeLabel;
    private static  final String TAG = Partition.class.getSimpleName();
    public static Partition createPartition(PartitionTableEntry entry, BlockDeviceDriver blockDevice){
         try {
             // 创建分区信息 , entry 中包含磁盘的逻辑偏移地址
            Partition partition = new Partition(blockDevice, entry);
            FileSystem fs = FileSystemFactory.createFileSystem(entry, partition);
            Log.i(TAG,"FileSystemName : "+fs.getFileSystemName());
            partition.fileSystem = fs;
            partition.volumeLabel = fs.getVolumeLabel();
            return partition;
        } catch (IOException e) {
             e.printStackTrace();
             Log.w(TAG, "Unsupported fs on partition");
             return null;
        }

    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public String getVolumeLabel() {
        return volumeLabel;
    }
}
