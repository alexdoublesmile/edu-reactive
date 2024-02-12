package org.example.file;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.Path.of;
import static java.nio.file.StandardOpenOption.*;
import static java.time.Instant.now;
import static org.example.util.PrintUtil.printProgress;
import static org.example.file.FileConstants.DEFAULT_QUARANTINE_PATH;
import static org.example.file.FileError.FileErrorType.*;

public final class FileService {

    public static void transferFile(String source, String target) {
        try (
                FileChannel sourceChannel = open(of(source), READ);
                FileChannel targetChannel = open(of(target), CREATE, WRITE)
        ) {
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        } catch (IOException e) {
            throw new RuntimeException("Error opening file: " + e.getMessage(), e);
            // TODO: 12.02.2024 collect exceptions

        }
    }

    // adjust the buffer size based on performance needs and available memory
    // e.g. use as large buffer, as you can by memory & result latency
    public static void copyFile(String source, String target, int bufferSize, boolean withProgress) {
        try (
                FileChannel sourceChannel = open(of(source), READ);
                FileChannel targetChannel = open(of(target), CREATE, WRITE)
        ) {
            // create byte[bufferSize] in Heap with position = 0, limit = capacity
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            // return file size
            long totalBytes = sourceChannel.size();
            long bytesCopied = 0;
            while (bytesCopied < totalBytes) {
                try {
                    // write all data to buffer(position = last byte) & return num of read bytes
                    int bytesRead = sourceChannel.read(buffer);
                    if (bytesRead == -1) {
                        break; // Reached end of source file
                    }
                    // switch read to write mode (limit = position, position = 0)
                    buffer.flip();
                    // read all data from buffer
                    targetChannel.write(buffer);
                    // not clear, just move pointers (position = 0, limit = capacity)
                    buffer.clear();
                    bytesCopied += bytesRead;

                } catch (IOException e) {
                    throw new RuntimeException("Error copying file: " + e.getMessage(), e);
                    // TODO: 12.02.2024 collect exceptions

                }

                if (withProgress) {
                    printProgress(bytesCopied, totalBytes);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error opening file: " + e.getMessage(), e);
            // TODO: 12.02.2024 collect exceptions

        }
    }

    public static void copyLargeFile(String source, String target, long bufferSize, boolean isSafe, boolean withProgress) {
        try (
                FileChannel sourceChannel = open(of(source), READ);
                FileChannel targetChannel = open(of(target), CREATE, READ, WRITE)
        ) {
            long fileSize = sourceChannel.size();

            // Loop through the file in chunks to ensure memory efficiency
            long remaining = fileSize;
            long bytesCopied = 0;
            while (remaining > 0) {
                long chunkSize = Math.min(remaining, bufferSize);
                MappedByteBuffer sourceBuffer = sourceChannel
                        .map(FileChannel.MapMode.READ_ONLY, sourceChannel.position(), chunkSize);
                MappedByteBuffer targetBuffer = targetChannel
                        .map(FileChannel.MapMode.READ_WRITE, targetChannel.position(), chunkSize);

                // Transfer data in chunks
                targetBuffer.put(sourceBuffer);

                if (withProgress) {
                    bytesCopied += chunkSize;
                    printProgress(bytesCopied, fileSize);
                }

                // Release buffers and update positions
                if (isSafe) {
                    // Ensure data is written to disk
                    sourceBuffer.force();
                    targetBuffer.force();
                }
                sourceChannel.position(sourceChannel.position() + chunkSize);
                targetChannel.position(targetChannel.position() + chunkSize);

                remaining -= chunkSize;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
            // TODO: 12.02.2024 collect exceptions

        }
    }

    public static void validateCopy(
            String source,
            String target,
            CopyAction copyAction,
            int maxRetries,
            int baseDelay,
            boolean exponential,
            Map<String, List<FileError>> exceptionInfo
    ) {
        exceptionInfo.put(source, new ArrayList<>());
        int retry = 0;
        int delay = baseDelay;

        while (retry < maxRetries) {
            try {
                // Calculate hash of original file
                String originalHash = DigestUtils.sha256Hex(new FileInputStream(source));

                // Perform copy operation
                copyAction.execute(source, target);

                // Calculate hash of copied file
                String copiedHash = DigestUtils.sha256Hex(new FileInputStream(target));

                // Verify integrity
                if (originalHash.equals(copiedHash)) {
                    System.out.println("File copied successfully!");
                    return;
                } else {
                    System.err.println("Error: File integrity mismatch! Retrying...");
                }
            } catch (IOException e) {
                System.err.println("Copy failed with exception: " + e.getMessage());
                exceptionInfo.get(source).add(FileError.builder()
                        .type(COPY_ERROR)
                        .message(format("Copying of file %s to %s failed on %d try", source, target, retry))
                        .timestamp(now())
                        .cause(e)
                        .build());
            }

            if (exponential) {
                delay = (int) Math.pow(2, retry) * baseDelay;
            }
            System.out.println("Waiting " + delay + " milliseconds before retry...");
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                System.err.println("Copy waiting failed with exception: " + e.getMessage());
                exceptionInfo.get(source).add(FileError.builder()
                        .type(RETRY_WAITING_ERROR)
                        .message(format(
                                "Waiting for %d retry of copying for file %s to %s was interrupted",
                                retry + 1, source, target))
                        .timestamp(now())
                        .cause(e)
                        .build());
            }
            retry++;
        }

        System.err.println("Failed to copy file after multiple attempts.");

        System.err.println("Error: Failed to copy file after multiple attempts.! Moving to quarantine...");
        exceptionInfo.get(source).add(FileError.builder()
                .type(INTEGRITY_ERROR)
                .message(format(
                        "Failed to copy file %s to %s after %d attempts. File will moved to dir %s for investigation.",
                        source, target, retry, DEFAULT_QUARANTINE_PATH))
                .timestamp(now())
                .build());
        moveFileToQuarantine(source, DEFAULT_QUARANTINE_PATH, exceptionInfo);
    }

    private static void moveFileToQuarantine(String source, String quarantineDir, Map<String, List<FileError>> exceptionInfo) {
        File sourceFile = new File(source);
        File quarantineFile = new File(quarantineDir, sourceFile.getName());
        // Create quarantine directory if it doesn't exist
        if (!quarantineFile.getParentFile().exists()) {
            quarantineFile.getParentFile().mkdirs();
        }
        // Move the file to quarantine
        if (sourceFile.renameTo(quarantineFile)) {
            System.out.println("File moved to quarantine: " + quarantineFile.getAbsolutePath());

        } else {
            System.err.println("Failed to move file to quarantine");
            exceptionInfo.get(source).add(FileError.builder()
                    .type(MOVE_ERROR)
                    .message(format(
                            "Failed to move file %s to quarantine as %s",
                            source, quarantineFile))
                    .timestamp(now())
                    .build());
        }
    }

    private static final int DEFAULT_MAX_RETRY = 3;
    private static final int DEFAULT_BASE_DELAY = 1000;
    public static void validateCopy(String source, String target, CopyAction copyAction,
                                    Map<String, List<FileError>> exceptionInfo) {
        validateCopy(source, target, copyAction, DEFAULT_MAX_RETRY, DEFAULT_BASE_DELAY, true, exceptionInfo);
    }

    public static void validateCopy(String source, String target, CopyAction copyAction, boolean exponential,
                                    Map<String, List<FileError>> exceptionInfo) {
        validateCopy(source, target, copyAction, DEFAULT_MAX_RETRY, DEFAULT_BASE_DELAY, exponential, exceptionInfo);
    }

    public static void validateCopy(String source, String target, CopyAction copyAction, int maxRetries,
                                    Map<String, List<FileError>> exceptionInfo) {
        validateCopy(source, target, copyAction, maxRetries, DEFAULT_BASE_DELAY, true, exceptionInfo);
    }

    public static void validateCopy(
            String source,
            String target,
            CopyAction copyAction,
            int maxRetries,
            boolean exponential,
            Map<String, List<FileError>> exceptionInfo) {
        validateCopy(source, target, copyAction, maxRetries, DEFAULT_BASE_DELAY, exponential, exceptionInfo);
    }

    public static void validateCopy(
            String source,
            String target,
            CopyAction copyAction,
            int maxRetries,
            int baseDelay,
            Map<String, List<FileError>> exceptionInfo) {
        validateCopy(source, target, copyAction, maxRetries, baseDelay, true, exceptionInfo);
    }
}
