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

- Checkout to FaultDetectionAndRecovery Branch : `git checkout FaultDetectionAndRecovery`

#### Compile the Java files:
- `javac ObstacleDetector.java BackupReplica.java HeartbeatMonitor.java`

#### Run the components in separate terminals:
- Terminal 1 – Start the Primary Detector :
    - `java ObstacleDetector 8888 0.3`
        - Runs primary detector on port 8888
        - 0.3 = 30% chance of simulated failure

- Terminal 2 - Start the Backup Replica :
    - `java BackupReplica 8889`
        - Runs backup replica on port 8889

- Terminal 3 - Start the Heartbeat Monitor :
    - `java HeartbeatMonitor 2 3 3`
        - Sends heartbeat every 2 seconds
        - Waits 3 seconds before timing out
        - Switches to BackupReplica after 3 consecutive failures


### Core Implementation Files
1. ObstacleDetector.java - Primary critical process
    - Implements obstacle detection functionality
    - Simulates random failures (crash/unresponsive)
    - Responds with ALIVE | Status: `<Clear OR Obstacle Detected>`

2. BackupReplica.java - Redundant replica process
    - Acts as passive backup to primary
    - Responds to heartbeat with ALIVE | Status: Backup `<Clear OR Obstacle Detected>`

3. HeartbeatMonitor.java - Monitoring and recovery process
    - Sends periodic heartbeat requests to the primary
    - Detects failures via timeouts
    - Switches automatically to the backup after threshold failures
    - Declares critical system failure if both primary and backup fail

### How it works 
- The monitor sends a HEARTBEAT request every 2 seconds.
- If the primary detector is alive, it responds with its status.
- If the primary fails (crash or unresponsive), failures accumulate.
- After 3 consecutive failures, the monitor switches to the backup replica.
- The backup continues responding, ensuring system availability.
- If both primary and backup fail, the monitor logs CRITICAL FAILURE.