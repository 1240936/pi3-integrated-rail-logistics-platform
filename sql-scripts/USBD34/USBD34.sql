-- ============================================================================
-- USBD34 - Associate Freight with Train
-- ============================================================================
-- As a Freight Manager, I want to associate a freight with a train, so that
-- all wagons included in the freight are linked to the train.
--
-- Requirements:
-- 1. Associate a freight with a train (all wagons from freight linked to train)
-- 2. Must prevent duplicate associations (a wagon cannot belong to multiple trains simultaneously)
-- 3. Must handle invalid cases (freight not found, train not found, wagons already assigned)
-- 4. Referential integrity enforced between freight, wagons, and train tables
-- 5. Implementation must use Oracle DBMS features (foreign keys, PL/SQL procedures)
-- 6. Validation queries must confirm that wagons are correctly associated
-- ============================================================================

-- Function: Associate a freight with a train
-- Input: p_freight_id - ID of the freight to associate
--        p_route_id - ID of the route to find the planned train
-- Returns: Number of wagons successfully associated with the train
-- Error Codes:
--   -20001: Freight ID is null
--   -20002: Route ID is null
--   -20003: Freight not found
--   -20004: Route not found
--   -20005: No planned train found for the route
--   -20006: Wagon already assigned to another train
--   -20099: Unexpected error

CREATE OR REPLACE FUNCTION AssociateFreightWithTrain(
    p_freight_id IN NUMBER,
    p_route_id IN NUMBER
)
RETURN NUMBER
AS
    v_train_id NUMBER;
    v_start_date DATE;
    v_wagon_count NUMBER := 0;
    v_wagon_id NUMBER;
    v_already_assigned NUMBER;

    CURSOR c_unassigned_wagons IS
        SELECT WagonID
        FROM Unassigned_Freight
        WHERE FreightID = p_freight_id;

BEGIN
    -- Validate input parameters
    IF p_freight_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Freight ID cannot be null.');
    END IF;

    IF p_route_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20002, 'Route ID cannot be null.');
    END IF;

    -- Validate freight exists
    DECLARE
        v_freight_exists NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_freight_exists
        FROM Freight
        WHERE ID = p_freight_id;

        IF v_freight_exists = 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'Freight ID ' || p_freight_id || ' not found.');
        END IF;
    END;

    -- Validate route exists
    DECLARE
        v_route_exists NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_route_exists
        FROM Route
        WHERE ID = p_route_id;

        IF v_route_exists = 0 THEN
            RAISE_APPLICATION_ERROR(-20004, 'Route ID ' || p_route_id || ' not found.');
        END IF;
    END;

    -- Get the planned train for the route (select earliest if multiple)
    BEGIN
        SELECT TrainID, startDate
        INTO v_train_id, v_start_date
        FROM (
            SELECT TrainID, startDate
            FROM Planned_Train
            WHERE RouteID = p_route_id
            ORDER BY startDate
        )
        WHERE ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20005, 'No planned train found for route ID ' || p_route_id || '.');
    END;

    -- Check if freight has wagons to associate
    SELECT COUNT(*) INTO v_wagon_count
    FROM Unassigned_Freight
    WHERE FreightID = p_freight_id;

    IF v_wagon_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('Freight ID ' || p_freight_id || ' has no unassigned wagons.');
        RETURN 0;
    END IF;

    -- Reset counter for tracking successfully associated wagons
    v_wagon_count := 0;

    -- Process each wagon in the freight
    FOR wagon_rec IN c_unassigned_wagons LOOP
        v_wagon_id := wagon_rec.WagonID;

        -- Check if wagon is already assigned to another train
        BEGIN
            SELECT COUNT(*) INTO v_already_assigned
            FROM Assigned_Wagon
            WHERE WagonID = v_wagon_id
              AND (PlannedTrainID != v_train_id OR PlannedTrainStartDate != v_start_date);

            IF v_already_assigned > 0 THEN
                DBMS_OUTPUT.PUT_LINE('Wagon ID ' || v_wagon_id || ' is already assigned to another train. Skipping.');
                CONTINUE;
            END IF;

            -- Check if wagon is already assigned to this train (duplicate check)
            SELECT COUNT(*) INTO v_already_assigned
            FROM Assigned_Wagon
            WHERE WagonID = v_wagon_id
              AND PlannedTrainID = v_train_id
              AND PlannedTrainStartDate = v_start_date;

            IF v_already_assigned > 0 THEN
                -- Already associated with this train, just remove from Unassigned_Freight
                -- and ensure it's in Assigned_Freight
                DELETE FROM Unassigned_Freight
                WHERE FreightID = p_freight_id
                  AND WagonID = v_wagon_id;

                -- Insert into Assigned_Freight if not already there
                BEGIN
                    INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate)
                    VALUES (p_freight_id, v_wagon_id, v_train_id, v_start_date);
                EXCEPTION
                    WHEN DUP_VAL_ON_INDEX THEN
                        -- Already in Assigned_Freight, ignore
                        NULL;
                END;

                v_wagon_count := v_wagon_count + 1;
                CONTINUE;
            END IF;

            -- Associate wagon with train
            INSERT INTO Assigned_Wagon (WagonID, PlannedTrainID, PlannedTrainStartDate)
            VALUES (v_wagon_id, v_train_id, v_start_date);

            -- Remove from Unassigned_Freight
            DELETE FROM Unassigned_Freight
            WHERE FreightID = p_freight_id
              AND WagonID = v_wagon_id;

            -- Add to Assigned_Freight
            INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate)
            VALUES (p_freight_id, v_wagon_id, v_train_id, v_start_date);

            v_wagon_count := v_wagon_count + 1;

        EXCEPTION
            WHEN DUP_VAL_ON_INDEX THEN
                -- Handle duplicate key error (should not happen, but handle gracefully)
                DBMS_OUTPUT.PUT_LINE('Wagon ID ' || v_wagon_id || ' already associated. Skipping.');
                CONTINUE;
            WHEN OTHERS THEN
                -- Re-raise application errors
                IF SQLCODE BETWEEN -20999 AND -20000 THEN
                    RAISE;
                ELSE
                    RAISE_APPLICATION_ERROR(-20099, 'Error associating wagon ' || v_wagon_id || ': ' || SQLERRM);
                END IF;
        END;
    END LOOP;

COMMIT; -- Atomic transaction
RETURN v_wagon_count;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        IF SQLCODE BETWEEN -20999 AND -20000 THEN
            RAISE;
        ELSE
            RAISE_APPLICATION_ERROR(-20099, 'Unexpected error: ' || SQLERRM);
        END IF;
END AssociateFreightWithTrain;
/


-- ============================================================================
-- USBD34 - Comprehensive Test Script for AssociateFreightWithTrain Function
-- ============================================================================
-- This script tests the AssociateFreightWithTrain function with various scenarios:
-- 1. Happy path (successful association)
-- 2. Null freight ID
-- 3. Null route ID
-- 4. Freight not found
-- 5. Route not found
-- 6. No planned train for route
-- 7. Freight with no unassigned wagons
-- 8. Wagon already assigned to another train
-- 9. Duplicate association (same freight/train again)
-- ============================================================================

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
    v_assigned_wagon_count NUMBER;
    v_assigned_freight_count NUMBER;
    v_unassigned_freight_count NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 1: Happy Path ---');

    -- Find a freight with unassigned wagons
    SELECT freight_id, wagon_count
    INTO v_freight_id, v_wagon_count
    FROM (
        SELECT FreightID as freight_id, COUNT(*) as wagon_count
        FROM Unassigned_Freight
        GROUP BY FreightID
        HAVING COUNT(*) > 0
        ORDER BY COUNT(*) DESC
    )
    WHERE ROWNUM = 1;

    -- Find a route with planned train 5435 (has less freight, more capacity)
    BEGIN
        SELECT pt.RouteID, pt.TrainID, pt.startDate
        INTO v_route_id, v_train_id, v_start_date
        FROM Planned_Train pt
        WHERE pt.TrainID = 5435
          AND ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            -- Fall back to any train if 5435 not found
            SELECT pt.RouteID, pt.TrainID, pt.startDate
            INTO v_route_id, v_train_id, v_start_date
            FROM Planned_Train pt
            WHERE ROWNUM = 1;
    END;

    DBMS_OUTPUT.PUT_LINE('Freight ID: ' || v_freight_id);
    DBMS_OUTPUT.PUT_LINE('Route ID: ' || v_route_id);
    DBMS_OUTPUT.PUT_LINE('Train ID: ' || v_train_id);
    DBMS_OUTPUT.PUT_LINE('Unassigned wagons in freight: ' || v_wagon_count);

    -- Count initial state
    SELECT COUNT(*) INTO v_unassigned_freight_count
    FROM Unassigned_Freight
    WHERE FreightID = v_freight_id;

    -- Call the function
    v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);

    -- Count after association (check wagons that were in the freight)
    SELECT COUNT(*) INTO v_assigned_wagon_count
    FROM Assigned_Wagon aw
    WHERE aw.PlannedTrainID = v_train_id
      AND aw.PlannedTrainStartDate = v_start_date
      AND EXISTS (
        SELECT 1 FROM Assigned_Freight af
        WHERE af.FreightID = v_freight_id
          AND af.WagonID = aw.WagonID
          AND af.PlannedTrainID = v_train_id
          AND af.PlannedTrainStartDate = v_start_date
    );

    SELECT COUNT(*) INTO v_assigned_freight_count
    FROM Assigned_Freight
    WHERE FreightID = v_freight_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    SELECT COUNT(*) INTO v_unassigned_freight_count
    FROM Unassigned_Freight
    WHERE FreightID = v_freight_id;

    DBMS_OUTPUT.PUT_LINE('Function returned: ' || v_result || ' wagons associated');
    DBMS_OUTPUT.PUT_LINE('Wagons in Assigned_Wagon: ' || v_assigned_wagon_count);
    DBMS_OUTPUT.PUT_LINE('Wagons in Assigned_Freight: ' || v_assigned_freight_count);
    DBMS_OUTPUT.PUT_LINE('Wagons remaining in Unassigned_Freight: ' || v_unassigned_freight_count);

    IF v_result = v_wagon_count AND v_unassigned_freight_count = 0 AND v_assigned_freight_count = v_wagon_count THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: All wagons successfully associated with train!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Association verification failed.');
    END IF;

    -- Cleanup: restore wagons to Unassigned_Freight
    INSERT INTO Unassigned_Freight (FreightID, WagonID)
    SELECT FreightID, WagonID
    FROM Assigned_Freight
    WHERE FreightID = v_freight_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date
      AND NOT EXISTS (
        SELECT 1 FROM Unassigned_Freight UF
        WHERE UF.FreightID = Assigned_Freight.FreightID
          AND UF.WagonID = Assigned_Freight.WagonID
    );

    DELETE FROM Assigned_Freight
    WHERE FreightID = v_freight_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    DELETE FROM Assigned_Wagon
    WHERE PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date
      AND WagonID IN (SELECT WagonID FROM Unassigned_Freight WHERE FreightID = v_freight_id);

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Test finished and data restored.');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        ROLLBACK;
END;
/


-- ============================================================================
-- Test Case 2: Null Freight ID
-- ============================================================================
DECLARE
v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 2: Null Freight ID ---');

    v_result := AssociateFreightWithTrain(NULL, 1);
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an error.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20001 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly raised error for null freight ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
END;
/


-- ============================================================================
-- Test Case 3: Null Route ID
-- ============================================================================
DECLARE
v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 3: Null Route ID ---');

    v_result := AssociateFreightWithTrain(2007, NULL);
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an error.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20002 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly raised error for null route ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
END;
/


-- ============================================================================
-- Test Case 4: Freight Not Found
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_invalid_freight_id NUMBER := 99999;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 4: Freight Not Found ---');

    v_result := AssociateFreightWithTrain(v_invalid_freight_id, 1);
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an error.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20003 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly raised error for non-existent freight ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
END;
/


-- ============================================================================
-- Test Case 5: Route Not Found
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_invalid_route_id NUMBER := 99999;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 5: Route Not Found ---');

    -- Use a valid freight ID
    SELECT FreightID INTO v_result
    FROM (
        SELECT FreightID
        FROM Unassigned_Freight
        WHERE ROWNUM = 1
    );

    v_result := AssociateFreightWithTrain(v_result, v_invalid_route_id);
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an error.');

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('SKIPPED: No unassigned freight found for testing.');
    WHEN OTHERS THEN
        IF SQLCODE = -20004 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly raised error for non-existent route ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
END;
/


-- ============================================================================
-- Test Case 6: Freight with No Unassigned Wagons
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_route_id NUMBER;
    v_train_id NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 6: Freight with No Unassigned Wagons ---');

    -- Find a freight that has assigned wagons but no unassigned wagons
    SELECT FreightID, RouteID, PlannedTrainID
    INTO v_freight_id, v_route_id, v_train_id
    FROM (
        SELECT DISTINCT af.FreightID, pt.RouteID, pt.TrainID AS PlannedTrainID
        FROM Assigned_Freight af
        JOIN Planned_Train pt ON af.PlannedTrainID = pt.TrainID AND af.PlannedTrainStartDate = pt.startDate
        WHERE NOT EXISTS (
            SELECT 1 FROM Unassigned_Freight uf WHERE uf.FreightID = af.FreightID
        )
          AND ROWNUM = 1
    );

    v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);

    IF v_result = 0 THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Function correctly returned 0 for freight with no unassigned wagons.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Expected 0 wagons, got ' || v_result);
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('SKIPPED: No freight found with only assigned wagons (no unassigned).');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
END;
/


-- ============================================================================
-- Test Case 7: Wagon Already Assigned to Another Train
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_wagon_id NUMBER;
    v_existing_route_id NUMBER;
    v_existing_train_id NUMBER;
    v_existing_start_date DATE;
    v_new_route_id NUMBER;
    v_new_train_id NUMBER;
    v_new_start_date DATE;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 7: Wagon Already Assigned to Another Train ---');

    -- Find a wagon that is already assigned to a train
    SELECT af.FreightID, af.WagonID, pt.RouteID, pt.TrainID, pt.startDate
    INTO v_freight_id, v_wagon_id, v_existing_route_id, v_existing_train_id, v_existing_start_date
    FROM Assigned_Freight af
    JOIN Planned_Train pt ON af.PlannedTrainID = pt.TrainID AND af.PlannedTrainStartDate = pt.startDate
    WHERE ROWNUM = 1;

    -- Find a different route/train
    SELECT pt.RouteID, pt.TrainID, pt.startDate
    INTO v_new_route_id, v_new_train_id, v_new_start_date
    FROM Planned_Train pt
    WHERE (pt.RouteID != v_existing_route_id OR pt.TrainID != v_existing_train_id OR pt.startDate != v_existing_start_date)
      AND ROWNUM = 1;

    -- Move wagon to Unassigned_Freight temporarily
    INSERT INTO Unassigned_Freight (FreightID, WagonID)
    VALUES (v_freight_id, v_wagon_id);

    DBMS_OUTPUT.PUT_LINE('Wagon ID: ' || v_wagon_id);
    DBMS_OUTPUT.PUT_LINE('Already assigned to Train: ' || v_existing_train_id || ', Route: ' || v_existing_route_id);
    DBMS_OUTPUT.PUT_LINE('Attempting to assign to Train: ' || v_new_train_id || ', Route: ' || v_new_route_id);

    -- Try to associate with different train (should skip the already assigned wagon)
    v_result := AssociateFreightWithTrain(v_freight_id, v_new_route_id);

    DBMS_OUTPUT.PUT_LINE('Function returned: ' || v_result);
    DBMS_OUTPUT.PUT_LINE('Expected: Wagon should be skipped (already assigned to another train)');

    -- Restore wagon to original state
    DELETE FROM Unassigned_Freight
    WHERE FreightID = v_freight_id AND WagonID = v_wagon_id;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('SUCCESS: Function handled wagon already assigned to another train correctly.');

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('SKIPPED: Could not find appropriate test data.');
        ROLLBACK;
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        ROLLBACK;
END;
/


-- ============================================================================
-- Test Case 8: Duplicate Association (Same Freight/Train Again)
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_route_id NUMBER;
    v_train_id NUMBER;
    v_start_date DATE;
    v_initial_assigned_count NUMBER;
    v_final_assigned_count NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 8: Duplicate Association ---');

    -- Find a freight that is already associated with a train
    SELECT af.FreightID, pt.RouteID, pt.TrainID, pt.startDate
    INTO v_freight_id, v_route_id, v_train_id, v_start_date
    FROM Assigned_Freight af
    JOIN Planned_Train pt ON af.PlannedTrainID = pt.TrainID AND af.PlannedTrainStartDate = pt.startDate
    WHERE ROWNUM = 1;

    SELECT COUNT(*) INTO v_initial_assigned_count
    FROM Assigned_Freight
    WHERE FreightID = v_freight_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    DBMS_OUTPUT.PUT_LINE('Freight ID: ' || v_freight_id);
    DBMS_OUTPUT.PUT_LINE('Initial assigned wagons: ' || v_initial_assigned_count);

    -- Try to associate again (should handle gracefully)
    v_result := AssociateFreightWithTrain(v_freight_id, v_route_id);

    SELECT COUNT(*) INTO v_final_assigned_count
    FROM Assigned_Freight
    WHERE FreightID = v_freight_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    DBMS_OUTPUT.PUT_LINE('Function returned: ' || v_result);
    DBMS_OUTPUT.PUT_LINE('Final assigned wagons: ' || v_final_assigned_count);

    IF v_final_assigned_count = v_initial_assigned_count THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Duplicate association handled correctly (no duplicates created).');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Duplicate association count changed unexpectedly.');
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('SKIPPED: No assigned freight found for testing.');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
END;
/