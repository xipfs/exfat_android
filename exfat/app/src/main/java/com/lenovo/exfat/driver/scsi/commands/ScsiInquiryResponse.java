package com.lenovo.exfat.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScsiInquiryResponse {

    private byte peripheralQualifier = 0;
    private byte peripheralDeviceType = 0;
    private boolean isRemovableMedia = false;
    private byte spcVersion = 0;
    private byte responseDataFormat = 0;
    public String toString(){
        return ("ScsiInquiryResponse [peripheralQualifier=" + peripheralQualifier
                + ", peripheralDeviceType=" + peripheralDeviceType + ", removableMedia="
                + isRemovableMedia + ", spcVersion=" + spcVersion + ", responseDataFormat="
                + responseDataFormat + "]");
    }
    public static ScsiInquiryResponse read(ByteBuffer buffer){
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        byte b = buffer.get();
        ScsiInquiryResponse resp = new ScsiInquiryResponse();
        resp.peripheralDeviceType =(byte)(b & 0x1f);
        resp.peripheralQualifier = (byte)(b & 0xe0);
        if(buffer.get() == 0x80){
            resp.isRemovableMedia = true;
        }else{
            resp.isRemovableMedia = false;
        }
        resp.spcVersion = buffer.get();
        resp.responseDataFormat = (byte)(buffer.get() & 0x7);
        return resp;
    }

    public byte getPeripheralQualifier() {
        return peripheralQualifier;
    }

    public byte getPeripheralDeviceType() {
        return peripheralDeviceType;
    }

    public boolean isRemovableMedia() {
        return isRemovableMedia;
    }

    public byte getSpcVersion() {
        return spcVersion;
    }

    public byte getResponseDataFormat() {
        return responseDataFormat;
    }
}
