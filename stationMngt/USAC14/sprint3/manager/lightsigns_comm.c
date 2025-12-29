/*
 * LightSigns Communication Implementation - USAC14
 * 
 * Implements communication with LightSigns component to control track LEDs.
 * Uses format_command() assembly function from USAC04 to format commands.
 * 
 * Note: No string.h or ctype.h allowed - all string operations are manual.
 */

#include <stdio.h>
#include <stdlib.h>
#include "lightsigns_comm.h"
#include "asm.h"  // For format_command assembly function
#include "serial_comm.h"  // For send_command function

// Manual string length function (no string.h)
static int my_strlen(const char* str) {
    int len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

// Manual string copy function (no string.h)
static void my_strcpy(char* dest, const char* src) {
    int i = 0;
    while (src[i] != '\0') {
        dest[i] = src[i];
        i++;
    }
    dest[i] = '\0';
}

/**
 * Send light command to LightSigns component
 * 
 * Formats the command using format_command() assembly function and sends it via serial.
 */
int send_light_command(SerialPort* port, LightCommandType command_type, int track_id) {
    if (port == NULL || !port->is_open) {
        return -1;
    }
    
    // Validate track_id range (0-99 as per format_command requirements)
    if (track_id < 0 || track_id > 99) {
        return -1;
    }
    
    // Map command type to command string
    const char* cmd_str;
    switch (command_type) {
        case LIGHT_CMD_RED:
            cmd_str = "RE";
            break;
        case LIGHT_CMD_YELLOW:
            cmd_str = "YE";
            break;
        case LIGHT_CMD_GREEN:
            cmd_str = "GE";
            break;
        case LIGHT_CMD_RED_BLINK:
            cmd_str = "RB";
            break;
        default:
            return -1;  // Invalid command type
    }
    
    // Format command using assembly function (USAC04)
    char formatted_cmd[20];
    int result = format_command((char*)cmd_str, track_id, formatted_cmd);
    
    if (result != 1) {
        // format_command failed (invalid input or out of range)
        return -1;
    }
    
    // Send formatted command via serial
    // Note: send_command from serial_comm.h adds newline automatically
    if (send_command(port, formatted_cmd) != 0) {
        return -1;
    }
    
    return 0;
}

/**
 * Turn off all LEDs for a specific track
 * 
 * Sends "OFF,x" command to LightSigns component.
 * Note: The format_command function doesn't support "OFF" command,
 * so we format it manually.
 */
int turn_off_track_leds(SerialPort* port, int track_id) {
    if (port == NULL || !port->is_open) {
        return -1;
    }
    
    // Validate track_id range
    if (track_id < 0 || track_id > 99) {
        return -1;
    }
    
    // Format OFF command manually (format_command doesn't support OFF)
    // Format: "OFF,XX" where XX is track_id with zero padding
    char off_cmd[20];
    
    // Manual string formatting (no string.h allowed)
    off_cmd[0] = 'O';
    off_cmd[1] = 'F';
    off_cmd[2] = 'F';
    off_cmd[3] = ',';
    
    // Format track_id with zero padding (2 digits)
    int tens = track_id / 10;
    int ones = track_id % 10;
    
    off_cmd[4] = '0' + tens;
    off_cmd[5] = '0' + ones;
    off_cmd[6] = '\0';
    
    // Send OFF command via serial
    if (send_command(port, off_cmd) != 0) {
        return -1;
    }
    
    return 0;
}

