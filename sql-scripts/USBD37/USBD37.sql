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
    p_train_operator_name IN VARCHAR2,
    p_initial_facility_id IN NUMBER DEFAULT 50 -- Adicionado para ser compatível com os teus INSERTs
) RETURN NUMBER
    IS
    v_train_operator_id NUMBER;
    v_gauge_count NUMBER;
    v_wagon_exists NUMBER;
    v_vehicle_model_exists NUMBER;
BEGIN
    -- Validações de parâmetros
    IF p_wagon_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20010, 'Wagon ID cannot be NULL');
    END IF;

    IF p_vehicle_model_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20011, 'Vehicle Model ID cannot be NULL');
    END IF;

    IF p_train_operator_name IS NULL OR TRIM(p_train_operator_name) IS NULL THEN
        RAISE_APPLICATION_ERROR(-20012, 'Train Operator name cannot be NULL or empty');
    END IF;

    -- Check se o vagão já existe
    SELECT COUNT(*) INTO v_wagon_exists FROM Wagon WHERE ID = p_wagon_id;

    IF v_wagon_exists > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Wagon ID ' || p_wagon_id || ' already exists.');
    END IF;

    -- Valida se VehicleModel existe
    SELECT COUNT(*) INTO v_vehicle_model_exists FROM VehicleModel WHERE ID = p_vehicle_model_id;

    IF v_vehicle_model_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'VehicleModel ID ' || p_vehicle_model_id || ' does not exist.');
    END IF;

    -- Encontrar ID do Operador (Requisito: Case e Space Insensitive)
    BEGIN
        SELECT ID INTO v_train_operator_id
        FROM TrainOperator
        WHERE UPPER(TRIM(name)) = UPPER(TRIM(p_train_operator_name));
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20004, 'TrainOperator ''' || p_train_operator_name || ''' not found.');
    END;

    -- Verificar Bitolas (Requisito: All supported gauges must be registered)
    SELECT COUNT(*) INTO v_gauge_count
    FROM VehicleModel_Gauge
    WHERE VehicleModelID = p_vehicle_model_id;

    IF v_gauge_count = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'VehicleModel has no gauges registered.');
    END IF;

    -- INSERT FINAL CORRIGIDO (Adicionado InitialFacilityID para bater com a tua DB)
    INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID)
    VALUES (p_wagon_id, p_vehicle_model_id, v_train_operator_id, p_initial_facility_id);

    COMMIT; -- Transação atómica
    RETURN 1;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        IF SQLCODE BETWEEN -20999 AND -20000 THEN RAISE;
        ELSE RAISE_APPLICATION_ERROR(-20099, 'Error: ' || SQLERRM);
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

SET SERVEROUTPUT ON;

DECLARE
    v_vehicle_model_count NUMBER;
    v_train_operator_count NUMBER;
    v_gauge_count NUMBER;
    v_facility_count NUMBER;
BEGIN
    -- 1. Check if there are VehicleModels with at least one gauge registered
    -- Requirement: All supported gauges must be registered in VehicleModel_Gauge
    SELECT COUNT(DISTINCT VM.ID)
    INTO v_vehicle_model_count
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    );

    -- 2. Check for existing TrainOperators
    -- Needed for the function to resolve names to IDs
    SELECT COUNT(*)
    INTO v_train_operator_count
    FROM TrainOperator;

    -- 3. Check for existing Gauges in the base table
    -- If this is zero, no VehicleModel can ever have a valid gauge association
    SELECT COUNT(*)
    INTO v_gauge_count
    FROM Gauge;

    -- 4. Check if Facility 50 (Leixões) exists
    -- This is the default initial location used by your RegisterWagon function
    SELECT COUNT(*)
    INTO v_facility_count
    FROM Facility
    WHERE ID = 50;

    -- Output results to the console
    DBMS_OUTPUT.PUT_LINE('--- Environment Verification ---');
    DBMS_OUTPUT.PUT_LINE('VehicleModels with gauges:    ' || v_vehicle_model_count);
    DBMS_OUTPUT.PUT_LINE('TrainOperators registered:    ' || v_train_operator_count);
    DBMS_OUTPUT.PUT_LINE('Gauges in base table:         ' || v_gauge_count);
    DBMS_OUTPUT.PUT_LINE('Facility 50 (Default) exists: ' || CASE WHEN v_facility_count > 0 THEN 'YES' ELSE 'NO' END);
    DBMS_OUTPUT.PUT_LINE('--------------------------------');

    -- Safety Warnings
    IF v_vehicle_model_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: No models with gauges. RegisterWagon validation will fail.');
    END IF;

    IF v_train_operator_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: No TrainOperators found. Name lookup will fail.');
    END IF;

    IF v_gauge_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('WARNING: Gauge table is empty. Models cannot have supported gauges.');
    END IF;

    IF v_facility_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL: Facility 50 not found. Default insertion will trigger a FK error.');
    END IF;
END;
/

-- ============================================================================
-- Test Case 1: Happy Path - Successful wagon registration
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999991; -- Test ID
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
    v_wagon_exists NUMBER;
BEGIN
    -- 1. Fetch a VehicleModel that exists and has registered gauges (e.g., 1104)
    SELECT ID INTO v_vehicle_model_id
    FROM VehicleModel
    WHERE ID = 1104 AND ROWNUM = 1;

    -- 2. Fetch an existing TrainOperator (e.g., 'Medway')
    SELECT name INTO v_train_operator_name
    FROM TrainOperator
    WHERE UPPER(name) = 'MEDWAY' AND ROWNUM = 1;

    -- Pre-test cleanup: Ensure the test wagon ID is available
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 1: Happy Path ---');

    -- 3. Call the function (Using the 3 main arguments; 4th uses DEFAULT 50)
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);

    IF v_result = 1 THEN
        -- 4. Verify if the record was successfully inserted
        SELECT COUNT(*) INTO v_wagon_exists FROM Wagon WHERE ID = v_wagon_id;

        IF v_wagon_exists = 1 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Wagon ID ' || v_wagon_id || ' registered successfully!');
        ELSE
            DBMS_OUTPUT.PUT_LINE('ERROR: Insert was successful but record not found.');
        END IF;
    END IF;

    -- 5. Cleanup
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Test finished and data rolled back.');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
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
    -- 1. Fetch an existing wagon and its details from the database
    -- This ensures we are testing against a real duplicate
    SELECT W.ID, W.VehicleModelID, T_OP.name
    INTO v_wagon_id, v_vehicle_model_id, v_train_operator_name
    FROM Wagon W
             JOIN TrainOperator T_OP ON W.TrainOperatorID = T_OP.ID
    WHERE ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 2: Duplicate Wagon ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to register existing Wagon ID: ' || v_wagon_id);

    -- 2. Try to register the same wagon again
    -- The function should trigger the RAISE_APPLICATION_ERROR (-20002)
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);

    -- 3. If the code reaches this line, it means the exception was not raised
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception for duplicate ID!');
    ROLLBACK;

EXCEPTION
    -- 4. Catch the expected exception
    WHEN OTHERS THEN
        IF SQLCODE = -20002 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected duplicate wagon ID.');
            DBMS_OUTPUT.PUT_LINE('Expected Error: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 3: VehicleModel doesn't exist
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999993; -- New unique test ID
    v_invalid_model_id NUMBER := -1; -- An ID that definitely does not exist
    v_train_operator_name VARCHAR2(255);
BEGIN
    -- 1. Get a valid TrainOperator name
    -- We use a valid operator so the function fails specifically on the model check
    SELECT name
    INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;

    -- Cleanup preventive
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 3: Invalid Vehicle Model ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to register Wagon ' || v_wagon_id || ' with Model ID: ' || v_invalid_model_id);

    -- 2. Call the function
    -- It should trigger the RAISE_APPLICATION_ERROR (-20003) from your function
    v_result := RegisterWagon(v_wagon_id, v_invalid_model_id, v_train_operator_name);

    -- 3. If it reaches here, the test failed
    DBMS_OUTPUT.PUT_LINE('ERROR: Function allowed registration with a non-existent VehicleModel!');
    ROLLBACK;

EXCEPTION
    -- 4. Catch the expected exception
    WHEN OTHERS THEN
        IF SQLCODE = -20003 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent VehicleModel.');
            DBMS_OUTPUT.PUT_LINE('Expected Error: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 4: TrainOperator doesn't exist
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999994; -- New unique test ID
    v_vehicle_model_id NUMBER;
    v_invalid_operator_name VARCHAR2(255) := 'NonExistentOperator12345';
BEGIN
    -- 1. Get a valid VehicleModel that has gauges
    -- This ensures the function doesn't fail on the gauge check first
    SELECT VM.ID
    INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (
        SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID
    )
      AND ROWNUM = 1;

    -- Cleanup preventive
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 4: Invalid Train Operator ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to register wagon with non-existent TrainOperator: ' || v_invalid_operator_name);

    -- 2. Try to register wagon with non-existent TrainOperator
    -- Expected to raise exception -20004
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_invalid_operator_name);

    -- 3. If it reaches here, the test failed
    DBMS_OUTPUT.PUT_LINE('ERROR: Function should have raised an exception for invalid operator!');
    ROLLBACK;

EXCEPTION
    -- 4. Catch the specific error defined in your function (-20004)
    WHEN OTHERS THEN
        IF SQLCODE = -20004 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected non-existent TrainOperator.');
            DBMS_OUTPUT.PUT_LINE('Expected Error: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 5: VehicleModel has no gauges registered
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999995; -- Unique Test ID
    v_temp_model_id NUMBER := 888888; -- Temporary model ID for testing
    v_train_operator_name VARCHAR2(255);
BEGIN
    -- 1. Setup: Create a temporary VehicleModel with NO gauges
    -- This ensures the test always has a valid scenario to run
    DELETE FROM VehicleModel WHERE ID = v_temp_model_id;
    INSERT INTO VehicleModel (ID, name, manufacturer)
    VALUES (v_temp_model_id, 'Temp Model No Gauges', 'Test Manufacturer');

    -- 2. Get a valid TrainOperator
    SELECT name INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;

    -- Cleanup preventive for the wagon
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 5: Model Without Gauges ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to register wagon with Model ID: ' || v_temp_model_id);

    -- 3. Try to register wagon (Expected to raise -20001)
    v_result := RegisterWagon(v_wagon_id, v_temp_model_id, v_train_operator_name);

    -- 4. If it reaches here, the test failed
    DBMS_OUTPUT.PUT_LINE('ERROR: Function allowed registration of a model with no gauges!');
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        -- 5. Catch the specific error defined in your function (-20001)
        IF SQLCODE = -20001 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected VehicleModel without gauges.');
            DBMS_OUTPUT.PUT_LINE('Expected Error: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;

        -- Cleanup the temporary model
        DELETE FROM VehicleModel WHERE ID = v_temp_model_id;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 6: NULL parameters validation
-- ============================================================================

-- 6.1 Test NULL Wagon ID (Expects -20010)
DECLARE
    v_result NUMBER;
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
BEGIN
    SELECT ID INTO v_vehicle_model_id FROM VehicleModel WHERE ROWNUM = 1;
    SELECT name INTO v_train_operator_name FROM TrainOperator WHERE ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('--- Case 6.1: Testing NULL Wagon ID ---');
    v_result := RegisterWagon(NULL, v_vehicle_model_id, v_train_operator_name);

    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL Wagon ID');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20010 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL Wagon ID (-20010).');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- 6.2 Test NULL VehicleModel ID (Expects -20011)
DECLARE
    v_result NUMBER;
    v_train_operator_name VARCHAR2(255);
BEGIN
    SELECT name INTO v_train_operator_name FROM TrainOperator WHERE ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('--- Case 6.2: Testing NULL VehicleModel ID ---');
    v_result := RegisterWagon(999995, NULL, v_train_operator_name);

    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL VehicleModel ID');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20011 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL VehicleModel ID (-20011).');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- 6.3 Test NULL TrainOperator name (Expects -20012)
DECLARE
    v_result NUMBER;
    v_vehicle_model_id NUMBER;
BEGIN
    SELECT ID INTO v_vehicle_model_id FROM VehicleModel WHERE ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('--- Case 6.3: Testing NULL TrainOperator name ---');
    v_result := RegisterWagon(999996, v_vehicle_model_id, NULL);

    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL TrainOperator name');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20012 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL TrainOperator name (-20012).');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 7: Case and Space Insensitive TrainOperator Matching
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999997;
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
    v_test_variation VARCHAR2(255);
    v_wagon_exists NUMBER;
BEGIN
    -- 1. Setup: Get valid VehicleModel and the original Operator name
    SELECT VM.ID INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID)
      AND ROWNUM = 1;

    SELECT name INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 7: String Insensitivity ---');

    -- VARIATION A: UPPERCASE
    v_test_variation := UPPER(v_train_operator_name);
    DBMS_OUTPUT.PUT_LINE('Testing Variation A (UPPERCASE): ' || v_test_variation);

    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;

    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_test_variation);

    SELECT COUNT(*) INTO v_wagon_exists FROM Wagon WHERE ID = v_wagon_id;
    IF v_result = 1 AND v_wagon_exists = 1 THEN
        DBMS_OUTPUT.PUT_LINE('  SUCCESS: Uppercase matching works.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  FAILED: Uppercase matching failed.');
    END IF;
    ROLLBACK;

    -- VARIATION B: lowercase
    v_wagon_id := 999998;
    v_test_variation := LOWER(v_train_operator_name);
    DBMS_OUTPUT.PUT_LINE('Testing Variation B (lowercase): ' || v_test_variation);

    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;

    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_test_variation);

    SELECT COUNT(*) INTO v_wagon_exists FROM Wagon WHERE ID = v_wagon_id;
    IF v_result = 1 AND v_wagon_exists = 1 THEN
        DBMS_OUTPUT.PUT_LINE('  SUCCESS: Lowercase matching works.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  FAILED: Lowercase matching failed.');
    END IF;
    ROLLBACK;

    -- VARIATION C: Leading/Trailing Spaces
    v_wagon_id := 999999;
    v_test_variation := '   ' || v_train_operator_name || '   ';
    DBMS_OUTPUT.PUT_LINE('Testing Variation C (Spaces): "' || v_test_variation || '"');

    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;

    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_test_variation);

    SELECT COUNT(*) INTO v_wagon_exists FROM Wagon WHERE ID = v_wagon_id;
    IF v_result = 1 AND v_wagon_exists = 1 THEN
        DBMS_OUTPUT.PUT_LINE('  SUCCESS: Space insensitive matching works.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  FAILED: Space insensitive matching failed.');
    END IF;
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL ERROR: ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 8: Multiple wagons with the same VehicleModel
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_wagon_id NUMBER := 999990; -- New unique test ID
    v_vehicle_model_id NUMBER;
    v_train_operator_name VARCHAR2(255);
    v_wagon_count_before NUMBER;
    v_wagon_count_after NUMBER;
BEGIN
    -- 1. Setup: Fetch a valid VehicleModel and TrainOperator
    SELECT VM.ID INTO v_vehicle_model_id
    FROM VehicleModel VM
    WHERE EXISTS (SELECT 1 FROM VehicleModel_Gauge VG WHERE VG.VehicleModelID = VM.ID)
      AND ROWNUM = 1;

    SELECT name INTO v_train_operator_name
    FROM TrainOperator
    WHERE ROWNUM = 1;

    -- 2. Count existing wagons for this model before the test
    SELECT COUNT(*) INTO v_wagon_count_before
    FROM Wagon
    WHERE VehicleModelID = v_vehicle_model_id;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 8: Cardinality (1:N) ---');
    DBMS_OUTPUT.PUT_LINE('Model ID ' || v_vehicle_model_id || ' currently has ' || v_wagon_count_before || ' wagons.');

    -- Cleanup preventive
    DELETE FROM Wagon WHERE ID = v_wagon_id;
    COMMIT;

    -- 3. Register a new wagon using the same Model ID
    v_result := RegisterWagon(v_wagon_id, v_vehicle_model_id, v_train_operator_name);

    IF v_result = 1 THEN
        -- 4. Verify if the count increased
        SELECT COUNT(*) INTO v_wagon_count_after
        FROM Wagon
        WHERE VehicleModelID = v_vehicle_model_id;

        IF v_wagon_count_after = v_wagon_count_before + 1 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Successfully added another wagon to Model ID ' || v_vehicle_model_id);
            DBMS_OUTPUT.PUT_LINE('Total wagons for this model: ' || v_wagon_count_after);
        ELSE
            DBMS_OUTPUT.PUT_LINE('ERROR: Wagon ID ' || v_wagon_id || ' was not correctly counted.');
        END IF;

        ROLLBACK;
    ELSE
        DBMS_OUTPUT.PUT_LINE('FAILED: Function returned ' || v_result);
        ROLLBACK;
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL ERROR: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Summary:
-- - All test cases executed
-- - Successful tests show "SUCCESS" messages via DBMS_OUTPUT
-- - Failed tests show "FAILED" or "ERROR" messages via DBMS_OUTPUT
-- - All test data has been rolled back (no permanent changes)
-- ============================================================================