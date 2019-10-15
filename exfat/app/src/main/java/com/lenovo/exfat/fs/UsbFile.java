package com.lenovo.exfat.fs;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface UsbFile {
    String separator = "/";
    UsbFile search(String path) throws IOException;
    boolean isDirectory();
    String getName();
    String getAbsolutePath();
    void setName(String newName) throws IOException;
    long createdAt();
    long lastModified();
    long lastAccessed();
    UsbFile getParent();
    String[] list() throws IOException;
    UsbFile[] listFiles() throws IOException;
    long getLength();
    void setLength(long newLength) throws IOException;
    void read(long offset, ByteBuffer destination) throws IOException;
    void write(long offset, ByteBuffer source) throws IOException;
    void flush() throws IOException;
    void close() throws IOException;
    UsbFile createDirectory(String name) throws IOException;
    UsbFile createFile(String name) throws IOException;
    void moveTo(UsbFile destination) throws IOException;
    boolean delete() throws IOException;
    boolean isRoot();
}
