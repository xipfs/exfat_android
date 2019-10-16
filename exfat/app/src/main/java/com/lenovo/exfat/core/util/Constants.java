package com.lenovo.exfat.core.util;


/**
 * exFAT 文件系统常量
 * 解析 DBR 之后存放这里，方便使用
 *
 * @auther xiehui
 * @create 2019-10-11 上午11:13
 */
public class Constants {
    public static String DISK_LABEL;                 // 标签
    public static long TOTAL_BLOCKS;			// 总扇区数
    public static long FAT_BLOCK_START;			// FAT 起始扇区号
    public static int  FAT_BLOCK_COUNT;			// FAT 扇区数
    public static long CLUSTER_BLOCK_START;		// 首簇起始扇区号
    public static long CLUSTER_COUNT;			// 总簇数
    public static long ROOT_DIRECTORY_CLUSTER;	// 根目录首簇号
    public static int  VOLUME_SERIAL;			// 卷序列号
    public static byte MINOR_VERSION;		    // 版本号
    public static byte MAJOR_VERSION;
    public static short VOLUME_STATE;  		    // 卷状态
    public static byte  BLOCK_BITS;				// 每扇区字节数 2^n
    public static byte CLUSTER_BLOCKS;	        // 每簇扇区数 2^n
    public static byte PERCENT_IN_USE;			// 使用百分比



    public static final int DIR_ENTRY_SIZE = 32;               // 目录项大小
    public static final int ENAME_MAX_LEN = 15;
    public static final int VALID = 0x80;
    public static final int CONTINUED = 0x40;
    public static final int IMPORTANCE_MASK = 0x20;

    //Directory Entry Type
    public static final int EOD =      0x00;
    public static final int NO_LABEL =   0x03;
    public static final int BITMAP =   0x81;
    public static final int UPCASE =   0x82;
    public static final int LABEL =    0x83;
    public static final int FILE =     0x85;
    public static final int GUID =     0xA0;
    public static final int TexFATPadding      = 0xA1;
    public static final int AccessControlTable = 0xA2;
    public static final int StreamExtension    = 0xC0;
    public static final int FileName           = 0xC1;


    public static final int FILE_DEL = 0x05;
    public static final int StreamExtension_DEL = 0x40;
    public static final int FileName_DEL = 0x41;

    public static final int FILE_INFO = (0x00 | CONTINUED);
    public static final int FILE_NAME = (0x01 | CONTINUED);
    public static final int FLAG_FRAGMENTED = 1;
    public static final int FLAG_CONTIGUOUS = 3;

    //文件属性
    public static final byte ATTRIB_RO = 0x01;		// 00000001 只读
    public static final byte ATTRIB_HIDDEN = 0x02;	// 00000010 隐藏
    public static final byte ATTRIB_SYSTEM = 0x04;	// 00000100 系统
    public static final byte ATTRIB_VOLUME = 0x08;   // 00001000 卷簇
    public static final byte ATTRIB_DIR = 0x10;      // 00010000 子目录
    public static final byte ATTRIB_ARCH = 0x20;     // 00100000 存档

}
