## Heartbeat Tactic Implementation for Autonomous Vehicle System

### Languages & Frameworks
- Java 11+ (no external dependencies required)
- Standard Libraries:
  - `java.net` - TCP communication between processes
  - `java.io` - Input/Output stream handling
  - `java.util.Random` - Non-deterministic failure simulation
  - `java.lang.Thread` - Sleep and timing functions

### Running the System
- Clone the project : `git clone https://github.com/SWEN-755-Heartbeat-tactic/Obstacle-Detection-Heartbeat-Tactic`

#### Compile the Java files:
- `javac ObstacleDetector.java`
- `javac HeartbeatMonitor.java`

#### Run the Java files in different terminals:
- Terminal 1 - Start the Obstacle Detector
    - `java ObstacleDetector`

- Terminal 2 - Start the Heartbeat Monitor:
    - `java HeartbeatMonitor`

### Core Implementation Files
1. ObstacleDetector.java - Critical process
    - Implements obstacle detection functionality
    - Simulates random failures (crash/unresponsive)
    - Listens on TCP port 9999 for heartbeats

2. HeartbeatMonitor.java - Monitoring process
    - Sends periodic heartbeat requests
    - Detects failures via timeouts
    - Triggers emergency protocol after consecutive failures

### How it works 
- The monitor sends `HEARTBEAT` every 2 seconds to the detector
- The detector responds with `ALIVE | Status: <Clear/Obstacle Detected>` if functioning normally
- Random failures are simulated:
    - Crash - closes connection immediately
    - Unresponsive - delays response for 10 seconds
- The monitor detects timeouts and counts consecutive failures
- After 3 failures, the emergency protocol is triggered