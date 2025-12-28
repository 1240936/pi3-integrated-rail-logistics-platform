/*
 * Manager Component - USAC13 Test/Demo
 * 
 * Purpose: Test the functionality of getting data from Sensors component
 * Integrated with USAC11 initialization to use ManagerData structures
 * 
 * Usage: ./manager.elf <config_file> [serial_port]
 * Example: ./manager.elf config.txt /dev/ttyUSB0
 * 
 * Default serial port: /dev/ttyS0
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "sensor_manager.h"
#include "sensor_data.h"
#include "serial_comm.h"
#include "../../../USAC11/sprint3/manager/data_structures.h"

#define DEFAULT_SERIAL_PORT "/dev/ttyS0"

// Forward declaration - function is in initialize.c from USAC11
extern int initialize_from_file(const char* filename, ManagerData* data);
extern void free_manager_data(ManagerData* data);

int main(int argc, char* argv[]) {
    const char* config_file = NULL;
    const char* serial_port = DEFAULT_SERIAL_PORT;
    
    // Parse command line arguments
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <config_file> [serial_port]\n", argv[0]);
        fprintf(stderr, "Example: %s config.txt /dev/ttyUSB0\n", argv[0]);
        return 1;
    }
    
    config_file = argv[1];
    if (argc > 2) {
        serial_port = argv[2];
    }
    
    printf("=== Manager Component - USAC13 (Integrated with USAC11) ===\n\n");
    
    // Initialize ManagerData from config file (USAC11)
    ManagerData manager_data;
    printf("Loading configuration from: %s\n", config_file);
    if (initialize_from_file(config_file, &manager_data) != 1) {
        fprintf(stderr, "ERROR: Failed to initialize data structures from file.\n");
        return 1;
    }
    printf("Manager data initialized successfully!\n");
    printf("  - Users: %d\n", manager_data.num_users);
    printf("  - Tracks: %d\n", manager_data.num_tracks);
    printf("  - Trains: %d\n", manager_data.num_trains);
    printf("  - Sensor Config: Temp buffer=%d, window=%d | Hum buffer=%d, window=%d\n",
           manager_data.sensor_config.temp_buffer_length,
           manager_data.sensor_config.temp_window_length,
           manager_data.sensor_config.hum_buffer_length,
           manager_data.sensor_config.hum_window_length);
    printf("\n");
    
    // Initialize serial port
    printf("Initializing serial port: %s\n", serial_port);
    printf("Baud rate: %d\n", BAUD_RATE);
    SerialPort port;
    if (init_serial_port(&port, serial_port) != 0) {
        fprintf(stderr, "Failed to initialize serial port\n");
        free_manager_data(&manager_data);
        return 1;
    }
    printf(" Serial port opened successfully\n\n");
    
    // Initialize sensor data structure
    SensorData sensor_data;
    init_sensor_data(&sensor_data);
    
    // Get sensor data (USAC13 core functionality)
    printf("Requesting sensor data from Sensors component...\n");
    if (get_sensor_data(&port, &sensor_data) == 0) {
        printf(" Sensor data retrieved successfully!\n");
        printf("\n");
        printf("=== Sensor Readings ===\n");
        printf("Temperature: %d %s\n", sensor_data.temperature, sensor_data.temp_unit);
        printf("Humidity: %d %s\n", sensor_data.humidity, sensor_data.hum_unit);
        printf("\n");
        printf("Note: Sensor configuration from config file:\n");
        printf("  - Temperature buffer length: %d, window length: %d\n",
               manager_data.sensor_config.temp_buffer_length,
               manager_data.sensor_config.temp_window_length);
        printf("  - Humidity buffer length: %d, window length: %d\n",
               manager_data.sensor_config.hum_buffer_length,
               manager_data.sensor_config.hum_window_length);
    } else {
        fprintf(stderr, "Failed to get sensor data\n");
        close_serial_port(&port);
        free_manager_data(&manager_data);
        return 1;
    }
    
    // Close serial port
    close_serial_port(&port);
    printf("\n Serial port closed\n");
    
    // Free ManagerData
    free_manager_data(&manager_data);
    printf(" Manager data freed\n");
    
    return 0;
}

