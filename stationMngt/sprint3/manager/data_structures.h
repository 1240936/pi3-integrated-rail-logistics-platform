/*
 * Data Structures - USAC11
 *
 * Defines the core data structures for the Manager component:
 * - User: Name, username, encrypted password, Caesar cipher key
 * - Track: ID, state (FREE/OCCUPIED/MAINTENANCE), train ID
 * - Train: ID
 * - SensorConfig: Buffer and window lengths for temperature/humidity sensors
 * - Log: ID, user identification, action, timestamp
 * - ManagerData: Container for all dynamic arrays
 */

#ifndef DATA_STRUCTURES_H
#define DATA_STRUCTURES_H

#include <time.h>

// Maximum string lengths
#define MAX_NAME_LENGTH 100
#define MAX_USERNAME_LENGTH 50
#define MAX_PASSWORD_LENGTH 50
#define MAX_ACTION_LENGTH 200

// Track states
typedef enum {
    TRACK_FREE = 0,           // Track is free
    TRACK_ASSIGNED = 1,       // Track is free but assigned to an arriving train (not yet occupied)
    TRACK_OCCUPIED = 2,       // Track is busy (occupied by train)
    TRACK_MAINTENANCE = 3     // Track is inoperative (maintenance)
} TrackState;

// User structure
typedef struct {
    char name[MAX_NAME_LENGTH];
    char username[MAX_USERNAME_LENGTH];
    char password[MAX_PASSWORD_LENGTH];  // Encrypted password
    int caesar_key;                      // Caesar Cipher key [1, 26]
} User;

// Train structure
typedef struct {
    int id;                              // Train identifier
} Train;

// Track structure
typedef struct {
    int id;                              // Track identifier (integer)
    TrackState state;                     // Track state
    int train_id;                        // Train using it (0 if free)
} Track;

// Sensor configuration structure
typedef struct {
    // Temperature sensor
    int temp_buffer_length;              // CircularBuffer length for temperature
    int temp_window_length;              // Moving median window length for temperature

    // Humidity sensor
    int hum_buffer_length;               // CircularBuffer length for humidity
    int hum_window_length;               // Moving median window length for humidity
} SensorConfig;

// Log structure
typedef struct {
    int id;                              // Log identifier (integer)
    char user_identification[MAX_USERNAME_LENGTH];  // User who performed the action
    char action[MAX_ACTION_LENGTH];      // Action taken
    time_t timestamp;                     // Timestamp of the action
} Log;

// Manager data structures (all dynamically allocated)
typedef struct {
    User* users;                         // Dynamic array of users
    int num_users;                       // Number of users
    int users_capacity;                  // Capacity of users array

    Track* tracks;                       // Dynamic array of tracks
    int num_tracks;                      // Number of tracks
    int tracks_capacity;                 // Capacity of tracks array

    Train* trains;                      // Dynamic array of trains
    int num_trains;                      // Number of trains
    int trains_capacity;                 // Capacity of trains array

    SensorConfig sensor_config;          // Sensor configuration

    Log* logs;                           // Dynamic array of logs
    int num_logs;                        // Number of logs
    int logs_capacity;                   // Capacity of logs array
    int next_log_id;                     // Next log ID to assign
} ManagerData;

/**
 * Initialize Manager data structures from a text file
 *
 * @param filename Path to the initialization text file
 * @param data Pointer to ManagerData structure to initialize
 * @return 1 if successful, 0 if failed
 */
int initialize_from_file(const char* filename, ManagerData* data);

/**
 * Free all dynamically allocated memory in ManagerData
 *
 * @param data Pointer to ManagerData structure to free
 */
void free_manager_data(ManagerData* data);

#endif // DATA_STRUCTURES_H