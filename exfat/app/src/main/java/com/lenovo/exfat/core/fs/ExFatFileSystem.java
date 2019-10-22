package com.lenovo.exfat.core.fs;

import android.util.Log;

import com.lenovo.exfat.core.FileSystem;
import com.lenovo.exfat.core.fs.directory.ChildDirectoryParser;
import com.lenovo.exfat.core.fs.directory.RootDirectoryParser;
import com.lenovo.exfat.core.io.ExFatBuffer;
import com.lenovo.exfat.core.io.ExFatFileInputStream;
import com.lenovo.exfat.core.io.ExFatFileOutputStream;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatCache;
import com.lenovo.exfat.driver.BlockDeviceDriver;

import java.io.FileInputStream;
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

        /*4. 递归解析子目录文件
        Log.i(TAG,"6.4 Build exFAT Child Directory Structure");
        ChildDirectoryParser childParser = new ChildDirectoryParser();
        for(ExFatFile child:root.getChildren()){
            if(child.isDirectory()){
                childParser.build(child);
            }else{
                Log.i(TAG,child.getName()+", cluster "+child.getFileCluster());
            }
        }
        */
    }

    public ExFatFile findFile(String absolutePath) throws IOException{
        String[] paths = absolutePath.split("/");
        if(paths.length ==0 ){ //
            return root;
        }if(paths.length ==2 ){ //
            return root.getCache(paths[1]);
        }else {
            boolean flag = false;
            ExFatFile temp = ExFatFileSystem.root.getCache(paths[1]);
            for(int i = 2; i<paths.length ; i++){
                flag = false;
                Log.i(TAG,"path => "+ paths[i]);
                List<ExFatFile>  files = temp.listFiles();
                for(ExFatFile file : files){
                    if(file.getName().equals(paths[i])){
                        Log.i(TAG,"file => "+ paths[i]);
                        temp = file;
                        flag = true;
                        break;
                    }
                }
                if(flag){
                    continue;
                }else{
                    return  null;
                }
            }
            if(flag){
                return temp;
            }else{
                return null;
            }
        }
    }

    //=============================== test method ==================================

    public void testExist() throws IOException{
        ExFatFile file = new ExFatFile("/123/456/789/hello.txt");
        Log.i(TAG,file.getAbsolutePath()+" , exist : "+file.exists());
       // ExFatFile file2 = new ExFatFile("/abc/a.txt");
       // Log.i(TAG,file.getAbsolutePath()+" , exist : "+file2.exists());
    }

    public void testListFile() throws IOException{
        List<ExFatFile> files = root.listFiles();
        if(files != null ){
            for(ExFatFile file : files){
                Log.i(TAG,"find file "+file.getName());
                List<ExFatFile> files2 = file.listFiles();
                if(files2 != null){
                    for(ExFatFile file2 : files2){
                        Log.i(TAG,"find file "+file2.getName());
                        List<ExFatFile> files3 = file2.listFiles();
                        if(files3 != null){
                            for(ExFatFile file3 : files3){
                                Log.i(TAG,"find file "+file3.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    public void testFindFile() throws IOException{
        ExFatFile file = findFile("/123/456/789/hello.txt");
        if(file !=null ){
            Log.i(TAG,"find file "+file.getName().equals("hello.txt"));
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
        ExFatFile file = new ExFatFile("/helloworldthisislongfilename");
        file.mkdir();
        ExFatFile file02 = new ExFatFile("/hello.txt");
        file02.createNewFile();
    }

    public void testMkdir() throws IOException{
        //ExFatFile file = new ExFatFile("/hello/world");
        //file.mkdir();
        ExFatFile file2 = new ExFatFile("/hello/1.txt");
        file2.createNewFile();
    }
    public void testWriteFile() throws IOException{
        ExFatFile file = findFile("/hello/1.txt");
        ExFatFileOutputStream out = new ExFatFileOutputStream(file);

        String str = "this is test write file";
        Log.i(TAG,"str length -> "+str.getBytes().length);
        out.write(str.getBytes(),str.getBytes().length);
        out.flush();

    }

}
