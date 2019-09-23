package com.lenovo.exfat.driver.scsi.commands;

import java.nio.ByteBuffer;

public class ScsiReadCapacity extends CommandBlockWrapper {

    private static int RESPONSE_LENGTH = 0x8;
    private static byte LENGTH = 0x10;
    private static byte OPCODE = 0x25;

    public ScsiReadCapacity(byte lun) {
       super(RESPONSE_LENGTH, Direction.IN, lun, LENGTH);
    }

    public void serialize(ByteBuffer buffer) {
        super.serialize(buffer);
        buffer.put(OPCODE);
    }
}
