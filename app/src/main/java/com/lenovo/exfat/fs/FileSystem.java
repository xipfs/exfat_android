package com.lenovo.exfat.fs;

public interface FileSystem {
    String getFileSystemName();
    UsbFile getRootDirectory();
    String getVolumeLabel();
    long getCapacity();
    long getOccupiedSpace();
    long getFreeSpace();
    int getChunkSize();
    int getType();
}
