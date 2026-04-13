package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Swing-based chat client.
 *
 * On startup a login dialog asks for the server host, port, and username.
 * After a successful connection the main chat window opens and messages
 * are displayed in real time as they arrive from the server.
 */
public class ChatClient extends JFrame {

    private final JTextArea chatArea;
    private final JTextField messageField;
    private PrintWriter writer;
    private Socket socket;

    // -----------------------------------------------------------------------
    // Constructor / setup
    // -----------------------------------------------------------------------

    private ChatClient(String host, int port, String username) throws IOException {
        socket = new Socket(host, port);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);

        // Send username as the first line so the server can register it
        writer.println(username);

        // ---- Build GUI ----
        setTitle("Chat Room  —  " + username);
        setSize(680, 480);
        setMinimumSize(new Dimension(400, 300));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });

        // Chat display
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // Input row
        messageField = new JTextField();
        messageField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JButton sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(80, 30));

        ActionListener sendAction = e -> {
            String text = messageField.getText().trim();
            if (!text.isEmpty() && writer != null) {
                writer.println(text);
                messageField.setText("");
            }
            messageField.requestFocusInWindow();
        };
        sendButton.addActionListener(sendAction);
        messageField.addActionListener(sendAction);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
        messageField.requestFocusInWindow();

        // ---- Background thread: listen for incoming messages ----
        Thread receiverThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> appendMessage(msg));
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                        appendMessage("[Disconnected from server]"));
            }
        }, "receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        // Auto-scroll to the bottom
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    // -----------------------------------------------------------------------
    // Entry point — show login dialog, then open chat window
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::showLoginDialog);
    }

    private static void showLoginDialog() {
        JTextField hostField = new JTextField("localhost", 15);
        JTextField portField = new JTextField("12345", 6);
        JTextField userField = new JTextField(15);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = new Insets(4, 4, 4, 4);
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(4, 4, 4, 4);

        lc.gridx = 0; lc.gridy = 0; panel.add(new JLabel("Server host:"), lc);
        fc.gridx = 1; fc.gridy = 0; panel.add(hostField, fc);
        lc.gridy = 1;                panel.add(new JLabel("Port:"), lc);
        fc.gridy = 1;                panel.add(portField, fc);
        lc.gridy = 2;                panel.add(new JLabel("Username:"), lc);
        fc.gridy = 2;                panel.add(userField, fc);

        // Focus username field when dialog opens
        userField.requestFocusInWindow();

        int result = JOptionPane.showConfirmDialog(
                null, panel, "Login to Chat Room",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String host     = hostField.getText().trim();
        String portText = portField.getText().trim();
        String username = userField.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Username cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            showLoginDialog();   // re-show
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null,
                    "Invalid port number: " + portText, "Error", JOptionPane.ERROR_MESSAGE);
            showLoginDialog();
            return;
        }

        try {
            new ChatClient(host, port, username);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Could not connect to " + host + ":" + port + "\n" + ex.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            showLoginDialog();
        }
    }
}
