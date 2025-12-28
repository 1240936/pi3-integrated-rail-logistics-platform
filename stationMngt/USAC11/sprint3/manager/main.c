/*
 * USAC11 - Manager Component Initialization Test
 * 
 * Purpose: Test the initialization of data structures from a text file
 */

#include <stdio.h>
#include <stdlib.h>
#include "data_structures.h"
#include "asm.h"

int main(int argc, char* argv[]) {
    if (argc < 2) {
        printf("Usage: %s <config_file>\n", argv[0]);
        printf("Example: %s config.txt\n", argv[0]);
        return 1;
    }
    
    ManagerData data;
    
    printf("=== USAC11 - Manager Component Initialization ===\n\n");
    printf("Loading configuration from: %s\n\n", argv[1]);
    
    // Check if file exists
    FILE* test_file = fopen(argv[1], "r");
    if (test_file == NULL) {
        printf("ERROR: Cannot open file: %s\n", argv[1]);
        printf("       File does not exist or cannot be read.\n");
        return 1;
    }
    fclose(test_file);

    // Initialize from file
    if (initialize_from_file(argv[1], &data) != 1) {
        printf("ERROR: Failed to initialize data structures from file: %s\n", argv[1]);
        printf("       Please check the file format.\n");
        printf("       Expected format:\n");
        printf("         USER:name:username:password:key\n");
        printf("         TRACK🆔state:train_id\n");
        printf("         TRAIN:id\n");
        printf("         SENSOR:temp_buffer:temp_window:hum_buffer:hum_window\n");
        printf("         LOG🆔user_id:action:timestamp\n");
        return 1;
    }

    printf("✓ Initialization successful!\n\n");

    // Display loaded data
    printf("=== Loaded Data ===\n\n");

    // Display users
    printf("Users (%d):\n", data.num_users);
    for (int i = 0; i < data.num_users; i++) {
        printf("  [%d] Name: %s, Username: %s, Key: %d\n",
               i + 1, data.users[i].name, data.users[i].username, data.users[i].caesar_key);
        printf("       Password (encrypted): %s\n", data.users[i].password);
    }
    printf("\n");

    // Display tracks
    printf("Tracks (%d):\n", data.num_tracks);
    for (int i = 0; i < data.num_tracks; i++) {
        const char* state_str;
        switch (data.tracks[i].state) {
            case TRACK_FREE: state_str = "FREE"; break;
            case TRACK_OCCUPIED: state_str = "OCCUPIED"; break;
            case TRACK_MAINTENANCE: state_str = "MAINTENANCE"; break;
            default: state_str = "UNKNOWN"; break;
        }
        printf("  [%d] ID: %d, State: %s, Train ID: %d\n",
               i + 1, data.tracks[i].id, state_str, data.tracks[i].train_id);
    }
    printf("\n");

    // Display trains
    printf("Trains (%d):\n", data.num_trains);
    for (int i = 0; i < data.num_trains; i++) {
        printf("  [%d] ID: %d\n", i + 1, data.trains[i].id);
    }
    printf("\n");

    // Display sensor configuration
    printf("Sensor Configuration:\n");
    printf("  Temperature - Buffer Length: %d, Window Length: %d\n",
           data.sensor_config.temp_buffer_length, data.sensor_config.temp_window_length);
    printf("  Humidity - Buffer Length: %d, Window Length: %d\n",
           data.sensor_config.hum_buffer_length, data.sensor_config.hum_window_length);
    printf("\n");

    // Display logs (should be empty initially)
    printf("Logs (%d):\n", data.num_logs);
    if (data.num_logs == 0) {
        printf("  (No logs yet)\n");
    } else {
        for (int i = 0; i < data.num_logs; i++) {
            printf("  [%d] ID: %d, User: %s, Action: %s\n",
                   i + 1, data.logs[i].id, data.logs[i].user_identification, data.logs[i].action);
        }
    }
    printf("\n");

    // Free allocated memory
    free_manager_data(&data);

    printf("✓ Memory freed successfully.\n");

    return 0;
}