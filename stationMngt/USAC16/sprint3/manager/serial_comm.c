/*
 * Serial Communication Implementation - USAC16
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <termios.h>
#include <unistd.h>
#include <fcntl.h>
#include "serial_comm.h"

/**
 * Initialize serial port
 */
int init_serial_port(SerialPort* port, const char* port_path) {
    if (port == NULL || port_path == NULL) {
        return -1;
    }

    // Special demo mode - no real serial port needed
    if (strcmp(port_path, "DEMO_MODE") == 0) {
        port->fd = -1;  // Mark as demo mode
        port->is_open = 1;
        strncpy(port->port_path, port_path, sizeof(port->port_path) - 1);
        return 0;
    }

    // Open serial port
    port->fd = open(port_path, O_RDWR | O_NOCTTY | O_NDELAY);
    if (port->fd < 0) {
        // Don't print error here as it will be handled by caller
        return -1;
    }

    // Configure serial port
    struct termios tty;
    if (tcgetattr(port->fd, &tty) != 0) {
        close(port->fd);
        return -1;
    }

    // Try to set basic configuration to verify it's a real serial port
    tty.c_cflag |= CLOCAL;  // Ignore modem control lines
    if (tcsetattr(port->fd, TCSANOW, &tty) != 0) {
        // Not a critical error - continue anyway
    }

    // Set baud rate
    cfsetospeed(&tty, B9600);
    cfsetispeed(&tty, B9600);

    // 8N1 configuration
    tty.c_cflag &= ~PARENB;         // No parity
    tty.c_cflag &= ~CSTOPB;         // 1 stop bit
    tty.c_cflag &= ~CSIZE;          // Clear size bits
    tty.c_cflag |= CS8;             // 8 data bits
#ifdef CRTSCTS
    tty.c_cflag &= ~CRTSCTS;        // No hardware flow control
#endif
    tty.c_cflag |= CREAD | CLOCAL;  // Enable receiver, ignore modem controls

    // Input flags
    tty.c_iflag &= ~(IXON | IXOFF | IXANY);  // Disable software flow control
    tty.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL);

    // Output flags
    tty.c_oflag &= ~OPOST;  // Raw output

    // Local flags
    tty.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);  // Raw mode

    // Control characters
    tty.c_cc[VMIN] = 0;   // Non-blocking read
    tty.c_cc[VTIME] = 10; // 1 second timeout

    // Apply settings
    if (tcsetattr(port->fd, TCSANOW, &tty) != 0) {
        perror("Error setting serial port attributes");
        close(port->fd);
        return -1;
    }

    // Flush buffers
    tcflush(port->fd, TCIOFLUSH);

    // Store port information
    strncpy(port->port_path, port_path, sizeof(port->port_path) - 1);
    port->port_path[sizeof(port->port_path) - 1] = '\0';
    port->is_open = 1;

    return 0;
}

/**
 * Close serial port
 */
void close_serial_port(SerialPort* port) {
    if (port != NULL && port->is_open) {
        close(port->fd);
        port->is_open = 0;
        port->fd = -1;
    }
}

/**
 * Write data to serial port
 */
int write_serial_port(SerialPort* port, const char* data, int length) {
    if (port == NULL || !port->is_open || data == NULL || length <= 0) {
        return -1;
    }

    ssize_t written = write(port->fd, data, length);
    if (written < 0) {
        perror("Error writing to serial port");
        return -1;
    }

    // Ensure data is sent
    tcdrain(port->fd);

    return 0;
}
