package src;

/**
 * Unified Server Launcher
 * Starts both the Quiz Server and Chat Server
 */
public class ServerLauncher {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  QUIZ SYSTEM - SERVER LAUNCHER");
        System.out.println("===========================================\n");

        // Start Quiz Server in a separate thread
        Thread quizServerThread = new Thread(() -> {
            System.out.println("[1] Starting Quiz Server...");
            QuizServer.main(new String[]{});
        }, "QuizServerThread");
        quizServerThread.setDaemon(false);
        quizServerThread.start();

        // Give Quiz Server time to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start Chat Server (NIO-based) in a separate thread
        Thread chatServerThread = new Thread(() -> {
            System.out.println("[2] Starting Chat Server (NIO)...");
            ChatServer.main(new String[]{});
        }, "ChatServerThread");
        chatServerThread.setDaemon(false);
        chatServerThread.start();

        System.out.println("\n===========================================");
        System.out.println("  Both servers are now running!");
        System.out.println("  - Quiz Server: Port " + QuizServer.PORT);
        System.out.println("  - Chat Server: Port " + ChatServer.CHAT_PORT + " (NIO)");
        System.out.println("===========================================\n");
        System.out.println("Press Ctrl+C to stop all servers.\n");
    }
}

