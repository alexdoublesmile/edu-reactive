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

    public static void copyFile(String source, String target) throws IOException {
        copyFile(source, target, DEFAULT_BUFFER_SIZE);
    }
    public static void copyFile(String source, String target, int bufferSize) throws IOException {
        try (
                FileChannel sourceChannel = FileChannel.open(of(source), READ);
                FileChannel targetChannel = FileChannel.open(of(target), CREATE, WRITE)
        ) {
            // create byte[bufferSize] in Heap with position = 0, limit = capacity
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            // return file size
            long totalBytes = sourceChannel.size();
            long bytesCopied = 0;
            while (bytesCopied < totalBytes) {
                try {
                    // return num of read bytes & position = last byte
                    int bytesRead = sourceChannel.read(buffer);
                    if (bytesRead == -1) {
                        break; // Reached end of source file
                    }
                    // switch read to write mode (limit = position, position = 0)
                    buffer.flip();
                    targetChannel.write(buffer);
                    // not clear, just move pointers (position = 0, limit = capacity)
                    buffer.clear();
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
