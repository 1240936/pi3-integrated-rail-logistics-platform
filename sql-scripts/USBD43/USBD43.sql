-- ============================================================================
-- USBD43 - Add a new Electric locomotive model
-- ============================================================================
-- As a Planner, I want to add a new Electric locomotive model.
--
-- Requirements:
-- 1. Register a new electric locomotive model (insert into VehicleModel, LocomotiveSpecs, ElectricalType tables)
-- 2. Register supported gauges for the model (insert into VehicleModel_Gauge)
-- 3. Database must enforce uniqueness of model IDs and names
-- 4. Referential integrity must be maintained between locomotive models and gauge tables
-- 5. Must prevent duplicate registrations
-- 6. Implementation must use Oracle DBMS features (foreign keys, PL/SQL procedures)
-- 7. Validation queries must confirm that the new electric locomotive model is correctly registered
-- ============================================================================

CREATE OR REPLACE PROCEDURE ADD_ELECTRIC_LOCOMOTIVE_MODEL(
    p_model_id IN NUMBER,
    p_model_name IN VARCHAR2,
    p_length IN NUMBER,
    p_width IN NUMBER,
    p_height IN NUMBER,
    p_tare IN NUMBER,
    p_year_eis IN NUMBER,
    p_make IN VARCHAR2,
    p_power IN NUMBER,
    p_acceleration IN NUMBER,
    p_operational_speed IN NUMBER,
    p_max_speed IN NUMBER,
    p_number_of_wheels IN NUMBER,
    p_frequency IN NUMBER,
    p_voltage IN NUMBER,
    p_gauge_ids IN VARCHAR2
)
AS
    v_count         NUMBER;
    v_gauge_id      NUMBER;
    v_pos           NUMBER := 1;
    v_next_pos      NUMBER;
    v_current_gauge VARCHAR2(100);
BEGIN
    -- Validate model name is not null or empty
    IF p_model_name IS NULL OR TRIM(p_model_name) = '' THEN
        RAISE_APPLICATION_ERROR(-20004, 'Model name cannot be null or empty.');
    END IF;

    -- Check if model ID already exists
    SELECT COUNT(*)
    INTO v_count
    FROM VehicleModel
    WHERE ID = p_model_id;

    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Model ID ' || p_model_id || ' already exists. Please choose a different ID.');
    END IF;

    -- Check if model name already exists (case-insensitive, space-insensitive)
    SELECT COUNT(*)
    INTO v_count
    FROM VehicleModel
    WHERE UPPER(TRIM(modelName)) = UPPER(TRIM(p_model_name));

    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Model name ''' || p_model_name ||
                                        ''' already exists. Please choose a different name.');
    END IF;

    -- Insert into VehicleModel
    INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
    VALUES (p_model_id, TRIM(p_model_name), p_length, p_width, p_height, p_tare, p_year_eis);

    -- Insert into LocomotiveSpecs
    INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
    VALUES (p_model_id, p_make, p_power, p_acceleration, p_operational_speed, p_max_speed, p_number_of_wheels);

    -- Insert into ElectricalType
    INSERT INTO ElectricalType (VehicleModelID, frequency, voltage)
    VALUES (p_model_id, p_frequency, p_voltage);

    -- Validate that gauge_ids is provided
    IF p_gauge_ids IS NULL OR TRIM(p_gauge_ids) = '' THEN
        RAISE_APPLICATION_ERROR(-20005, 'At least one gauge ID must be provided.');
    END IF;

    -- Parse and insert gauge IDs
    -- Start from position 1
    v_pos := 1;

    -- Loop through the string to find each gauge ID
    LOOP
        -- Find the next comma (or end of string)
        v_next_pos := INSTR(p_gauge_ids, ',', v_pos);

        -- If no comma found, we're at the last (or only) gauge ID
        IF v_next_pos = 0 THEN
            v_next_pos := LENGTH(p_gauge_ids) + 1;
        END IF;

        -- Extract the current gauge ID
        v_current_gauge := TRIM(SUBSTR(p_gauge_ids, v_pos, v_next_pos - v_pos));

        -- Only process non-empty gauge IDs
        IF v_current_gauge IS NOT NULL AND LENGTH(v_current_gauge) > 0 THEN
            -- Convert to number
            BEGIN
                v_gauge_id := TO_NUMBER(v_current_gauge);
            EXCEPTION
                WHEN VALUE_ERROR THEN
                    RAISE_APPLICATION_ERROR(-20003, 'Invalid gauge ID format: ' || v_current_gauge);
            END;

            -- Validate gauge ID exists
            SELECT COUNT(*)
            INTO v_count
            FROM Gauge
            WHERE ID = v_gauge_id;

            IF v_count = 0 THEN
                RAISE_APPLICATION_ERROR(-20003, 'Gauge ID ' || v_gauge_id || ' does not exist.');
            END IF;

            -- Insert into VehicleModel_Gauge
            INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID)
            VALUES (p_model_id, v_gauge_id);
        END IF;

        -- Move to the next position (after the comma)
        v_pos := v_next_pos + 1;

        -- Exit loop if we've processed the entire string
        EXIT WHEN v_pos > LENGTH(p_gauge_ids);
    END LOOP;

    COMMIT; -- Atomic transaction
    DBMS_OUTPUT.PUT_LINE('Electric locomotive model ' || p_model_id || ' (' || p_model_name || ') successfully added.');

EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
        -- Handle duplicate key errors (e.g., duplicate gauge entry)
        ROLLBACK;
        IF SQLERRM LIKE '%VEHICLEMODEL_GAUGE%' THEN
            RAISE_APPLICATION_ERROR(-20006, 'Gauge ID ' || v_gauge_id || ' is already associated with model ID ' ||
                                            p_model_id || '.');
        ELSE
            RAISE;
        END IF;
    WHEN OTHERS THEN
        ROLLBACK;
        IF SQLCODE BETWEEN -20999 AND -20000 THEN
            RAISE;
        ELSE
            RAISE_APPLICATION_ERROR(-20099, 'Error: ' || SQLERRM);
        END IF;
END ADD_ELECTRIC_LOCOMOTIVE_MODEL;
/


-- ============================================================================
-- USBD43 - Comprehensive Test Script for ADD_ELECTRIC_LOCOMOTIVE_MODEL Procedure
-- ============================================================================
-- This script tests the ADD_ELECTRIC_LOCOMOTIVE_MODEL procedure with various scenarios:
-- 1. Happy path (successful registration)
-- 2. Model ID already exists
-- 3. Model name already exists
-- 4. Invalid gauge ID
-- 5. NULL/empty model name
-- 6. Multiple gauges support
-- 7. Case and space insensitive model name matching
-- ============================================================================

-- ============================================================================
-- Test Case 1: Happy Path - Successful electric locomotive model registration
-- ============================================================================
DECLARE
    v_model_id          NUMBER := 9001; -- Test ID
    v_model_exists      NUMBER;
    v_specs_exists      NUMBER;
    v_electrical_exists NUMBER;
    v_gauge_exists      NUMBER;
BEGIN
    -- Cleanup: Delete test data if exists (in reverse order of dependencies)
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
    DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
    DELETE FROM VehicleModel WHERE ID = v_model_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 1: Happy Path ---');

    -- Call the procedure
    ADD_ELECTRIC_LOCOMOTIVE_MODEL(
            p_model_id => v_model_id,
            p_model_name => 'TestElectricModel',
            p_length => 20.5,
            p_width => 3.0,
            p_height => 4.4,
            p_tare => 92.0,
            p_year_eis => 2024,
            p_make => 'Siemens',
            p_power => 6000,
            p_acceleration => 0.6,
            p_operational_speed => 75,
            p_max_speed => 240,
            p_number_of_wheels => 8,
            p_frequency => 50,
            p_voltage => 25000,
            p_gauge_ids => '1'
    );

    -- Verify if the record was successfully inserted
    SELECT COUNT(*) INTO v_model_exists FROM VehicleModel WHERE ID = v_model_id;
    SELECT COUNT(*) INTO v_specs_exists FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
    SELECT COUNT(*) INTO v_electrical_exists FROM ElectricalType WHERE VehicleModelID = v_model_id;
    SELECT COUNT(*) INTO v_gauge_exists FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id AND GaugeID = 1;

    IF v_model_exists = 1 AND v_specs_exists = 1 AND v_electrical_exists = 1 AND v_gauge_exists = 1 THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Electric locomotive model ID ' || v_model_id || ' registered successfully!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Insert verification failed.');
        DBMS_OUTPUT.PUT_LINE('Model: ' || v_model_exists || ', Specs: ' || v_specs_exists || ', Electrical: ' ||
                             v_electrical_exists || ', Gauge: ' || v_gauge_exists);
    END IF;

    -- Cleanup
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
    DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
    DELETE FROM VehicleModel WHERE ID = v_model_id;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Test finished and data cleaned up.');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        ROLLBACK;
        -- Cleanup on error
        DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
        DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
        DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
        DELETE FROM VehicleModel WHERE ID = v_model_id;
        COMMIT;
END;
/

-- ============================================================================
-- Test Case 2: Model ID already exists
-- ============================================================================
DECLARE
    v_existing_model_id NUMBER;
    v_model_name        VARCHAR2(255);
BEGIN
    -- Get an existing model ID from the database
    SELECT ID, modelName
    INTO v_existing_model_id, v_model_name
    FROM VehicleModel
    WHERE EXISTS (SELECT 1
                  FROM LocomotiveSpecs ls
                           INNER JOIN ElectricalType et ON ls.VehicleModelID = et.VehicleModelID
                  WHERE ls.VehicleModelID = VehicleModel.ID)
      AND ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 2: Duplicate Model ID ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to register existing Model ID: ' || v_existing_model_id);

    -- Try to register the same model ID again
    ADD_ELECTRIC_LOCOMOTIVE_MODEL(
            p_model_id => v_existing_model_id,
            p_model_name => 'DifferentName',
            p_length => 21.0,
            p_width => 3.0,
            p_height => 4.4,
            p_tare => 93.0,
            p_year_eis => 2025,
            p_make => 'Alstom',
            p_power => 5500,
            p_acceleration => 0.55,
            p_operational_speed => 70,
            p_max_speed => 220,
            p_number_of_wheels => 8,
            p_frequency => 50,
            p_voltage => 25000,
            p_gauge_ids => '1'
    );

    DBMS_OUTPUT.PUT_LINE('ERROR: Procedure should have raised an exception for duplicate ID!');
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20001 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected duplicate model ID.');
            DBMS_OUTPUT.PUT_LINE('Expected Error: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 3: Model name already exists
-- ============================================================================
DECLARE
    v_new_model_id        NUMBER := 9002;
    v_existing_model_name VARCHAR2(255);
BEGIN
    -- Get an existing model name from the database
    SELECT modelName
    INTO v_existing_model_name
    FROM VehicleModel
    WHERE ROWNUM = 1;

    -- Cleanup preventive
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_new_model_id;
    DELETE FROM ElectricalType WHERE VehicleModelID = v_new_model_id;
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_new_model_id;
    DELETE FROM VehicleModel WHERE ID = v_new_model_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 3: Duplicate Model Name ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to register existing Model Name: ' || v_existing_model_name);

    -- Try to register with the same model name
    ADD_ELECTRIC_LOCOMOTIVE_MODEL(
            p_model_id => v_new_model_id,
            p_model_name => v_existing_model_name,
            p_length => 21.0,
            p_width => 3.0,
            p_height => 4.4,
            p_tare => 93.0,
            p_year_eis => 2025,
            p_make => 'Alstom',
            p_power => 5500,
            p_acceleration => 0.55,
            p_operational_speed => 70,
            p_max_speed => 220,
            p_number_of_wheels => 8,
            p_frequency => 50,
            p_voltage => 25000,
            p_gauge_ids => '1'
    );

    DBMS_OUTPUT.PUT_LINE('ERROR: Procedure should have raised an exception for duplicate name!');
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20002 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected duplicate model name.');
            DBMS_OUTPUT.PUT_LINE('Expected Error: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
        -- Cleanup
        DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_new_model_id;
        DELETE FROM ElectricalType WHERE VehicleModelID = v_new_model_id;
        DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_new_model_id;
        DELETE FROM VehicleModel WHERE ID = v_new_model_id;
        COMMIT;
END;
/

-- ============================================================================
-- Test Case 4: Invalid gauge ID
-- ============================================================================
DECLARE
    v_model_id         NUMBER := 9003;
    v_invalid_gauge_id NUMBER := 999; -- An ID that definitely does not exist
BEGIN
    -- Cleanup preventive
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
    DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
    DELETE FROM VehicleModel WHERE ID = v_model_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 4: Invalid Gauge ID ---');
    DBMS_OUTPUT.PUT_LINE('Attempting to register model with non-existent Gauge ID: ' || v_invalid_gauge_id);

    -- Try to register with invalid gauge ID
    ADD_ELECTRIC_LOCOMOTIVE_MODEL(
            p_model_id => v_model_id,
            p_model_name => 'TestModelInvalidGauge',
            p_length => 20.0,
            p_width => 3.0,
            p_height => 4.3,
            p_tare => 90.0,
            p_year_eis => 2024,
            p_make => 'Siemens',
            p_power => 5500,
            p_acceleration => 0.55,
            p_operational_speed => 70,
            p_max_speed => 220,
            p_number_of_wheels => 8,
            p_frequency => 50,
            p_voltage => 25000,
            p_gauge_ids => TO_CHAR(v_invalid_gauge_id)
    );

    DBMS_OUTPUT.PUT_LINE('ERROR: Procedure should have raised an exception for invalid gauge!');
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20003 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected invalid gauge ID.');
            DBMS_OUTPUT.PUT_LINE('Expected Error: ' || SQLERRM);
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error code: ' || SQLCODE || ' - ' || SQLERRM);
        END IF;
        ROLLBACK;
        -- Cleanup
        DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
        DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
        DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
        DELETE FROM VehicleModel WHERE ID = v_model_id;
        COMMIT;
END;
/

-- ============================================================================
-- Test Case 5: NULL/empty model name
-- ============================================================================
-- 5.1 Test NULL model name (Expects -20004)
DECLARE
    v_model_id NUMBER := 9004;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 5.1: NULL Model Name ---');

    ADD_ELECTRIC_LOCOMOTIVE_MODEL(
            p_model_id => v_model_id,
            p_model_name => NULL,
            p_length => 20.0,
            p_width => 3.0,
            p_height => 4.3,
            p_tare => 90.0,
            p_year_eis => 2024,
            p_make => 'Siemens',
            p_power => 5500,
            p_acceleration => 0.55,
            p_operational_speed => 70,
            p_max_speed => 220,
            p_number_of_wheels => 8,
            p_frequency => 50,
            p_voltage => 25000,
            p_gauge_ids => '1'
    );

    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for NULL model name');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20004 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected NULL model name (-20004).');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- 5.2 Test empty model name (Expects -20004)
DECLARE
    v_model_id NUMBER := 9005;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 5.2: Empty Model Name ---');

    ADD_ELECTRIC_LOCOMOTIVE_MODEL(
            p_model_id => v_model_id,
            p_model_name => '',
            p_length => 20.0,
            p_width => 3.0,
            p_height => 4.3,
            p_tare => 90.0,
            p_year_eis => 2024,
            p_make => 'Siemens',
            p_power => 5500,
            p_acceleration => 0.55,
            p_operational_speed => 70,
            p_max_speed => 220,
            p_number_of_wheels => 8,
            p_frequency => 50,
            p_voltage => 25000,
            p_gauge_ids => '1'
    );

    DBMS_OUTPUT.PUT_LINE('ERROR: Should have raised exception for empty model name');
    ROLLBACK;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -20004 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Correctly rejected empty model name (-20004).');
        ELSE
            DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        END IF;
        ROLLBACK;
END;
/

-- ============================================================================
-- Test Case 6: Model with multiple gauges
-- ============================================================================
DECLARE
    v_model_id     NUMBER := 9006;
    v_model_exists NUMBER;
    v_gauge_count  NUMBER;
BEGIN
    -- Cleanup preventive
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
    DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
    DELETE FROM VehicleModel WHERE ID = v_model_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 6: Multiple Gauges ---');

    -- Call the procedure with multiple gauge IDs
    ADD_ELECTRIC_LOCOMOTIVE_MODEL(
            p_model_id => v_model_id,
            p_model_name => 'MultiGaugeElectricModel',
            p_length => 21.0,
            p_width => 3.0,
            p_height => 4.4,
            p_tare => 95.0,
            p_year_eis => 2024,
            p_make => 'Bombardier',
            p_power => 6500,
            p_acceleration => 0.65,
            p_operational_speed => 80,
            p_max_speed => 250,
            p_number_of_wheels => 8,
            p_frequency => 50,
            p_voltage => 25000,
            p_gauge_ids => '1,2' -- Multiple gauges
    );

    -- Verify gauge associations (check if at least 2 gauges exist, assuming gauges 1 and 2 exist)
    SELECT COUNT(*)
    INTO v_gauge_count
    FROM VehicleModel_Gauge
    WHERE VehicleModelID = v_model_id;

    SELECT COUNT(*) INTO v_model_exists FROM VehicleModel WHERE ID = v_model_id;

    IF v_model_exists = 1 AND v_gauge_count >= 2 THEN
        DBMS_OUTPUT.PUT_LINE('SUCCESS: Model with multiple gauges registered successfully!');
        DBMS_OUTPUT.PUT_LINE('Number of gauges associated: ' || v_gauge_count);
    ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR: Multiple gauge registration verification failed.');
        DBMS_OUTPUT.PUT_LINE('Model: ' || v_model_exists || ', Gauges: ' || v_gauge_count);
    END IF;

    -- Cleanup
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
    DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
    DELETE FROM VehicleModel WHERE ID = v_model_id;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Test finished and data cleaned up.');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FAILED: Unexpected error: ' || SQLERRM);
        ROLLBACK;
        -- Cleanup on error
        DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
        DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
        DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
        DELETE FROM VehicleModel WHERE ID = v_model_id;
        COMMIT;
END;
/

-- ============================================================================
-- Test Case 7: Case and Space Insensitive Model Name Matching
-- ============================================================================
DECLARE
    v_model_id            NUMBER := 9007;
    v_existing_model_name VARCHAR2(255);
    v_test_variation      VARCHAR2(255);
    v_model_exists        NUMBER;
BEGIN
    -- Get an existing model name from the database
    SELECT modelName
    INTO v_existing_model_name
    FROM VehicleModel
    WHERE ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('--- Starting Test Case 7: String Insensitivity (Model Name) ---');
    DBMS_OUTPUT.PUT_LINE('Original model name: ' || v_existing_model_name);

    -- VARIATION A: UPPERCASE
    v_test_variation := UPPER(v_existing_model_name);
    DBMS_OUTPUT.PUT_LINE('Testing Variation A (UPPERCASE): ' || v_test_variation);

    -- Cleanup preventive
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
    DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
    DELETE FROM VehicleModel WHERE ID = v_model_id;
    COMMIT;

    BEGIN
        ADD_ELECTRIC_LOCOMOTIVE_MODEL(
                p_model_id => v_model_id,
                p_model_name => v_test_variation,
                p_length => 20.0,
                p_width => 3.0,
                p_height => 4.3,
                p_tare => 90.0,
                p_year_eis => 2024,
                p_make => 'Siemens',
                p_power => 5500,
                p_acceleration => 0.55,
                p_operational_speed => 70,
                p_max_speed => 220,
                p_number_of_wheels => 8,
                p_frequency => 50,
                p_voltage => 25000,
                p_gauge_ids => '1'
        );

        DBMS_OUTPUT.PUT_LINE('  ERROR: Uppercase variation should have been rejected as duplicate.');
        ROLLBACK;
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20002 THEN
                DBMS_OUTPUT.PUT_LINE('  SUCCESS: Uppercase variation correctly rejected as duplicate.');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  FAILED: Unexpected error: ' || SQLERRM);
            END IF;
            ROLLBACK;
    END;

    -- VARIATION B: Leading/Trailing Spaces
    v_model_id := 9008;
    v_test_variation := '   ' || v_existing_model_name || '   ';
    DBMS_OUTPUT.PUT_LINE('Testing Variation B (Spaces): "' || v_test_variation || '"');

    -- Cleanup preventive
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID = v_model_id;
    DELETE FROM ElectricalType WHERE VehicleModelID = v_model_id;
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID = v_model_id;
    DELETE FROM VehicleModel WHERE ID = v_model_id;
    COMMIT;

    BEGIN
        ADD_ELECTRIC_LOCOMOTIVE_MODEL(
                p_model_id => v_model_id,
                p_model_name => v_test_variation,
                p_length => 20.0,
                p_width => 3.0,
                p_height => 4.3,
                p_tare => 90.0,
                p_year_eis => 2024,
                p_make => 'Siemens',
                p_power => 5500,
                p_acceleration => 0.55,
                p_operational_speed => 70,
                p_max_speed => 220,
                p_number_of_wheels => 8,
                p_frequency => 50,
                p_voltage => 25000,
                p_gauge_ids => '1'
        );

        DBMS_OUTPUT.PUT_LINE('  ERROR: Space variation should have been rejected as duplicate.');
        ROLLBACK;
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20002 THEN
                DBMS_OUTPUT.PUT_LINE('  SUCCESS: Space variation correctly rejected as duplicate.');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  FAILED: Unexpected error: ' || SQLERRM);
            END IF;
            ROLLBACK;
    END;

    -- Cleanup
    DELETE FROM VehicleModel_Gauge WHERE VehicleModelID IN (9007, 9008);
    DELETE FROM ElectricalType WHERE VehicleModelID IN (9007, 9008);
    DELETE FROM LocomotiveSpecs WHERE VehicleModelID IN (9007, 9008);
    DELETE FROM VehicleModel WHERE ID IN (9007, 9008);
    COMMIT;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('CRITICAL ERROR: ' || SQLERRM);
        ROLLBACK;
        -- Cleanup on error
        DELETE FROM VehicleModel_Gauge WHERE VehicleModelID IN (9007, 9008);
        DELETE FROM ElectricalType WHERE VehicleModelID IN (9007, 9008);
        DELETE FROM LocomotiveSpecs WHERE VehicleModelID IN (9007, 9008);
        DELETE FROM VehicleModel WHERE ID IN (9007, 9008);
        COMMIT;
END;
/
