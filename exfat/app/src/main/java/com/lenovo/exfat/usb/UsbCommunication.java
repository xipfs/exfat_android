package com.lenovo.exfat.usb;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface UsbCommunication {
    public static final int TRANSFER_TIMEOUT = 5000;
    public int bulkInTransfer(ByteBuffer dest) throws IOException;
    public int bulkOutTransfer(ByteBuffer src)throws IOException;
}
