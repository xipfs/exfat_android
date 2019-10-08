package com.lenovo.exfat.fs.exfat;

import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 */
public class NodeDirectory{
    public static final String TAG = NodeDirectory.class.getSimpleName();
    private final NodeEntry nodeEntry;
    private final Map<String, NodeEntry> nameToNode;
    private final Map<String, NodeEntry> idToNode;
    private final UpcaseTable upcase;
    private ExFatFileSystem fs;
    private String name;
    public NodeDirectory(ExFatFileSystem fs, NodeEntry nodeEntry)
            throws IOException {
        this(fs, nodeEntry, false);
    }

    public NodeDirectory(ExFatFileSystem fs, NodeEntry nodeEntry, boolean showDeleted)
            throws IOException {
        this.fs = fs;
        this.nodeEntry = nodeEntry;
        this.upcase = fs.getUpcase();
        this.nameToNode = new LinkedHashMap<String, NodeEntry>();
        this.idToNode = new LinkedHashMap<String, NodeEntry>();

        Log.i(TAG,"Director Parse ");
        DirectoryParser.
                create(nodeEntry.getNode(), showDeleted).
                setUpcase(this.upcase).
                parse(new VisitorImpl(),false);

    }
    public String getDirectoryId() {
        return Long.toString(nodeEntry.getNode().getStartCluster());
    }
    public String getName() {
        return nodeEntry.getName();
    }


    public NodeEntry getEntry(String name) throws IOException {
        return this.nameToNode.get(upcase.toUpperCase(name));
    }

    public NodeEntry getEntryById(String id) throws IOException {
        NodeEntry nodeEntry = idToNode.get(id);

        if (nodeEntry != null) {
            return nodeEntry;
        }

        throw new IOException("Failed to find entry with ID:" + id);
    }
    public Iterator<NodeEntry> iterator() {
        return Collections.<NodeEntry>unmodifiableCollection(
                idToNode.values()).iterator();
    }
    public NodeEntry addFile(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeEntry addDirectory(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    public void remove(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void flush() throws IOException {
        /* nothing to do */
    }

    /**
     * Gets the node associated with this directory.
     *
     * @return the node.
     */
    public Node getNode() {
        return nodeEntry.getNode();
    }

    public NodeDirectory getParent() {
        return nodeEntry.getParent();
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
            final String upcaseName = upcase.toUpperCase(node.getName());
            NodeEntry nodeEntry = new NodeEntry(fs, node, NodeDirectory.this, index);
            nameToNode.put(upcaseName, nodeEntry);
            idToNode.put(nodeEntry.getId(), nodeEntry);
            // Log.i(TAG,"Node Directory name : "+node.toString()+" index : "+index+", start cluster : "+node.getStartCluster());
        }

    }

}
