package com.lenovo.exfat.core.io;


import com.lenovo.exfat.core.util.ExFatUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ExFat 缓冲区
 *
 * @auther xiehui
 * @create 2019-10-15 上午11:04
 */
public class ExFatBuffer {
    private ByteBuffer buffer;

    public ExFatBuffer(){
        buffer = ByteBuffer.allocate(ExFatUtil.getBytesPerCluster());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public ExFatBuffer(ByteBuffer buffer){
        this.buffer = buffer;
    }

    public void clear(){
        buffer.clear();
    }

    public void flip(){
        buffer.flip();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

}
