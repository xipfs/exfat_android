package com.lenovo.exfat.driver.scsi.commands;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CommandStatusWrapper {
    private int dCswSignature = 0;
    private int dCswTag = 0;
    private int dCswDataResidue = 0;
    private byte bCswStatus =  0;
    public static int COMMAND_PASSED = 0;
    public static int COMMAND_FAILED = 1;
    private int PHASE_ERROR = 2;
    public static int SIZE = 13;
    private final String TAG = CommandStatusWrapper.class.getSimpleName();
    //常数0x53425355，标识为CSW状态块
    private int D_CSW_SIGNATURE = 0x53425355;
    public void read(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        dCswSignature = buffer.getInt();
        if (dCswSignature != D_CSW_SIGNATURE) {
            Log.e(TAG, "unexpected dCSWSignature $dCswSignature");
        }
        dCswTag = buffer.getInt();
        dCswDataResidue = buffer.getInt();
        bCswStatus = buffer.get();
    }

    public int getdCswTag(){
        return dCswTag;
    }
    public int getdCswDataResidue(){
        return dCswDataResidue;
    }
    public byte getbCswStatus(){
        return bCswStatus;
    }
}
