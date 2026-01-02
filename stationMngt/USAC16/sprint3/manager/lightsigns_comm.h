/*
 * LightSigns Communication - USAC16
 *
 * Purpose: Send commands to LightSigns component to control track LED signals
 *
 * Communication protocol:
 * - Commands: RE,x (Red), YE,x (Yellow), GE,x (Green), RB,x (Red Blinking)
 * - Format: Uses serial communication with formatted commands
 * - Serial communication at 9600 baud
 *
 * Note: Before turning on an LED, the LightSigns component automatically
 * turns off all LEDs for that track.
 */

#ifndef LIGHTSIGNS_COMM_H
#define LIGHTSIGNS_COMM_H

#include "serial_comm.h"

// Light command types
typedef enum {
    LIGHT_CMD_RED = 0,        // RE - Red LED
    LIGHT_CMD_YELLOW = 1,     // YE - Yellow LED
    LIGHT_CMD_GREEN = 2,      // GE - Green LED
    LIGHT_CMD_RED_BLINK = 3   // RB - Red LED blinking
} LightCommandType;

/**
 * Send light command to LightSigns component
 *
 * Formats the command and sends it via serial communication.
 *
 * @param port Pointer to SerialPort structure (must be initialized)
 * @param command_type Type of light command (RE, YE, GE, RB)
 * @param track_id Track number (0-99)
 * @return 0 on success, -1 on error
 *
 * Example:
 *   send_light_command(port, LIGHT_CMD_GREEN, 1);  // Sends "GE,01"
 *   send_light_command(port, LIGHT_CMD_RED, 2);     // Sends "RE,02"
 */
int send_light_command(SerialPort* port, LightCommandType command_type, int track_id);

/**
 * Turn off all LEDs for a specific track
 *
 * Sends "OFF,x" command to LightSigns component
 *
 * @param port Pointer to SerialPort structure (must be initialized)
 * @param track_id Track number (0-99)
 * @return 0 on success, -1 on error
 */
int turn_off_track_leds(SerialPort* port, int track_id);

#endif // LIGHTSIGNS_COMM_H
