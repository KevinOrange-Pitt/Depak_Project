# Depak_Project — Java Chat Room

A simple **client–server chat application** written in Java.  
Multiple teammates can log in to a shared chat room and exchange messages in real time.

---

## Features

| Feature | Details |
|---------|---------|
| Multi-user | Any number of clients can connect simultaneously |
| Live messages | Every message is broadcast instantly to all connected users |
| Login dialog | Enter server host, port, and your username before joining |
| Auto-scroll | The chat window always scrolls to the latest message |
| Join / leave notifications | The room announces when someone connects or disconnects |

---

## Requirements

* **Java 11** or newer (`java` and `javac` must be on `PATH`)

---

## Quick Start

### 1 — Compile

```bash
./compile.sh
```

This compiles all sources into the `out/` directory.

### 2 — Start the server

Run the server **once** (on any machine that is reachable by your teammates):

```bash
./run_server.sh
```

You should see:

```
Chat server started on port 12345
```

### 3 — Find the server's IP address

On the machine running the server, find its local IP:

```bash
# macOS / Linux
ipconfig getifaddr en0        # Wi-Fi
# or
hostname -I                   # Linux fallback
```

Share that IP with your teammates (e.g. `192.168.1.42`).

### 4 — Each teammate clones and launches a client

```bash
git clone <repo-url>
cd Depak_Project
./compile.sh
./run_client.sh
```

A login dialog will appear:

* **Server host** — the IP address from step 3  
  (use `localhost` only if running on the same machine as the server)
* **Port** — `12345` (default, matches the server)
* **Username** — the name that will be shown next to your messages

Click **OK** to enter the chat room.

> **Same network required** — all machines must be on the same Wi-Fi/LAN, or the
> server port must be forwarded if connecting over the internet.

---

## Project Structure

```
src/
  server/
    ChatServer.java      # Listens for connections; broadcasts messages
    ClientHandler.java   # Per-client thread: reads input, sends output
  client/
    ChatClient.java      # Swing GUI: login dialog + live chat window
compile.sh               # Compiles sources → out/
run_server.sh            # Starts the server
run_client.sh            # Starts a client GUI
```

---

## How It Works

```
  [Client A] ──┐
               │  TCP sockets    ┌──────────────┐
  [Client B] ──┼────────────────►│  ChatServer  │
               │                 │  (port 12345)│
  [Client C] ──┘                 └──────────────┘
```

1. `ChatServer` listens on port **12345** and creates a `ClientHandler` thread for every accepted connection.  
2. The first line a client sends is its **username**.  
3. Every subsequent line is **broadcast** to all other connected clients by the server.  
4. The `ChatClient` GUI runs a background daemon thread that reads incoming lines and appends them to the chat area on the Swing Event Dispatch Thread.

---

## Changing the Port

Edit the constant in `src/server/ChatServer.java`:

```java
static final int PORT = 12345;   // ← change this
```

Then re-compile and update the port in the client login dialog.
