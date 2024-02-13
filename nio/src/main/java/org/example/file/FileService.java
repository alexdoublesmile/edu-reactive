package org.example.file;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.Path.of;
import static java.nio.file.StandardOpenOption.*;
import static java.time.Instant.now;
import static org.example.file.FileConstants.DEFAULT_BUFFER_SIZE;
import static org.example.file.FileConstants.DEFAULT_QUARANTINE_PATH;
import static org.example.file.FileError.FileErrorType.*;
import static org.example.file.exception.ExceptionHelper.handleException;
import static org.example.util.PrintUtil.printProgress;

public final class FileService {

    public static void transferFile(String source, String target, Map<String, List<FileError>> exceptionInfo) {
        try (
                FileChannel sourceChannel = open(of(source), READ);
                FileChannel targetChannel = open(of(target), CREATE, WRITE)
        ) {
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        } catch (IOException ex) {
            handleException(OPEN_ERROR, source, format("Failed to open file channel %s", source), ex, exceptionInfo);
        }
    }

    // adjust the buffer size based on performance needs and available memory
    // e.g. use as large buffer, as you can by memory & result latency
    public static void copyFile(
            String source,
            String target,
            int bufferSize,
            boolean withProgress,
            Map<String, List<FileError>> exceptionInfo) {
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
                int bytesRead = 0;
                try {
                    // write all data to buffer(position = last byte) & return num of read bytes
                    bytesRead = sourceChannel.read(buffer);
                } catch (IOException ex) {
                    handleException(READ_ERROR, source, format("Failed to read data from %s", source), ex, exceptionInfo);
                }
                if (bytesRead == -1) {
                    break; // Reached end of source file
                }
                // switch read to write mode (limit = position, position = 0)
                buffer.flip();
                try {
                    // read all data from buffer
                    targetChannel.write(buffer);
                } catch (IOException ex) {
                    handleException(WRITE_ERROR, source, format("Failed to write data to %s", target), ex, exceptionInfo);
                }
                // not clear, just move pointers (position = 0, limit = capacity)
                buffer.clear();
                bytesCopied += bytesRead;

                if (withProgress) {
                    printProgress(bytesCopied, totalBytes);
                }
            }
        } catch (IOException ex) {
            handleException(OPEN_ERROR, source, format("Failed to open file channel %s", source), ex, exceptionInfo);
        }
    }

    public static void copyFileWithValidation(
            String source,
            String target,
            int maxRetries,
            int baseDelay,
            boolean exponential,
            Map<String, List<FileError>> exceptionInfo
    ) {
        int retry = 0;
        int delay;

        while (retry < maxRetries) {
            String originalHash = calculateHash(source, exceptionInfo);
            copyFile(source, target, DEFAULT_BUFFER_SIZE, false, exceptionInfo);
//            transferFile(source, target, exceptionInfo);
            String copiedHash = calculateHash(target, exceptionInfo);

            // Verify integrity
            if (!originalHash.isBlank() && originalHash.equals(copiedHash)) {
                System.out.println("File copied successfully!");
                return;
            } else {
                System.err.println("File integrity mismatch! Retrying...");
            }

            delay = exponential ? (int) Math.pow(2, retry) * baseDelay : baseDelay;
            System.out.println("Waiting " + delay + " milliseconds before retry...");

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                handleException(RETRY_WAITING_ERROR, source, format(
                        "Waiting for %d retry of copying for file %s to %s was interrupted",
                        retry + 1, source, target), e, exceptionInfo);
                return;
            }
            retry++;
        }
        handleException(COPY_INTEGRITY_ERROR, source, format(
                "Failed to copy file %s to %s after %d attempts. File will moved to dir %s for investigation.",
                source, target, retry, DEFAULT_QUARANTINE_PATH), null, exceptionInfo);
        moveFileToQuarantine(source, exceptionInfo);
    }

    private static String calculateHash(String filePath, Map<String, List<FileError>> exceptionInfo) {
        String hash = "";
        try {
            hash = DigestUtils.sha256Hex(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            handleException(EXIST_ERROR, filePath, format("File %s doesn't exist", filePath), e, exceptionInfo);
        } catch (IOException e) {
            handleException(HASH_ERROR, filePath, format("Failed to calculate hash from %s", filePath), e, exceptionInfo);
        }
        return hash;
    }

    private static void moveFileToQuarantine(String source, Map<String, List<FileError>> exceptionInfo) {
        File sourceFile = new File(source);
        File quarantineFile = new File(DEFAULT_QUARANTINE_PATH, sourceFile.getName());
        // Create quarantine directory if it doesn't exist
        if (!quarantineFile.getParentFile().exists()) {
            if (!quarantineFile.getParentFile().mkdirs()) {
                handleException(CREATE_ERROR, source, format(
                        "Failed to create quarantine directory %s", quarantineFile), null, exceptionInfo);
                return;
            }
        }
        // Move the file to quarantine
        if (sourceFile.renameTo(quarantineFile)) {
            System.out.println("File moved to quarantine: " + quarantineFile.getAbsolutePath());

        } else {
            handleException(MOVE_ERROR, source, format(
                    "Failed to move file %s to quarantine as %s", source, quarantineFile), null, exceptionInfo);
        }
    }

    public static void copyLargeFile(
            String source,
            String target,
            long bufferSize,
            boolean isSafe,
            boolean withProgress,
            Map<String, List<FileError>> exceptionInfo
    ) {
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

        } catch (IOException ex) {
            handleException(COPY_ERROR, source, format("Failed to copy file %s", source), ex, exceptionInfo);
        }
    }


    // standard strategy of thread pool size for overlapping I/O processing & handling bursts of activity

    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final int DEFAULT_LARGE_BUFFER_SIZE = 1024 * 1024; // 1MB buffer

    public static void copyFilesAsync(
            String sourceDir,
            String targetDir,
            Map<String, List<FileError>> exceptionInfo,
            int maxConcurrent
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CompletionHandler<Void, Path> copyHandler = new CopyCompletionHandler(targetDir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(of(sourceDir))) {
            for (Path sourceFile : stream) {
                // Handle graceful shutdown if maxConcurrent reached
                if (executor.isShutdown()) {
                    break;
                }

                Path targetFile = targetDir.resolve(sourceFile.getFileName());
//                executor.submit(() -> copyFileAsync(sourceFile, targetFile, copyHandler, exceptionInfo));
                executor.submit(() -> copyFile(sourceDir, targetDir, DEFAULT_BUFFER_SIZE, false, exceptionInfo));
//                executor.submit(() -> FileService.copyFileWithValidation(
//                        sourceFile.toString(), targetFile.toString(), 3, 1000, true, exceptionInfo));
            }
        } catch (IOException ex) {
            handleException(OPEN_ERROR, sourceDir.toString(), format("Failed to open dir stream %s", sourceDir), ex, exceptionInfo);
            return;
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS); // Wait for all tasks to finish
        } catch (InterruptedException e) {
            throw new RuntimeException("Connection pool wasn't terminated correctly", e);
        }
    }

    private static void copyFileAsync(Path sourcePath, Path targetPath, CompletionHandler<Void, Path> handler, Map<String, List<FileError>> exceptionInfo) {
        final String src = sourcePath.toString();
        exceptionInfo.put(src, new ArrayList<>());

        try (AsynchronousFileChannel sourceChannel = AsynchronousFileChannel.open(sourcePath, StandardOpenOption.READ);
             AsynchronousFileChannel targetChannel = AsynchronousFileChannel.open(targetPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            if (!Files.exists(sourcePath)) {
                handleException(EXIST_ERROR, src, format("Source %s doesn't exist", src),
                        new NoSuchFileException("Source file not found: " + sourcePath), exceptionInfo);
                return;
            }

            ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_LARGE_BUFFER_SIZE);

            // Inner completion handler for reading chunks
            CompletionHandler<Integer, Path> readHandler = new CompletionHandler<>() {
                @Override
                public void completed(Integer bytesRead, Path sourceFile) {
                    if (bytesRead == -1) { // EOF reached
                        handler.completed(null, sourceFile); // Signal completion
                        return;
                    }

                    // Flip the buffer and write to target
                    buffer.flip();
                    targetChannel.write(buffer, 0, targetPath, new CompletionHandler<>() {
                        @Override
                        public void completed(Integer bytesWritten, Path targetFile) {
                            // Ensure all bytes were written
                            if (bytesWritten != bytesRead) {
                                System.err.println("Incomplete copy for: " + sourceFile);
                            }
                            buffer.clear(); // Prepare for next read
                            sourceChannel.read(buffer, 0, sourcePath, this);
                        }

                        @Override
                        public void failed(Throwable exc, Path targetFile) {
                            handler.failed(exc, targetFile);
                        }
                    });
                }

                @Override
                public void failed(Throwable exc, Path sourceFile) {
                    handler.failed(exc, sourceFile);
                }
            };

            // Initiate the first read operation
            sourceChannel.read(buffer, 0, sourcePath, readHandler);

        } catch (IOException e) {
            handleException(COPY_ERROR, src, format("Failed to copy %s", src), e, exceptionInfo);
        }
    }

    private static class CopyCompletionHandler implements CompletionHandler<Void, Path> {

        private final String targetDir;

        public CopyCompletionHandler(String targetDir) {
            this.targetDir = targetDir;
        }

        @Override
        public void completed(Void result, Path sourceFile) {
            System.out.println("Copied file: " + sourceFile + " to " + targetDir.resolve(sourceFile.getFileName()));
        }

        @Override
        public void failed(Throwable exc, Path sourceFile) {
            System.err.println("Error copying file: " + sourceFile + ". Reason: " + exc.getMessage());
            // Handle errors more comprehensively (e.g., retry, move to error directory)
        }
    }
}
