package src;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NIO-based Chat Server using Selector for non-blocking I/O
 * Handles multiple client connections efficiently with a single thread
 */
public class ChatServer {
    public static final int CHAT_PORT = 5001;
    private Selector selector;
    private ServerSocketChannel serverChannel;

    // Map to store client info: channel -> username
    private final Map<SocketChannel, String> clients = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteBuffer> clientBuffers = new ConcurrentHashMap<>();

    // Chat message history
    private final List<ChatMessage> messageHistory = Collections.synchronizedList(new ArrayList<>());

    public ChatServer() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(CHAT_PORT));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Chat Server started on port " + CHAT_PORT + " (NIO mode)");
    }

    public void start() {
        try {
            while (true) {
                // Wait for events (non-blocking after timeout)
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) continue;

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        }
                    } catch (IOException e) {
                        handleClientDisconnect(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Accept new client connection
     */
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            clientBuffers.put(clientChannel, ByteBuffer.allocate(8192));

            System.out.println("New chat client connected: " + clientChannel.getRemoteAddress());
        }
    }

    /**
     * Read data from client using NIO
     */
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = clientBuffers.get(clientChannel);

        if (buffer == null) {
            buffer = ByteBuffer.allocate(8192);
            clientBuffers.put(clientChannel, buffer);
        }

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            handleClientDisconnect(key);
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();

            try {
                // Deserialize the ChatMessage object
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object obj = ois.readObject();

                if (obj instanceof ChatMessage) {
                    handleChatMessage(clientChannel, (ChatMessage) obj);
                } else if (obj instanceof String) {
                    // Handle username registration
                    String username = (String) obj;
                    clients.put(clientChannel, username);
                    System.out.println("User registered: " + username);

                    // Send message history to new client
                    sendMessageHistory(clientChannel);

                    // Broadcast join message
                    ChatMessage joinMsg = new ChatMessage("SYSTEM",
                            username + " joined the chat",
                            ChatMessage.MessageType.SYSTEM_MESSAGE);
                    broadcastMessage(joinMsg, null);
                }

                buffer.clear();
            } catch (ClassNotFoundException e) {
                System.err.println("Error deserializing message: " + e.getMessage());
                buffer.clear();
            } catch (IOException e) {
                // If deserialization fails, just clear and continue
                buffer.clear();
            }
        }
    }

    /**
     * Process and broadcast chat message
     */
    private void handleChatMessage(SocketChannel sender, ChatMessage message) {
        String username = clients.get(sender);

        if (username == null) {
            System.err.println("Received message from unregistered client");
            return;
        }

        // Add to history
        messageHistory.add(message);
        System.out.println("Chat: " + message.toDisplayString());

        // Broadcast to all clients except sender
        broadcastMessage(message, sender);
    }

    /**
     * Broadcast message to all connected clients
     */
    private void broadcastMessage(ChatMessage message, SocketChannel excludeChannel) {
        byte[] messageData = serializeMessage(message);
        if (messageData == null) return;

        ByteBuffer buffer = ByteBuffer.wrap(messageData);

        for (SocketChannel client : clients.keySet()) {
            if (client != excludeChannel && client.isOpen()) {
                try {
                    buffer.rewind();
                    client.write(buffer);
                } catch (IOException e) {
                    System.err.println("Error broadcasting to client: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Send message history to newly connected client
     */
    private void sendMessageHistory(SocketChannel client) {
        synchronized (messageHistory) {
            for (ChatMessage msg : messageHistory) {
                byte[] data = serializeMessage(msg);
                if (data != null) {
                    try {
                        client.write(ByteBuffer.wrap(data));
                    } catch (IOException e) {
                        System.err.println("Error sending history: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Serialize ChatMessage to byte array
     */
    private byte[] serializeMessage(ChatMessage message) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Error serializing message: " + e.getMessage());
            return null;
        }
    }

    /**
     * Handle client disconnection
     */
    private void handleClientDisconnect(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        String username = clients.remove(clientChannel);
        clientBuffers.remove(clientChannel);

        if (username != null) {
            System.out.println("User disconnected: " + username);

            // Broadcast leave message
            ChatMessage leaveMsg = new ChatMessage("SYSTEM",
                    username + " left the chat",
                    ChatMessage.MessageType.SYSTEM_MESSAGE);
            broadcastMessage(leaveMsg, null);
        }

        try {
            clientChannel.close();
        } catch (IOException e) {
            System.err.println("Error closing channel: " + e.getMessage());
        }

        key.cancel();
    }

    public static void main(String[] args) {
        try {
            ChatServer server = new ChatServer();
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start chat server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

