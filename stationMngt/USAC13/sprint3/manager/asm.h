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

// Note: validate_sensor_data is declared in sensor_data.h
// (it takes SensorData struct as parameter)

#endif // ASM_H

