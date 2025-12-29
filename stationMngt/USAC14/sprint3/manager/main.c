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
        printf(stderr, "Failed to initialize serial port\n");
        return 1;
    }
    printf(" Serial port opened successfully\n\n");
    
    // Test sending light commands
    printf("Testing light commands...\n\n");
    
    // Test 1: Send Green command to track 1
    printf("Test 1: Sending GE (Green) command to track 1\n");
    if (send_light_command(&port, LIGHT_CMD_GREEN, 1) == 0) {
        printf("Command sent successfully: GE,01\n");
    } else {
        printf("Failed to send command\n");
    }
    printf("\n");
    
    // Test 2: Send Yellow command to track 2
    printf("Test 2: Sending YE (Yellow) command to track 2\n");
    if (send_light_command(&port, LIGHT_CMD_YELLOW, 2) == 0) {
        printf("Command sent successfully: YE,02\n");
    } else {
        printf("Failed to send command\n");
    }
    printf("\n");
    
    // Test 3: Send Red command to track 1
    printf("Test 3: Sending RE (Red) command to track 1\n");
    if (send_light_command(&port, LIGHT_CMD_RED, 1) == 0) {
        printf("Command sent successfully: RE,01\n");
    } else {
        printf("Failed to send command\n");
    }
    printf("\n");
    
    // Test 4: Send Red Blinking command to track 2
    printf("Test 4: Sending RB (Red Blinking) command to track 2\n");
    if (send_light_command(&port, LIGHT_CMD_RED_BLINK, 2) == 0) {
        printf("Command sent successfully: RB,02\n");
    } else {
        printf("Failed to send command\n");
    }
    printf("\n");
    
    // Test 5: Turn off track 1 LEDs
    printf("Test 5: Turning off all LEDs for track 1\n");
    if (turn_off_track_leds(&port, 1) == 0) {
        printf("OFF command sent successfully: OFF,01\n");
    } else {
        printf("Failed to send OFF command\n");
    }
    printf("\n");
    
    // Test 6: Test invalid track ID (should fail)
    printf("Test 6: Testing invalid track ID (100 - should fail)\n");
    if (send_light_command(&port, LIGHT_CMD_GREEN, 100) == 0) {
        printf("Command should have failed but didn't\n");
    } else {
        printf("Command correctly rejected (invalid track ID)\n");
    }
    printf("\n");
    
    // Close serial port
    close_serial_port(&port);
    printf("Serial port closed\n");
    
    printf("\n=== Test Complete ===\n");
    
    return 0;
}

