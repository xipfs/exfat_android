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

import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;


/**
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class NodeFile {
    private final Node node;
    private ExFatFileSystem fs;
    private static final String TAG = NodeFile.class.getSimpleName();
    public NodeFile(ExFatFileSystem fs, Node node) {
        this.fs = fs;
        this.node = node;
    }

    public String getName() {
        return node.getName();
    }

    public long getLength(){
        return node.getSize();
    }

    public boolean delete(String fileName) throws IOException{
        long cluster = node.getFileInfoCluster();
        Log.i(TAG,"cluster : "+cluster+" , rootDirCluster :"+node.getSuperBlock().getRootDirCluster());
        if(cluster == node.getSuperBlock().getRootDirCluster()){
            return deleteRootFile(fileName);
        }else{
            return deleteChildFile();
        }
    }


    public boolean deleteRootFile(String fileName) throws IOException{
        Node rootNode = fs.getRootEntry().getNode();
        // 通过 根目录首簇号 计算偏移 然后跳转到文件分配表
        DirectoryParser.
                create(rootNode, false).
                setUpcase(fs.getUpcase()).setClusterBitMap(fs.getClusterBitmap()).
                parse(new VisitorImpl(),true,fileName);
        return true;
    }

    public boolean deleteChildFile() throws IOException{
        // 先将文件元数据设置为删除状态
        Log.i(TAG,node.getName()+" "+node.getFileInfoCluster()+" "+node.getFlags());
        final int bpc = node.getSuperBlock().getBytesPerCluster();
        ClusterBitMap cb = fs.getClusterBitmap();
        long cluster = node.getFileInfoCluster();
        Log.i(TAG,"root dir cluster :"+node.getSuperBlock().getRootDirCluster());
        try {
            Log.i(TAG,"cluster : "+cluster+" , is free : "+cb.isClusterFree(cluster));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteBuffer chunk = ByteBuffer.allocate(bpc);
        chunk.order(ByteOrder.LITTLE_ENDIAN);
        try {
            node.getSuperBlock().readCluster(chunk, cluster);
            chunk.rewind();
            /*
            StringBuilder sb = new StringBuilder();
            byte[] bs = chunk.array();
            for(byte bb: bs) {
                sb.append(bb).append(" ");
            }
            Log.d("delete block", sb.toString());
            */
            int type = DeviceAccess.getUint8(chunk); // 85H
            Log.i(TAG,Integer.toHexString(type));

            chunk.position(chunk.position()-1);
            chunk.put((byte)0x05);

            int conts = DeviceAccess.getUint8(chunk);
            final int referenceChecksum = DeviceAccess.getUint16(chunk);
            final int attrib = DeviceAccess.getUint16(chunk);
            skip(chunk,2);
            final EntryTimes times = EntryTimes.read(chunk);
            skip(chunk,7);
            Log.i(TAG,"chunk.remaining : "+chunk.remaining());
            if (chunk.remaining() == 0) {
                node.getSuperBlock().writeCluster(chunk,cluster); // write delete flag;
                cb.freeCluster(cluster);
                cluster = node.nextCluster(cluster);
                try {
                    Log.i(TAG,"cluster "+cluster+" , is free"+cb.isClusterFree(cluster));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Cluster.invalid(cluster)) {
                    return false;
                }
                chunk.rewind();
                node.getSuperBlock().readCluster(chunk, cluster);
                chunk.rewind();
            }
            int fileInfo = DeviceAccess.getUint8(chunk); // C0H
            Log.i(TAG,Integer.toHexString(fileInfo));

            chunk.position(chunk.position()-1);
            chunk.put((byte)0x40);

            final int flag = DeviceAccess.getUint8(chunk);
            skip(chunk,1);
            int nameLen = DeviceAccess.getUint8(chunk);
            final int nameHash = DeviceAccess.getUint16(chunk);
            skip(chunk,2);
            final long realSize = DeviceAccess.getUint64(chunk);
            skip(chunk,4);
            final long startCluster = DeviceAccess.getUint32(chunk);
            final long size = DeviceAccess.getUint64(chunk);
            conts--;
            final StringBuilder nameBuilder = new StringBuilder(nameLen);
            while (conts-- > 0) {
                Log.i(TAG,"chunk.remaining2 : "+chunk.remaining());
                if (chunk.remaining() == 0) {
                    node.getSuperBlock().writeCluster(chunk,cluster); // write
                    cb.freeCluster(cluster);
                    cluster = node.nextCluster(cluster);
                    try {
                        Log.i(TAG,"cluster "+cluster+" , is free"+cb.isClusterFree(cluster));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (Cluster.invalid(cluster)) {
                        return false;
                    }
                    chunk.rewind();
                    node.getSuperBlock().readCluster(chunk, cluster);
                    chunk.rewind();
                }
                int file_name=DeviceAccess.getUint8(chunk); // C1H
                Log.i(TAG,Integer.toHexString(file_name));

                chunk.position(chunk.position()-1);
                chunk.put((byte)0x41);
                node.getSuperBlock().writeCluster(chunk,cluster);
                cb.freeCluster(cluster);
            }

            // 设置文件内容为删除状态
            cluster = node.getStartCluster();
            cb.freeCluster(cluster);
            long clustersToSkip = node.getSize() / bpc;
            for (int i = 0; i < clustersToSkip; i++) {
                cluster = this.node.nextCluster(cluster);
                cb.freeCluster(cluster);
                if (Cluster.invalid(cluster)) {
                    throw new IOException("invalid cluster");
                }
            }

            node.setDeleted(true);
            return true;
        }catch(IOException e){
            e.printStackTrace();
        }
        return false;
    }
    private void skip(ByteBuffer chunk,int bytes) {
        chunk.position(chunk.position() + bytes);
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

    private class VisitorImpl implements DirectoryParser.Visitor {
        @Override
        public void foundLabel(String label) {

        }

        @Override
        public void foundBitmap(
                long startCluster, long size) {
        }

        @Override
        public void foundUpcaseTable(DirectoryParser parser, long checksum,
                                     long startCluster, long size) {
        }

        @Override
        public void foundNode(Node node, int index) throws IOException {
        }

    }

}
