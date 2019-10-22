package com.lenovo.exfat.core.util;


import com.lenovo.exfat.core.fs.ExFatFile;

import java.util.HashMap;

/**
 * @auther xiehui
 * @create 2019-10-17 下午4:24
 */
public class ExFatCache {
    public static HashMap<String, ExFatFile> extFatFileCache = new HashMap<>();
}
