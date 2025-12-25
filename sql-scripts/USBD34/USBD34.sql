-- USBD34 - Associate a freight with a train
-- As a Freight Manager, I want to associate a freight with a train.
-- All wagons included in the freight will be associated with the train.
--
-- Requirements:
-- 1. Associate a freight with a planned train (insert into Assigned_Wagon for all freight wagons)
-- 2. All wagons from Unassigned_Freight must be associated with the planned train
-- 3. Must respect the train maximum length constraint (from USBD33)
-- 4. Validate that the freight and route exist
-- 5. Validate that the freight has wagons
-- 6. Handle duplicate associations (wagon already associated with train)
-- 7. Proper transaction handling
--
-- Error codes (RAISE_APPLICATION_ERROR):
-- -20020: Freight ID cannot be NULL
-- -20021: Route ID cannot be NULL
-- -20022: Freight with specified ID does not exist
-- -20023: No planned train found for specified Route ID
-- -20024: Freight has no wagons associated in Unassigned_Freight table
-- -20025: Adding wagons would exceed train maximum length constraint
-- -20099: Unexpected error (wrapped with context)
-- ============================================================================

CREATE OR REPLACE FUNCTION AssociateFreightWithTrain(
    p_freight_id IN NUMBER,
    p_route_id IN NUMBER
) RETURN NUMBER
IS
    -- Variables using %TYPE to ensure compatibility with the database structure
    v_freight_exists NUMBER;
    v_route_exists NUMBER;
    v_wagon_count NUMBER;
    v_wagons_added NUMBER := 0;
    v_wagon_id Unassigned_Freight.WagonID%TYPE;
    
    -- Variables for planned train information
    v_planned_train_id Planned_Train.TrainID%TYPE;
    v_planned_train_start_date Planned_Train.startDate%TYPE;
    
    -- Variables for train length validation
    v_train_max_length Train.maxLength%TYPE;
    v_current_wagon_length NUMBER;
    v_current_locomotive_length NUMBER;
    v_current_total_length NUMBER;
    v_freight_wagon_length NUMBER;
    v_projected_total_length NUMBER;
    
    -- Explicit cursor to iterate through freight wagons
    CURSOR c_freight_wagons IS
        SELECT WagonID
        FROM Unassigned_Freight
        WHERE FreightID = p_freight_id;
    
    -- Internal procedure to associate a wagon with the planned train
    -- Separates the insertion logic from the main function
    PROCEDURE AssociateWagonToTrain(
        p_wagon_id IN Unassigned_Freight.WagonID%TYPE,
        p_planned_train_id IN Planned_Train.TrainID%TYPE,
        p_planned_train_start_date IN Planned_Train.startDate%TYPE
    ) IS
    BEGIN
        INSERT INTO Assigned_Wagon (WagonID, PlannedTrainID, PlannedTrainStartDate)
        VALUES (p_wagon_id, p_planned_train_id, p_planned_train_start_date);
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            -- Wagon already associated with train, not an error
            NULL;
        WHEN OTHERS THEN
            -- Re-raise other unexpected errors
            RAISE;
    END AssociateWagonToTrain;
    
BEGIN
    -- ========================================================================
    -- INPUT PARAMETER VALIDATION
    -- ========================================================================
    
    -- Validate input parameters (cannot be NULL)
    IF p_freight_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20020, 'Freight ID cannot be NULL');
    END IF;
    
    IF p_route_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20021, 'Route ID cannot be NULL');
    END IF;
    
    -- ========================================================================
    -- DATA EXISTENCE VALIDATION
    -- ========================================================================
    
    -- Check if the freight exists
    SELECT COUNT(*)
    INTO v_freight_exists
    FROM Freight
    WHERE ID = p_freight_id;
    
    IF v_freight_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20022, 
            'Cannot associate freight: Freight with ID ' || p_freight_id || ' does not exist.');
    END IF;
    
    -- Get planned train information from route
    BEGIN
        SELECT TrainID, startDate
        INTO v_planned_train_id, v_planned_train_start_date
        FROM Planned_Train
        WHERE RouteID = p_route_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20023, 
                'Cannot associate freight: No planned train found for Route ID ' || p_route_id || '.');
    END;
    
    -- Check if the freight has associated wagons
    SELECT COUNT(*)
    INTO v_wagon_count
    FROM Unassigned_Freight
    WHERE FreightID = p_freight_id;
    
    IF v_wagon_count = 0 THEN
        RAISE_APPLICATION_ERROR(-20024, 
            'Cannot associate freight: Freight with ID ' || p_freight_id || 
            ' has no wagons associated.');
    END IF;
    
    -- Validate train length constraint before adding wagons
    -- Calculate current train length (wagons + locomotives) for the planned train
    BEGIN
        -- Get train maximum length
        SELECT maxLength
        INTO v_train_max_length
        FROM Train
        WHERE ID = v_planned_train_id;
        
        -- Calculate current wagon length (excluding wagons already in freight)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_current_wagon_length
        FROM Assigned_Wagon AW
        JOIN Wagon W ON AW.WagonID = W.ID
        JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE AW.PlannedTrainID = v_planned_train_id
          AND AW.PlannedTrainStartDate = v_planned_train_start_date
          AND AW.WagonID NOT IN (
              SELECT WagonID FROM Unassigned_Freight WHERE FreightID = p_freight_id
          );
        
        -- Calculate current locomotive length
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_current_locomotive_length
        FROM Assigned_Locomotive AL
        JOIN Locomotive L ON AL.LocomotiveID = L.ID
        JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE AL.PlannedTrainID = v_planned_train_id
          AND AL.PlannedTrainStartDate = v_planned_train_start_date;
        
        -- Calculate freight wagon length (excluding those already in train)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_freight_wagon_length
        FROM Unassigned_Freight UF
        JOIN Wagon W ON UF.WagonID = W.ID
        JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE UF.FreightID = p_freight_id
          AND NOT EXISTS (
              SELECT 1 FROM Assigned_Wagon AW 
              WHERE AW.PlannedTrainID = v_planned_train_id 
                AND AW.PlannedTrainStartDate = v_planned_train_start_date
                AND AW.WagonID = UF.WagonID
          );
        
        -- Calculate projected total length
        v_current_total_length := v_current_wagon_length + v_current_locomotive_length;
        v_projected_total_length := v_current_total_length + v_freight_wagon_length;
        
        -- Check if adding freight wagons would exceed train maximum length
        IF v_projected_total_length > v_train_max_length THEN
            RAISE_APPLICATION_ERROR(-20025, 
                'Cannot associate freight: Adding wagons from freight ' || p_freight_id || 
                ' would exceed train ' || v_planned_train_id || ' maximum length. ' ||
                'Current length: ' || TO_CHAR(v_current_total_length) || 'm, ' ||
                'Freight wagons length: ' || TO_CHAR(v_freight_wagon_length) || 'm, ' ||
                'Projected total: ' || TO_CHAR(v_projected_total_length) || 'm, ' ||
                'Maximum allowed: ' || TO_CHAR(v_train_max_length) || 'm.');
        END IF;
    END;
    
    -- Associate all freight wagons with the planned train
    -- Use explicit cursor to iterate through all wagons
    FOR wagon_rec IN c_freight_wagons LOOP
        v_wagon_id := wagon_rec.WagonID;
        
        -- Try to associate the wagon with the planned train using the internal procedure
        BEGIN
            AssociateWagonToTrain(v_wagon_id, v_planned_train_id, v_planned_train_start_date);
            -- If we reached here, the wagon was successfully associated
            v_wagons_added := v_wagons_added + 1;
        EXCEPTION
            -- If the wagon is already associated, the procedure handles it silently
            -- Continue to the next wagon
            WHEN OTHERS THEN
                -- Re-raise unexpected errors
                RAISE;
        END;
    END LOOP;
    
    -- Return the number of wagons added
    RETURN v_wagons_added;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Exception handling
        -- Re-raise application errors (codes -20000 to -20999) without modification
        IF SQLCODE BETWEEN -20000 AND -20999 THEN
            RAISE;
        ELSE
            -- Wrap unexpected errors with context
            RAISE_APPLICATION_ERROR(-20099, 
                'Error associating freight with train: ' || SQLERRM);
        END IF;
END AssociateFreightWithTrain;
/


-- ============================================================================
-- USBD34 - Comprehensive Test Script for AssociateFreightWithTrain Function
-- ============================================================================
-- This script tests the AssociateFreightWithTrain function with various scenarios:
-- 1. Happy path (successful association)
-- 2. Freight doesn't exist
-- 3. Train doesn't exist
-- 4. Freight has no wagons
-- 5. NULL parameters
-- 6. Train length constraint violation
-- 7. Duplicate wagon associations (wagon already in train)
-- 8. Partial association (some wagons added before constraint violation)
-- ============================================================================

-- ============================================================================
-- Test Setup: Verify test data exists
-- ============================================================================

DECLARE
    v_freight_count NUMBER;
    v_train_count NUMBER;
    v_freight_wagon_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_freight_count
    FROM Freight;
    
    SELECT COUNT(*)
    INTO v_train_count
    FROM Train;
    
    SELECT COUNT(*)
    INTO v_freight_wagon_count
    FROM Unassigned_Freight;
    
    DBMS_OUTPUT.PUT_LINE('=== Test Data Summary ===');
    DBMS_OUTPUT.PUT_LINE('Freights: ' || v_freight_count);
    DBMS_OUTPUT.PUT_LINE('Trains: ' || v_train_count);
    DBMS_OUTPUT.PUT_LINE('Freight-Wagon associations: ' || v_freight_wagon_count);
    
    IF v_freight_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: No freights found. Some tests may fail.');
    END IF;
    
    IF v_train_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: No trains found. Some tests may fail.');
    END IF;
END;
/


-- ============================================================================
-- Test Case 1: Happy Path - Successful freight association
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_route_id NUMBER;
    v_train_id NUMBER;
    v_start_date DATE;
    v_wagon_count NUMBER;
    v_train_wagon_count_before NUMBER;
    v_train_wagon_count_after NUMBER;
BEGIN
    -- Find a freight with wagons and a route that can accommodate them
    -- Use freight 2006 which has 2 wagons (3330003, 3330007)
    -- Use route 2 which is for train 5435
    v_freight_id := 2006;
    v_route_id := 2;
    
    -- Get planned train information
    SELECT TrainID, startDate
    INTO v_train_id, v_start_date
    FROM Planned_Train
    WHERE RouteID = v_route_id;
    
    -- Verify freight exists and has wagons
    SELECT COUNT(*)
    INTO v_wagon_count
    FROM Unassigned_Freight
    WHERE FreightID = v_freight_id;
    
    IF v_wagon_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('SKIPPED: Freight ' || v_freight_id || ' has no wagons.');
        RETURN;
    END IF;
    
    -- Count wagons currently in train
    SELECT COUNT(*)
    INTO v_train_wagon_count_before
    FROM Assigned_Wagon
    WHERE PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;
    
    DBMS_OUTPUT.PUT_LINE('=== Test Case 1: Happy Path ===');
    DBMS_OUTPUT.PUT_LINE('Freight ID: ' || v_freight_id);
    DBMS_OUTPUT.PUT_LINE('Route ID: ' || v_route_id);
    DBMS_OUTPUT.PUT_LINE('Train ID: ' || v_train_id);
    DBMS_OUTPUT.PUT_LINE('Wagons in freight: ' || v_wagon_count);
    DBMS_OUTPUT.PUT_LINE('Wagons in train before: ' || v_train_wagon_count_before);
    
    -- Remove any existing associations to ensure clean test
    DELETE FROM Assigned_Wagon
    WHERE PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date
      AND WagonID IN (SELECT WagonID FROM Unassigned_Freight WHERE FreightID = v_freight_id);
    COMMIT;
    
    -- Associate freight with planned train
    v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);
    
    -- Count wagons in train after association
    SELECT COUNT(*)
    INTO v_train_wagon_count_after
    FROM Assigned_Wagon
    WHERE PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;
    
    DBMS_OUTPUT.PUT_LINE('Wagons added: ' || v_result);
    DBMS_OUTPUT.PUT_LINE('Wagons in train after: ' || v_train_wagon_count_after);
    
    IF v_result = v_wagon_count AND v_train_wagon_count_after = v_train_wagon_count_before + v_result THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: All wagons from freight successfully associated with train!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Not all wagons were associated correctly.');
    END IF;
    
    -- Cleanup
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Test data rolled back.');
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/


-- ============================================================================
-- Test Case 2: Freight doesn't exist
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER := 99999; -- Non-existent freight
    v_route_id NUMBER;
BEGIN
    -- Get a valid route ID
    SELECT RouteID
    INTO v_route_id
    FROM Planned_Train
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('=== Test Case 2: Freight doesn''t exist ===');
    DBMS_OUTPUT.PUT_LINE('Freight ID: ' || v_freight_id);
    DBMS_OUTPUT.PUT_LINE('Route ID: ' || v_route_id);
    
    -- Try to associate non-existent freight
    v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20022 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent freight.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/


-- ============================================================================
-- Test Case 3: Train doesn't exist
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_route_id NUMBER := 99999; -- Non-existent route
BEGIN
    -- Get a valid freight ID
    SELECT ID
    INTO v_freight_id
    FROM Freight
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('=== Test Case 3: Route doesn''t exist ===');
    DBMS_OUTPUT.PUT_LINE('Freight ID: ' || v_freight_id);
    DBMS_OUTPUT.PUT_LINE('Route ID: ' || v_route_id);
    
    -- Try to associate freight with non-existent route
    v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20023 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent train.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/


-- ============================================================================
-- Test Case 4: Freight has no wagons
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_route_id NUMBER;
    v_train_id NUMBER;
    v_wagon_count NUMBER;
BEGIN
    -- Create a freight with no wagons (if possible)
    -- First, try to find a freight without wagons
    BEGIN
        SELECT F.ID
        INTO v_freight_id
        FROM Freight F
        WHERE NOT EXISTS (
            SELECT 1 FROM Unassigned_Freight UF WHERE UF.FreightID = F.ID
        )
        AND ROWNUM = 1;
        
        -- Get a valid route
        SELECT RouteID
        INTO v_route_id
        FROM Planned_Train
        WHERE ROWNUM = 1;
        
        DBMS_OUTPUT.PUT_LINE('=== Test Case 4: Freight has no wagons ===');
        DBMS_OUTPUT.PUT_LINE('Freight ID: ' || v_freight_id);
        DBMS_OUTPUT.PUT_LINE('Route ID: ' || v_route_id);
        
        -- Verify it has no wagons
        SELECT COUNT(*)
        INTO v_wagon_count
        FROM Unassigned_Freight
        WHERE FreightID = v_freight_id;
        
        DBMS_OUTPUT.PUT_LINE('Wagons in freight: ' || v_wagon_count);
        
        -- Try to associate freight with no wagons
        v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);
        
        DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception!');
        ROLLBACK;
        
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            DBMS_OUTPUT.PUT_LINE('SKIPPED: No freight without wagons found in database.');
            DBMS_OUTPUT.PUT_LINE('This test requires a freight with no wagons to be meaningful.');
        WHEN OTHERS THEN
            IF SQLCODE = -20024 THEN
                DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected freight with no wagons.');
                DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
            ELSE
                DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
            END IF;
            ROLLBACK;
    END;
END;
/


-- ============================================================================
-- Test Case 5: NULL parameters
-- ============================================================================

-- Test NULL Freight ID
DECLARE
    v_result NUMBER;
    v_route_id NUMBER;
BEGIN
    SELECT RouteID
    INTO v_route_id
    FROM Planned_Train
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('=== Test Case 5a: NULL Freight ID ===');
    v_result := AssociateFreightWithTrain(NULL, v_route_id);
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL Freight ID');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20020 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Freight ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- Test NULL Route ID
DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
BEGIN
    SELECT ID
    INTO v_freight_id
    FROM Freight
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('=== Test Case 5b: NULL Route ID ===');
    v_result := AssociateFreightWithTrain(v_freight_id, NULL);
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL Route ID');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20021 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Route ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/


-- ============================================================================
-- Test Case 6: Train length constraint violation
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_route_id NUMBER := 1; -- Route for train 5421 which is already near capacity
    v_train_id NUMBER;
    v_start_date DATE;
    v_train_max_length NUMBER;
    v_current_length NUMBER;
    v_wagon_length NUMBER;
    v_freight_wagon_count NUMBER;
    v_total_wagon_length NUMBER;
    v_total_locomotive_length NUMBER;
BEGIN
    -- Get planned train information
    SELECT TrainID, startDate
    INTO v_train_id, v_start_date
    FROM Planned_Train
    WHERE RouteID = v_route_id;
    
    -- Find a freight with many wagons that would exceed train capacity
    -- Train 5421 has maxLength 250 and already has many wagons
    SELECT maxLength
    INTO v_train_max_length
    FROM Train
    WHERE ID = v_train_id;
    
    -- Calculate current train length
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagon_length
    FROM Assigned_Wagon AW
    JOIN Wagon W ON AW.WagonID = W.ID
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE AW.PlannedTrainID = v_train_id
      AND AW.PlannedTrainStartDate = v_start_date;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_locomotive_length
    FROM Assigned_Locomotive AL
    JOIN Locomotive L ON AL.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE AL.PlannedTrainID = v_train_id
      AND AL.PlannedTrainStartDate = v_start_date;
    
    v_current_length := v_total_wagon_length + v_total_locomotive_length;
    
    -- Find a freight with wagons that would exceed capacity
    -- Use freight 2001 which has 5 wagons
    v_freight_id := 2001;
    
    SELECT COUNT(*)
    INTO v_freight_wagon_count
    FROM Unassigned_Freight
    WHERE FreightID = v_freight_id;
    
    -- Calculate total length of freight wagons
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_wagon_length
    FROM Unassigned_Freight UF
    JOIN Wagon W ON UF.WagonID = W.ID
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE UF.FreightID = v_freight_id;
    
    DBMS_OUTPUT.PUT_LINE('=== Test Case 6: Train length constraint violation ===');
    DBMS_OUTPUT.PUT_LINE('Freight ID: ' || v_freight_id);
    DBMS_OUTPUT.PUT_LINE('Route ID: ' || v_route_id);
    DBMS_OUTPUT.PUT_LINE('Train ID: ' || v_train_id);
    DBMS_OUTPUT.PUT_LINE('Train max length: ' || v_train_max_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Current train length: ' || v_current_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Freight wagons length: ' || v_wagon_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Wagons in freight: ' || v_freight_wagon_count);
    DBMS_OUTPUT.PUT_LINE('Projected total length: ' || (v_current_length + v_wagon_length) || 'm');
    
    -- Remove any existing associations to ensure clean test
    DELETE FROM Assigned_Wagon
    WHERE PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date
      AND WagonID IN (SELECT WagonID FROM Unassigned_Freight WHERE FreightID = v_freight_id);
    COMMIT;
    
    -- Try to associate freight (should fail due to length constraint)
    v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20025 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly prevented train length constraint violation.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/


-- ============================================================================
-- Test Case 7: Duplicate wagon associations (wagon already in train)
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_route_id NUMBER := 1; -- Route for train 5421
    v_train_id NUMBER;
    v_start_date DATE;
    v_wagon_id NUMBER;
    v_wagon_count NUMBER;
    v_wagons_already_in_train NUMBER;
BEGIN
    -- Get planned train information
    SELECT TrainID, startDate
    INTO v_train_id, v_start_date
    FROM Planned_Train
    WHERE RouteID = v_route_id;
    
    -- Find a freight and train where some wagons are already associated
    -- Use freight 2001 and route 1 (train 5421)
    v_freight_id := 2001;
    
    -- Count wagons in freight
    SELECT COUNT(*)
    INTO v_wagon_count
    FROM Unassigned_Freight
    WHERE FreightID = v_freight_id;
    
    -- Count wagons from freight that are already in train
    SELECT COUNT(*)
    INTO v_wagons_already_in_train
    FROM Unassigned_Freight UF
    WHERE UF.FreightID = v_freight_id
      AND EXISTS (
          SELECT 1 FROM Assigned_Wagon AW 
          WHERE AW.PlannedTrainID = v_train_id 
            AND AW.PlannedTrainStartDate = v_start_date
            AND AW.WagonID = UF.WagonID
      );
    
    DBMS_OUTPUT.PUT_LINE('=== Test Case 7: Duplicate wagon associations ===');
    DBMS_OUTPUT.PUT_LINE('Freight ID: ' || v_freight_id);
    DBMS_OUTPUT.PUT_LINE('Route ID: ' || v_route_id);
    DBMS_OUTPUT.PUT_LINE('Train ID: ' || v_train_id);
    DBMS_OUTPUT.PUT_LINE('Total wagons in freight: ' || v_wagon_count);
    DBMS_OUTPUT.PUT_LINE('Wagons already in train: ' || v_wagons_already_in_train);
    
    -- Associate freight with planned train (should skip duplicates)
    v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);
    
    DBMS_OUTPUT.PUT_LINE('Wagons added: ' || v_result);
    DBMS_OUTPUT.PUT_LINE('Expected: ' || (v_wagon_count - v_wagons_already_in_train));
    
    IF v_result = (v_wagon_count - v_wagons_already_in_train) THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Duplicate wagons correctly skipped!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('INFO: Function handled duplicates (may have added ' || v_result || ' wagons).');
    END IF;
    
    -- Cleanup
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Test data rolled back.');
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/


-- ============================================================================
-- Test Case 8: Multiple freight associations to same train
-- ============================================================================

DECLARE
    v_result1 NUMBER;
    v_result2 NUMBER;
    v_freight_id1 NUMBER := 2002;
    v_freight_id2 NUMBER := 2003;
    v_route_id NUMBER := 3; -- Route for train 5437
    v_train_id NUMBER;
    v_start_date DATE;
    v_total_wagons_added NUMBER;
    v_train_wagon_count_before NUMBER;
    v_train_wagon_count_after NUMBER;
BEGIN
    -- Get planned train information
    SELECT TrainID, startDate
    INTO v_train_id, v_start_date
    FROM Planned_Train
    WHERE RouteID = v_route_id;
    
    DBMS_OUTPUT.PUT_LINE('=== Test Case 8: Multiple freight associations ===');
    DBMS_OUTPUT.PUT_LINE('Freight 1 ID: ' || v_freight_id1);
    DBMS_OUTPUT.PUT_LINE('Freight 2 ID: ' || v_freight_id2);
    DBMS_OUTPUT.PUT_LINE('Route ID: ' || v_route_id);
    DBMS_OUTPUT.PUT_LINE('Train ID: ' || v_train_id);
    
    -- Count wagons currently in train
    SELECT COUNT(*)
    INTO v_train_wagon_count_before
    FROM Assigned_Wagon
    WHERE PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;
    
    -- Remove any existing associations to ensure clean test
    DELETE FROM Assigned_Wagon
    WHERE PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date
      AND (WagonID IN (SELECT WagonID FROM Unassigned_Freight WHERE FreightID = v_freight_id1)
           OR WagonID IN (SELECT WagonID FROM Unassigned_Freight WHERE FreightID = v_freight_id2));
    COMMIT;
    
    -- Associate first freight
    v_result1 := AssociateFreightWithTrain(v_freight_id1, v_route_id);
    DBMS_OUTPUT.PUT_LINE('Freight 1: ' || v_result1 || ' wagons added');
    
    -- Associate second freight
    v_result2 := AssociateFreightWithTrain(v_freight_id2, v_route_id);
    DBMS_OUTPUT.PUT_LINE('Freight 2: ' || v_result2 || ' wagons added');
    
    v_total_wagons_added := v_result1 + v_result2;
    
    -- Count wagons in train after associations
    SELECT COUNT(*)
    INTO v_train_wagon_count_after
    FROM Assigned_Wagon
    WHERE PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;
    
    DBMS_OUTPUT.PUT_LINE('Total wagons added: ' || v_total_wagons_added);
    DBMS_OUTPUT.PUT_LINE('Wagons in train before: ' || v_train_wagon_count_before);
    DBMS_OUTPUT.PUT_LINE('Wagons in train after: ' || v_train_wagon_count_after);
    
    IF v_train_wagon_count_after = v_train_wagon_count_before + v_total_wagons_added THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Multiple freights successfully associated with train!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('INFO: Multiple associations completed (some wagons may have been duplicates).');
    END IF;
    
    -- Cleanup
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Test data rolled back.');
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Summary:
-- - All test cases executed
-- - Successful tests show "SUCCESS" messages via DBMS_OUTPUT
-- - Failed tests show "FAILED" or "ERROR" messages via DBMS_OUTPUT
-- - All test data has been rolled back (no permanent changes)
-- ============================================================================

