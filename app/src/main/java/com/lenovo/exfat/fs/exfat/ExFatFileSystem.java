package com.lenovo.exfat.fs.exfat;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.fs.FileSystem;
import com.lenovo.exfat.fs.UsbFile;

import java.io.IOException;

/**
 * exFAT 文件系统格式
 * 
 * ---------------------------------------------------------
 * | DBR 及其保留扇区| FAT | 簇位图文件 | 大写字符文件 | 用户数据区|
 * ---------------------------------------------------------
 * 
 */
public final class ExFatFileSystem implements FileSystem {
    private static final String TAG = ExFatFileSystem.class.getSimpleName();
    private String label;
    private ExFatSuperBlock sb;
    private UpcaseTable upcase;
    private ClusterBitMap bitmap;
    private BlockDeviceDriver device;
    private Node rootNode;

    public ExFatFileSystem(BlockDeviceDriver device, boolean readOnly) throws IOException {
        try {
            this.device = device;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() throws IOException{
        sb = new ExFatSuperBlock(device);
        sb.read();
        Log.i(TAG,sb.toString());
        rootNode = Node.createRoot(sb);
        final RootDirVisitor rootDirVis = new RootDirVisitor(sb);
        // 通过 根目录首簇号 计算偏移 然后跳转到文件分配表
        DirectoryParser.create(rootNode).parse(rootDirVis);
        if (rootDirVis.bitmap == null) {
            throw new IOException("cluster bitmap not found");
        }
        if (rootDirVis.upcase == null) {
            throw new IOException("upcase table not found");
        }
        this.label = rootDirVis.label;
        Log.i(TAG,"Label : "+rootDirVis.label);

        this.bitmap = rootDirVis.bitmap;
        Log.i(TAG,bitmap.toString());

        this.upcase = rootDirVis.upcase;
        Log.i(TAG,upcase.toString());

    }


    private static class RootDirVisitor implements DirectoryParser.Visitor {
        private final ExFatSuperBlock sb;
        private ClusterBitMap bitmap; // 簇位图
        private UpcaseTable upcase;
        private String label;

        private RootDirVisitor(ExFatSuperBlock sb) {
            this.sb = sb;
        }
        @Override
        public void foundLabel(String label) {
            this.label = label;
        }

        @Override
        public void foundBitmap(
                long startCluster, long size) throws IOException {
            if (this.bitmap != null) {
                throw new IOException("already had a bitmap");
            }
            this.bitmap = ClusterBitMap.read(this.sb, startCluster, size);
        }

        @Override
        public void foundUpcaseTable(DirectoryParser parser, long startCluster, long size,
                                     long checksum) throws IOException {

            if (this.upcase != null) {
                throw new IOException("already had an upcase table");
            }

            this.upcase = UpcaseTable.read(
                    this.sb, startCluster, size, checksum);

            /* the parser may use this table for file names to come */
            parser.setUpcase(this.upcase);
        }

        @Override
        public void foundNode(Node node, int index) {
            /* ignore */
        }

    }

    public BlockDeviceDriver getDevice(){
        return device;
    }

    public UpcaseTable getUpcase() {
        return upcase;
    }

    public ExFatSuperBlock getSuperBlock() {
        return sb;
    }

    public ClusterBitMap getClusterBitmap() {
        return bitmap;
    }

    @Override
    public String getFileSystemName() {
        return "exFat";
    }
    @Override
    public UsbFile getRootDirectory() {
        return null;
    }
    @Override
    public String getVolumeLabel() {
        return label;
    }
    @Override
    public long getCapacity() {
        return 0;
    }
    @Override
    public long getOccupiedSpace() {
        return 0;
    }
    @Override
    public long getFreeSpace() {
        return 0;
    }
    @Override
    public int getChunkSize() {
        return 0;
    }
    @Override
    public int getType() {
        return 0;
    }

}
