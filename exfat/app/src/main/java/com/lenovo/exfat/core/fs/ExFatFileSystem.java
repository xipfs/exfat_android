package com.lenovo.exfat.core.fs;

import android.util.Log;

import com.lenovo.exfat.core.FileSystem;
import com.lenovo.exfat.core.fs.directory.ChildDirectoryParser;
import com.lenovo.exfat.core.fs.directory.RootDirectoryParser;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.IOException;
import java.util.List;

/**
 * exFAT 文件系统格式
 *
 * -------------------------------------------------------------------------
 * | MBR(512字节)| DBR 及其保留扇区| FAT | 簇位图文件 | 大写字符文件 | 用户数据区|
 * -------------------------------------------------------------------------
 *
 */

public class ExFatFileSystem implements FileSystem {
    private static final String TAG = ExFatFileSystem.class.getSimpleName();
    private BlockDeviceDriver device;   // 只是保存作用，尽量不使用它.
    private DeviceAccess da;            // 通过此类对设备进行读写
    private DosBootRecord dbr;          // DOS引导记录
    private Fat fat;                    // 文件分配表
    private AllocationBitmap bitmap;    // 簇位图
    private UpCaseTable upCaseTable;    // 大写字符表


    // 根文件
    private ExFatFile root;

    public ExFatFileSystem(BlockDeviceDriver device) {
        this.device = device;
        da = new DeviceAccess(device);
    }

    public void init() throws IOException {
        // 1. 建立 DOS Boot Record
        Log.i(TAG,"6.1 Build DosBootRecord ");
        dbr = new DosBootRecord(da);
        dbr.build();
        Log.i(TAG,dbr.toString());

        // 2. 建立 FAT
        Log.i(TAG,"6.2 Build FAT ");
        fat = new Fat(da);
        fat.build();

        // 3. 解析用户根目录结构
        Log.i(TAG,"6.3 Build exFAT Root Directory Structure");
        bitmap = new AllocationBitmap(da);
        upCaseTable = new UpCaseTable(da);
        RootDirectoryParser rootParser  = new RootDirectoryParser(da,dbr,fat,bitmap,upCaseTable);
        root = rootParser.build();
        Log.i(TAG,root.toString());

        //4. 递归解析子目录文件
        Log.i(TAG,"6.4 Build exFAT Child Directory Structure");
        ChildDirectoryParser childParser = new ChildDirectoryParser(da,fat);
        for(ExFatFile child:root.getChildren()){
            if(child.isDirectory()){
                childParser.build(child);
            }else{
                Log.i(TAG,child.getName()+", cluster "+child.getFileCluster());
            }
        }


        //9. test
        ExFatFile file = findFile("/123/456/hello.txt");
        if(file !=null ){
            Log.i(TAG,"find file "+file.getName().equals("hello.txt"));
        }


    }

    public ExFatFile findFile(String path) {
        if(path == null || path.equals("/") || path.trim().equals("")){
            return root;
        }else{
            return root.findChild(path);
        }
    }


    
}
