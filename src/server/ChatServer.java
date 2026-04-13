package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Multi-threaded chat server.
 * Accepts client connections on PORT and spawns a ClientHandler thread for each one.
 * Broadcasts every incoming message to all other connected clients.
 */
public class ChatServer {

    static final int PORT = 12345;

    // Thread-safe set of all active client handlers
    static final Set<ClientHandler> clients =
            Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) throws IOException {
        int port = PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: '" + args[0] + "'. Expected an integer.");
                System.err.println("Usage: java server.ChatServer [port]");
                System.exit(1);
            }
        }
        start(port);
    }

    /**
     * Starts accepting client connections on {@code port}.
     * Blocks until the server socket is closed.
     */
    public static void start(int port) throws IOException {
        System.out.println("Chat server started on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, clients);
                clients.add(handler);
                new Thread(handler).start();
            }
        }
    }

    /**
     * Sends {@code message} to every connected client except {@code sender}.
     */
    static void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    /**
     * Removes a client handler from the active set (called on disconnect).
     */
    static void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }
}
