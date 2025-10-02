import java.io.*;
import java.net.*;
import java.util.Random;

public class BackupReplica {
    private int port;
    private Random random = new Random();

    public BackupReplica(int port) {
        this.port = port;
    }

    private String detectObstacle() {
        return random.nextBoolean() ? "Obstacle Detected" : "Clear";
    }

    public void runReplica() throws IOException {
        ServerSocket server = new ServerSocket(port);
        System.out.println("BackupReplica running on port " + port);

        while (true) {
            Socket client = server.accept();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

                String data = in.readLine();
                if ("HEARTBEAT".equals(data)) {
                    String response = "ALIVE | Status: Backup " + detectObstacle();
                    out.println(response);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9998;
        BackupReplica replica = new BackupReplica(port);
        replica.runReplica();
    }
}
