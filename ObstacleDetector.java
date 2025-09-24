import java.io.*;
import java.net.*;
import java.util.Random;

public class ObstacleDetector {
    private int port;
    private double failureChance;
    private Random random = new Random();

    public ObstacleDetector(int port, double failureChance) {
        this.port = port;
        this.failureChance = failureChance;
    }

    private String detectObstacle() {
        return random.nextBoolean() ? "Obstacle Detected" : "Clear";
    }

    public void runServer() throws IOException {
        ServerSocket server = new ServerSocket(port);
        System.out.println("ObstacleDetector running on port " + port);

        while (true) {
            Socket client = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            String data = in.readLine();

            // Simulated failure
            if (random.nextDouble() < failureChance) {
                if (random.nextBoolean()) {
                    System.out.println("ObstacleDetector CRASHED (simulated)!");
                    client.close();
                    continue;
                } else {
                    System.out.println("ObstacleDetector UNRESPONSIVE (simulated)!");
                    try { Thread.sleep(10000); } catch (InterruptedException e) {}
                    client.close();
                    continue;
                }
            }

            // Normal heartbeat
            if ("HEARTBEAT".equals(data)) {
                String response = "ALIVE | Status: " + detectObstacle();
                out.println(response);
            }

            client.close();
        }
    }

    public static void main(String[] args) throws IOException {
        ObstacleDetector detector = new ObstacleDetector(9999, 0.3);
        detector.runServer();
    }
}
