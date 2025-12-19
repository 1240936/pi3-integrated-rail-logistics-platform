/*
 * Moving Median Helper Functions
 * 
 * Helper functions to properly implement Moving Median filter
 * without removing values from the circular buffer
 */

#ifndef MOVING_MEDIAN_H
#define MOVING_MEDIAN_H

// Get the last n values from circular buffer without removing them
// Copies values to array in chronological order (oldest first)
int get_last_n_values(int* buffer, int length, int nelem, int tail, int head, int n, int* array);

// Calculate moving median from circular buffer
// Returns median value in me parameter
int calculate_moving_median(int* buffer, int length, int nelem, int tail, int head, int window_size, int* me);

#endif

