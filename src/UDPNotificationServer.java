package src;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * UDP Notification Server
 * Demonstrates UDP (connectionless, fast) communication for:
 * - Broadcasting quiz reminders
 * - System announcements
 * - Quick notifications
 */
public class UDPNotificationServer {
    public static final int UDP_BROADCAST_PORT = 5002;
    public static final int UDP_CLIENT_PORT = 5003;
    
    private DatagramSocket socket;
    private boolean running = true;
    private Set<InetAddress> registeredClients = ConcurrentHashMap.newKeySet();
    
    public static void main(String[] args) {
        try {
            UDPNotificationServer server = new UDPNotificationServer();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void start() throws Exception {
        socket = new DatagramSocket(UDP_BROADCAST_PORT);
        System.out.println("📡 UDP Notification Server started on port " + UDP_BROADCAST_PORT);
        System.out.println("📢 Broadcasting to clients on port " + UDP_CLIENT_PORT);
        
        // Start client registration listener
        startRegistrationListener();
        
        // Start periodic notifications
        startPeriodicNotifications();
        
        // Keep server running
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nCommands:");
        System.out.println("- 'remind <message>' : Send reminder to all clients");
        System.out.println("- 'announce <message>' : Send announcement");
        System.out.println("- 'clients' : Show registered clients");
        System.out.println("- 'quit' : Stop server");
        System.out.println();
        
        while (running) {
            System.out.print("UDP Server> ");
            String input = scanner.nextLine().trim();
            
            if (input.startsWith("remind ")) {
                String message = input.substring(7);
                broadcastReminder(message);
            } else if (input.startsWith("announce ")) {
                String message = input.substring(9);
                broadcastAnnouncement(message);
            } else if (input.equals("clients")) {
                showRegisteredClients();
            } else if (input.equals("quit")) {
                running = false;
            } else {
                System.out.println("Unknown command: " + input);
            }
        }
        
        socket.close();
        System.out.println("UDP Notification Server stopped.");
    }
    
    private void startRegistrationListener() {
        new Thread(() -> {
            try {
                DatagramSocket regSocket = new DatagramSocket(UDP_BROADCAST_PORT + 10); // 5012
                byte[] buffer = new byte[1024];
                
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    regSocket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.startsWith("REGISTER_CLIENT:")) {
                        InetAddress clientAddress = packet.getAddress();
                        registeredClients.add(clientAddress);
                        
                        String clientName = message.substring(16);
                        System.out.println("✅ Client registered: " + clientName + " (" + clientAddress + ")");
                        
                        // Send acknowledgment
                        String ack = "REGISTRATION_ACK:Welcome to UDP notifications!";
                        sendToClient(ack, clientAddress, UDP_CLIENT_PORT);
                    }
                }
                regSocket.close();
            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        }, "RegistrationListener").start();
    }
    
    private void startPeriodicNotifications() {
        new Thread(() -> {
            try {
                while (running) {
                    Thread.sleep(60000); // Every minute
                    
                    LocalDateTime now = LocalDateTime.now();
                    String timeMessage = "⏰ Time Update: " + 
                        now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    
                    broadcastToAllClients("SYSTEM_TIME:" + timeMessage);
                }
            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        }, "PeriodicNotifier").start();
    }
    
    public void broadcastReminder(String message) {
        String notification = "QUIZ_REMINDER:" + getCurrentTime() + " - " + message;
        broadcastToAllClients(notification);
        System.out.println("📢 Reminder sent: " + message);
    }
    
    public void broadcastAnnouncement(String message) {
        String notification = "ANNOUNCEMENT:" + getCurrentTime() + " - " + message;
        broadcastToAllClients(notification);
        System.out.println("📣 Announcement sent: " + message);
    }
    
    public void broadcastQuizStart(String quizName, int durationMinutes) {
        String notification = "QUIZ_STARTING:" + quizName + ":" + durationMinutes + " minutes";
        broadcastToAllClients(notification);
        System.out.println("🚀 Quiz start broadcast: " + quizName);
    }
    
    public void broadcastQuizEnd() {
        String notification = "QUIZ_ENDED:" + getCurrentTime();
        broadcastToAllClients(notification);
        System.out.println("🏁 Quiz end broadcast sent");
    }
    
    private void broadcastToAllClients(String message) {
        // Simple broadcast approach - send to multiple common ports
        int[] targetPorts = {5003, 5006, 5007, 5008, 5009}; // Try multiple ports
        
        for (int port : targetPorts) {
            try {
                byte[] data = message.getBytes();
                InetAddress localhost = InetAddress.getByName("localhost");
                DatagramPacket packet = new DatagramPacket(data, data.length, localhost, port);
                socket.send(packet);
            } catch (Exception e) {
                // Ignore port failures - some clients might not be listening on all ports
            }
        }
        
        // Also try broadcast address
        try {
            byte[] data = message.getBytes();
            InetAddress broadcast = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, UDP_CLIENT_PORT);
            socket.send(packet);
        } catch (Exception e) {
            // Ignore broadcast failures
        }
        
        System.out.println("📡 Broadcast sent: " + message);
    }
    
    private void sendToClient(String message, InetAddress address, int port) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Failed to send to " + address + ": " + e.getMessage());
            // Remove failed client
            registeredClients.remove(address);
        }
    }
    
    private void showRegisteredClients() {
        System.out.println("\n📱 Registered Clients (" + registeredClients.size() + "):");
        if (registeredClients.isEmpty()) {
            System.out.println("   No clients registered");
        } else {
            int i = 1;
            for (InetAddress client : registeredClients) {
                System.out.println("   " + i + ". " + client.getHostAddress());
                i++;
            }
        }
        System.out.println();
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}