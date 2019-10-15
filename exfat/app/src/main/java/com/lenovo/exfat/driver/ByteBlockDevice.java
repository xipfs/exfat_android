package com.lenovo.exfat.driver;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBlockDevice implements  BlockDeviceDriver{
    public static final String TAG = ByteBlockDevice.class.getSimpleName();
    private BlockDeviceDriver targetBlockDevice;
    private int logicalOffsetToAdd;

    private int blockSize;
    private long blocks;

    public ByteBlockDevice(BlockDeviceDriver targetBlockDevice,int logicalOffsetToAdd){
        this.targetBlockDevice = targetBlockDevice;
        this.logicalOffsetToAdd = logicalOffsetToAdd;
        this.blockSize = targetBlockDevice.getBlockSize();
        this.blocks = targetBlockDevice.getBlocks();
    }

    public int getBlockSize() {
        return this.blockSize;
    }

    public long getBlocks() {
        return this.blocks;
    }

    @Override
    public void init() throws IOException {
        Log.i(TAG,"ByteBlockDevice init");
    }

    @Override
    public void read(Long byteOffset, ByteBuffer dest) throws IOException {
        long devOffset = byteOffset / blockSize + logicalOffsetToAdd;
        if (byteOffset % blockSize != 0L) {
            ByteBuffer tmp = ByteBuffer.allocate(blockSize);
            targetBlockDevice.read(devOffset, tmp);
            tmp.clear();
            tmp.position((int)(byteOffset % blockSize));
            int limit = Math.min(dest.remaining(), tmp.remaining());
            tmp.limit(tmp.position() + limit);
            dest.put(tmp);
            devOffset++;
        }
        if (dest.remaining() > 0) {
            ByteBuffer buffer;
            if (dest.remaining() % blockSize != 0) {
                int rounded = blockSize - dest.remaining() % blockSize + dest.remaining();
                buffer = ByteBuffer.allocate(rounded);
                buffer.limit(rounded);
            } else {
                buffer = dest;
            }
            targetBlockDevice.read(devOffset, buffer);
            if (dest.remaining() % blockSize != 0) {
                System.arraycopy(buffer.array(), 0, dest.array(), dest.position(), dest.remaining());
            }

            dest.position(dest.limit());
        }
    }

    @Override
    public void write(Long byteOffset, ByteBuffer src) throws IOException {
        long devOffset = byteOffset / blockSize + logicalOffsetToAdd;
        if (byteOffset % blockSize != 0L) {
            ByteBuffer tmp = ByteBuffer.allocate(blockSize);
            targetBlockDevice.read(devOffset, tmp);
            tmp.clear();
            tmp.position((int)(byteOffset % blockSize));
            int remaining = Math.min(tmp.remaining(), src.remaining());
            tmp.put(src.array(), src.position(), remaining);
            src.position(src.position() + remaining);
            tmp.clear();
            targetBlockDevice.write(devOffset, tmp);
            devOffset++;
        }

        if (src.remaining() > 0) {
            ByteBuffer buffer;
            if (src.remaining() % blockSize != 0) {
                int rounded = blockSize - src.remaining() % blockSize + src.remaining();
                buffer = ByteBuffer.allocate(rounded);
                buffer.limit(rounded);
                System.arraycopy(src.array(), src.position(), buffer.array(), 0, src.remaining());
                src.position(src.limit());
            } else {
                buffer = src;
            }
            targetBlockDevice.write(devOffset, buffer);
        }
    }
}
