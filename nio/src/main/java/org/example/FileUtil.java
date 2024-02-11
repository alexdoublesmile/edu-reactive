package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static java.nio.file.Path.of;
import static java.nio.file.StandardOpenOption.*;

public final class FileUtil {

    // adjust the buffer size based on performance needs and available memory
    // e.g. use as large buffer, as you can by memory & result latency
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public static void copy(String source, String target) throws IOException {
        copy(source, target, DEFAULT_BUFFER_SIZE);
    }
    public static void copy(String source, String target, int bufferSize) throws IOException {
        try (
                FileChannel sourceChannel = FileChannel.open(of(source), READ);
                FileChannel targetChannel = FileChannel.open(of(target), CREATE, WRITE)
        ) {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            // Transfer data in chunks
            long totalBytes = sourceChannel.size();
            long bytesCopied = 0;
            while (bytesCopied < totalBytes) {
                try {
                    int bytesRead = sourceChannel.read(buffer);
                    if (bytesRead == -1) {
                        break; // Reached end of source file
                    }
                    buffer.flip(); // Flip buffer to switch from reading to writing mode
                    targetChannel.write(buffer);
                    buffer.clear(); // Clear buffer to prepare for next read
                    bytesCopied += bytesRead;

                } catch (IOException e) {
                    throw new IOException("Error copying file: " + e.getMessage(), e);
                }

                printProgress(bytesCopied, totalBytes);
            }
        } catch (IOException e) {
            throw new IOException("Error opening file: " + e.getMessage(), e);
        }
    }

    private static void printProgress(long bytesCopied, long totalBytes) {
        double progress = ((double) bytesCopied / totalBytes) * 100;
        System.out.printf("\rProgress: %.1f%% ", progress);
    }
}
