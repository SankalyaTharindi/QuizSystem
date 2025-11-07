# Online Quiz System (Java Socket Programming + NIO Chat)

A network-based online quiz system built with **Java**, using **TCP sockets**, **NIO (Non-blocking I/O)**, and **Swing GUI**.

## Features
- **Quiz System** — Students take quizzes, teachers monitor scores in real-time
- **Live Chat (NIO)** — Real-time messaging using Java NIO Channels and Selector
- **Multithreading** — Handles multiple concurrent clients efficiently
- **Non-blocking I/O** — Chat server uses NIO for scalable communication

## Roles
- **Teacher** — can log in and see student scores live + chat with students
  - Teacher username: `admin`, password: `123`
- **Student** — can log in, take a quiz, view score + chat with teacher and other students
  - Student username: (any name), password: `student`

---

## Technologies Used
- Java 21
- **TCP Sockets** (`ServerSocket`, `Socket`) - for quiz system
- **Java NIO** (`SocketChannel`, `Selector`, `ByteBuffer`) - for chat system
- Object Serialization (`ObjectInputStream`, `ObjectOutputStream`)
- Multithreading (`ClientHandler`)
- Swing GUI (`JSplitPane`, `JTextArea`, etc.)

---

## How It Works

### Quiz System
1. The **quiz server** runs on port 5000 and waits for clients.
2. A **client** logs in as either Student or Teacher.
3. Students receive a quiz, answer questions, and get a score.
4. Teachers see live updates of all student scores.

### Chat System (NIO)
1. The **chat server** runs on port 5001 using Java NIO.
2. Uses `Selector` for non-blocking I/O with multiple clients.
3. Students and teachers can send messages in real-time.
4. All messages are broadcast to connected users.
5. Single-threaded server handles all chat connections efficiently.

---

## Run Instructions

### Run Both Servers (Recommended)
```bash
cd QuizSystem
javac src\*.java
java src.ServerLauncher
```

OR simply double-click `start-servers.bat` (Windows)

### Run the Client
```bash
cd QuizSystem
java src.QuizClient
```

OR double-click `start-client.bat` (Windows)

**💡 For detailed testing instructions with multiple students and teachers, see [TESTING_GUIDE.md](TESTING_GUIDE.md)**

### Run Servers Separately (Optional)

**Run the Quiz Server:**
```bash
cd QuizSystem
javac src\*.java
java src.QuizServer
```

**Run the Chat Server (NIO):**
```bash
cd QuizSystem
java src.ChatServer
```


---

## Architecture

### Quiz Server (Traditional TCP)
- Uses blocking I/O with thread-per-client model
- Each client handled by `ClientHandler` in separate thread
- Port: 5000

### Chat Server (NIO - Non-blocking)
- Uses `java.nio.channels.Selector` for event-driven I/O
- Single thread handles multiple clients with channel multiplexing
- `SocketChannel` configured in non-blocking mode
- `ByteBuffer` for efficient data transfer
- Port: 5001

### Client Application
- Swing GUI with split pane layout
- Left side: Quiz interface (for students) or score dashboard (for teachers)
- Right side: Live chat panel using NIO `SocketChannel`
- Separate threads for quiz and chat communication

---

## NIO Features Demonstrated

1. **Non-blocking SocketChannel** - Chat connections don't block threads
2. **Selector Pattern** - Multiplexing multiple channels with single thread
3. **ByteBuffer** - Efficient memory management for data transfer
4. **Channel I/O** - Modern Java NIO API for network communication
5. **Scalability** - Can handle many chat connections without thread overhead

---

## Project Structure
```
QuizSystem/
├── src/
│   ├── QuizServer.java          # Main quiz server (TCP)
│   ├── ClientHandler.java       # Handles quiz client connections
│   ├── ChatServer.java          # NIO-based chat server ⭐ NEW
│   ├── ChatMessage.java         # Chat message data model ⭐ NEW
│   ├── ChatClientPanel.java    # Chat UI component (NIO) ⭐ NEW
│   ├── QuizClient.java          # Client application (updated with chat)
│   ├── ServerLauncher.java      # Launches both servers ⭐ NEW
│   ├── LoginRequest.java        # Login data model
│   ├── LoginResponse.java       # Login response model
│   ├── Question.java            # Quiz question model
│   └── QuizData.java            # Quiz question data
└── README.md
```

---

## Screenshots Flow

1. **Login** → Choose Student or Teacher role
2. **Student View** → Quiz on left, chat on right
3. **Teacher View** → Student scores on left, chat on right
4. **Real-time Chat** → Messages appear instantly using NIO

---

## Comparison: Traditional I/O vs NIO

| Feature | Quiz Server (Traditional) | Chat Server (NIO) |
|---------|---------------------------|-------------------|
| I/O Model | Blocking | Non-blocking |
| Threading | One thread per client | Single thread for all |
| Scalability | Limited by threads | Highly scalable |
| API | Socket, ServerSocket | SocketChannel, Selector |
| Complexity | Simple | More complex |
| Use Case | Request-response (quiz) | Real-time messaging |

---

## Key Learning Points

### Traditional Socket Programming (Quiz Server)
- Good for request-response patterns
- Simple to implement and understand
- Each client requires a thread
- Suitable for moderate number of clients

### NIO (Chat Server)
- Ideal for many concurrent connections
- Event-driven architecture with Selector
- Single thread handles multiple channels
- More complex but highly efficient
- Perfect for real-time messaging

---

## Future Enhancements
- File transfer using `FileChannel`
- Private messaging between users
- Message history persistence with `AsynchronousFileChannel`
- Quiz creation and upload using NIO.2 file APIs
- Performance monitoring dashboard

