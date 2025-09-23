import socket
import time
import random

# Class for Obstacle Detector functionality 
class ObstacleDetector:
    def __init__(self, port=9999, failure_chance=0.2):
        self.port = port
        self.failure_chance = failure_chance

    def run_server(self):
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.bind(('localhost', self.port))
        server.listen(5)
        print(f"ObstacleDetector running on port {self.port}")

        while True:
            conn, addr = server.accept()
            data = conn.recv(1024).decode()

            # Random crash/unresponsiveness
            if random.random() < self.failure_chance:
                print("-.- ObstacleDetector CRASHED (simulated)!")
                time.sleep(10)  # simulate being unresponsive
            else:
                if data == "HEARTBEAT":
                    conn.send("ALIVE".encode())

            conn.close()


if __name__ == "__main__":
    detector = ObstacleDetector(port=9999, failure_chance=0.2)
    detector.run_server()
