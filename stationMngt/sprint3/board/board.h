/*
 * Board Component - USAC15
 * 
 * Purpose: Display track management information with a "funny" interface
 * 
 * This file implements USAC15: "As a User, I want to send data to the Board component."
 * The Board displays information using icons, symbols, boxes, and lines.
 */

#ifndef BOARD_H
#define BOARD_H

#define MAX_DATA_LENGTH 256
#define MAX_TRACKS 10

// Track state display types
typedef enum {
    BOARD_TRACK_ASSIGNED,
    BOARD_TRACK_MAINTENANCE,
    BOARD_TRACK_FREE,
    BOARD_DEPARTURE_ORDER,
    BOARD_EMERGENCY_STOP,
    BOARD_SYNOPSIS
} BoardDisplayType;

// Function prototypes
void display_track_assigned(int track_id, int train_id);
void display_track_maintenance(int track_id);
void display_track_free(int track_id);
void display_departure_order(int track_id, int train_id);
void display_emergency_stop(int train_id);
void display_synopsis(int tracks[], int trains[], int states[], int num_tracks);
void clear_screen(void);
void print_box_top(int width);
void print_box_middle(const char* text, int width);
void print_box_bottom(int width);

#endif // BOARD_H

