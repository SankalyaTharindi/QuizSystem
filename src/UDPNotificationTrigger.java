package src;

import java.net.*;

/**
 * UDP Notification Trigger
 * Sends commands to the AutoUDPNotifier to automatically start/stop quiz timers
 */
public class UDPNotificationTrigger {
    
    public static void triggerQuizStart(String studentName) {
        try {
            DatagramSocket socket = new DatagramSocket();
            
            // Send student-specific command
            String command = "START_QUIZ_TIMER:" + studentName;
            byte[] data = command.getBytes("UTF-8");
            
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, 5020);
            
            socket.send(packet);
            socket.close();
            
            System.out.println("🎯 Triggered individual quiz timer start for: " + studentName);
        } catch (Exception e) {
            System.err.println("Failed to trigger quiz start: " + e.getMessage());
        }
    }
    
    public static void triggerQuizEnd(String studentName) {
        try {
            DatagramSocket socket = new DatagramSocket();
            
            // Send student-specific command
            String command = "STOP_QUIZ_TIMER:" + studentName;
            byte[] data = command.getBytes("UTF-8");
            
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, 5020);
            
            socket.send(packet);
            socket.close();
            
            System.out.println("🎯 Triggered quiz timer stop for: " + studentName);
        } catch (Exception e) {
            System.err.println("Failed to trigger quiz end: " + e.getMessage());
        }
    }
    
    public static void sendQuizEvent(String message) {
        try {
            DatagramSocket socket = new DatagramSocket();
            
            String command = "QUIZ_EVENT:" + message;
            byte[] data = command.getBytes();
            
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, 5020);
            
            socket.send(packet);
            socket.close();
            
            System.out.println("🎯 Sent quiz event: " + message);
        } catch (Exception e) {
            System.err.println("Failed to send quiz event: " + e.getMessage());
        }
    }
}