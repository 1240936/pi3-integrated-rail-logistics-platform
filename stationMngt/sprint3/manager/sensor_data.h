/*
 * Sensor Data Structures - USAC13
 * 
 * Purpose: Data structures for storing sensor data (temperature and humidity)
 * 
 * This file implements part of USAC13: "As a User, I want to get data from the Sensors component."
 */

#ifndef SENSOR_DATA_H
#define SENSOR_DATA_H

// Sensor data structure
typedef struct {
    int temperature;      // Temperature value in Celsius
    int humidity;         // Humidity value in percentage
    char temp_unit[20];   // Unit for temperature (e.g., "celsius")
    char hum_unit[20];    // Unit for humidity (e.g., "percentage")
} SensorData;

// Function to initialize sensor data structure
void init_sensor_data(SensorData* sensor);

// Function to update sensor data
void update_sensor_data(SensorData* sensor, int temp, int hum, 
                        const char* temp_unit, const char* hum_unit);

// Function to validate sensor data (assembly function - takes struct parameter)
// Returns 1 if valid, 0 if invalid
// Valid ranges: temperature [-50, 50], humidity [0, 100]
// NOTE: This function is implemented in assembly (validate_sensor_data.s)
// and declared in asm.h - it satisfies the requirement for at least one
// assembly function with a struct parameter
int validate_sensor_data(void* sensor);

#endif // SENSOR_DATA_H

