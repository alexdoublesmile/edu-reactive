package org.example.file.impl;

import org.example.file.CopyAction;
import org.example.file.FileService;

public class CopyFile implements CopyAction {
    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private boolean withProgress;

    public CopyFile() {
    }

    public CopyFile(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void withProgress() {
        withProgress = true;
    }

    @Override
    public void execute(String source, String target) {
        copyFile(source, target, bufferSize, withProgress);
    }

    public static void copyFile(String source, String target, int bufferSize, boolean withProgress) {
        FileService.copyFile(source, target, bufferSize, withProgress);
    }
}
