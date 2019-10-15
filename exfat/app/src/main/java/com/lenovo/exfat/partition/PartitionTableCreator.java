package com.lenovo.exfat.partition;

import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;

public interface PartitionTableCreator {
    PartitionTable read(BlockDeviceDriver blockDevice) throws IOException;
    String getName();
}
