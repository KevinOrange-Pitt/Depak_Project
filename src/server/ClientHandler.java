package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

/**
 * Handles communication with a single connected chat client.
 * The first line received from the client is treated as the username.
 * Subsequent lines are broadcast as chat messages.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Set<ClientHandler> clients;
    private PrintWriter writer;
    private String username = "Unknown";

    public ClientHandler(Socket socket, Set<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            writer = out;

            // First message from the client is the username
            username = reader.readLine();
            if (username == null || username.isBlank()) {
                username = "Anonymous";
            }

            System.out.println(username + " joined the chat.");
            ChatServer.broadcast("[" + username + " has joined the chat]", this);
            sendMessage("[You have joined the chat as: " + username + "]");

            String message;
            while ((message = reader.readLine()) != null) {
                String formatted = username + ": " + message;
                System.out.println(formatted);
                // Echo back to sender so they see their own message
                sendMessage(formatted);
                // Broadcast to everyone else
                ChatServer.broadcast(formatted, this);
            }
        } catch (IOException e) {
            System.out.println(username + " disconnected unexpectedly: " + e.getMessage());
        } finally {
            ChatServer.removeClient(this);
            ChatServer.broadcast("[" + username + " has left the chat]", this);
            System.out.println(username + " left the chat.");
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Sends a message to this client's output stream.
     */
    void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    public String getUsername() {
        return username;
    }
}
