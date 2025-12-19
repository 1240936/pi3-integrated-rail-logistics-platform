/*
 * Moving Median Helper Functions Implementation
 */

#include <stddef.h>
#include "moving_median.h"
#include "circular_buffer.h"

// Get the last n values from circular buffer without removing them
int get_last_n_values(int* buffer, int length, int nelem, int tail, int head, int n, int* array) {
  if (buffer == NULL || array == NULL || nelem < n) {
    return 0;
  }
  
  // Start from the position that is n elements before head
  // We need to go backwards from head
  int start_pos = (head - n + length) % length;
  
  // Copy n values
  for (int i = 0; i < n; i++) {
    int pos = (start_pos + i) % length;
    array[i] = buffer[pos];
  }
  
  return 1;
}

// Calculate moving median from circular buffer
int calculate_moving_median(int* buffer, int length, int nelem, int tail, int head, int window_size, int* me) {
  if (buffer == NULL || me == NULL || nelem < window_size) {
    return 0;
  }
  
  // Get last window_size values
  int window[window_size];
  if (!get_last_n_values(buffer, length, nelem, tail, head, window_size, window)) {
    return 0;
  }
  
  // Calculate median
  return median(window, window_size, me);
}

