package com.lenovo.exfat.driver;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface BlockDeviceDriver {

    void init() throws IOException;
    void read(Long deviceOffset, ByteBuffer buffer) throws  IOException;
    void write(Long deviceOffset, ByteBuffer buffer) throws IOException;
    int getBlockSize();
    long getBlocks() ;
}
