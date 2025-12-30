/*
 * Manager Component - USAC12 Test/Demo
 *
 * Purpose: Test logging functionality (recording user actions and writing to a file)
 *
 * Usage: ./manager_usac12.elf
 * Output: logs.txt and alice_logs.txt
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "logging.h"
#include "data_structures.h"

#define LOG_FILENAME_ALL   "logs.txt"
#define LOG_FILENAME_ALICE "alice_logs.txt"

int main() {
    printf("=== Manager Component - USAC12 (Action Logging) ===\n\n");

    /* Initialize manager data */
    ManagerData manager_data = {0};
    (*(&manager_data)).logs = NULL;
    (*(&manager_data)).num_logs = 0;
    (*(&manager_data)).logs_capacity = 0;
    (*(&manager_data)).next_log_id = 1;

    /* Create example users */
    User user1 = {
        "Alice Johnson",
        "alice",
        "ENCRYPTEDPASS",
        3
    };

    User user2 = {
        "Bob Smith",
        "bob",
        "ENCRYPTEDPASS",
        5
    };

    printf("Recording actions...\n");

    /* Record some actions */
    if (record_action(&manager_data, &user1, "login") == 0)
        printf("Action recorded: alice - login\n");

    if (record_action(&manager_data, &user1, "create_file") == 0)
        printf("Action recorded: alice - create_file\n");

    if (record_action(&manager_data, &user2, "login") == 0)
        printf("Action recorded: bob - login\n");

    if (record_action(&manager_data, &user2, "delete_file") == 0)
        printf("Action recorded: bob - delete_file\n");

    printf("\nWriting ALL logs to file: %s\n", LOG_FILENAME_ALL);
    if (create_action_log_file(LOG_FILENAME_ALL,
                               (*(&manager_data)).logs,
                               (*(&manager_data)).num_logs) == 0)
        printf("All logs written successfully\n");
    else
        printf("Failed to write logs\n");

    printf("\nExporting ONLY Alice's logs to: %s\n", LOG_FILENAME_ALICE);
    int count = export_user_logs(LOG_FILENAME_ALICE,
                                 (*(&manager_data)).logs,
                                 (*(&manager_data)).num_logs,
                                 "alice");

    if (count >= 0)
        printf("Exported %d logs for user 'alice'\n", count);
    else
        printf("Failed to export user logs\n");

    /* Free allocated memory */
    free_logs(&manager_data);

    printf("\n=== USAC12 Logging Test Complete ===\n");
    return 0;
}
