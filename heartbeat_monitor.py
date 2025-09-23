import socket
import time

# Class for continous Heartbeat monitoring
class HeartbeatMonitor:
    def __init__(self, interval=3, timeout=5, max_failures=3):
        self.interval = interval
        self.timeout = timeout
        self.max_failures = max_failures
        self.failures = 0

    def start_monitoring(self, host='localhost', port=9999):
        print("------ HeartbeatMonitor started ------")
        while True:
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(self.timeout)
                sock.connect((host, port))
                sock.send("HEARTBEAT".encode())
                response = sock.recv(1024).decode()
                sock.close()

                if response.startswith("ALIVE"):
                    self.failures = 0
                    print(f"Success : {time.ctime()}: {response}")
                else:
                    self.failures += 1
                    print(f"Fail : {time.ctime()}: Failure #{self.failures}")
            except:
                self.failures += 1
                print(f"Fail : {time.ctime()}: Failure #{self.failures}")

            # If too many failures, trigger emergency
            if self.failures >= self.max_failures:
                print("(┬┬﹏┬┬) CRITICAL FAILURE DETECTED - triggering emergency protocol")
                break

            time.sleep(self.interval)

# Allowed standalone use
if __name__ == "__main__":
    monitor = HeartbeatMonitor(interval=2, timeout=3, max_failures=3)
    monitor.start_monitoring()
