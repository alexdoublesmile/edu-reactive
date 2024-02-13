package org.example.file.impl;

import org.example.file.CopyAction;
import org.example.file.FileError;
import org.example.file.FileService;

import java.util.List;
import java.util.Map;

import static org.example.file.FileConstants.DEFAULT_BUFFER_SIZE;

public class CopyFile implements CopyAction {

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

    @Override
    public void execute(String source, String target, Map<String, List<FileError>> exceptionInfo) {
        copyFile(source, target, bufferSize, withProgress);
    }

    public static void copyFile(String source, String target, int bufferSize, boolean withProgress) {
//        FileService.copyFile(source, target, bufferSize, withProgress);
    }
}
