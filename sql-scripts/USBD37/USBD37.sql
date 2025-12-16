-- ============================================================================
-- USBD37 - Register a new wagon in the database
-- ============================================================================
-- As a Planner, I want to register a new wagon in the database.
-- All supported gauges must also be registered.
--
-- Requirements:
-- 1. Register a new wagon (insert into Wagon table)
-- 2. Ensure all supported gauges for the VehicleModel are registered
--    (verify VehicleModel_Gauge has entries)
-- 3. String comparisons must be case and space insensitive for unique constraints
-- 4. No sequences (not covered in course)
-- 5. Proper transaction handling
-- ============================================================================

CREATE OR REPLACE FUNCTION RegisterWagon(
    p_wagon_id IN NUMBER,
    p_vehicle_model_id IN NUMBER,
    p_train_operator_name IN VARCHAR2
) RETURN NUMBER
IS
    v_train_operator_id NUMBER;
    v_gauge_count NUMBER;
    v_wagon_exists NUMBER;
    v_vehicle_model_exists NUMBER;
BEGIN
    -- Start transaction (implicit in PL/SQL, but we'll be explicit about rollback points)
    
    -- Validate input parameters
    IF p_wagon_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20010, 'Wagon ID cannot be NULL');
    END IF;
    
    IF p_vehicle_model_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20011, 'Vehicle Model ID cannot be NULL');
    END IF;
    
    IF p_train_operator_name IS NULL OR TRIM(p_train_operator_name) IS NULL THEN
        RAISE_APPLICATION_ERROR(-20012, 'Train Operator name cannot be NULL or empty');
    END IF;
    
    -- Check if wagon already exists
    SELECT COUNT(*)
    INTO v_wagon_exists
    FROM Wagon
    WHERE ID = p_wagon_id;
    
    IF v_wagon_exists > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 
            'Cannot register wagon: Wagon with ID ' || p_wagon_id || ' already exists.');
    END IF;
    
    -- Validate VehicleModel exists
    SELECT COUNT(*)
    INTO v_vehicle_model_exists
    FROM VehicleModel
    WHERE ID = p_vehicle_model_id;
    
    IF v_vehicle_model_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 
            'Cannot register wagon: VehicleModel with ID ' || p_vehicle_model_id || ' does not exist.');
    END IF;
    
    -- Find TrainOperator by name (case and space insensitive)
    -- Using UPPER and TRIM for case and space insensitive comparison
    -- SELECT INTO will raise NO_DATA_FOUND if no rows found
    BEGIN
        SELECT ID
        INTO v_train_operator_id
        FROM TrainOperator
        WHERE UPPER(TRIM(name)) = UPPER(TRIM(p_train_operator_name));
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20004, 
                'Cannot register wagon: TrainOperator with name ''' || p_train_operator_name || ''' does not exist.');
    END;
    
    -- Verify that VehicleModel has at least one gauge registered
    -- All supported gauges must be registered in VehicleModel_Gauge
    SELECT COUNT(*)
    INTO v_gauge_count
    FROM VehicleModel_Gauge
    WHERE VehicleModelID = p_vehicle_model_id;
    
    IF v_gauge_count = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 
            'Cannot register wagon: VehicleModel ' || p_vehicle_model_id || 
            ' has no gauges registered. All supported gauges must be registered in VehicleModel_Gauge.');
    END IF;
    
    -- All validations passed, insert the wagon
    INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID)
    VALUES (p_wagon_id, p_vehicle_model_id, v_train_operator_id);
    
    -- All validations passed, insert the wagon
    INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID)
    VALUES (p_wagon_id, p_vehicle_model_id, v_train_operator_id);
    
    -- Return success (1 = success, 0 = failure)
    RETURN 1;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Re-raise with context if it's not already an application error
        IF SQLCODE BETWEEN -20000 AND -20999 THEN
            RAISE;  -- Re-raise application errors as-is
        ELSE
            -- Wrap unexpected errors
            RAISE_APPLICATION_ERROR(-20099, 
                'Error registering wagon: ' || SQLERRM);
        END IF;
END RegisterWagon;
/


-- ============================================================================
-- USBD37 - Comprehensive Test Script for RegisterWagon Function
-- ============================================================================
-- This script tests the RegisterWagon function with various scenarios:
-- 1. Happy path (successful registration)
-- 2. Wagon already exists
-- 3. VehicleModel doesn't exist
-- 4. TrainOperator doesn't exist
-- 5. VehicleModel has no gauges registered
-- 6. NULL parameters
-- 7. Case and space insensitive TrainOperator name matching
-- 8. Multiple wagons with same VehicleModel
-- ============================================================================

-- ============================================================================
-- Test Setup: Verify test data exists (without inserting new data)
-- ============================================================================

DECLARE
    v_vehicle_model_count NUMBER;
    v_train_operator_count NUMBER;
    v_gauge_count NUMBER;
BEGIN
    -- Check if we have VehicleModels with gauges registered
    SELECT COUNT(DISTINCT VM.ID)
    INTO v_vehicle_model_count
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    );
    
    -- Check if we have TrainOperators
    SELECT COUNT(*)
    INTO v_train_operator_count
    FROM TrainOperator;
    
    -- Check if we have gauges
    SELECT COUNT(*)
    INTO v_gauge_count
    FROM Gauge;
    
    DBMS_OUTPUT.PUT_LINE('VehicleModels with gauges: ' || v_vehicle_model_count);
    DBMS_OUTPUT.PUT_LINE('TrainOperators: ' || v_train_operator_count);
    DBMS_OUTPUT.PUT_LINE('Gauges: ' || v_gauge_count);
    
    IF v_vehicle_model_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: No VehicleModels with gauges found. Some tests may fail.');
    END IF;
    
    IF v_train_operator_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: No TrainOperators found. Some tests may fail.');
    END IF;
END;
/

-- ============================================================================
-- Test Case 1: Happy Path - Successful wagon registration
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999991;
    v_vehicle_model_id NUMBER;       
    v_train_operator_name VARCHAR2(255);
    v_wagon_exists NUMBER;
BEGIN
    -- Get a valid VehicleModel with gauges
    SELECT VM.ID
    INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    )
    AND ROWNUM = 1;
    
    -- Get a valid TrainOperator name
    SELECT name
    INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;
    
    -- Ensure wagon doesn't exist
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;
    
    DBMS_OUTPUT.PUT_LINE('Attempting to register wagon:');
    DBMS_OUTPUT.PUT_LINE('  Wagon ID: ' || v_wagon_id);
    DBMS_OUTPUT.PUT_LINE('  VehicleModel ID: ' || v_vehicle_model_id);
    DBMS_OUTPUT.PUT_LINE('  TrainOperator: ' || v_train_operator_name);
    
    -- Register the wagon
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);
    
    IF v_result = 1 THEN
        -- Verify wagon was inserted
        SELECT COUNT(*)
        INTO v_wagon_exists
        FROM Wagon
        WHERE ID = v_wagon_id;
        
        IF v_wagon_exists = 1 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Wagon registered successfully!');
        ELSE
            DBMS_OUTPUT.PUT_LINE('ERROR: Wagon was not inserted into database.');
        END IF;
        
        -- Cleanup
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Test data rolled back.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Function returned failure code: ' || v_result);
        ROLLBACK;
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 2: Wagon already exists
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER;
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
BEGIN
    -- Get an existing wagon
    SELECT W.ID, W.VehicleModelID, "TO".name
    INTO v_wagon_id, v_vehicle_model_id, v_train_operator_name
    FROM Wagon W
    JOIN TrainOperator "TO" ON W.TrainOperatorID = "TO".ID
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Attempting to register existing wagon:');
    DBMS_OUTPUT.PUT_LINE('  Wagon ID: ' || v_wagon_id);
    
    -- Try to register the same wagon again
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20002 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected duplicate wagon ID.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 3: VehicleModel doesn't exist
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999992;
    v_vehicle_model_id NUMBER := 9999999; -- Non-existent ID
    v_train_operator_name VARCHAR2(255);
BEGIN
    -- Get a valid TrainOperator
    SELECT name
    INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Attempting to register wagon with non-existent VehicleModel:');
    DBMS_OUTPUT.PUT_LINE('  VehicleModel ID: ' || v_vehicle_model_id);
    
    -- Try to register wagon with non-existent VehicleModel
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20003 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent VehicleModel.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 4: TrainOperator doesn't exist
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999993;
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255) := 'NonExistentOperator12345';
BEGIN
    -- Get a valid VehicleModel with gauges
    SELECT VM.ID
    INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    )
    AND ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Attempting to register wagon with non-existent TrainOperator:');
    DBMS_OUTPUT.PUT_LINE('  TrainOperator: ' || v_train_operator_name);
    
    -- Try to register wagon with non-existent TrainOperator
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception!');
    ROLLBACK;
    
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20004 OR SQLCODE = -20005 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent TrainOperator.');
            DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 5: VehicleModel has no gauges registered
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999994;
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
    v_gauge_count NUMBER;
BEGIN
    -- Find a VehicleModel without gauges (if any exist)
    BEGIN
        SELECT VM.ID
        INTO v_vehicle_model_id
        FROM VehicleModel VM
        WHERE NOT EXISTS (
            SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
        )
        AND ROWNUM = 1;
        
        -- Get a valid TrainOperator
        SELECT name
        INTO v_train_operator_name
        FROM TrainOperator
        WHERE ROWNUM = 1;
        
        DBMS_OUTPUT.PUT_LINE('Attempting to register wagon with VehicleModel without gauges:');
        DBMS_OUTPUT.PUT_LINE('  VehicleModel ID: ' || v_vehicle_model_id);
        
        -- Verify it has no gauges
        SELECT COUNT(*)
        INTO v_gauge_count
        FROM VehicleModel_Gauge
        WHERE VehicleModelID = v_vehicle_model_id;
        
        DBMS_OUTPUT.PUT_LINE('  Gauges registered: ' || v_gauge_count);
        
        -- Try to register wagon
        v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);
        
        DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception!');
        ROLLBACK;
        
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            DBMS_OUTPUT.PUT_LINE('SKIPPED: No VehicleModel without gauges found in database.');
            DBMS_OUTPUT.PUT_LINE('This test requires a VehicleModel with no gauges to be meaningful.');
        WHEN OTHERS THEN
            IF SQLCODE = -20001 THEN
                DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected VehicleModel without gauges.');
                DBMS_OUTPUT.PUT_LINE('Error message: ' || SQLERRM);
            ELSE
                DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
            END IF;
            ROLLBACK;
    END;
END;
/

-- ============================================================================
-- Test Case 6: NULL parameters
-- ============================================================================

-- Test NULL Wagon ID
DECLARE
    v_result NUMBER;
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
BEGIN
    SELECT VM.ID
    INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    )
    AND ROWNUM = 1;
    
    SELECT name
    INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Testing NULL Wagon ID...');
    v_result := RegisterWagon(NULL, v_vehicle_model_id, v_train_operator_name);
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL Wagon ID');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20010 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Wagon ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- Test NULL VehicleModel ID
DECLARE
    v_result NUMBER;
    v_train_operator_name VARCHAR2(255);
BEGIN
    SELECT name
    INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Testing NULL VehicleModel ID...');
    v_result := RegisterWagon(999995, NULL, v_train_operator_name);
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL VehicleModel ID');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20011 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL VehicleModel ID.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- Test NULL TrainOperator name
DECLARE
    v_result NUMBER;
    v_vehicle_model_id NUMBER;
BEGIN
    SELECT VM.ID
    INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    )
    AND ROWNUM = 1;
    
    DBMS_OUTPUT.PUT_LINE('Testing NULL TrainOperator name...');
    v_result := RegisterWagon(999996, v_vehicle_model_id, NULL);
    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL TrainOperator name');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20012 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL TrainOperator name.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('UNEXPECTED ERROR: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 7: Case and space insensitive TrainOperator name matching
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999997;
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
    v_train_operator_name_variations VARCHAR2(255);
    v_wagon_exists NUMBER;
BEGIN
    -- Get a valid VehicleModel with gauges
    SELECT VM.ID
    INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    )
    AND ROWNUM = 1;
    
    -- Get a valid TrainOperator name
    SELECT name
    INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;
    
    -- Test with uppercase
    v_train_operator_name_variations := UPPER(v_train_operator_name);
    DBMS_OUTPUT.PUT_LINE('Testing with UPPERCASE: ' || v_train_operator_name_variations);
    
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;
    
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name_variations);
    
    IF v_result = 1 THEN
        SELECT COUNT(*)
        INTO v_wagon_exists
        FROM Wagon
        WHERE ID = v_wagon_id;
        
        IF v_wagon_exists = 1 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Case insensitive matching works!');
        END IF;
        ROLLBACK;
    END IF;
    
    -- Test with lowercase
    v_wagon_id := 999998;
    v_train_operator_name_variations := LOWER(v_train_operator_name);
    DBMS_OUTPUT.PUT_LINE('Testing with lowercase: ' || v_train_operator_name_variations);
    
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;
    
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name_variations);
    
    IF v_result = 1 THEN
        SELECT COUNT(*)
        INTO v_wagon_exists
        FROM Wagon
        WHERE ID = v_wagon_id;
        
        IF v_wagon_exists = 1 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Case insensitive matching works!');
        END IF;
        ROLLBACK;
    END IF;
    
    -- Test with spaces
    v_wagon_id := 999999;
    v_train_operator_name_variations := '  ' || v_train_operator_name || '  ';
    DBMS_OUTPUT.PUT_LINE('Testing with spaces: "' || v_train_operator_name_variations || '"');
    
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;
    
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name_variations);
    
    IF v_result = 1 THEN
        SELECT COUNT(*)
        INTO v_wagon_exists
        FROM Wagon
        WHERE ID = v_wagon_id;
        
        IF v_wagon_exists = 1 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Space insensitive matching works!');
        END IF;
        ROLLBACK;
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 8: Multiple wagons with same VehicleModel
-- ============================================================================

DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999990;
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
    v_wagon_count NUMBER;
    v_wagon_exists NUMBER;
BEGIN
    -- Get a valid VehicleModel with gauges
    SELECT VM.ID
    INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    )
    AND ROWNUM = 1;
    
    -- Get a valid TrainOperator
    SELECT name
    INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;
    
    -- Count existing wagons with this VehicleModel
    SELECT COUNT(*)
    INTO v_wagon_count
    FROM Wagon
    WHERE VehicleModelID = v_vehicle_model_id;
    
    DBMS_OUTPUT.PUT_LINE('Existing wagons with VehicleModel ' || v_vehicle_model_id || ': ' || v_wagon_count);
    DBMS_OUTPUT.PUT_LINE('Registering another wagon with the same VehicleModel...');
    
    -- Ensure test wagon doesn't exist
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;
    
    -- Register the wagon
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);
    
    IF v_result = 1 THEN
        -- Verify wagon was inserted
        SELECT COUNT(*)
        INTO v_wagon_exists
        FROM Wagon
        WHERE ID = v_wagon_id;
        
        IF v_wagon_exists = 1 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Multiple wagons with same VehicleModel allowed!');
        END IF;
        
        ROLLBACK;
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Function returned failure code: ' || v_result);
        ROLLBACK;
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Summary:
-- - All test cases executed
-- - Successful tests show "SUCCESS" messages via DBMS_OUTPUT
-- - Failed tests show "FAILED" or "ERROR" messages via DBMS_OUTPUT
-- - All test data has been rolled back (no permanent changes)
-- ============================================================================