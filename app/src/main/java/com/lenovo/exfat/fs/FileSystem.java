package com.lenovo.exfat.fs;

import java.io.IOException;

public interface FileSystem {
    String getFileSystemName();
    UsbFile getRootDirectory();
    String getVolumeLabel();
    long getCapacity();
    long getOccupiedSpace();
    long getFreeSpace();
    int getChunkSize();
    int getType();
    void buildStructure() throws IOException;
    UsbFile search(String path);
    UsbFile createFile(String path) throws IOException;
    UsbFile mkdir(String path) throws IOException;
    boolean delete(String path);
}
