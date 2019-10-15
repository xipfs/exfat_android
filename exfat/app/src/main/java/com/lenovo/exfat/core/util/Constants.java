package com.lenovo.exfat.core.util;


/**
 * exFAT 文件系统常量
 * 解析 DBR 之后存放这里，方便使用
 *
 * @auther xiehui
 * @create 2019-10-11 上午11:13
 */
public class Constants {
    public static String LABEL;                 // 标签
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
}
