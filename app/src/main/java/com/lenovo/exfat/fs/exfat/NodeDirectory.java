package com.lenovo.exfat.fs.exfat;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 */
public class NodeDirectory{
    private final NodeEntry nodeEntry;
    private final Map<String, NodeEntry> nameToNode;
    private final Map<String, NodeEntry> idToNode;
    private final UpcaseTable upcase;
    private ExFatFileSystem fs;
    public NodeDirectory(ExFatFileSystem fs, NodeEntry nodeEntry)
        throws IOException {
        this(fs, nodeEntry, false);
    }

    public NodeDirectory(ExFatFileSystem fs, NodeEntry nodeEntry, boolean showDeleted)
        throws IOException {

        this.fs =fs;
        this.nodeEntry = nodeEntry;
        this.upcase = fs.getUpcase();
        this.nameToNode = new LinkedHashMap<String, NodeEntry>();
        this.idToNode = new LinkedHashMap<String, NodeEntry>();

        DirectoryParser.
            create(nodeEntry.getNode(), showDeleted).
            setUpcase(this.upcase).
            parse(new VisitorImpl());

    }

    public String getDirectoryId() {
        return Long.toString(nodeEntry.getNode().getStartCluster());
    }

    public Node getNode() {
        return nodeEntry.getNode();
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
            idToNode.put("1", nodeEntry);
        }

    }

}
