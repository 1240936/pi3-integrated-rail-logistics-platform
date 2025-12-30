#ifndef LOGGING_H
#define LOGGING_H

#include "data_structures.h"

/*
 * USAC12
 * Logging system for recording and exporting user actions.
 */

/**
 * Records a new action performed by a user.
 *
 * @param manager_data Pointer to ManagerData structure
 * @param user Pointer to the User who performed the action
 * @param action Description of the action
 * @return 0 on success, -1 on failure
 */
int record_action(ManagerData* manager_data,
                  const User* user,
                  const char* action);

/**
 * Writes ALL log entries to a text file.
 * Format:
 *   LOG:id:user:action:timestamp
 *
 * @param filename Output file path
 * @param logs Array of Log entries
 * @param num_logs Number of logs in the array
 * @return 0 on success, -1 on failure
 */
int create_action_log_file(const char* filename,
                           Log* logs,
                           int num_logs);

/**
 * Writes ONLY the logs belonging to a specific user.
 * Required for USAC12.
 *
 * @param filename Output file path
 * @param logs Array of Log entries
 * @param num_logs Number of logs
 * @param username Username to filter by
 * @return Number of logs written, or -1 on error
 */
int export_user_logs(const char* filename,
                     Log* logs,
                     int num_logs,
                     const char* username);

/**
 * Frees all dynamically allocated log memory inside ManagerData.
 *
 * @param manager_data Pointer to ManagerData
 */
void free_logs(ManagerData* manager_data);

#endif
