package com.lenovo.exfat.fs;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.fs.exfat.ExFatFileSystemCreator;
import com.lenovo.exfat.partition.PartitionTableEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class FileSystemFactory {
    public static class UnsupportedFileSystemException extends IOException {

    }
    public static final String TAG=FileSystemFactory.class.getSimpleName();
    private static List<FileSystemCreator> fileSystems = new ArrayList<>();
    private static TimeZone timeZone = TimeZone.getDefault();

    static {
        FileSystemFactory.registerFileSystem(new ExFatFileSystemCreator());
    }

    public static FileSystem createFileSystem(PartitionTableEntry entry,
                                              BlockDeviceDriver blockDevice) throws IOException, UnsupportedFileSystemException {
        for(FileSystemCreator creator : fileSystems) {
            Log.i(TAG,creator.getClass().getSimpleName());
            FileSystem fs = creator.read(entry, blockDevice);
            if(fs != null) {
                return fs;
            }
        }

        throw new UnsupportedFileSystemException();
    }
    public static synchronized void registerFileSystem(FileSystemCreator creator) {
        fileSystems.add(creator);
    }
    public static void setTimeZone(TimeZone zone) {
        timeZone = zone;
    }

    public static TimeZone getTimeZone() {
        return timeZone;
    }
}
