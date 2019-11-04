package com.lenovo.exfat.core;

import androidx.annotation.Nullable;

import com.lenovo.exfat.core.partition.PartitionTableEntry;
import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;

/**
 * Created by magnusja on 28/02/17.
 */

public interface FileSystemCreator {
    @Nullable FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException;
}
