#include "logging.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

/* Internal helper: ensure log array has capacity */
static int ensure_log_capacity(ManagerData* manager_data)
{
    if (!manager_data)
        return -1;

    /* First-time allocation */
    if ((*manager_data).logs_capacity == 0) {
        (*manager_data).logs_capacity = 4;  /* initial capacity */
        (*manager_data).logs = malloc(sizeof(Log) * (*manager_data).logs_capacity);
        if (!(*manager_data).logs)
            return -1;
        return 0;
    }

    /* If full, grow capacity */
    if ((*manager_data).num_logs >= (*manager_data).logs_capacity) {
        int new_capacity = (*manager_data).logs_capacity * 2;
        Log* tmp = realloc((*manager_data).logs, sizeof(Log) * new_capacity);
        if (!tmp)
            return -1;

        (*manager_data).logs = tmp;
        (*manager_data).logs_capacity = new_capacity;
    }

    return 0;
}

/* Record a new action */
int record_action(ManagerData* manager_data,
                  const User* user,
                  const char* action)
{
    if (!manager_data || !user || !action)
        return -1;

    /* Ensure capacity before inserting */
    if (ensure_log_capacity(manager_data) != 0)
        return -1;

    Log* log = &((*manager_data).logs[(*manager_data).num_logs]);

    (*log).id = (*manager_data).next_log_id++;
    (*log).timestamp = time(NULL);

    strncpy((*log).user_identification,
            (*user).username,
            MAX_USERNAME_LENGTH - 1);
    (*log).user_identification[MAX_USERNAME_LENGTH - 1] = '\0';

    strncpy((*log).action,
            action,
            MAX_ACTION_LENGTH - 1);
    (*log).action[MAX_ACTION_LENGTH - 1] = '\0';

    (*manager_data).num_logs++;
    return 0;
}

/* Write ALL logs to a file */
int create_action_log_file(const char* filename,
                           Log* logs,
                           int num_logs)
{
    if (!filename || num_logs < 0)
        return -1;

    if (num_logs == 0)
        return 0;  /* nothing to write */

    if (!logs)
        return -1;

    FILE* fp = fopen(filename, "w");
    if (!fp)
        return -1;

    for (int i = 0; i < num_logs; i++) {
        fprintf(fp,
                "LOG:%d:%s:%s:%ld\n",
                (*(&logs[i])).id,
                (*(&logs[i])).user_identification,
                (*(&logs[i])).action,
                (long)(*(&logs[i])).timestamp);
    }

    fclose(fp);
    return 0;
}

/* USAC12 — Export logs for a specific user */
int export_user_logs(const char* filename,
                     Log* logs,
                     int num_logs,
                     const char* username)
{
    if (!filename || !username || num_logs < 0)
        return -1;

    if (num_logs == 0)
        return 0;

    if (!logs)
        return -1;

    FILE* fp = fopen(filename, "w");
    if (!fp)
        return -1;

    int count = 0;

    for (int i = 0; i < num_logs; i++) {
        if (strcmp((*(&logs[i])).user_identification, username) == 0) {
            fprintf(fp,
                    "LOG:%d:%s:%s:%ld\n",
                    (*(&logs[i])).id,
                    (*(&logs[i])).user_identification,
                    (*(&logs[i])).action,
                    (long)(*(&logs[i])).timestamp);
            count++;
        }
    }

    fclose(fp);
    return count;  /* 0 means no logs for that user */
}

/* Free all logs safely */
void free_logs(ManagerData* manager_data)
{
    if (!manager_data)
        return;

    free((*manager_data).logs);
    (*manager_data).logs = NULL;
    (*manager_data).num_logs = 0;
    (*manager_data).logs_capacity = 0;
}
