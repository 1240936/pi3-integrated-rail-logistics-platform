/*
 * Serial Communication - USAC16
 *
 * Purpose: Handle serial communication with Board and LightSigns components
 */

#ifndef SERIAL_COMM_H
#define SERIAL_COMM_H

#include <termios.h>
#include <unistd.h>
#include <fcntl.h>

#define BAUD_RATE 9600
#define SERIAL_BUFFER_SIZE 256

// Serial port structure
typedef struct {
    int fd;                 // File descriptor
    char port_path[256];    // Path to serial port (e.g., "/dev/ttyUSB0")
    int is_open;            // 1 if port is open, 0 otherwise
} SerialPort;

/**
 * Initialize serial port
 *
 * @param port Pointer to SerialPort structure to initialize
 * @param port_path Path to serial port (e.g., "/dev/ttyUSB0")
 * @return 0 on success, -1 on error
 */
int init_serial_port(SerialPort* port, const char* port_path);

/**
 * Close serial port
 *
 * @param port Pointer to SerialPort structure to close
 */
void close_serial_port(SerialPort* port);

/**
 * Write data to serial port
 *
 * @param port Pointer to initialized SerialPort structure
 * @param data Data buffer to write
 * @param length Number of bytes to write
 * @return 0 on success, -1 on error
 */
int write_serial_port(SerialPort* port, const char* data, int length);

#endif // SERIAL_COMM_H
