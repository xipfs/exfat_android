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
    private static final String TAG = "exfat : "+Fat.class.getSimpleName();

    private static final int SIZE = 512;

    private static List<FatEntry> entries = new ArrayList<>();

    private ExFatFileSystem exFatFileSystem;
    public Fat(ExFatFileSystem exFatFileSystem) {
        this.exFatFileSystem = exFatFileSystem;
    }

    public  void build() throws IOException {

        long offset = ExFatUtil.blockToOffset(Constants.FAT_BLOCK_START);
        Log.i(TAG,Constants.FAT_BLOCK_START+" , "+ Constants.FAT_BLOCK_COUNT+ ","+ExFatUtil.getBlocksPerCluster());
        // 起始扇区簇对齐 并且扇区数目正好整簇
        Log.i(TAG,"Fat offset :"+Long.toHexString(offset));
        final ByteBuffer buffer = ByteBuffer.allocate(SIZE); // 每个扇区512字节
        buffer.order(ByteOrder.LITTLE_ENDIAN); // 小端序
        for(int i = 0 ; i< Constants.FAT_BLOCK_COUNT;i++) {
            exFatFileSystem.da.read(buffer,offset);
            buffer.flip();
            for(int j = 0 ; j < buffer.limit()/Constants.FAT_ENTRY_SIZE; j++){
                byte[] values = new byte[4];
                values[0] = buffer.get();
                values[1] = buffer.get();
                values[2] = buffer.get();
                values[3] = buffer.get();
                FatEntry entry = new FatEntry(byte2long(values));
                entry.setOffset(offset);
                entries.add(entry);
                offset += Constants.FAT_ENTRY_SIZE;
            }
            buffer.clear();
        }
        /*
        if((Constants.FAT_BLOCK_START % ExFatUtil.getBlocksPerCluster()== 0)&&(Constants.FAT_BLOCK_COUNT%ExFatUtil.getBlocksPerCluster() == 0)){
            Log.i(TAG,"Fat  align offset :"+Long.toHexString(offset));
            final ByteBuffer buffer = ByteBuffer.allocate(ExFatUtil.getBytesPerCluster()); // 读一个簇
            buffer.order(ByteOrder.LITTLE_ENDIAN); // 小端序
            for(int i = 0 ; i< Constants.FAT_BLOCK_COUNT/Constants.CLUSTER_BLOCKS;i++) {
                exFatFileSystem.da.read(buffer,offset);
                buffer.flip();
                for(int j = 0 ; j < buffer.limit()/FAT_ENTRY_SIZE; j++){
                    byte[] values = new byte[4];
                    values[0] = buffer.get();
                    values[1] = buffer.get();
                    values[2] = buffer.get();
                    values[3] = buffer.get();
                    FatEntry entry = new FatEntry(byte2long(values));
                    entries.add(entry);
                    offset +=FAT_ENTRY_SIZE;
                }
                buffer.clear();

            }
        }else{

        }*/
    }

    /**
     * 通过簇号获取到FAT项信息，用于判断是不是连续簇
     * @param cluster
     * @return
     */
    public  FatEntry getFatEntryByCluster(long cluster){
        return entries.get((int)cluster);
    }
    private long byte2long(byte[] bytes){
        long i0 = bytes[0] & 0xff;
        long i1 = (bytes[1] & 0xff) << 8;
        long i2 = (bytes[2] & 0xff) << 16;
        long i3 = (bytes[3] & 0xff) << 24;
        return i0 | i1 | i2 | i3;
    }
}
