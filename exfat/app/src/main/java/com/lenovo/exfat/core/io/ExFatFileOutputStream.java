package com.lenovo.exfat.core.io;


import android.util.Log;

import com.lenovo.exfat.core.fs.AllocationBitmap;
import com.lenovo.exfat.core.fs.DeviceAccess;
import com.lenovo.exfat.core.fs.ExFatFile;
import com.lenovo.exfat.core.fs.ExFatFileSystem;
import com.lenovo.exfat.core.fs.Fat;
import com.lenovo.exfat.core.fs.FatEntry;
import com.lenovo.exfat.core.fs.directory.EntryTimes;
import com.lenovo.exfat.core.fs.directory.ExFatFileEntry;
import com.lenovo.exfat.core.fs.directory.FileNameEntry;
import com.lenovo.exfat.core.fs.directory.StreamExtensionEntry;
import com.lenovo.exfat.core.util.Constants;
import com.lenovo.exfat.core.util.ExFatUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @auther xiehui
 * @create 2019-10-15 上午10:43
 */
public class ExFatFileOutputStream {
    private static final String TAG = ExFatFileOutputStream.class.getSimpleName();
    private ExFatFile exFatFile;
    private long offset;
    private long length;
    private long fileCluster;
    private FatEntry entry;
    private long write;
    private ByteBuffer temp;
    public ExFatFileOutputStream(ExFatFile exFatFile){
        Log.i(TAG,"ExFatFile cluster : "+exFatFile.getFileCluster());
        this.exFatFile = exFatFile;
        offset = ExFatUtil.clusterToOffset(exFatFile.getFileCluster());
        length = exFatFile.length();
        fileCluster = exFatFile.getFileCluster();
        entry = Fat.getFatEntryByCluster(fileCluster);
        temp = ByteBuffer.allocate(ExFatUtil.getBytesPerCluster());
        temp.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * 读取文件
     *
     * @return
     * @throws IOException
     */
    public int write(byte[] data,int length) throws IOException {
        if(exFatFile.isDirectory()){
            throw new IOException("this file is directory !");
        }
        for(int i= 0 ;i<length;i++){
            if(temp.position()<temp.limit()){
                temp.put(data[i]);
                write++;
            }else{
                Log.i(TAG,"write full cluster");
                temp.flip();
                ExFatFileSystem.da.write(temp,offset);
                if(entry.getNextCluster() == FatEntry.UNDEFINED){
                    fileCluster +=1;
                    offset  = ExFatUtil.clusterToOffset(exFatFile.getFileCluster());
                    entry = Fat.getFatEntryByCluster(fileCluster);
                }else{
                    Log.i(TAG,"get free cluster");
                    fileCluster = AllocationBitmap.getNextFreeCluster();
                    offset  = ExFatUtil.clusterToOffset(fileCluster);
                    entry = Fat.getFatEntryByCluster(fileCluster);
                }
                write = write + ExFatUtil.getBytesPerCluster();
                temp.clear();
            }

        }

        return ExFatUtil.getBytesPerCluster();

    }

    public void flush() throws IOException{

        ByteBuffer dataBuffer = ByteBuffer.allocate(temp.position());
        dataBuffer.put(temp.array(), 0,temp.position());
        Log.i(TAG,"str => "+ new String(dataBuffer.array(),0,dataBuffer.limit()));
        Log.i(TAG,"offset => "+Long.toHexString(offset)+" , cluster : "+fileCluster);
        Log.i(TAG,"FatFile second offset ： "+Long.toHexString(exFatFile.getSecondEntry().getOffset()));

        dataBuffer.clear();
        ExFatFileSystem.da.write(dataBuffer,offset);

        Log.i(TAG,"file info offset : "+Long.toHexString(ExFatUtil.clusterToOffset(exFatFile.getFileInfoCluster())));

        // 修改长度
        ByteBuffer buffer = ByteBuffer.allocate(ExFatUtil.getBytesPerCluster());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        ExFatFileSystem.da.read(buffer,ExFatUtil.clusterToOffset(exFatFile.getFileInfoCluster()));

        buffer.position(exFatFile.getSecondEntry().getOffset()+8);
        buffer.putLong(write);
        buffer.position(exFatFile.getSecondEntry().getOffset()+24);
        buffer.putLong(write);

        // 改改校验码
        buffer.position(exFatFile.getFirstEntry().getOffset());
        int checkSum = startChecksum(buffer);
        Log.i(TAG,"start check sum "+Integer.toHexString(checkSum));
        buffer.position(exFatFile.getSecondEntry().getOffset());
        checkSum = addChecksum(checkSum,buffer);
        Log.i(TAG,"second check sum "+Integer.toHexString(checkSum));
        for(FileNameEntry nameEntry: exFatFile.getThirdsEntry()){
            Log.i(TAG,"name offset :"+Long.toHexString(nameEntry.getOffset()));
            buffer.position(nameEntry.getOffset());
            checkSum = addChecksum(checkSum,buffer);
            Log.i(TAG,"third check sum "+Integer.toHexString(checkSum));
        }
        buffer.position(exFatFile.getFirstEntry().getOffset()+2);
        buffer.putShort((short)checkSum);
        Log.i(TAG,"new check sum : "+Integer.toHexString(checkSum));
        buffer.clear();
        ExFatFileSystem.da.write(buffer,ExFatUtil.clusterToOffset(exFatFile.getFileInfoCluster()));

    }

    public static  int startChecksum(ByteBuffer buffer) {
        int result = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Constants.DIR_ENTRY_SIZE; i++) {
            final int b = DeviceAccess.getUint8(buffer);
            sb.append(Integer.toHexString(b)+" ");
            if ((i == 2) || (i == 3)) {
                continue;
            }
            result = ((result << 15) | (result >> 1)) + b;
            result &= 0xffff;
        }
        Log.i(TAG,sb.toString());
        return result;
    }

    public static  int addChecksum(int sum,ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Constants.DIR_ENTRY_SIZE ; i++) {
            final int b = DeviceAccess.getUint8(buffer);
            sb.append(Integer.toHexString(b)+" ");
            sum = ((sum << 15) | (sum >> 1)) + b;
            sum &= 0xffff;
        }
        Log.i(TAG,sb.toString());
        return sum;
    }

}
