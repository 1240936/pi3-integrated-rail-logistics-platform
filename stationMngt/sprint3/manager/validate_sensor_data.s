.section .text
.globl validate_sensor_data

# USAC13 - Validate Sensor Data (Assembly function with struct parameter)
# 
# This function validates sensor data ranges according to specifications:
# - Temperature: [-50, 50] Celsius
# - Humidity: [0, 100] percentage
#
# Parameters:
#   a0 = pointer to SensorData struct
#
# SensorData struct layout:
#   offset 0:  int temperature (4 bytes)
#   offset 4:  int humidity (4 bytes)
#   offset 8:  char temp_unit[20] (20 bytes)
#   offset 28: char hum_unit[20] (20 bytes)
#   Total size: 48 bytes
#
# Returns:
#   a0 = 1 if valid, 0 if invalid

validate_sensor_data:
    # Check if pointer is NULL
    beq a0, zero, fail_validation
    
    # Load temperature from struct (offset 0)
    lw t0, 0(a0)          # t0 = sensor->temperature
    
    # Check temperature range: [-50, 50]
    li t1, -50
    blt t0, t1, fail_validation  # if temp < -50, fail
    li t1, 50
    bgt t0, t1, fail_validation  # if temp > 50, fail
    
    # Load humidity from struct (offset 4)
    lw t0, 4(a0)          # t0 = sensor->humidity
    
    # Check humidity range: [0, 100]
    li t1, 0
    blt t0, t1, fail_validation  # if hum < 0, fail
    li t1, 100
    bgt t0, t1, fail_validation  # if hum > 100, fail
    
    # All checks passed
    li a0, 1
    ret
    
fail_validation:
    li a0, 0
    ret

