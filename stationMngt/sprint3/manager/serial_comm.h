/*
 * Serial Communication - USAC13
 * 
 * Purpose: Handle serial communication with Sensors, Board, and LightSigns components
 * 
 * Communication protocol:
 * - Baud rate: 9600
 * - Command: "GTH" (Get Temperature and Humidity)
 * - Response format: "TEMP&unit:celsius&value:xx#HUM&unit:percentage&value:xx"
 * 
 * This module is used by:
 * - USAC13: Sensor communication
 * - USAC14: LightSigns communication  
 * - USAC15: Board communication
 */

#ifndef SERIAL_COMM_H
#define SERIAL_COMM_H

#include <termios.h>
#include <unistd.h>
#include <fcntl.h>

#define BAUD_RATE 9600
#define SERIAL_BUFFER_SIZE 256
#define GTH_CMD "GTH"
#define CMD_TIMEOUT_MS 5000  // 5 seconds timeout (Arduino needs 2s delay + processing time)

// Serial port structure
typedef struct {
    int fd;                 // File descriptor
    char port_path[256];    // Path to serial port (e.g., "/dev/ttyUSB0")
    int is_open;            // 1 if port is open, 0 otherwise
} SerialPort;

// Initialize serial port
// Returns 0 on success, -1 on error
int init_serial_port(SerialPort* port, const char* port_path);

// Close serial port
void close_serial_port(SerialPort* port);

// Send command to Sensors component
// Returns 0 on success, -1 on error
int send_command(SerialPort* port, const char* command);

// Receive response from Sensors component
// Returns number of bytes read, -1 on error, 0 on timeout
int receive_response(SerialPort* port, char* buffer, int buffer_size);

// Write raw data to serial port (for board and lightsigns communication)
// Returns 0 on success, -1 on error
int write_serial_port(SerialPort* port, const char* data, int length);

// Get sensor data (send GTH command and receive response)
// Returns 0 on success, -1 on error
int get_sensor_response(SerialPort* port, char* response, int response_size);

#endif // SERIAL_COMM_H

