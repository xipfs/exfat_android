package com.lenovo.exfat.core.partition.mbr;



import android.util.Log;

import com.lenovo.exfat.core.partition.PartitionTable;
import com.lenovo.exfat.core.partition.PartitionTableEntry;
import com.lenovo.exfat.core.partition.PartitionTypes;
import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *                      MBR 结构
 *  ----------------------------------------------------------
 *  |         地址       |                     |              |
 *  | ------------------|      描述            |  长度(字节)   |
 *  | Hex |  Oct |  Dec |                      |              |
 *  ---------------------------------------------------------|
 *  |0000 | 0000 |   0  |     代码区            |  440 最大446 |
 *  |--------------------------------------------------------|
 *  |01B8 | 0670 |  440 |   选用磁盘标志         |     4       |
 *  |---------------------------------------------------------|
 *  |01BC | 0674 | 444  | 一般为空值 0x0000      |      2       |                                  |
 *  |---------------------------------------------------------|
 *  |01BE | 0676 | 446  | MBR 分区表规划         |   64        |
 *  |     |      |      | 4个16byte的主分区表入口 |             |
 *  |----------------------------------------------------------
 *  |01FE | 0776 | 510  | 55h |                 |             |
 *  |-------------------------| MBR 有效标志：   |      2      |
 *  |01FF | 0777 | 511  | AAh |       0x55AA    |             |
 *  |---------------------------------------------------------|
 *  |   MBR 总大小：446+64+2 =                   |   512       |
 *  |----------------------------------------------------
 *
 *
 * @auther xiehui
 * @create 2019-10-10 上午11:19
 */
public class MasterBootRecord{
    private static final String TAG = MasterBootRecord.class.getSimpleName();
    private static final int TABLE_OFFSET = 446;
    private static final int TABLE_ENTRY_SIZE = 16;
    private static final int MBR_SIZE =512;
    private static HashMap<Integer, Integer> partitionTypes = new HashMap<>();
    private ArrayList<PartitionTableEntry> partitionTableEntries = new ArrayList<>();
    public MasterBootRecord(){
        partitionTypes.put(0x0b, PartitionTypes.FAT32);
        partitionTypes.put(0x0c, PartitionTypes.FAT32);
        partitionTypes.put(0x1b, PartitionTypes.FAT32);
        partitionTypes.put(0x1c, PartitionTypes.FAT32);
        partitionTypes.put(0x01, PartitionTypes.FAT12);
        partitionTypes.put(0x04, PartitionTypes.FAT16);
        partitionTypes.put(0x06, PartitionTypes.FAT16);
        partitionTypes.put(0x0e, PartitionTypes.FAT16);
        partitionTypes.put(0x83, PartitionTypes.LINUX_EXT);
        partitionTypes.put(0x07, PartitionTypes.NTFS_EXFAT);
        partitionTypes.put(0xaf, PartitionTypes.APPLE_HFS_HFS_PLUS);
    }

    public PartitionTable read(BlockDeviceDriver blockDevice) throws IOException {
        Log.i(TAG,"获取分区表信息开始！");
        ByteBuffer buffer = ByteBuffer.allocate(Math.max(MBR_SIZE, blockDevice.getBlockSize()));
        blockDevice.read(0l, buffer);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.limit() < 512) {
            throw new IOException("Size mismatch!");
        }
        if (buffer.get(510) != (byte)0x55 || buffer.get(511) != (byte)0xaa) {
            Log.i(TAG, "not a valid mbr partition table !");
            throw new IOException("not a valid mbr partition table !");
        }
        int size = 4;
        for (int i = 0; i< size ; i++) {
            int offset = TABLE_OFFSET + i * TABLE_ENTRY_SIZE;
            // 第5字节 分区类型符
            byte partitionType = buffer.get(offset + 4);
            if (partitionType == 0)
                continue;
            if (partitionType == 0x05 || partitionType == 0x0f) {
                Log.w(TAG, "extended partitions are currently unsupported!");
                continue;
            }
            Integer type = partitionTypes.get(partitionType & 0xff);

            if (type == null) {
                Log.d(TAG, "Unknown partition type$partitionType");
                type = PartitionTypes.UNKNOWN;
                throw new IOException("Unknown partition type$partitionType !");
            }
            // 第9、10、11、12字节  逻辑起始扇区号 ，本分区之前已用了的扇区数
            // 第13、14、15、16字节 本分区的总扇区数
            PartitionTableEntry entry = new PartitionTableEntry(type, buffer.getInt(offset + 8), buffer.getInt(offset + 12));
            partitionTableEntries.add(entry);
        }
        Log.i(TAG,"获取分区表信息结束！");
        PartitionTable table = new PartitionTable(partitionTableEntries);
        return table;
    }
}
