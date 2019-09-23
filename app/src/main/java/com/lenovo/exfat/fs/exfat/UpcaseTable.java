/*
 * $Id$
 *
 * Copyright (C) 2003-2015 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package com.lenovo.exfat.fs.exfat;

import java.io.IOException;

/**
 * TODO: Add support for compressed format:
 * http://www.ntfs.com/exfat-upcase-table.htm
 *
 * exFAT 文件系统格式
 * 
 * ---------------------------------------------------------
 * | DBR 及其保留扇区| FAT | 簇位图文件 | 大写字符文件 | 用户数据区|
 * ---------------------------------------------------------
 * 
 * 大写字符文件
 */
public final class UpcaseTable {
    private final ExFatSuperBlock sb;
    private final long size;
    private final long chars;
    private final long offset;  //偏移
    private final DeviceAccess da;
    public static UpcaseTable read(ExFatSuperBlock sb,
                                   long startCluster, long size, long checksum) throws IOException {

    	// 验证簇
        Cluster.checkValid(startCluster);
        /* 验证大小 */
        if ((size == 0) || (size > (0xffff * 2)) || (size % 2) != 0) {
            throw new IOException("bad upcase table size " + size);
        }
        final UpcaseTable result = new UpcaseTable(sb,
            sb.clusterToOffset(startCluster), size);
        /* 验证校验码 */
        final long actualCs = result.checkSum();
        if (checksum != actualCs) {
            final StringBuilder msg = new StringBuilder();
            msg.append("checksum mismatch (expected 0x");
            msg.append(Long.toHexString(checksum));
            msg.append(", got 0x");
            msg.append(Long.toHexString(actualCs));
            msg.append(")");

            throw new IOException(msg.toString());
        }
        return result;
    }

    private UpcaseTable(ExFatSuperBlock sb, long offset, long size) {
        this.sb = sb;
        this.da = sb.getDeviceAccess();
        this.size = size;
        this.chars = size / 2;
        this.offset = offset;
    }
    public long getOffset() {
        return offset;
    }
    public long getSize() {
        return size;
    }
    public long checkSum() throws IOException {
        long sum = 0;
        for (int i = 0; i < size; i++) {
            sum = ((sum << 31) | (sum >> 1)) + da.getUint8(offset + i);
            sum &= 0xffffffffl;
        }
        return sum;
    }

    public char toUpperCase(char c) throws IOException {
        if (c > this.chars) {
            return c;
        } else {
            return da.getChar(offset + (c * 2));
        }
    }

    public String toUpperCase(String s) throws IOException {
        final StringBuilder result = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            result.append(toUpperCase(c));
        }
        return result.toString();
    }

    public String toString(){
        final StringBuilder result = new StringBuilder();
        result.append(UpcaseTable.class.getSimpleName());
        result.append(" [偏移位置 = ");
        result.append(this.offset);
        result.append(", table 大小 = ");
        result.append(this.size);
        result.append("]");

        return result.toString();
    }

    public long getCharCount() {
        return this.chars;
    }

	public ExFatSuperBlock getSb() {
		return sb;
	}
    
}
