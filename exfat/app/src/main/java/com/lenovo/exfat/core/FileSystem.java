package com.lenovo.exfat.core;

import com.lenovo.exfat.core.fs.ExFatFile;

import java.io.IOException;
import java.util.List;

public interface FileSystem {
    void init() throws IOException;
    ExFatFile findFile(String path);
}
