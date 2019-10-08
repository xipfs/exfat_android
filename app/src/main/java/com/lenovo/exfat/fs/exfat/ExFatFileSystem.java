package com.lenovo.exfat.fs.exfat;

import android.util.Log;

import com.lenovo.exfat.driver.BlockDeviceDriver;
import com.lenovo.exfat.fs.FileSystem;
import com.lenovo.exfat.fs.UsbFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
    public static HashMap<String,NodeEntry> entryMap = new HashMap<>();
    public static HashMap<String,ExFatUsbFile> files = new HashMap<>();
    private String label;
    private ExFatSuperBlock sb;
    private UpcaseTable upcase;
    private ClusterBitMap bitmap;
    private BlockDeviceDriver device;
    private Node rootNode;
    private ExFatUsbFile rootFile;
    private NodeEntry rootEntry;


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
        DirectoryParser.create(rootNode).parse(rootDirVis,false);
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

    public UsbFile createFile(String path){

        return null;
    }
    public UsbFile mkdir(String path){

        return null;
    }

    public UsbFile search(String path){
        return files.get(path);
    }

    public boolean delete(String path){
        UsbFile file = search(path);
        if(file == null){
            return false;
        }else{
            try {
                file.delete();
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void buildStructure() throws IOException{
        NodeEntry entry = getRootEntry();
        buildStructure(entry,null,"");
    }

    public void buildStructure(NodeEntry entry,ExFatUsbFile parentFile,String parentPath) throws IOException {
        if (entry.isFile()) {
            NodeFile nodeFile = entry.getFile();
            ExFatUsbFile exFatFile = new ExFatUsbFile(this);
            exFatFile.isDirectory(false);
            exFatFile.isRoot(false);
            exFatFile.setName(entry.getName());
            exFatFile.setLength(entry.getNode().getSize());
            exFatFile.setAbsolutePath(parentPath+entry.getName());
            parentFile.addChild(exFatFile);
            entryMap.put(exFatFile.getAbsolutePath(),entry);
            files.put(exFatFile.getAbsolutePath(),exFatFile);
            // Log.i(TAG,exFatFile.toString());
        } else {
            ExFatUsbFile exFatFile = new ExFatUsbFile(this);
            exFatFile.isDirectory(true);
            // get directory
            NodeDirectory nodeDir = entry.getDirectory();
            if(parentFile == null) {
                rootFile = exFatFile;
                exFatFile.setName("/");
                exFatFile.setAbsolutePath("/");
                exFatFile.isRoot(true);
                exFatFile.setLength(entry.getNode().getSize());
            }else {
                exFatFile.isRoot(false);
                exFatFile.setName(entry.getName());
                exFatFile.setAbsolutePath(parentPath+entry.getName()+"/");
                exFatFile.setLength(entry.getNode().getSize());
                parentFile.addChild(exFatFile);
            }
            entryMap.put(exFatFile.getAbsolutePath(),entry);
            files.put(exFatFile.getAbsolutePath(),exFatFile);
            //Log.i(TAG,exFatFile.toString());
            Iterator<? extends NodeEntry> iterator = nodeDir.iterator();
            while (iterator.hasNext()) {
                NodeEntry child = iterator.next();
                if (".".equals(child.getName()) || "..".equals(child.getName())) {
                    continue;
                }
                buildStructure(child,exFatFile,exFatFile.getAbsolutePath());
            }
        }
    }

    public NodeEntry getRootEntry() throws IOException {
        if (rootEntry == null) {
            rootEntry = createRootEntry();
        }
        return rootEntry;
    }
    public NodeEntry createRootEntry() throws IOException {
        return new NodeEntry(this, rootNode, null, 2);
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
    public UsbFile getRootDirectory() { return rootFile; }
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
    private static class RootDirVisitor implements DirectoryParser.Visitor {
        private final ExFatSuperBlock sb;
        private ClusterBitMap bitmap; // 簇位图
        private UpcaseTable upcase;   // 大写字符表
        private String label;         // 标签
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
            parser.setUpcase(this.upcase);
        }

        @Override
        public void foundNode(Node node, int index) throws IOException {
            /*Log.i(TAG,node.toString());
            ExFatUsbFile file = new ExFatUsbFile();
            file.setName(node.getName());
            file.setLength(node.getSize());
            file.setAccessedTime(node.getTimes().getAccessed().getTime());
            file.setCreateTime(node.getTimes().getCreated().getTime());
            file.setModifiedTime(node.getTimes().getModified().getTime());
            file.setAbsolutePath("/"+node.getName());
            file.isDirectory(node.isDirectory());
            rootFile.addChild(file);*/
            //Log.i(TAG,"FileSystem name : "+node.toString()+" index : "+index+", start cluster : "+node.getStartCluster());
        }

    }
}
