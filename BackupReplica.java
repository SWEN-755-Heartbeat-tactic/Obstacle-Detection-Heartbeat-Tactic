import java.io.*;
import java.net.*;

/**
 * BACKUP replica — serves heartbeats using the last checkpoint received from PRIMARY.
 * Listens on a checkpoint port for PRIMARY's state updates.
 */
public class BackupReplica {
    private final int heartbeatPort;
    private final int checkpointPort;

    // Checkpointed state from PRIMARY
    private volatile String lastKnownState = "Unknown";
    private volatile long lastUpdateNanos = 0L;

    // Consider state stale if no checkpoint has arrived for this window
    private static final long STALE_WINDOW_MS = 5000;

    public BackupReplica(int heartbeatPort, int checkpointPort) {
        this.heartbeatPort = heartbeatPort;
        this.checkpointPort = checkpointPort;
    }

    /** Accept checkpoints from PRIMARY on a dedicated port. */
    private void startCheckpointListener() {
        Thread t = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(checkpointPort)) {
                System.out.println("[BACKUP] Checkpoint listener on port " + checkpointPort);
                while (true) {
                    try (Socket client = server.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
                        String s = in.readLine();
                        if (s != null) {
                            lastKnownState = s;
                            lastUpdateNanos = System.nanoTime();
                            System.out.println("[BACKUP] Checkpoint received: " + lastKnownState);
                        }
                    } catch (IOException ignored) {}
                }
            } catch (IOException e) {
                System.err.println("[BACKUP] Checkpoint listener failed: " + e.getMessage());
            }
        }, "CheckpointListener");
        t.setDaemon(true);
        t.start();
    }

    /** Serve heartbeat requests from the monitor. */
    public void runReplica() throws IOException {
        startCheckpointListener();
        try (ServerSocket server = new ServerSocket(heartbeatPort)) {
            System.out.println("[BACKUP] Serving heartbeats on port " + heartbeatPort);
            while (true) {
                try {
                    Socket client = server.accept();
                    handleClient(client);
                } catch (IOException ignored) {}
            }
        }
    }

    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            String data = in.readLine();
            if ("HEARTBEAT".equals(data)) {
                boolean stale = (System.nanoTime() - lastUpdateNanos) / 1_000_000 > STALE_WINDOW_MS;
                String state = lastKnownState + (stale ? " [STALE]" : "");
                out.println("ALIVE | Status: Backup " + state);
            }
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) throws IOException {
        // Args: [heartbeatPort=9998] [checkpointPort=7003]
        int hbPort = args.length > 0 ? Integer.parseInt(args[0]) : 9998;
        int cpPort = args.length > 1 ? Integer.parseInt(args[1]) : 7003;

        BackupReplica replica = new BackupReplica(hbPort, cpPort);
        replica.runReplica();
    }
}
