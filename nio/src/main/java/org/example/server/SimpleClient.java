package org.example.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;

// TODO: 24.02.2024 need to rewrite
public class SimpleClient {
    private static final String HOST = "localhost";
    private static final int PORT = 8020;

    public static void main(String[] args) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(HOST, PORT));

        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        Scanner scanner = new Scanner(System.in);

        while (true) {
            int selected = selector.select();

//            if (selected > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key, scanner);
                    }
                }
//            }


        }
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(buffer);

        if (bytesRead > 0) {
            buffer.flip();
            String message = new String(buffer.array(), 0, bytesRead, StandardCharsets.UTF_8);
            System.out.println("Received message from server: " + message);
        } else if (bytesRead == -1) {
            // Server closed connection
            System.out.println("Server closed connection.");
            key.channel().close();
            System.exit(0);
        }
    }

    private static void handleWrite(SelectionKey key, Scanner scanner) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        if (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            buffer.put(message.getBytes(StandardCharsets.UTF_8));
            buffer.flip();

            // Ensure channel is ready for writing before attempting
            if (channel.write(buffer) == 0) {
                // Channel not ready, register for OP_WRITE event
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        }
    }
}
