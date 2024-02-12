package org.example.file.impl;

import org.example.file.CopyAction;
import org.example.file.FileService;

public class CopyLargeFile implements CopyAction {
    private static final long DEFAULT_BUFFER_SIZE = 128 * 1024 * 1024;
    private boolean isSafe = true;
    private long bufferSize = DEFAULT_BUFFER_SIZE;
    private boolean withProgress;

    public CopyLargeFile() {
    }

    public CopyLargeFile(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public CopyLargeFile(boolean isSafe) {
        this.isSafe = isSafe;
    }

    public CopyLargeFile(int bufferSize, boolean isSafe) {
        this.bufferSize = bufferSize;
        this.isSafe = isSafe;
    }

    public CopyAction withProgress() {
        withProgress = true;
        return this;
    }

    @Override
    public void execute(String source, String target) {
        copyLargeFile(source, target, bufferSize, isSafe, withProgress);
    }

    public static void copyLargeFile(String source, String target, long bufferSize, boolean isSafe, boolean withProgress) {
        FileService.copyLargeFile(source, target, bufferSize, isSafe, withProgress);
    }
}
