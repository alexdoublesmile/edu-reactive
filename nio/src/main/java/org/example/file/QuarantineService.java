package org.example.file;

import org.example.file.exception.ExceptionHelper;
import org.example.file.exception.FileError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.file.Path.of;
import static org.example.util.FileConstants.DEFAULT_QUARANTINE_PATH;

public final class QuarantineService {
    private static final List<Path> quarantineList = new ArrayList<>();


    private static void moveFileToQuarantine(Path srcPath, Map<String, List<FileError>> exceptionInfo) {
        // Create quarantine directory if it doesn't exist
        final Path quarantinePath = of(DEFAULT_QUARANTINE_PATH);
        if (!Files.exists(quarantinePath)) {
            try {
                Files.createDirectories(quarantinePath);
            } catch (IOException e) {
                ExceptionHelper.handleException(FileError.FileErrorType.CREATE_ERROR, srcPath.toString(), format(
                        "Failed to create quarantine directory %s", quarantinePath), e, exceptionInfo);
                return;
            }
        }
        Path quarantineFile = of(DEFAULT_QUARANTINE_PATH, srcPath.getFileName().toString()).toAbsolutePath().normalize();
        // Move the file to quarantine
        try {
            Files.move(srcPath, quarantineFile);
        } catch (IOException e) {
            ExceptionHelper.handleException(FileError.FileErrorType.MOVE_ERROR, srcPath.toString(), format(
                    "Failed to move file %s to quarantine as %s", srcPath, quarantineFile), e, exceptionInfo);
        }
    }

    public static void addToQuarantine(Path srcPath) {
        System.err.printf("File will be moved to dir %s for investigation%n", DEFAULT_QUARANTINE_PATH);
        quarantineList.add(srcPath);
    }

    public static void processQuarantineList(Map<String, List<FileError>> exceptionInfo) {
        quarantineList.forEach(file -> moveFileToQuarantine(file, exceptionInfo));
    }
}
