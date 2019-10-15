package com.lenovo.exfat.partition.fs;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.fs.FileSystem;
import com.lenovo.exfat.partition.PartitionTable;
import com.lenovo.exfat.partition.PartitionTableEntry;

import java.util.ArrayList;
import java.util.List;

public class FileSystemPartitionTable implements PartitionTable {
    private static final String TAG = FileSystemPartitionTable.class.getSimpleName();
    private int size = 0;
    private List<PartitionTableEntry> partitionTableEntries = new ArrayList<>();

    public FileSystemPartitionTable(BlockDeviceDriver blockDevice, FileSystem fs){
        Log.i(TAG, "Found a device without partition table, yay!");
        int totalNumberOfSectors =(int)fs.getCapacity() / blockDevice.getBlockSize();
        if (fs.getCapacity() % blockDevice.getBlockSize() != 0L) {
            Log.w(TAG, "fs capacity is not multiple of block size");
        }
        partitionTableEntries.add(new PartitionTableEntry(fs.getType(), 0, totalNumberOfSectors));
    }

    @Override
    public List<PartitionTableEntry> getPartitionTableEntries() {
        return partitionTableEntries;
    }
}
