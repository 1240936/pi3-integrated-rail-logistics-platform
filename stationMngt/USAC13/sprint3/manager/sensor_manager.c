/*
 * Sensor Manager Implementation - USAC13
 * 
 * This implements the core functionality for USAC13:
 * Getting data from the Sensors component
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "sensor_manager.h"
#include "asm.h"

#define TEMP_TOKEN "TEMP"
#define HUM_TOKEN "HUM"

int parse_sensor_response(const char* response, SensorData* sensor_data) {
    if (response == NULL || sensor_data == NULL) {
        return -1;
    }
    
    char temp_unit[20] = {0};
    char hum_unit[20] = {0};
    int temp_value = 0;
    int hum_value = 0;
    
    // Extract temperature data using assembly function
    int temp_result = extract_data((char*)response, (char*)TEMP_TOKEN, temp_unit, &temp_value);
    if (temp_result != 1) {
        fprintf(stderr, "Error: Failed to extract temperature data\n");
        return -1;
    }
    
    // Extract humidity data using assembly function
    int hum_result = extract_data((char*)response, (char*)HUM_TOKEN, hum_unit, &hum_value);
    if (hum_result != 1) {
        fprintf(stderr, "Error: Failed to extract humidity data\n");
        return -1;
    }
    
    // Update sensor data structure
    update_sensor_data(sensor_data, temp_value, hum_value, temp_unit, hum_unit);
    
    return 0;
}

int get_sensor_data(SerialPort* port, SensorData* sensor_data) {
    if (port == NULL || sensor_data == NULL) {
        fprintf(stderr, "Error: Invalid parameters\n");
        return -1;
    }
    
    if (!port->is_open) {
        fprintf(stderr, "Error: Serial port is not open\n");
        return -1;
    }
    
    // Buffer for sensor response
    char response[SERIAL_BUFFER_SIZE] = {0};
    
    // Get response from Sensors component
    int bytes_read = get_sensor_response(port, response, sizeof(response));
    
    if (bytes_read <= 0) {
        if (bytes_read == 0) {
            fprintf(stderr, "Error: Timeout waiting for sensor response\n");
        } else {
            fprintf(stderr, "Error: Failed to receive sensor response\n");
        }
        return -1;
    }
    
    // Parse and update sensor data
    if (parse_sensor_response(response, sensor_data) != 0) {
        fprintf(stderr, "Error: Failed to parse sensor response: %s\n", response);
        return -1;
    }
    
    // Validate sensor data using assembly function (takes struct parameter)
    if (validate_sensor_data(sensor_data) == 0) {
        fprintf(stderr, "Warning: Sensor data validation failed (temp: %d, hum: %d)\n", 
                sensor_data->temperature, sensor_data->humidity);
        // Note: We still return success as the data was received, but log the warning
    }
    
    return 0;
}

