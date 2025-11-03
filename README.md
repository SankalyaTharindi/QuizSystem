# Online Quiz System (Java Socket Programming)

A network-based online quiz system built with **Java**, using **TCP sockets** and **Swing GUI**.

## Roles
- **Teacher** — can log in and see student scores live.
- Teacher username:admin , password:123
- **Student** — can log in, take a quiz, and view their score.
- student username:(any name) , password:student
---

## Technologies Used
- Java 21
- TCP Sockets (`ServerSocket`, `Socket`)
- Object Serialization (`ObjectInputStream`, `ObjectOutputStream`)
- Multithreading (`ClientHandler`)
- Swing GUI

---

## How It Works
1. The **server** runs (`QuizServer.java`) and waits for clients.
2. A **client** logs in as either Student or Teacher.
3. Students receive a quiz, answer questions, and get a score.
4. Teachers see live updates of all student scores.

---

## Run Instructions

### Run the Server
```bash
cd QuizSystem
javac src\*.java
java src.QuizServer

### Run the Client
```bash
cd QuizSystem
java src.QuizClient
