# Sprint 3 - Station Management System

This directory contains the implementation of Sprint 3 components for the Station Management System according to the specification.

## Architecture Overview

The system consists of five main components:

1. **Manager** (C for Linux/RV32IM) - Main coordination component
2. **UI** (C for Linux/RV32IM) - Console-based user interface
3. **Board** (C for Linux/RV32IM) - Display/synoptic board
4. **Sensors** (C/C++ for Arduino) - Temperature and humidity sensors
5. **LightSigns** (C/C++ for Arduino) - Track signal LEDs

## Directory Structure

```
sprint3/
├── manager/         # Manager component (USAC11-16)
│   ├── main.c       # Main entry point with algorithm loop
│   ├── data_structures.h  # Data structures (USAC11)
│   ├── initialize.c       # Initialization from file (USAC11)
│   ├── user_auth.c        # Login/authentication (Manager algorithm)
│   ├── logging.c          # Action logging (USAC12)
│   ├── sensor_manager.c   # Sensor communication (USAC13)
│   ├── lightsigns_comm.c  # LightSigns communication (USAC14)
│   ├── board_comm.c       # Board communication (USAC15)
│   ├── track_manager.c    # Track management (USAC16)
│   └── ...
├── ui/              # UI component (Console Interface)
│   ├── main.c       # UI main entry point
│   ├── ui.h         # UI header
│   ├── ui.c         # UI implementation
│   ├── input_handler.c  # Robust input handling
│   ├── input_handler.h  # Input handler header
│   └── Makefile
├── board/           # Board component (USAC15-16)
│   ├── main.c       # Board main loop
│   ├── board.h      # Board header
│   ├── board.c      # Board display functions
│   └── Makefile
└── shared/          # Shared headers
    └── communication.h  # Communication protocol constants
```

## Component Responsibilities

### Manager Component
The Manager component manages the railway station and coordinates all other components. It implements the following algorithm:

```
1. user = login(username, password)
2. while(1) {
3.     send_cmd_to_sensors("GTH")
4.     str = wait_for_data_from_sensors()
5.     extract_data(str, data)
6.     update_sensors_data(data)
7.     inst = wait_for_instructions_from_ui()
8.     {data, cmd} = process(inst)
9.     send_data_to_board(data)
10.    send_cmd_to_lightsigns(cmd)
11.    record(user, inst)
12. }
```

**User Stories:**
- **USAC11**: Initial setup of data structures using a text file
- **USAC12**: Create text file with sequence of actions performed by a user
- **USAC13**: Get data from Sensors component
- **USAC14**: Control the track sign light
- **USAC15**: Send data to Board component
- **USAC16**: Manage railway tracks (assign, maintenance, free, departure)

### UI Component
The UI component provides a console-based user interface that is robust and handles error input. It communicates with the Manager component to send instructions and receive feedback.

**Features:**
- Robust input validation (handles malicious/error input)
- Menu-driven interface
- User-friendly error messages
- Command parsing and validation

### Board Component
The Board component displays information in a "funny" way using icons, symbols, boxes, and lines. It receives data from the Manager component and displays it in the console.

### Sensors Component
Located in `stationMngt/USAC10/sprint3/sensors/` (Arduino). Provides temperature and humidity readings using moving median filter.

### LightSigns Component
Located in `stationMngt/USAC10/sprint3/lightsigns/` (Arduino). Controls track signal LEDs (Red, Yellow, Green, Red Blinking).

## Build Instructions

### Manager Component
```bash
cd sprint3/manager
make
./manager.elf config_example.txt
```

### UI Component
```bash
cd sprint3/ui
make
./ui.elf
```

### Board Component
```bash
cd sprint3/board
make
./board.elf
```

## Communication Protocol

### Manager ↔ UI
The UI sends text instructions to the Manager via stdin/stdout:
- `ASSIGN_TRACK:<train_id>` - Assign track to train
- `SET_MAINTENANCE:<track_id>` - Set track to maintenance
- `SET_FREE:<track_id>` - Free a track
- `DEPART:<track_id>` - Issue departure order
- `SYNOPSIS` - Request track status synopsis
- `EXIT` - Exit the system

### Manager ↔ Sensors
Serial communication at 9600 baud:
- Command: `GTH` (Get Temperature and Humidity)
- Response: `TEMP&unit:celsius&value:xx#HUM&unit:percentage&value:xx`

### Manager ↔ Board
Serial communication with packet protocol:
- Format: `[STX][DATA][CHKSUM][LF]`
- Commands: Track status updates, synopsis, emergency stop

### Manager ↔ LightSigns
Serial communication at 9600 baud:
- Format: `CMD,x` where CMD is RE/YE/GE/RB and x is track number

## Assembly Functions Used

The Manager component uses assembly functions from Sprint 2 (USAC01-09):
- USAC01: `encrypt_data` - Password encryption
- USAC02: `decrypt_data` - Password decryption
- USAC03: `extract_data` - Parse sensor data
- USAC04: `format_command` - Format light commands
- USAC05-09: Circular buffer, sorting, median operations

## Testing

Each component can be tested independently or integrated together. The Manager can run in simulation mode if hardware ports are not available.

## Architecture Requirements

- **Language**: C for Linux components (Manager, UI, Board), C/C++ for Arduino (Sensors, LightSigns)
- **Architecture**: RV32IM for Linux components
- **Memory**: All arrays must be dynamically allocated
- **Assembly**: Must use Sprint 2 assembly functions
- **Testing**: C unit tests required

## File Organization by USAC

### USAC11 (Initial Setup)
- `manager/data_structures.h`
- `manager/initialize.c`
- `manager/config_example.txt`

### USAC12 (Logging)
- `manager/logging.h`
- `manager/logging.c`

### USAC13 (Sensor Communication)
- `manager/sensor_data.h`
- `manager/sensor_data.c`
- `manager/sensor_manager.h`
- `manager/sensor_manager.c`
- `manager/serial_comm.h`
- `manager/serial_comm.c`

### USAC14 (LightSigns Communication)
- `manager/lightsigns_comm.h`
- `manager/lightsigns_comm.c`

### USAC15 (Board Communication)
- `manager/board_comm.h`
- `manager/board_comm.c`
- `board/board.h`
- `board/board.c`
- `board/main.c`
- `shared/communication.h`

### USAC16 (Track Management)
- `manager/track_manager.h`
- `manager/track_manager.c`
- `manager/main.c` (includes algorithm loop)

### UI Component (Console Interface)
- `ui/ui.h`
- `ui/ui.c`
- `ui/main.c`
- `ui/input_handler.c`
- `ui/input_handler.h`
- `ui/Makefile`

**Note:** The UI component provides robust input handling as required by the specification.

