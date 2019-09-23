package com.lenovo.exfat.fs.exfat;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.fs.FileSystem;
import com.lenovo.exfat.fs.FileSystemCreator;
import com.lenovo.exfat.partition.PartitionTableEntry;

import java.io.IOException;

public class ExFatFileSystemCreator  implements FileSystemCreator {
    private static final String TAG= ExFatFileSystemCreator.class.getSimpleName();
    public FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException {
        Log.i(TAG,"ExFatFileSystemCreator blocksize "+blockDevice.getBlockSize());
        ExFatFileSystem fileSystem = new ExFatFileSystem(blockDevice,false);
        fileSystem.init();
        return fileSystem;
    }
}