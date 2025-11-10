package src;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            // 1️⃣ Wait for login
            Object first = in.readObject();
            if (!(first instanceof src.LoginRequest login)) {
                out.writeObject(new src.LoginResponse(false, "Invalid initial request."));
                socket.close();
                return;
            }

            // 2️⃣ Teacher login
            if (login.getRole().equalsIgnoreCase("teacher")
                    && login.getUsername().equals("admin")
                    && login.getPassword().equals("123")) {

                out.writeObject(new src.LoginResponse(true, "Teacher login successful"));
                out.flush();
                System.out.println("Teacher logged in: " + socket.getInetAddress());

                synchronized (src.QuizServer.teacherStreams) {
                    src.QuizServer.teacherStreams.add(out);
                }

                synchronized (src.QuizServer.studentResults) {
                    out.writeObject(new ArrayList<>(src.QuizServer.studentResults));
                    out.flush();
                }

                while (!socket.isClosed()) Thread.sleep(5000);
                return;
            }

            // 3️⃣ Student login
            if (login.getRole().equalsIgnoreCase("student")
                    && login.getPassword().equals("student")) {
                out.writeObject(new src.LoginResponse(true, "Student login successful"));
                out.flush();
                System.out.println("Student logged in: " + login.getUsername());
            } else {
                out.writeObject(new src.LoginResponse(false, "Invalid credentials"));
                out.flush();
                socket.close();
                return;
            }

            // 4️⃣ Send quiz and start timer
            List<src.Question> quiz = src.QuizData.getQuestions();
            out.writeObject(quiz);
            out.flush();

            // Send timer start message (5 minutes)
            out.writeObject("START_QUIZ:300");
            out.flush();

            // 5️⃣ Receive answers
            Object received = in.readObject();
            if (!(received instanceof int[] answers)) {
                System.out.println("Unexpected data type from client.");
                socket.close();
                return;
            }

            // 6️⃣ Calculate score
            int score = 0;
            for (int i = 0; i < quiz.size() && i < answers.length; i++) {
                if (answers[i] == quiz.get(i).getCorrectOption()) score++;
            }

            String resultLine = "Student " + login.getUsername() + " scored: " + score + "/" + quiz.size();

            // 7️⃣ Save result and update teacher views
            synchronized (src.QuizServer.studentResults) {
                src.QuizServer.studentResults.add(resultLine);
            }

            synchronized (src.QuizServer.teacherStreams) {
                for (ObjectOutputStream teacherOut : src.QuizServer.teacherStreams) {
                    try {
                        teacherOut.writeObject(new ArrayList<>(src.QuizServer.studentResults));
                        teacherOut.flush();
                    } catch (IOException ignored) {}
                }
            }

            // 8️⃣ Send result to student
            out.writeObject(Integer.valueOf(score));
            out.flush();
            System.out.println(resultLine);

        } catch (Exception e) {
            System.out.println("Error with client " + socket.getInetAddress() + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
