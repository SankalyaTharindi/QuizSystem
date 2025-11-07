# Testing Guide - Chat App with Students and Teachers

## 🧪 How to Test the Chat Feature

This guide will walk you through testing the chat functionality between students and teachers.

---

## 📋 Prerequisites

1. Make sure all files are compiled:
```bash
cd "D:\Netwotk Programming Project\QuizSystem"
javac src\*.java
```

2. Verify you have these batch files:
   - `start-servers.bat`
   - `start-client.bat`

---

## 🎬 Step-by-Step Testing Instructions

### Step 1: Start the Servers

**Option A: Using Batch File (Easiest)**
1. Double-click `start-servers.bat`
2. Wait until you see:
   ```
   Starting Quiz Server on port 5000...
   Server started. Waiting for clients...
   Chat Server started on port 5001 (NIO mode)
   ```

**Option B: Using Command Line**
```bash
cd "D:\Netwotk Programming Project\QuizSystem"
java src.ServerLauncher
```

⚠️ **Important:** Keep this terminal/window open while testing!

---

### Step 2: Start Multiple Clients

You need to open **3 client windows** to test properly:

#### **Client 1 - Teacher**
1. Open a **new terminal/command prompt**
2. Run:
   ```bash
   cd "D:\Netwotk Programming Project\QuizSystem"
   java src.QuizClient
   ```
   OR double-click `start-client.bat`

3. **Login as Teacher:**
   - Username: `admin`
   - Password: `123`
   - Role: Select `Teacher`
   - Click `Login`

4. You should see:
   - Left side: "Student Scores" panel
   - Right side: "Live Chat" panel
   - Chat shows: "Connected to chat server!"

---

#### **Client 2 - Student (Alice)**
1. Open **another new terminal/command prompt**
2. Run:
   ```bash
   cd "D:\Netwotk Programming Project\QuizSystem"
   java src.QuizClient
   ```
   OR double-click `start-client.bat` again

3. **Login as Student:**
   - Username: `alice`
   - Password: `student`
   - Role: Select `Student`
   - Click `Login`

4. You should see:
   - Left side: Quiz questions
   - Right side: "Live Chat" panel
   - Chat shows: "Connected to chat server!"
   - Chat shows: "[TIME] SYSTEM: alice joined the chat"

---

#### **Client 3 - Student (Bob)**
1. Open **yet another new terminal/command prompt**
2. Run:
   ```bash
   cd "D:\Netwotk Programming Project\QuizSystem"
   java src.QuizClient
   ```
   OR double-click `start-client.bat` one more time

3. **Login as Student:**
   - Username: `bob`
   - Password: `student`
   - Role: Select `Student`
   - Click `Login`

4. You should see:
   - Left side: Quiz questions
   - Right side: "Live Chat" panel
   - Chat shows: "Connected to chat server!"
   - Chat shows: "[TIME] SYSTEM: bob joined the chat"

---

### Step 3: Test Chat Messaging

Now you have 3 windows open:
- **Window 1:** Teacher (admin)
- **Window 2:** Student (alice)
- **Window 3:** Student (bob)

#### Test Scenario 1: Teacher to Students
1. **In Teacher window (admin):**
   - Type in chat box: `Hello students! Welcome to the quiz!`
   - Press `Enter` or click `Send`

2. **Check all 3 windows:**
   - ✅ Teacher window shows: `[TIME] You: Hello students! Welcome to the quiz!`
   - ✅ Alice window shows: `[TIME] admin (Teacher): Hello students! Welcome to the quiz!`
   - ✅ Bob window shows: `[TIME] admin (Teacher): Hello students! Welcome to the quiz!`

---

#### Test Scenario 2: Student to All
1. **In Alice window:**
   - Type: `Hi teacher! I'm ready to start.`
   - Press `Enter`

2. **Check all 3 windows:**
   - ✅ Alice shows: `[TIME] You: Hi teacher! I'm ready to start.`
   - ✅ Teacher shows: `[TIME] alice: Hi teacher! I'm ready to start.`
   - ✅ Bob shows: `[TIME] alice: Hi teacher! I'm ready to start.`

---

#### Test Scenario 3: Student to Student
1. **In Bob window:**
   - Type: `Good luck Alice!`
   - Press `Enter`

2. **Check all 3 windows:**
   - ✅ Bob shows: `[TIME] You: Good luck Alice!`
   - ✅ Alice shows: `[TIME] bob: Good luck Alice!`
   - ✅ Teacher shows: `[TIME] bob: Good luck Alice!`

---

#### Test Scenario 4: Multiple Messages
1. Have all 3 users send messages rapidly:
   - Teacher: `Question 1 is about Java basics`
   - Alice: `Got it!`
   - Bob: `Can you repeat that?`
   - Teacher: `Sure! It's about Java basics`

2. **Verify:**
   - ✅ All messages appear in order
   - ✅ All timestamps are correct
   - ✅ No messages are lost
   - ✅ Real-time delivery (no delays)

---

### Step 4: Test Join/Leave Notifications

#### Test User Joining
1. **Open a 4th client window**
2. Login as student with username: `charlie`
3. **Check all existing chat windows:**
   - ✅ All should show: `[TIME] SYSTEM: charlie joined the chat`

---

#### Test User Leaving
1. **Close Bob's client window** (the 3rd window)
2. **Check remaining chat windows:**
   - ✅ All should show: `[TIME] SYSTEM: bob left the chat`

---

## 🎯 Visual Test Layout

Here's what your screen should look like:

```
┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
│ Teacher Dashboard       │  │ Student - alice         │  │ Student - bob           │
├────────────┬────────────┤  ├────────────┬────────────┤  ├────────────┬────────────┤
│ Scores     │ Chat       │  │ Quiz       │ Chat       │  │ Quiz       │ Chat       │
│            │            │  │            │            │  │            │            │
│ alice: 8/10│[10:30]     │  │ Q1: Which  │[10:30]     │  │ Q1: Which  │[10:30]     │
│            │alice joined│  │ language   │alice joined│  │ language   │alice joined│
│            │            │  │            │            │  │            │            │
│            │[10:31]     │  │ ○ Java     │[10:31]     │  │ ○ Java     │[10:31]     │
│            │bob joined  │  │ ○ Python   │bob joined  │  │ ○ Python   │bob joined  │
│            │            │  │            │            │  │            │            │
│            │[10:32]     │  │            │[10:32]     │  │            │[10:32]     │
│            │admin: Hello│  │            │admin: Hello│  │            │admin: Hello│
│            │            │  │            │            │  │            │            │
│            │[_______]   │  │            │[_______]   │  │            │[_______]   │
│            │[Send]      │  │            │[Send]      │  │            │[Send]      │
└────────────┴────────────┘  └────────────┴────────────┘  └────────────┴────────────┘
```

---

## ✅ Checklist: What to Verify

### Chat Functionality
- [ ] Messages appear in real-time (no delay)
- [ ] All users see all messages
- [ ] Timestamps are correct
- [ ] "You:" appears for sender's own messages
- [ ] Username appears correctly for received messages
- [ ] Teacher is labeled as "(Teacher)"
- [ ] System messages show join/leave events

### NIO Performance
- [ ] Server console shows "User registered: [username]"
- [ ] Server console shows chat messages being received
- [ ] Single server thread handles all chat connections
- [ ] No lag or delays even with multiple users
- [ ] Chat works while quiz is in progress

### UI Behavior
- [ ] Chat panel scrolls automatically to show new messages
- [ ] Text input clears after sending
- [ ] Enter key sends messages (as well as Send button)
- [ ] Split pane divider can be resized
- [ ] Chat doesn't interfere with quiz functionality

### Error Handling
- [ ] If chat server isn't running, shows error message
- [ ] When client closes, others receive "left the chat" notification
- [ ] Reconnection handled gracefully

---

## 🎭 Advanced Testing Scenarios

### Scenario 1: Quiz While Chatting
1. Student Alice starts taking the quiz
2. Teacher sends a message: "You have 10 minutes"
3. Alice can see the message while answering questions
4. Alice can reply without losing quiz progress

**Expected:** Both features work independently

---

### Scenario 2: Load Testing
1. Open 10+ client windows
2. Have them all login (mix of students and teacher)
3. Send messages from random clients
4. Monitor server console for performance

**Expected:** 
- Single thread handles all connections
- No performance degradation
- Messages delivered to all clients

---

### Scenario 3: Teacher Broadcast
1. Teacher sends announcement: "Quiz starts now!"
2. All students receive it simultaneously
3. Students can ask questions
4. Teacher can respond to everyone

**Expected:** Group communication works smoothly

---

## 🐛 Troubleshooting

### Problem: "Could not connect to chat server"
**Solution:**
1. Check if ChatServer is running
2. Look for "Chat Server started on port 5001" in server console
3. If not, restart `ServerLauncher`

---

### Problem: Messages not appearing
**Diagnostic Steps:**
1. Check server console for "Chat: [message]" logs
2. Verify all clients show "Connected to chat server!"
3. Try closing and reopening a client
4. Restart servers if needed

---

### Problem: Only some users see messages
**Solution:**
1. Verify all clients successfully connected
2. Check server console for "User registered: [name]"
3. Look for any exceptions in server console

---

### Problem: Duplicate messages
**Cause:** Multiple server instances running
**Solution:**
1. Close all server windows
2. Check Task Manager for java.exe processes
3. End all java processes
4. Start fresh with `ServerLauncher`

---

## 📊 Server Console Output Example

When testing correctly, you should see:

```
===========================================
  QUIZ SYSTEM - SERVER LAUNCHER
===========================================

[1] Starting Quiz Server...
Starting Quiz Server on port 5000...
Server started. Waiting for clients...

[2] Starting Chat Server (NIO)...
Chat Server started on port 5001 (NIO mode)

===========================================
  Both servers are now running!
  - Quiz Server: Port 5000
  - Chat Server: Port 5001 (NIO)
===========================================

Client connected from /127.0.0.1
Teacher logged in: /127.0.0.1
New chat client connected: /127.0.0.1:xxxxx
User registered: admin (Teacher)

Client connected from /127.0.0.1
Student logged in: /127.0.0.1
New chat client connected: /127.0.0.1:xxxxx
User registered: alice

Client connected from /127.0.0.1
Student logged in: /127.0.0.1
New chat client connected: /127.0.0.1:xxxxx
User registered: bob

Chat: [10:32:15] admin (Teacher): Hello students!
Chat: [10:32:20] alice: Hi teacher!
Chat: [10:32:25] bob: Hello!
```

---

## 🎓 What You're Testing

### NIO Concepts
1. **Non-blocking I/O** - Server handles multiple clients without blocking
2. **Selector** - Single thread monitors all chat connections
3. **Event-driven** - Messages processed as events arrive
4. **Scalability** - One thread handles all users efficiently

### System Integration
1. **Dual Protocol** - Quiz (TCP) and Chat (NIO) work together
2. **Real-time** - Messages delivered instantly
3. **Broadcast** - All users receive messages
4. **State Management** - User tracking and notifications

---

## 📝 Quick Test Script

Copy and paste this test script:

```
Test 1: Basic Chat
------------------
1. Start servers
2. Login: Teacher (admin/123)
3. Login: Student (alice/student)
4. Teacher: "Hello alice"
5. Alice: "Hi teacher"
✓ Both see messages

Test 2: Multi-user
------------------
1. Login: Student (bob/student)
2. Bob: "Hello everyone"
✓ All 3 users see the message

Test 3: Join/Leave
------------------
1. Login: Student (charlie/student)
✓ All see: "charlie joined the chat"
2. Close charlie's window
✓ All see: "charlie left the chat"

Test 4: While Taking Quiz
--------------------------
1. Alice answers quiz questions
2. Teacher: "5 minutes left"
3. Alice: "Okay thanks"
✓ Chat works during quiz
```

---

## 🎉 Success Criteria

Your chat system is working correctly if:

✅ Multiple clients can connect simultaneously
✅ All users see all messages in real-time
✅ Join/leave notifications appear
✅ Teacher and student chats are distinguished
✅ No lag or delays
✅ Server console shows NIO handling connections
✅ Chat works alongside quiz functionality
✅ Messages persist for late joiners (history)

---

## 📞 Need Help?

If something isn't working:
1. Check server console for errors
2. Verify both servers are running
3. Ensure ports 5000 and 5001 are not blocked
4. Restart everything and try again
5. Check firewall settings

---

**Happy Testing! 🚀**

The chat feature showcases Java NIO's power for real-time communication!

