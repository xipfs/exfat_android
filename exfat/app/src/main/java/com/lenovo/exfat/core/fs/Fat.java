package com.lenovo.exfat.core.fs;


import android.util.Log;

import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件分配表
 *
 * File Allocation Table (FAT) may contain 1 or 2 FATs, as defined in NumberOfFats field.
 * ActiveFat field in VolumeFlags in the Main Boot Sector determines which FAT is active.
 * The first cluster is cluster 2, as in FAT32. Each FatEntry represents one cluster.
 * In exFAT, FAT is not used for tracking an allocation;
 * an Allocation Bitmap is used for this purpose.
 * FAT is only used for keeping chains of clusters of fragmented files.
 * If a file is not fragmented, FAT table does not need to be updated.
 * A Stream Extensions Directory Entry should be consulted to determine if the FAT chain is valid or not.
 * If FAT chain is not valid, it does not need to be zeroed.
 *
 *
 * @auther xiehui
 * @create 2019-10-10 下午4:59
 */
public class Fat {
    private static final String TAG = Fat.class.getSimpleName();

    private static final int SIZE = 512;
    private static final int FAT_ENTRY_SIZE =4;

    private static List<FatEntry> entries = new ArrayList<>();

    public Fat() {
    }

    public static void build() throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(SIZE); // 每个扇区512字节
        buffer.order(ByteOrder.LITTLE_ENDIAN); // 小端序
        long offset = ExFatUtil.blockToOffset(Constants.FAT_BLOCK_START);
        Log.i(TAG,"Fat offset :"+Long.toHexString(offset));
        for(int i = 0 ; i< Constants.FAT_BLOCK_COUNT;i++) {
            ExFatFileSystem.da.read(buffer,offset);
            buffer.flip();
            for(int j = 0 ; j < buffer.limit()/FAT_ENTRY_SIZE; j++){
                byte[] values = new byte[4];
                values[0] = buffer.get();
                values[1] = buffer.get();
                values[2] = buffer.get();
                values[3] = buffer.get();
                FatEntry entry = new FatEntry(byte2long(values));
                entries.add(entry);
            }
            buffer.clear();
            offset +=SIZE;
        }

    }

    /**
     * 通过簇号获取到FAT项信息，用于判断是不是连续簇
     * @param cluster
     * @return
     */
    public static FatEntry getFatEntryByCluster(long cluster){
        return entries.get((int)cluster);
    }
    private static long byte2long(byte[] bytes){
        long i0 = bytes[0] & 0xff;
        long i1 = (bytes[1] & 0xff) << 8;
        long i2 = (bytes[2] & 0xff) << 16;
        long i3 = (bytes[3] & 0xff) << 24;
        return i0 | i1 | i2 | i3;
    }
}
