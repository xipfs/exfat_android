package com.lenovo.exfat.driver.scsi.commands;

import java.nio.ByteBuffer;

public class ScsiRequestSense extends CommandBlockWrapper {
    private static byte OPCODE = 0x3;
    private static byte LENGTH = 0x6;

    private byte allocationLength;
    private byte lun;

    public ScsiRequestSense(byte allocationLength,byte lun) {
        super(0, Direction.NONE, lun, LENGTH);
        this.allocationLength = allocationLength;
        this.lun = lun;
    }

    public void serialize(ByteBuffer buffer) {
        super.serialize(buffer);
        buffer.put(OPCODE).put((byte)0).put((byte)0).put((byte)0).put(allocationLength);
    }
}
