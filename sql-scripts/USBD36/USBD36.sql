-- ============================================================================
-- USBD36 - Add a building to an existing facility
-- ============================================================================
-- As a Planner, I want to add a building to an existing facility.
--
-- Requirements:
-- 1. Validate that the facility exists
-- 2. Validate that the building type exists
-- 3. Create a new building associated with the facility
-- 4. Preserve referential integrity
-- 5. Proper transaction handling (COMMIT/ROLLBACK handled by caller)
--
-- Note: This implementation uses the Facility, Building, and BuildingType
-- tables defined in USBD31.
-- ============================================================================

-- ============================================================================
-- Stored Function: AddBuildingToFacility
-- ============================================================================
-- Adds a building to an existing facility.
--
-- Parameters:
--   p_facility_id   - ID of the facility (required)
--   p_building_type - Building type name (required)
--
-- Returns:
--   The ID of the newly created Building
--
-- Exceptions:
--   -20030: Facility ID cannot be NULL
--   -20031: Building type cannot be NULL
--   -20032: Facility does not exist
--   -20033: Building type does not exist
-- ============================================================================

CREATE OR REPLACE FUNCTION AddBuildingToFacility (
    p_facility_id   IN NUMBER,
    p_building_type IN VARCHAR2
) RETURN NUMBER
IS
    v_facility_exists  NUMBER;
    v_building_type_id NUMBER;
    v_new_building_id  NUMBER;
BEGIN
    ---------------------------------------------------------------------------
    -- 1. Basic validation: Facility ID cannot be NULL
    ---------------------------------------------------------------------------
    IF p_facility_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20030, 'Facility ID cannot be NULL.');
    END IF;

    ---------------------------------------------------------------------------
    -- 2. Basic validation: Building type cannot be NULL
    ---------------------------------------------------------------------------
    IF p_building_type IS NULL THEN
        RAISE_APPLICATION_ERROR(-20031, 'Building type cannot be NULL.');
    END IF;

    ---------------------------------------------------------------------------
    -- 3. Validate that the Facility exists
    ---------------------------------------------------------------------------
    SELECT COUNT(*)
    INTO v_facility_exists
    FROM Facility
    WHERE ID = p_facility_id;

    IF v_facility_exists = 0 THEN
        RAISE_APPLICATION_ERROR(
            -20032,
            'Facility with ID ' || p_facility_id || ' does not exist.'
        );
    END IF;

    ---------------------------------------------------------------------------
    -- 4. Validate that the Building Type exists and get its ID
    -- Case and space insensitive comparison
    ---------------------------------------------------------------------------
    BEGIN
        SELECT ID
        INTO v_building_type_id
        FROM BuildingType
        WHERE UPPER(TRIM(type)) = UPPER(TRIM(p_building_type));
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(
                -20033,
                'Building type "' || p_building_type || '" does not exist.'
            );
        WHEN TOO_MANY_ROWS THEN
            -- Defensive check (type should be unique)
            RAISE_APPLICATION_ERROR(
                -20033,
                'Building type "' || p_building_type || '" is not unique.'
            );
    END;

    ---------------------------------------------------------------------------
    -- 5. Insert the new Building
    -- Generate next Building ID
    ---------------------------------------------------------------------------
    SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID) + 1 END
    INTO v_new_building_id
    FROM Building;

    INSERT INTO Building (ID, FacilityID, BuildingTypeID)
    VALUES (
        v_new_building_id,
        p_facility_id,
        v_building_type_id
    );

    ---------------------------------------------------------------------------
    -- 6. Return newly created Building ID
    ---------------------------------------------------------------------------
    RETURN v_new_building_id;

EXCEPTION
    WHEN OTHERS THEN
        -- Re-raise application errors
        IF SQLCODE BETWEEN -20999 AND -20000 THEN
            RAISE;
        ELSE
            RAISE_APPLICATION_ERROR(-20999, 'Unexpected error in AddBuildingToFacility: ' || SQLERRM);
        END IF;
END AddBuildingToFacility;
/
-- ============================================================================


-- ============================================================================
-- COMPREHENSIVE TEST ANONYMOUS BLOCKS
-- ============================================================================


-- ============================================================================
-- Test 1: Happy path - Add building to existing facility
-- Expected: Should return newly created Building ID
-- ============================================================================

DECLARE
    v_facility_id NUMBER;
    v_building_id NUMBER;
    v_verified NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 1: Happy path');

    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;

    v_building_id := AddBuildingToFacility(v_facility_id, 'WAREHOUSE');

    SELECT COUNT(*)
    INTO v_verified
    FROM Building
    WHERE ID = v_building_id
      AND FacilityID = v_facility_id;

    IF v_verified = 1 THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Building not found');
    END IF;

    ROLLBACK;
END;
/
-- ============================================================================


-- ============================================================================
-- Test 2: NULL facility ID
-- Expected: Error -20030
-- ============================================================================

DECLARE
    v_error_code NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 2: NULL facility ID');

    BEGIN
        v_result := AddBuildingToFacility(NULL, 'WAREHOUSE');
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Exception expected');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20030 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Got ' || v_error_code);
            END IF;
    END;
END;
/
-- ============================================================================


-- ============================================================================
-- Test 3: NULL building type
-- Expected: Error -20031
-- ============================================================================

DECLARE
    v_facility_id NUMBER;
    v_error_code NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 3: NULL building type');

    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;

    BEGIN
        v_result := AddBuildingToFacility(v_facility_id, NULL);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Exception expected');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20031 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Got ' || v_error_code);
            END IF;
    END;

    ROLLBACK;
END;
/
-- ============================================================================


-- ============================================================================
-- Test 4: Non-existent facility
-- Expected: Error -20032
-- ============================================================================

DECLARE
    v_error_code NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 4: Non-existent facility');

    BEGIN
        v_result := AddBuildingToFacility(999999, 'WAREHOUSE');
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Exception expected');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20032 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Got ' || v_error_code);
            END IF;
    END;
END;
/
-- ============================================================================


-- ============================================================================
-- Test 5: Non-existent building type
-- Expected: Error -20033
-- ============================================================================

DECLARE
    v_facility_id NUMBER;
    v_error_code NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 5: Non-existent building type');

    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;

    BEGIN
        v_result := AddBuildingToFacility(v_facility_id, 'INVALID_TYPE');
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Exception expected');
    EXCEPTION
        WHEN OTHERS THEN
            v_error_code := SQLCODE;
            IF v_error_code = -20033 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Got ' || v_error_code);
            END IF;
    END;

    ROLLBACK;
END;
/
-- ============================================================================


-- ============================================================================
-- Test 6: Multiple buildings added to the same facility
-- Expected: Both insertions succeed
-- ============================================================================

DECLARE
    v_facility_id NUMBER;
    v_building_1 NUMBER;
    v_building_2 NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 6: Multiple buildings for same facility');

    SELECT ID
    INTO v_facility_id
    FROM Facility
    WHERE ROWNUM = 1;

    v_building_1 := AddBuildingToFacility(v_facility_id, 'WAREHOUSE');
    v_building_2 := AddBuildingToFacility(v_facility_id, 'WAREHOUSE');

    IF v_building_1 IS NOT NULL AND v_building_2 IS NOT NULL THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED');
    END IF;

    ROLLBACK;
END;
/
-- ============================================================================


-- ============================================================================
-- Test 7: Transaction rollback test
-- Expected: Function does not commit; rollback removes inserted building
-- ============================================================================

DECLARE
    v_facility_id NUMBER;
    v_building_id NUMBER;
    v_before NUMBER;
    v_after NUMBER;
    v_after_rollback NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 7: Transaction rollback');

    SELECT COUNT(*) INTO v_before FROM Building;

    SELECT ID INTO v_facility_id FROM Facility WHERE ROWNUM = 1;

    v_building_id := AddBuildingToFacility(v_facility_id, 'WAREHOUSE');

    SELECT COUNT(*) INTO v_after FROM Building;

    ROLLBACK;

    SELECT COUNT(*) INTO v_after_rollback FROM Building;

    IF v_after = v_before + 1 AND v_after_rollback = v_before THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED');
    END IF;
END;
/
-- ============================================================================