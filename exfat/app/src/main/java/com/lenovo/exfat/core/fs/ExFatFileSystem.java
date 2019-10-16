package com.lenovo.exfat.core.fs;

import android.util.Log;

import com.lenovo.exfat.core.FileSystem;
import com.lenovo.exfat.core.fs.directory.ChildDirectoryParser;
import com.lenovo.exfat.core.fs.directory.RootDirectoryParser;
import com.lenovo.exfat.core.io.ExFatBuffer;
import com.lenovo.exfat.core.io.ExFatFileInputStream;
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
    public  static  DeviceAccess da;            // 通过此类对设备进行读写

    // 根文件
    public static ExFatFile root;

    public ExFatFileSystem(BlockDeviceDriver device) {
        this.device = device;
        da = new DeviceAccess(device);
    }

    public void init() throws IOException {
        // 1. 建立 DOS Boot Record
        Log.i(TAG,"6.1 Build DosBootRecord ");
        DosBootRecord.build();
        Log.i(TAG,DosBootRecord.print());

        // 2. 建立 FAT
        Log.i(TAG,"6.2 Build FAT ");
        Fat.build();

        // 3. 解析用户根目录结构
        Log.i(TAG,"6.3 Build exFAT Root Directory Structure");
        RootDirectoryParser rootParser  = new RootDirectoryParser();
        root = rootParser.build();
        Log.i(TAG,root.toString());

        //4. 递归解析子目录文件
        Log.i(TAG,"6.4 Build exFAT Child Directory Structure");
        ChildDirectoryParser childParser = new ChildDirectoryParser();
        for(ExFatFile child:root.getChildren()){
            if(child.isDirectory()){
                childParser.build(child);
            }else{
                Log.i(TAG,child.getName()+", cluster "+child.getFileCluster());
            }
        }

        //9. test find read delete file
        //testFindFile();
        //testReadFile();
        //testDeleteFile();
        testCreateFile();
        //testWriteFile();

    }

    public ExFatFile findFile(String path) {
        if(path == null || path.equals("/") || path.trim().equals("")){
            return root;
        }else{
            return root.find(path);
        }
    }
    public void testFindFile() throws IOException{
        ExFatFile file = findFile("/hello");
        if(file !=null ){
            Log.i(TAG,"find file "+file.getName().equals("hello"));
            Log.i(TAG,"file length = "+file.length());
            long cluster = file.getFileCluster();
            FatEntry fatEntry = Fat.getFatEntryByCluster(cluster);
            long nextCluster = fatEntry.getNextCluster();
            Log.i(TAG,"Cluster "+ Long.toHexString(cluster)+" is free "+AllocationBitmap.isClusterFree(cluster));
            Log.i(TAG,"NextCLuster "+Long.toHexString(nextCluster)+" is free "+AllocationBitmap.isClusterFree(cluster));
        }
    }


    public void testReadFile() throws IOException{
        ExFatFile file = findFile("/123/456/hello.txt");
        //10. test read file
        ExFatFileInputStream input = new ExFatFileInputStream(file);
        ExFatBuffer buffer = new ExFatBuffer();
        byte[] data = new byte[(int)file.length()];
        int offset =0;
        int read = -1;
        while((read = input.read(buffer)) != -1 ){
            Log.i(TAG,"read = "+read);
            buffer.getBuffer().flip();
            buffer.getBuffer().get(data,offset,read);
            offset = offset+read;

        }
        Log.i(TAG,new String(data));
    }
    public void testDeleteFile() throws IOException{
        ExFatFile file = findFile("/123/456/hello.txt");
        //10. test delete file
        Log.i(TAG,"start  delete  file : "+file.getName());
        file.delete();
    }

    public void testCreateFile() throws IOException{
        ExFatFile file = new ExFatFile("/hello");
        file.mkdir();
    }

    public void testWriteFile(){

    }

}
