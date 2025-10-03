import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * PRIMARY (critical process) — simulates nondeterministic failures (crash/unresponsive).
 * Publishes checkpoints (latest state) to BACKUP on a separate TCP port.
 */
public class ObstacleDetector {
    private final int port;
    private final double failureChance;
    private final String backupHost;
    private final int checkpointPort;
    private final Random random = new Random();

    // Shared state that gets checkpointed to backup
    private volatile String lastState = "Clear";

    public ObstacleDetector(int port, double failureChance, String backupHost, int checkpointPort) {
        this.port = port;
        this.failureChance = failureChance;
        this.backupHost = backupHost;
        this.checkpointPort = checkpointPort;
    }

    private String detectObstacle() {
        return random.nextBoolean() ? "Obstacle Detected" : "Clear";
    }

    /** Periodically push the latest state to the backup. */
    private void startCheckpointPublisher() {
        Thread t = new Thread(() -> {
            System.out.println("[PRIMARY] Checkpoint publisher -> " + backupHost + ":" + checkpointPort);
            while (true) {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(backupHost, checkpointPort), 1000);
                    try (PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                        out.println(lastState);
                    }
                } catch (IOException ignored) {
                    // backup might be down or not yet started; keeps trying
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
            }
        }, "CheckpointPublisher");
        t.setDaemon(true);
        t.start();
    }

    public void runServer() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[PRIMARY] ObstacleDetector running on port " + port);
            startCheckpointPublisher();

            while (true) {
                Socket client = server.accept();
                handleClient(client);
            }
        }
    }

    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            String data = in.readLine();

            // Simulated failure path
            if (random.nextDouble() < failureChance) {
                if (random.nextBoolean()) {
                    System.out.println("[PRIMARY] CRASHED (simulated): closing connection immediately");
                    // Crash-like: drop connection without responding
                    return; // connection closes via try-with-resources
                } else {
                    System.out.println("[PRIMARY] UNRESPONSIVE (simulated): sleeping long");
                    // Sleep longer than any reasonable timeout to simulate hang
                    try { Thread.sleep(15_000); } catch (InterruptedException ignored) {}
                    return;
                }
            }

            // Normal heartbeat handling
            if ("HEARTBEAT".equals(data)) {
                lastState = detectObstacle(); // update state and checkpoint thread will ship it
                String response = "ALIVE | Status: " + lastState;
                out.println(response);
            }
        } catch (IOException ignored) {
            // client might have already given up on timeout; ignore
        }
    }

    public static void main(String[] args) throws IOException {
        // Args: [port=9999] [failureChance=0.30] [backupHost=localhost] [checkpointPort=7003]
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9999;
        double failureChance = args.length > 1 ? Double.parseDouble(args[1]) : 0.30;
        String backupHost = args.length > 2 ? args[2] : "localhost";
        int checkpointPort = args.length > 3 ? Integer.parseInt(args[3]) : 7003;

        ObstacleDetector detector = new ObstacleDetector(port, failureChance, backupHost, checkpointPort);
        detector.runServer();
    }
}
