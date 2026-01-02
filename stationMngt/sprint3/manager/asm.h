#ifndef ASM_H
#define ASM_H

// Include all assembly function headers from USAC01-09

// USAC01 - Encrypt data using Caesar Cipher
int encrypt_data(char* in, int key, char *out);

// USAC02 - Decrypt data using Caesar Cipher
int decrypt_data(char* in, int key, char *out);

// USAC03 - Extract data from token string
int extract_data(char* str, char* token, char* unit, int* value);

// USAC04 - Format command
int format_command(char* op, int n, char *cmd);

// USAC05 - Enqueue value into circular buffer
int enqueue_value(int* buffer, int length, int *nelem, int* tail, int* head, int value);

// USAC06 - Dequeue value from circular buffer
int dequeue_value(int* buffer, int length, int* nelem, int* tail, int* head, int *value);

// USAC07 - Move n elements from buffer to array
int move_n_to_array(int* buffer, int length, int *nelem, int *tail, int *head, int n, int* array);

// USAC08 - Sort array
int sort_array(int* vec, int length, char order);

// USAC09 - Calculate median
int median(int* vec, int length, int *me);

// USAC13 - Validate sensor data (assembly function with struct parameter)
// This function takes a SensorData struct pointer and validates the data ranges
// Valid ranges: temperature [-50, 50], humidity [0, 100]
// Returns 1 if valid, 0 if invalid
// NOTE: This function must be implemented in assembly as it takes a struct parameter
// This satisfies the requirement: "it is mandatory to implement at least one function
// using assembly, in which at least one of its parameters is a struct"
int validate_sensor_data(void* sensor);

#endif // ASM_H

