/*
 * Circular Buffer and Median Functions for Arduino
 * 
 * These are C implementations of the RISC-V assembly functions
 * from Sprint 2, adapted for Arduino/ESP32 architecture
 */

#ifndef CIRCULAR_BUFFER_H
#define CIRCULAR_BUFFER_H

// USAC05 - Enqueue value into circular buffer
// Returns 1 if buffer is full after insertion, 0 otherwise
int enqueue_value(int* buffer, int length, int *nelem, int* tail, int* head, int value);

// USAC06 - Dequeue value from circular buffer
// Returns 1 if succeeds, 0 otherwise
int dequeue_value(int* buffer, int length, int* nelem, int* tail, int* head, int *value);

// USAC07 - Move n elements from buffer to array
// Returns 1 if succeeds, 0 otherwise
int move_n_to_array(int* buffer, int length, int* nelem, int* tail, int* head, int n, int* array);

// USAC08 - Sort array
// order: 1 for ascending, 0 for descending
// Returns 1 if succeeds, 0 otherwise
int sort_array(int* vec, int length, char order);

// USAC09 - Calculate median
// Returns 1 if succeeds, 0 otherwise
int median(int* vec, int length, int *me);

#endif

