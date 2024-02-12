package org.example.file.impl;

import org.example.file.CopyAction;
import org.example.file.FileService;

public class TransferFile implements CopyAction {
    @Override
    public void execute(String source, String target) {
        transferFile(source, target);
    }
    public static void transferFile(String source, String target) {
        FileService.transferFile(source, target);
    }
}
