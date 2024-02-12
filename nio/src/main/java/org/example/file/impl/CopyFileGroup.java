package org.example.file.impl;

import org.example.file.CopyAction;

public class CopyFileGroup implements CopyAction {
    @Override
    public void execute(String source, String target) {

    }

    // make java function as an example of copying file by NIO
    // make a java code example of such Concurrent file operations (make analog of your previous example but for multiple files concurrent operations)
//    public static void copyFiles(String sourceDir, String targetDir, int bufferSize) throws IOException {
//        // Use a selector for non-blocking I/O
//        Selector selector = Selector.open();
//
//        // Map of source files to their corresponding target channels
//        Map<String, FileChannel> targetChannels = new HashMap<>();
//
//        // Open target channels for all files in the target directory
//        for (String file : Files.list(of(targetDir))) {
//            String targetPath = Paths.get(targetDir, file).toString();
//            FileChannel targetChannel = FileChannel.open(Paths.get(targetPath), CREATE, WRITE);
//            targetChannels.put(file, targetChannel);
//            targetChannel.register(selector, SelectionKey.OP_WRITE);
//        }
//
//        // Loop through source files and register them for reading
//        for (String file : Files.list(Paths.get(sourceDir))) {
//            String sourcePath = Paths.get(sourceDir, file).toString();
//            FileChannel sourceChannel = FileChannel.open(Paths.get(sourcePath), READ);
//            sourceChannel.register(selector, SelectionKey.OP_READ);
//        }
//
//        // Main loop for handling I/O operations
//        while (true) {
//            int selected = selector.select();
//            if (selected == 0) {
//                continue; // No keys ready, wait for events
//            }
//
//            Set<SelectionKey> selectedKeys = selector.selectedKeys();
//            Iterator<SelectionKey> it = selectedKeys.iterator();
//
//            while (it.hasNext()) {
//                SelectionKey key = it.next();
//                it.remove();
//
//                if (key.isReadable()) {
//                    handleRead(key);
//                } else if (key.isWritable()) {
//                    handleWrite(key, targetChannels);
//                }
//            }
//        }
//
//        // Close all channels and selector
//        for (FileChannel channel : targetChannels.values()) {
//            channel.close();
//        }
//        selector.close();
//    }
//
//    private static void handleRead(SelectionKey key) throws IOException {
//        FileChannel sourceChannel = (FileChannel) key.channel();
//        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
//        int bytesRead = sourceChannel.read(buffer);
//
//        if (bytesRead == -1) {
//            // Reached end of file, unregister and close channel
//            key.cancel();
//            sourceChannel.close();
//        } else {
//            buffer.flip();
//            key.attach(buffer); // Store partially read data for writing
//            key.interestOps(SelectionKey.OP_WRITE); // Switch to write interest
//        }
//    }
//
//    private static void handleWrite(SelectionKey key, Map<String, FileChannel> targetChannels) throws IOException {
//        FileChannel sourceChannel = (FileChannel) key.channel();
//        ByteBuffer buffer = (ByteBuffer) key.attachment();
//        String fileName = (String) key.attachment(1); // Store filename for target channel retrieval
//        FileChannel targetChannel = targetChannels.get(fileName);
//
//        int bytesWritten = targetChannel.write(buffer);
//
//        if (buffer.hasRemaining()) {
//            // Buffer not empty, keep writing
//        } else {
//            // Buffer empty, clear it and switch to read interest
//            buffer.clear();
//            sourceChannel.register(key.selector(), SelectionKey.OP_READ);
//        }
//    }
}
