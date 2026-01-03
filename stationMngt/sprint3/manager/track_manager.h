/*
 * Track Manager - USAC16
 *
 * Purpose: Manage railway tracks with coordination to Board and LightSigns components
 * 
 * This file implements USAC16: "As a User, I want to manage the railway tracks of a station."
 *
 * Features:
 * - Assign tracks to arriving trains (or emit emergency stop if no track available)
 * - Set tracks as nonoperational (maintenance)
 * - Set tracks as free
 * - Issue departure orders to stopped trains
 * - Coordinate with Board (USAC15) and LightSigns (USAC14) components for status updates
 */

#ifndef TRACK_MANAGER_H
#define TRACK_MANAGER_H

#include "data_structures.h"

// Track management operation results
typedef enum {
    TRACK_OP_SUCCESS = 0,
    TRACK_OP_NO_AVAILABLE_TRACK = -1,
    TRACK_OP_TRACK_NOT_FOUND = -2,
    TRACK_OP_TRACK_OCCUPIED = -3,
    TRACK_OP_TRACK_UNDER_MAINTENANCE = -4,
    TRACK_OP_INVALID_TRAIN = -5,
    TRACK_OP_BOARD_COMM_ERROR = -6,
    TRACK_OP_LIGHTSIGNS_COMM_ERROR = -7
} TrackOperationResult;

/**
 * Initialize track manager with communication components
 *
 * @param manager_data Pointer to initialized ManagerData structure
 * @param board_port Serial port for board communication
 * @param lightsigns_port Serial port for lightsigns communication
 * @return 0 on success, -1 on error
 */
int init_track_manager(ManagerData* manager_data, const char* board_port, const char* lightsigns_port);

/**
 * Assign a track to an arriving train
 *
 * Searches for an available track and assigns it to the train.
 * Updates Board and LightSigns components accordingly.
 * If no track is available, emits an emergency stop order.
 *
 * @param manager_data Pointer to ManagerData structure
 * @param train_id ID of the arriving train
 * @param assigned_track_id Output parameter for assigned track ID (0 if emergency stop)
 * @return TRACK_OP_SUCCESS if assigned, TRACK_OP_NO_AVAILABLE_TRACK if emergency stop needed
 */
TrackOperationResult assign_track_to_train(ManagerData* manager_data, int train_id, int* assigned_track_id);

/**
 * Set a track as nonoperational (maintenance mode)
 *
 * Only works if track is currently free. Sets track to MAINTENANCE state
 * and updates Board and LightSigns components.
 *
 * @param manager_data Pointer to ManagerData structure
 * @param track_id ID of track to set as maintenance
 * @return TRACK_OP_SUCCESS on success, error code on failure
 */
TrackOperationResult set_track_maintenance(ManagerData* manager_data, int track_id);

/**
 * Set a track as free
 *
 * Works if track is currently occupied or in maintenance. Sets track to FREE state,
 * removes train assignment, and updates Board and LightSigns components.
 *
 * @param manager_data Pointer to ManagerData structure
 * @param track_id ID of track to free
 * @return TRACK_OP_SUCCESS on success, error code on failure
 */
TrackOperationResult free_track(ManagerData* manager_data, int track_id);

/**
 * Issue departure order to a stopped train
 *
 * Only works if track is currently occupied. Issues departure command
 * and updates Board and LightSigns components.
 *
 * @param manager_data Pointer to ManagerData structure
 * @param track_id ID of track where train should depart
 * @return TRACK_OP_SUCCESS on success, error code on failure
 */
TrackOperationResult issue_departure_order(ManagerData* manager_data, int track_id);

/**
 * Get string representation of track operation result
 *
 * @param result TrackOperationResult enum value
 * @return String description of the result
 */
const char* get_track_operation_result_string(TrackOperationResult result);

/**
 * Send synopsis of all tracks to board
 */
void send_synopsis_to_board(ManagerData* data);

/**
 * Cleanup track manager resources
 */
void cleanup_track_manager();

/**
 * Update board with track status change
 *
 * @param track_id ID of the track
 * @param state New state of the track
 * @param train_id Train ID (0 if track is free)
 * @return TRACK_OP_SUCCESS on success, error code on failure
 */
TrackOperationResult update_board_track_status(int track_id, TrackState state, int train_id);

/**
 * Update lightsigns with track status change
 *
 * @param track_id ID of the track
 * @param state New state of the track
 * @return TRACK_OP_SUCCESS on success, error code on failure
 */
TrackOperationResult update_lightsigns_track_status(int track_id, TrackState state);

#endif // TRACK_MANAGER_H
