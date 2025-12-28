/*
 * Helper functions for USAC11 initialize.c
 * 
 * These functions are used by initialize_from_file but are not yet implemented
 * in the USAC11 codebase. This file provides implementations for them.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/**
 * Extract token from string starting at position, using delimiter
 * 
 * @param str Input string
 * @param start_pos Starting position in string
 * @param delimiter Character that delimits tokens
 * @param token Output buffer for token
 * @param token_size Size of token buffer
 * @return Next position after token and delimiter, or -1 on error
 */
int get_token(char* str, int start_pos, char delimiter, char* token, int token_size) {
    if (str == NULL || token == NULL || token_size <= 0) {
        return -1;
    }
    
    int i = start_pos;
    int token_len = 0;
    
    // Skip leading whitespace
    while (str[i] != '\0' && (str[i] == ' ' || str[i] == '\t')) {
        i++;
    }
    
    // Extract token until delimiter or end of string
    while (str[i] != '\0' && str[i] != delimiter && token_len < token_size - 1) {
        token[token_len] = str[i];
        token_len++;
        i++;
    }
    
    token[token_len] = '\0';
    
    // Skip delimiter if present
    if (str[i] == delimiter) {
        i++;
    }
    
    return i;
}

/**
 * Convert integer to string
 * 
 * @param value Integer value to convert
 * @param output Output buffer for string
 * @param max_len Maximum length of output buffer
 * @return 1 on success, 0 on error
 */
int int_to_string(int value, char* output, int max_len) {
    if (output == NULL || max_len <= 0) {
        return 0;
    }
    
    // Handle negative numbers
    int is_negative = 0;
    if (value < 0) {
        is_negative = 1;
        value = -value;
    }
    
    // Convert to string (reverse order)
    char temp[32];
    int pos = 0;
    
    if (value == 0) {
        temp[pos++] = '0';
    } else {
        while (value > 0 && pos < 31) {
            temp[pos++] = '0' + (value % 10);
            value /= 10;
        }
    }
    
    // Check if we have enough space
    int needed = pos + (is_negative ? 1 : 0) + 1; // +1 for null terminator
    if (needed > max_len) {
        return 0;
    }
    
    // Reverse and copy to output
    int out_pos = 0;
    if (is_negative) {
        output[out_pos++] = '-';
    }
    
    for (int i = pos - 1; i >= 0; i--) {
        output[out_pos++] = temp[i];
    }
    output[out_pos] = '\0';
    
    return 1;
}

/**
 * Parse long integer from string
 * 
 * @param str String to parse
 * @param value Pointer to long int where result will be stored
 * @return 1 on success, 0 on error
 */
int parse_long_int(const char* str, long int* value) {
    if (str == NULL || value == NULL) {
        return 0;
    }
    
    long int result = 0;
    int sign = 1;
    int i = 0;
    
    // Skip whitespace
    while (str[i] != '\0' && (str[i] == ' ' || str[i] == '\t')) {
        i++;
    }
    
    // Check for sign
    if (str[i] == '-') {
        sign = -1;
        i++;
    } else if (str[i] == '+') {
        i++;
    }
    
    // Parse digits
    int has_digits = 0;
    while (str[i] != '\0' && str[i] >= '0' && str[i] <= '9') {
        result = result * 10 + (str[i] - '0');
        i++;
        has_digits = 1;
    }
    
    // Check if there are invalid characters remaining
    while (str[i] != '\0') {
        if (str[i] != ' ' && str[i] != '\t') {
            return 0; // Invalid character found
        }
        i++;
    }
    
    if (!has_digits) {
        return 0; // No digits found
    }
    
    *value = result * sign;
    return 1;
}

