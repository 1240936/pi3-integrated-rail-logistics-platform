/*
 * USAC11 - Manager Component Initialization
 * 
 * Purpose: Initialize data structures from a text file
 * 
 * This module reads a configuration file and initializes:
 * - Users (with encrypted passwords using Caesar Cipher)
 * - Tracks
 * - Trains
 * - Sensor configuration
 * - Logs
 */

#include <stdio.h>
#include <stdlib.h>
#include "data_structures.h"
#include "asm.h"
#include "helper_functions.h"

#define MAX_LINE_LENGTH 500
#define INITIAL_CAPACITY 10

/**
 * Helper function to calculate string length
 */
static int str_length(const char* str) {
    if (str == NULL) return 0;
    int len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

/**
 * Helper function to check if character is whitespace
 */
static int is_whitespace(char c) {
    return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
}

/**
 * Helper function to copy string
 */
static void str_copy(char* dest, const char* src, int max_len) {
    if (dest == NULL || src == NULL || max_len <= 0) return;
    int i = 0;
    while (i < max_len - 1 && src[i] != '\0') {
        dest[i] = src[i];
        i++;
    }
    dest[i] = '\0';
}

/**
 * Helper function to compare strings
 */
static int str_compare(const char* str1, const char* str2) {
    if (str1 == NULL && str2 == NULL) return 0;
    if (str1 == NULL) return -1;
    if (str2 == NULL) return 1;

    int i = 0;
    while (str1[i] != '\0' && str2[i] != '\0') {
        if (str1[i] != str2[i]) {
            return str1[i] - str2[i];
        }
        i++;
    }
    return str1[i] - str2[i];
}

/**
 * Helper function to compare n characters of strings
 */
static int str_ncompare(const char* str1, const char* str2, int n) {
    if (str1 == NULL && str2 == NULL) return 0;
    if (str1 == NULL) return -1;
    if (str2 == NULL) return 1;
    if (n <= 0) return 0;

    int i = 0;
    while (i < n && str1[i] != '\0' && str2[i] != '\0') {
        if (str1[i] != str2[i]) {
            return str1[i] - str2[i];
        }
        i++;
    }
    if (i < n) {
        return str1[i] - str2[i];
    }
    return 0;
}

/**
 * Helper function to move memory block
 */
static void mem_move(char* dest, const char* src, int n) {
    if (dest == NULL || src == NULL || n <= 0) return;

    // Handle overlapping memory
    if (dest < src || dest >= src + n) {
        // No overlap, copy forward
        for (int i = 0; i < n; i++) {
            dest[i] = src[i];
        }
    } else {
        // Overlap, copy backward
        for (int i = n - 1; i >= 0; i--) {
            dest[i] = src[i];
        }
    }
}

/**
 * Helper function to set memory to zero
 */
static void mem_set(void* ptr, int value, int n) {
    if (ptr == NULL || n <= 0) return;
    char* p = (char*)ptr;
    for (int i = 0; i < n; i++) {
        p[i] = (char)value;
    }
}


/**
 * Helper function to trim whitespace from string
 */
static void trim_string(char* str) {
    if (str == NULL) return;

    // Find start (skip leading whitespace)
    int start = 0;
    while (str[start] != '\0' && is_whitespace(str[start])) {
        start++;
    }

    // Find end (last non-whitespace character)
    int len = str_length(str);
    int end = len - 1;
    while (end >= start && is_whitespace(str[end])) {
        end--;
    }

    // Move trimmed string to beginning
    if (start > 0 || end < len - 1) {
        int new_len = end - start + 1;
        if (start > 0) {
            mem_move(str, str + start, new_len);
        }
        str[new_len] = '\0';
    }
}

/**
 * Helper function to check if character is digit
 */
static int is_digit(char c) {
    return (c >= '0' && c <= '9');
}

/**
 * Helper function to parse integer from string
 */
static int parse_int(const char* str, int* value) {
    if (str == NULL || value == NULL) return 0;

    int result = 0;
    int sign = 1;
    int i = 0;

    // Skip whitespace
    while (str[i] != '\0' && is_whitespace(str[i])) {
        i++;
    }

    // Check for sign
    if (str[i] == '-') {
        sign = -1;
        i++;
    } else if (str[i] == '+') {
        i++;
    }

    // Parse digits
    int has_digits = 0;
    while (str[i] != '\0' && is_digit(str[i])) {
        result = result * 10 + (str[i] - '0');
        i++;
        has_digits = 1;
    }

    // Check if there are invalid characters remaining
    while (str[i] != '\0') {
        if (!is_whitespace(str[i])) {
            return 0; // Invalid character found
        }
        i++;
    }

    if (!has_digits) {
        return 0; // No digits found
    }

    *value = result * sign;
    return 1;
}

/**
 * Expand users array if needed
 */
static int expand_users_array(ManagerData* data) {
    if (data->num_users >= data->users_capacity) {
        int new_capacity = data->users_capacity * 2;
        User* new_users = realloc(data->users, new_capacity * sizeof(User));
        if (new_users == NULL) {
            return 0;
        }
        data->users = new_users;
        data->users_capacity = new_capacity;
    }
    return 1;
}

/**
 * Parse user line: USER:name:username:password:key
 */
static int parse_user_line(const char* line, ManagerData* data) {
    char line_copy[MAX_LINE_LENGTH];
    str_copy(line_copy, line, MAX_LINE_LENGTH);

    char token[MAX_LINE_LENGTH];
    int pos = 0;

    // Parse "USER"
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0 || str_compare(token, "USER") != 0) {
        return 0;
    }

    if (!expand_users_array(data)) {
        return 0;
    }

    User* user = &data->users[data->num_users];

    // Parse name
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    str_copy(user->name, token, MAX_NAME_LENGTH);

    // Parse username
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    str_copy(user->username, token, MAX_USERNAME_LENGTH);

    // Parse password (plain text, will be encrypted)
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    char plain_password[MAX_PASSWORD_LENGTH];
    str_copy(plain_password, token, MAX_PASSWORD_LENGTH);

    // Convert password to uppercase (encrypt_data only works with A-Z)
    int pwd_len = str_length(plain_password);
    for (int i = 0; i < pwd_len; i++) {
        if (plain_password[i] >= 'a' && plain_password[i] <= 'z') {
            plain_password[i] = plain_password[i] - 'a' + 'A';
        }
        // Remove non-letter characters (encrypt_data only accepts A-Z)
        if (plain_password[i] < 'A' || plain_password[i] > 'Z') {
            // Replace with a placeholder or skip
            // For now, we'll replace with 'X'
            plain_password[i] = 'X';
        }
    }

    // Parse Caesar key
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &user->caesar_key)) {
        return 0;
    }
    if (user->caesar_key < 1 || user->caesar_key > 26) {
        return 0;
    }

    // Encrypt password using assembly function
    if (encrypt_data(plain_password, user->caesar_key, user->password) != 1) {
        return 0;
    }

    data->num_users++;
    return 1;
}

/**
 * Expand tracks array if needed
 */
static int expand_tracks_array(ManagerData* data) {
    if (data->num_tracks >= data->tracks_capacity) {
        int new_capacity = data->tracks_capacity * 2;
        Track* new_tracks = realloc(data->tracks, new_capacity * sizeof(Track));
        if (new_tracks == NULL) {
            return 0;
        }
        data->tracks = new_tracks;
        data->tracks_capacity = new_capacity;
    }
    return 1;
}

/**
 * Expand trains array if needed
 */
static int expand_trains_array(ManagerData* data) {
    if (data->num_trains >= data->trains_capacity) {
        int new_capacity = data->trains_capacity * 2;
        Train* new_trains = realloc(data->trains, new_capacity * sizeof(Train));
        if (new_trains == NULL) {
            return 0;
        }
        data->trains = new_trains;
        data->trains_capacity = new_capacity;
    }
    return 1;
}


/**
 * Parse track line: TRACK:id:state:train_id
 */
static int parse_track_line(const char* line, ManagerData* data) {
    char line_copy[MAX_LINE_LENGTH];
    str_copy(line_copy, line, MAX_LINE_LENGTH);

    char token[MAX_LINE_LENGTH];
    int pos = 0;

    // Parse "TRACK"
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0 || str_compare(token, "TRACK") != 0) {
        return 0;
    }

    if (!expand_tracks_array(data)) {
        return 0;
    }

    Track* track = &data->tracks[data->num_tracks];

    // Parse track ID
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &track->id)) {
        return 0;
    }

    // Parse state
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;

    // Try to parse as string first (FREE, OCCUPIED, MAINTENANCE), then as number
    int state_value;
    if (str_compare(token, "FREE") == 0) {
        state_value = 0;
    } else if (str_compare(token, "OCCUPIED") == 0) {
        state_value = 1;
    } else if (str_compare(token, "MAINTENANCE") == 0) {
        state_value = 2;
    } else if (parse_int(token, &state_value)) {
        // If it's a valid number, use it
        if (state_value < 0 || state_value > 2) {
            return 0;
        }
    } else {
        return 0;
    }

    track->state = (TrackState)state_value;

    // Parse train_id (0 if free)
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &track->train_id)) {
        return 0;
    }

    data->num_tracks++;
    return 1;
}

/**
 * Parse train line: TRAIN:id
 */
static int parse_train_line(const char* line, ManagerData* data) {
    char line_copy[MAX_LINE_LENGTH];
    str_copy(line_copy, line, MAX_LINE_LENGTH);

    char token[MAX_LINE_LENGTH];
    int pos = 0;

    // Parse "TRAIN"
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0 || str_compare(token, "TRAIN") != 0) {
        return 0;
    }

    if (!expand_trains_array(data)) {
        return 0;
    }

    Train* train = &data->trains[data->num_trains];

    // Parse train ID
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &train->id)) {
        return 0;
    }

    data->num_trains++;
    return 1;
}

/**
 * Parse sensor config line: SENSOR:temp_buffer:temp_window:hum_buffer:hum_window
 */
static int parse_sensor_line(const char* line, ManagerData* data) {
    char line_copy[MAX_LINE_LENGTH];
    str_copy(line_copy, line, MAX_LINE_LENGTH);

    char token[MAX_LINE_LENGTH];
    int pos = 0;

    // Parse "SENSOR"
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0 || str_compare(token, "SENSOR") != 0) {
        return 0;
    }

    // Parse temp_buffer_length
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &data->sensor_config.temp_buffer_length)) {
        return 0;
    }
    if (data->sensor_config.temp_buffer_length <= 0) {
        return 0;
    }

    // Parse temp_window_length
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &data->sensor_config.temp_window_length)) {
        return 0;
    }
    if (data->sensor_config.temp_window_length <= 0) {
        return 0;
    }

    // Parse hum_buffer_length
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &data->sensor_config.hum_buffer_length)) {
        return 0;
    }
    if (data->sensor_config.hum_buffer_length <= 0) {
        return 0;
    }

    // Parse hum_window_length
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &data->sensor_config.hum_window_length)) {
        return 0;
    }
    if (data->sensor_config.hum_window_length <= 0) {
        return 0;
    }

    return 1;
}

/**
 * Expand logs array if needed
 */
static int expand_logs_array(ManagerData* data) {
    if (data->num_logs >= data->logs_capacity) {
        int new_capacity = data->logs_capacity * 2;
        Log* new_logs = realloc(data->logs, new_capacity * sizeof(Log));
        if (new_logs == NULL) {
            return 0;
        }
        data->logs = new_logs;
        data->logs_capacity = new_capacity;
    }
    return 1;
}

/**
 * Parse log line: LOG:id:user_id:action:timestamp
 */
static int parse_log_line(const char* line, ManagerData* data) {
    char line_copy[MAX_LINE_LENGTH];
    str_copy(line_copy, line, MAX_LINE_LENGTH);

    char token[MAX_LINE_LENGTH];
    int pos = 0;

    // Parse "LOG"
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0 || str_compare(token, "LOG") != 0) {
        return 0;
    }

    if (!expand_logs_array(data)) {
        return 0;
    }

    Log* log = &data->logs[data->num_logs];

    // Parse log ID
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    if (!parse_int(token, &log->id)) {
        return 0;
    }
    if (log->id <= 0) {
        return 0;
    }

    // Parse user_id (can be index or identifier - storing as int for flexibility)
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    int user_id;
    if (!parse_int(token, &user_id)) {
        return 0;
    }
    // Store user_id as part of user_identification field (convert to string)
    int_to_string(user_id, log->user_identification, MAX_USERNAME_LENGTH);

    // Parse action
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    str_copy(log->action, token, MAX_ACTION_LENGTH);

    // Parse timestamp
    pos = get_token(line_copy, pos, ':', token, sizeof(token));
    if (pos < 0) return 0;
    long int timestamp;
    if (!parse_long_int(token, &timestamp)) {
        return 0;
    }
    log->timestamp = (time_t)timestamp;

    data->num_logs++;
    return 1;
}

/**
 * Initialize Manager data structures from a text file
 */
int initialize_from_file(const char* filename, ManagerData* data) {
    if (filename == NULL || data == NULL) {
        return 0;
    }

    // Initialize data structure
    mem_set(data, 0, sizeof(ManagerData));

    // Allocate initial arrays
    data->users_capacity = INITIAL_CAPACITY;
    data->users = malloc(data->users_capacity * sizeof(User));
    if (data->users == NULL) {
        return 0;
    }

    data->tracks_capacity = INITIAL_CAPACITY;
    data->tracks = malloc(data->tracks_capacity * sizeof(Track));
    if (data->tracks == NULL) {
        free(data->users);
        return 0;
    }

    data->trains_capacity = INITIAL_CAPACITY;
    data->trains = malloc(data->trains_capacity * sizeof(Train));
    if (data->trains == NULL) {
        free(data->users);
        free(data->tracks);
        return 0;
    }

    data->logs_capacity = INITIAL_CAPACITY;
    data->logs = malloc(data->logs_capacity * sizeof(Log));
    if (data->logs == NULL) {
        free(data->users);
        free(data->tracks);
        free(data->trains);
        return 0;
    }

    data->next_log_id = 1;

    // Open file
    FILE* file = fopen(filename, "r");
    if (file == NULL) {
        free(data->users);
        free(data->tracks);
        free(data->trains);
        free(data->logs);
        return 0;
    }

    char line[MAX_LINE_LENGTH];
    int line_number = 0;

    // Read file line by line
    while (fgets(line, sizeof(line), file) != NULL) {
        line_number++;
        trim_string(line);

        // Skip empty lines and comments
        if (line[0] == '\0' || line[0] == '#') {
            continue;
        }

        // Parse based on line type
        if (str_ncompare(line, "USER:", 5) == 0) {
            if (!parse_user_line(line, data)) {
                fclose(file);
                free_manager_data(data);
                return 0;
            }
        } else if (str_ncompare(line, "TRACK:", 6) == 0) {
            if (!parse_track_line(line, data)) {
                fclose(file);
                free_manager_data(data);
                return 0;
            }
        } else if (str_ncompare(line, "TRAIN:", 6) == 0) {
            if (!parse_train_line(line, data)) {
                fclose(file);
                free_manager_data(data);
                return 0;
            }
        } else if (str_ncompare(line, "SENSOR:", 7) == 0) {
            if (!parse_sensor_line(line, data)) {
                fclose(file);
                free_manager_data(data);
                return 0;
            }
        } else if (str_ncompare(line, "LOG:", 4) == 0) {
            if (!parse_log_line(line, data)) {
                fclose(file);
                free_manager_data(data);
                return 0;
            }
        } else {
            // Unknown line type, skip or return error
            // For robustness, we'll skip unknown lines
            continue;
        }
    }

    fclose(file);
    return 1;
}

/**
 * Free all dynamically allocated memory in ManagerData
 */
void free_manager_data(ManagerData* data) {
    if (data == NULL) {
        return;
    }

    if (data->users != NULL) {
        free(data->users);
        data->users = NULL;
    }

    if (data->tracks != NULL) {
        free(data->tracks);
        data->tracks = NULL;
    }

    if (data->trains != NULL) {
        free(data->trains);
        data->trains = NULL;
    }

    if (data->logs != NULL) {
        free(data->logs);
        data->logs = NULL;
    }

    mem_set(data, 0, sizeof(ManagerData));
}