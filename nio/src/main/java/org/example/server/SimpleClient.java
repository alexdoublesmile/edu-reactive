package org.example.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static org.example.server.SimpleServer.EXIT_CODE;

public class SimpleClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    private final String host;
    private final int port;

    public SimpleClient(String host, int port) {
        this.host = host;
        this.port = port;
        initClient();
    }

    public SimpleClient(int port) {
        this(DEFAULT_HOST, port);
    }

    public SimpleClient(String host) {
        this(host, DEFAULT_PORT);
    }

    public SimpleClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    private void initClient() {

    }

    public void run() throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);

        Selector selector = Selector.open();
        channel.register(selector, OP_CONNECT);
        channel.connect(new InetSocketAddress(host, port));
        final BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                final String line = scanner.nextLine();
                try {
                    queue.put(line);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final SelectionKey key = channel.keyFor(selector);
                key.interestOps(OP_WRITE);
                selector.wakeup();
            }
        }).start();

        while (channel.isOpen()) {
            int selected = selector.select();
            if (selected > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (key.isConnectable()) {
                        channel.finishConnect();
                        key.interestOps(OP_WRITE);
                    } else if (key.isReadable()) {
//                        buffer.clear();
                        channel.read(buffer);
                        buffer.flip();
                        String message = new String(buffer.array(), 0, buffer.limit());
                        System.out.println(message);
                        buffer.clear();
                    } else if (key.isWritable()) {
                        final String line = queue.poll();
                        if (line != null) {
                            channel.write(ByteBuffer.wrap(line.getBytes()));
                            if (line.equals(EXIT_CODE)) {
                                System.exit(0);
                            }
                        }
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            new SimpleClient(8020).run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
