import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Integration test for the ChatServer / ClientHandler pair.
 *
 * Starts the server on a dedicated test port, connects two clients,
 * and verifies that messages are correctly broadcast between them.
 *
 * Run from the project root after compiling:
 *   javac -cp out -d out test/ChatIntegrationTest.java
 *   java  -ea -cp out ChatIntegrationTest
 */
public class ChatIntegrationTest {

    private static final int TEST_PORT = 13580;
    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        // Start the server in a background daemon thread
        Thread serverThread = new Thread(() -> {
            try { server.ChatServer.start(TEST_PORT); }
            catch (IOException ignored) { /* normal on shutdown */ }
        }, "test-server");
        serverThread.setDaemon(true);
        serverThread.start();

        // Give the server a moment to bind the port
        Thread.sleep(300);

        testSingleClient();
        testTwoClients();

        if (failures == 0) {
            System.out.println("\n=== ALL TESTS PASSED ===");
        } else {
            System.err.println("\n=== " + failures + " TEST(S) FAILED ===");
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------

    private static void testSingleClient() throws Exception {
        System.out.println("\n-- testSingleClient --");
        try (Socket sock = new Socket("localhost", TEST_PORT)) {
            BufferedReader r = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            PrintWriter    w = new PrintWriter(sock.getOutputStream(), true);

            w.println("Solo");
            String welcome = r.readLine();
            System.out.println("  welcome: " + welcome);
            assertTrue("welcome contains username", welcome.contains("Solo"));
        }
        System.out.println("  PASS");
    }

    private static void testTwoClients() throws Exception {
        System.out.println("\n-- testTwoClients --");

        try (
            Socket sockA = new Socket("localhost", TEST_PORT);
            Socket sockB = new Socket("localhost", TEST_PORT);
        ) {
            BufferedReader rA = new BufferedReader(new InputStreamReader(sockA.getInputStream()));
            PrintWriter    wA = new PrintWriter(sockA.getOutputStream(), true);
            wA.println("Alice");
            String welcomeA = rA.readLine();
            System.out.println("  Alice welcome: " + welcomeA);
            assertTrue("Alice welcome ok", welcomeA.contains("Alice"));

            BufferedReader rB = new BufferedReader(new InputStreamReader(sockB.getInputStream()));
            PrintWriter    wB = new PrintWriter(sockB.getOutputStream(), true);
            wB.println("Bob");
            String welcomeB = rB.readLine();
            System.out.println("  Bob welcome: " + welcomeB);
            assertTrue("Bob welcome ok", welcomeB.contains("Bob"));

            // Alice should see a join notification for Bob
            String joinNotice = rA.readLine();
            System.out.println("  Alice sees: " + joinNotice);
            assertTrue("Alice sees Bob join", joinNotice.contains("Bob"));

            // Alice sends a message
            wA.println("Hello Bob!");

            // Alice receives the echo of her own message
            String aliceEcho = rA.readLine();
            System.out.println("  Alice echo: " + aliceEcho);
            assertTrue("echo contains text",   aliceEcho.contains("Hello Bob!"));
            assertTrue("echo contains sender", aliceEcho.contains("Alice"));

            // Bob receives the broadcast of Alice's message
            String bobMsg = rB.readLine();
            System.out.println("  Bob receives: " + bobMsg);
            assertTrue("broadcast contains text",   bobMsg.contains("Hello Bob!"));
            assertTrue("broadcast contains sender", bobMsg.contains("Alice"));
        }
        System.out.println("  PASS");
    }

    // ------------------------------------------------------------------

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            System.err.println("  FAIL: " + label);
            failures++;
        }
    }
}
