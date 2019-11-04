package com.lenovo.exfat.core.fs;


import androidx.annotation.Nullable;

import com.lenovo.exfat.core.FileSystem;
import com.lenovo.exfat.core.FileSystemCreator;
import com.lenovo.exfat.core.partition.PartitionTableEntry;
import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;

/**
 * @author xiehui
 * @date 2019-10-23 下午4:02
 */
public class ExFatFileSystemCreator implements FileSystemCreator {
    @Nullable
    @Override
    public FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException {
        return ExFatFileSystem.read(blockDevice);
    }
}
