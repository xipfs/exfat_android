package com.lenovo.exfat.driver.scsi.commands;

import java.nio.ByteBuffer;

public class ScsiInquiry extends CommandBlockWrapper {
    private static final byte LENGTH = 0x6;
    private static final byte OPCODE = 0x12;
    private byte allocationLength;
    public ScsiInquiry(byte allocationLength) {
        super(allocationLength,Direction.IN,(byte)0,LENGTH);
        this.allocationLength = allocationLength;
    }
    public void serialize(ByteBuffer buffer) {
        super.serialize(buffer);
        buffer.put(OPCODE).put((byte)0).put((byte)0).put((byte)0).put(allocationLength);
    }

}
