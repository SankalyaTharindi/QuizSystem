package src;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class QuizClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private JFrame frame;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;

    private List<src.Question> quiz;
    private int[] answers;
    private int currentIndex = 0;

    private JLabel lblQuestion, lblTimer;
    private JRadioButton[] optionButtons;
    private ButtonGroup group;
    private JButton btnNext;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRole;

    private src.ChatClientPanel chatPanel;
    private String currentUsername;

    private javax.swing.Timer countdownTimer;
    private int timeLeftSeconds = 0;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizClient().showLogin());
    }

    private void showLogin() {
        frame = new JFrame("Quiz Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLayout(new GridLayout(5, 2, 10, 10));
        frame.setLocationRelativeTo(null);

        frame.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        frame.add(txtUsername);

        frame.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        frame.add(txtPassword);

        frame.add(new JLabel("Role:"));
        cmbRole = new JComboBox<>(new String[]{"Student", "Teacher"});
        frame.add(cmbRole);

        JButton btnLogin = new JButton("Login");
        btnLogin.addActionListener(this::onLoginClicked);
        frame.add(btnLogin);

        frame.setVisible(true);
    }

    private void onLoginClicked(ActionEvent e) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String role = (String) cmbRole.getSelectedItem();
        currentUsername = username;

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill in all fields.");
            return;
        }
        new Thread(() -> sendLoginRequest(username, password, role)).start();
    }

    private void sendLoginRequest(String username, String password, String role) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(new src.LoginRequest(username, password, role));
            out.flush();

            Object response = in.readObject();
            if (response instanceof src.LoginResponse res) {
                if (res.isSuccess()) {
                    SwingUtilities.invokeLater(() -> {
                        frame.dispose();
                        if (role.equalsIgnoreCase("teacher")) showTeacherPanel(res.getMessage());
                        else {
                            showQuizUI();
                            new Thread(this::receiveQuizFromServer).start();
                        }
                    });
                } else {
                    JOptionPane.showMessageDialog(frame, "Login failed: " + res.getMessage());
                    socket.close();
                }
            }
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                    "Could not connect: " + ex.getMessage()));
        }
    }

    private void showTeacherPanel(String msg) {
        JFrame teacherFrame = new JFrame("Teacher Dashboard - " + currentUsername);
        teacherFrame.setSize(800, 500);
        teacherFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        teacherFrame.setLayout(new BorderLayout(10, 10));

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scoresScroll = new JScrollPane(textArea);
        scoresScroll.setBorder(BorderFactory.createTitledBorder("Student Scores"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(scoresScroll);
        chatPanel = new src.ChatClientPanel(currentUsername + " (Teacher)");
        splitPane.setRightComponent(chatPanel);
        splitPane.setDividerLocation(450);

        teacherFrame.add(splitPane, BorderLayout.CENTER);
        teacherFrame.setLocationRelativeTo(null);
        teacherFrame.setVisible(true);

        textArea.setText("Welcome Teacher!\nThe students' scores will be displayed here.\n\n");

        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof java.util.List<?> results) {
                        StringBuilder sb = new StringBuilder("Welcome Teacher!\n\n");
                        for (Object line : results) sb.append(line).append("\n");
                        textArea.setText(sb.toString());
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    /** Show student quiz GUI with fixed timer and button outside card */
    private void showQuizUI() {
        frame = new JFrame("Online Quiz - " + currentUsername);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLayout(new BorderLayout());

        // 🔹 Timer bar at top
        JPanel timerPanel = new JPanel();
        timerPanel.setBackground(new Color(245, 245, 245));
        timerPanel.setBorder(new MatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

        lblTimer = new JLabel("Waiting for quiz to start...", SwingConstants.CENTER);
        lblTimer.setFont(new Font("Arial", Font.BOLD, 18));
        lblTimer.setForeground(Color.RED);
        timerPanel.add(lblTimer);
        frame.add(timerPanel, BorderLayout.NORTH);

        // 🔹 Quiz card in center (white)
        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setBackground(new Color(240, 240, 240));

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 5, 5, new Color(220, 220, 220)),
                new EmptyBorder(20, 30, 20, 30)
        ));

        // Question
        lblQuestion = new JLabel("Loading quiz...");
        lblQuestion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        lblQuestion.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblQuestion.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));
        cardPanel.add(lblQuestion);

        // Options
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        optionButtons = new JRadioButton[4];
        group = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("SansSerif", Font.PLAIN, 14));
            group.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }
        cardPanel.add(optionsPanel);
        outerPanel.add(cardPanel);

        // 🔹 Next/Submit button (outside the card)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(245, 245, 245));
        bottomPanel.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        btnNext = new JButton("Next");
        btnNext.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnNext.setPreferredSize(new Dimension(200, 40));
        btnNext.addActionListener(e -> onNextClicked());
        bottomPanel.add(btnNext);

        // 🔹 Split pane (quiz + chat)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JPanel leftContainer = new JPanel(new BorderLayout());
        leftContainer.add(outerPanel, BorderLayout.CENTER);
        leftContainer.add(bottomPanel, BorderLayout.SOUTH);
        splitPane.setLeftComponent(leftContainer);

        chatPanel = new src.ChatClientPanel(currentUsername);
        splitPane.setRightComponent(chatPanel);
        splitPane.setDividerLocation(550);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (chatPanel != null) chatPanel.disconnect();
                if (countdownTimer != null) countdownTimer.stop();
            }
        });
    }

    private void receiveQuizFromServer() {
        try {
            Object obj = in.readObject();
            if (obj instanceof List<?>) {
                quiz = (List<src.Question>) obj;
                answers = new int[quiz.size()];
                for (int i = 0; i < answers.length; i++) answers[i] = -1;
                SwingUtilities.invokeLater(() -> displayQuestion(0));
            }

            Object timerObj = in.readObject();
            if (timerObj instanceof String msg && msg.startsWith("START_QUIZ:")) {
                int seconds = Integer.parseInt(msg.split(":")[1]);
                SwingUtilities.invokeLater(() -> startCountdownTimer(seconds));
            }

            while (true) Thread.sleep(1000);
        } catch (Exception e) {
            showError("Connection lost: " + e.getMessage());
        }
    }

    private void startCountdownTimer(int seconds) {
        timeLeftSeconds = seconds;
        if (countdownTimer != null && countdownTimer.isRunning()) countdownTimer.stop();

        countdownTimer = new javax.swing.Timer(1000, e -> {
            if (timeLeftSeconds > 0) {
                timeLeftSeconds--;
                lblTimer.setText("Time Left: " + formatTime(timeLeftSeconds));

                if (timeLeftSeconds <= 60) lblTimer.setForeground(Color.ORANGE);
                if (timeLeftSeconds <= 30) lblTimer.setForeground(Color.RED);
            } else {
                ((javax.swing.Timer) e.getSource()).stop();
                lblTimer.setText("Time Left: 00:00");
                JOptionPane.showMessageDialog(frame, "Time is up! Submitting your answers...");
                submitAnswers();
            }
        });
        countdownTimer.start();
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void displayQuestion(int index) {
        if (quiz == null || index < 0 || index >= quiz.size()) return;
        src.Question q = quiz.get(index);
        lblQuestion.setText("Q" + (index + 1) + ": " + q.getQuestionText());
        String[] opts = q.getOptions();
        for (int i = 0; i < 4; i++) optionButtons[i].setText((i + 1) + ". " + opts[i]);
        group.clearSelection();
        if (answers[index] >= 0) optionButtons[answers[index]].setSelected(true);
        btnNext.setText(index == quiz.size() - 1 ? "Submit" : "Next");
    }

    private void onNextClicked() {
        int selected = -1;
        for (int i = 0; i < 4; i++) if (optionButtons[i].isSelected()) selected = i;
        answers[currentIndex] = selected;

        if (currentIndex < quiz.size() - 1) {
            currentIndex++;
            displayQuestion(currentIndex);
        } else {
            btnNext.setEnabled(false);
            new Thread(this::submitAnswers).start();
        }
    }

    private void submitAnswers() {
        try {
            if (countdownTimer != null) countdownTimer.stop();
            out.writeObject(answers);
            out.flush();
            Object resp = in.readObject();
            if (resp instanceof Integer score) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "You scored: " + score + "/" + quiz.size());
                    frame.dispose();
                });
            }
        } catch (Exception e) {
            showError("Error submitting answers: " + e.getMessage());
        }
    }

    private void showError(String text) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(frame, text, "Error", JOptionPane.ERROR_MESSAGE));
    }
}
