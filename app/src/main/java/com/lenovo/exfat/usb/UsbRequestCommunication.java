package com.lenovo.exfat.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

public class UsbRequestCommunication implements UsbCommunication {
    private UsbDeviceConnection deviceConnection;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;
    private UsbRequest outRequest;
    private UsbRequest inRequest;
    private ByteBuffer workaroundBuffer = ByteBuffer.allocate(1024 * 32 * 4);

    public UsbRequestCommunication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
        this.deviceConnection = deviceConnection;
        this.outEndpoint = outEndpoint;
        this.inEndpoint = inEndpoint;
        outRequest =new UsbRequest();
        outRequest.initialize(deviceConnection, outEndpoint);
        inRequest = new UsbRequest();
        inRequest.initialize(deviceConnection, inEndpoint);
    }

    @Override
    public int bulkInTransfer(ByteBuffer dest) throws IOException {
        int length = dest.remaining();
        workaroundBuffer.clear();
        workaroundBuffer.limit(length);

        if (!inRequest.queue(workaroundBuffer, length)) {
            throw new IOException("Error queueing request.");
        }

        UsbRequest request = deviceConnection.requestWait();
        if (request == inRequest) {
            workaroundBuffer.flip();
            dest.put(workaroundBuffer);
            return workaroundBuffer.limit();
        }

        throw new IOException("requestWait failed! Request: $request");
    }

    @Override
    public int bulkOutTransfer(ByteBuffer src) throws IOException {
        int length = src.remaining();
        int oldPosition = src.position();
        workaroundBuffer.clear();
        workaroundBuffer.put(src);

        if (!outRequest.queue(workaroundBuffer, length)) {
            throw new IOException("Error queueing request.");
        }

        UsbRequest request = deviceConnection.requestWait();
        if (request == outRequest) {
            src.position(oldPosition + workaroundBuffer.position());
            return workaroundBuffer.position();
        }

        throw new IOException("requestWait failed! Request: $request");
    }
}
