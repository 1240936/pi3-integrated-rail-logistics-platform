// Board Component Main Program - USAC16
// Receives data from Manager and displays track management information
// Uses funny interface with ASCII art, boxes, icons, and symbols

#include "board.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

// Custom string functions (avoiding string.h)
static int str_length(const char* str) {
    if (str == NULL) return 0;
    int len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

static int str_compare(const char* s1, const char* s2) {
    if (s1 == NULL || s2 == NULL) return -1;
    while (*s1 && (*s1 == *s2)) {
        s1++;
        s2++;
    }
    return *(const unsigned char*)s1 - *(const unsigned char*)s2;
}

static int str_ncompare(const char* s1, const char* s2, int n) {
    if (s1 == NULL || s2 == NULL || n <= 0) return -1;
    while (n-- > 0 && *s1 && (*s1 == *s2)) {
        s1++;
        s2++;
    }
    if (n < 0) return 0;
    return *(const unsigned char*)s1 - *(const unsigned char*)s2;
}

static int parse_int(const char* str, int* value) {
    if (str == NULL || value == NULL) return 0;
    *value = 0;
    int sign = 1;
    int i = 0;
    
    if (str[0] == '-') {
        sign = -1;
        i = 1;
    }
    
    while (str[i] >= '0' && str[i] <= '9') {
        *value = *value * 10 + (str[i] - '0');
        i++;
    }
    
    *value *= sign;
    return (str[i] == '\0') ? 1 : 0;
}

// Calculate checksum (same as Manager)
static char calculate_checksum(const char* str) {
    char cksum = 0;
    while (*str) {
        cksum ^= *str++;
    }
    return cksum;
}

// Parse command from Manager
// Format: TRACK_ASSIGN:track_id:train_id
//         TRACK_MAINT:track_id
//         TRACK_FREE:track_id
//         DEPART:track_id:train_id
//         EMERGENCY_STOP:train_id
//         SYNOPSIS:track1:train1:state1:track2:train2:state2:...
static void parse_and_display(const char* data) {
    if (data == NULL) return;
    
    // Check command type
    if (str_ncompare(data, "TRACK_ASSIGN:", 13) == 0) {
        int track_id, train_id;
        if (sscanf(data + 13, "%d:%d", &track_id, &train_id) == 2) {
            display_track_assigned(track_id, train_id);
        }
    }
    else if (str_ncompare(data, "TRACK_MAINT:", 12) == 0) {
        int track_id;
        if (sscanf(data + 12, "%d", &track_id) == 1) {
            display_track_maintenance(track_id);
        }
    }
    else if (str_ncompare(data, "TRACK_FREE:", 11) == 0) {
        int track_id;
        if (sscanf(data + 11, "%d", &track_id) == 1) {
            display_track_free(track_id);
        }
    }
    else if (str_ncompare(data, "DEPART:", 7) == 0) {
        int track_id, train_id;
        if (sscanf(data + 7, "%d:%d", &track_id, &train_id) == 2) {
            display_departure_order(track_id, train_id);
        }
    }
    else if (str_ncompare(data, "EMERGENCY_STOP:", 15) == 0) {
        int train_id;
        if (sscanf(data + 15, "%d", &train_id) == 1) {
            display_emergency_stop(train_id);
        }
    }
    else if (str_ncompare(data, "SYNOPSIS:", 9) == 0) {
        // Parse synopsis data
        int tracks[MAX_TRACKS];
        int trains[MAX_TRACKS];
        int states[MAX_TRACKS];
        int count = 0;
        const char* ptr = data + 9;
        
        while (*ptr && count < MAX_TRACKS) {
            if (sscanf(ptr, "%d:%d:%d", &tracks[count], &trains[count], &states[count]) == 3) {
                count++;
                // Move to next entry
                while (*ptr && *ptr != ':') ptr++;
                if (*ptr == ':') {
                    ptr++;
                    while (*ptr && *ptr != ':') ptr++;
                }
                if (*ptr == ':') ptr++;
            } else {
                break;
            }
        }
        
        if (count > 0) {
            display_synopsis(tracks, trains, states, count);
        }
    }
}

int main(void) {
    char buffer[256];
    int buffer_pos = 0;
    char data[256];
    int data_pos = 0;
    int in_packet = 0;
    char received_checksum = 0;
    
    // Initial display
    clear_screen();
    printf("\n");
    print_box_top(60);
    print_box_middle("╔═══════════════════════════════════════════════════╗", 60);
    print_box_middle("║      🚂 STATION BOARD COMPONENT 🚂                ║", 60);
    print_box_middle("║      Waiting for data from Manager...             ║", 60);
    print_box_middle("╚═══════════════════════════════════════════════════╝", 60);
    print_box_bottom(60);
    printf("\n");
    fflush(stdout);
    
    // Main loop: read data from stdin
    // Protocol: [STX][DATA...][CHKSUM][LF]
    while (1) {
        int ch = getchar();
        if (ch == EOF) break;
        
        if (!in_packet) {
            // Waiting for STX (0x02)
            if (ch == 0x02) {
                in_packet = 1;
                buffer_pos = 0;
                buffer[0] = '\0';
            }
        } else {
            // Inside packet
            if (ch == '\n' || ch == 0x0A) {
                // End of packet (LF)
                // The last byte in buffer is the checksum
                // All bytes before that are the data
                if (buffer_pos > 0) {
                    received_checksum = buffer[buffer_pos - 1];
                    // Copy data without checksum
                    for (int i = 0; i < buffer_pos - 1; i++) {
                        data[i] = buffer[i];
                    }
                    data[buffer_pos - 1] = '\0';
                    
                    // Verify checksum
                    char calculated_cksum = calculate_checksum(data);
                    if (calculated_cksum == received_checksum) {
                        // Valid packet, parse and display
                        parse_and_display(data);
                    }
                }
                in_packet = 0;
                buffer_pos = 0;
            } else {
                // Data byte (payload or checksum)
                if (buffer_pos < 255) {
                    buffer[buffer_pos++] = (char)ch;
                    buffer[buffer_pos] = '\0';
                }
            }
        }
    }
    
    return 0;
}

