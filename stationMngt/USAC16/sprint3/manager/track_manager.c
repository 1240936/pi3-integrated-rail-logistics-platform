/*
 * Track Manager Implementation - USAC16
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "track_manager.h"
#include "board_comm.h"
#include "lightsigns_comm.h"
#include "../../../USAC12/logging.h"

// Static variables for communication ports
static ManagerData* g_manager_data = NULL;
static SerialPort g_board_port;
static SerialPort g_lightsigns_port;
static int g_initialized = 0;

/**
 * Initialize track manager with communication components
 */
int init_track_manager(ManagerData* manager_data, const char* board_port, const char* lightsigns_port) {
    if (g_initialized) {
        printf("Track manager already initialized\n");
        return -1;
    }

    if (manager_data == NULL) {
        printf("Invalid manager data\n");
        return -1;
    }

    g_manager_data = manager_data;

    // Try to initialize board communication (if port specified)
    if (board_port != NULL) {
        if (init_board_comm(&g_board_port, board_port) != 0) {
            printf("WARNING: Failed to initialize board communication - running in SIMULATION mode\n");
            // Continue without board communication
        }
    } else {
        printf("Board communication disabled\n");
    }

    // Try to initialize lightsigns communication (if port specified)
    if (lightsigns_port != NULL) {
        if (init_serial_port(&g_lightsigns_port, lightsigns_port) != 0) {
            printf("WARNING: Failed to initialize lightsigns communication - running in SIMULATION mode\n");
            // Continue without lightsigns communication
        }
    } else {
        printf("LightSigns communication disabled\n");
    }

    g_initialized = 1;
    return 0;
}

/**
 * Find an available track for train assignment
 */
static int find_available_track(ManagerData* manager_data) {
    for (int i = 0; i < manager_data->num_tracks; i++) {
        if (manager_data->tracks[i].state == TRACK_FREE) {
            return manager_data->tracks[i].id;
        }
    }
    return -1; // No available track
}

/**
 * Find track by ID
 */
static int find_track_index(ManagerData* manager_data, int track_id) {
    for (int i = 0; i < manager_data->num_tracks; i++) {
        if (manager_data->tracks[i].id == track_id) {
            return i;
        }
    }
    return -1; // Track not found
}

/**
 * Send command to board component
 */
static TrackOperationResult send_command_to_board(const char* command) {
    // Check if board communication is available
    if (g_board_port.is_open) {
        if (send_to_board(&g_board_port, command) != 0) {
            return TRACK_OP_BOARD_COMM_ERROR;
        }
    } else {
        printf("[SIMULATION] Board command: %s\n", command);
    }

    return TRACK_OP_SUCCESS;
}

/**
 * Update board with track status
 */
TrackOperationResult update_board_track_status(int track_id, TrackState state, int train_id) {
    char command[128];

    switch (state) {
        case TRACK_FREE:
            snprintf(command, sizeof(command), "TRACK_FREE:%d", track_id);
            break;
        case TRACK_OCCUPIED:
            snprintf(command, sizeof(command), "TRACK_ASSIGN:%d:%d", track_id, train_id);
            break;
        case TRACK_MAINTENANCE:
            snprintf(command, sizeof(command), "TRACK_MAINT:%d", track_id);
            break;
        default:
            return TRACK_OP_BOARD_COMM_ERROR;
    }

    // Send command to board
    return send_command_to_board(command);
}

/**
 * Send synopsis of all tracks to board
 */
void send_synopsis_to_board(ManagerData* data) {
    char synopsis[512] = "SYNOPSIS:";

    for (int i = 0; i < data->num_tracks; i++) {
        char track_info[64];
        int state_num = (data->tracks[i].state == TRACK_FREE) ? 0 :
                       (data->tracks[i].state == TRACK_OCCUPIED) ? 1 : 2;

        snprintf(track_info, sizeof(track_info), "%d:%d:%d:",
                 data->tracks[i].id, data->tracks[i].train_id, state_num);

        if (strlen(synopsis) + strlen(track_info) < sizeof(synopsis) - 1) {
            strcat(synopsis, track_info);
        }
    }

    // Remove last colon if present
    size_t len = strlen(synopsis);
    if (len > 0 && synopsis[len - 1] == ':') {
        synopsis[len - 1] = '\0';
    }

    send_command_to_board(synopsis);
}

/**
 * Update lightsigns with track status
 */
TrackOperationResult update_lightsigns_track_status(int track_id, TrackState state) {
    LightCommandType command;
    const char* command_name;

    switch (state) {
        case TRACK_FREE:
            // Green light for free track
            command = LIGHT_CMD_GREEN;
            command_name = "GREEN";
            break;
        case TRACK_OCCUPIED:
            // Red light for occupied track
            command = LIGHT_CMD_RED;
            command_name = "RED";
            break;
        case TRACK_MAINTENANCE:
            // Yellow light for maintenance
            command = LIGHT_CMD_YELLOW;
            command_name = "YELLOW";
            break;
        default:
            return TRACK_OP_LIGHTSIGNS_COMM_ERROR;
    }

    // Check if lightsigns communication is available
    if (g_lightsigns_port.is_open) {
        if (send_light_command(&g_lightsigns_port, command, track_id) != 0) {
            return TRACK_OP_LIGHTSIGNS_COMM_ERROR;
        }
    } else {
        printf("[SIMULATION] Lightsigns: Track %d -> %s\n", track_id, command_name);
    }

    return TRACK_OP_SUCCESS;
}

/**
 * Assign a track to an arriving train
 */
TrackOperationResult assign_track_to_train(ManagerData* manager_data, int train_id, int* assigned_track_id) {
    if (!g_initialized || assigned_track_id == NULL) {
        return TRACK_OP_INVALID_TRAIN;
    }

    // Find available track
    int track_id = find_available_track(manager_data);
    if (track_id == -1) {
        // No available track - emergency stop
        *assigned_track_id = 0;

        // Send emergency stop to board
        char emergency_cmd[64];
        snprintf(emergency_cmd, sizeof(emergency_cmd), "EMERGENCY_STOP:%d", train_id);
        send_command_to_board(emergency_cmd);

        // Log emergency stop
        User system_user = {"SYSTEM", "system", "", 0};
        char action[200];
        snprintf(action, sizeof(action), "Emergency stop: no available track for train %d", train_id);
        record_action(manager_data, &system_user, action);

        return TRACK_OP_NO_AVAILABLE_TRACK;
    }

    // Find track index
    int track_index = find_track_index(manager_data, track_id);
    if (track_index == -1) {
        return TRACK_OP_TRACK_NOT_FOUND;
    }

    // Assign track to train
    manager_data->tracks[track_index].state = TRACK_OCCUPIED;
    manager_data->tracks[track_index].train_id = train_id;
    *assigned_track_id = track_id;

    // Update board and lightsigns
    TrackOperationResult board_result = update_board_track_status(track_id, TRACK_OCCUPIED, train_id);
    TrackOperationResult light_result = update_lightsigns_track_status(track_id, TRACK_OCCUPIED);

    if (board_result != TRACK_OP_SUCCESS) {
        return board_result;
    }
    if (light_result != TRACK_OP_SUCCESS) {
        return light_result;
    }

    // Log successful assignment
    User system_user = {"SYSTEM", "system", "", 0};
    char action[200];
    snprintf(action, sizeof(action), "Assigned track %d to train %d", track_id, train_id);
    record_action(manager_data, &system_user, action);

    return TRACK_OP_SUCCESS;
}

/**
 * Set a track as nonoperational (maintenance mode)
 */
TrackOperationResult set_track_maintenance(ManagerData* manager_data, int track_id) {
    if (!g_initialized) {
        return TRACK_OP_TRACK_NOT_FOUND;
    }

    int track_index = find_track_index(manager_data, track_id);
    if (track_index == -1) {
        return TRACK_OP_TRACK_NOT_FOUND;
    }

    // Can only set maintenance if track is free
    if (manager_data->tracks[track_index].state != TRACK_FREE) {
        return TRACK_OP_TRACK_OCCUPIED;
    }

    // Set track to maintenance
    manager_data->tracks[track_index].state = TRACK_MAINTENANCE;
    manager_data->tracks[track_index].train_id = 0;

    // Update board and lightsigns
    TrackOperationResult board_result = update_board_track_status(track_id, TRACK_MAINTENANCE, 0);
    TrackOperationResult light_result = update_lightsigns_track_status(track_id, TRACK_MAINTENANCE);

    if (board_result != TRACK_OP_SUCCESS) {
        return board_result;
    }
    if (light_result != TRACK_OP_SUCCESS) {
        return light_result;
    }

    // Log maintenance action
    User system_user = {"SYSTEM", "system", "", 0};
    char action[200];
    snprintf(action, sizeof(action), "Set track %d to maintenance", track_id);
    record_action(manager_data, &system_user, action);

    return TRACK_OP_SUCCESS;
}

/**
 * Set a track as free
 */
TrackOperationResult free_track(ManagerData* manager_data, int track_id) {
    if (!g_initialized) {
        return TRACK_OP_TRACK_NOT_FOUND;
    }

    int track_index = find_track_index(manager_data, track_id);
    if (track_index == -1) {
        return TRACK_OP_TRACK_NOT_FOUND;
    }

    // Can only free if track is occupied
    if (manager_data->tracks[track_index].state != TRACK_OCCUPIED) {
        return TRACK_OP_TRACK_UNDER_MAINTENANCE;
    }

    int old_train_id = manager_data->tracks[track_index].train_id;

    // Free the track
    manager_data->tracks[track_index].state = TRACK_FREE;
    manager_data->tracks[track_index].train_id = 0;

    // Update board and lightsigns
    TrackOperationResult board_result = update_board_track_status(track_id, TRACK_FREE, 0);
    TrackOperationResult light_result = update_lightsigns_track_status(track_id, TRACK_FREE);

    if (board_result != TRACK_OP_SUCCESS) {
        return board_result;
    }
    if (light_result != TRACK_OP_SUCCESS) {
        return light_result;
    }

    // Log free action
    User system_user = {"SYSTEM", "system", "", 0};
    char action[200];
    snprintf(action, sizeof(action), "Freed track %d (was occupied by train %d)", track_id, old_train_id);
    record_action(manager_data, &system_user, action);

    return TRACK_OP_SUCCESS;
}

/**
 * Issue departure order to a stopped train
 */
TrackOperationResult issue_departure_order(ManagerData* manager_data, int track_id) {
    if (!g_initialized) {
        return TRACK_OP_TRACK_NOT_FOUND;
    }

    int track_index = find_track_index(manager_data, track_id);
    if (track_index == -1) {
        return TRACK_OP_TRACK_NOT_FOUND;
    }

    // Can only depart if track is occupied
    if (manager_data->tracks[track_index].state != TRACK_OCCUPIED) {
        return TRACK_OP_INVALID_TRAIN;  // No train to depart
    }

    int train_id = manager_data->tracks[track_index].train_id;

    // Send departure order to board
    char depart_cmd[64];
    snprintf(depart_cmd, sizeof(depart_cmd), "DEPART:%d:%d", track_id, train_id);
    send_command_to_board(depart_cmd);

    // Update lightsigns to show departure (could be blinking red or specific signal)
    if (g_lightsigns_port.is_open) {
        if (send_light_command(&g_lightsigns_port, LIGHT_CMD_RED_BLINK, track_id) != 0) {
            return TRACK_OP_LIGHTSIGNS_COMM_ERROR;
        }
    } else {
        printf("[SIMULATION] Lightsigns: Track %d -> RED_BLINK (departure)\n", track_id);
    }

    // Free the track after departure
    manager_data->tracks[track_index].state = TRACK_FREE;
    manager_data->tracks[track_index].train_id = 0;

    // Update board and lightsigns to show track is now free
    update_board_track_status(track_id, TRACK_FREE, 0);
    update_lightsigns_track_status(track_id, TRACK_FREE);

    // Log departure order and track liberation
    User system_user = {"SYSTEM", "system", "", 0};
    char action[200];
    snprintf(action, sizeof(action), "Issued departure order for train %d on track %d", train_id, track_id);
    record_action(manager_data, &system_user, action);

    snprintf(action, sizeof(action), "Track %d freed after train %d departure", track_id, train_id);
    record_action(manager_data, &system_user, action);

    return TRACK_OP_SUCCESS;
}

/**
 * Get string representation of track operation result
 */
const char* get_track_operation_result_string(TrackOperationResult result) {
    switch (result) {
        case TRACK_OP_SUCCESS: return "Success";
        case TRACK_OP_NO_AVAILABLE_TRACK: return "No available track";
        case TRACK_OP_TRACK_NOT_FOUND: return "Track not found";
        case TRACK_OP_TRACK_OCCUPIED: return "Track occupied";
        case TRACK_OP_TRACK_UNDER_MAINTENANCE: return "Track under maintenance";
        case TRACK_OP_INVALID_TRAIN: return "No train to depart";
        case TRACK_OP_BOARD_COMM_ERROR: return "Board communication error";
        case TRACK_OP_LIGHTSIGNS_COMM_ERROR: return "Lightsigns communication error";
        default: return "Unknown error";
    }
}

/**
 * Cleanup track manager resources
 */
void cleanup_track_manager() {
    if (g_initialized) {
        if (g_board_port.is_open) {
            close_board_comm(&g_board_port);
        }
        if (g_lightsigns_port.is_open) {
            close_serial_port(&g_lightsigns_port);
        }
        g_initialized = 0;
        g_manager_data = NULL;
    }
}
