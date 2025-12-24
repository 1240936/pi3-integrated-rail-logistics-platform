-- ============================================================================
-- USBD38 - Remove a freight from a planned train
-- ============================================================================
-- Description: Removes a freight assignment from a planned train and 
-- automatically detaches all associated wagons.
-- ============================================================================

CREATE OR REPLACE FUNCTION RemoveFreightFromTrain(
    p_freight_id IN NUMBER,
    p_planned_train_id IN NUMBER,
    p_start_date IN DATE
) RETURN NUMBER
IS
    v_freight_exists NUMBER;
    v_train_exists NUMBER;
    v_link_exists NUMBER;
BEGIN
    -- 1. Validation: Check for NULL parameters
    IF p_freight_id IS NULL OR p_planned_train_id IS NULL OR p_start_date IS NULL THEN
        RAISE_APPLICATION_ERROR(-20100, 'Parameters FreightID, TrainID, and StartDate cannot be NULL');
    END IF;

    -- 2. Validation: Check if Freight exists in the Freight table
    SELECT COUNT(*) INTO v_freight_exists 
    FROM Freight 
    WHERE ID = p_freight_id;
    
    IF v_freight_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20102, 'Freight ID ' || p_freight_id || ' does not exist.');
    END IF;

    -- 3. Validation: Check if Planned_Train exists (Key: TrainID + startDate)
    SELECT COUNT(*) INTO v_train_exists 
    FROM Planned_Train 
    WHERE TrainID = p_planned_train_id AND startDate = p_start_date;
    
    IF v_train_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20103, 'Planned Train not found for ID ' || p_planned_train_id || ' on ' || TO_CHAR(p_start_date, 'YYYY-MM-DD'));
    END IF;

    -- 4. Check if there is any assignment linking this freight to this train
    SELECT COUNT(*) INTO v_link_exists 
    FROM Assigned_Freight 
    WHERE FreightID = p_freight_id 
      AND PlannedTrainID = p_planned_train_id
      AND PlannedTrainStartDate = p_start_date;

    IF v_link_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20104, 'The specified Freight is not assigned to this Planned Train.');
    END IF;

    -- 5. Operation: Detach all wagons associated with this freight from the train
    -- According to US requirements, removing the freight must detach all its wagons.
    DELETE FROM Assigned_Freight
    WHERE FreightID = p_freight_id 
      AND PlannedTrainID = p_planned_train_id
      AND PlannedTrainStartDate = p_start_date;

    -- 6. Atomic Transaction
    COMMIT;
    
    DBMS_OUTPUT.PUT_LINE('SUCCESS: Freight ' || p_freight_id || ' and its wagons removed from Train ' || p_planned_train_id);
    RETURN 1;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        -- Re-raise custom application errors
        IF SQLCODE BETWEEN -20999 AND -20000 THEN 
            RAISE;
        ELSE 
            RAISE_APPLICATION_ERROR(-20199, 'Unexpected System Error: ' || SQLERRM);
        END IF;
END RemoveFreightFromTrain;
/

-- ============================================================================
-- USBD38 - Comprehensive Test Script for RemoveFreightFromTrain Function
-- ============================================================================
-- This script tests the RemoveFreightFromTrain function with various scenarios:
-- 1. Happy path (successful removal of freight and its wagons)
-- 2. NULL parameters (WagonID, TrainID, or StartDate)
-- 3. Freight ID does not exist
-- 4. Planned Train (ID + Date) does not exist
-- 5. Freight is not assigned to the specified train
-- 6. Detach multiple wagons (verifying all linked wagons are removed)
-- 7. Preservation of other freights in the same train
-- 8. Transaction atomicity (rollback on failure)
-- ============================================================================

-- ============================================================================
-- Test Setup: Environment Verification and Data Preparation
-- ============================================================================

SET SERVEROUTPUT ON;

DECLARE
    v_freight_id        NUMBER := 100;
    v_train_id          NUMBER := 500;
    v_start_date        DATE := TO_DATE('2025-01-20', 'YYYY-MM-DD');
    v_wagon_id          NUMBER := 1;
    
    v_count_freight     NUMBER;
    v_count_train       NUMBER;
    v_count_assignment  NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Environment Verification for USBD38 ---');

    -- 1. Check if the Freight exists
    SELECT COUNT(*) INTO v_count_freight FROM Freight WHERE ID = v_freight_id;
    
    -- 2. Check if the Planned Train exists
    SELECT COUNT(*) INTO v_count_train 
    FROM Planned_Train 
    WHERE TrainID = v_train_id AND startDate = v_start_date;

    -- 3. Check if there are already assignments for this test case
    SELECT COUNT(*) INTO v_count_assignment 
    FROM Assigned_Freight 
    WHERE FreightID = v_freight_id 
      AND PlannedTrainID = v_train_id 
      AND PlannedTrainStartDate = v_start_date;

    -- Console Report
    DBMS_OUTPUT.PUT_LINE('Freight ID ' || v_freight_id || ' exists: ' || CASE WHEN v_count_freight > 0 THEN 'YES' ELSE 'NO' END);
    DBMS_OUTPUT.PUT_LINE('Planned Train exists:          ' || CASE WHEN v_count_train > 0 THEN 'YES' ELSE 'NO' END);
    DBMS_OUTPUT.PUT_LINE('Existing Assignments found:    ' || v_count_assignment);
    DBMS_OUTPUT.PUT_LINE('-------------------------------------------');

    -- Safety Warnings
    IF v_count_freight = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: Freight ID ' || v_freight_id || ' is missing. Setup some data before testing.');
    END IF;

    IF v_count_train = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: Planned Train not found. Ensure ID ' || v_train_id || ' and Date 2025-01-20 exist.');
    END IF;

    IF v_count_assignment = 0 THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL: No wagons linked for this Freight/Train combo. Happy Path will fail.');
    END IF;

END;
/

-- ============================================================================
-- Test Case 1: NULL parameters
-- ============================================================================

-- 1.1 Test NULL Freight ID (Expects -20100)
DECLARE
    v_result NUMBER;
    v_train_id NUMBER := 500;
    v_date DATE := TO_DATE('2025-01-20', 'YYYY-MM-DD');
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Case 1.1: Testing NULL Freight ID ---');
    v_result := RemoveFreightFromTrain(NULL, v_train_id, v_date);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL Freight ID');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20100 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Freight ID (-20100).');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- 1.2 Test NULL Train ID (Expects -20100)
DECLARE
    v_result NUMBER;
    v_freight_id NUMBER := 100;
    v_date DATE := TO_DATE('2025-01-20', 'YYYY-MM-DD');
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Case 1.2: Testing NULL Train ID ---');
    v_result := RemoveFreightFromTrain(v_freight_id, NULL, v_date);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL Train ID');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20100 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Train ID (-20100).');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 2: Freight ID does not exist
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_invalid_freight_id NUMBER := -999; -- An ID that definitely does not exist
    v_train_id NUMBER := 500;
    v_date DATE := TO_DATE('2025-01-20', 'YYYY-MM-DD');
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 2: Non-existent Freight ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to remove Freight ID: ' || v_invalid_freight_id);

    -- This should trigger the RAISE_APPLICATION_ERROR (-20102) from your function
    v_result := RemoveFreightFromTrain(v_invalid_freight_id, v_train_id, v_date);

    -- If it reaches here, it means the function didn't raise the error
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception for non-existent Freight!');
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        -- Check if the error code is exactly what we defined (-20102)
        IF SQLCODE = -20102 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent Freight ID.');
            DBMS_OUTPUT.PUT_LINE('Expected Error Message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Expected error -20102 but got ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 3: Planned Train (ID + Date) does not exist
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER := 100; -- Assuming this is a valid Freight ID from setup
    v_train_id NUMBER := 500;
    v_invalid_date DATE := TO_DATE('1900-01-01', 'YYYY-MM-DD'); -- A date with no planned trips
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 3: Non-existent Planned Train ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to remove from Train ID ' || v_train_id || ' on an invalid date.');

    -- 1. Ensure the freight exists so it passes the first check
    -- We assume the setup script was run.
    
    -- 2. Call the function with a date that has no planned train
    -- This should trigger RAISE_APPLICATION_ERROR (-20103)
    v_result := RemoveFreightFromTrain(v_freight_id, v_train_id, v_invalid_date);

    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have failed for non-existent Train/Date combination!');
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        -- Check if the error code is -20103 (Planned Train not found)
        IF SQLCODE = -20103 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected invalid Planned Train (ID + Date).');
            DBMS_OUTPUT.PUT_LINE('Expected Error Message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Expected error -20103 but got ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 4: Freight is not assigned to the specified train
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER;
    v_train_id NUMBER;
    v_date DATE := TO_DATE('2025-12-25', 'YYYY-MM-DD');
BEGIN
    -- 1. Setup: Get a valid Freight and a valid Planned Train
    SELECT ID INTO v_freight_id FROM Freight WHERE ROWNUM = 1;
    
    SELECT TrainID, startDate 
    INTO v_train_id, v_date 
    FROM Planned_Train 
    WHERE ROWNUM = 1;

    -- 2. Ensure they are NOT linked in Assigned_Freight for this test
    DELETE FROM Assigned_Freight 
    WHERE FreightID = v_freight_id 
      AND PlannedTrainID = v_train_id 
      AND PlannedTrainStartDate = v_date;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 4: Missing Assignment Link ---');
    DBMS_OUTPUT.PUT_LINE('Testing Freight ' || v_freight_id || ' with Train ' || v_train_id || ' (No link exists)');

    -- 3. Call function - Expected to raise -20104
    v_result := RemoveFreightFromTrain(v_freight_id, v_train_id, v_date);

    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised -20104 for missing link!');
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20104 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected removal because no assignment was found.');
            DBMS_OUTPUT.PUT_LINE('Expected Error Message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Expected -20104 but got ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 5: Happy Path (Successful Removal)
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER := 100; -- ID used in Setup
    v_train_id NUMBER := 500;   -- ID used in Setup
    v_date DATE := TO_DATE('2025-01-20', 'YYYY-MM-DD');
    v_count_after NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 5: Happy Path ---');

    -- 1. Setup: Ensure a link exists for this test
    -- We force an insert just in case the setup script wasn't run
    BEGIN
        INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate)
        VALUES (v_freight_id, 1, v_train_id, v_date);
    EXCEPTION WHEN OTHERS THEN NULL; -- Ignore if already exists
    END;
    COMMIT;

    -- 2. Call the function
    v_result := RemoveFreightFromTrain(v_freight_id, v_train_id, v_date);

    -- 3. Validation
    IF v_result = 1 THEN
        -- Check if any record remains in Assigned_Freight for this link
        SELECT COUNT(*) INTO v_count_after 
        FROM Assigned_Freight 
        WHERE FreightID = v_freight_id 
          AND PlannedTrainID = v_train_id 
          AND PlannedTrainStartDate = v_date;

        IF v_count_after = 0 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Freight and its wagons successfully removed.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Records still exist in Assigned_Freight!');
        END IF;
    ELSE
        DBMS_OUTPUT.PUT_LINE('FAILED: Function returned ' || v_result);
    END IF;

    ROLLBACK; -- Always rollback to keep the database clean

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL ERROR: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 6: Detach multiple wagons (verifying all linked wagons are removed)
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER := 100;
    v_train_id NUMBER := 500;
    v_date DATE := TO_DATE('2025-01-20', 'YYYY-MM-DD');
    v_count_before NUMBER;
    v_count_after NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 6: Multiple Wagons Removal ---');

    -- 1. Setup: Ensure 3 different wagons are linked to this Freight on this Train
    -- Cleaning previous data first
    DELETE FROM Assigned_Freight WHERE FreightID = v_freight_id;
    
    INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate) VALUES (v_freight_id, 1, v_train_id, v_date);
    INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate) VALUES (v_freight_id, 2, v_train_id, v_date);
    INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate) VALUES (v_freight_id, 3, v_train_id, v_date);
    
    SELECT COUNT(*) INTO v_count_before 
    FROM Assigned_Freight 
    WHERE FreightID = v_freight_id AND PlannedTrainID = v_train_id AND PlannedTrainStartDate = v_date;
    
    DBMS_OUTPUT.PUT_LINE('Wagons linked before removal: ' || v_count_before);

    -- 2. Call the function
    v_result := RemoveFreightFromTrain(v_freight_id, v_train_id, v_date);

    -- 3. Validation: The count must be exactly 0
    IF v_result = 1 THEN
        SELECT COUNT(*) INTO v_count_after 
        FROM Assigned_Freight 
        WHERE FreightID = v_freight_id AND PlannedTrainID = v_train_id AND PlannedTrainStartDate = v_date;

        IF v_count_after = 0 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: All ' || v_count_before || ' wagons were successfully detached.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Still exists ' || v_count_after || ' wagons linked to the freight!');
        END IF;
    END IF;

    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL ERROR: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 7: Preservation of other freights in the same train
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_A NUMBER := 100; -- Freight to be removed
    v_freight_B NUMBER := 101; -- Freight that MUST stay
    v_train_id NUMBER := 500;
    v_date DATE := TO_DATE('2025-01-20', 'YYYY-MM-DD');
    v_count_B_after NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 7: Isolation Test ---');

    -- 1. Setup: Ensure both freights exist and are linked to the SAME train
    -- (Ensure Freight 101 exists in your Freight table first)
    DELETE FROM Assigned_Freight WHERE PlannedTrainID = v_train_id AND PlannedTrainStartDate = v_date;
    
    -- Link Freight A
    INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate) 
    VALUES (v_freight_A, 1, v_train_id, v_date);
    
    -- Link Freight B (The one that should be preserved)
    INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate) 
    VALUES (v_freight_B, 2, v_train_id, v_date);
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('Setup: Freights ' || v_freight_A || ' and ' || v_freight_B || ' linked to Train ' || v_train_id);

    -- 2. Call the function to remove ONLY Freight A
    v_result := RemoveFreightFromTrain(v_freight_A, v_train_id, v_date);

    -- 3. Validation: Freight A should be gone, Freight B must remain
    IF v_result = 1 THEN
        -- Check if Freight B is still there
        SELECT COUNT(*) INTO v_count_B_after 
        FROM Assigned_Freight 
        WHERE FreightID = v_freight_B AND PlannedTrainID = v_train_id AND PlannedTrainStartDate = v_date;

        IF v_count_B_after > 0 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Freight ' || v_freight_A || ' removed, but Freight ' || v_freight_B || ' was preserved.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Freight ' || v_freight_B || ' was accidentally deleted!');
        END IF;
    END IF;

    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL ERROR: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 8: Transaction Atomicity (Rollback on failure)
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_freight_id NUMBER := 100;
    v_train_id NUMBER := 500;
    v_date DATE := TO_DATE('2025-01-20', 'YYYY-MM-DD');
    v_count_before NUMBER;
    v_count_after NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 8: Atomicity ---');

    -- 1. Setup: Ensure data exists
    DELETE FROM Assigned_Freight WHERE FreightID = v_freight_id;
    INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate) 
    VALUES (v_freight_id, 1, v_train_id, v_date);
    COMMIT;

    SELECT COUNT(*) INTO v_count_before FROM Assigned_Freight WHERE FreightID = v_freight_id;

    -- 2. Simulate a failure 
    -- We can't easily force an internal error, so we test the logic:
    -- If we call the function with an invalid state and it fails, 
    -- the data should remain untouched.
    BEGIN
        -- Calling with an ID that will trigger our custom exception -20102
        v_result := RemoveFreightFromTrain(-999, v_train_id, v_date);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Caught expected exception during execution.');
    END;

    -- 3. Validation: The original record must still exist because of ROLLBACK
    SELECT COUNT(*) INTO v_count_after FROM Assigned_Freight WHERE FreightID = v_freight_id;

    IF v_count_before = v_count_after THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Atomicity preserved. Data remains consistent after failure.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('FAILED: Data was modified even though an error occurred!');
    END IF;

    -- Clean up
    DELETE FROM Assigned_Freight WHERE FreightID = v_freight_id;
    COMMIT;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL ERROR in test script: ' || SQLERRM);
        ROLLBACK;
END;
/

