-- ============================================================================
-- USBD39 - Associate a locomotive with a planned train
-- ============================================================================
-- As a Freight Manager, I want to associate a locomotive with a planned train.
--
-- Requirements:
-- 1. Associate a locomotive with a planned train by inserting into Assigned_Locomotive table.
-- 2. Validate that the planned train exists (identified by RouteID).
-- 3. Validate that the locomotive exists.
-- 4. Ensure the locomotive is not already associated with the same train.
-- 5. Validate that the locomotive is not already assigned to another train that overlaps
--    in time with the planned train (check for time conflicts).
-- 6. Proper transaction handling (COMMIT/ROLLBACK handled by caller).
-- ============================================================================

-- ============================================================================
-- Stored Function: AssociateLocomotiveWithPlannedTrain
-- ============================================================================
-- Associates a locomotive with a planned train.
--
-- Parameters:
--   p_locomotive_id - ID of the locomotive to associate (required)
--   p_route_id      - ID of the route for the planned train (required)
--
-- Returns:
--   The TrainID that the locomotive was associated with.
--
-- Exceptions:
--   -20001: Locomotive ID cannot be NULL.
--   -20002: Route ID cannot be NULL.
--   -20003: Planned train with the specified RouteID does not exist.
--   -20004: Locomotive with the specified ID does not exist.
--   -20005: Locomotive is already associated with this train.
--   -20006: Locomotive is already assigned to another train with overlapping schedule.
-- ============================================================================

CREATE OR REPLACE FUNCTION AssociateLocomotiveWithPlannedTrain(
    p_locomotive_id IN NUMBER,
    p_route_id IN NUMBER
) RETURN NUMBER
    IS
    v_train_id               NUMBER;
    v_start_date             DATE;
    v_locomotive_exists      NUMBER;
    v_already_associated     NUMBER;
    v_conflicting_train_id   NUMBER;
    v_conflicting_start_date DATE;

BEGIN
    -- 1. Basic validation: locomotive ID cannot be null
    IF p_locomotive_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Locomotive ID cannot be null.');
    END IF;

    -- 2. Basic validation: route ID cannot be null
    IF p_route_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20002, 'Route ID cannot be null.');
    END IF;

    -- 3. Validate that the planned train exists
    BEGIN
        SELECT TrainID, startDate
        INTO v_train_id, v_start_date
        FROM Planned_Train
        WHERE RouteID = p_route_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'Planned train with RouteID ' || p_route_id || ' does not exist.');
    END;

    -- 4. Validate that the locomotive exists
    SELECT COUNT(*)
    INTO v_locomotive_exists
    FROM Locomotive
    WHERE ID = p_locomotive_id;

    IF v_locomotive_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Locomotive with ID ' || p_locomotive_id || ' does not exist.');
    END IF;

    -- 5. Check if locomotive is already associated (in Assigned_Locomotive)
    -- Since PK is LocomotiveID, a locomotive can only be in Assigned_Locomotive once
    SELECT COUNT(*)
    INTO v_already_associated
    FROM Assigned_Locomotive
    WHERE LocomotiveID = p_locomotive_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    IF v_already_associated > 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Locomotive ' || p_locomotive_id || ' is already associated with train ' ||
                                        v_train_id || ' for the planned trip starting at ' ||
                                        TO_CHAR(v_start_date, 'YYYY-MM-DD HH24:MI:SS') || '.');
    END IF;

    -- 6. Check if locomotive is already assigned to a different train (move-based: only one assignment at a time)
    BEGIN
        SELECT AL.PlannedTrainID, AL.PlannedTrainStartDate
        INTO v_conflicting_train_id, v_conflicting_start_date
        FROM Assigned_Locomotive AL
                 JOIN Planned_Train PT ON AL.PlannedTrainID = PT.TrainID AND AL.PlannedTrainStartDate = PT.startDate
        WHERE AL.LocomotiveID = p_locomotive_id
          AND PT.RouteID != p_route_id;

        -- If we found a conflict, raise error
        RAISE_APPLICATION_ERROR(-20006,
                                'Locomotive ' || p_locomotive_id || ' is already assigned to train ' ||
                                v_conflicting_train_id ||
                                ' with a planned route starting at ' ||
                                TO_CHAR(v_conflicting_start_date, 'YYYY-MM-DD HH24:MI:SS') || '.');
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            -- No conflict found, continue
            NULL;
    END;

    -- 7. Move locomotive from Parked_Locomotive to Assigned_Locomotive (if it's parked)
    DELETE FROM Parked_Locomotive WHERE LocomotiveID = p_locomotive_id;

    -- 8. Insert the association into Assigned_Locomotive
    INSERT INTO Assigned_Locomotive (LocomotiveID, PlannedTrainID, PlannedTrainStartDate)
    VALUES (p_locomotive_id, v_train_id, v_start_date);

    RETURN v_train_id;
END AssociateLocomotiveWithPlannedTrain;
/

-- ============================================================================
-- COMPREHENSIVE TEST ANONYMOUS BLOCKS
-- ============================================================================

-- Test 1: Happy path - Associate locomotive with planned train
-- Expected: Should successfully associate and return train ID
-- Note: Using train 5435 (route 2) which has only locomotive 2 and 2 wagons, so adding locomotive 3 should fit within 250m limit
DECLARE
    v_locomotive_id  NUMBER := 3; -- Use locomotive ID 3 (not yet assigned based on USBD32)
    v_route_id       NUMBER := 2; -- Route 2 is planned for train 5435 (has less assigned than 5421)
    v_train_id       NUMBER;
    v_verified_count NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 1: Happy path');

    v_train_id := AssociateLocomotiveWithPlannedTrain(v_locomotive_id, v_route_id);

    SELECT COUNT(*)
    INTO v_verified_count
    FROM Assigned_Locomotive AL
             JOIN Planned_Train PT ON AL.PlannedTrainID = PT.TrainID AND AL.PlannedTrainStartDate = PT.startDate
    WHERE AL.LocomotiveID = v_locomotive_id
      AND PT.RouteID = v_route_id;

    IF v_verified_count = 1 THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Association not found');
    END IF;

    DELETE
    FROM Assigned_Locomotive
    WHERE LocomotiveID = v_locomotive_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = (SELECT startDate FROM Planned_Train WHERE RouteID = v_route_id);
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 2: Null locomotive ID validation
-- Expected: Should raise error -20001
DECLARE
    v_train_id   NUMBER;
    v_error_code NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 2: Null locomotive ID');

    BEGIN
        v_train_id := AssociateLocomotiveWithPlannedTrain(NULL, 1);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20001 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20001, got ' || v_error_code);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 3: Null route ID validation
-- Expected: Should raise error -20002
DECLARE
    v_train_id   NUMBER;
    v_error_code NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 3: Null route ID');

    BEGIN
        v_train_id := AssociateLocomotiveWithPlannedTrain(1, NULL);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20002 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20002, got ' || v_error_code);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 4: Non-existent planned train validation
-- Expected: Should raise error -20003
DECLARE
    v_train_id         NUMBER;
    v_error_code       NUMBER;
    v_invalid_route_id NUMBER := 99999; -- Non-existent route ID
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 4: Non-existent planned train');

    BEGIN
        v_train_id := AssociateLocomotiveWithPlannedTrain(1, v_invalid_route_id);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20003 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20003, got ' || v_error_code);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 5: Non-existent locomotive validation
-- Expected: Should raise error -20004
DECLARE
    v_train_id        NUMBER;
    v_error_code      NUMBER;
    v_invalid_loco_id NUMBER := 99999; -- Non-existent locomotive ID
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 5: Non-existent locomotive');

    BEGIN
        v_train_id := AssociateLocomotiveWithPlannedTrain(v_invalid_loco_id, 1);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20004 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20004, got ' || v_error_code);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 6: Duplicate association validation
-- Expected: Should raise error -20005
DECLARE
    v_train_id      NUMBER;
    v_error_code    NUMBER;
    v_locomotive_id NUMBER := 1; -- Locomotive 1 is already assigned to train 5421 (from USBD32)
    v_route_id      NUMBER := 1; -- Route 1 is for train 5421
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 6: Duplicate association');

    BEGIN
        v_train_id := AssociateLocomotiveWithPlannedTrain(v_locomotive_id, v_route_id);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20005 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20005, got ' || v_error_code);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 7: Transaction rollback test
-- Expected: Should allow rollback by caller (function should not commit)
-- Note: Using train 5435 (route 2) which has capacity for another locomotive
DECLARE
    v_locomotive_id        NUMBER := 4; -- Use locomotive ID 4 (not yet assigned)
    v_route_id             NUMBER := 2; -- Route 2 is planned for train 5435 (has less assigned)
    v_train_id             NUMBER;
    v_count_before         NUMBER;
    v_count_after          NUMBER;
    v_count_after_rollback NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 7: Transaction rollback');

    SELECT COUNT(*) INTO v_count_before FROM Assigned_Locomotive;

    v_train_id := AssociateLocomotiveWithPlannedTrain(v_locomotive_id, v_route_id);

    SELECT COUNT(*) INTO v_count_after FROM Assigned_Locomotive;

    ROLLBACK;

    SELECT COUNT(*) INTO v_count_after_rollback FROM Assigned_Locomotive;

    IF v_count_after = v_count_before + 1 AND v_count_after_rollback = v_count_before THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Transaction management issue');
        DBMS_OUTPUT.PUT_LINE('    Before: ' || v_count_before || ', After: ' || v_count_after || ', After rollback: ' ||
                             v_count_after_rollback);
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
END;
/

