package org.example.file.impl;

import org.example.file.CopyAction;
import org.example.file.exception.FileError;

import java.util.List;
import java.util.Map;

public class TransferFile implements CopyAction {
    @Override
    public void execute(String source, String target) {

    }

    @Override
    public void execute(String source, String target, Map<String, List<FileError>> exceptionInfo) {
        transferFile(source, target);
    }
    public static void transferFile(String source, String target) {
//        FileService.transferFile(source, target);
    }
}
