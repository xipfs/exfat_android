package com.lenovo.exfat.fs.exfat;

import java.io.IOException;

/**
 * 
 */
public final class NodeEntry {

    //文件属性
    public static final int ATTRIB_RO = 0x01;		// 00000001 只读
    public static final int ATTRIB_HIDDEN = 0x02;	// 00000010 隐藏
    public static final int ATTRIB_SYSTEM = 0x04;	// 00000100 系统
    public static final int ATTRIB_VOLUME = 0x08;   // 00001000 卷簇
    public static final int ATTRIB_DIR = 0x10;      // 00010000 子目录
    public static final int ATTRIB_ARCH = 0x20;     // 00100000 存档
    private final Node node;
    private final NodeDirectory parent;
    private ExFatFileSystem fs;
    private int index;

    public NodeEntry(ExFatFileSystem fs, Node node, NodeDirectory parent, int index) {
        this.fs = fs;
        this.node = node;
        this.parent = parent;
        this.index = index;
    }

    public String getId() {
        return Integer.toString(index);
    }

    public String getName() {
        return node.getName();
    }

    public NodeDirectory getParent() {
        return parent;
    }

    public long getLastModified() throws IOException {
        return node.getTimes().getModified() == null ? 0 : node.getTimes().getModified().getTime();
    }

    public long getCreated() throws IOException {
        return node.getTimes().getCreated() == null ? 0 : node.getTimes().getCreated().getTime();
    }

    public long getLastAccessed() throws IOException {
        return node.getTimes().getAccessed() == null ? 0 : node.getTimes().getAccessed().getTime();
    }

    public boolean isFile() {
        return (!this.node.isDirectory());
    }

    public boolean isDirectory() {
        return this.node.isDirectory();
    }

    public void setName(String newName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setLastModified(long lastModified) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeFile getFile() throws IOException {
        if (!isFile()) {
            throw new UnsupportedOperationException("not a file");
        }
        return new NodeFile(fs, this.node);
    }

    public NodeDirectory getDirectory() throws IOException {
        if (!isDirectory()) {
            throw new UnsupportedOperationException("not a directory");
        }
        return new NodeDirectory(fs, this);
    }


    public boolean isDirty() {
        return false;
    }

    public Node getNode() {
        return node;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(NodeEntry.class.getName());
        sb.append(" [node=");
        sb.append(this.node);
        sb.append(", parent=");
        sb.append(this.parent);
        sb.append("]");
        return sb.toString();
    }
}
