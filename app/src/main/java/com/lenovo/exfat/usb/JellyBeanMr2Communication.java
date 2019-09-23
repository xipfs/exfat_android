package com.lenovo.exfat.usb;

import android.annotation.TargetApi;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellyBeanMr2Communication implements UsbCommunication {
    private static final String TAG = JellyBeanMr2Communication.class.getSimpleName();
    private UsbDeviceConnection deviceConnection;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;

    public JellyBeanMr2Communication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
        this.deviceConnection = deviceConnection;
        this.outEndpoint = outEndpoint;
        this.inEndpoint = inEndpoint;
    }

    @Override
    public int bulkInTransfer(ByteBuffer dest) throws IOException {
        int result = deviceConnection.bulkTransfer(inEndpoint,
                dest.array(), dest.position(), dest.remaining(), UsbCommunication.TRANSFER_TIMEOUT);

        if (result == -1) {
            throw new IOException("从设备读取失败！");
        }

        dest.position(dest.position() + result);
        return result;
    }

    @Override
    public int bulkOutTransfer(ByteBuffer src) throws IOException {
        int result = deviceConnection.bulkTransfer(outEndpoint,
                src.array(), src.position(), src.remaining(), UsbCommunication.TRANSFER_TIMEOUT);

        if (result == -1) {
            throw new IOException("向设备写入失败！");
        }

        src.position(src.position() + result);
        return result;
    }
}
