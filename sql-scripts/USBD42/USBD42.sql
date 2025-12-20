-- ============================================================================
-- USBD42 - Register a train movement in the database
-- ============================================================================
-- As a Freight Manager, I want to register a train movement in the database.
-- If the train stopped at the facility, the departure time shall also be
-- registered.
--
-- Requirements:
-- 1. Register a train movement using the TrainEvent table (from USBD31)
-- 2. Record arrival time (required) - creates TrainEvent with eventType='ARRIVAL'
-- 3. Record departure time (optional, only if train stopped at facility) - creates TrainEvent with eventType='DEPARTURE'
-- 4. Validate that Train and Facility exist
-- 5. Validate that departure time is after arrival time (if provided)
-- 6. Proper transaction handling
--
-- Note: This implementation uses the TrainEvent table from USBD31.sql
-- ============================================================================

-- ============================================================================
-- Stored Procedure: RegisterTrainMovement
-- ============================================================================
-- Registers a train movement in the database using the TrainEvent table.
-- Creates one or two TrainEvent records:
--   - One ARRIVAL event (always created)
--   - One DEPARTURE event (only if train stopped, i.e., departure_time is provided)
-- 
-- Parameters:
--   p_train_id       - ID of the train
--   p_facility_id    - ID of the facility where the movement occurred
--   p_arrival_time   - Arrival time at the facility (required)
--   p_departure_time - Departure time from the facility (optional, NULL if train didn't stop)
--   p_route_id       - ID of the route (optional, can be NULL) - Note: TrainEvent doesn't have RouteID,
--                     so this parameter is kept for future compatibility but not stored
--
-- Returns:
--   Number of events created (1 if no departure, 2 if departure recorded)
--
-- Exceptions:
--   -20010: Train ID cannot be NULL
--   -20011: Facility ID cannot be NULL
--   -20012: Arrival time cannot be NULL
--   -20013: Train does not exist
--   -20014: Facility does not exist
--   -20016: Departure time must be after arrival time
-- ============================================================================

CREATE OR REPLACE PROCEDURE RegisterTrainMovement(
    p_train_id IN NUMBER,
    p_facility_id IN NUMBER,
    p_arrival_time IN DATE,
    p_departure_time IN DATE DEFAULT NULL,
    p_route_id IN NUMBER DEFAULT NULL
)
IS
    v_train_exists NUMBER;
    v_facility_exists NUMBER;
    v_arrival_event_id NUMBER;
    v_departure_event_id NUMBER;
    v_events_created NUMBER := 0;
    v_next_id NUMBER;
BEGIN
    -- Validate input parameters
    IF p_train_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20010, 'Train ID cannot be NULL');
    END IF;
    
    IF p_facility_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20011, 'Facility ID cannot be NULL');
    END IF;
    
    IF p_arrival_time IS NULL THEN
        RAISE_APPLICATION_ERROR(-20012, 'Arrival time cannot be NULL');
    END IF;
    
    -- Validate Train exists
    SELECT COUNT(*)
    INTO v_train_exists
    FROM Train
    WHERE ID = p_train_id;
    
    IF v_train_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20013, 
            'Cannot register train movement: Train with ID ' || p_train_id || ' does not exist.');
    END IF;
    
    -- Validate Facility exists
    SELECT COUNT(*)
    INTO v_facility_exists
    FROM Facility
    WHERE ID = p_facility_id;
    
    IF v_facility_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20014, 
            'Cannot register train movement: Facility with ID ' || p_facility_id || ' does not exist.');
    END IF;
    
    -- Validate departure time is after arrival time (if provided)
    IF p_departure_time IS NOT NULL THEN
        IF p_departure_time <= p_arrival_time THEN
            RAISE_APPLICATION_ERROR(-20016, 
                'Cannot register train movement: Departure time must be after arrival time.');
        END IF;
    END IF;
    
    -- Get next available ID for TrainEvent (get once, increment as needed)
    SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID) + 1 END
    INTO v_next_id
    FROM TrainEvent;
    
    -- Insert arrival event
    INSERT INTO TrainEvent (ID, FacilityID, TrainID, eventTime, eventType)
    VALUES (v_next_id, p_facility_id, p_train_id, p_arrival_time, 'ARRIVAL');
    
    v_arrival_event_id := v_next_id;
    v_events_created := 1;
    
    -- Insert departure event if train stopped (departure_time is provided)
    IF p_departure_time IS NOT NULL THEN
        -- Increment ID for departure event
        v_next_id := v_next_id + 1;
        
        INSERT INTO TrainEvent (ID, FacilityID, TrainID, eventTime, eventType)
        VALUES (v_next_id, p_facility_id, p_train_id, p_departure_time, 'DEPARTURE');
        
        v_departure_event_id := v_next_id;
        v_events_created := 2;
    END IF;
    
    -- Note: p_route_id parameter is accepted but not stored in TrainEvent table
    -- as TrainEvent table from USBD31 does not have a RouteID column
    
    -- Commit is handled by the caller
    
EXCEPTION
    WHEN OTHERS THEN
        -- Check if this is an application error (between -20000 and -20999)
        -- If so, let it propagate as-is without wrapping
        IF SQLCODE >= -20999 AND SQLCODE <= -20000 THEN
            RAISE;  -- Re-raise application errors as-is
        ELSE
            -- Wrap unexpected database errors
            RAISE_APPLICATION_ERROR(-20099, 
                'Error registering train movement: ' || SQLERRM);
        END IF;
END RegisterTrainMovement;
/

-- ============================================================================
-- USBD42 - Test Script for RegisterTrainMovement Procedure
-- ============================================================================
-- This script tests the RegisterTrainMovement procedure with various scenarios:
-- 1. Happy path (successful registration with arrival only)
-- 2. Happy path (successful registration with arrival and departure)
-- 3. Train doesn't exist
-- 4. Facility doesn't exist
-- 5. NULL parameters
-- 6. Departure time before arrival time
-- ============================================================================

-- ============================================================================
-- Test Case 1: Happy Path - Register movement with arrival only
-- ============================================================================

DECLARE
    v_train_id NUMBER;
    v_facility_id NUMBER;
    v_arrival_time DATE := SYSDATE;
    v_events_before NUMBER;
    v_events_after NUMBER;
BEGIN
    -- Get a valid Train
    SELECT ID
    INTO v_train_id
    FROM Train
    WHERE ROWNUM = 1;
    
    -- Get a valid Facility
    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;
    
    -- Count events before
    SELECT COUNT(*)
    INTO v_events_before
    FROM TrainEvent;
    
    DBMS_OUTPUT.PUT_LINE('Test 1: Registering train movement (arrival only)');
    DBMS_OUTPUT.PUT_LINE('  Train ID: ' || v_train_id);
    DBMS_OUTPUT.PUT_LINE('  Facility ID: ' || v_facility_id);
    DBMS_OUTPUT.PUT_LINE('  Arrival Time: ' || TO_CHAR(v_arrival_time, 'YYYY-MM-DD HH24:MI:SS'));
    
    RegisterTrainMovement(
        p_train_id => v_train_id,
        p_facility_id => v_facility_id,
        p_arrival_time => v_arrival_time,
        p_departure_time => NULL,
        p_route_id => NULL
    );
    
    -- Count events after
    SELECT COUNT(*)
    INTO v_events_after
    FROM TrainEvent;
    
    IF v_events_after = v_events_before + 1 THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Train movement registered successfully! (1 event created)');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Expected 1 event, but ' || (v_events_after - v_events_before) || ' events were created.');
    END IF;
    
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 2: Happy Path - Register movement with arrival and departure
-- ============================================================================

DECLARE
    v_train_id NUMBER;
    v_facility_id NUMBER;
    v_arrival_time DATE := SYSDATE;
    v_departure_time DATE := SYSDATE + INTERVAL '1' HOUR;
    v_events_before NUMBER;
    v_events_after NUMBER;
BEGIN
    -- Get a valid Train
    SELECT ID
    INTO v_train_id
    FROM Train
    WHERE ROWNUM = 1;
    
    -- Get a valid Facility
    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;
    
    -- Count events before
    SELECT COUNT(*)
    INTO v_events_before
    FROM TrainEvent;
    
    DBMS_OUTPUT.PUT_LINE('Test 2: Registering train movement (arrival and departure)');
    DBMS_OUTPUT.PUT_LINE('  Train ID: ' || v_train_id);
    DBMS_OUTPUT.PUT_LINE('  Facility ID: ' || v_facility_id);
    DBMS_OUTPUT.PUT_LINE('  Arrival Time: ' || TO_CHAR(v_arrival_time, 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('  Departure Time: ' || TO_CHAR(v_departure_time, 'YYYY-MM-DD HH24:MI:SS'));
    
    RegisterTrainMovement(
        p_train_id => v_train_id,
        p_facility_id => v_facility_id,
        p_arrival_time => v_arrival_time,
        p_departure_time => v_departure_time,
        p_route_id => NULL
    );
    
    -- Count events after
    SELECT COUNT(*)
    INTO v_events_after
    FROM TrainEvent;
    
    IF v_events_after = v_events_before + 2 THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Train movement registered successfully! (2 events created)');
        
        -- Verify event types
        DECLARE
            v_arrival_count NUMBER;
            v_departure_count NUMBER;
        BEGIN
            SELECT COUNT(*)
            INTO v_arrival_count
            FROM TrainEvent
            WHERE TrainID = v_train_id
            AND FacilityID = v_facility_id
            AND eventType = 'ARRIVAL'
            AND eventTime = v_arrival_time;
            
            SELECT COUNT(*)
            INTO v_departure_count
            FROM TrainEvent
            WHERE TrainID = v_train_id
            AND FacilityID = v_facility_id
            AND eventType = 'DEPARTURE'
            AND eventTime = v_departure_time;
            
            IF v_arrival_count = 1 AND v_departure_count = 1 THEN
                DBMS_OUTPUT.PUT_LINE('  Verified: ARRIVAL and DEPARTURE events created correctly.');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  WARNING: Event types may not be correct.');
            END IF;
        END;
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Expected 2 events, but ' || (v_events_after - v_events_before) || ' events were created.');
    END IF;
    
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 3: Train doesn't exist
-- ============================================================================

DECLARE
    v_facility_id NUMBER;
    v_arrival_time DATE := SYSDATE;
BEGIN
    -- Get a valid Facility
    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Test 3: Registering movement with non-existent Train');
    
    RegisterTrainMovement(
        p_train_id => 999999,
        p_facility_id => v_facility_id,
        p_arrival_time => v_arrival_time,
        p_departure_time => NULL,
        p_route_id => NULL
    );
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Check for the expected application error code
        -- Note: SQLCODE for application errors is negative (e.g., -20013)
        IF SQLCODE = -20013 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent Train.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSIF SQLCODE = -20099 THEN
            -- If we get -20099, it means the error was wrapped, check the message
            IF SQLERRM LIKE '%20013%' OR SQLERRM LIKE '%Train with ID 999999%' THEN
                DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent Train (wrapped error).');
                DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
            ELSE
                DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
            END IF;
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: SQLCODE=' || SQLCODE || ', ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 4: Facility doesn't exist
-- ============================================================================

DECLARE
    v_train_id NUMBER;
    v_arrival_time DATE := SYSDATE;
BEGIN
    -- Get a valid Train
    SELECT ID
    INTO v_train_id
    FROM Train
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Test 4: Registering movement with non-existent Facility');
    
    RegisterTrainMovement(
        p_train_id => v_train_id,
        p_facility_id => 999999,
        p_arrival_time => v_arrival_time,
        p_departure_time => NULL,
        p_route_id => NULL
    );
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20014 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent Facility.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 5: NULL parameters
-- ============================================================================

-- Test NULL Train ID
DECLARE
    v_facility_id NUMBER;
    v_arrival_time DATE := SYSDATE;
BEGIN
    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Test 5a: Testing NULL Train ID');
    RegisterTrainMovement(
        p_train_id => NULL,
        p_facility_id => v_facility_id,
        p_arrival_time => v_arrival_time,
        p_departure_time => NULL,
        p_route_id => NULL
    );
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception!');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20010 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Train ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- Test NULL Facility ID
DECLARE
    v_train_id NUMBER;
    v_arrival_time DATE := SYSDATE;
BEGIN
    SELECT ID
    INTO v_train_id
    FROM Train
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Test 5b: Testing NULL Facility ID');
    RegisterTrainMovement(
        p_train_id => v_train_id,
        p_facility_id => NULL,
        p_arrival_time => v_arrival_time,
        p_departure_time => NULL,
        p_route_id => NULL
    );
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception!');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20011 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Facility ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- Test NULL Arrival Time
DECLARE
    v_train_id NUMBER;
    v_facility_id NUMBER;
BEGIN
    SELECT ID
    INTO v_train_id
    FROM Train
    WHERE ROWNUM = 1;
    
    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Test 5c: Testing NULL Arrival Time');
    RegisterTrainMovement(
        p_train_id => v_train_id,
        p_facility_id => v_facility_id,
        p_arrival_time => NULL,
        p_departure_time => NULL,
        p_route_id => NULL
    );
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception!');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20012 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Arrival Time.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 6: Departure time before arrival time
-- ============================================================================

DECLARE
    v_train_id NUMBER;
    v_facility_id NUMBER;
    v_arrival_time DATE := SYSDATE;
    v_departure_time DATE := SYSDATE - INTERVAL '1' HOUR;
BEGIN
    -- Get a valid Train
    SELECT ID
    INTO v_train_id
    FROM Train
    WHERE ROWNUM = 1;
    
    -- Get a valid Facility
    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Test 6: Registering movement with departure before arrival');
    DBMS_OUTPUT.PUT_LINE('  Arrival Time: ' || TO_CHAR(v_arrival_time, 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('  Departure Time: ' || TO_CHAR(v_departure_time, 'YYYY-MM-DD HH24:MI:SS'));
    
    RegisterTrainMovement(
        p_train_id => v_train_id,
        p_facility_id => v_facility_id,
        p_arrival_time => v_arrival_time,
        p_departure_time => v_departure_time,
        p_route_id => NULL
    );
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20016 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected departure before arrival.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 7: Verify event types are stored correctly
-- ============================================================================

DECLARE
    v_train_id NUMBER;
    v_facility_id NUMBER;
    v_arrival_time DATE := SYSDATE;
    v_departure_time DATE := SYSDATE + INTERVAL '30' MINUTE;
    v_arrival_events NUMBER;
    v_departure_events NUMBER;
BEGIN
    -- Get a valid Train
    SELECT ID
    INTO v_train_id
    FROM Train
    WHERE ROWNUM = 1;
    
    -- Get a valid Facility
    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Test 7: Verifying event types are stored correctly');
    
    RegisterTrainMovement(
        p_train_id => v_train_id,
        p_facility_id => v_facility_id,
        p_arrival_time => v_arrival_time,
        p_departure_time => v_departure_time,
        p_route_id => NULL
    );
    
    -- Verify arrival event
    SELECT COUNT(*)
    INTO v_arrival_events
    FROM TrainEvent
    WHERE TrainID = v_train_id
    AND FacilityID = v_facility_id
    AND eventType = 'ARRIVAL'
    AND eventTime = v_arrival_time;
    
    -- Verify departure event
    SELECT COUNT(*)
    INTO v_departure_events
    FROM TrainEvent
    WHERE TrainID = v_train_id
    AND FacilityID = v_facility_id
    AND eventType = 'DEPARTURE'
    AND eventTime = v_departure_time;
    
    IF v_arrival_events = 1 AND v_departure_events = 1 THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Event types stored correctly!');
        DBMS_OUTPUT.PUT_LINE('  ARRIVAL events: ' || v_arrival_events);
        DBMS_OUTPUT.PUT_LINE('  DEPARTURE events: ' || v_departure_events);
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Event types not stored correctly.');
        DBMS_OUTPUT.PUT_LINE('  ARRIVAL events: ' || v_arrival_events || ' (expected 1)');
        DBMS_OUTPUT.PUT_LINE('  DEPARTURE events: ' || v_departure_events || ' (expected 1)');
    END IF;
    
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

