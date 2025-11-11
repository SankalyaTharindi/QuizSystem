package src;

import java.net.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP Client Handler
 * Handles UDP communication for:
 * - Receiving notifications
 * - Participating in quick polls
 * - Real-time announcements
 */
public class UDPClientHandler {
    public static final int UDP_NOTIFICATION_PORT = 5003;
    public static final int UDP_POLL_LISTEN_PORT = 5006;
    public static final int UDP_POLL_RESPONSE_PORT = 5005;
    public static final int UDP_REGISTRATION_PORT = 5012;
    
    private DatagramSocket notificationSocket;
    private DatagramSocket pollSocket;
    private DatagramSocket responseSocket;
    private JFrame parentFrame;
    private String clientName;
    private AtomicBoolean running = new AtomicBoolean(true);
    
    public UDPClientHandler(JFrame parent, String clientName) throws Exception {
        this.parentFrame = parent;
        this.clientName = clientName;
        
        // Initialize sockets - use different approach for notification listening
        try {
            this.notificationSocket = new DatagramSocket(UDP_NOTIFICATION_PORT);
        } catch (BindException e) {
            // If port is busy, try a random port
            this.notificationSocket = new DatagramSocket();
            System.out.println("⚠️ Using random port for notifications: " + notificationSocket.getLocalPort());
        }
        
        try {
            this.pollSocket = new DatagramSocket(UDP_POLL_LISTEN_PORT);
        } catch (BindException e) {
            // If port is busy, try a random port  
            this.pollSocket = new DatagramSocket();
            System.out.println("⚠️ Using random port for polls: " + pollSocket.getLocalPort());
        }
        
        this.responseSocket = new DatagramSocket();
        
        // Register with UDP server first
        registerWithServer();
        
        // Start listening threads
        startNotificationListener();
        startPollListener();
        
        System.out.println("📡 UDP Client Handler initialized for: " + clientName);
    }
    
    private void registerWithServer() {
        try {
            String registrationMessage = "REGISTER_CLIENT:" + clientName;
            byte[] data = registrationMessage.getBytes();
            
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(
                data, data.length, serverAddress, UDP_REGISTRATION_PORT
            );
            
            responseSocket.send(packet);
            System.out.println("📝 Registered with UDP Notification Server");
        } catch (Exception e) {
            System.err.println("Failed to register with UDP server: " + e.getMessage());
        }
    }
    
    private void startNotificationListener() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                
                while (running.get()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    notificationSocket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    handleNotification(message);
                }
            } catch (SocketException e) {
                if (running.get()) {
                    System.err.println("Notification listener error: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Notification listener error: " + e.getMessage());
            }
        }, "UDP-NotificationListener").start();
    }
    
    private void startPollListener() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                
                while (running.get()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    pollSocket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    
                    if (message.startsWith("QUICK_POLL:")) {
                        handleQuickPoll(message, packet.getAddress());
                    }
                }
            } catch (SocketException e) {
                if (running.get()) {
                    System.err.println("Poll listener error: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Poll listener error: " + e.getMessage());
            }
        }, "UDP-PollListener").start();
    }
    
    private void handleNotification(String message) {
        SwingUtilities.invokeLater(() -> {
            String displayMessage = "";
            String title = "Quiz Notification";
            int messageType = JOptionPane.INFORMATION_MESSAGE;
            
            if (message.startsWith("QUIZ_REMINDER:")) {
                displayMessage = "📢 " + message.substring(14);
                title = "Quiz Reminder";
                messageType = JOptionPane.WARNING_MESSAGE;
            } else if (message.startsWith("ANNOUNCEMENT:")) {
                displayMessage = "📣 " + message.substring(13);
                title = "Announcement";
                messageType = JOptionPane.INFORMATION_MESSAGE;
            } else if (message.startsWith("QUIZ_STARTING:")) {
                String[] parts = message.substring(14).split(":");
                if (parts.length >= 2) {
                    displayMessage = "🚀 Quiz Starting: " + parts[0] + "\nDuration: " + parts[1];
                }
                title = "Quiz Starting!";
                messageType = JOptionPane.WARNING_MESSAGE;
            } else if (message.startsWith("QUIZ_ENDED:")) {
                displayMessage = "🏁 Quiz has ended at " + message.substring(11);
                title = "Quiz Ended";
                messageType = JOptionPane.INFORMATION_MESSAGE;
            } else if (message.startsWith("SYSTEM_TIME:")) {
                displayMessage = message.substring(12);
                title = "System Update";
                messageType = JOptionPane.PLAIN_MESSAGE;
            } else if (message.startsWith("REGISTRATION_ACK:")) {
                displayMessage = "✅ " + message.substring(17);
                title = "UDP Registration";
                messageType = JOptionPane.INFORMATION_MESSAGE;
            } else {
                displayMessage = message;
            }
            
            // Show notification popup
            JOptionPane.showMessageDialog(parentFrame, displayMessage, title, messageType);
        });
    }
    
    private void handleQuickPoll(String pollData, InetAddress serverAddress) {
        SwingUtilities.invokeLater(() -> {
            // Parse poll: "QUICK_POLL:What is OOP?:A.Inheritance|B.Encapsulation|C.Polymorphism|D.All of above"
            String[] parts = pollData.split(":", 3);
            if (parts.length >= 3) {
                String question = parts[1];
                String optionsData = parts[2];
                
                // Parse options: "A.Inheritance|B.Encapsulation|C.Polymorphism|D.All of above"
                String[] optionParts = optionsData.split("\\|");
                String[] cleanOptions = new String[optionParts.length];
                
                for (int i = 0; i < optionParts.length; i++) {
                    cleanOptions[i] = optionParts[i]; // Keep the "A.Inheritance" format
                }
                
                // Create poll dialog with timer
                JDialog pollDialog = createPollDialog(question, cleanOptions, serverAddress);
                pollDialog.setVisible(true);
            }
        });
    }
    
    private JDialog createPollDialog(String question, String[] options, InetAddress serverAddress) {
        JDialog dialog = new JDialog(parentFrame, "Quick Poll - Respond Fast!", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Create UI components
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Question label
        JLabel questionLabel = new JLabel("<html><b>" + question + "</b></html>");
        questionLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(questionLabel);
        panel.add(Box.createVerticalStrut(10));
        
        // Options as radio buttons
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButton[] radioButtons = new JRadioButton[options.length];
        
        for (int i = 0; i < options.length; i++) {
            radioButtons[i] = new JRadioButton(options[i]);
            buttonGroup.add(radioButtons[i]);
            panel.add(radioButtons[i]);
        }
        
        panel.add(Box.createVerticalStrut(10));
        
        // Submit button
        JButton submitButton = new JButton("Submit Answer");
        submitButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        submitButton.addActionListener(e -> {
            for (int i = 0; i < radioButtons.length; i++) {
                if (radioButtons[i].isSelected()) {
                    char answer = (char)('A' + i);
                    sendPollResponse(String.valueOf(answer), serverAddress);
                    dialog.dispose();
                    
                    // Show confirmation
                    JOptionPane.showMessageDialog(parentFrame, 
                        "✅ Your answer '" + answer + "' has been submitted!",
                        "Poll Response Sent", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
            
            JOptionPane.showMessageDialog(dialog, 
                "Please select an option before submitting!", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
        });
        
        panel.add(submitButton);
        
        // Auto-close timer (30 seconds)
        Timer autoCloseTimer = new Timer(30000, e -> {
            dialog.dispose();
            JOptionPane.showMessageDialog(parentFrame, 
                "⏰ Poll timed out!", "Poll Timeout", JOptionPane.WARNING_MESSAGE);
        });
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
        
        dialog.add(panel);
        return dialog;
    }
    
    private void sendPollResponse(String answer, InetAddress serverAddress) {
        try {
            String response = "POLL_ANSWER:" + answer + ":" + clientName;
            byte[] data = response.getBytes();
            
            DatagramPacket packet = new DatagramPacket(
                data, data.length, serverAddress, UDP_POLL_RESPONSE_PORT
            );
            
            responseSocket.send(packet);
            System.out.println("📤 Poll response sent: " + answer);
        } catch (Exception e) {
            System.err.println("Failed to send poll response: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(parentFrame, 
                    "❌ Failed to send response: " + e.getMessage(),
                    "Send Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    public void shutdown() {
        running.set(false);
        
        if (notificationSocket != null) notificationSocket.close();
        if (pollSocket != null) pollSocket.close();
        if (responseSocket != null) responseSocket.close();
        
        System.out.println("📡 UDP Client Handler shutdown for: " + clientName);
    }
    
    // Utility method to send custom notification requests
    public void requestNotification(String message) {
        try {
            String request = "NOTIFICATION_REQUEST:" + clientName + ":" + message;
            byte[] data = request.getBytes();
            
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(
                data, data.length, serverAddress, UDPNotificationServer.UDP_BROADCAST_PORT
            );
            
            responseSocket.send(packet);
        } catch (Exception e) {
            System.err.println("Failed to request notification: " + e.getMessage());
        }
    }
}