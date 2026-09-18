/*
 * User Authentication Implementation - Manager Algorithm (Step 1)
 * 
 * Purpose: Login functionality using password decryption
 * 
 * This module implements the login step of the Manager algorithm.
 * Uses USAC02 decrypt_data assembly function to verify encrypted passwords.
 */

#include "user_auth.h"
#include "asm.h"
#include "helper_functions.h"

/**
 * Helper function to compare strings (custom implementation, avoiding string.h)
 */
static int str_compare_custom(const char* s1, const char* s2) {
    if (s1 == NULL || s2 == NULL) return -1;
    while (*s1 && (*s1 == *s2)) {
        s1++;
        s2++;
    }
    return *(const unsigned char*)s1 - *(const unsigned char*)s2;
}

/**
 * Helper function to get string length (custom implementation)
 */
static int str_length_custom(const char* str) {
    if (str == NULL) return 0;
    int len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

/**
 * Convert password to uppercase (A-Z only, as required by decrypt_data)
 */
static void normalize_password(char* password) {
    if (password == NULL) return;
    int i = 0;
    while (password[i] != '\0') {
        if (password[i] >= 'a' && password[i] <= 'z') {
            password[i] = password[i] - 'a' + 'A';
        }
        // Remove non-letter characters
        if (password[i] < 'A' || password[i] > 'Z') {
            password[i] = 'X';  // Replace with placeholder
        }
        i++;
    }
}

/**
 * Login user with username and password
 * Uses decrypt_data assembly function (USAC02) to verify encrypted password
 */
const User* login(ManagerData* manager_data, const char* username, const char* password) {
    if (manager_data == NULL || username == NULL || password == NULL) {
        return NULL;
    }

    // Normalize input password to uppercase A-Z only
    char normalized_password[MAX_PASSWORD_LENGTH];
    int pwd_len = str_length_custom(password);
    if (pwd_len >= MAX_PASSWORD_LENGTH) {
        return NULL;  // Password too long
    }
    
    // Copy and normalize password
    for (int i = 0; i < pwd_len; i++) {
        normalized_password[i] = password[i];
    }
    normalized_password[pwd_len] = '\0';
    normalize_password(normalized_password);

    // Search for user by username
    for (int i = 0; i < manager_data->num_users; i++) {
        const User* user = &manager_data->users[i];
        
        // Compare username (case-sensitive)
        if (str_compare_custom(user->username, username) == 0) {
            // Username matches, verify password
            // Decrypt stored password using user's Caesar key
            char decrypted_password[MAX_PASSWORD_LENGTH];
            
            // Use assembly function decrypt_data (USAC02)
            if (decrypt_data((char*)user->password, user->caesar_key, decrypted_password) == 1) {
                // Compare decrypted password with provided password
                if (str_compare_custom(decrypted_password, normalized_password) == 0) {
                    // Password matches, login successful
                    return user;
                }
            }
            // If password doesn't match or decryption failed, return NULL
            return NULL;
        }
    }

    // User not found
    return NULL;
}

