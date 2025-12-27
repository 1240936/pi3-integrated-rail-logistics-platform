-- ============================================================================
-- USBD45 - Add a new line with at least one segment
-- ============================================================================
-- As a Planner, I want to add a new line.
-- A line must have at least one segment.
--
-- Requirements:
-- 1. Create a new RailLine
-- 2. Create at least one LineSegment associated with the RailLine
-- 3. Validate that Owner, Start Facility, End Facility and Gauge exist
-- 4. Start and End Facility must be different
-- 5. Transaction control is handled by the caller
-- ============================================================================

-- ============================================================================
-- Stored Function: AddNewLine
-- ============================================================================
CREATE OR REPLACE FUNCTION AddNewLine(
    p_owner_id          IN NUMBER,
    p_start_facility_id IN NUMBER,
    p_end_facility_id   IN NUMBER,
    p_gauge_id          IN NUMBER,
    p_is_electrified    IN NUMBER,
    p_segment_max_weight IN NUMBER,
    p_segment_length     IN NUMBER,
    p_segment_num_tracks IN NUMBER,
    p_segment_speed_limit IN NUMBER
) RETURN NUMBER
IS
    v_owner_exists    NUMBER;
    v_start_exists    NUMBER;
    v_end_exists      NUMBER;
    v_gauge_exists    NUMBER;

    v_new_line_id     NUMBER;
    v_new_segment_id  NUMBER;
BEGIN
    ---------------------------------------------------------------------------
    -- 1. Basic validation
    ---------------------------------------------------------------------------
    IF p_owner_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20050, 'Owner ID cannot be NULL');
    END IF;

    IF p_start_facility_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20051, 'Start Facility ID cannot be NULL');
    END IF;

    IF p_end_facility_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20052, 'End Facility ID cannot be NULL');
    END IF;

    IF p_gauge_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20053, 'Gauge ID cannot be NULL');
    END IF;

    IF p_start_facility_id = p_end_facility_id THEN
        RAISE_APPLICATION_ERROR(-20054, 'Start and End Facility cannot be the same');
    END IF;

    IF p_segment_max_weight IS NULL THEN
        RAISE_APPLICATION_ERROR(-20059, 'Segment max weight cannot be NULL');
    END IF;

    IF p_segment_length IS NULL THEN
        RAISE_APPLICATION_ERROR(-20060, 'Segment length cannot be NULL');
    END IF;

    IF p_segment_num_tracks IS NULL THEN
        RAISE_APPLICATION_ERROR(-20061, 'Segment number of tracks cannot be NULL');
    END IF;

    IF p_segment_speed_limit IS NULL THEN
        RAISE_APPLICATION_ERROR(-20062, 'Segment speed limit cannot be NULL');
    END IF;

    ---------------------------------------------------------------------------
    -- 2. Validate referenced entities exist
    ---------------------------------------------------------------------------
    SELECT COUNT(*) INTO v_owner_exists
    FROM Owner
    WHERE ID = p_owner_id;

    IF v_owner_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20055, 'Owner does not exist');
    END IF;

    SELECT COUNT(*) INTO v_start_exists
    FROM Facility
    WHERE ID = p_start_facility_id;

    IF v_start_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20056, 'Start Facility does not exist');
    END IF;

    SELECT COUNT(*) INTO v_end_exists
    FROM Facility
    WHERE ID = p_end_facility_id;

    IF v_end_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20057, 'End Facility does not exist');
    END IF;

    SELECT COUNT(*) INTO v_gauge_exists
    FROM Gauge
    WHERE ID = p_gauge_id;

    IF v_gauge_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20058, 'Gauge does not exist');
    END IF;

    ---------------------------------------------------------------------------
    -- 3. Create RailLine
    ---------------------------------------------------------------------------
    SELECT CASE
             WHEN MAX(ID) IS NULL THEN 1
             ELSE MAX(ID) + 1
           END
    INTO v_new_line_id
    FROM RailLine;

    INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
    VALUES (v_new_line_id, p_owner_id, p_start_facility_id, p_end_facility_id, p_gauge_id, p_is_electrified);

    ---------------------------------------------------------------------------
    -- 4. Create mandatory first LineSegment
    ---------------------------------------------------------------------------
    SELECT CASE
             WHEN MAX(ID) IS NULL THEN 1
             ELSE MAX(ID) + 1
           END
    INTO v_new_segment_id
    FROM LineSegment;

    INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
    VALUES (v_new_segment_id, v_new_line_id, p_segment_max_weight, p_segment_length, p_segment_num_tracks, p_segment_speed_limit, 1);

    ---------------------------------------------------------------------------
    -- 5. Return new RailLine ID
    ---------------------------------------------------------------------------
    RETURN v_new_line_id;
END AddNewLine;
/

-- ============================================================================
-- TESTS FOR USBD45
-- ============================================================================

-- ============================================================================
-- Test 1: Happy path
-- ============================================================================
DECLARE
    v_owner_id NUMBER;
    v_start_facility_id NUMBER;
    v_end_facility_id NUMBER;
    v_gauge_id NUMBER;

    v_line_id NUMBER;
    v_line_count NUMBER;
    v_segment_count NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 1: Happy path');

    SELECT ID INTO v_owner_id FROM Owner WHERE ROWNUM = 1;
    SELECT ID INTO v_start_facility_id FROM Facility WHERE ROWNUM = 1;
    SELECT ID INTO v_end_facility_id FROM Facility WHERE ID <> v_start_facility_id AND ROWNUM = 1;
    SELECT ID INTO v_gauge_id FROM Gauge WHERE ROWNUM = 1;

    v_line_id := AddNewLine(v_owner_id, v_start_facility_id, v_end_facility_id, v_gauge_id, 1, 8000, 10000, 2, 110);

    SELECT COUNT(*) INTO v_line_count FROM RailLine WHERE ID = v_line_id;
    SELECT COUNT(*) INTO v_segment_count FROM LineSegment WHERE RailLineID = v_line_id;

    IF v_line_count = 1 AND v_segment_count = 1 THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED');
    END IF;

    ROLLBACK;
END;
/

-- ============================================================================
-- Test 2: NULL owner ID
-- ============================================================================
DECLARE
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 2: NULL owner ID');

    BEGIN
        v_result := AddNewLine(NULL, 1, 2, 1, 1, 8000, 10000, 2, 110);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Exception expected');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20050 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20050, got ' || SQLCODE);
            END IF;
    END;

    ROLLBACK;
END;
/

-- ============================================================================
-- Test 3: Same start and end facility
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_owner_id NUMBER;
    v_facility_id NUMBER;
    v_gauge_id NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 3: Same start and end facility');

    SELECT ID INTO v_owner_id FROM Owner WHERE ROWNUM = 1;
    SELECT ID INTO v_facility_id FROM Facility WHERE ROWNUM = 1;
    SELECT ID INTO v_gauge_id FROM Gauge WHERE ROWNUM = 1;

    BEGIN
        v_result := AddNewLine(v_owner_id, v_facility_id, v_facility_id, v_gauge_id, 1, 8000, 10000, 2, 110);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Exception expected');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20054 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20054, got ' || SQLCODE);
            END IF;
    END;

    ROLLBACK;
END;
/

-- ============================================================================
-- Test 4: Non-existent owner
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_start_facility_id NUMBER;
    v_end_facility_id NUMBER;
    v_gauge_id NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 4: Non-existent owner');

    SELECT ID INTO v_start_facility_id FROM Facility WHERE ROWNUM = 1;
    SELECT ID INTO v_end_facility_id FROM Facility WHERE ID <> v_start_facility_id AND ROWNUM = 1;
    SELECT ID INTO v_gauge_id FROM Gauge WHERE ROWNUM = 1;

    BEGIN
        v_result := AddNewLine(999999, v_start_facility_id, v_end_facility_id, v_gauge_id, 1, 8000, 10000, 2, 110);
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Exception expected');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20055 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20055, got ' || SQLCODE);
            END IF;
    END;

    ROLLBACK;
END;
/

-- ============================================================================
-- Test 5: Transaction rollback
-- ============================================================================
DECLARE
    v_result NUMBER;
    v_owner_id NUMBER;
    v_start_facility_id NUMBER;
    v_end_facility_id NUMBER;
    v_gauge_id NUMBER;

    v_before NUMBER;
    v_after NUMBER;
    v_after_rollback NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 5: Transaction rollback');

    SELECT COUNT(*) INTO v_before FROM RailLine;

    SELECT ID INTO v_owner_id FROM Owner WHERE ROWNUM = 1;
    SELECT ID INTO v_start_facility_id FROM Facility WHERE ROWNUM = 1;
    SELECT ID INTO v_end_facility_id FROM Facility WHERE ID <> v_start_facility_id AND ROWNUM = 1;
    SELECT ID INTO v_gauge_id FROM Gauge WHERE ROWNUM = 1;

    v_result := AddNewLine(v_owner_id, v_start_facility_id, v_end_facility_id, v_gauge_id, 0, 8000, 10000, 2, 110);

    SELECT COUNT(*) INTO v_after FROM RailLine;

    ROLLBACK;

    SELECT COUNT(*) INTO v_after_rollback FROM RailLine;

    IF v_after = v_before + 1 AND v_after_rollback = v_before THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED');
    END IF;
END;
/