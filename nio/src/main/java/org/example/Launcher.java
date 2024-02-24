package org.example;

import org.example.file.exception.FileError;
import org.example.server.SimpleServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Launcher {
    public static void main(String[] args) throws IOException {
//        final HashMap<String, List<FileError>> exceptionInfo = new HashMap<>();
//
//        FileService.copyFilesAsync(RESOURCE_PATH, RESOURCE_PATH + "/dest", exceptionInfo, 0);
//        printExceptions(exceptionInfo);
        new SimpleServer(8020).start();
    }

    private static void printExceptions(HashMap<String, List<FileError>> exceptions) {
        if (exceptions.size() > 0) {
            for (Map.Entry<String, List<FileError>> entry : exceptions.entrySet()) {
                System.out.println(entry.getKey() + ":");
                entry.getValue().forEach(error -> {
                    System.out.println(error);
                    error.getCause().printStackTrace();
                });
            }
        } else {
            System.out.println("All files were copied successfully!");
        }
    }
}