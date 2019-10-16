package com.lenovo.exfat.core.io;


import android.util.Log;

import com.lenovo.exfat.core.fs.ExFatFile;
import com.lenovo.exfat.core.fs.ExFatFileSystem;
import com.lenovo.exfat.core.fs.Fat;
import com.lenovo.exfat.core.fs.FatEntry;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;

/**
 * ExFile 输入流
 *
 * @auther xiehui
 * @create 2019-10-15 上午10:42
 */
public class ExFatFileInputStream {
    private static final String TAG = ExFatFileInputStream.class.getSimpleName();
    private ExFatFile exFatFile;
    private long offset;
    private long length;
    private long fileCluster;
    private FatEntry entry;
    private int read;
    public ExFatFileInputStream(ExFatFile exFatFile){
        this.exFatFile = exFatFile;
        offset = ExFatUtil.clusterToOffset(exFatFile.getFileCluster());
        length = exFatFile.length();
        fileCluster = exFatFile.getFileCluster();
        entry = Fat.getFatEntryByCluster(fileCluster);
    }

    /**
     * 读取文件
     *
     * @param buffer
     * @return
     * @throws IOException
     */
    public int read(ExFatBuffer buffer) throws IOException {
        if(exFatFile.isDirectory()){
            throw new IOException("this file is directory !");
        }
        buffer.clear();
        if(read >= length){
            return -1;
        }else{
            ExFatFileSystem.da.read(buffer.getBuffer(),offset);
            if(entry.getNextCluster() == FatEntry.UNDEFINED){
                fileCluster +=1;
                offset  = ExFatUtil.clusterToOffset(exFatFile.getFileCluster());
                entry = Fat.getFatEntryByCluster(fileCluster);
            }else{
                fileCluster = entry.getNextCluster();
                offset  = ExFatUtil.clusterToOffset(exFatFile.getFileCluster());
                entry = Fat.getFatEntryByCluster(fileCluster);
            }
            read = read + ExFatUtil.getBytesPerCluster();
            if(read >= length){
                return (int)(length % ExFatUtil.getBytesPerCluster());
            }
            return ExFatUtil.getBytesPerCluster();
        }
    }
}
