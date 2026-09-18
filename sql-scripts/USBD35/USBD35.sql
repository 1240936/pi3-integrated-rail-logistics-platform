-- ============================================================================
-- USBD35 - Register a new facility in the database
-- ============================================================================
-- As a Planner, I want to register a new facility in the database.
--
-- Requirements:
-- 1. Register a new facility (station, terminal, yard, etc.) in the Facility table.
-- 2. Validate that the facility name is provided and not empty.
-- 3. Ensure that the facility name is unique (case-insensitive and space-insensitive).
-- 4. Generate a unique ID for the new facility.
-- 5. Support additional attributes if defined in the model (e.g., location, type).
-- 6. Proper transaction handling (COMMIT/ROLLBACK handled by caller).
--
-- Note: This implementation is part of the Infrastructure Management module.
-- ============================================================================

-- ============================================================================
-- Stored Function: RegisterFacility
-- ============================================================================
-- Registers a new facility in the database.
--
-- Parameters:
--   p_name - Name of the facility (required)
--
-- Returns:
--   The ID of the newly created facility.
--
-- Exceptions:
--   -20001: Facility name cannot be NULL or empty.
--   -20002: Facility with the same name already exists.
-- ============================================================================

CREATE OR REPLACE FUNCTION RegisterFacility(
    p_name IN VARCHAR2
) RETURN NUMBER
    IS
    v_new_id           NUMBER;
    v_exists           NUMBER;
    v_normalized_input VARCHAR2(255);
BEGIN
    -- 1. Basic validation: name cannot be null or empty
    IF p_name IS NULL OR TRIM(p_name) IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Facility name cannot be null or empty.');
    END IF;

    -- 2. Normalization for comparison: remove all spaces and convert to uppercase
    -- This ensures case and space insensitive duplicate checking
    -- "Test Space" and "TestSpace" are treated as duplicates
    v_normalized_input := UPPER(REPLACE(p_name, ' ', ''));

    -- 3. Duplicate check: compare normalized names (case and space insensitive)
    SELECT COUNT(*)
    INTO v_exists
    FROM Facility
    WHERE UPPER(REPLACE(name, ' ', '')) = v_normalized_input;

    IF v_exists > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Facility with name ''' || p_name || ''' already exists.');
    END IF;

    -- 4. Generate new ID: get maximum ID and increment by 1 (returns 1 if table is empty)
    SELECT CASE WHEN MAX(ID) IS NULL THEN 0 ELSE MAX(ID) END + 1 INTO v_new_id FROM Facility;

    -- 5. Insert new facility (trim leading/trailing spaces from name)
    INSERT INTO Facility (ID, name)
    VALUES (v_new_id, TRIM(p_name));

    RETURN v_new_id;
END RegisterFacility;
/

-- ============================================================================
-- COMPREHENSIVE TEST ANONYMOUS BLOCKS
-- ============================================================================

-- Test 1: Happy path - Register a new facility with valid name
-- Expected: Should successfully register and return new facility ID
DECLARE
    v_test_name     VARCHAR2(255) := 'Test Facility ' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF');
    v_new_id        NUMBER;
    v_verified_id   NUMBER;
    v_verified_name VARCHAR2(255);
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 1: Happy Path - Register New Facility ===');

    -- Register the facility
    v_new_id := RegisterFacility(v_test_name);
    DBMS_OUTPUT.PUT_LINE('Registered facility with ID: ' || v_new_id);

    -- Verify the facility was inserted correctly
    SELECT ID, name
    INTO v_verified_id, v_verified_name
    FROM Facility
    WHERE ID = v_new_id;

    IF v_verified_id = v_new_id AND v_verified_name = TRIM(v_test_name) THEN
        DBMS_OUTPUT.PUT_LINE('TEST 1 PASSED: Facility registered correctly');
    ELSE
        DBMS_OUTPUT.PUT_LINE('TEST 1 FAILED: Facility data mismatch');
    END IF;

    -- Cleanup: Delete the test facility
    DELETE FROM Facility WHERE ID = v_new_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 1 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 2: Null input validation
-- Expected: Should raise error -20001
DECLARE
    v_new_id     NUMBER;
    v_error_code NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 2: Null Input Validation ===');

    BEGIN
        v_new_id := RegisterFacility(NULL);
        DBMS_OUTPUT.PUT_LINE('TEST 2 FAILED: Should have raised exception for NULL input');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20001 THEN
                DBMS_OUTPUT.PUT_LINE('TEST 2 PASSED: Correctly rejected NULL input');
            ELSE
                DBMS_OUTPUT.PUT_LINE('TEST 2 FAILED: Wrong error code. Expected -20001, got ' || v_error_code);
            END IF;
    END;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 2 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 3: Empty string validation
-- Expected: Should raise error -20001
DECLARE
    v_new_id     NUMBER;
    v_error_code NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 3: Empty String Validation ===');

    BEGIN
        v_new_id := RegisterFacility('');
        DBMS_OUTPUT.PUT_LINE('TEST 3 FAILED: Should have raised exception for empty string');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20001 THEN
                DBMS_OUTPUT.PUT_LINE('TEST 3 PASSED: Correctly rejected empty string');
            ELSE
                DBMS_OUTPUT.PUT_LINE('TEST 3 FAILED: Wrong error code. Expected -20001, got ' || v_error_code);
            END IF;
    END;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 3 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 4: Whitespace-only string validation
-- Expected: Should raise error -20001
DECLARE
    v_new_id     NUMBER;
    v_error_code NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 4: Whitespace-Only String Validation ===');

    BEGIN
        v_new_id := RegisterFacility('   ');
        DBMS_OUTPUT.PUT_LINE('TEST 4 FAILED: Should have raised exception for whitespace-only string');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20001 THEN
                DBMS_OUTPUT.PUT_LINE('TEST 4 PASSED: Correctly rejected whitespace-only string');
            ELSE
                DBMS_OUTPUT.PUT_LINE('TEST 4 FAILED: Wrong error code. Expected -20001, got ' || v_error_code);
            END IF;
    END;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 4 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 5: Duplicate name detection (case-insensitive)
-- Expected: Should raise error -20002 on duplicate
DECLARE
    v_test_name  VARCHAR2(255) := 'Test Duplicate Case ' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF');
    v_new_id     NUMBER;
    v_error_code NUMBER;
    v_first_id   NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 5: Duplicate Name Detection (Case-Insensitive) ===');

    -- Register first facility
    v_first_id := RegisterFacility(v_test_name);
    DBMS_OUTPUT.PUT_LINE('Registered first facility with ID: ' || v_first_id || ', name: ' || v_test_name);

    -- Try to register duplicate with different case
    BEGIN
        v_new_id := RegisterFacility(UPPER(v_test_name));
        DBMS_OUTPUT.PUT_LINE('TEST 5 FAILED: Should have raised exception for duplicate name (different case)');
        -- Cleanup if somehow it succeeded
        DELETE FROM Facility WHERE ID = v_new_id;
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20002 THEN
                DBMS_OUTPUT.PUT_LINE('TEST 5 PASSED: Correctly rejected duplicate name (case-insensitive)');
            ELSE
                DBMS_OUTPUT.PUT_LINE('TEST 5 FAILED: Wrong error code. Expected -20002, got ' || v_error_code);
            END IF;
    END;

    -- Cleanup
    DELETE FROM Facility WHERE ID = v_first_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 5 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 6: Duplicate name detection (space-insensitive)
-- Expected: Should raise error -20002 on duplicate with different spacing
DECLARE
    v_test_name        VARCHAR2(255) := 'TestSpace';
    v_test_name_spaced VARCHAR2(255) := '  Test Space  ';
    v_new_id           NUMBER;
    v_error_code       NUMBER;
    v_first_id         NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 6: Duplicate Name Detection (Space-Insensitive) ===');

    -- Register first facility
    v_first_id := RegisterFacility(v_test_name);
    DBMS_OUTPUT.PUT_LINE('Registered first facility with ID: ' || v_first_id || ', name: ' || v_test_name);

    -- Commit the first registration so it's visible for duplicate check
    COMMIT;

    -- Try to register duplicate with spaces (after removing all spaces, should be same)
    BEGIN
        v_new_id := RegisterFacility(v_test_name_spaced);
        DBMS_OUTPUT.PUT_LINE('TEST 6 FAILED: Should have raised exception for duplicate name (different spacing)');
        -- Cleanup if somehow it succeeded
        DELETE FROM Facility WHERE ID = v_new_id;
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20002 THEN
                DBMS_OUTPUT.PUT_LINE('TEST 6 PASSED: Correctly rejected duplicate name (space-insensitive)');
            ELSE
                DBMS_OUTPUT.PUT_LINE('TEST 6 FAILED: Wrong error code. Expected -20002, got ' || v_error_code);
            END IF;
    END;

    -- Cleanup
    DELETE FROM Facility WHERE ID = v_first_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 6 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 7: Name with leading/trailing spaces handling
-- Expected: Should successfully register and trim spaces
DECLARE
    v_test_name        VARCHAR2(255) := '  Facility With Spaces  ';
    v_expected_trimmed VARCHAR2(255) := 'Facility With Spaces';
    v_new_id           NUMBER;
    v_verified_name    VARCHAR2(255);
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 7: Name with Leading/Trailing Spaces ===');

    -- Register facility with spaces
    v_new_id := RegisterFacility(v_test_name);
    DBMS_OUTPUT.PUT_LINE('Registered facility with ID: ' || v_new_id);

    -- Verify spaces were trimmed
    SELECT name
    INTO v_verified_name
    FROM Facility
    WHERE ID = v_new_id;

    IF v_verified_name = v_expected_trimmed THEN
        DBMS_OUTPUT.PUT_LINE('TEST 7 PASSED: Spaces correctly trimmed');
    ELSE
        DBMS_OUTPUT.PUT_LINE('TEST 7 FAILED: Expected "' || v_expected_trimmed || '", got "' || v_verified_name || '"');
    END IF;

    -- Cleanup
    DELETE FROM Facility WHERE ID = v_new_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 7 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 8: ID generation with empty table
-- Expected: Should return ID = 1 if table is empty (or MAX+1 if not empty)
DECLARE
    v_test_name   VARCHAR2(255) := 'Test ID Generation ' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF');
    v_new_id      NUMBER;
    v_max_id      NUMBER;
    v_expected_id NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 8: ID Generation ===');

    -- Get current max ID (might be NULL if table is empty)
    SELECT CASE WHEN MAX(ID) IS NULL THEN 0 ELSE MAX(ID) END INTO v_max_id FROM Facility;
    v_expected_id := v_max_id + 1;

    -- Register facility
    v_new_id := RegisterFacility(v_test_name);
    DBMS_OUTPUT.PUT_LINE('Current max ID: ' || v_max_id || ', Expected new ID: ' || v_expected_id || ', Got: ' ||
                         v_new_id);

    IF v_new_id = v_expected_id THEN
        DBMS_OUTPUT.PUT_LINE('TEST 8 PASSED: ID generated correctly');
    ELSE
        DBMS_OUTPUT.PUT_LINE('TEST 8 FAILED: Expected ID ' || v_expected_id || ', got ' || v_new_id);
    END IF;

    -- Cleanup
    DELETE FROM Facility WHERE ID = v_new_id;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 8 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 9: Multiple sequential registrations
-- Expected: Each should get a unique, sequential ID
DECLARE
    v_test_name1  VARCHAR2(255) := 'Sequential Test 1 ' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF');
    v_test_name2  VARCHAR2(255) := 'Sequential Test 2 ' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF');
    v_test_name3  VARCHAR2(255) := 'Sequential Test 3 ' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF');
    v_id1         NUMBER;
    v_id2         NUMBER;
    v_id3         NUMBER;
    v_test_passed BOOLEAN       := TRUE;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 9: Multiple Sequential Registrations ===');

    -- Register three facilities sequentially
    v_id1 := RegisterFacility(v_test_name1);
    v_id2 := RegisterFacility(v_test_name2);
    v_id3 := RegisterFacility(v_test_name3);

    DBMS_OUTPUT.PUT_LINE('Registered IDs: ' || v_id1 || ', ' || v_id2 || ', ' || v_id3);

    -- Verify IDs are sequential
    IF v_id2 <> v_id1 + 1 THEN
        DBMS_OUTPUT.PUT_LINE('ID2 should be ' || (v_id1 + 1) || ', got ' || v_id2);
        v_test_passed := FALSE;
    END IF;

    IF v_id3 <> v_id2 + 1 THEN
        DBMS_OUTPUT.PUT_LINE('ID3 should be ' || (v_id2 + 1) || ', got ' || v_id3);
        v_test_passed := FALSE;
    END IF;

    IF v_test_passed THEN
        DBMS_OUTPUT.PUT_LINE('TEST 9 PASSED: Sequential IDs generated correctly');
    ELSE
        DBMS_OUTPUT.PUT_LINE('TEST 9 FAILED: IDs not sequential');
    END IF;

    -- Cleanup
    DELETE FROM Facility WHERE ID IN (v_id1, v_id2, v_id3);
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEST 9 FAILED: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 10: Transaction rollback test
-- Expected: Should allow rollback by caller (function should not commit)
DECLARE
    v_test_name            VARCHAR2(255) := 'Rollback Test ' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF');
    v_new_id               NUMBER;
    v_count_before         NUMBER;
    v_count_after          NUMBER;
    v_count_after_rollback NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== TEST 10: Transaction Rollback Test ===');

    -- Count facilities before
    SELECT COUNT(*) INTO v_count_before FROM Facility;

    -- Register facility
    v_new_id := RegisterFacility(v_test_name);
    DBMS_OUTPUT.PUT_LINE('Registered facility with ID: ' || v_new_id);

    -- Count after registration (before commit)
    SELECT COUNT(*) INTO v_count_after FROM Facility;

    -- Rollback (simulating caller rollback)
    ROLLBACK;

    -- Count after rollback
    SELECT COUNT(*) INTO v_count_after_rollback FROM Facility;

    IF v_count_after = v_count_before + 1 AND v_count_after_rollback = v_count_before THEN
        DBMS_OUTPUT.PUT_LINE('TEST 10 PASSED: Transaction can be rolled back by caller');
    ELSE
        DBMS_OUTPUT.PUT_LINE('TEST 10 FAILED: Transaction management issue');
        DBMS_OUTPUT.PUT_LINE('  Before: ' || v_count_before || ', After insert: ' || v_count_after ||
                             ', After rollback: ' || v_count_after_rollback);
    END IF;

    DBMS_OUTPUT.PUT_LINE('');

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('TEST 10 FAILED: ' || SQLERRM);
END;
/