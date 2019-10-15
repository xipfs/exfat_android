package com.lenovo.exfat.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScsiReadCapacityResponse {
    private int logicalBlockAddress = 0;
    private int blockLength = 0;
    public static  ScsiReadCapacityResponse read(ByteBuffer buffer){
        buffer.order(ByteOrder.BIG_ENDIAN);
        ScsiReadCapacityResponse resp =  new ScsiReadCapacityResponse();
        resp.logicalBlockAddress = buffer.getInt();
        resp.blockLength = buffer.getInt();
        return resp;
    }

    public int getLogicalBlockAddress() {
        return logicalBlockAddress;
    }

    public int getBlockLength() {
        return blockLength;
    }

    public String toString(){
        return ("ScsiReadCapacityResponse [logicalBlockAddress=" + logicalBlockAddress
                + ", blockLength=" + blockLength + "]");
    }
}
