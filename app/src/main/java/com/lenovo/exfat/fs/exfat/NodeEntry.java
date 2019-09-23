package com.lenovo.exfat.fs.exfat;

import java.io.IOException;

/**
 * 
 */
public final class NodeEntry {

    private final Node node;
    private final NodeDirectory parent;
    private ExFatFileSystem fs;
    /**
     * The index of this entry in the parent.
     */
    private int index;

    public NodeEntry(ExFatFileSystem fs, Node node, NodeDirectory parent, int index) {
        this.fs = fs;
        this.node = node;
        this.parent = parent;
        this.index = index;
    }

    /**
     * Gets the node for this entry.
     *
     * @return the node.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Gets the index of this node entry.
     *
     * @return the index.
     */
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
