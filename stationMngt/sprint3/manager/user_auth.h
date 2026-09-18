/*
 * User Authentication - Manager Algorithm (Step 1)
 * 
 * Purpose: User login functionality using password decryption
 * 
 * This module implements the login step of the Manager algorithm.
 * Uses USAC02 decrypt_data assembly function to verify encrypted passwords.
 */

#ifndef USER_AUTH_H
#define USER_AUTH_H

#include "data_structures.h"

/**
 * User authentication (login)
 * 
 * @param manager_data Pointer to ManagerData structure containing users
 * @param username Username to authenticate
 * @param password Plain text password to verify
 * @return Pointer to authenticated User if successful, NULL otherwise
 */
const User* login(ManagerData* manager_data, const char* username, const char* password);

#endif // USER_AUTH_H

