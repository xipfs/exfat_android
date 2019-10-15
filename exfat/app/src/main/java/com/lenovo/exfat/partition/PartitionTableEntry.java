package com.lenovo.exfat.partition;

import android.util.Log;

public class PartitionTableEntry {
    private int partitionType = 0;
    private int logicalBlockAddress = 0;
    private int totalNumberOfSectors = 0;

    public PartitionTableEntry(int partitionType, int logicalBlockAddress,int totalNumberOfSectors){
        this.partitionType = partitionType;
        this.logicalBlockAddress = logicalBlockAddress;
        this.totalNumberOfSectors = totalNumberOfSectors;
        Log.i("PartitionTableEntry",toString());
    }

    public int getPartitionType() {
        return partitionType;
    }

    public int getLogicalBlockAddress() {
        return logicalBlockAddress;
    }

    public int getTotalNumberOfSectors() {
        return totalNumberOfSectors;
    }

    public String toString(){
        return "[partitionType = "+partitionType+" ,logicalBlockAddress = "+logicalBlockAddress+" , totalNumberOfSectors = "+totalNumberOfSectors+"]";
    }
}
