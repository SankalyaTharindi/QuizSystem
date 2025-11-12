package src;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 * Simple UDP Notification Test
 * A basic UDP notification system that works reliably
 */
public class SimpleUDPNotifier {
    private static final int NOTIFICATION_PORT = 5010;
    private DatagramSocket socket;
    private Map<String, ClientInfo> registeredClients = new HashMap<>();
    
    private static class ClientInfo {
        String name;
        InetAddress address;
        int port;
        
        ClientInfo(String name, InetAddress address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
        }
    }
    
    public static void main(String[] args) {
        try {
            SimpleUDPNotifier notifier = new SimpleUDPNotifier();
            notifier.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void start() throws Exception {
        socket = new DatagramSocket(NOTIFICATION_PORT);
        System.out.println("📡 Simple UDP Notifier started on port " + NOTIFICATION_PORT);
        
        // Start registration listener
        startRegistrationListener();
        
        System.out.println("Commands:");
        System.out.println("- notify <message> : Send notification");
        System.out.println("- quiz : Send quiz reminder");
        System.out.println("- time : Send time warning");
        System.out.println("- clients : Show registered clients");
        System.out.println("- quit : Exit");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        
        while ((input = reader.readLine()) != null) {
            if (input.equals("quit")) {
                break;
            } else if (input.startsWith("notify ")) {
                String message = input.substring(7);
                sendNotification("NOTIFICATION:" + message);
            } else if (input.equals("quiz")) {
                sendNotification("QUIZ_REMINDER:Quiz ending in 2 minutes! Finish your answers.");
            } else if (input.equals("time")) {
                sendNotification("TIME_WARNING:⏰ 1 minute remaining!");
            } else if (input.equals("clients")) {
                showRegisteredClients();
            } else {
                System.out.println("Unknown command: " + input);
            }
        }
        
        socket.close();
        System.out.println("UDP Notifier stopped.");
    }
    
    private void startRegistrationListener() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    
                    if (message.startsWith("REGISTER:")) {
                        handleClientRegistration(message, packet.getAddress());
                    }
                }
            } catch (Exception e) {
                System.err.println("Registration listener error: " + e.getMessage());
            }
        }, "RegistrationListener").start();
    }
    
    private void handleClientRegistration(String message, InetAddress clientAddress) {
        try {
            // Parse: "REGISTER:StudentName:12345"
            String[] parts = message.split(":");
            if (parts.length == 3) {
                String clientName = parts[1];
                int clientPort = Integer.parseInt(parts[2]);
                
                ClientInfo client = new ClientInfo(clientName, clientAddress, clientPort);
                registeredClients.put(clientName, client);
                
                System.out.println("✅ Client registered: " + clientName + " (" + clientAddress + ":" + clientPort + ")");
                
                // Send welcome message
                sendToClient("NOTIFICATION:Welcome! You are now registered for UDP notifications.", client);
            }
        } catch (Exception e) {
            System.err.println("Error handling client registration: " + e.getMessage());
        }
    }
    
    private void showRegisteredClients() {
        System.out.println("\n📱 Registered Clients (" + registeredClients.size() + "):");
        if (registeredClients.isEmpty()) {
            System.out.println("   No clients registered");
        } else {
            int i = 1;
            for (ClientInfo client : registeredClients.values()) {
                System.out.println("   " + i + ". " + client.name + " (" + client.address + ":" + client.port + ")");
                i++;
            }
        }
        System.out.println();
    }
    
    
    private void sendNotification(String message) {
        if (registeredClients.isEmpty()) {
            System.out.println("⚠️ No registered clients to notify");
            return;
        }
        
        int sent = 0;
        for (ClientInfo client : registeredClients.values()) {
            if (sendToClient(message, client)) {
                sent++;
            }
        }
        
        System.out.println("📤 Sent notification to " + sent + " clients: " + message);
    }
    
    private boolean sendToClient(String message, ClientInfo client) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, client.address, client.port);
            socket.send(packet);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send to " + client.name + ": " + e.getMessage());
            // Remove failed client
            registeredClients.remove(client.name);
            return false;
        }
    }
}