package com.lenovo.exfat.driver;

import com.lenovo.exfat.driver.scsi.ScsiBlockDevice;
import com.lenovo.exfat.usb.UsbCommunication;
import com.lenovo.exfat.usb.UsbCommunicationInfo;

import java.io.IOException;

public class BlockDeviceDriverFactory {
    public static ScsiBlockDevice createBlockDevice(UsbCommunicationInfo usbCommunicationInfo) throws IOException {
        return new ScsiBlockDevice(usbCommunicationInfo.getCommunication(),usbCommunicationInfo.getLun());
    }
}
