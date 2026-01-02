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

/**
 * Encrypt data using Caesar Cipher (USAC01)
 *
 * @param in Input string (A-Z only)
 * @param key Caesar cipher key (1-25)
 * @param out Output buffer for encrypted string
 * @return 1 on success, 0 on error
 */
int encrypt_data(char* in, int key, char* out) {
    if (in == NULL || out == NULL || key < 1 || key > 25) {
        return 0;
    }

    int i = 0;
    while (in[i] != '\0') {
        if (in[i] >= 'A' && in[i] <= 'Z') {
            out[i] = ((in[i] - 'A' + key) % 26) + 'A';
        } else {
            // Keep non-letter characters as is
            out[i] = in[i];
        }
        i++;
    }
    out[i] = '\0';
    return 1;
}

/**
 * Decrypt data using Caesar Cipher (USAC02)
 *
 * @param in Input encrypted string
 * @param key Caesar cipher key (1-25)
 * @param out Output buffer for decrypted string
 * @return 1 on success, 0 on error
 */
int decrypt_data(char* in, int key, char* out) {
    if (in == NULL || out == NULL || key < 1 || key > 25) {
        return 0;
    }

    int i = 0;
    while (in[i] != '\0') {
        if (in[i] >= 'A' && in[i] <= 'Z') {
            out[i] = ((in[i] - 'A' - key + 26) % 26) + 'A';
        } else {
            // Keep non-letter characters as is
            out[i] = in[i];
        }
        i++;
    }
    out[i] = '\0';
    return 1;
}

/**
 * Extract data from token string (USAC03)
 *
 * @param str Input string in format "TOKEN&unit:value"
 * @param token Output buffer for token
 * @param unit Output buffer for unit
 * @param value Pointer to int for value
 * @return 1 on success, 0 on error
 */
int extract_data(char* str, char* token, char* unit, int* value) {
    if (str == NULL || token == NULL || unit == NULL || value == NULL) {
        return 0;
    }

    // Simple parsing for format like "TEMP&celsius&25"
    char* pos1 = strchr(str, '&');
    if (pos1 == NULL) return 0;

    int token_len = pos1 - str;
    if (token_len >= 50) return 0; // Assume max 50 chars
    strncpy(token, str, token_len);
    token[token_len] = '\0';

    char* pos2 = strchr(pos1 + 1, '&');
    if (pos2 == NULL) return 0;

    int unit_len = pos2 - pos1 - 1;
    if (unit_len >= 20) return 0; // Assume max 20 chars
    strncpy(unit, pos1 + 1, unit_len);
    unit[unit_len] = '\0';

    *value = atoi(pos2 + 1);
    return 1;
}

/**
 * Format command (USAC04)
 *
 * @param op Operation string (e.g., "RE", "GE", "YE")
 * @param n Track number
 * @param cmd Output buffer for formatted command
 * @return 1 on success, 0 on error
 */
int format_command(char* op, int n, char* cmd) {
    if (op == NULL || cmd == NULL || n < 0 || n > 99) {
        return 0;
    }

    // Format as "OP,x" where x is track number (USAC10 compatible)
    sprintf(cmd, "%s,%d", op, n);
    return 1;
}

/**
 * Enqueue value into circular buffer (USAC05)
 *
 * @param buffer Circular buffer array
 * @param length Buffer length
 * @param nelem Current number of elements
 * @param tail Tail pointer
 * @param head Head pointer
 * @param value Value to enqueue
 * @return 1 on success, 0 on error
 */
int enqueue_value(int* buffer, int length, int* nelem, int* tail, int* head, int value) {
    if (buffer == NULL || nelem == NULL || tail == NULL || head == NULL) {
        return 0;
    }

    if (*nelem >= length) {
        return 0; // Buffer full
    }

    buffer[*head] = value;
    *head = (*head + 1) % length;
    (*nelem)++;

    return 1;
}

/**
 * Dequeue value from circular buffer (USAC06)
 *
 * @param buffer Circular buffer array
 * @param length Buffer length
 * @param nelem Current number of elements
 * @param tail Tail pointer
 * @param head Head pointer
 * @param value Pointer to store dequeued value
 * @return 1 on success, 0 on error
 */
int dequeue_value(int* buffer, int length, int* nelem, int* tail, int* head, int* value) {
    if (buffer == NULL || nelem == NULL || tail == NULL || head == NULL || value == NULL) {
        return 0;
    }

    if (*nelem <= 0) {
        return 0; // Buffer empty
    }

    *value = buffer[*tail];
    *tail = (*tail + 1) % length;
    (*nelem)--;

    return 1;
}

/**
 * Move n elements from buffer to array (USAC07)
 *
 * @param buffer Circular buffer array
 * @param length Buffer length
 * @param nelem Current number of elements
 * @param tail Tail pointer
 * @param head Head pointer
 * @param n Number of elements to move
 * @param array Output array
 * @return 1 on success, 0 on error
 */
int move_n_to_array(int* buffer, int length, int* nelem, int* tail, int* head, int n, int* array) {
    if (buffer == NULL || nelem == NULL || tail == NULL || head == NULL || array == NULL) {
        return 0;
    }

    if (*nelem < n || n <= 0) {
        return 0;
    }

    for (int i = 0; i < n; i++) {
        if (!dequeue_value(buffer, length, nelem, tail, head, &array[i])) {
            return 0;
        }
    }

    return 1;
}

/**
 * Sort array (USAC08)
 *
 * @param vec Array to sort
 * @param length Array length
 * @param order 'A' for ascending, 'D' for descending
 * @return 1 on success, 0 on error
 */
int sort_array(int* vec, int length, char order) {
    if (vec == NULL || length <= 0) {
        return 0;
    }

    if (order == 'A') {
        // Bubble sort ascending
        for (int i = 0; i < length - 1; i++) {
            for (int j = 0; j < length - i - 1; j++) {
                if (vec[j] > vec[j + 1]) {
                    int temp = vec[j];
                    vec[j] = vec[j + 1];
                    vec[j + 1] = temp;
                }
            }
        }
    } else if (order == 'D') {
        // Bubble sort descending
        for (int i = 0; i < length - 1; i++) {
            for (int j = 0; j < length - i - 1; j++) {
                if (vec[j] < vec[j + 1]) {
                    int temp = vec[j];
                    vec[j] = vec[j + 1];
                    vec[j + 1] = temp;
                }
            }
        }
    } else {
        return 0; // Invalid order
    }

    return 1;
}

/**
 * Calculate median (USAC09)
 *
 * @param vec Array of values
 * @param length Array length
 * @param me Pointer to store median value
 * @return 1 on success, 0 on error
 */
int median(int* vec, int length, int* me) {
    if (vec == NULL || length <= 0 || me == NULL) {
        return 0;
    }

    // Create a copy to sort
    int* sorted = (int*)malloc(length * sizeof(int));
    if (sorted == NULL) {
        return 0;
    }

    memcpy(sorted, vec, length * sizeof(int));
    sort_array(sorted, length, 'A');

    if (length % 2 == 1) {
        *me = sorted[length / 2];
    } else {
        *me = (sorted[length / 2 - 1] + sorted[length / 2]) / 2;
    }

    free(sorted);
    return 1;
}