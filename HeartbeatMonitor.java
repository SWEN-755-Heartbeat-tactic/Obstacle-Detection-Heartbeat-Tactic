import java.io.*;
import java.net.*;

public class HeartbeatMonitor {
    private int interval;
    private int timeout;
    private int maxFailures;
    private int failures;

    public HeartbeatMonitor(int interval, int timeout, int maxFailures) {
        this.interval = interval;
        this.timeout = timeout;
        this.maxFailures = maxFailures;
        this.failures = 0;
    }

    public void startMonitoring(String primaryHost, int primaryPort, String backupHost, int backupPort) {
        boolean usingPrimary = true;

        while (true) {
            int port = usingPrimary ? primaryPort : backupPort;
            String host = usingPrimary ? primaryHost : backupHost;

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeout * 1000);
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("HEARTBEAT");
                    String response = in.readLine();

                    if (response != null && response.startsWith("ALIVE")) {
                        failures = 0;
                        System.out.println((usingPrimary ? "[PRIMARY]" : "[BACKUP]") + " Success: " + response);
                    } else {
                        failures++;
                        System.out.println("Fail: Failure #" + failures);
                    }
                }
            } catch (IOException e) {
                failures++;
                System.out.println("Fail: Failure #" + failures);
            }

            if (failures >= maxFailures) {
                if (usingPrimary) {
                    System.out.println("Switching to BACKUP replica...");
                    usingPrimary = false;
                    failures = 0; // reset for backup
                } else {
                    System.out.println("CRITICAL FAILURE: Both primary and backup down!");
                    break;
                }
            }

            try { Thread.sleep(interval * 1000); } catch (InterruptedException e) {}
        }
    }

    public static void main(String[] args) {
        int interval = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        int timeout = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int maxFailures = args.length > 2 ? Integer.parseInt(args[2]) : 3;

        HeartbeatMonitor monitor = new HeartbeatMonitor(interval, timeout, maxFailures);
        monitor.startMonitoring("localhost", 9999, "localhost", 9998);
    }
}
