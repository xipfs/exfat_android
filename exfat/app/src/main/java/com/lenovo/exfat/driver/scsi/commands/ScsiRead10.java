package com.lenovo.exfat.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScsiRead10 extends CommandBlockWrapper{
    private int blockAddress = 0;
    private int transferBytes = 0;
    private int blockSize = 0;
    private short transferBlocks = 0;
    private static final byte LENGTH = 10;
    private static final byte OPCODE = 0x28;
    public ScsiRead10(byte lun){
        super(0, Direction.IN, lun, LENGTH);
    }

    public ScsiRead10(int blockAddress, int transferBytes,int blockSize){
        super(transferBytes, Direction.IN, (byte)0, LENGTH);
        init(blockAddress, transferBytes, blockSize);
    }

    public void init(int blockAddress, int transferBytes, int blockSize) throws IllegalArgumentException{
        super.dCbwDataTransferLength = transferBytes;
        this.blockAddress = blockAddress;
        this.transferBytes = transferBytes;
        this.blockSize = blockSize;
        short transferBlocks = (short) (transferBytes / blockSize);
        if (transferBytes % blockSize != 0) {
            throw new IllegalArgumentException("transfer bytes is not a multiple of block size");
        }
        this.transferBlocks = transferBlocks;
    }

    public void serialize(ByteBuffer buffer) {
        super.serialize(buffer);
        buffer.order(ByteOrder.BIG_ENDIAN).
                put(OPCODE).
                put((byte)0).
                putInt(blockAddress).
                put((byte)0).
                putShort(transferBlocks);
    }
    public String toString(){
        return ("ScsiRead10 [blockAddress=" + blockAddress + ", transferBytes=" + transferBytes
                + ", blockSize=" + blockSize + ", transferBlocks=" + transferBlocks
                + ", getdCbwDataTransferLength()=" + getdCbwDataTransferLength() + "]");
    }


}
