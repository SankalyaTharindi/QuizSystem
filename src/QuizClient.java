package src;

import javax.swing.*;
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

    // Quiz GUI components
    private JLabel lblQuestion;
    private JRadioButton[] optionButtons;
    private ButtonGroup group;
    private JButton btnNext;

    // Login GUI components
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRole;

    // Chat component
    private ChatClientPanel chatPanel;
    private String currentUsername;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizClient().showLogin());
    }

    /** Show login GUI */
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

    /** Handle login button click */
    private void onLoginClicked(ActionEvent e) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String role = (String) cmbRole.getSelectedItem();


        currentUsername = username; // Store username for chat
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill in all fields.");
            return;
        }

        new Thread(() -> sendLoginRequest(username, password, role)).start();
    }

    /** Send login to server */
    private void sendLoginRequest(String username, String password, String role) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Send login request
            src.LoginRequest login = new src.LoginRequest(username, password, role);
            out.writeObject(login);
            out.flush();

            Object response = in.readObject();
            if (response instanceof src.LoginResponse res) {
                if (res.isSuccess()) {
                    SwingUtilities.invokeLater(() -> {
                        frame.dispose(); // close login window
                        if (role.equalsIgnoreCase("teacher")) {
                            showTeacherPanel(res.getMessage());
                        } else {
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

    /** Show teacher view */
    private void showTeacherPanel(String msg) {
        JFrame teacherFrame = new JFrame("Teacher Dashboard - " + currentUsername);
        teacherFrame.setSize(800, 500);
        teacherFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        teacherFrame.setLayout(new BorderLayout(10, 10));

        // Scores panel
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scoresScroll = new JScrollPane(textArea);
        scoresScroll.setBorder(BorderFactory.createTitledBorder("Student Scores"));

        // Create split pane with scores on left and chat on right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(scoresScroll);

        // Add chat panel
        chatPanel = new ChatClientPanel(currentUsername + " (Teacher)");
        splitPane.setRightComponent(chatPanel);
        splitPane.setDividerLocation(450);

        teacherFrame.add(splitPane, BorderLayout.CENTER);
        teacherFrame.setLocationRelativeTo(null);
        teacherFrame.setVisible(true);

        // initialize text right away
        textArea.setText("Welcome Teacher!\nThe students' scores will be displayed here.\n\n");

        // Cleanup on close
        teacherFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (chatPanel != null) {
                    chatPanel.disconnect();
                }
            }
        });

        // Start a background thread to listen for updates from the server
        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof java.util.List<?>) {
                        List<?> results = (List<?>) obj;
                        StringBuilder sb = new StringBuilder("Welcome Teacher!\nThe students' scores will be displayed here.\n\n");
                        for (Object line : results) sb.append(line).append("\n");
                        textArea.setText(sb.toString());
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }



    /** STEP 5: Show student quiz GUI */
    private void showQuizUI() {
        frame = new JFrame("Online Quiz - " + currentUsername);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLayout(new BorderLayout());

        // Quiz panel (left side)
        JPanel quizPanel = new JPanel(new BorderLayout());

        lblQuestion = new JLabel("Loading quiz...");
        lblQuestion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        lblQuestion.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        quizPanel.add(lblQuestion, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(4, 1));
        optionButtons = new JRadioButton[4];
        group = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            group.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }
        quizPanel.add(optionsPanel, BorderLayout.CENTER);

        btnNext = new JButton("Next");
        btnNext.addActionListener(e -> onNextClicked());
        quizPanel.add(btnNext, BorderLayout.SOUTH);

        // Create split pane with quiz on left and chat on right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(quizPanel);

        // Add chat panel
        chatPanel = new ChatClientPanel(currentUsername);
        splitPane.setRightComponent(chatPanel);
        splitPane.setDividerLocation(550);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Cleanup on close
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (chatPanel != null) {
                    chatPanel.disconnect();
                }
            }
        });
    }

    /** STEP 6: Receive quiz data from server */
    private void receiveQuizFromServer() {
        try {
            Object obj = in.readObject();
            if (obj instanceof List<?>) {
                quiz = (List<src.Question>) obj;
                answers = new int[quiz.size()];
                for (int i = 0; i < answers.length; i++) answers[i] = -1;

                SwingUtilities.invokeLater(() -> displayQuestion(0));
            }
        } catch (Exception e) {
            showError("Failed to load quiz: " + e.getMessage());
        }
    }

    private void displayQuestion(int index) {
        if (quiz == null || index < 0 || index >= quiz.size()) return;
        src.Question q = quiz.get(index);
        lblQuestion.setText("Q" + (index + 1) + ": " + q.getQuestionText());
        String[] opts = q.getOptions();
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText((i + 1) + ". " + opts[i]);
        }
        group.clearSelection();

        if (answers[index] >= 0) optionButtons[answers[index]].setSelected(true);
        btnNext.setText(index == quiz.size() - 1 ? "Submit" : "Next");
    }

    private void onNextClicked() {
        int selected = -1;
        for (int i = 0; i < 4; i++) {
            if (optionButtons[i].isSelected()) selected = i;
        }
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
