package src;

import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class QuizServer {
    public static final int PORT = 5000;

    // Shared list of results accessible by all handlers
    public static final List<String> studentResults = new ArrayList<>();
    public static final List<ObjectOutputStream> teacherStreams = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Starting Quiz Server on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Waiting for clients...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress());
                Thread t = new Thread(new src.ClientHandler(clientSocket));
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
