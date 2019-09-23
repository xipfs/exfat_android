package com.lenovo.exfat.driver;

import com.lenovo.exfat.driver.scsi.ScsiBlockDevice;
import com.lenovo.exfat.usb.UsbCommunication;

import java.io.IOException;

public class BlockDeviceDriverFactory {
    public static ScsiBlockDevice createBlockDevice(UsbCommunication usbCommunication, byte lun) throws IOException {
        return new ScsiBlockDevice(usbCommunication, lun);
    }
}
