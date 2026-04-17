package client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

/**
 * Persona 3 – themed Swing chat client.
 *
 * Dark navy palette with teal / cyan accents.
 * Uses a StyledDocument so each message line gets its own colour.
 */
public class ChatClient extends JFrame {

    // -----------------------------------------------------------------------
    // Persona 3 colour palette
    // -----------------------------------------------------------------------
    private static final Color BG_DEEP      = new Color(0x0A, 0x15, 0x20); // darkest navy
    private static final Color BG_SURFACE   = new Color(0x0D, 0x20, 0x35); // message bubbles / inputs
    private static final Color BG_PANEL     = new Color(0x07, 0x15, 0x25); // header / footer
    private static final Color ACCENT_TEAL  = new Color(0x4D, 0xA6, 0xD4); // primary accent
    private static final Color ACCENT_GREEN = new Color(0x5D, 0xCA, 0xA5); // "online" / join events
    private static final Color ACCENT_PURP  = new Color(0xAF, 0xA9, 0xEC); // alt sender colour
    private static final Color BORDER_DIM   = new Color(0x1D, 0x3A, 0x55); // subtle border
    private static final Color TEXT_PRIMARY = new Color(0xE8, 0xF4, 0xFD); // main text
    private static final Color TEXT_MUTED   = new Color(0x7B, 0xAF, 0xC8); // timestamps / hints
    private static final Color TEXT_OWN     = new Color(0xC8, 0xDF, 0xE8); // your own messages

    // Rotate through these for other users' name labels
    private static final Color[] SENDER_COLORS = {
        ACCENT_TEAL,
        ACCENT_GREEN,
        ACCENT_PURP,
        new Color(0xD4, 0x53, 0x7E), // pink
        new Color(0xEF, 0x9F, 0x27), // amber
    };

    // -----------------------------------------------------------------------
    // Instance state
    // -----------------------------------------------------------------------
    private final JTextPane  chatPane;
    private final StyledDocument doc;
    private JTextField messageField;
    private PrintWriter writer;
    private Socket      socket;
    private final String ownUsername;

    // Simple per-sender colour assignment (username → colour index)
    private final java.util.Map<String, Integer> senderColorMap = new java.util.LinkedHashMap<>();
    private int nextColorIndex = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    private ChatClient(String host, int port, String username) throws IOException {
        this.ownUsername = username;

        socket = new Socket(host, port);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println(username);

        // ---- Window setup ----
        setTitle("Deepak's room  —  " + username);
        setSize(760, 520);
        setMinimumSize(new Dimension(480, 360));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });

        getContentPane().setBackground(BG_DEEP);
        setLayout(new BorderLayout());

        // ---- Header bar ----
        JPanel header = buildHeader(username);
        add(header, BorderLayout.NORTH);

        // ---- Chat pane ----
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(BG_DEEP);
        chatPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        chatPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        doc = chatPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, BORDER_DIM));
        scroll.setBackground(BG_DEEP);
        scroll.getViewport().setBackground(BG_DEEP);
        scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        add(scroll, BorderLayout.CENTER);

        // ---- Input row ----
        JPanel bottom = buildInputPanel();
        add(bottom, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
        messageField.requestFocusInWindow();

        // ---- Receiver thread ----
        Thread receiver = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> appendMessage(msg));
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> appendSystemMessage("[Disconnected from server]"));
            }
        }, "receiver");
        receiver.setDaemon(true);
        receiver.start();
    }

    // -----------------------------------------------------------------------
    // UI builders
    // -----------------------------------------------------------------------

    private JPanel buildHeader(String username) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(BG_PANEL);
        header.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_DIM),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left: status dot + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel dot = new JLabel("\u25CF"); // filled circle
        dot.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        dot.setForeground(ACCENT_GREEN);

        JLabel title = new JLabel("Deepak's Room");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setForeground(TEXT_PRIMARY);
        title.putClientProperty("html.disable", Boolean.TRUE);

        left.add(dot);
        left.add(title);

        // Right: username badge
        JLabel badge = new JLabel(username.toUpperCase());
        badge.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        badge.setForeground(ACCENT_TEAL);
        badge.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_DIM, 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));

        header.add(left, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);
        return header;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_DIM),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        messageField = new JTextField();
        messageField.setBackground(BG_SURFACE);
        messageField.setForeground(TEXT_PRIMARY);
        messageField.setCaretColor(ACCENT_TEAL);
        messageField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        messageField.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_DIM, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        // Placeholder text
        messageField.setText("Type a message...");
        messageField.setForeground(TEXT_MUTED);
        messageField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (messageField.getText().equals("Type a message...")) {
                    messageField.setText("");
                    messageField.setForeground(TEXT_PRIMARY);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (messageField.getText().isEmpty()) {
                    messageField.setText("Type a message...");
                    messageField.setForeground(TEXT_MUTED);
                }
            }
        });

        JButton sendBtn = new JButton("SEND");
        sendBtn.setBackground(BG_SURFACE);
        sendBtn.setForeground(ACCENT_TEAL);
        sendBtn.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        sendBtn.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(ACCENT_TEAL, 1),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                sendBtn.setBackground(new Color(0x0F, 0x3A, 0x5A));
            }
            @Override public void mouseExited(MouseEvent e) {
                sendBtn.setBackground(BG_SURFACE);
            }
        });

        ActionListener sendAction = e -> {
            String text = messageField.getText().trim();
            if (!text.isEmpty() && !text.equals("Type a message...") && writer != null) {
                writer.println(text);
                messageField.setText("");
                messageField.setForeground(TEXT_PRIMARY);
            }
            messageField.requestFocusInWindow();
        };
        sendBtn.addActionListener(sendAction);
        messageField.addActionListener(sendAction);

        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendBtn, BorderLayout.EAST);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Message rendering
    // -----------------------------------------------------------------------

    /**
     * Parses lines in the format "Username: message body" and applies
     * per-sender colour to the name label, with muted body text.
     * Lines that don't match (system events) are rendered as centred notices.
     */
    private void appendMessage(String raw) {
        // Detect system / join / leave lines (no colon separator typical of
        // broadcast format, or explicit bracket notation)
        if (raw.startsWith("[") || !raw.contains(": ")) {
            appendSystemMessage(raw);
            return;
        }

        int colonIdx = raw.indexOf(": ");
        String sender = raw.substring(0, colonIdx);
        String body   = raw.substring(colonIdx + 2);

        boolean isOwn = sender.equalsIgnoreCase(ownUsername);
        Color nameColor = isOwn ? ACCENT_TEAL : getSenderColor(sender);
        Color bodyColor = isOwn ? TEXT_OWN    : TEXT_PRIMARY;

        try {
            // Left accent rule (3-px coloured spacer via tab stop trick)
            SimpleAttributeSet accent = new SimpleAttributeSet();
            StyleConstants.setBackground(accent, nameColor);
            StyleConstants.setForeground(accent, nameColor);
            StyleConstants.setFontSize(accent, 13);
            doc.insertString(doc.getLength(), " ", accent);

            // Spacer
            SimpleAttributeSet space = new SimpleAttributeSet();
            StyleConstants.setForeground(space, BG_DEEP);
            StyleConstants.setFontSize(space, 13);
            doc.insertString(doc.getLength(), "  ", space);

            // Sender name
            SimpleAttributeSet nameStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(nameStyle, nameColor);
            StyleConstants.setBold(nameStyle, true);
            StyleConstants.setFontSize(nameStyle, 12);
            StyleConstants.setFontFamily(nameStyle, Font.MONOSPACED);
            doc.insertString(doc.getLength(), sender.toUpperCase() + "  ", nameStyle);

            // Message body
            SimpleAttributeSet bodyStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(bodyStyle, bodyColor);
            StyleConstants.setFontSize(bodyStyle, 13);
            StyleConstants.setFontFamily(bodyStyle, Font.SANS_SERIF);
            doc.insertString(doc.getLength(), body + "\n", bodyStyle);

        } catch (BadLocationException ignored) {}

        chatPane.setCaretPosition(doc.getLength());
    }

    private void appendSystemMessage(String text) {
        try {
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, BORDER_DIM);
            StyleConstants.setFontSize(style, 11);
            StyleConstants.setFontFamily(style, Font.MONOSPACED);
            StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
            doc.setParagraphAttributes(doc.getLength(), 1, style, false);
            doc.insertString(doc.getLength(), "\n— " + text + " —\n\n", style);
        } catch (BadLocationException ignored) {}
        chatPane.setCaretPosition(doc.getLength());
    }

    private Color getSenderColor(String sender) {
        if (!senderColorMap.containsKey(sender)) {
            senderColorMap.put(sender, nextColorIndex % SENDER_COLORS.length);
            nextColorIndex++;
        }
        return SENDER_COLORS[senderColorMap.get(sender)];
    }

    // -----------------------------------------------------------------------
    // Disconnect
    // -----------------------------------------------------------------------

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // -----------------------------------------------------------------------
    // Login dialog
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
    try {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception e) {
        e.printStackTrace();
    }
    SwingUtilities.invokeLater(ChatClient::showLoginDialog);
}
    private static void showLoginDialog() {
        // ---- Style the dialog itself ----
        JDialog dialog = new JDialog((Frame) null, "Deepak's Room — Connect", true);
        dialog.setSize(360, 310);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);
        dialog.getContentPane().setBackground(BG_DEEP);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG_DEEP);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        // Title
        JLabel titleLabel = new JLabel("Deepaks's Room");
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        titleLabel.setForeground(ACCENT_TEAL);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(titleLabel);

        JLabel subtitleLabel = new JLabel("enter your credentials to connect");
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(subtitleLabel);
        root.add(Box.createVerticalStrut(20));

        // Fields
        JTextField hostField = styledField("localhost");
        JTextField portField = styledField("12345");
        JTextField userField = styledField("");

        root.add(fieldRow("SERVER HOST", hostField));
        root.add(Box.createVerticalStrut(10));
        root.add(fieldRow("PORT", portField));
        root.add(Box.createVerticalStrut(10));
        root.add(fieldRow("USERNAME", userField));
        root.add(Box.createVerticalStrut(20));

        // Connect button
        JButton connectBtn = new JButton("CONNECT");
        connectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        connectBtn.setBackground(new Color(0x0F, 0x3A, 0x5A));
        connectBtn.setForeground(ACCENT_TEAL);
        connectBtn.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        connectBtn.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(ACCENT_TEAL, 1),
            BorderFactory.createEmptyBorder(6, 0, 6, 0)
        ));
        connectBtn.setFocusPainted(false);
        connectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        root.add(connectBtn);

        dialog.add(root);

        // ---- Action ----
        final boolean[] confirmed = {false};
        ActionListener connect = e -> {
            confirmed[0] = true;
            dialog.dispose();
        };
        connectBtn.addActionListener(connect);
        userField.addActionListener(connect);
        portField.addActionListener(connect);
        hostField.addActionListener(connect);

        dialog.setVisible(true);

        if (!confirmed[0]) return;

        String host     = hostField.getText().trim();
        String portText = portField.getText().trim();
        String username = userField.getText().trim();

        if (username.isEmpty()) {
            showError("Username cannot be empty.");
            showLoginDialog();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            showError("Invalid port number: " + portText);
            showLoginDialog();
            return;
        }

        try {
            new ChatClient(host, port, username);
        } catch (IOException ex) {
            showError("Could not connect to " + host + ":" + port + "\n" + ex.getMessage());
            showLoginDialog();
        }
    }

    private static JTextField styledField(String text) {
        JTextField f = new JTextField(text);
        f.setBackground(BG_SURFACE);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(ACCENT_TEAL);
        f.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_DIM, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return f;
    }

    private static JPanel fieldRow(String labelText, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private static void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Connection Error",
                JOptionPane.ERROR_MESSAGE);
    }

    // -----------------------------------------------------------------------
    // Dark scrollbar UI
    // -----------------------------------------------------------------------

    private static class DarkScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor      = BORDER_DIM;
            trackColor      = BG_DEEP;
            thumbDarkShadowColor = BG_DEEP;
            thumbHighlightColor  = BG_DEEP;
            thumbLightShadowColor = BG_DEEP;
        }
        @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
    }
}