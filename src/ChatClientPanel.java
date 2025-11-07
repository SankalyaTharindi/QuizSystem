package src;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Chat Client using NIO SocketChannel
 * Provides a GUI for real-time messaging
 */
public class ChatClientPanel extends JPanel {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;

    private SocketChannel chatChannel;
    private String username;
    private boolean isConnected = false;

    public ChatClientPanel(String username) {
        this.username = username;
        setupUI();
        connectToServer();
    }

    private void setupUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Live Chat"));

        // Chat display area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        add(scrollPane, BorderLayout.CENTER);

        // Message input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());

        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Status label
        JLabel statusLabel = new JLabel("Connecting to chat...");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        add(statusLabel, BorderLayout.NORTH);
    }

    /**
     * Connect to chat server using NIO
     */
    private void connectToServer() {
        new Thread(() -> {
            try {
                chatChannel = SocketChannel.open();
                chatChannel.connect(new InetSocketAddress("localhost", ChatServer.CHAT_PORT));
                chatChannel.configureBlocking(false); // Non-blocking mode

                // Register username
                sendObject(username);

                isConnected = true;
                SwingUtilities.invokeLater(() -> {
                    appendMessage("Connected to chat server!\n");
                    messageField.setEnabled(true);
                    sendButton.setEnabled(true);
                });

                // Start receiving messages
                startReceiving();

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    appendMessage("Failed to connect to chat server: " + e.getMessage() + "\n");
                    messageField.setEnabled(false);
                    sendButton.setEnabled(false);
                });
            }
        }).start();
    }

    /**
     * Send a chat message
     */
    private void sendMessage() {
        String content = messageField.getText().trim();
        if (content.isEmpty() || !isConnected) return;

        ChatMessage message = new ChatMessage(username, content, ChatMessage.MessageType.USER_MESSAGE);

        if (sendObject(message)) {
            // Display own message immediately
            appendMessage("[" + message.getTimestamp() + "] You: " + content + "\n");
            messageField.setText("");
        } else {
            appendMessage("Failed to send message\n");
        }
    }

    /**
     * Send object through NIO channel
     */
    private boolean sendObject(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();

            byte[] data = baos.toByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(data);

            while (buffer.hasRemaining()) {
                chatChannel.write(buffer);
            }

            return true;
        } catch (IOException e) {
            System.err.println("Error sending object: " + e.getMessage());
            return false;
        }
    }

    /**
     * Receive messages from server (non-blocking)
     */
    private void startReceiving() {
        new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(8192);

            while (isConnected && chatChannel.isOpen()) {
                try {
                    buffer.clear();
                    int bytesRead = chatChannel.read(buffer);

                    if (bytesRead > 0) {
                        buffer.flip();

                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);

                        ByteArrayInputStream bais = new ByteArrayInputStream(data);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        Object obj = ois.readObject();

                        if (obj instanceof ChatMessage) {
                            ChatMessage msg = (ChatMessage) obj;
                            SwingUtilities.invokeLater(() ->
                                appendMessage(msg.toDisplayString() + "\n"));
                        }
                    } else if (bytesRead == -1) {
                        // Connection closed
                        isConnected = false;
                        SwingUtilities.invokeLater(() ->
                            appendMessage("Disconnected from chat server\n"));
                        break;
                    }

                    // Small delay to prevent busy-waiting
                    Thread.sleep(100);

                } catch (IOException | ClassNotFoundException | InterruptedException e) {
                    if (isConnected) {
                        System.err.println("Error receiving message: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * Append message to chat area
     */
    private void appendMessage(String message) {
        chatArea.append(message);
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * Close chat connection
     */
    public void disconnect() {
        isConnected = false;
        try {
            if (chatChannel != null && chatChannel.isOpen()) {
                chatChannel.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing chat channel: " + e.getMessage());
        }
    }
}

