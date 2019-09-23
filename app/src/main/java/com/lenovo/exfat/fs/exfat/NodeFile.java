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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;


/**
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class NodeFile {

    private final Node node;
    private ExFatFileSystem fs;
    public NodeFile(ExFatFileSystem fs, Node node) {
        this.fs = fs;
        this.node = node;
    }

    public int getLength(){
        return 1024;
    }
    public void read(long offset, ByteBuffer dest) throws IOException {
        final int len = dest.remaining();
        if (len == 0) return;
        if (offset + len > getLength()) {
            throw new EOFException();
        }
        final int bpc = node.getSuperBlock().getBytesPerCluster();
        long cluster = node.getStartCluster();
        int remain = dest.remaining();
        // Skip to the cluster that corresponds to the requested offset
        long clustersToSkip = offset / bpc;
        for (int i = 0; i < clustersToSkip; i++) {
            cluster = this.node.nextCluster(cluster);

            if (Cluster.invalid(cluster)) {
                throw new IOException("invalid cluster");
            }
        }
        // Read in any leading partial cluster
        if (offset % bpc != 0) {
            ByteBuffer tmpBuffer = ByteBuffer.allocate(bpc);
            node.getSuperBlock().readCluster(tmpBuffer, cluster);
            int tmpOffset = (int) (offset % bpc);
            int tmpLength = Math.min(remain, bpc - tmpOffset);
            dest.put(tmpBuffer.array(), tmpOffset, tmpLength);
            remain -= tmpLength;
            cluster = this.node.nextCluster(cluster);
            if (remain != 0 && Cluster.invalid(cluster)) {
                throw new IOException("invalid cluster");
            }
        }

        // Read in the remaining data
        while (remain > 0) {
            int toRead = Math.min(bpc, remain);
            dest.limit(dest.position() + toRead);
            node.getSuperBlock().readCluster(dest, cluster);
            remain -= toRead;
            cluster = this.node.nextCluster(cluster);

            if (remain != 0 && Cluster.invalid(cluster)) {
                throw new IOException("invalid cluster");
            }
        }
    }

}
