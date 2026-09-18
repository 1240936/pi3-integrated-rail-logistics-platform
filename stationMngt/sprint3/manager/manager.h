/*
 * Manager Component - Main Header
 * 
 * This header consolidates all manager component functionality
 * as described in the Sprint 3 architecture.
 */

#ifndef MANAGER_H
#define MANAGER_H

// Core data structures
#include "data_structures.h"

// Assembly function declarations
#include "asm.h"

// Initialization (USAC11)
// initialize_from_file() and free_manager_data() declared in data_structures.h

// User Authentication (Login)
#include "user_auth.h"

// Logging (USAC12)
#include "logging.h"

// Sensor Management (USAC13)
#include "sensor_manager.h"
#include "sensor_data.h"
#include "serial_comm.h"

// LightSigns Communication (USAC14)
#include "lightsigns_comm.h"

// Board Communication (USAC15)
#include "board_comm.h"

// Track Management (USAC16)
#include "track_manager.h"

// Helper functions
#include "helper_functions.h"

#endif // MANAGER_H

