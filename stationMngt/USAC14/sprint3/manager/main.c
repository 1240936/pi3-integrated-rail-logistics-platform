/*
 * Manager Component - USAC14 Test/Demo
 * 
 * Purpose: Test the functionality of sending light commands to LightSigns component
 * 
 * Usage: ./manager.elf [serial_port]
 * Example: ./manager.elf /dev/ttyUSB0
 * 
 * Default serial port: /dev/ttyS0
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "lightsigns_comm.h"
#include "../../../USAC13/sprint3/manager/serial_comm.h"

#define DEFAULT_SERIAL_PORT "/dev/ttyS0"

int main(int argc, char* argv[]) {
    const char* serial_port = DEFAULT_SERIAL_PORT;
    
    // Parse command line arguments
    if (argc > 1) {
        serial_port = argv[1];
    }
    
    printf("=== Manager Component - USAC14 (LightSigns Communication) ===\n\n");
    
    // Initialize serial port
    printf("Initializing serial port: %s\n", serial_port);
    printf("Baud rate: %d\n", BAUD_RATE);
    SerialPort port;
    if (init_serial_port(&port, serial_port) != 0) {
        fprintf(stderr, "Failed to initialize serial port\n");
        return 1;
    }
    printf("Serial port opened successfully\n\n");
    
    // Flash all lights: Turn on all lights, then turn them off
    printf("=== Flash All Lights ===\n\n");
    
    // Turn on all lights for all tracks
    printf("Turning on all lights...\n");
    
    // Track 1: Green
    printf("Track 1: Green\n");
    if (send_light_command(&port, LIGHT_CMD_GREEN, 1) == 0) {
        printf("Command sent successfully: GE,01\n");
    } else {
        printf("Failed to send command\n");
    }
    
    // Track 2: Yellow
    printf("  Track 2: Yellow\n");
    if (send_light_command(&port, LIGHT_CMD_YELLOW, 2) == 0) {
        printf("Command sent successfully: YE,02\n");
    } else {
        printf("Failed to send command\n");
    }
    
    printf("\nLights are ON. Waiting 2 seconds...\n");
    usleep(2000000);  // 2 seconds delay
    
    // Turn off all lights
    printf("\nTurning off all lights...\n");
    
    // Turn off track 1
    printf("  Track 1: OFF\n");
    if (turn_off_track_leds(&port, 1) == 0) {
        printf("OFF command sent successfully: OFF,01\n");
    } else {
        printf("Failed to send OFF command\n");
    }
    
    // Turn off track 2
    printf("  Track 2: OFF\n");
    if (turn_off_track_leds(&port, 2) == 0) {
        printf("OFF command sent successfully: OFF,02\n");
    } else {
        printf("Failed to send OFF command\n");
    }
    
    printf("\n=== Flash Complete ===\n\n");
    
    // Close serial port
    close_serial_port(&port);
    printf("Serial port closed\n");
    
    printf("\n=== Test Complete ===\n");
    
    return 0;
}

