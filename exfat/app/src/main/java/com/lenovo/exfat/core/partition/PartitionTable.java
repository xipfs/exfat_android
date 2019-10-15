package com.lenovo.exfat.core.partition;


import java.util.List;

/**
 * 分区表
 *
 * @auther xiehui
 * @create 2019-10-10 上午11:03
 */
public class PartitionTable {
    private List<PartitionTableEntry> partitionTableEntries;

    public PartitionTable(List<PartitionTableEntry> partitionTableEntries){
        this.partitionTableEntries = partitionTableEntries;
    }
    public List<PartitionTableEntry> getPartitionTableEntries(){
        return partitionTableEntries;
    }
}
