package org.example.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class SimpleServer {
    private static final int DEFAULT_PORT = 8020;
    private static final int DEFAULT_MAX_CONNECTIONS = 1000;
    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;


    private final ExecutorService threadPool = newFixedThreadPool(getRuntime().availableProcessors() * 2);;
    // in huge apps better dynamically resize the buffer pool & check shutdown
    private final Map<SocketChannel, ByteBuffer> clientBufferMap = new ConcurrentHashMap<>();
    private final int bufferSize;
    private final int port;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

//    private final ReentrantLock shutdownLock = new ReentrantLock();;
//    private ByteBuffer[] bufferPool;
//    private boolean[] availableBuffers;

    public SimpleServer(int port, int bufferSize, int maxConnections) {
        this.port = port;
        this.bufferSize = bufferSize;
        initServer(port, maxConnections);
    }

    public SimpleServer(int port, int bufferSize) {
        this(port, bufferSize, DEFAULT_MAX_CONNECTIONS);
    }

    public SimpleServer(int port) {
        this(port, DEFAULT_BUFFER_SIZE, DEFAULT_MAX_CONNECTIONS);
    }

    public SimpleServer() {
        this(DEFAULT_PORT, DEFAULT_BUFFER_SIZE, DEFAULT_MAX_CONNECTIONS);
    }

    private void initServer(int port, int maxConnections) {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            // Activate non-blocking mode
            serverSocketChannel.configureBlocking(false);

            // Create Selector for managing I/O events
            selector = Selector.open();
            // Register channel into selector for acceptable keys (IO events)
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

//            bufferPool = new ByteBuffer[maxConnections];
//            for (int i = 0; i < maxConnections; i++) {
//                bufferPool[i] = ByteBuffer.allocate(bufferSize);
//            }
//            availableBuffers = new boolean[maxConnections];
//            Arrays.fill(availableBuffers, true); // Initially all buffers available

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startServer() {
        System.out.printf("Server listening on port %s...%n", port);

        // Instead of while(true) for better clarity and graceful shutdown
        while (serverSocketChannel != null && serverSocketChannel.isOpen()) {
            int selected = 0;
            try {
                // wait for client connection (the only blocking operation)
                // return number of ready & occurred within timeout events
                selected = selector.select();

                // avoid unnecessary processing (busy waiting)
                if (selected > 0) {
                    // Select ready I/O events
                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

                    while (keyIterator.hasNext()) {
                        // Iterate each key with all IO-event info & remove it from selector
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();
                        if (key.isAcceptable()) {
                            acceptConnection(key);
                        }
                        if (key.isReadable()) {
                            readData(key);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error trying select event by selector");
                throw new RuntimeException(e);
            }
            // Check for shutdown signal
//            if (shutdownLock.isLocked()) {
//                try {
//                    // Graceful shutdown
//                    serverSocketChannel.close();
//                    waitForActiveConnectionsToClose();
//                    processRemainingMessages();
//                    clearBuffers();
//                    connPool.shutdown();
//                    break;
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    shutdownLock.unlock();
//                }
//            }
        }
    }

    private void acceptConnection(SelectionKey eventKey) {
        SocketChannel client;
        SocketAddress remoteAddress = null;
        try {
            // won't block thread if no connection is available because of configureBlocking(false)
            client = serverSocketChannel.accept();
            remoteAddress = client.getRemoteAddress();
            // Non-blocking mode for client
            client.configureBlocking(false);

            // allocate separate buffer for each connection
            client.register(selector, OP_READ);
            clientBufferMap.put(client, allocate(bufferSize));
            broadcast("[System]: " + remoteAddress + " connected!");

            System.out.println("Client connected: " + remoteAddress);
        } catch (IOException e) {
            System.err.println("Failed to connect client: " + remoteAddress);
            closeConnection(eventKey);
        }
    }

    private void readData(SelectionKey eventKey) {
        SocketChannel clientChannel;
        SocketAddress remoteAddress = null;
        int bytesRead;
        try {
            clientChannel = (SocketChannel) eventKey.channel();
            remoteAddress = clientChannel.getRemoteAddress();

            ByteBuffer buffer = clientBufferMap.get(clientChannel);

            // write data into buffer (read data from channel to buffer)
            bytesRead = clientChannel.read(buffer);
            if (bytesRead > 0) {
                // Handle received data (e.g., process message)
                threadPool.submit(() -> processData(buffer.flip(), clientChannel));
            } else if (bytesRead == -1) {
                closeConnection(eventKey);
            }
        } catch (IOException e) {
            System.err.println("Failed to read from client: " + remoteAddress);
        }
    }

    private void processData(ByteBuffer dataBuffer, SocketChannel clientChannel) {
        try {
            final SocketAddress clientName = clientChannel.getRemoteAddress();
            String message = new String(dataBuffer.array(), 0, dataBuffer.limit());

            // Process message (e.g., parse data, generate response)
            broadcast(format("[%s]: %s", clientName, message));

            // Clear buffer for reuse & mark as available
            dataBuffer.clear();
        } catch (Exception e) {
            // handle potential errors (or send error response to client)
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private void broadcast(String message) {
        final ByteBuffer buffer = wrap(message.getBytes());
        clientBufferMap.keySet().forEach(client -> {
            try {
                client.write(buffer);
                buffer.flip();
            } catch (IOException e) {
                try {
                    System.err.println("Can't send message to: " + client.getRemoteAddress());
                } catch (IOException ex) {
                    System.err.println("No client for send message: " + client);
                }
            }
        });
        buffer.clear();
    }

    private void closeConnection(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        SocketAddress remoteAddress = null;
        clientBufferMap.get(clientChannel).clear();
        clientBufferMap.remove(clientChannel);
        key.cancel();
        try {
            remoteAddress = clientChannel.getRemoteAddress();
            broadcast("[System]: " + remoteAddress + " disconnected.");

            clientChannel.close();
        } catch (IOException e) {
            System.err.printf("Error closing connection: [%s]", remoteAddress);
            throw new RuntimeException(e);
        }
        System.out.printf("Client disconnected: [%s]%n", remoteAddress);
    }

//    private void closeConnection(SocketChannel clientChannel, SelectionKey key) throws IOException {
//        if (key != null) {
//            closeConnection(key);
//        } else {
//            closeConnection(clientChannel);
//        }
//    }
//
//    private void closeConnection(SocketChannel clientChannel) {
//        SocketAddress remoteAddress = null;
//        session.remove(clientChannel);
//        try {
//            remoteAddress = clientChannel.getRemoteAddress();
//            clientChannel.close();
//        } catch (IOException e) {
//            System.err.printf("Error closing connection: [%s]", remoteAddress);
//            throw new RuntimeException(e);
//        }
//        System.out.printf("Client disconnected: [%s]", remoteAddress);
//    }

//    private void waitForActiveConnectionsToClose() {
//        // Wait for all active connections to finish
//        while (!session.isEmpty()) {
//            Iterator<SocketChannel> iterator = session.iterator();
//            while (iterator.hasNext()) {
//                SocketChannel channel = iterator.next();
//                if (!channel.isOpen()) {
//                    iterator.remove();
//                }
//            }
//            try {
//                Thread.sleep(100); // Check periodically for closed connections
//            } catch (InterruptedException e) {
//                // Handle interruption (e.g., shutdown signal)
//                break;
//            }
//        }
//    }
//
//    private void processRemainingMessages() {
//        // ... (Optional) Implement logic to process any remaining messages
//        // in the queue or buffers if applicable ...
//    }

//    private void clearBuffers() {
//        for (ByteBuffer buffer : bufferPool) {
//            buffer.clear();
//        }
//    }
//
//    public void shutdown() {
//        shutdownLock.lock();
//    }

//    private int getNextBufferIndex() throws IOException {
//        int index = nextBufferIndex.getAndIncrement() % bufferPool.length;
//        // Handle potential buffer exhaustion (reject connection)
//        if (!availableBuffers[index]) {
//            throw new IOException("Buffer pool exhausted"); // Indicate connection rejection
//        }
//        // Handle potential buffer exhaustion (wait connection)
////        while (!availableBuffers[index]) {
////            System.err.println("Warning: Buffer pool exhausted, retrying...");
////            Thread.sleep(100); // Simulate waiting for buffer availability
////            index = nextBufferIndex.getAndIncrement() % bufferPool.length;
////        }
//        // Mark buffer as unavailable
//        availableBuffers[index] = false;
//        return index;
//    }


    public static void main(String[] args) {
        new SimpleServer(8020).startServer();
    }
}
