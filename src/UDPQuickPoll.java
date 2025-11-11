package src;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * UDP Quick Poll System
 * Demonstrates UDP's speed advantage for:
 * - Instant student polling
 * - Live feedback collection
 * - Quick surveys during quiz sessions
 */
public class UDPQuickPoll {
    public static final int UDP_POLL_PORT = 5004;
    public static final int UDP_RESPONSE_PORT = 5005;
    
    private DatagramSocket pollSocket;
    private DatagramSocket responseSocket;
    private Map<String, String> pollResponses = new ConcurrentHashMap<>();
    private Map<String, Long> responseTimestamps = new ConcurrentHashMap<>();
    private boolean pollActive = false;
    private String currentPollQuestion;
    private String[] currentOptions;
    
    public static void main(String[] args) {
        try {
            UDPQuickPoll pollServer = new UDPQuickPoll();
            pollServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void start() throws Exception {
        pollSocket = new DatagramSocket(UDP_POLL_PORT);
        responseSocket = new DatagramSocket(UDP_RESPONSE_PORT);
        
        System.out.println("🗳️ UDP Quick Poll Server started");
        System.out.println("📤 Poll broadcast port: " + UDP_POLL_PORT);
        System.out.println("📥 Response collection port: " + UDP_RESPONSE_PORT);
        
        // Start response collection thread
        startResponseCollector();
        
        // Interactive poll creation
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nCommands:");
        System.out.println("- 'poll <question> | option1 | option2 | option3 | option4' : Start a poll");
        System.out.println("- 'results' : Show current poll results");
        System.out.println("- 'close' : Close current poll and show final results");
        System.out.println("- 'demo' : Start a demo poll");
        System.out.println("- 'quit' : Stop server");
        System.out.println();
        
        while (true) {
            System.out.print("Poll Server> ");
            String input = scanner.nextLine().trim();
            
            if (input.startsWith("poll ")) {
                handlePollCommand(input.substring(5));
            } else if (input.equals("results")) {
                showCurrentResults();
            } else if (input.equals("close")) {
                closePoll();
            } else if (input.equals("demo")) {
                startDemoPoll();
            } else if (input.equals("quit")) {
                break;
            } else {
                System.out.println("Unknown command: " + input);
            }
        }
        
        pollSocket.close();
        responseSocket.close();
        System.out.println("UDP Quick Poll Server stopped.");
    }
    
    private void handlePollCommand(String pollData) {
        String[] parts = pollData.split(" \\| ");
        if (parts.length < 3) {
            System.out.println("❌ Invalid format. Use: poll <question> | option1 | option2 | ...");
            return;
        }
        
        String question = parts[0].trim();
        String[] options = Arrays.copyOfRange(parts, 1, parts.length);
        
        startPoll(question, options);
    }
    
    private void startDemoPoll() {
        String question = "What is the best feature of Java NIO?";
        String[] options = {
            "Non-blocking I/O",
            "Channel-based architecture", 
            "Selector for multiplexing",
            "Better performance"
        };
        startPoll(question, options);
    }
    
    public void startPoll(String question, String[] options) {
        if (pollActive) {
            System.out.println("⚠️ A poll is already active. Close it first.");
            return;
        }
        
        // Reset poll data
        pollResponses.clear();
        responseTimestamps.clear();
        currentPollQuestion = question;
        currentOptions = options;
        pollActive = true;
        
        System.out.println("\n🚀 Starting new poll:");
        System.out.println("❓ " + question);
        for (int i = 0; i < options.length; i++) {
            System.out.println("   " + (char)('A' + i) + ". " + options[i]);
        }
        
        // Broadcast poll via UDP
        broadcastPoll();
        
        System.out.println("📡 Poll broadcasted to all clients!");
        System.out.println("⏱️ Poll is now active - responses being collected...\n");
    }
    
    private void broadcastPoll() {
        try {
            // Create poll message: "QUICK_POLL:question:option1|option2|option3|option4"
            StringBuilder pollMessage = new StringBuilder("QUICK_POLL:");
            pollMessage.append(currentPollQuestion).append(":");
            
            for (int i = 0; i < currentOptions.length; i++) {
                if (i > 0) pollMessage.append("|");
                pollMessage.append((char)('A' + i)).append(".").append(currentOptions[i]);
            }
            
            byte[] data = pollMessage.toString().getBytes();
            
            // Broadcast to subnet (you might want to use a multicast group instead)
            InetAddress broadcast = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, 5006);
            pollSocket.send(packet);
            
        } catch (Exception e) {
            System.err.println("Failed to broadcast poll: " + e.getMessage());
        }
    }
    
    private void startResponseCollector() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    responseSocket.receive(packet);
                    
                    String response = new String(packet.getData(), 0, packet.getLength());
                    String clientIP = packet.getAddress().getHostAddress();
                    
                    if (response.startsWith("POLL_ANSWER:") && pollActive) {
                        handlePollResponse(response, clientIP);
                    }
                }
            } catch (Exception e) {
                System.err.println("Response collector error: " + e.getMessage());
            }
        }, "ResponseCollector").start();
    }
    
    private void handlePollResponse(String response, String clientIP) {
        // Format: "POLL_ANSWER:B:StudentName"
        String[] parts = response.split(":", 3);
        if (parts.length >= 3) {
            String answer = parts[1];
            String studentName = parts[2];
            
            String clientKey = clientIP + ":" + studentName;
            pollResponses.put(clientKey, answer);
            responseTimestamps.put(clientKey, System.currentTimeMillis());
            
            System.out.println("📥 Response from " + studentName + " (" + clientIP + "): " + answer);
        }
    }
    
    private void showCurrentResults() {
        if (!pollActive) {
            System.out.println("❌ No active poll");
            return;
        }
        
        System.out.println("\n📊 Current Poll Results:");
        System.out.println("❓ " + currentPollQuestion);
        System.out.println("👥 Responses: " + pollResponses.size());
        
        // Count votes for each option
        Map<String, Integer> voteCounts = new HashMap<>();
        for (int i = 0; i < currentOptions.length; i++) {
            voteCounts.put(String.valueOf((char)('A' + i)), 0);
        }
        
        for (String answer : pollResponses.values()) {
            voteCounts.put(answer, voteCounts.getOrDefault(answer, 0) + 1);
        }
        
        // Display results
        int totalVotes = pollResponses.size();
        for (int i = 0; i < currentOptions.length; i++) {
            char option = (char)('A' + i);
            int votes = voteCounts.get(String.valueOf(option));
            double percentage = totalVotes > 0 ? (votes * 100.0 / totalVotes) : 0;
            
            System.out.printf("   %c. %s: %d votes (%.1f%%)\n", 
                option, currentOptions[i], votes, percentage);
        }
        System.out.println();
    }
    
    public void closePoll() {
        if (!pollActive) {
            System.out.println("❌ No active poll to close");
            return;
        }
        
        pollActive = false;
        
        System.out.println("\n🏁 Poll Closed - Final Results:");
        showCurrentResults();
        
        // Show response times
        if (!responseTimestamps.isEmpty()) {
            System.out.println("⏱️ Response Times:");
            responseTimestamps.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    String client = entry.getKey();
                    String answer = pollResponses.get(client);
                    System.out.println("   " + client + " -> " + answer);
                });
        }
        
        System.out.println();
    }
    
    public boolean isPollActive() {
        return pollActive;
    }
    
    public String getCurrentPollQuestion() {
        return currentPollQuestion;
    }
    
    public Map<String, String> getPollResponses() {
        return new HashMap<>(pollResponses);
    }
}