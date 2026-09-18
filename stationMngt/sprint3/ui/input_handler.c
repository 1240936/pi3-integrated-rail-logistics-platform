/*
 * Input Handler - Robust Input Processing
 * 
 * Purpose: Handle user input with validation and error checking
 * Implements robust input handling as required by the UI specification
 */

#include <stdio.h>
#include "ui.h"

// Custom string length (avoiding string.h)
static int str_length(const char* str) {
    if (str == NULL) return 0;
    int len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

// Check if character is whitespace
static int is_whitespace(char c) {
    return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
}

// Check if character is a digit
static int is_digit(char c) {
    return (c >= '0' && c <= '9');
}

// Trim whitespace from string (modifies input)
static void trim_whitespace(char* str) {
    if (str == NULL) return;
    
    int len = str_length(str);
    if (len == 0) return;
    
    // Trim trailing whitespace
    int end = len - 1;
    while (end >= 0 && is_whitespace(str[end])) {
        str[end] = '\0';
        end--;
    }
    
    // Trim leading whitespace
    int start = 0;
    while (str[start] != '\0' && is_whitespace(str[start])) {
        start++;
    }
    
    if (start > 0) {
        int i = 0;
        while (str[start + i] != '\0') {
            str[i] = str[start + i];
            i++;
        }
        str[i] = '\0';
    }
}

/**
 * Read a line from stdin with length limit
 * 
 * @param buffer Output buffer
 * @param max_size Maximum buffer size
 * @return Number of characters read (excluding null terminator)
 */
int read_input_line(char* buffer, int max_size) {
    if (buffer == NULL || max_size <= 0) {
        return 0;
    }
    
    int i = 0;
    int ch;
    
    while (i < max_size - 1) {
        ch = getchar();
        
        if (ch == EOF) {
            if (i == 0) {
                return -1;  // EOF before any input
            }
            break;
        }
        
        if (ch == '\n') {
            break;
        }
        
        // Store character
        buffer[i++] = (char)ch;
    }
    
    buffer[i] = '\0';
    trim_whitespace(buffer);
    
    return i;
}

/**
 * Parse integer from string with validation
 * 
 * @param str String to parse
 * @param value Output integer value
 * @return 1 on success, 0 on failure
 */
int parse_integer(const char* str, int* value) {
    if (str == NULL || value == NULL) {
        return 0;
    }
    
    int len = str_length(str);
    if (len == 0) {
        return 0;
    }
    
    // Check for negative sign
    int start = 0;
    int negative = 0;
    if (str[0] == '-') {
        negative = 1;
        start = 1;
    } else if (str[0] == '+') {
        start = 1;
    }
    
    // Check if all remaining characters are digits
    for (int i = start; i < len; i++) {
        if (!is_digit(str[i])) {
            return 0;
        }
    }
    
    // Parse integer (simple implementation)
    int result = 0;
    for (int i = start; i < len; i++) {
        result = result * 10 + (str[i] - '0');
    }
    
    if (negative) {
        result = -result;
    }
    
    *value = result;
    return 1;
}

/**
 * Validate that a string contains only printable characters
 * 
 * @param str String to validate
 * @return 1 if valid, 0 if invalid
 */
int is_valid_string(const char* str) {
    if (str == NULL) {
        return 0;
    }
    
    for (int i = 0; str[i] != '\0'; i++) {
        // Allow printable ASCII characters (32-126)
        if (str[i] < 32 || str[i] > 126) {
            return 0;
        }
    }
    
    return 1;
}

/**
 * Convert string to uppercase (for command comparison)
 */
void to_uppercase(char* str) {
    if (str == NULL) return;
    
    for (int i = 0; str[i] != '\0'; i++) {
        if (str[i] >= 'a' && str[i] <= 'z') {
            str[i] = str[i] - 'a' + 'A';
        }
    }
}

