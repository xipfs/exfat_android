package com.lenovo.exfat.fs.exfat;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class DirectoryParser {
    public static final String TAG = DirectoryParser.class.getSimpleName();
    private static final int ENTRY_SIZE = 32;
    private static final int ENAME_MAX_LEN = 15;
    private static final int VALID = 0x80;
    private static final int CONTINUED = 0x40;

    /**
     * If this bit is not set it means "critical", if it is set "benign".
     */
    private static final int IMPORTANCE_MASK = 0x20;

    private static final int EOD = (0x00);
    private static final int BITMAP = (0x01 | VALID);
    private static final int UPCASE = (0x02 | VALID);
    private static final int LABEL = (0x03 | VALID);
    private static final int FILE = (0x05);
    private static final int FILE_INFO = (0x00 | CONTINUED);
    private static final int FILE_NAME = (0x01 | CONTINUED);

    private static final int FLAG_FRAGMENTED = 1;
    private static final int FLAG_CONTIGUOUS = 3;

    public static DirectoryParser create(Node node) throws IOException {
        return create(node, false);
    }

    public static DirectoryParser create(Node node, boolean showDeleted) throws IOException {
        final DirectoryParser result = new DirectoryParser(node, showDeleted);
        result.init();
        return result;
    }

    private final ExFatSuperBlock sb;
    private final ByteBuffer chunk;
    private final Node node;
    private boolean showDeleted;
    private long cluster;
    private UpcaseTable upcase;
    private ClusterBitMap cb;
    private int index;

    private DirectoryParser(Node node, boolean showDeleted) {
        this.node = node;
        this.showDeleted = showDeleted;
        this.sb = node.getSuperBlock();
        this.chunk = ByteBuffer.allocate(sb.getBytesPerCluster());
        this.chunk.order(ByteOrder.LITTLE_ENDIAN);
        this.cluster = node.getStartCluster();
        this.upcase = null;
    }

    public DirectoryParser setUpcase(UpcaseTable upcase) {
        if (this.upcase != null) {
            throw new IllegalStateException("already had an upcase table");
        }
        this.upcase = upcase;
        return this;
    }
    public DirectoryParser setClusterBitMap(ClusterBitMap cb) {
        this.cb = cb;
        return this;
    }
    private void init() throws IOException {
        Log.i(TAG,"Directory Parser init "+cluster);
        this.sb.readCluster(chunk, cluster);
        chunk.rewind();
    }

    private boolean advance() throws IOException {
        assert ((chunk.position() % ENTRY_SIZE) == 0) :
            "not on entry boundary"; //NOI18N
        if (chunk.remaining() == 0) {
            cluster = node.nextCluster(cluster);
            Log.i(TAG,"advance add index");
            if (Cluster.invalid(cluster)) {
                return false;
            }
            this.chunk.rewind();
            this.sb.readCluster(chunk, cluster);
            this.chunk.rewind();
        }

        return true;
    }

    private void skip(int bytes) {
        chunk.position(chunk.position() + bytes);
    }

    /**
     *0x81 Allocation Bitmap
     *0x82 Up-case Table
     *0x83 Volume Label
     *0x85 File
     *0xA0 Volume GUID
     *0xA1 TexFAT Padding
     *0xA2 Windows CE Access Control Table
     *0xC0 Stream Extension
     *0xC1 File Name
     */
    public void parse(Visitor v,boolean isDelete,String fileName) throws IOException {
        while (true) {
            final int entryType = DeviceAccess.getUint8(chunk);
            if (entryType == LABEL) {
                Log.i(TAG,"start parse label ");
                parseLabel(v);
            } else if (entryType == BITMAP) {
                Log.i(TAG,"start parse bitmap ");
                parseBitmap(v);
            } else if (entryType == UPCASE) {
                Log.i(TAG,"start parse upcase ");
                parseUpcaseTable(v);
            } else if ((entryType & FILE) == FILE) {
                Log.i(TAG,"start parse file ");
                boolean deleted = (entryType & VALID) == 0;
                if(isDelete){
                    delFile(fileName);
                }else{
                    if (showDeleted || !deleted) {
                        parseFile(v, deleted);
                    } else {
                        skip(ENTRY_SIZE - 1);
                    }
                }

            } else if (entryType == EOD) {
                Log.i(TAG,"start parse end ");
                return;
            } else {
                Log.i(TAG,"unknown entry type ");
                if ((entryType & VALID) != 0) {
                    throw new IOException(
                        "unknown entry type " + entryType);
                } else {
                    skip(ENTRY_SIZE - 1);
                }
            }
            if (!advance()) {
                Log.i(TAG,"start parse advance ");
                return;
            }
            index++;
        }

    }

    /**
     * Volume Label Directory Entry
     *
     * 字节偏移   字节长度     描述
     *  0x00      1       EntryType
     *  0x01      1     CharacterCount  Length in Unicode characters (max 11)
     *  0x02      22      VolumeLabel
     *  0x18      8          保留
     */
    private void parseLabel(Visitor v) throws IOException {
        final int len = DeviceAccess.getUint8(chunk);
        if (len > ENAME_MAX_LEN) {
            throw new IOException(len + " is too long");
        }
        final StringBuilder labelBuilder = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            labelBuilder.append(DeviceAccess.getChar(chunk));
        }

        v.foundLabel(labelBuilder.toString());

        skip((ENAME_MAX_LEN - len) * DeviceAccess.BYTES_PER_CHAR);
    }

    /**
     *  字节偏移    字节长度      内容及含义
     *  0x00         1         目录项类型
     *  0x01         1          保留
     *  0x02         18         保留
     *  0x14         4         起始簇号
     *  0x18         8         文件大小
     */
    private void parseBitmap(Visitor v) throws IOException {
        skip(19); /* unknown content */
        final long startCluster = DeviceAccess.getUint32(chunk); // 起始簇号
        final long size = DeviceAccess.getUint64(chunk);         // 文件大小
        v.foundBitmap(startCluster, size);
    }

    /**
     *字节偏移      字节大小      描述
     * 0x00           1       EntryType
     * 0x01           3       Reserved1
     * 0x04           4    TableChecksum
     * 0x08          12       Reserved2
     * 0x14           4       FirstCluster
     * 0x18           8        DataLength
     */
    private void parseUpcaseTable(Visitor v) throws IOException {
        skip(3); /* unknown */
        final long checksum = DeviceAccess.getUint32(chunk);
        assert (checksum >= 0);
        skip(12); /* unknown */
        final long startCluster = DeviceAccess.getUint32(chunk);
        final long size = DeviceAccess.getUint64(chunk);
        v.foundUpcaseTable(this, startCluster, size, checksum);
    }

    /**
     *   File Directory Entry
     * 属性 1
     * 字节偏移   字节大小       描述
     * 0x00        1        EntryType
     * 0x01        1        SecondaryCount  Must be from 2 to 18
     * 0x02        2        SetChecksum
     * 0x04        2        FileAttributes
     * 0x06        2        Reserved1
     * 0x08        4        CreateTimestamp
     * 0x0C        4      LastModifiedTimestamp
     * 0x10        4      LastAccessedTimestamp
     * 0x14        1      Create10msIncrement
     * 0x15        1      LastModified10msIncrement
     * 0x16        1      CreateTimezoneOffset
     * 0x17        1     LastModifiedTimezoneOffset
     * 0x18        1    LastAccessedTimezoneOffset
     * 0x19        7        Reserved2
     *
     *-------------------------------------------------------------
     * 属性2
     * 字节偏移   字节大小       描述
     * 0x00       1           目录项类型
     * 0x01       1           文件碎片标志
     * 0x02       1            保留
     * 0x03       1            文件名字符数N
     * 0x04       2            文件名 Hash 值
     * 0x06       2            保留
     * 0x08       2            文件大小1
     * 0x10       4             保留
     * 0x14       4             起始簇号
     * 0x18       8             文件大小2
     *--------------------------------------------------
     * 属性3
     * 字节偏移   字节大小          描述
     * 0x00         1           目录项类型
     * 0x01        1            保留
     * 0x02        2N           文件名
     */
    private void parseFile(Visitor v, boolean deleted) throws IOException {
         // 处理属性1

        int actualChecksum = startChecksum();

        int conts = DeviceAccess.getUint8(chunk);

        if (conts < 2) {
            throw new IOException("too few continuations (" + conts + ")");
        }

        final int referenceChecksum = DeviceAccess.getUint16(chunk);
        final int attrib = DeviceAccess.getUint16(chunk);
        skip(2); /* unknown */
        final EntryTimes times = EntryTimes.read(chunk);
        skip(7); /* unknown */
        advance();

        //属性2
        actualChecksum = addChecksum(actualChecksum);
        if ((DeviceAccess.getUint8(chunk) & FILE_INFO) != FILE_INFO) {
            throw new IOException("expected file info");
        }
        if (deleted) {
            // Keep the index consistent with the index when not recovering deleted files
            index++;
        }

        final int flag = DeviceAccess.getUint8(chunk);
        skip(1); /* unknown */
        int nameLen = DeviceAccess.getUint8(chunk);
        final int nameHash = DeviceAccess.getUint16(chunk);
        skip(2); /* unknown */
        final long realSize = DeviceAccess.getUint64(chunk);
        skip(4); /* unknown */
        final long startCluster = DeviceAccess.getUint32(chunk);
        final long size = DeviceAccess.getUint64(chunk);

        if (realSize != size) {
            throw new IOException("real size does not equal size");
        }

        conts--;

        //属性3
        /* read file name */
        final StringBuilder nameBuilder = new StringBuilder(nameLen);
        while (conts-- > 0) {
            advance();
            actualChecksum = addChecksum(actualChecksum);

            if ((DeviceAccess.getUint8(chunk) & FILE_NAME) != FILE_NAME) {
                throw new IOException("expected file name");
            }

            if (deleted) {
                // Keep the index consistent with the index when not recovering deleted files
                index++;
            }

            skip(1); /* unknown */

            final int toRead = Math.min(ENAME_MAX_LEN, nameLen);

            for (int i = 0; i < toRead; i++) {
                nameBuilder.append(DeviceAccess.getChar(chunk));
            }

            nameLen -= toRead;
            assert (nameLen >= 0);

            if (nameLen == 0) {
                assert (conts == 0) : "conts remaining?!"; //NOI18N
                skip((ENAME_MAX_LEN - toRead) * DeviceAccess.BYTES_PER_CHAR);
            }
        }

        if (!deleted && referenceChecksum != actualChecksum) {
            throw new IOException("checksum mismatch");
        }

        final String name = nameBuilder.toString();
        if ((this.upcase != null) && (hashName(name) != nameHash)) {
            throw new IOException("name hash mismatch ("
                + Integer.toHexString(hashName(name)) +
                " != " + Integer.toHexString(nameHash) + ")");
        }
        Log.i(TAG,name +"   "+realSize+" startCluster : "+startCluster+" , cluster info :"+this.cluster);
        v.foundNode(Node.create(sb, startCluster, attrib, name, (flag == FLAG_CONTIGUOUS), realSize, times, deleted,this.cluster,index),
            index);
    }

    private void delFile(String fileName) throws IOException {
        // 处理属性1
        Log.i(TAG,"chunk position :"+chunk.position());
        chunk.position(chunk.position()-1);
        Log.i(TAG,"文件属性一 "+Integer.toHexString(DeviceAccess.getUint8(chunk))+" ,cluster : "+cluster); // 85H
        chunk.position(chunk.position()-1);
        chunk.put((byte)0x05);


        Log.i(TAG,"chunk position :"+chunk.position()+" , limit :"+chunk.limit());
        int actualChecksum = startChecksum();

        int conts = DeviceAccess.getUint8(chunk);

        if (conts < 2) {
            throw new IOException("too few continuations (" + conts + ")");
        }

        final int referenceChecksum = DeviceAccess.getUint16(chunk);
        final int attrib = DeviceAccess.getUint16(chunk);
        skip(2); /* unknown */
        final EntryTimes times = EntryTimes.read(chunk);
        skip(7); /* unknown */
        advance();

        //属性2
        actualChecksum = addChecksum(actualChecksum);
        if ((DeviceAccess.getUint8(chunk) & FILE_INFO) != FILE_INFO) {
            throw new IOException("expected file info");
        }

        chunk.position(chunk.position()-1);
        Log.i(TAG,"文件属性二状态 "+Integer.toHexString(DeviceAccess.getUint8(chunk))+" ,cluster : "+cluster); // C0H
        chunk.position(chunk.position()-1);
        chunk.put((byte)0x40);

        final int flag = DeviceAccess.getUint8(chunk);
        skip(1); /* unknown */
        int nameLen = DeviceAccess.getUint8(chunk);
        final int nameHash = DeviceAccess.getUint16(chunk);
        skip(2); /* unknown */
        final long realSize = DeviceAccess.getUint64(chunk);
        skip(4); /* unknown */
        final long startCluster = DeviceAccess.getUint32(chunk);
        final long size = DeviceAccess.getUint64(chunk);

        if (realSize != size) {
            throw new IOException("real size does not equal size");
        }

        conts--;

        //属性3
        /* read file name */
        final StringBuilder nameBuilder = new StringBuilder(nameLen);
        while (conts-- > 0) {
            advance();
            actualChecksum = addChecksum(actualChecksum);

            if ((DeviceAccess.getUint8(chunk) & FILE_NAME) != FILE_NAME) { //c1
                throw new IOException("expected file name");
            }
            chunk.position(chunk.position()-1);
            chunk.put((byte)0x41);
            skip(1); /* unknown */

            final int toRead = Math.min(ENAME_MAX_LEN, nameLen);

            for (int i = 0; i < toRead; i++) {
                nameBuilder.append(DeviceAccess.getChar(chunk));
            }

            nameLen -= toRead;
            assert (nameLen >= 0);

            if (nameLen == 0) {
                assert (conts == 0) : "conts remaining?!"; //NOI18N
                skip((ENAME_MAX_LEN - toRead) * DeviceAccess.BYTES_PER_CHAR);
            }
        }

        final String name = nameBuilder.toString();
        if(("/"+name).equals(fileName)){
            Log.i(TAG,"delete file :"+fileName);
            node.getSuperBlock().writeCluster(chunk,cluster); //write delete flag;
        }
        if ((this.upcase != null) && (hashName(name) != nameHash)) {
            throw new IOException("name hash mismatch ("
                    + Integer.toHexString(hashName(name)) +
                    " != " + Integer.toHexString(nameHash) + ")");
        }
        cb.freeCluster(startCluster);
        Log.i(TAG,name +"   "+realSize+" startCluster : "+startCluster+" , cluster info :"+this.cluster);
    }

    private int hashName(String name) throws IOException {
        int hash = 0;

        for (int i = 0; i < name.length(); i++) {
            final int c = this.upcase.toUpperCase(name.charAt(i));

            hash = ((hash << 15) | (hash >> 1)) + (c & 0xff);
            hash &= 0xffff;
            hash = ((hash << 15) | (hash >> 1)) + (c >> 8);
            hash &= 0xffff;
        }

        return (hash & 0xffff);
    }

    private int startChecksum() {
        final int oldPos = chunk.position();
        chunk.position(chunk.position() - 1);
        assert ((chunk.position() % ENTRY_SIZE) == 0);

        int result = 0;

        for (int i = 0; i < ENTRY_SIZE; i++) {
            final int b = DeviceAccess.getUint8(chunk);
            if ((i == 2) || (i == 3)) continue;
            result = ((result << 15) | (result >> 1)) + b;
            result &= 0xffff;
        }

        chunk.position(oldPos);
        return result;
    }

    private int addChecksum(int sum) {
        chunk.mark();
        assert ((chunk.position() % ENTRY_SIZE) == 0);

        for (int i = 0; i < ENTRY_SIZE; i++) {
            sum = ((sum << 15) | (sum >> 1)) + DeviceAccess.getUint8(chunk);
            sum &= 0xffff;
        }

        chunk.reset();
        return sum;
    }

    interface Visitor {
        public void foundLabel(
                String label) throws IOException;
        /**
         * @param startCluster
         * @param size         bitmap size in bytes
         */
        public void foundBitmap(
                long startCluster, long size) throws IOException;

        /**
         * @param checksum
         * @param startCluster
         * @param size         table size in bytes
         */
        public void foundUpcaseTable(DirectoryParser parser,
                                     long checksum, long startCluster, long size) throws IOException;

        public void foundNode(Node node, int index) throws IOException;
    }

}
