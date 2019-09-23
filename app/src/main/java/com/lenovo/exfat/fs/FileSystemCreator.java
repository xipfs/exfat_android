package com.lenovo.exfat.fs;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.partition.PartitionTableEntry;

import java.io.IOException;

public interface FileSystemCreator {
     FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException;
}