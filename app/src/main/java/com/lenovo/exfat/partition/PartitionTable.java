package com.lenovo.exfat.partition;

import java.util.List;

public interface PartitionTable {
    List<PartitionTableEntry> getPartitionTableEntries();
}
