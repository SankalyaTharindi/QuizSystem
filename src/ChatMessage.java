package src;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a chat message sent between teacher and students
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sender;
    private String content;
    private String timestamp;
    private MessageType type;

    public enum MessageType {
        USER_MESSAGE,      // Regular chat message
        SYSTEM_MESSAGE,    // System notifications
        BROADCAST,         // Teacher broadcast to all
        PRIVATE_MESSAGE    // Private message
    }

    public ChatMessage(String sender, String content, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
    public MessageType getType() { return type; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }

    public String toDisplayString() {
        switch (type) {
            case SYSTEM_MESSAGE:
                return "[" + timestamp + "] SYSTEM: " + content;
            case BROADCAST:
                return "[" + timestamp + "] [BROADCAST] " + sender + ": " + content;
            default:
                return "[" + timestamp + "] " + sender + ": " + content;
        }
    }
}

