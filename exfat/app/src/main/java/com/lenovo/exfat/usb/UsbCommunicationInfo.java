package com.lenovo.exfat.usb;

/**
 * 保存 usb 连接成功后的信息
 */
public class UsbCommunicationInfo {

    private UsbCommunication communication;
    private byte lun;

    public UsbCommunicationInfo(UsbCommunication communication, byte lun) {
        this.communication = communication;
        this.lun = lun;
    }

    public UsbCommunication getCommunication() {
        return communication;
    }

    public void setCommunication(UsbCommunication communication) {
        this.communication = communication;
    }

    public byte getLun() {
        return lun;
    }

    public void setLun(byte lun) {
        this.lun = lun;
    }
}
