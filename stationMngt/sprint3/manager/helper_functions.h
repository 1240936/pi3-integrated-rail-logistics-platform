#ifndef HELPER_FUNCTIONS_H
#define HELPER_FUNCTIONS_H

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
int get_token(char* str, int start_pos, char delimiter, char* token, int token_size);

/**
 * Convert integer to string
 *
 * @param value Integer value to convert
 * @param output Output buffer for string
 * @param max_len Maximum length of output buffer
 * @return 1 on success, 0 on error
 */
int int_to_string(int value, char* output, int max_len);

/**
 * Parse long integer from string
 *
 * @param str String to parse
 * @param value Pointer to long int where result will be stored
 * @return 1 on success, 0 on error
 */
int parse_long_int(const char* str, long int* value);

#endif // HELPER_FUNCTIONS_H