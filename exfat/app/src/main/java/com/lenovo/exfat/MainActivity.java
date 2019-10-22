package com.lenovo.exfat;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.lenovo.exfat.core.FileSystemFactory;
import com.lenovo.exfat.core.FileSystem;
import com.lenovo.exfat.core.fs.ExFatFileSystem;
import com.lenovo.exfat.usb.UsbCommunicationInfo;
import com.lenovo.exfat.usb.UsbMassStorageDevice;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "com.lenovo.exfat.USB_PERMISSION";
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private UsbMassStorageDevice massStorageDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent);
            String action = intent.getAction();
            if (action == null)
                return;
            switch (action) {
                case ACTION_USB_PERMISSION://用户授权广播
                    synchronized (this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) { //允许权限申请
                            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                            Log.i(TAG,"开始访问USB设备");
                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                if (device != null) {
                                    try {
                                        setupDevice();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            Log.i(TAG,"用户未授权，访问USB设备失败");
                        }
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED://USB设备插入广播
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.i(TAG,"USB设备插入");
                    if (device != null) {
                        discoverDevice();
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED://USB设备拔出广播
                    Log.i(TAG,"USB设备拔出");
                    break;
            }

        }
    };

    private void init(){
        Log.i(TAG,"注册USB设备监听事件");
        //USB管理器
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        //注册广播,监听USB插入和拔出
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, intentFilter);
        //读写权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 111);
        }
    }

    public void discoverDevice(){
        Log.i(TAG,"discoverDevice");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        massStorageDevice = UsbMassStorageDevice.getMassStorageDevices(this);
        if (massStorageDevice == null) {
            Log.w(TAG, "未发现设备");
            return;
        }
        UsbDevice usbDevice = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
            Log.d(TAG, "received usb device via intent");
            massStorageDevice.createUsbCommunication();
        } else {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                    ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(massStorageDevice.getUsbDevice(), permissionIntent);
        }
    }

    private void setupDevice() throws IOException {
        // 获取设备
        Log.i(TAG,"1. 获取存储设备");
        massStorageDevice = UsbMassStorageDevice.getMassStorageDevices(this);
        // 建立连接
        Log.i(TAG,"2. 与存储设备建立连接");
        UsbCommunicationInfo usbCommunicationInfo = massStorageDevice.createUsbCommunication();
        // 创建文件系统
        Log.i(TAG,"3. 开始创建文件系统");
        ExFatFileSystem fs = (ExFatFileSystem)FileSystemFactory.createFileSystem(usbCommunicationInfo);

        //9. test find read delete file

        //fs.testExist();
        //fs.testListFile();
        //fs.testFindFile();
        //fs.testReadFile();
        //fs.testDeleteFile();
        //fs.testCreateFile();
        //fs.testMkdir();
        fs.testWriteFile();
    }



    public static String fSize(long sizeInByte) {
        if (sizeInByte < 1024)
            return String.format("%s", sizeInByte);
        else if (sizeInByte < 1024 * 1024)
            return String.format(Locale.CANADA, "%.2fKB", sizeInByte / 1024.);
        else if (sizeInByte < 1024 * 1024 * 1024)
            return String.format(Locale.CANADA, "%.2fMB", sizeInByte / 1024. / 1024);
        else
            return String.format(Locale.CANADA, "%.2fGB", sizeInByte / 1024. / 1024 / 1024);
    }
}
