package com.lenovo.exfat.partition;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.driver.ByteBlockDevice;
import com.lenovo.exfat.fs.FileSystem;
import com.lenovo.exfat.fs.FileSystemFactory;
import com.lenovo.exfat.fs.UsbFile;

import java.io.IOException;
import java.nio.ByteBuffer;

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
            fs.buildStructure();
            // Log.i(TAG,fs.getRootDirectory().toString());
            UsbFile file = fs.search("/hello.txt");
            byte[] buffer = new byte[0x1000];
            long position = 0;
            long length = file.getLength();
            while (position < length) {
                 int chunkLength = (int) Math.min(length - position, buffer.length);
                 file.read(position, ByteBuffer.wrap(buffer, 0, chunkLength));
                 position += chunkLength;
            }
            Log.i(TAG, file.toString());
            Log.i(TAG,new String(buffer,0,(int)length));
            fs.delete("/hello.txt");
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
