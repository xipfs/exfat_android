package com.lenovo.exfat.fs.exfat;

import android.util.Log;

import java.io.IOException;

/**
 * 用户文件目录项
 * @author root
 *
 */

public final class Node {
    private static final String TAG = Node.class.getSimpleName();
    //文件属性
    public static final int ATTRIB_RO = 0x01;		// 00000001 只读
    public static final int ATTRIB_HIDDEN = 0x02;	// 00000010 隐藏
    public static final int ATTRIB_SYSTEM = 0x04;	// 00000100 系统
    public static final int ATTRIB_VOLUME = 0x08;   // 00001000 卷簇
    public static final int ATTRIB_DIR = 0x10;      // 00010000 子目录
    public static final int ATTRIB_ARCH = 0x20;     // 00100000 存档

    private final ExFatSuperBlock sb;
    private final DeviceAccess da;
    private final long startCluster; 	// file首簇号
    private long fileInfoCluster;       // fileInfo cluster
    private final EntryTimes times;		// 文件创建时间

    private boolean isContiguous;
    private int flags;					// 文件属性
    private String name;				// 文件名
    private long size;					// 文件大小
    private boolean deleted;
    private long clusterCount;			// 簇数量
    private int index;
    private long offset;

    private Node(ExFatSuperBlock sb, long startCluster, EntryTimes times) {
        this.sb = sb;
        this.da = sb.getDeviceAccess();
        this.startCluster = startCluster;
        this.times = times;
    }
    
    public static Node createRoot(ExFatSuperBlock sb)
        throws IOException {
        // 根目录首簇号 sb.getRootDirCluster()
        final Node result = new Node(sb, sb.getRootDirCluster(), new EntryTimes(null, null, null));
        result.flags = ATTRIB_DIR;
        result.name  = "/";
        result.clusterCount = result.rootDirSize();
        result.size  = 0;
        result.index = 0;
        return result;
    }

    public static Node create(
        ExFatSuperBlock sb, long startCluster, int flags,
        String name, boolean isContiguous, long size, EntryTimes times, boolean deleted,long fileInfoCluster,long offset) {
        final Node result = new Node(sb, startCluster, times);
        result.name = name;
        result.isContiguous = isContiguous;
        result.size = size;
        result.flags = flags;
        result.deleted = deleted;
        result.fileInfoCluster = fileInfoCluster;
        result.offset = offset;
        return result;
    }

    /**
     * 确定簇中根目录的大小
     *
     * @return the number of clusters for the root directoy
     * @throws IOException on read error
     */
    public long rootDirSize() throws IOException {
        long result = 0;
        long current = this.sb.getRootDirCluster();
        while (!Cluster.invalid(current)) {
            result++;
            current = nextCluster(current);
        }
        return result;
    }

    // 下一个簇
    public long nextCluster(long cluster) throws IOException {
        Cluster.checkValid(cluster);
        Log.i(TAG,"contiguous "+isContiguous);
        if (this.isContiguous) {
            return cluster + 1;
        } else {
            //  
            // 找到用户根目录对应到磁盘上的字节偏移位置
            Log.i(TAG,sb.blockToOffset(this.sb.getFatBlockStart())+"@"+cluster);
            final long fatOffset = sb.blockToOffset(this.sb.getFatBlockStart())+ cluster * 4;
            return this.da.getUint32(fatOffset);
        }
    }

    public int getFlags() {
        return flags;
    }
    public boolean isContiguous() {
        return isContiguous;
    }
    public boolean isDirectory() {
        return ((this.flags & ATTRIB_DIR) != 0);
    }
    public EntryTimes getTimes() {
        return times;
    }
    public ExFatSuperBlock getSuperBlock() {
        return sb;
    }
    public long getStartCluster() {
        return startCluster;
    }
    public long getFileInfoCluster() {
        return fileInfoCluster;
    }
    public String getName() {
        return name;
    }
    public long getSize() {
        return size;
    }
    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean deleted){
        this.deleted = deleted;
    }

    public int getIndex(){
        return index;
    }


    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(Node.class.getName());
        result.append(" [文件名 = ");
        result.append(this.name);
        result.append(", 大小 = ");
        result.append(this.size);
        result.append(", 属性 = ");
        result.append(this.flags);
        result.append(", 时间 = ");
        result.append(this.times.toString());
        result.append("]");
        return result.toString();
    }

}
