package org.example;

import org.example.file.*;
import org.example.file.exception.FileError;
import org.example.file.exception.QuarantineService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.Path.of;
import static org.example.util.Benchmark.operation;
import static org.example.file.FileConstants.RESOURCE_PATH;

public class Launcher {
    public static void main(String[] args) {
        final HashMap<String, List<FileError>> exceptionInfo = new HashMap<>();

        FileService.copyFilesAsync(RESOURCE_PATH, RESOURCE_PATH + "/dest", exceptionInfo, 0);
        printExceptions(exceptionInfo);
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

    private static void compareCopying() {

//        try {
//            measure(operation("Text copy",
//                    () -> FileService.validateCopy(
//                            RESOURCE_PATH + "text.txt",
//                            RESOURCE_PATH + "result.txt", new CopyFile(), exceptionInfo)));
//            measure(operation("Text transfer",
//                    () -> FileService.validateCopy(
//                            RESOURCE_PATH + "text.txt",
//                            RESOURCE_PATH + "result2.txt", new TransferFile(), exceptionInfo)));
//            measure(operation("Binary copy",
//                    () -> FileService.validateCopy(
//                            RESOURCE_PATH + "binary.jpg",
//                            RESOURCE_PATH + "result.jpg", new CopyFile(), exceptionInfo)));
//            measure(operation("Binary transfer",
//                    () -> FileService.validateCopy(
//                            RESOURCE_PATH + "binary.jpg",
//                            RESOURCE_PATH + "result2.jpg", new TransferFile(), exceptionInfo)));
//            measure(operation("Big file copy",
//                    () -> new CopyFile().execute(
//                            "../big_file.avi",
//                            "../big_file_copy_result.avi", exceptionInfo), SECONDS));
//            measure(operation("Big file transfer",
//                    () -> new TransferFile().execute(
//                            "../big_file.avi",
//                            "../big_file_transfer_result.avi", exceptionInfo), SECONDS));
//            measure(operation("Big file safe map copy",
//                    () -> new CopyLargeFile(true).execute(
//                            "../big_file.avi",
//                            "../big_file_safe_map_copy_result.avi", exceptionInfo), SECONDS));
//            measure(operation("Big file unsafe map copy",
//                    () -> new CopyLargeFile().execute(
//                            "../big_file.avi",
//                            "../big_file_unsafe_map_copy_result.avi", exceptionInfo), MILLISECONDS));
//            measure(operation("Big file unsafe validated map copy",
//                    () -> FileService.validateCopy(
//                            "../big_file.avi",
//                            "../big_file_unsafe_validated_map_copy_result.avi", new CopyLargeFile(), exceptionInfo), MILLISECONDS));
//            measure(operation("Big file validated map copy",
//                    () -> FileService.validateCopy(
//                            "../big_file.avi",
//                            "../big_file_validated_map_copy_result.avi", new CopyLargeFile().withProgress(), exceptionInfo), MILLISECONDS));
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }
}