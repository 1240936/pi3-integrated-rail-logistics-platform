/*
 * Serial Communication Implementation - USAC13
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/select.h>
#include <sys/time.h>
#include "serial_comm.h"

int init_serial_port(SerialPort* port, const char* port_path) {
    if (port == NULL || port_path == NULL) {
        return -1;
    }
    
    // Open serial port
    port->fd = open(port_path, O_RDWR | O_NOCTTY | O_NDELAY);
    if (port->fd < 0) {
        perror("Error opening serial port");
        return -1;
    }
    
    // Configure serial port
    struct termios tty;
    if (tcgetattr(port->fd, &tty) != 0) {
        perror("Error getting serial port attributes");
        close(port->fd);
        return -1;
    }
    
    // Set baud rate
    cfsetospeed(&tty, B9600);
    cfsetispeed(&tty, B9600);
    
    // 8N1 configuration
    tty.c_cflag &= ~PARENB;         // No parity
    tty.c_cflag &= ~CSTOPB;         // 1 stop bit
    tty.c_cflag &= ~CSIZE;          // Clear size bits
    tty.c_cflag |= CS8;             // 8 data bits
    tty.c_cflag &= ~CRTSCTS;        // No hardware flow control
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
    tty.c_cc[VTIME] = 10; // 1 second timeout (10 * 0.1s)
    
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

void close_serial_port(SerialPort* port) {
    if (port != NULL && port->is_open) {
        close(port->fd);
        port->is_open = 0;
        port->fd = -1;
    }
}

int send_command(SerialPort* port, const char* command) {
    if (port == NULL || !port->is_open || command == NULL) {
        return -1;
    }
    
    // Write command with newline
    int len = strlen(command);
    char cmd_with_newline[len + 2];
    strcpy(cmd_with_newline, command);
    strcat(cmd_with_newline, "\n");
    
    ssize_t written = write(port->fd, cmd_with_newline, strlen(cmd_with_newline));
    if (written < 0) {
        perror("Error writing to serial port");
        return -1;
    }
    
    // Ensure data is sent
    tcdrain(port->fd);
    
    return 0;
}

int receive_response(SerialPort* port, char* buffer, int buffer_size) {
    if (port == NULL || !port->is_open || buffer == NULL || buffer_size <= 0) {
        return -1;
    }
    
    // Use select for timeout
    fd_set read_fds;
    struct timeval timeout;
    
    FD_ZERO(&read_fds);
    FD_SET(port->fd, &read_fds);
    
    timeout.tv_sec = CMD_TIMEOUT_MS / 1000;
    timeout.tv_usec = (CMD_TIMEOUT_MS % 1000) * 1000;
    
    int select_result = select(port->fd + 1, &read_fds, NULL, NULL, &timeout);
    
    if (select_result < 0) {
        perror("Error in select");
        return -1;
    }
    
    if (select_result == 0) {
        // Timeout
        return 0;
    }
    
    if (!FD_ISSET(port->fd, &read_fds)) {
        return -1;
    }
    
    // Read data
    ssize_t bytes_read = read(port->fd, buffer, buffer_size - 1);
    
    if (bytes_read < 0) {
        perror("Error reading from serial port");
        return -1;
    }
    
    if (bytes_read == 0) {
        return 0;  // No data available
    }
    
    // Null-terminate the string
    buffer[bytes_read] = '\0';
    
    // Remove newline if present
    if (bytes_read > 0 && buffer[bytes_read - 1] == '\n') {
        buffer[bytes_read - 1] = '\0';
    }
    if (bytes_read > 1 && buffer[bytes_read - 2] == '\r') {
        buffer[bytes_read - 2] = '\0';
    }
    
    return (int)bytes_read;
}

int get_sensor_response(SerialPort* port, char* response, int response_size) {
    if (port == NULL || response == NULL || response_size <= 0) {
        return -1;
    }
    
    // Send GTH command
    if (send_command(port, GTH_CMD) != 0) {
        return -1;
    }
    
    // Arduino needs 2 seconds delay after receiving GTH before reading sensor
    // Wait 2.5 seconds to ensure sensor reading is complete
    usleep(2500000);  // 2.5 seconds
    
    // Receive response
    int bytes_read = receive_response(port, response, response_size);
    
    return bytes_read;
}

