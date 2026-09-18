/*
 * Sensor Data Implementation - USAC13
 */

#include <string.h>
#include "sensor_data.h"

void init_sensor_data(SensorData* sensor) {
    if (sensor == NULL) {
        return;
    }
    
    sensor->temperature = 0;
    sensor->humidity = 0;
    strcpy(sensor->temp_unit, "");
    strcpy(sensor->hum_unit, "");
}

void update_sensor_data(SensorData* sensor, int temp, int hum, 
                        const char* temp_unit, const char* hum_unit) {
    if (sensor == NULL) {
        return;
    }
    
    sensor->temperature = temp;
    sensor->humidity = hum;
    
    if (temp_unit != NULL) {
        strncpy(sensor->temp_unit, temp_unit, sizeof(sensor->temp_unit) - 1);
        sensor->temp_unit[sizeof(sensor->temp_unit) - 1] = '\0';
    }
    
    if (hum_unit != NULL) {
        strncpy(sensor->hum_unit, hum_unit, sizeof(sensor->hum_unit) - 1);
        sensor->hum_unit[sizeof(sensor->hum_unit) - 1] = '\0';
    }
}

