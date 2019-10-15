package com.lenovo.exfat.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * PC机发送的数据必须符合CBW格式（31byte，小端模式），
 * 而 Mass Storage 设备的应答，其格式必须符合 CSW 格式（13byte，小端模式）。
 * 至于中间过程传输的数据，根据不同的命令，格式也有不同地要求。
 */
public abstract class CommandBlockWrapper {
    //在本命令执行期间，主机期望通过Bulk-In或Bulk-Out端点传输的数据长度。
    // 如果为0，则表示这之间没有数据传输。
    protected int dCbwDataTransferLength;
    private Direction direction;
    //表示正在发送命令字的设备的逻辑单元号（LUN）。
    // 对于支持多个LUN的设备，主机设置相对应的LUN值。否则，该值为0。
    private byte bCbwLun;
    //CBWCB的有效字节长度。有效值是在1到16之间。
    private byte bCbwcbLength;
    private int dCbwTag = 0; // 由主机发送的CBW标签 设备应该在相关的CSW的dCSWTag以相同的值应答主机。
    //bmCBWFlags： 定义如下（Bit7 Direction（dCBWDataTransferLength为0时，该值无意义） ：
    //0= DataOut，数据从主机到设备
    //1= DataIn, 数据从设备到主机
    private byte bmCbwFlags = 0;
    //常数 0x43425355，标识为CBW命令块.
    private int D_CBW_SIGNATURE = 0x43425355;

    public CommandBlockWrapper(int dCbwDataTransferLength,
                               Direction direction,
                               byte bCbwLun,
                               byte bCbwcbLength) {
        this.dCbwDataTransferLength = dCbwDataTransferLength;
        this.direction = direction;
        this.bCbwLun = bCbwLun;
        this.bCbwcbLength = bCbwcbLength;
        if (direction == Direction.IN) {
            bmCbwFlags = (byte) 0x80;
        }
    }

    public void serialize(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
                .putInt(D_CBW_SIGNATURE)
                .putInt(dCbwTag)
                .putInt(dCbwDataTransferLength)
                .put(bmCbwFlags)
                .put(bCbwLun)
                .put(bCbwcbLength);
    }

    public int getdCbwTag() {
        return dCbwTag;
    }

    public void setdCbwTag(int dCbwTag) {
        this.dCbwTag = dCbwTag;
    }

    public int getdCbwDataTransferLength() {
        return dCbwDataTransferLength;
    }

    public Direction getDirection() {
        return direction;
    }
}
