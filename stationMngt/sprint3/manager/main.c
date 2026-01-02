/*
 * Manager Component - Main Entry Point
 * 
 * Implements the Manager algorithm as specified in Sprint 3:
 * 
 * 1. user = login(username, password)
 * 2. while(1) {
 * 3.     send_cmd_to_sensors("GTH")
 * 4.     str = wait_for_data_from_sensors()
 * 5.     extract_data(str, data)
 * 6.     update_sensors_data(data)
 * 7.     inst = wait_for_instructions_from_ui()
 * 8.     {data, cmd} = process(inst)
 * 9.     send_data_to_board(data)
 * 10.    send_cmd_to_lightsigns(cmd)
 * 11.    record(user, inst)
 * 12. }
 */

#include <stdio.h>
#include <stdlib.h>
#include "manager.h"
#include "user_auth.h"
#include "sensor_manager.h"
#include "serial_comm.h"
#include "track_manager.h"

// Configuration
#define DEFAULT_SENSORS_PORT "/dev/ttyS0"
#define DEFAULT_BOARD_PORT "/dev/ttyS1"
#define DEFAULT_LIGHTSIGNS_PORT "/dev/ttyS2"
#define CONFIG_FILE "config_example.txt"
#define MAX_INSTRUCTION_LENGTH 256

// Global state
static ManagerData g_manager_data;
static SerialPort g_sensors_port;
static SerialPort g_board_port;
static SerialPort g_lightsigns_port;
static SensorData g_sensor_data;
static int g_initialized = 0;

/**
 * Custom string comparison (avoiding string.h)
 */
static int str_compare_custom(const char* s1, const char* s2) {
    if (s1 == NULL || s2 == NULL) return -1;
    while (*s1 && (*s1 == *s2)) {
        s1++;
        s2++;
    }
    return *(const unsigned char*)s1 - *(const unsigned char*)s2;
}

/**
 * Custom string length (avoiding string.h)
 */
static int str_length_custom(const char* str) {
    if (str == NULL) return 0;
    int len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

/**
 * Find substring in string (simple implementation)
 */
static int str_ncompare_custom(const char* s1, const char* s2, int n) {
    if (s1 == NULL || s2 == NULL || n <= 0) return -1;
    int i = 0;
    while (i < n && s1[i] != '\0' && s2[i] != '\0' && s1[i] == s2[i]) {
        i++;
    }
    if (i == n) return 0;
    return (unsigned char)s1[i] - (unsigned char)s2[i];
}

/**
 * Parse integer from string (custom implementation, avoiding stdlib)
 */
static int parse_integer(const char* str, int* value) {
    if (str == NULL || value == NULL) return 0;
    
    int len = str_length_custom(str);
    if (len == 0) return 0;
    
    int result = 0;
    int sign = 1;
    int start = 0;
    
    if (str[0] == '-') {
        sign = -1;
        start = 1;
    } else if (str[0] == '+') {
        start = 1;
    }
    
    for (int i = start; i < len; i++) {
        if (str[i] < '0' || str[i] > '9') {
            return 0;
        }
        result = result * 10 + (str[i] - '0');
    }
    
    *value = result * sign;
    return 1;
}

/**
 * Read line from stdin (for UI communication)
 */
static int read_line(char* buffer, int max_len) {
    if (buffer == NULL || max_len <= 0) return 0;
    
    int i = 0;
    int ch;
    while (i < max_len - 1) {
        ch = getchar();
        if (ch == EOF || ch == '\n') {
            break;
        }
        buffer[i++] = (char)ch;
    }
    buffer[i] = '\0';
    return i;
}

/**
 * Find position of character in string
 */
static int find_char(const char* str, char c) {
    if (str == NULL) return -1;
    int pos = 0;
    while (str[pos] != '\0') {
        if (str[pos] == c) {
            return pos;
        }
        pos++;
    }
    return -1;
}

/**
 * Process instruction using track_manager functions
 * Parses commands: ASSIGN_TRACK:train_id, SET_MAINTENANCE:track_id, SET_FREE:track_id, DEPART:track_id
 */
static int process_instruction(const char* instruction) {
    if (instruction == NULL) {
        return 0;
    }
    
    // Parse instruction format: "COMMAND:param1:param2"
    // Find the colon separator
    int colon_pos = find_char(instruction, ':');
    
    if (colon_pos < 0) {
        // No parameters (like SYNOPSIS)
        return 0;
    }
    
    // Extract command name (before colon)
    char command[64];
    int cmd_len = (colon_pos < 63) ? colon_pos : 63;
    for (int i = 0; i < cmd_len; i++) {
        command[i] = instruction[i];
    }
    command[cmd_len] = '\0';
    
    // Extract parameter(s) (after colon)
    const char* param_start = instruction + colon_pos + 1;
    
    // Parse command and call appropriate track_manager function
    if (str_ncompare_custom(command, "ASSIGN_TRACK", 12) == 0) {
        int train_id;
        if (parse_integer(param_start, &train_id) && train_id > 0) {
            int assigned_track_id;
            TrackOperationResult result = assign_track_to_train(&g_manager_data, train_id, &assigned_track_id);
            if (result == TRACK_OP_SUCCESS) {
                printf("SUCCESS: Assigned track %d to train %d\n", assigned_track_id, train_id);
                return 1;
            } else if (result == TRACK_OP_NO_AVAILABLE_TRACK) {
                printf("ERROR: No available track for train %d - Emergency stop issued\n", train_id);
                return 1;
            } else {
                printf("ERROR: %s\n", get_track_operation_result_string(result));
                return 0;
            }
        } else {
            printf("ERROR: Invalid train ID in ASSIGN_TRACK command\n");
            return 0;
        }
    }
    else if (str_ncompare_custom(command, "SET_MAINTENANCE", 15) == 0) {
        int track_id;
        if (parse_integer(param_start, &track_id) && track_id > 0) {
            TrackOperationResult result = set_track_maintenance(&g_manager_data, track_id);
            if (result == TRACK_OP_SUCCESS) {
                printf("SUCCESS: Track %d set to maintenance mode\n", track_id);
                return 1;
            } else {
                printf("ERROR: %s\n", get_track_operation_result_string(result));
                return 0;
            }
        } else {
            printf("ERROR: Invalid track ID in SET_MAINTENANCE command\n");
            return 0;
        }
    }
    else if (str_ncompare_custom(command, "SET_FREE", 8) == 0) {
        int track_id;
        if (parse_integer(param_start, &track_id) && track_id > 0) {
            TrackOperationResult result = free_track(&g_manager_data, track_id);
            if (result == TRACK_OP_SUCCESS) {
                printf("SUCCESS: Track %d is now free\n", track_id);
                return 1;
            } else {
                printf("ERROR: %s\n", get_track_operation_result_string(result));
                return 0;
            }
        } else {
            printf("ERROR: Invalid track ID in SET_FREE command\n");
            return 0;
        }
    }
    else if (str_ncompare_custom(command, "DEPART", 6) == 0) {
        int track_id;
        if (parse_integer(param_start, &track_id) && track_id > 0) {
            TrackOperationResult result = issue_departure_order(&g_manager_data, track_id);
            if (result == TRACK_OP_SUCCESS) {
                printf("SUCCESS: Departure order issued for track %d\n", track_id);
                return 1;
            } else {
                printf("ERROR: %s\n", get_track_operation_result_string(result));
                return 0;
            }
        } else {
            printf("ERROR: Invalid track ID in DEPART command\n");
            return 0;
        }
    }
    
    return 0;  // Unknown command
}

/**
 * Main function implementing the Manager algorithm
 */
int main(int argc, char* argv[]) {
    const char* config_file = CONFIG_FILE;
    const char* sensors_port = DEFAULT_SENSORS_PORT;
    const char* board_port = DEFAULT_BOARD_PORT;
    const char* lightsigns_port = DEFAULT_LIGHTSIGNS_PORT;
    
    // Parse command line arguments
    if (argc > 1) {
        config_file = argv[1];
    }
    if (argc > 2) {
        sensors_port = argv[2];
    }
    if (argc > 3) {
        board_port = argv[3];
    }
    if (argc > 4) {
        lightsigns_port = argv[4];
    }
    
    printf("=== Manager Component - Sprint 3 ===\n\n");
    
    // Initialize data structures from config file (USAC11)
    printf("Initializing data structures from %s...\n", config_file);
    if (initialize_from_file(config_file, &g_manager_data) != 1) {
        fprintf(stderr, "ERROR: Failed to initialize data structures\n");
        return 1;
    }
    printf("✓ Data structures initialized\n");
    printf("  Users: %d, Tracks: %d, Trains: %d\n\n", 
           g_manager_data.num_users, g_manager_data.num_tracks, g_manager_data.num_trains);
    
    // Initialize sensor data
    init_sensor_data(&g_sensor_data);
    
    // Initialize serial ports
    printf("Initializing communication ports...\n");
    if (init_serial_port(&g_sensors_port, sensors_port) != 0) {
        fprintf(stderr, "WARNING: Failed to initialize sensors port (%s) - running in simulation mode\n", sensors_port);
    } else {
        printf("  ✓ Sensors port: %s\n", sensors_port);
    }
    
    if (init_board_comm(&g_board_port, board_port) != 0) {
        fprintf(stderr, "WARNING: Failed to initialize board port (%s) - running in simulation mode\n", board_port);
    } else {
        printf("  ✓ Board port: %s\n", board_port);
    }
    
    if (init_serial_port(&g_lightsigns_port, lightsigns_port) != 0) {
        fprintf(stderr, "WARNING: Failed to initialize lightsigns port (%s) - running in simulation mode\n", lightsigns_port);
    } else {
        printf("  ✓ LightSigns port: %s\n", lightsigns_port);
    }
    printf("\n");
    
    // Initialize track manager (for USAC16 operations)
    if (init_track_manager(&g_manager_data, board_port, lightsigns_port) != 0) {
        fprintf(stderr, "WARNING: Failed to initialize track manager - some features may not work\n");
    }
    
    g_initialized = 1;
    
    // Step 1: Login
    printf("=== User Login ===\n");
    printf("Username: ");
    fflush(stdout);
    char username[64];
    if (read_line(username, sizeof(username)) == 0) {
        fprintf(stderr, "ERROR: Failed to read username\n");
        free_manager_data(&g_manager_data);
        return 1;
    }
    
    printf("Password: ");
    fflush(stdout);
    char password[64];
    if (read_line(password, sizeof(password)) == 0) {
        fprintf(stderr, "ERROR: Failed to read password\n");
        free_manager_data(&g_manager_data);
        return 1;
    }
    
    const User* user = login(&g_manager_data, username, password);
    if (user == NULL) {
        fprintf(stderr, "ERROR: Login failed - invalid username or password\n");
        free_manager_data(&g_manager_data);
        return 1;
    }
    
    printf("✓ Login successful: %s (%s)\n\n", user->name, user->username);
    
    // Step 2: Main loop
    printf("=== Starting Manager Main Loop ===\n");
    printf("Enter commands (EXIT to quit, SYNOPSIS for status):\n\n");
    
    char instruction[MAX_INSTRUCTION_LENGTH];
    
    while (1) {
        // Step 3: Send command to sensors
        if (g_sensors_port.is_open) {
            send_command(&g_sensors_port, "GTH");
            
            // Step 4: Wait for data from sensors
            char sensor_response[256];
            if (get_sensor_response(&g_sensors_port, sensor_response, sizeof(sensor_response)) > 0) {
                // Step 5 & 6: Extract data and update sensor data
                get_sensor_data(&g_sensors_port, &g_sensor_data);
            }
        }
        
        // Step 7: Wait for instructions from UI
        printf("> ");
        fflush(stdout);
        if (read_line(instruction, sizeof(instruction)) > 0) {
            // Check for exit command
            if (str_compare_custom(instruction, "EXIT") == 0 || 
                str_compare_custom(instruction, "QUIT") == 0) {
                printf("Exiting...\n");
                break;
            }
            
            // Step 8: Process instruction
            if (str_compare_custom(instruction, "SYNOPSIS") == 0) {
                // SYNOPSIS command - send to board
                send_synopsis_to_board(&g_manager_data);
                printf("SUCCESS: Synopsis sent to board\n");
            } else {
                // Parse and process other instructions (ASSIGN_TRACK, SET_MAINTENANCE, etc.)
                // The track_manager functions handle board and lightsigns internally
                process_instruction(instruction);
            }
            
            // Step 11: Record action
            record_action(&g_manager_data, user, instruction);
        }
    }
    
    // Cleanup
    printf("\n=== Cleaning up ===\n");
    close_serial_port(&g_sensors_port);
    close_board_comm(&g_board_port);
    close_serial_port(&g_lightsigns_port);
    cleanup_track_manager();
    free_manager_data(&g_manager_data);
    printf("✓ Cleanup complete\n");
    
    return 0;
}
