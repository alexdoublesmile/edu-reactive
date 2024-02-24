package org.example.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class SimpleServer {
    private static final int DEFAULT_PORT = 8020;
    private static final int DEFAULT_MAX_CONNECTIONS = 1000;
    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;


    private final int port;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private ExecutorService connPool;
    private Set<SocketChannel> activeConnections;
    private ByteBuffer[] bufferPool;
    private boolean[] availableBuffers;
    private AtomicInteger nextBufferIndex;
    private ThreadLocal<ByteBuffer> currentBuffer;
    private ReentrantLock shutdownLock;

    public SimpleServer(int port, int bufferSize, int maxConnections) {
        this.port = port;
        initServer(port, bufferSize, maxConnections);
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

    private void initServer(int port, int bufferSize, int maxConnections) {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            // Activate non-blocking mode
            serverSocketChannel.configureBlocking(false);

            // Create Selector for managing I/O events
            selector = Selector.open();
            // Register channel into selector for acceptable keys (IO events)
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            connPool = newFixedThreadPool(getRuntime().availableProcessors() * 2);
            activeConnections = ConcurrentHashMap.newKeySet();

            // in huge apps better dynamically resize the pool
            bufferPool = new ByteBuffer[maxConnections];
            for (int i = 0; i < maxConnections; i++) {
                bufferPool[i] = ByteBuffer.allocate(bufferSize);
            }
            availableBuffers = new boolean[maxConnections];
            Arrays.fill(availableBuffers, true); // Initially all buffers available
            nextBufferIndex = new AtomicInteger(0);
            currentBuffer = new ThreadLocal<>();
            shutdownLock = new ReentrantLock();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        System.out.println("Server listening on port: " + port);

        // Instead of while(true) for better clarity and graceful shutdown
        while (serverSocketChannel.isOpen()) {
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
                        // Iterate each key with all IO event information
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();
                        if (key.isAcceptable()) {
                            // Accept new client connection
                            handleAccept(key);
                        }
                        if (key.isReadable()) {
                            // Read data from a client
                            handleRead(key);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error trying select event by selector");
                throw new RuntimeException(e);
            }
            // Check for shutdown signal
            if (shutdownLock.isLocked()) {
                try {
                    // Graceful shutdown
                    serverSocketChannel.close();
                    waitForActiveConnectionsToClose();
                    processRemainingMessages();
                    clearBuffers();
                    connPool.shutdown();
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    shutdownLock.unlock();
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) {
        SocketChannel clientChannel;
        SocketAddress remoteAddress = null;
        try {
            // won't block thread if no connection is available because of configureBlocking(false)
            clientChannel = serverSocketChannel.accept();
            remoteAddress = clientChannel.getRemoteAddress();
            // Non-blocking mode for client
            clientChannel.configureBlocking(false);

            try {
                // get available buffer and store it in ThreadLocal
                final int bufferIdx = getNextBufferIndex();
                currentBuffer.set(bufferPool[bufferIdx]);

                // Register client channel into selector for readable keys & buffer idx attach
                clientChannel.register(selector, SelectionKey.OP_READ, bufferIdx);
                activeConnections.add(clientChannel);
                System.out.println("Client connected: " + remoteAddress);
            } catch (IOException e) {
                // Buffer exhaustion: close connection and send rejection message (optional)
                System.err.println("Failed to accept connection due to buffer exhaustion.");
                closeConnection(clientChannel);
                // ... (Optional) Send rejection message to client
            }
        } catch (IOException e) {
            System.err.println("Failed to connect client: " + remoteAddress);
            closeConnection(key);
        }
    }

    private int getNextBufferIndex() throws IOException {
        int index = nextBufferIndex.getAndIncrement() % bufferPool.length;
        // Handle potential buffer exhaustion (reject connection)
        if (!availableBuffers[index]) {
            throw new IOException("Buffer pool exhausted"); // Indicate connection rejection
        }
        // Handle potential buffer exhaustion (wait connection)
//        while (!availableBuffers[index]) {
//            System.err.println("Warning: Buffer pool exhausted, retrying...");
//            Thread.sleep(100); // Simulate waiting for buffer availability
//            index = nextBufferIndex.getAndIncrement() % bufferPool.length;
//        }
        // Mark buffer as unavailable
        availableBuffers[index] = false;
        return index;
    }

    private void handleRead(SelectionKey key) {
        SocketChannel clientChannel;
        SocketAddress remoteAddress = null;
        String attachmentString = "";
        int bytesRead;
        try {
            clientChannel = (SocketChannel) key.channel();
            remoteAddress = clientChannel.getRemoteAddress();

            // Retrieve buffer from ThreadLocal
            ByteBuffer buffer = currentBuffer.get();

            // write data into buffer (read data from channel to buffer)
            bytesRead = clientChannel.read(buffer);
            if (bytesRead > 0) {
                // Handle received data (e.g., process message)
                buffer.flip();
                connPool.submit(() -> handleReceivedMessage(buffer, key, clientChannel));
            } else if (bytesRead == -1) {
                closeConnection(clientChannel, key);
            }
        } catch (IOException e) {
            System.err.println("Failed to read from client: " + remoteAddress);
        }
    }

    private void handleReceivedMessage(ByteBuffer buffer, SelectionKey key, SocketChannel clientChannel) {
        try {
            String message = new String(buffer.array(), 0, buffer.limit());

            // Process message (e.g., parse data, generate response)
            System.out.printf("[%s]: %s", clientChannel.getRemoteAddress(), message);

            // ... handle message and send response ...
            String response = "Server received your message: " + message;
            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();

            // Send response to client
            clientChannel.write(buffer);
            System.out.println("Sent response to " + clientChannel.getRemoteAddress() + ": " + response);

            // Clear buffer for reuse & mark as available
            buffer.clear();
            int bufferIndex = (int) key.attachment();
            availableBuffers[bufferIndex] = true;
        } catch (Exception e) {
            // handle potential errors (or send error response to client)
            System.err.println("Error processing message: " + e.getMessage());
        } finally {
            // Remove buffer from ThreadLocal
            currentBuffer.remove();
        }
    }

    private void closeConnection(SocketChannel clientChannel, SelectionKey key) throws IOException {
        if (key != null) {
            closeConnection(key);
        } else {
            closeConnection(clientChannel);
        }
    }

    private void closeConnection(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        SocketAddress remoteAddress = null;
        activeConnections.remove(clientChannel);
        key.cancel();
        try {
            remoteAddress = clientChannel.getRemoteAddress();
            clientChannel.close();
        } catch (IOException e) {
            System.err.printf("Error closing connection: [%s]", remoteAddress);
            throw new RuntimeException(e);
        }
        System.out.printf("Client disconnected: [%s]", remoteAddress);
    }

    private void closeConnection(SocketChannel clientChannel) {
        SocketAddress remoteAddress = null;
        activeConnections.remove(clientChannel);
        try {
            remoteAddress = clientChannel.getRemoteAddress();
            clientChannel.close();
        } catch (IOException e) {
            System.err.printf("Error closing connection: [%s]", remoteAddress);
            throw new RuntimeException(e);
        }
        System.out.printf("Client disconnected: [%s]", remoteAddress);
    }

    private void waitForActiveConnectionsToClose() {
        // Wait for all active connections to finish
        while (!activeConnections.isEmpty()) {
            Iterator<SocketChannel> iterator = activeConnections.iterator();
            while (iterator.hasNext()) {
                SocketChannel channel = iterator.next();
                if (!channel.isOpen()) {
                    iterator.remove();
                }
            }
            try {
                Thread.sleep(100); // Check periodically for closed connections
            } catch (InterruptedException e) {
                // Handle interruption (e.g., shutdown signal)
                break;
            }
        }
    }

    private void processRemainingMessages() {
        // ... (Optional) Implement logic to process any remaining messages
        // in the queue or buffers if applicable ...
    }

    private void clearBuffers() {
        for (ByteBuffer buffer : bufferPool) {
            buffer.clear();
        }
    }

    public void shutdown() {
        shutdownLock.lock();
    }
}
