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

    public void startMonitoring(String host, int port) {
        System.out.println("------ HeartbeatMonitor started ------");

        while (true) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeout * 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("HEARTBEAT");
                String response = in.readLine();

                if (response != null && response.startsWith("ALIVE")) {
                    failures = 0;
                    System.out.println("Success: " + response);
                } else {
                    failures++;
                    System.out.println("Fail: Failure #" + failures);
                }
            } catch (IOException e) {
                failures++;
                System.out.println("Fail: Failure #" + failures);
            }

            if (failures >= maxFailures) {
                System.out.println("CRITICAL FAILURE DETECTED - triggering emergency protocol");
                break;
            }

            try { Thread.sleep(interval * 1000); } catch (InterruptedException e) {}
        }
    }

    public static void main(String[] args) {
        HeartbeatMonitor monitor = new HeartbeatMonitor(2, 3, 3);
        monitor.startMonitoring("localhost", 9999);
    }
}
