package com.lenovo.exfat.partition;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.partition.fs.FileSystemPartitionTableCreator;
import com.lenovo.exfat.partition.mbr.MasterBootRecordCreator;

import java.io.IOException;
import java.util.ArrayList;

public class PartitionTableFactory {
    private static final String TAG = PartitionTableFactory.class.getSimpleName();
    private static ArrayList<PartitionTableCreator> partitionTables = new ArrayList<>();
    static {
        registerPartitionTable(new MasterBootRecordCreator());
        //registerPartitionTable(new FileSystemPartitionTableCreator());
    }
    public PartitionTableFactory(){

    }

    public static PartitionTable createPartitionTable(BlockDeviceDriver blockDevice) throws IOException {
        for (PartitionTableCreator creator : partitionTables) {
            Log.i(TAG,creator.getName());
            PartitionTable table = creator.read(blockDevice);
            if (table != null) {
                return table;
            }
        }
        throw new IOException("获取分区表信息失败！");
    }

    public synchronized static void  registerPartitionTable(PartitionTableCreator creator) {
        partitionTables.add(creator);
    }
}
