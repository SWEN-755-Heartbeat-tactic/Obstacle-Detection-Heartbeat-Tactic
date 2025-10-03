import java.io.*;
import java.net.*;

/**
 * Heartbeat monitor — pings target (PRIMARY or BACKUP) every 'interval' seconds.
 * - Declares failure after 'maxFailures' consecutive misses, then fails over to BACKUP.
 * - Optional failback: when on BACKUP, it probes PRIMARY periodically; if PRIMARY
 *   responds 'okStreakForFailback' times in a row, switch back to PRIMARY.
 */
public class HeartbeatMonitor {
    private final int intervalSec;
    private final int timeoutSec;
    private final int maxFailures;

    private final String primaryHost;
    private final int primaryPort;
    private final String backupHost;
    private final int backupPort;

    // Failback tuning
    private final int probePrimaryEveryNBeats;
    private final int okStreakForFailback;

    public HeartbeatMonitor(
            int intervalSec,
            int timeoutSec,
            int maxFailures,
            String primaryHost,
            int primaryPort,
            String backupHost,
            int backupPort,
            int probePrimaryEveryNBeats,
            int okStreakForFailback) {
        this.intervalSec = intervalSec;
        this.timeoutSec = timeoutSec;
        this.maxFailures = maxFailures;
        this.primaryHost = primaryHost;
        this.primaryPort = primaryPort;
        this.backupHost = backupHost;
        this.backupPort = backupPort;
        this.probePrimaryEveryNBeats = probePrimaryEveryNBeats;
        this.okStreakForFailback = okStreakForFailback;
    }

    public void start() {
        boolean usingPrimary = true;
        int failures = 0;

        int beatsSinceLastProbe = 0;
        int primaryOkStreak = 0;

        while (true) {
            String host = usingPrimary ? primaryHost : backupHost;
            int port = usingPrimary ? primaryPort : backupPort;

            boolean ok = pingOnce(host, port);
            if (ok) {
                failures = 0;
                System.out.println((usingPrimary ? "[PRIMARY]" : "[BACKUP]") + " OK: ALIVE");
            } else {
                failures++;
                System.out.println((usingPrimary ? "[PRIMARY]" : "[BACKUP]") + " MISS #" + failures);
            }

            // Failover if needed
            if (failures >= maxFailures) {
                if (usingPrimary) {
                    System.out.println("[MONITOR] Switching to BACKUP after " + failures + " misses.");
                    usingPrimary = false;
                    failures = 0;
                    primaryOkStreak = 0; // reset
                    beatsSinceLastProbe = 0;
                } else {
                    System.out.println("[MONITOR] CRITICAL: Both PRIMARY and BACKUP appear down. Exiting.");
                    break;
                }
            }

            // Failback logic: when on BACKUP, occasionally probe PRIMARY
            if (!usingPrimary) {
                beatsSinceLastProbe++;
                if (beatsSinceLastProbe >= probePrimaryEveryNBeats) {
                    beatsSinceLastProbe = 0;
                    boolean primaryOk = pingOnce(primaryHost, primaryPort);
                    if (primaryOk) {
                        primaryOkStreak++;
                        System.out.println("[MONITOR] Primary probe OK (" + primaryOkStreak + "/" + okStreakForFailback + ")");
                        if (primaryOkStreak >= okStreakForFailback) {
                            System.out.println("[MONITOR] Failing back to PRIMARY.");
                            usingPrimary = true;
                            failures = 0;
                            primaryOkStreak = 0;
                        }
                    } else {
                        primaryOkStreak = 0; // break the streak
                        System.out.println("[MONITOR] Primary probe failed; stay on BACKUP.");
                    }
                }
            }

            try { Thread.sleep(intervalSec * 1000L); } catch (InterruptedException e) { break; }
        }
    }

    private boolean pingOnce(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutSec * 1000);
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println("HEARTBEAT");
                socket.setSoTimeout(timeoutSec * 1000);
                String response = in.readLine();
                return response != null && response.startsWith("ALIVE");
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        // Args:
        // [intervalSec=2] [timeoutSec=3] [maxFailures=3]
        // [primaryHost=localhost] [primaryPort=9999]
        // [backupHost=localhost]  [backupPort=9998]
        // [probePrimaryEveryNBeats=5] [okStreakForFailback=2]
        int idx = 0;
        int intervalSec = args.length > idx ? Integer.parseInt(args[idx++]) : 2;
        int timeoutSec = args.length > idx ? Integer.parseInt(args[idx++]) : 3;
        int maxFailures = args.length > idx ? Integer.parseInt(args[idx++]) : 3;

        String primaryHost = args.length > idx ? args[idx++] : "localhost";
        int primaryPort = args.length > idx ? Integer.parseInt(args[idx++]) : 9999;

        String backupHost = args.length > idx ? args[idx++] : "localhost";
        int backupPort = args.length > idx ? Integer.parseInt(args[idx++]) : 9998;

        int probeEvery = args.length > idx ? Integer.parseInt(args[idx++]) : 5;
        int okStreak = args.length > idx ? Integer.parseInt(args[idx])   : 2;

        HeartbeatMonitor monitor = new HeartbeatMonitor(
                intervalSec, timeoutSec, maxFailures,
                primaryHost, primaryPort, backupHost, backupPort,
                probeEvery, okStreak
        );
        monitor.start();
    }
}
