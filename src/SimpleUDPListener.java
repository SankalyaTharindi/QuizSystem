package src;

import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple UDP Listener for QuizClient
 * Listens for UDP notifications and shows popups
 */
public class SimpleUDPListener {
    private DatagramSocket socket;
    private JFrame parentFrame;
    private String clientName;
    private AtomicBoolean running = new AtomicBoolean(true);
    private int actualPort;
    
    public SimpleUDPListener(JFrame parent, String clientName) {
        this.parentFrame = parent;
        this.clientName = clientName;
        
        try {
            // Use random available port to avoid conflicts
            this.socket = new DatagramSocket(0); // 0 = random available port
            this.actualPort = socket.getLocalPort();
            System.out.println("📡 UDP Listener started on port " + actualPort + " for: " + clientName);
            
            // Register our port with the notification server
            registerWithNotificationServer();
            
            startListening();
        } catch (Exception e) {
            System.err.println("❌ Failed to start UDP listener: " + e.getMessage());
        }
    }
    
    private void registerWithNotificationServer() {
        try {
            // Send our listening port to the notification server
            String registration = "REGISTER:" + clientName + ":" + actualPort;
            byte[] data = registration.getBytes();
            
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, 5010);
            
            socket.send(packet);
            System.out.println("📝 Registered with UDP server: " + clientName + " on port " + actualPort);
        } catch (Exception e) {
            System.err.println("Failed to register with UDP server: " + e.getMessage());
        }
    }
    
    private void startListening() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                
                while (running.get()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    // Ensure proper UTF-8 decoding
                    String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    handleNotification(message);
                }
            } catch (SocketException e) {
                if (running.get()) {
                    System.err.println("UDP Listener error: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("UDP Listener error: " + e.getMessage());
            }
        }, "UDP-Listener-" + clientName).start();
    }
    
    private void handleNotification(String receivedMessage) {
        System.out.println("📥 UDP message received: " + receivedMessage);
        
        SwingUtilities.invokeLater(() -> {
            String displayMessage = "";
            String title = "Quiz Notification";
            int messageType = JOptionPane.INFORMATION_MESSAGE;
            
            // Clean the message and ensure proper encoding
            final String message = receivedMessage.trim();
            
            if (message.startsWith("QUIZ_START:")) {
                displayMessage = message.substring(11);
                title = "Quiz Started!";
                messageType = JOptionPane.INFORMATION_MESSAGE;
            } else if (message.startsWith("QUIZ_END:")) {
                displayMessage = message.substring(9);
                title = "Quiz Ended";
                messageType = JOptionPane.WARNING_MESSAGE;
            } else if (message.startsWith("QUIZ_REMINDER:")) {
                displayMessage = message.substring(14);
                title = "Quiz Reminder";
                messageType = JOptionPane.WARNING_MESSAGE;
            } else if (message.startsWith("TIME_WARNING:")) {
                displayMessage = message.substring(13);
                title = "⚠️ TIME WARNING";
                messageType = JOptionPane.WARNING_MESSAGE;
            } else if (message.startsWith("NOTIFICATION:")) {
                displayMessage = message.substring(13);
                title = "Notification";
                messageType = JOptionPane.INFORMATION_MESSAGE;
            } else if (message.startsWith("SYSTEM_PING:")) {
                // Don't show system pings as popups, just log them
                System.out.println("System ping: " + message.substring(12));
                return;
            } else {
                displayMessage = message;
            }
            
            // Ensure the message is properly encoded and readable
            displayMessage = displayMessage.trim();
            if (displayMessage.isEmpty()) {
                displayMessage = "Notification received";
            }
            
            // Create a more robust dialog
            JDialog dialog = new JDialog(parentFrame, title, false);
            dialog.setSize(450, 200);
            dialog.setLocationRelativeTo(parentFrame);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            // Add padding manually since we removed BorderFactory import
            
            // Message area with proper text handling
            JTextArea messageArea = new JTextArea(displayMessage);
            messageArea.setEditable(false);
            messageArea.setWrapStyleWord(true);
            messageArea.setLineWrap(true);
            messageArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
            messageArea.setBackground(dialog.getBackground());
            
            JScrollPane scrollPane = new JScrollPane(messageArea);
            scrollPane.setPreferredSize(new Dimension(400, 80));
            
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton okButton = new JButton("OK");
            okButton.setPreferredSize(new Dimension(80, 30));
            okButton.addActionListener(e -> dialog.dispose());
            buttonPanel.add(okButton);
            
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            dialog.add(mainPanel);
            dialog.setVisible(true);
            
            // Auto-close time depends on message urgency
            int autoCloseTime = 15000; // 15 seconds default
            if (displayMessage.contains("SECONDS LEFT") || displayMessage.contains("TIME'S UP")) {
                autoCloseTime = 30000; // 30 seconds for urgent messages
            }
            
            Timer autoClose = new Timer(autoCloseTime, e -> dialog.dispose());
            autoClose.setRepeats(false);
            autoClose.start();
        });
    }
    
    public void shutdown() {
        running.set(false);
        if (socket != null) {
            socket.close();
        }
        System.out.println("📡 UDP Listener shutdown for: " + clientName);
    }
}