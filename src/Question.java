package src;

import java.io.Serializable;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    private String questionText;
    private String[] options;
    private int correctOption; // zero-based index
    
    // Network programming demonstration fields
    public enum NetworkMode {
        TCP_RELIABLE,    // Current implementation - for important quiz questions
        UDP_FAST,        // For quick polls, notifications
        NIO_ASYNC,       // For large question sets with async processing
        MULTICAST        // For broadcasting questions to all students
    }
    
    public enum QuestionType {
        MULTIPLE_CHOICE, // Current implementation
        TRUE_FALSE,      // Simple yes/no questions
        QUICK_POLL,      // Fast UDP-based polling
        TIMED_CHALLENGE  // Time-sensitive questions
    }
    
    private NetworkMode networkMode;
    private QuestionType questionType;
    private boolean isQuickPoll; // True for instant UDP polling
    private int timeoutSeconds;  // For UDP timeout handling
    private LocalDateTime timestamp;
    private String sourceServer; // Which server sent this question
    private long networkLatency; // Track transmission time

    // Original constructor (backward compatibility)
    public Question(String questionText, String[] options, int correctOption) {
        this.questionText = questionText;
        this.options = options;
        this.correctOption = correctOption;
        this.networkMode = NetworkMode.TCP_RELIABLE; // Default to reliable TCP
        this.questionType = QuestionType.MULTIPLE_CHOICE;
        this.isQuickPoll = false;
        this.timeoutSeconds = 30;
        this.timestamp = LocalDateTime.now();
        this.sourceServer = "QuizServer";
    }
    
    // Enhanced constructor for network programming demonstration
    public Question(String questionText, String[] options, int correctOption, 
                   NetworkMode mode, QuestionType type) {
        this(questionText, options, correctOption);
        this.networkMode = mode;
        this.questionType = type;
        this.isQuickPoll = (type == QuestionType.QUICK_POLL);
        this.timeoutSeconds = isQuickPoll ? 10 : 30;
        this.sourceServer = getServerNameByMode(mode);
    }

    public String getQuestionText() { return questionText; }
    public String[] getOptions() { return options; }
    public int getCorrectOption() { return correctOption; }
    
    // Network-specific getters
    public NetworkMode getNetworkMode() { return networkMode; }
    public QuestionType getQuestionType() { return questionType; }
    public boolean isQuickPoll() { return isQuickPoll; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSourceServer() { return sourceServer; }
    public long getNetworkLatency() { return networkLatency; }
    
    // Network utility methods
    public String getServerNameByMode(NetworkMode mode) {
        switch (mode) {
            case TCP_RELIABLE: return "QuizServer-TCP";
            case UDP_FAST: return "PollServer-UDP";
            case NIO_ASYNC: return "ChatServer-NIO";
            case MULTICAST: return "BroadcastServer-MC";
            default: return "UnknownServer";
        }
    }
    
    public void recordNetworkLatency(long startTime) {
        this.networkLatency = System.currentTimeMillis() - startTime;
    }
    
    // Convert question to UDP packet format (lightweight)
    public String toUDPString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QUESTION:").append(questionText).append("|");
        for (int i = 0; i < options.length; i++) {
            sb.append((char)('A' + i)).append(":").append(options[i]);
            if (i < options.length - 1) sb.append("|");
        }
        return sb.toString();
    }
    
    // Parse UDP response and check if correct
    public static boolean isCorrectUDPAnswer(Question question, String udpResponse) {
        // UDP response format: "POLL_ANSWER:B:StudentID"
        String[] parts = udpResponse.split(":");
        if (parts.length >= 2) {
            char selectedOption = parts[1].charAt(0);
            int selectedIndex = selectedOption - 'A';
            return selectedIndex == question.getCorrectOption();
        }
        return false;
    }
    
    // Create quick poll question for UDP
    public static Question createQuickPoll(String question, String[] options, int correct) {
        return new Question(question, options, correct, 
                          NetworkMode.UDP_FAST, QuestionType.QUICK_POLL);
    }

    @Override
    public String toString() {
        return "Question{" + questionText + ", options=" + Arrays.toString(options)
                + ", correct=" + correctOption + ", mode=" + networkMode + '}';
    }
}
