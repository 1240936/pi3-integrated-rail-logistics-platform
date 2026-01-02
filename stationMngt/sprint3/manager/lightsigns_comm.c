/*
 * LightSigns Communication Implementation - USAC14
 * 
 * Uses USAC04 format_command assembly function
 */

#include <stdio.h>
#include <string.h>
#include "lightsigns_comm.h"
#include "asm.h"

/**
 * Send light command to LightSigns component
 */
int send_light_command(SerialPort* port, LightCommandType command_type, int track_id) {
    if (port == NULL || !port->is_open) {
        return -1;
    }

    // Special demo mode: output to stdout for lightsigns component
    if (strcmp(port->port_path, "DEMO_MODE") == 0) {
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

        // Format command: "XX,x" where XX is command and x is track_id (USAC10 compatible)
        // Use USAC04 format_command assembly function
        char formatted_cmd[20];
        if (format_command((char*)cmd_str, track_id, formatted_cmd) != 1) {
            return -1;
        }
        
        // Output command to stdout for demo
        printf("[LIGHTSIGNS] %s\n", formatted_cmd);
        fflush(stdout);
        return 0;
    }

    // Validate track_id range
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

    // Format command: "XX,x" where XX is command and x is track_id (USAC10 compatible)
    // Use USAC04 format_command assembly function
    char formatted_cmd[20];
    if (format_command((char*)cmd_str, track_id, formatted_cmd) != 1) {
        return -1;
    }
    
    // Calculate length for serial output
    int len = 0;
    while (formatted_cmd[len] != '\0' && len < 19) {
        len++;
    }

    // Send formatted command via serial
    return write_serial_port(port, formatted_cmd, len);
}

/**
 * Turn off all LEDs for a specific track
 */
int turn_off_track_leds(SerialPort* port, int track_id) {
    if (port == NULL || !port->is_open) {
        return -1;
    }

    // Validate track_id range
    if (track_id < 0 || track_id > 99) {
        return -1;
    }

    // Format OFF command: "OFF,x" where x is track_id (USAC10 compatible)
    // Note: format_command assembly function only supports RE, RB, YE, GE, and GTH
    // OFF is not supported by format_command, so we format it manually
    char off_cmd[20];
    int len = sprintf(off_cmd, "OFF,%d", track_id);
    
    if (len <= 0) {
        return -1;
    }

    // Send OFF command via serial
    return write_serial_port(port, off_cmd, len);
}
