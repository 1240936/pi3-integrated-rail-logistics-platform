#ifndef BOARD_COMM_H
#define BOARD_COMM_H

#include "serial_comm.h"

/**
 * USAC16 - Board Communication Interface
 * Integrates with serial port for track status updates
 */

// Communication protocol constants
#define STX 0x02  // Start of Text
#define LF  '\n'  // Line Feed (End of Packet)

/**
 * Initialize board communication on specified serial port
 *
 * @param port Pointer to SerialPort structure to initialize
 * @param serial_port Path to serial port (e.g., "/dev/ttyUSB0")
 * @return 0 on success, -1 on error
 */
int init_board_comm(SerialPort* port, const char* serial_port);

/**
 * Send formatted data to board component
 *
 * @param port Pointer to initialized SerialPort structure
 * @param formatted_data Null-terminated string to send
 * @return 0 on success, -1 on error
 */
int send_to_board(SerialPort* port, const char* formatted_data);

/**
 * Close board communication port
 *
 * @param port Pointer to SerialPort structure to close
 */
void close_board_comm(SerialPort* port);

/**
 * Calculate checksum for data integrity
 *
 * @param str Null-terminated string to calculate checksum for
 * @return XOR checksum of all characters
 */
char calculate_checksum(const char* str);

#endif // BOARD_COMM_H
