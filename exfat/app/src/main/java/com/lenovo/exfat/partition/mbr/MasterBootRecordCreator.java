package com.lenovo.exfat.partition.mbr;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.partition.PartitionTable;
import com.lenovo.exfat.partition.PartitionTableCreator;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MasterBootRecordCreator implements PartitionTableCreator {
    private static final String TAG=MasterBootRecord.class.getSimpleName();
    @Override
    public PartitionTable read(BlockDeviceDriver blockDevice) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Math.max(512, blockDevice.getBlockSize()));
        blockDevice.read(0l, buffer);
        return MasterBootRecord.read(buffer);
    }

    @Override
    public String getName() {
        return "MasterBootRecordCreator";
    }
}
