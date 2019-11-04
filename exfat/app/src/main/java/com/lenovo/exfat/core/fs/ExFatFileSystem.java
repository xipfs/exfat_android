package com.lenovo.exfat.core.fs;


import android.util.Log;


import com.lenovo.exfat.core.FileSystem;
import com.lenovo.exfat.core.UsbFile;
import com.lenovo.exfat.core.fs.directory.ChildDirectoryParser;
import com.lenovo.exfat.core.fs.directory.RootDirectoryParser;
import com.lenovo.exfat.core.partition.PartitionTypes;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;
import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;

/**
 *
 * exFAT 文件系统格式
 *
 * -------------------------------------------------------------------------
 * | MBR(512字节)| DBR 及其保留扇区| FAT | 簇位图文件 | 大写字符文件 | 用户数据区|
 * -------------------------------------------------------------------------
 *
 * @author xiehui
 * @date 2019-10-23 下午4:04
 */
public class ExFatFileSystem implements FileSystem {
    private static final String TAG ="exfat : "+ExFatFileSystem.class.getSimpleName();


    private BlockDeviceDriver device;
    public  DeviceAccess da;
    // 根文件
    public ExFatFile root;

    public DosBootRecord dbr;
    public UpCaseTable upCaseTable;
    public Fat fat;
    public AllocationBitmap bitmap;
    public RootDirectoryParser rootParser;
    public ChildDirectoryParser childParser;

    public ExFatFileSystem(BlockDeviceDriver blockDevice) {
        this.device = blockDevice;
        Log.i(TAG,device.getClass().getSimpleName());
        this.da = new DeviceAccess(device);
    }

    public static ExFatFileSystem read(BlockDeviceDriver blockDevice) throws IOException {
        ExFatFileSystem exFatFileSystem = new ExFatFileSystem(blockDevice);
        try{
            exFatFileSystem.init();
        }catch (Exception e){
            e.printStackTrace();
            return  null;
        }
        return exFatFileSystem;
    }

    public void init() throws IOException {
        // 1. 建立 DOS Boot Record
        Log.i(TAG,"6.1 Build DosBootRecord ");
        dbr = new DosBootRecord(this);
        dbr.build();
        Log.i(TAG,dbr.print());

        // 2. 建立 FAT
        Log.i(TAG,"6.2 Build FAT ");
        fat = new Fat(this);
        fat.build();

        // 3. 解析用户根目录结构
        Log.i(TAG,"6.3 Build exFAT Root Directory Structure");
        rootParser  = new RootDirectoryParser(this);
        root = rootParser.getRoot();
        rootParser.build();
        Log.i(TAG,root.toString());


        childParser = new ChildDirectoryParser(this);
    }

    @Override
    public UsbFile getRootDirectory() {
        return root;
    }

    @Override
    public String getVolumeLabel() {
        return Constants.DISK_LABEL;
    }

    @Override
    public long getCapacity() {
        return Constants.CLUSTER_COUNT * ExFatUtil.getBytesPerCluster();
    }

    @Override
    public long getOccupiedSpace(){
        try{
            if(bitmap != null){
                long space =  bitmap.getUsedClusterCount()*ExFatUtil.getBytesPerCluster();
                return space;
            }else{
                return 0;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public long getFreeSpace() {
        long free = getCapacity() - getOccupiedSpace();
        return free < 0 ? 0 : free;
    }

    @Override
    public int getChunkSize() {
        return ExFatUtil.getBytesPerCluster();
    }

    @Override
    public int getType() {
        return PartitionTypes.NTFS_EXFAT;
    }
}
