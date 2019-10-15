package com.lenovo.exfat.driver.scsi;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.driver.scsi.commands.CommandBlockWrapper;
import com.lenovo.exfat.driver.scsi.commands.CommandStatusWrapper;
import com.lenovo.exfat.driver.scsi.commands.Direction;
import com.lenovo.exfat.driver.scsi.commands.ScsiInquiry;
import com.lenovo.exfat.driver.scsi.commands.ScsiInquiryResponse;
import com.lenovo.exfat.driver.scsi.commands.ScsiRead10;
import com.lenovo.exfat.driver.scsi.commands.ScsiReadCapacity;
import com.lenovo.exfat.driver.scsi.commands.ScsiReadCapacityResponse;
import com.lenovo.exfat.driver.scsi.commands.ScsiTestUnitReady;
import com.lenovo.exfat.driver.scsi.commands.ScsiWrite10;
import com.lenovo.exfat.usb.UsbCommunication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ScsiBlockDevice implements BlockDeviceDriver {
    private String TAG = ScsiBlockDevice.class.getSimpleName();
    private ByteBuffer outBuffer = ByteBuffer.allocate(31);
    private ByteBuffer cswBuffer = ByteBuffer.allocate(CommandStatusWrapper.SIZE);

    private int blockSize = 0;
    private int lastBlockAddress = 0;

    private ScsiWrite10 writeCommand;
    private ScsiRead10 readCommand;
    private CommandStatusWrapper csw =new CommandStatusWrapper();
    private long blocks = lastBlockAddress;

    private UsbCommunication usbCommunication;
    private byte lun;

    public ScsiBlockDevice(UsbCommunication usbCommunication, byte lun) throws IOException {
        this.usbCommunication = usbCommunication;
        this.lun = lun;
        writeCommand = new ScsiWrite10(lun);
        readCommand = new ScsiRead10(lun);
        init();
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public long getBlocks() {
        return blocks;
    }

    public void init() throws  IOException{
        ByteBuffer inBuffer = ByteBuffer.allocate(36);
        ScsiInquiry inquiry = new ScsiInquiry((byte)(inBuffer.array().length));
        transferCommand(inquiry, inBuffer);
        inBuffer.clear();
        ScsiInquiryResponse inquiryResponse = ScsiInquiryResponse.read(inBuffer);
        Log.d(TAG, "inquiry response: "+inquiryResponse.toString());

        if (inquiryResponse.getPeripheralQualifier() != 0 || inquiryResponse.getPeripheralDeviceType() != 0) {
            throw new IOException("unsupported PeripheralQualifier or PeripheralDeviceType");
        }

        ScsiTestUnitReady testUnit = new ScsiTestUnitReady(lun);
        if (!transferCommand(testUnit, ByteBuffer.allocate(0))) {
            Log.e(TAG, "unit not ready!");
            throw new IOException("Device is not ready (Unsuccessful ScsiTestUnitReady Csw status)");
        }


        ScsiReadCapacity readCapacity = new ScsiReadCapacity(lun);
        inBuffer.clear();
        transferCommand(readCapacity, inBuffer);
        inBuffer.clear();
        ScsiReadCapacityResponse readCapacityResponse = ScsiReadCapacityResponse.read(inBuffer);
        Log.d(TAG, "readCapacity response: "+readCapacityResponse.toString());
        blockSize = readCapacityResponse.getBlockLength();
        lastBlockAddress = readCapacityResponse.getLogicalBlockAddress();
        blocks = lastBlockAddress;
    }

    private Boolean transferCommand(CommandBlockWrapper command, ByteBuffer inBuffer)throws IOException {
        byte[] outArray = outBuffer.array();
        Arrays.fill(outArray, (byte)0);
        outBuffer.clear();
        command.serialize(outBuffer);
        outBuffer.clear();
        int written = usbCommunication.bulkOutTransfer(outBuffer);
        if (written != outArray.length) {
            throw new IOException("Writing all bytes on command $command failed!");
        }
        int transferLength = command.getdCbwDataTransferLength();
        int read = 0;
        if (transferLength > 0) {

            if (command.getDirection() == Direction.IN) {
                do {
                    read += usbCommunication.bulkInTransfer(inBuffer);
                } while (read < transferLength);

                if (read != transferLength) {
                    throw new IOException("Unexpected command size (" + read + ") on response to "
                            + command);
                }
            } else {
                written = 0;
                do {
                    written += usbCommunication.bulkOutTransfer(inBuffer);
                } while (written < transferLength);

                if (written != transferLength) {
                    throw new IOException("Could not write all bytes: $command");
                }
            }
        }

        cswBuffer.clear();
        read = usbCommunication.bulkInTransfer(cswBuffer);
        if (read != CommandStatusWrapper.SIZE) {
            throw new IOException("Unexpected command size while expecting csw");
        }
        cswBuffer.clear();

        csw.read(cswBuffer);
        if (csw.getbCswStatus() != CommandStatusWrapper.COMMAND_PASSED) {
            throw new IOException("Unsuccessful Csw status: " + csw.getbCswStatus());
        }

        if (csw.getdCswTag() != command.getdCbwTag()) {
            throw new IOException("wrong csw tag!");
        }

        return csw.getbCswStatus() == CommandStatusWrapper.COMMAND_PASSED;
    }

    public synchronized void read(Long devOffset, ByteBuffer dest) throws IOException{
        if (dest.remaining() % blockSize != 0) {
            throw new IllegalArgumentException("dest.remaining() must be multiple of blockSize!");
        }
        readCommand.init(devOffset.intValue(), dest.remaining(), blockSize);
        transferCommand(readCommand, dest);
        dest.position(dest.limit());
    }

    public synchronized void write(Long devOffset,ByteBuffer src) throws IOException{
        if (src.remaining() % blockSize != 0) {
            throw new IllegalArgumentException("src.remaining() must be multiple of blockSize!");
        }
        writeCommand.init(devOffset.intValue(), src.remaining(), blockSize);
        transferCommand(writeCommand, src);
        src.position(src.limit());
    }
}
