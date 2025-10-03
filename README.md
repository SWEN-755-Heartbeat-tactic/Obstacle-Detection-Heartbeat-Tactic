## Heartbeat Tactic with Passive Redundancy (Checkpointing) 
- This project implements a heartbeat-based fault detection and recovery mechanism for an autonomous-vehicle Obstacle Detection service. It uses passive redundancy: a Primary process serves requests and continuously checkpoints its latest state to a Backup. A Monitor sends periodic heartbeats, detects failures after N consecutive misses, and fails over to the Backup. Also, it can fail back to the Primary after it proves healthy again.

### Languages & Frameworks
- Java 11+ (no external dependencies required)
- Standard Libraries:
  - `java.net` - TCP communication between processes
  - `java.io` - Input/Output stream handling
  - `java.util.Random` - Non-deterministic failure simulation
  - `java.lang.Thread` - Sleep and timing functions

### Components
1. ObstacleDetector (Primary)
    - Serves heartbeats on a TCP port.
    - Simulates failures (random crash or unresponsive hang).
    - Publishes checkpoints (latest `lastState`) every 1s to the Backup on a dedicated checkpoint port.

2. BackupReplica (Backup)
    - Listens for checkpoints from Primary (dedicated port).
    - Serves heartbeat responses on its heartbeat port using the last checkpointed state.
    - Tags responses with `[STALE]` if no checkpoint has arrived for 5s (configurable via `STALE_WINDOW_MS` constant).

3. HeartbeatMonitor (Monitor)
    - Sends heartbeats every interval seconds, with a per-request timeout.
    - Declares failure after maxFailures consecutive misses and switches to backup.
    - while on backup, periodically probes primary, if it passes okStreakForFailback consecutive probes, the monitor fails back to primary.

### Running the System
- Clone the project : `git clone https://github.com/SWEN-755-Heartbeat-tactic/Obstacle-Detection-Heartbeat-Tactic`

- Checkout to FaultDetectionAndRecovery Branch : `git checkout FaultDetectionAndRecovery`

#### Compile the Java files:
- `javac ObstacleDetector.java BackupReplica.java HeartbeatMonitor.java`

#### Run the components in separate terminals:
- Terminal 1 - Start Backup (heartbeat + checkpoint listener) :
    - `java BackupReplica 9998 7003`
        - 9998 is Backup heartbeat port (monitor will ping this)
        - 7003 is Checkpoint port (Primary pushes state here)

- Terminal 2 - Start Primary (with failure simulation + checkpoint publisher) :
    - `java ObstacleDetector 9999 0.30 localhost 7003`
        - 9999 is Primary heartbeat port (monitor pings this first)
        - 0.30 is Failure chance per request (30% crash/hang simulation)
        - localhost is Backup host to send checkpoints to
        - 7003 is Backup checkpoint port (must match Backup's second arg)

- Terminal 3 - Start Monitor (heartbeat, failover, optional failback) :
    - `java HeartbeatMonitor 2 3 3 localhost 9999 localhost 9998 5 2`
        - first 2 is Heartbeat interval (seconds between pings)
        - first 3 is Timeout (seconds to wait for a reply)
        - second 3 is Max failures before declaring target down
        - first localhost is Primary host
        - 9999 is Primary port
        - second localhost is Backup host
        - 9998 is Backup port
        - 5 is Probe Primary every N beats while on Backup
        - second 2 is Consecutive OK probes needed to fail back to Primary
        
#### Run (Two-Machine Demo)
Use two hosts on the same network:
- Machine B (Backup):
    - `java BackupReplica 9998 7003`
        - 9998 is Backup heartbeat port
        - 7003 is Checkpoint port (Primary sends state here)

- Machine A (Primary):
    - `java ObstacleDetector 9999 0.30 <B_HOSTNAME_OR_IP> 7003`
        - 9999 is Primary heartbeat port
        - 0.30 is Failure chance
        - `<B_HOSTNAME_OR_IP>` is Backup's host/IP (of Machine B)
        - 7003 is Backup's checkpoint port (must match Machine B)

- Monitor (A, B, or a third machine):
    - `java HeartbeatMonitor 2 3 3 <A_HOST> 9999 <B_HOST> 9998 5 2`
        - first 2 Heartbeat interval (s)
        - first 3 is Timeout (s)
        - second 3 is Max failures before failover
        - `<A_HOST>` is Primary host (Machine A)
        - 9999 is Primary port
        - `<B_HOST>` is Backup host (Machine B)
        - 9998 is Backup port
        - 5 is Probe Primary every 5 beats while on Backup
        - second 2 is Need 2 OK probes in a row to fail back

### How it works 
- Normal: Monitor → Primary: `"HEARTBEAT"` → Primary replies `"ALIVE | Status: <Clear|Obstacle Detected>"`.
- Checkpointing: Primary publishes `lastState` every 1s to Backup (`backupHost:checkpointPort`).
- Failover: Monitor misses `maxFailures` in a row → switches to Backup.
- Backup replies with `"ALIVE | Status: Backup <state>"`, where `<state>` is the last checkpointed state. If no checkpoint for 5s, appends `"[STALE]"`.
- Failback : While on Backup, Monitor probes Primary every `probePrimaryEveryNBeats`; after `okStreakForFailback` consecutive successes, it fails back.