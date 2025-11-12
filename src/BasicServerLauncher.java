package src;

/**
 * Basic Server Launcher - TCP + NIO only
 * Starts just the essential quiz and chat servers
 */
public class BasicServerLauncher {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  BASIC QUIZ SYSTEM - SERVER LAUNCHER");
        System.out.println("  TCP Quiz + NIO Chat Servers");
        System.out.println("===========================================\n");

        // Start TCP Quiz Server
        Thread quizServerThread = new Thread(() -> {
            System.out.println("[1] 🔗 Starting TCP Quiz Server...");
            QuizServer.main(new String[]{});
        }, "QuizServerThread");
        quizServerThread.setDaemon(false);
        quizServerThread.start();

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Start NIO Chat Server  
        Thread chatServerThread = new Thread(() -> {
            System.out.println("[2] ⚡ Starting NIO Chat Server...");
            ChatServer.main(new String[]{});
        }, "ChatServerThread");
        chatServerThread.setDaemon(false);
        chatServerThread.start();

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        System.out.println("\n===========================================");
        System.out.println("  🎉 CORE SERVERS ARE RUNNING!");
        System.out.println("===========================================");
        System.out.println("  ✅ TCP Quiz Server     - Port 5000 (Reliable)");
        System.out.println("  ✅ NIO Chat Server     - Port 5001 (Non-blocking)");
        System.out.println("===========================================");
        System.out.println("  📱 Start clients: java -cp . src.QuizClient");
        System.out.println("  📡 Start UDP notifier: java -cp . src.SimpleUDPNotifier");
        System.out.println("  🛑 Press Ctrl+C to stop servers.");
        System.out.println("===========================================\n");
    }
}