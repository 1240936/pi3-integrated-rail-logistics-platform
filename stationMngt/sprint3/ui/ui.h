/*
 * UI Component - Console User Interface
 * 
 * Purpose: Provide a robust console-based user interface for the Station Management System
 * 
 * Features:
 * - Robust input validation (handles malicious/error input)
 * - Menu-driven interface
 * - Command parsing and validation
 * - User-friendly error messages
 */

#ifndef UI_H
#define UI_H

// Maximum input length
#define MAX_INPUT_LENGTH 256
#define MAX_COMMAND_LENGTH 64
#define MAX_PARAM_LENGTH 128

// Command types
typedef enum {
    CMD_ASSIGN_TRACK,
    CMD_SET_MAINTENANCE,
    CMD_SET_FREE,
    CMD_DEPART,
    CMD_SYNOPSIS,
    CMD_GET_SENSOR_DATA,
    CMD_EXIT,
    CMD_HELP,
    CMD_INVALID
} CommandType;

// Parsed command structure
typedef struct {
    CommandType type;
    int param1;  // Track ID or Train ID
    int param2;  // Reserved for future use
    char raw_command[MAX_COMMAND_LENGTH];
} ParsedCommand;

/**
 * Initialize UI component
 */
void ui_init(void);

/**
 * Cleanup UI component
 */
void ui_cleanup(void);

/**
 * Display main menu
 */
void ui_show_main_menu(void);

/**
 * Display help information
 */
void ui_show_help(void);

/**
 * Read and parse user command
 * 
 * @param command Output structure for parsed command
 * @return 1 on success, 0 on failure
 */
int ui_read_command(ParsedCommand* command);

/**
 * Parse command string into ParsedCommand structure
 * 
 * @param input Input string from user
 * @param command Output structure for parsed command
 * @return 1 on success, 0 on failure
 */
int ui_parse_command(const char* input, ParsedCommand* command);

/**
 * Validate command parameters
 * 
 * @param command Parsed command to validate
 * @return 1 if valid, 0 if invalid
 */
int ui_validate_command(const ParsedCommand* command);

/**
 * Format command for sending to Manager
 * 
 * @param command Parsed command
 * @param output Output buffer for formatted command
 * @param output_size Size of output buffer
 * @return 1 on success, 0 on failure
 */
int ui_format_command(const ParsedCommand* command, char* output, int output_size);

/**
 * Display error message
 */
void ui_display_error(const char* message);

/**
 * Display success message
 */
void ui_display_success(const char* message);

/**
 * Display information message
 */
void ui_display_info(const char* message);

#endif // UI_H

