package com.lenovo.exfat.partition.fs;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.driver.ByteBlockDevice;
import com.lenovo.exfat.fs.FileSystemFactory;
import com.lenovo.exfat.partition.PartitionTable;
import com.lenovo.exfat.partition.PartitionTableCreator;

import java.io.IOException;

public class FileSystemPartitionTableCreator implements PartitionTableCreator {
    @Override
    public PartitionTable read(BlockDeviceDriver blockDevice) throws IOException {
        FileSystemPartitionTable table = new FileSystemPartitionTable(blockDevice,
                FileSystemFactory.createFileSystem(null, blockDevice));
        return table;
    }

    @Override
    public String getName() {
        return "FileSystemPartitionTableCreator";
    }
}
