package com.lenovo.exfat.usb;

import android.content.Context;
import android.hardware.usb.*;
import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.driver.BlockDeviceDriverFactory;
import com.lenovo.exfat.driver.ByteBlockDevice;
import com.lenovo.exfat.driver.scsi.ScsiBlockDevice;
import com.lenovo.exfat.partition.Partition;
import com.lenovo.exfat.partition.PartitionTable;
import com.lenovo.exfat.partition.PartitionTableEntry;
import com.lenovo.exfat.partition.PartitionTableFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class UsbMassStorageDevice{
    private static final String TAG = UsbMassStorageDevice.class.getSimpleName();
    //USB管理器:负责管理USB设备的类
    private UsbManager usbManager;
    //找到的USB设备
    private UsbDevice usbDevice;
    //代表USB设备的一个接口
    private UsbInterface usbInterface;
    private UsbDeviceConnection deviceConnection;
    //代表一个接口的某个节点的类:读数据节点
    private UsbEndpoint inEndpoint;
    //代表一个接口的某个节点的类:写数据节点
    private UsbEndpoint outEndpoint;
    //设备列表
    private HashMap<String, UsbDevice> deviceList;
    private boolean inited = false;

    private final int INTERFACE_SUBCLASS = 6;
    private final int INTERFACE_PROTOCOL = 80;

    // 保存保存连接上 USB 之后的信息
    private UsbCommunicationInfo usbCommunicationInfo;


    private UsbMassStorageDevice(){

    }

    private UsbMassStorageDevice(UsbManager usbManager, UsbDevice usbDevice, UsbInterface usbInterface, UsbEndpoint inEndpoint, UsbEndpoint outEndpoint) {
        this.usbManager = usbManager;
        this.usbDevice = usbDevice;
        this.usbInterface = usbInterface;
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
    }


    public static UsbMassStorageDevice getMassStorageDevices(Context context){
        UsbMassStorageDevice umsd = new UsbMassStorageDevice();
        return umsd.init(context);
    }


    public UsbMassStorageDevice init(Context context){
        // 获取USB设备
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        //获取到设备列表
        deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            usbDevice = deviceIterator.next();
        }
        //获取设备接口
        for (int i = 0; i < usbDevice.getInterfaceCount(); ) {
            // 一般来说一个设备都是一个接口，你可以通过getInterfaceCount()查看接口的个数
            // 这个接口上有两个端点，分别对应OUT 和 IN
            usbInterface = usbDevice.getInterface(i);
            break;
        }

        int endpointCount = usbInterface.getEndpointCount();
        if (endpointCount != 2) {
            Log.w(TAG, "Interface endpoint count != 2");
        }

        for (int j =0 ; j <  endpointCount ;j++) {
            UsbEndpoint endpoint = usbInterface.getEndpoint(j);
            Log.i(TAG, "Found usb endpoint: $endpoint");
            if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                    outEndpoint = endpoint;
                } else {
                    inEndpoint = endpoint;
                }
            }
        }

        if (usbInterface != null) {
            // 判断是否有权限
            if (usbManager.hasPermission(usbDevice)) {
                // 打开设备，获取 UsbDeviceConnection 对象，连接设备，用于后面的通讯
                deviceConnection = usbManager.openDevice(usbDevice);
                if (deviceConnection == null) {
                    return null;
                }
                if (deviceConnection.claimInterface(usbInterface, true)) {
                    Log.i(TAG,"找到设备接口");
                } else {
                    deviceConnection.close();
                }
            } else {
                Log.i(TAG,"没有权限");
            }
        } else {
            Log.i(TAG,"没有找到设备接口！");
        }
        return this;
    }

    public UsbCommunicationInfo createUsbCommunication(){
        UsbCommunication communication = UsbCommunicationFactory.createUsbCommunication(deviceConnection, outEndpoint, inEndpoint);
        byte[] maxLun = new byte[1];
        if(deviceConnection == null ){
            Log.i(TAG,"deviceConnection is null");
        }
        if(usbInterface == null){
            Log.i(TAG,"usbInterface is null");
        }
        deviceConnection.controlTransfer(161, 254, 0, usbInterface.getId(), maxLun, 1, 5000);
        Log.i(TAG, "MAX LUN " + maxLun[0]);
        return new UsbCommunicationInfo(communication,maxLun[0]);
    }
    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    public UsbInterface getUsbInterface() {
        return usbInterface;
    }
}
