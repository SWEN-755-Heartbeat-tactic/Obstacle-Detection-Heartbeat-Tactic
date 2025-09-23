## Heartbeat Tactic Implementation for Autonomous Vehicle System

### Languages & Frameworks
- Python 3.6+ (No external dependencies required)
- Standard Libraries : 
    - `socket` - TCP communication between processes
    - `threading` - Process isolation (simulated)
    - `random` - Non-deterministic failure simulation
    - `time` - Timing and sleep functions

#### Running the System
- Clone the project : `git clone https://github.com/SWEN-755-Heartbeat-tactic/Obstacle-Detection-Heartbeat-Tactic`

- Terminal 1 - Start the Obstacle Detector
    - `python obstacle_detector.py`

- Terminal 2 - Start the Heartbeat Monitor:
    - `python heartbeat_monitor.py`

### Core Implementation Files
1. obstacle_detector.py - Critical process
    - Implements obstacle detection functionality
    - Simulates random failures (crash/unresponsive)
    - Listens on TCP port 9999 for heartbeats

2. heartbeat_monitor.py - Monitoring process
    - Sends periodic heartbeat requests
    - Detects failures via timeouts
    - Triggers emergency protocol after consecutive failures

### How it works 
- Monitor sends "HEARTBEAT" every 2 seconds to detector
- Detector responds with status if working normally
- Random failures simulated - either crash (immediate) or hang (10 seconds)
- Monitor detects timeouts and counts failures
- Emergency protocol activates after 3 failures