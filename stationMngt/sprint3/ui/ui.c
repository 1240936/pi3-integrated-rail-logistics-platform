/*
 * UI Component Implementation
 * 
 * Purpose: Console-based user interface with robust input handling
 */

#include <stdio.h>
#include "ui.h"

// Declare input_handler functions (defined in input_handler.c)
extern int read_input_line(char* buffer, int max_size);
extern int parse_integer(const char* str, int* value);
extern void to_uppercase(char* str);

// Custom string comparison (avoiding string.h)
static int str_compare(const char* s1, const char* s2) {
    if (s1 == NULL || s2 == NULL) return -1;
    while (*s1 && (*s1 == *s2)) {
        s1++;
        s2++;
    }
    return *(const unsigned char*)s1 - *(const unsigned char*)s2;
}

// Custom string length (avoiding string.h)
static int str_length(const char* str) {
    if (str == NULL) return 0;
    int len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

void ui_init(void) {
    // Clear screen (if terminal supports it)
    printf("\033[2J\033[H");  // ANSI escape codes
    printf("=== Station Management System - UI Component ===\n\n");
}

void ui_cleanup(void) {
    printf("\n=== UI Component Shutting Down ===\n");
}

void ui_show_main_menu(void) {
    printf("\n=== MAIN MENU ===\n");
    printf("1. Assign track to train\n");
    printf("2. Set track to maintenance\n");
    printf("3. Set track as free\n");
    printf("4. Issue departure order\n");
    printf("5. View track status (synopsis)\n");
    printf("6. Help\n");
    printf("0. Exit\n");
    printf("\nEnter command (number or name): ");
    fflush(stdout);
}

void ui_show_help(void) {
    printf("\n=== HELP ===\n");
    printf("Available commands:\n");
    printf("  ASSIGN_TRACK <train_id>  - Assign a track to an arriving train\n");
    printf("  SET_MAINTENANCE <track_id> - Set a track to maintenance mode\n");
    printf("  SET_FREE <track_id>      - Free a track (make it available)\n");
    printf("  DEPART <track_id>        - Issue departure order for a train\n");
    printf("  SYNOPSIS                 - View current track status\n");
    printf("  HELP                     - Show this help message\n");
    printf("  EXIT                     - Exit the system\n");
    printf("\nExamples:\n");
    printf("  ASSIGN_TRACK 101\n");
    printf("  SET_MAINTENANCE 2\n");
    printf("  SYNOPSIS\n");
    printf("\n");
}

int ui_read_command(ParsedCommand* command) {
    if (command == NULL) {
        return 0;
    }
    
    char input[MAX_INPUT_LENGTH];
    int bytes_read = read_input_line(input, sizeof(input));
    
    if (bytes_read < 0) {
        return 0;  // EOF
    }
    
    if (bytes_read == 0) {
        command->type = CMD_INVALID;
        return 0;  // Empty input
    }
    
    return ui_parse_command(input, command);
}

int ui_parse_command(const char* input, ParsedCommand* command) {
    if (input == NULL || command == NULL) {
        return 0;
    }
    
    // Copy input to raw_command
    int len = str_length(input);
    if (len >= MAX_COMMAND_LENGTH) {
        return 0;  // Input too long
    }
    
    // Copy and convert to uppercase for comparison
    for (int i = 0; i < len; i++) {
        command->raw_command[i] = input[i];
    }
    command->raw_command[len] = '\0';
    to_uppercase(command->raw_command);
    
    // Try to parse as numeric menu selection first
    int menu_number;
    if (parse_integer(command->raw_command, &menu_number)) {
        switch (menu_number) {
            case 1: command->type = CMD_ASSIGN_TRACK; break;
            case 2: command->type = CMD_SET_MAINTENANCE; break;
            case 3: command->type = CMD_SET_FREE; break;
            case 4: command->type = CMD_DEPART; break;
            case 5: command->type = CMD_SYNOPSIS; break;
            case 6: command->type = CMD_HELP; break;
            case 0: command->type = CMD_EXIT; break;
            default: command->type = CMD_INVALID; return 0;
        }
        // For numeric commands, we'll need to prompt for parameters
        command->param1 = 0;
        command->param2 = 0;
        return 1;
    }
    
    // Parse text command
    // Find space or end of string
    int cmd_end = 0;
    while (command->raw_command[cmd_end] != '\0' && 
           command->raw_command[cmd_end] != ' ' &&
           command->raw_command[cmd_end] != '\t') {
        cmd_end++;
    }
    
    // Extract command name
    char cmd_name[MAX_COMMAND_LENGTH];
    for (int i = 0; i < cmd_end; i++) {
        cmd_name[i] = command->raw_command[i];
    }
    cmd_name[cmd_end] = '\0';
    
    // Parse command type
    if (str_compare(cmd_name, "ASSIGN_TRACK") == 0) {
        command->type = CMD_ASSIGN_TRACK;
    } else if (str_compare(cmd_name, "SET_MAINTENANCE") == 0) {
        command->type = CMD_SET_MAINTENANCE;
    } else if (str_compare(cmd_name, "SET_FREE") == 0) {
        command->type = CMD_SET_FREE;
    } else if (str_compare(cmd_name, "DEPART") == 0) {
        command->type = CMD_DEPART;
    } else if (str_compare(cmd_name, "SYNOPSIS") == 0) {
        command->type = CMD_SYNOPSIS;
    } else if (str_compare(cmd_name, "HELP") == 0) {
        command->type = CMD_HELP;
    } else if (str_compare(cmd_name, "EXIT") == 0 || str_compare(cmd_name, "QUIT") == 0) {
        command->type = CMD_EXIT;
    } else {
        command->type = CMD_INVALID;
        return 0;
    }
    
    // Parse parameter if needed
    command->param1 = 0;
    command->param2 = 0;
    
    if (command->type == CMD_ASSIGN_TRACK || 
        command->type == CMD_SET_MAINTENANCE ||
        command->type == CMD_SET_FREE ||
        command->type == CMD_DEPART) {
        
        // Find start of parameter (skip whitespace)
        int param_start = cmd_end;
        while (command->raw_command[param_start] != '\0' && 
               (command->raw_command[param_start] == ' ' || 
                command->raw_command[param_start] == '\t')) {
            param_start++;
        }
        
        if (command->raw_command[param_start] != '\0') {
            // Parse parameter
            char param_str[MAX_PARAM_LENGTH];
            int param_len = 0;
            while (command->raw_command[param_start] != '\0' && 
                   param_len < MAX_PARAM_LENGTH - 1) {
                param_str[param_len++] = command->raw_command[param_start++];
            }
            param_str[param_len] = '\0';
            
            if (!parse_integer(param_str, &command->param1)) {
                return 0;  // Invalid parameter
            }
        } else {
            // No parameter provided - will prompt later
            command->param1 = 0;
        }
    }
    
    return 1;
}

int ui_validate_command(const ParsedCommand* command) {
    if (command == NULL) {
        return 0;
    }
    
    if (command->type == CMD_INVALID) {
        return 0;
    }
    
    // Validate parameters for commands that require them
    if (command->type == CMD_ASSIGN_TRACK) {
        if (command->param1 <= 0) {
            ui_display_error("Train ID must be a positive integer");
            return 0;
        }
    } else if (command->type == CMD_SET_MAINTENANCE ||
               command->type == CMD_SET_FREE ||
               command->type == CMD_DEPART) {
        if (command->param1 <= 0) {
            ui_display_error("Track ID must be a positive integer");
            return 0;
        }
    }
    
    return 1;
}

int ui_format_command(const ParsedCommand* command, char* output, int output_size) {
    if (command == NULL || output == NULL || output_size <= 0) {
        return 0;
    }
    
    switch (command->type) {
        case CMD_ASSIGN_TRACK:
            // Simple integer formatting (avoiding sprintf)
            {
                int len = 0;
                output[len++] = 'A';
                output[len++] = 'S';
                output[len++] = 'S';
                output[len++] = 'I';
                output[len++] = 'G';
                output[len++] = 'N';
                output[len++] = '_';
                output[len++] = 'T';
                output[len++] = 'R';
                output[len++] = 'A';
                output[len++] = 'C';
                output[len++] = 'K';
                output[len++] = ':';
                // Convert param1 to string
                int num = command->param1;
                char num_str[16];
                int num_len = 0;
                if (num == 0) {
                    num_str[num_len++] = '0';
                } else {
                    char temp[16];
                    int temp_len = 0;
                    while (num > 0) {
                        temp[temp_len++] = '0' + (num % 10);
                        num /= 10;
                    }
                    // Reverse
                    for (int i = temp_len - 1; i >= 0; i--) {
                        num_str[num_len++] = temp[i];
                    }
                }
                for (int i = 0; i < num_len && len < output_size - 1; i++) {
                    output[len++] = num_str[i];
                }
                output[len] = '\0';
            }
            return 1;
            
        case CMD_SET_MAINTENANCE:
            {
                int len = 0;
                const char* cmd = "SET_MAINTENANCE:";
                while (cmd[len] != '\0' && len < output_size - 1) {
                    output[len] = cmd[len];
                    len++;
                }
                // Add parameter (simplified - using same logic as above)
                int num = command->param1;
                char num_str[16];
                int num_len = 0;
                if (num == 0) {
                    num_str[num_len++] = '0';
                } else {
                    char temp[16];
                    int temp_len = 0;
                    while (num > 0) {
                        temp[temp_len++] = '0' + (num % 10);
                        num /= 10;
                    }
                    for (int i = temp_len - 1; i >= 0; i--) {
                        num_str[num_len++] = temp[i];
                    }
                }
                for (int i = 0; i < num_len && len < output_size - 1; i++) {
                    output[len++] = num_str[i];
                }
                output[len] = '\0';
            }
            return 1;
            
        case CMD_SET_FREE:
            {
                int len = 0;
                const char* cmd = "SET_FREE:";
                while (cmd[len] != '\0' && len < output_size - 1) {
                    output[len] = cmd[len];
                    len++;
                }
                int num = command->param1;
                char num_str[16];
                int num_len = 0;
                if (num == 0) {
                    num_str[num_len++] = '0';
                } else {
                    char temp[16];
                    int temp_len = 0;
                    while (num > 0) {
                        temp[temp_len++] = '0' + (num % 10);
                        num /= 10;
                    }
                    for (int i = temp_len - 1; i >= 0; i--) {
                        num_str[num_len++] = temp[i];
                    }
                }
                for (int i = 0; i < num_len && len < output_size - 1; i++) {
                    output[len++] = num_str[i];
                }
                output[len] = '\0';
            }
            return 1;
            
        case CMD_DEPART:
            {
                int len = 0;
                const char* cmd = "DEPART:";
                while (cmd[len] != '\0' && len < output_size - 1) {
                    output[len] = cmd[len];
                    len++;
                }
                int num = command->param1;
                char num_str[16];
                int num_len = 0;
                if (num == 0) {
                    num_str[num_len++] = '0';
                } else {
                    char temp[16];
                    int temp_len = 0;
                    while (num > 0) {
                        temp[temp_len++] = '0' + (num % 10);
                        num /= 10;
                    }
                    for (int i = temp_len - 1; i >= 0; i--) {
                        num_str[num_len++] = temp[i];
                    }
                }
                for (int i = 0; i < num_len && len < output_size - 1; i++) {
                    output[len++] = num_str[i];
                }
                output[len] = '\0';
            }
            return 1;
            
        case CMD_SYNOPSIS:
            {
                int len = 0;
                const char* cmd = "SYNOPSIS";
                while (cmd[len] != '\0' && len < output_size - 1) {
                    output[len] = cmd[len];
                    len++;
                }
                output[len] = '\0';
            }
            return 1;
            
        case CMD_EXIT:
            {
                int len = 0;
                const char* cmd = "EXIT";
                while (cmd[len] != '\0' && len < output_size - 1) {
                    output[len] = cmd[len];
                    len++;
                }
                output[len] = '\0';
            }
            return 1;
            
        default:
            return 0;
    }
}

void ui_display_error(const char* message) {
    if (message != NULL) {
        printf("ERROR: %s\n", message);
        fflush(stdout);
    }
}

void ui_display_success(const char* message) {
    if (message != NULL) {
        printf("SUCCESS: %s\n", message);
        fflush(stdout);
    }
}

void ui_display_info(const char* message) {
    if (message != NULL) {
        printf("INFO: %s\n", message);
        fflush(stdout);
    }
}

