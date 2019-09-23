package com.lenovo.exfat.driver.scsi.commands;

import java.nio.ByteBuffer;

public class ScsiTestUnitReady extends CommandBlockWrapper {

    private static byte LENGTH = 0x6;
    private static byte OPCODE = 0x0;
    public ScsiTestUnitReady(byte lun) {
        super(0, Direction.NONE, lun, LENGTH);
    }

    public void serialize(ByteBuffer buffer) {
        super.serialize(buffer);
        buffer.put(OPCODE);
    }
}
