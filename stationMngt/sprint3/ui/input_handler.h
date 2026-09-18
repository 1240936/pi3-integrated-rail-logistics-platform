/*
 * Input Handler Header
 */

#ifndef INPUT_HANDLER_H
#define INPUT_HANDLER_H

/**
 * Read a line from stdin with length limit
 */
int read_input_line(char* buffer, int max_size);

/**
 * Parse integer from string with validation
 */
int parse_integer(const char* str, int* value);

/**
 * Validate that a string contains only printable characters
 */
int is_valid_string(const char* str);

/**
 * Convert string to uppercase
 */
void to_uppercase(char* str);

#endif // INPUT_HANDLER_H

