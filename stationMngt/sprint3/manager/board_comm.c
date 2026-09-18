/*
 * Board Communication Implementation - USAC16
 */

#include <stdio.h>
#include <string.h>
#include "board_comm.h"

/**
 * Initialize board communication on specified serial port
 */
int init_board_comm(SerialPort* port, const char* serial_port) {
    // Special handling for demo mode
    if (strcmp(serial_port, "DEMO_MODE") == 0) {
        // Set up port structure for demo mode
        strcpy(port->port_path, serial_port);
        port->is_open = 1;  // Mark as "open" for demo mode
        return 0;  // Success
    }

    // Normal serial port initialization
    return init_serial_port(port, serial_port);
}

/**
 * Send formatted data to board component
 */
int send_to_board(SerialPort* port, const char* formatted_data) {
    if (port == NULL || formatted_data == NULL) {
        return -1;
    }

    // Special demo mode: output to stdout for board component
    if (strcmp(port->port_path, "DEMO_MODE") == 0) {
        // Output text command for demo (easier to parse)
        printf("[BOARD] %s\n", formatted_data);
        fflush(stdout);
        return 0;
    }

    // Calculate checksum
    char checksum = calculate_checksum(formatted_data);

    // Create packet: STX + data + checksum + LF
    char packet[256];
    int packet_len = 0;

    packet[packet_len++] = STX;

    // Copy data
    strcpy(&packet[packet_len], formatted_data);
    packet_len += strlen(formatted_data);

    packet[packet_len++] = checksum;
    packet[packet_len++] = LF;

    // Send packet via serial port
    return write_serial_port(port, packet, packet_len);
}

/**
 * Close board communication port
 */
void close_board_comm(SerialPort* port) {
    if (port != NULL) {
        close_serial_port(port);
    }
}

/**
 * Calculate checksum for data integrity (XOR of all characters)
 */
char calculate_checksum(const char* str) {
    if (str == NULL) {
        return 0;
    }

    char checksum = 0;
    while (*str) {
        checksum ^= *str++;
    }
    return checksum;
}
