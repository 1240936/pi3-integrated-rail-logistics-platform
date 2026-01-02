/*
 * Manager Component - USAC16 Test/Demo
 *
 * Purpose: Test track management functionality with Board and LightSigns integration
 *
 * Usage: ./manager_usac16.elf [board_port] [lightsigns_port]
 * Example: ./manager_usac16.elf /dev/ttyUSB0 /dev/ttyUSB1
 *
 * Default ports: /dev/ttyS0 (board), /dev/ttyS1 (lightsigns)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "track_manager.h"
#include "../../../USAC12/logging.h"
#include "../../../USAC11/sprint3/manager/data_structures.h"

#define DEFAULT_BOARD_PORT "/dev/ttyS0"
#define DEFAULT_LIGHTSIGNS_PORT "/dev/ttyS0"  // Use same port as board
#define CONFIG_FILE "config_example.txt"

int main(int argc, char* argv[]) {
    const char* board_port = DEFAULT_BOARD_PORT;
    const char* lightsigns_port = DEFAULT_LIGHTSIGNS_PORT;

    // Parse command line arguments
    if (argc > 1) {
        // Check for special modes
        if (strcmp(argv[1], "--board-only") == 0) {
            lightsigns_port = NULL;  // Disable lightsigns
        } else if (strcmp(argv[1], "--board-demo") == 0) {
            // Special mode for integrated demo: output board commands to stdout
            board_port = "DEMO_MODE";
            lightsigns_port = NULL;
        } else if (strcmp(argv[1], "--full-demo") == 0) {
            // Special mode for full integrated demo: output both board and lightsigns commands
            board_port = "DEMO_MODE";
            lightsigns_port = "DEMO_MODE";
        } else if (strcmp(argv[1], "--lightsigns-only") == 0) {
            board_port = NULL;  // Disable board
        } else if (strcmp(argv[1], "--same-port") == 0) {
            // Use same port for both components
            board_port = DEFAULT_BOARD_PORT;
            lightsigns_port = DEFAULT_BOARD_PORT;
        } else {
            board_port = argv[1];
        }
    }
    if (argc > 2) {
        lightsigns_port = argv[2];
    }

    printf("=== Manager Component - USAC16 (Track Management) ===\n\n");
    printf("Board port: %s\n", board_port ? board_port : "DISABLED");
    printf("LightSigns port: %s\n", lightsigns_port ? lightsigns_port : "DISABLED");
    printf("Config file: %s\n\n", CONFIG_FILE);

    // Initialize manager data from config file
    ManagerData manager_data = {0};
    if (initialize_from_file(CONFIG_FILE, &manager_data) != 1) {
        printf("ERROR: Failed to initialize data structures from config file\n");
        printf("       Please ensure %s exists and is properly formatted\n", CONFIG_FILE);
        return 1;
    }

    printf("✓ Manager data initialized successfully\n\n");

    // Initialize track manager with communication ports
    if (init_track_manager(&manager_data, board_port, lightsigns_port) != 0) {
        printf("ERROR: Failed to initialize track manager\n");
        free_manager_data(&manager_data);
        return 1;
    }

    printf("✓ Track manager initialized successfully\n\n");

    // Display initial track status
    printf("=== Initial Track Status ===\n");
    for (int i = 0; i < manager_data.num_tracks; i++) {
        const char* state_str;
        switch (manager_data.tracks[i].state) {
            case TRACK_FREE: state_str = "FREE"; break;
            case TRACK_OCCUPIED: state_str = "OCCUPIED"; break;
            case TRACK_MAINTENANCE: state_str = "MAINTENANCE"; break;
            default: state_str = "UNKNOWN"; break;
        }
        printf("Track %d: %s (Train ID: %d)\n",
               manager_data.tracks[i].id, state_str, manager_data.tracks[i].train_id);
    }
    printf("\n");

    // Test 1: Assign tracks to arriving trains
    printf("=== Test 1: Track Assignment ===\n");
    int test_trains[] = {101, 102, 104}; // Test train IDs (104 will cause emergency)
    int assigned_track;

    for (size_t i = 0; i < sizeof(test_trains)/sizeof(test_trains[0]); i++) {
        printf("Assigning track to train %d...\n", test_trains[i]);
        TrackOperationResult result = assign_track_to_train(&manager_data, test_trains[i], &assigned_track);

        if (result == TRACK_OP_SUCCESS) {
            printf("✓ Success: Train %d assigned to track %d\n", test_trains[i], assigned_track);
        } else if (result == TRACK_OP_NO_AVAILABLE_TRACK) {
            printf("⚠ Emergency stop: No available track for train %d\n", test_trains[i]);
        } else {
            printf("✗ Failed: %s\n", get_track_operation_result_string(result));
        }
        printf("\n");
    }

    // Test 2: Set tracks to maintenance
    printf("=== Test 2: Set Tracks to Maintenance ===\n");
    printf("Setting track 1 to maintenance...\n");
    TrackOperationResult result = set_track_maintenance(&manager_data, 1);
    if (result == TRACK_OP_SUCCESS) {
        printf("✓ Success: Track 1 set to maintenance\n");
    } else {
        printf("✗ Failed: %s\n", get_track_operation_result_string(result));
    }

    printf("\nSetting track 2 to maintenance (should fail - occupied)...\n");
    result = set_track_maintenance(&manager_data, 2);
    if (result == TRACK_OP_SUCCESS) {
        printf("✓ Success: Track 2 set to maintenance\n");
    } else {
        printf("✗ Failed (expected): %s\n", get_track_operation_result_string(result));
    }
    printf("\n");

    // Test 3: Free tracks
    printf("=== Test 3: Free Tracks ===\n");
    printf("Freeing track 1...\n");
    result = free_track(&manager_data, 1);
    if (result == TRACK_OP_SUCCESS) {
        printf("✓ Success: Track 1 freed\n");
    } else {
        printf("✗ Failed: %s\n", get_track_operation_result_string(result));
    }
    printf("\n");

    // Test 4: Issue departure orders
    printf("=== Test 4: Departure Orders ===\n");
    printf("Issuing departure order for track 2...\n");
    result = issue_departure_order(&manager_data, 2);
    if (result == TRACK_OP_SUCCESS) {
        printf("✓ Success: Departure order issued for track 2\n");
    } else {
        printf("✗ Failed: %s\n", get_track_operation_result_string(result));
    }

    printf("\nIssuing departure order for track 1 (should fail - no train to depart)...\n");
    result = issue_departure_order(&manager_data, 1);
    if (result == TRACK_OP_SUCCESS) {
        printf("✓ Success: Departure order issued for track 1\n");
    } else {
        printf("✗ Failed (expected): %s\n", get_track_operation_result_string(result));
    }
    printf("\n");

    // Test 5: Try assigning another train (track 1 should be free)
    printf("=== Test 5: Additional Train Assignment ===\n");
    printf("Attempting to assign track to train 105...\n");
    result = assign_track_to_train(&manager_data, 105, &assigned_track);
    if (result == TRACK_OP_SUCCESS) {
        printf("✓ Success: Train 105 assigned to track %d\n", assigned_track);
    } else {
        printf("✗ Failed: %s\n", get_track_operation_result_string(result));
    }
    printf("\n");

    // Send synopsis to board
    send_synopsis_to_board(&manager_data);

    // Display final track status
    printf("=== Final Track Status ===\n");
    for (int i = 0; i < manager_data.num_tracks; i++) {
        const char* state_str;
        switch (manager_data.tracks[i].state) {
            case TRACK_FREE: state_str = "FREE"; break;
            case TRACK_OCCUPIED: state_str = "OCCUPIED"; break;
            case TRACK_MAINTENANCE: state_str = "MAINTENANCE"; break;
            default: state_str = "UNKNOWN"; break;
        }
        printf("Track %d: %s (Train ID: %d)\n",
               manager_data.tracks[i].id, state_str, manager_data.tracks[i].train_id);
    }
    printf("\n");

    // Display logged actions
    printf("=== Logged Actions ===\n");
    for (int i = 0; i < manager_data.num_logs; i++) {
        printf("[%d] %s: %s\n", manager_data.logs[i].id,
               manager_data.logs[i].user_identification, manager_data.logs[i].action);
    }
    printf("\n");

    // Cleanup
    cleanup_track_manager();
    free_manager_data(&manager_data);

    printf("✓ Cleanup completed\n");
    printf("\n=== USAC16 Track Management Test Complete ===\n");

    return 0;
}
