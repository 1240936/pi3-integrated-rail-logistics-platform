/*
 * Circular Buffer and Median Functions Implementation
 * C++ versions of Sprint 2 assembly functions for Arduino
 */

#include <stddef.h>
#include "circular_buffer.h"

// USAC05 - Enqueue value into circular buffer
int enqueue_value(int* buffer, int length, int *nelem, int* tail, int* head, int value) {
  if (buffer == NULL || nelem == NULL || tail == NULL || head == NULL) {
    return 0;
  }
  
  // If buffer is full, remove oldest element (overwrite)
  if (*nelem >= length) {
    buffer[*head] = value;
    *head = (*head + 1) % length;
    *tail = (*tail + 1) % length;
    return 1; // Buffer is full
  } else {
    // Add new element
    buffer[*head] = value;
    *head = (*head + 1) % length;
    (*nelem)++;
    return 0; // Buffer not full
  }
}

// USAC06 - Dequeue value from circular buffer
int dequeue_value(int* buffer, int length, int* nelem, int* tail, int* head, int *value) {
  if (buffer == NULL || nelem == NULL || tail == NULL || head == NULL || value == NULL) {
    return 0;
  }
  
  if (*nelem == 0) {
    return 0; // Buffer is empty
  }
  
  *value = buffer[*tail];
  *tail = (*tail + 1) % length;
  (*nelem)--;
  return 1;
}

// USAC07 - Move n elements from buffer to array
int move_n_to_array(int* buffer, int length, int* nelem, int* tail, int* head, int n, int* array) {
  if (buffer == NULL || nelem == NULL || tail == NULL || head == NULL || array == NULL) {
    return 0;
  }
  
  if (*nelem < n) {
    return 0; // Not enough elements
  }
  
  // Copy n elements starting from tail
  int current_tail = *tail;
  for (int i = 0; i < n; i++) {
    array[i] = buffer[current_tail];
    current_tail = (current_tail + 1) % length;
  }
  
  // Update buffer state (remove n elements)
  *tail = current_tail;
  *nelem -= n;
  
  return 1;
}

// USAC08 - Sort array (bubble sort)
int sort_array(int* vec, int length, char order) {
  if (vec == NULL || length <= 0) {
    return 0;
  }
  
  if (length == 1) {
    return 1;
  }
  
  // Bubble sort
  for (int i = 0; i < length - 1; i++) {
    for (int j = 0; j < length - i - 1; j++) {
      int swap = 0;
      if (order == 1) { // Ascending
        swap = (vec[j] > vec[j + 1]);
      } else { // Descending
        swap = (vec[j] < vec[j + 1]);
      }
      
      if (swap) {
        int temp = vec[j];
        vec[j] = vec[j + 1];
        vec[j + 1] = temp;
      }
    }
  }
  
  return 1;
}

// USAC09 - Calculate median
int median(int* vec, int length, int *me) {
  if (vec == NULL || me == NULL || length <= 0) {
    return 0;
  }
  
  // Create a copy to sort (don't modify original)
  int sorted[length];
  for (int i = 0; i < length; i++) {
    sorted[i] = vec[i];
  }
  
  // Sort ascending
  if (!sort_array(sorted, length, 1)) {
    return 0;
  }
  
  // Calculate median
  if (length % 2 == 0) {
    // Even number of elements: average of two middle values
    *me = (sorted[length / 2 - 1] + sorted[length / 2]) / 2;
  } else {
    // Odd number of elements: middle value
    *me = sorted[length / 2];
  }
  
  return 1;
}

