/*
 * Sensor Manager - USAC13
 * 
 * Purpose: Main interface for getting data from Sensors component
 * 
 * This component implements USAC13: "As a User, I want to get data from the Sensors component."
 * 
 * Acceptance Criteria:
 * 1. Manager can send "GTH" command to Sensors component via serial communication
 * 2. Manager can receive response in format: "TEMP&unit:celsius&value:xx#HUM&unit:percentage&value:xx"
 * 3. Manager uses extract_data assembly function (USAC03) to parse the response
 * 4. Manager updates sensor data structure with extracted values
 * 5. Function returns 0 on success, -1 on error
 * 6. Handles communication errors and timeouts gracefully
 */

#ifndef SENSOR_MANAGER_H
#define SENSOR_MANAGER_H

#include "sensor_data.h"
#include "serial_comm.h"

// Get sensor data from Sensors component
// This is the main function for USAC13
// Returns 0 on success, -1 on error
int get_sensor_data(SerialPort* port, SensorData* sensor_data);

// Parse sensor response string and update sensor data structure
// Uses extract_data assembly function from USAC03
// Returns 0 on success, -1 on error
int parse_sensor_response(const char* response, SensorData* sensor_data);

#endif // SENSOR_MANAGER_H

