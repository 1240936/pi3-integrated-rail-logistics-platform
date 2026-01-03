/*
 * UI Component - Main Entry Point
 * 
 * Purpose: Console-based user interface for Station Management System
 * 
 * This component provides a robust interface that:
 * - Handles malicious/error input gracefully
 * - Provides menu-driven navigation
 * - Validates all user input
 * - Communicates with Manager component via stdout/stdin
 */

#include <stdio.h>
#include "ui.h"

// Declare input_handler function
extern int read_input_line(char* buffer, int max_size);
extern int parse_integer(const char* str, int* value);

int main(void) {
    ui_init();
    
    // Handle login first - send LOGIN command to Manager
    // (Welcome message will appear after Manager initializes)
    fprintf(stderr, "=== User Login ===\n");
    fprintf(stderr, "Username: ");
    fflush(stderr);
    char username[64];
    if (read_input_line(username, sizeof(username)) <= 0) {
        fprintf(stderr, "ERROR: Failed to read username\n");
        ui_cleanup();
        return 1;
    }
    
    fprintf(stderr, "Password: ");
    fflush(stderr);
    char password[64];
    if (read_input_line(password, sizeof(password)) <= 0) {
        fprintf(stderr, "ERROR: Failed to read password\n");
        ui_cleanup();
        return 1;
    }
    
    // Format and send LOGIN command to Manager (stdout)
    printf("LOGIN:%s:%s\n", username, password);
    fflush(stdout);
    
    // Show welcome message after login command is sent
    // (Manager initialization messages will appear first)
    fprintf(stderr, "\nWelcome to the Station Management System\n");
    fprintf(stderr, "Type 'HELP' for available commands or use the menu numbers\n\n");
    
    ParsedCommand command;
    char formatted_command[MAX_COMMAND_LENGTH];
    int running = 1;
    
    while (running) {
        ui_show_main_menu();
        
        if (!ui_read_command(&command)) {
            if (command.type == CMD_INVALID) {
                ui_display_error("Invalid command. Type 'HELP' for assistance.");
            }
            continue;
        }
        
        // Handle commands that don't need parameters
        if (command.type == CMD_SYNOPSIS || command.type == CMD_GET_SENSOR_DATA) {
            if (ui_format_command(&command, formatted_command, sizeof(formatted_command))) {
                printf("%s\n", formatted_command);
                fflush(stdout);
            }
            continue;
        } else if (command.type == CMD_HELP) {
            ui_show_help();
            continue;
        } else if (command.type == CMD_EXIT) {
            fprintf(stderr, "Exiting...\n");
            running = 0;
            if (ui_format_command(&command, formatted_command, sizeof(formatted_command))) {
                printf("%s\n", formatted_command);
                fflush(stdout);
            }
            break;
        }
        
        // Handle commands that need parameters
        if (command.param1 == 0) {
            // Prompt for parameter (use stderr so it appears on terminal)
            if (command.type == CMD_ASSIGN_TRACK) {
                fprintf(stderr, "Enter Train ID: ");
            } else if (command.type == CMD_SET_MAINTENANCE ||
                       command.type == CMD_SET_FREE ||
                       command.type == CMD_DEPART) {
                fprintf(stderr, "Enter Track ID: ");
            }
            fflush(stderr);
            
            char param_input[MAX_PARAM_LENGTH];
            if (read_input_line(param_input, sizeof(param_input)) > 0) {
                if (!parse_integer(param_input, &command.param1)) {
                    ui_display_error("Invalid input. Please enter a number.");
                    continue;
                }
            } else {
                ui_display_error("No input provided.");
                continue;
            }
        }
        
        // Validate command
        if (!ui_validate_command(&command)) {
            continue;
        }
        
        // Format and send command to Manager
        if (ui_format_command(&command, formatted_command, sizeof(formatted_command))) {
            printf("%s\n", formatted_command);
            fflush(stdout);
        } else {
            ui_display_error("Failed to format command.");
        }
    }
    
    ui_cleanup();
    return 0;
}

