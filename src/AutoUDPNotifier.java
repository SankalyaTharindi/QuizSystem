package src;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Automatic UDP Notifier
 * Automatically sends time-based notifications during the 5-minute quiz
 */
public class AutoUDPNotifier {
    private static final int NOTIFICATION_PORT = 5010;
    private static final int QUIZ_DURATION_SECONDS = 300; // 5 minutes
    private DatagramSocket socket;
    private Map<String, ClientInfo> registeredClients = new HashMap<>();
    private Timer notificationTimer;
    
    private static class ClientInfo {
        String name;
        InetAddress address;
        int port;
        boolean isTeacher;
        Timer individualTimer;
        long quizStartTime;
        
        ClientInfo(String name, InetAddress address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
            this.isTeacher = name.toLowerCase().contains("teacher");
            this.individualTimer = null;
            this.quizStartTime = 0;
        }
    }
    
    public static void main(String[] args) {
        try {
            AutoUDPNotifier notifier = new AutoUDPNotifier();
            notifier.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void start() throws Exception {
        socket = new DatagramSocket(NOTIFICATION_PORT);
        System.out.println("🤖 Auto UDP Notifier started on port " + NOTIFICATION_PORT);
        System.out.println("⏰ Waiting for quiz events to automatically send notifications...");
        System.out.println("🎯 Will auto-start timer when students begin quiz!");
        
        // Start registration listener
        startRegistrationListener();
        
        // Start automatic notifications
        startAutomaticNotifications();
        
        // Start command listener (for automatic commands from QuizServer)
        startCommandListener();
        
        System.out.println("\n🚀 System ready! Quiz notifications will start automatically.");
        System.out.println("📱 Students will get time warnings during their 5-minute quiz.");
        System.out.println("⌨️  Manual commands (optional):");
        System.out.println("   - start : Manually start timer");
        System.out.println("   - stop : Stop current timer");
        System.out.println("   - notify <message> : Send custom notification");
        System.out.println("   - clients : Show registered clients");
        System.out.println("   - quit : Exit\n");
        
        // Keep the server running but don't block on input
        // The command listener will handle automatic commands
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        
        while ((input = reader.readLine()) != null) {
            if (input.equals("quit")) {
                break;
            } else if (input.equals("start")) {
                startQuizTimer();
            } else if (input.equals("stop")) {
                stopQuizTimer();
            } else if (input.startsWith("notify ")) {
                String message = input.substring(7);
                sendNotification("NOTIFICATION:" + message);
            } else if (input.equals("clients")) {
                showRegisteredClients();
            } else if (input.trim().isEmpty()) {
                // Ignore empty lines
                continue;
            } else {
                System.out.println("⚠️  Unknown command: " + input);
            }
        }
        
        socket.close();
        System.out.println("Auto UDP Notifier stopped.");
    }
    
    private void startQuizTimer() {
        if (notificationTimer != null) {
            notificationTimer.cancel();
        }
        
        notificationTimer = new Timer("QuizNotificationTimer");
        System.out.println("🚀 Starting 5-minute quiz timer with automatic notifications...");
        
        // Send quiz start notification
        sendNotification("QUIZ_START:5-minute quiz has begun! Good luck!");
        
        // Schedule notifications at specific times
        scheduleNotification(60, "NOTIFICATION:4 minutes remaining! Keep going!");           // After 1 min (4 left)
        scheduleNotification(120, "NOTIFICATION:3 minutes remaining! You're doing great!");   // After 2 min (3 left) 
        scheduleNotification(180, "TIME_WARNING:⏰ 2 minutes remaining! Speed up!");          // After 3 min (2 left)
        scheduleNotification(240, "TIME_WARNING:⚠️ 1 MINUTE LEFT! Finish your answers!");    // After 4 min (1 left)
        scheduleNotification(270, "TIME_WARNING:🚨 30 SECONDS LEFT! SUBMIT NOW!");           // After 4.5 min (30s left)
        scheduleNotification(285, "TIME_WARNING:🚨 15 SECONDS! SUBMIT IMMEDIATELY!");        // After 4.75 min (15s left)
        scheduleNotification(300, "QUIZ_END:⏰ TIME'S UP! Quiz has ended.");                 // After 5 min (end)
    }
    
    private void scheduleNotification(int delaySeconds, String message) {
        notificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendNotification(message);
                int remainingMinutes = (QUIZ_DURATION_SECONDS - delaySeconds) / 60;
                int remainingSeconds = (QUIZ_DURATION_SECONDS - delaySeconds) % 60;
                System.out.println("⏰ Sent: " + message + " (Time left: " + remainingMinutes + ":" + 
                    String.format("%02d", remainingSeconds) + ")");
            }
        }, delaySeconds * 1000L);
    }
    
    private void stopQuizTimer() {
        if (notificationTimer != null) {
            notificationTimer.cancel();
            notificationTimer = null;
            sendNotification("NOTIFICATION:Quiz timer has been stopped by teacher.");
            System.out.println("🛑 Quiz timer stopped.");
        } else {
            System.out.println("⚠️ No active timer to stop.");
        }
    }
    
    private void startAutomaticNotifications() {
        // This method is kept for future enhancements if needed
        // Individual student timers start automatically when they begin their quiz
        // No periodic pings needed - each student has their own timer
    }
    
    private void startCommandListener() {
        new Thread(() -> {
            try {
                // Listen on a different port for automatic commands from QuizServer
                DatagramSocket commandSocket = new DatagramSocket(5020);
                System.out.println("🎯 Command listener started on port 5020 for automatic quiz events");
                
                byte[] buffer = new byte[1024];
                
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    commandSocket.receive(packet);
                    
                    String command = new String(packet.getData(), 0, packet.getLength());
                    handleAutomaticCommand(command);
                }
            } catch (Exception e) {
                System.err.println("Command listener error: " + e.getMessage());
            }
        }, "CommandListener").start();
    }
    
    /**
     * Start individual quiz timer for a specific student
     * Retries if student not yet registered
     */
    private void startIndividualQuizTimer(String studentName) {
        ClientInfo student = registeredClients.get(studentName);
        
        if (student == null) {
            System.out.println("⚠️  Student '" + studentName + "' not found yet - will retry in 1 second...");
            // Retry after 1 second to allow registration to complete
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    startIndividualQuizTimerNow(studentName);
                }
            }, 1000);
            return;
        }
        
        startIndividualQuizTimerNow(studentName);
    }
    
    /**
     * Actually start the timer (internal method)
     */
    private void startIndividualQuizTimerNow(String studentName) {
        ClientInfo student = registeredClients.get(studentName);
        
        if (student == null) {
            System.out.println("❌ Student '" + studentName + "' still not found after retry - timer not started");
            return;
        }
        
        if (student.isTeacher) {
            System.out.println("🚫 Not starting timer for teacher: " + studentName);
            return;
        }
        
        // Stop any existing timer for this student
        if (student.individualTimer != null) {
            student.individualTimer.cancel();
        }
        
        // Create new timer for this student
        student.individualTimer = new Timer("QuizTimer-" + studentName);
        student.quizStartTime = System.currentTimeMillis();
        
        System.out.println("🚀 Starting 5-minute quiz timer for: " + studentName);
        
        // Send quiz start notification (only to this student)
        sendToClient("QUIZ_START:5-minute quiz has begun! Good luck!", student);
        
        // Schedule notifications at specific times for this student
        scheduleIndividualNotification(student, 60, "NOTIFICATION:4 minutes remaining! Keep going!");
        scheduleIndividualNotification(student, 120, "NOTIFICATION:3 minutes remaining! You're doing great!");
        scheduleIndividualNotification(student, 180, "TIME_WARNING:⏰ 2 minutes remaining! Speed up!");
        scheduleIndividualNotification(student, 240, "TIME_WARNING:⚠️ 1 MINUTE LEFT! Finish your answers!");
        scheduleIndividualNotification(student, 270, "TIME_WARNING:🚨 30 SECONDS LEFT! ");
        scheduleIndividualNotification(student, 285, "TIME_WARNING:🚨 15 SECONDS!");
        
    }
    
    /**
     * Schedule a notification for an individual student
     */
    private void scheduleIndividualNotification(ClientInfo student, int delaySeconds, String message) {
        student.individualTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendToClient(message, student);
                int remainingMinutes = (QUIZ_DURATION_SECONDS - delaySeconds) / 60;
                int remainingSeconds = (QUIZ_DURATION_SECONDS - delaySeconds) % 60;
                System.out.println("⏰ Sent to " + student.name + ": " + message + 
                    " (Time left: " + remainingMinutes + ":" + String.format("%02d", remainingSeconds) + ")");
            }
        }, delaySeconds * 1000L);
    }
    
    /**
     * Stop individual quiz timer for a specific student
     */
    private void stopIndividualQuizTimer(String studentName) {
        ClientInfo student = registeredClients.get(studentName);
        
        if (student == null) {
            System.out.println("⚠️  Student '" + studentName + "' not found");
            return;
        }
        
        if (student.individualTimer != null) {
            student.individualTimer.cancel();
            student.individualTimer = null;
            sendToClient("NOTIFICATION:Your quiz timer has been stopped.", student);
            System.out.println("🛑 Quiz timer stopped for: " + studentName);
        } else {
            System.out.println("⚠️ No active timer for: " + studentName);
        }
    }
    
    private void handleAutomaticCommand(String command) {
        System.out.println("🎯 Received automatic command: " + command);
        
        if (command.startsWith("START_QUIZ_TIMER:")) {
            // Format: START_QUIZ_TIMER:StudentName
            String studentName = command.substring(17);
            System.out.println("🚀 Student '" + studentName + "' started quiz - starting their individual timer!");
            startIndividualQuizTimer(studentName);
        } else if (command.startsWith("STOP_QUIZ_TIMER:")) {
            String studentName = command.substring(16);
            System.out.println("🛑 Quiz ended for " + studentName + " - stopping their timer");
            stopIndividualQuizTimer(studentName);
        } else if (command.equals("START_QUIZ_TIMER")) {
            // Old format - still supported but deprecated
            System.out.println("⚠️  Using old global timer format - should use per-student timers!");
            startQuizTimer();
        } else if (command.equals("STOP_QUIZ_TIMER")) {
            stopQuizTimer();
        } else if (command.startsWith("QUIZ_EVENT:")) {
            // Format: QUIZ_EVENT:Student aa has started the quiz!
            // OR: QUIZ_EVENT:SCORE:studentName:aa finished the quiz! Score: 5/10
            String eventMessage = command.substring(11);
            
            if (eventMessage.startsWith("SCORE:")) {
                // Extract student name and score message
                String[] parts = eventMessage.split(":", 3);
                if (parts.length >= 3) {
                    String studentName = parts[1];
                    String scoreMessage = parts[2];
                    // Send score only to that student and teachers
                    sendNotificationToStudentAndTeachers(studentName, "NOTIFICATION:" + scoreMessage);
                }
            } else if (eventMessage.contains("started the quiz")) {
                // Send "student started" notification only to teachers
                sendNotificationToTeachers("NOTIFICATION:" + eventMessage);
            } else {
                // Other general notifications go to everyone
                sendNotification("NOTIFICATION:" + eventMessage);
            }
        }
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
                    } else if (message.startsWith("AUTO_START:")) {
                        String studentName = message.substring(11);
                        System.out.println("🚀 Auto-starting quiz timer for student: " + studentName);
                        startQuizTimer();
                    } else if (message.startsWith("AUTO_STOP:")) {
                        String studentName = message.substring(10);
                        System.out.println("🛑 Auto-stopping quiz timer for student: " + studentName);
                        stopQuizTimer();
                    } else if (message.startsWith("AUTO_NOTIFY:")) {
                        String notificationMessage = message.substring(12);
                        System.out.println("📤 Auto-sending notification: " + notificationMessage);
                        sendNotification("NOTIFICATION:" + notificationMessage);
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
                
                // Send welcome message only to students, not teachers
                if (!client.isTeacher) {
                    sendToClient("NOTIFICATION:Welcome " + clientName + "! Your timer will start automatically when you begin the quiz.", client);
                } else {
                    System.out.println("👨‍🏫 Teacher registered: " + clientName + " (will not receive quiz timers)");
                }
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
    
    /**
     * Send notification only to teachers (e.g., "Student X started quiz")
     */
    private void sendNotificationToTeachers(String message) {
        if (registeredClients.isEmpty()) {
            System.out.println("⚠️ No registered clients to notify");
            return;
        }
        
        int sent = 0;
        for (ClientInfo client : registeredClients.values()) {
            if (client.isTeacher && sendToClient(message, client)) {
                sent++;
            }
        }
        
        System.out.println("📤 Sent notification to " + sent + " teacher(s): " + message);
    }
    
    /**
     * Send notification to a specific student and all teachers (e.g., quiz scores)
     */
    private void sendNotificationToStudentAndTeachers(String studentName, String message) {
        if (registeredClients.isEmpty()) {
            System.out.println("⚠️ No registered clients to notify");
            return;
        }
        
        int sent = 0;
        for (ClientInfo client : registeredClients.values()) {
            // Send to the specific student OR any teacher
            if (client.name.equals(studentName) || client.isTeacher) {
                if (sendToClient(message, client)) {
                    sent++;
                }
            }
        }
        
        System.out.println("📤 Sent notification to student '" + studentName + "' and teachers (" + sent + " recipients): " + message);
    }
    
    private boolean sendToClient(String message, ClientInfo client) {
        try {
            // Ensure proper UTF-8 encoding
            byte[] data = message.getBytes("UTF-8");
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
