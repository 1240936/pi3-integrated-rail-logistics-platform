-- ============================================================================
-- USBD44 - Add a segment to an existing line
-- ============================================================================
-- As a Planner, I want to add a segment to an existing line. If the segment has a siding, then it must also be added.
--
-- Requirements:
-- 1. Add a segment to an existing line by inserting into LineSegment table.
-- 2. Validate that the line exists (RailLineID must reference existing RailLine).
-- 3. Validate that segment ID is unique and not null.
-- 4. Validate segment attributes (length, numberOfTracks, speedLimit, maxWeight, orderNum).
-- 5. If siding information is provided, register the siding in Siding table.
-- 6. Ensure referential integrity between line, segment, and siding tables.
-- 7. Proper transaction handling (COMMIT/ROLLBACK handled by caller).
-- ============================================================================

-- ============================================================================
-- Stored Function: AddSegmentToLine
-- ============================================================================
-- Adds a new segment to an existing line. If siding information is provided,
-- the siding is also registered and associated with the segment.
--
-- Parameters:
--   p_segment_id       - Unique identifier for the segment (required)
--   p_rail_line_id     - ID of the existing rail line (required)
--   p_max_weight       - Maximum weight capacity (required, > 0)
--   p_length           - Length of the segment in appropriate units (required, > 0)
--   p_number_of_tracks - Number of tracks in the segment (required, > 0)
--   p_speed_limit      - Speed limit for the segment (required, > 0)
--   p_order_num        - Order/sequence number in the line (required, >= 0)
--   p_siding_position  - Position of siding if present (optional, NULL if no siding)
--   p_siding_length    - Length of siding if present (optional, required if position provided)
--
-- Returns:
--   The ID of the newly created segment.
--
-- Exceptions:
--   -20001: Segment ID cannot be NULL.
--   -20002: Rail line ID cannot be NULL.
--   -20003: Rail line with the specified ID does not exist.
--   -20004: Segment with the specified ID already exists.
--   -20005: Invalid segment attributes (negative or zero values).
--   -20006: Siding position provided but length is missing or invalid.
--   -20007: Siding position must be within segment length.
-- ============================================================================

CREATE OR REPLACE FUNCTION AddSegmentToLine(
    p_segment_id IN NUMBER,
    p_rail_line_id IN NUMBER,
    p_max_weight IN DOUBLE PRECISION,
    p_length IN DOUBLE PRECISION,
    p_number_of_tracks IN NUMBER,
    p_speed_limit IN DOUBLE PRECISION,
    p_order_num IN NUMBER,
    p_siding_position IN DOUBLE PRECISION DEFAULT NULL,
    p_siding_length IN DOUBLE PRECISION DEFAULT NULL
) RETURN NUMBER
    IS
    v_line_exists NUMBER;
    v_segment_exists NUMBER;
    v_siding_id NUMBER;
    v_has_siding NUMBER := 0;
BEGIN
    -- 1. Basic validation: segment ID cannot be null
    IF p_segment_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Segment ID cannot be null.');
    END IF;

    -- 2. Basic validation: rail line ID cannot be null
    IF p_rail_line_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20002, 'Rail line ID cannot be null.');
    END IF;

    -- 3. Validate that the rail line exists
    SELECT COUNT(*) INTO v_line_exists
    FROM RailLine
    WHERE ID = p_rail_line_id;

    IF v_line_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Rail line with ID ' || p_rail_line_id || ' does not exist.');
    END IF;

    -- 4. Validate that the segment ID is unique
    SELECT COUNT(*) INTO v_segment_exists
    FROM LineSegment
    WHERE ID = p_segment_id;

    IF v_segment_exists > 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Segment with ID ' || p_segment_id || ' already exists.');
    END IF;

    -- 5. Validate segment attributes
    IF p_max_weight <= 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Maximum weight must be greater than 0.');
    END IF;

    IF p_length <= 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Length must be greater than 0.');
    END IF;

    IF p_number_of_tracks <= 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Number of tracks must be greater than 0.');
    END IF;

    IF p_speed_limit <= 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Speed limit must be greater than 0.');
    END IF;

    IF p_order_num < 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Order number must be greater than or equal to 0.');
    END IF;

    -- 6. Validate siding parameters if provided
    IF p_siding_position IS NOT NULL THEN
        v_has_siding := 1;

        IF p_siding_length IS NULL OR p_siding_length <= 0 THEN
            RAISE_APPLICATION_ERROR(-20006, 'Siding length must be provided and greater than 0 when siding position is specified.');
        END IF;

        IF p_siding_position < 0 OR p_siding_position > p_length THEN
            RAISE_APPLICATION_ERROR(-20007, 'Siding position must be between 0 and segment length (' || p_length || ').');
        END IF;
    END IF;

    -- 7. Insert the segment
    INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
    VALUES (p_segment_id, p_rail_line_id, p_max_weight, p_length, p_number_of_tracks, p_speed_limit, p_order_num);

    -- 8. Insert siding if provided
    IF v_has_siding = 1 THEN
        -- Generate new siding ID
        SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID) + 1 END INTO v_siding_id FROM Siding;

        INSERT INTO Siding (ID, LineSegmentID, position, length)
        VALUES (v_siding_id, p_segment_id, p_siding_position, p_siding_length);
    END IF;

    RETURN p_segment_id;
END AddSegmentToLine;
/

-- ============================================================================
-- COMPREHENSIVE TEST ANONYMOUS BLOCKS
-- ============================================================================

-- Test 1: Happy path - Add segment without siding
-- Expected: Should successfully add segment and return segment ID
DECLARE
    v_segment_id NUMBER := 99991;  -- Use a high number to avoid conflicts
    v_rail_line_id NUMBER;
    v_result NUMBER;
    v_verified_count NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 1: Happy path - Add segment without siding');

    -- Get an existing rail line ID
    SELECT ID INTO v_rail_line_id
    FROM RailLine
    WHERE ROWNUM = 1;

    v_result := AddSegmentToLine(
        p_segment_id => v_segment_id,
        p_rail_line_id => v_rail_line_id,
        p_max_weight => 100.5,
        p_length => 1500.0,
        p_number_of_tracks => 2,
        p_speed_limit => 80.0,
        p_order_num => 1
    );

    -- Verify segment was added
    SELECT COUNT(*) INTO v_verified_count
    FROM LineSegment
    WHERE ID = v_segment_id AND RailLineID = v_rail_line_id;

    IF v_verified_count = 1 THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Segment not found');
    END IF;

    -- Cleanup
    DELETE FROM LineSegment WHERE ID = v_segment_id;
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 2: Happy path - Add segment with siding
-- Expected: Should successfully add segment and siding, return segment ID
DECLARE
    v_segment_id NUMBER := 99992;
    v_rail_line_id NUMBER;
    v_result NUMBER;
    v_verified_segment_count NUMBER;
    v_verified_siding_count NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 2: Happy path - Add segment with siding');

    -- Get an existing rail line ID
    SELECT ID INTO v_rail_line_id
    FROM RailLine
    WHERE ROWNUM = 1;

    v_result := AddSegmentToLine(
        p_segment_id => v_segment_id,
        p_rail_line_id => v_rail_line_id,
        p_max_weight => 200.0,
        p_length => 2000.0,
        p_number_of_tracks => 1,
        p_speed_limit => 60.0,
        p_order_num => 2,
        p_siding_position => 500.0,
        p_siding_length => 300.0
    );

    -- Verify segment was added
    SELECT COUNT(*) INTO v_verified_segment_count
    FROM LineSegment
    WHERE ID = v_segment_id AND RailLineID = v_rail_line_id;

    -- Verify siding was added
    SELECT COUNT(*) INTO v_verified_siding_count
    FROM Siding
    WHERE LineSegmentID = v_segment_id;

    IF v_verified_segment_count = 1 AND v_verified_siding_count = 1 THEN
        DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
    ELSE
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Segment or siding not found');
    END IF;

    -- Cleanup
    DELETE FROM Siding WHERE LineSegmentID = v_segment_id;
    DELETE FROM LineSegment WHERE ID = v_segment_id;
    ROLLBACK;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 3: Null segment ID validation
-- Expected: Should raise error -20001
DECLARE
    v_rail_line_id NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 3: Null segment ID');

    SELECT ID INTO v_rail_line_id
    FROM RailLine
    WHERE ROWNUM = 1;

    BEGIN
        v_result := AddSegmentToLine(
            p_segment_id => NULL,
            p_rail_line_id => v_rail_line_id,
            p_max_weight => 100.0,
            p_length => 1000.0,
            p_number_of_tracks => 1,
            p_speed_limit => 50.0,
            p_order_num => 1
        );
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20001 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20001, got ' || SQLCODE);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 4: Null rail line ID validation
-- Expected: Should raise error -20002
DECLARE
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 4: Null rail line ID');

    BEGIN
        v_result := AddSegmentToLine(
            p_segment_id => 99993,
            p_rail_line_id => NULL,
            p_max_weight => 100.0,
            p_length => 1000.0,
            p_number_of_tracks => 1,
            p_speed_limit => 50.0,
            p_order_num => 1
        );
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20002 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20002, got ' || SQLCODE);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 5: Non-existent rail line validation
-- Expected: Should raise error -20003
DECLARE
    v_result NUMBER;
    v_invalid_line_id NUMBER := 999999;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 5: Non-existent rail line');

    BEGIN
        v_result := AddSegmentToLine(
            p_segment_id => 99994,
            p_rail_line_id => v_invalid_line_id,
            p_max_weight => 100.0,
            p_length => 1000.0,
            p_number_of_tracks => 1,
            p_speed_limit => 50.0,
            p_order_num => 1
        );
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20003 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20003, got ' || SQLCODE);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 6: Duplicate segment ID validation
-- Expected: Should raise error -20004
DECLARE
    v_rail_line_id NUMBER;
    v_existing_segment_id NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 6: Duplicate segment ID');

    -- Get an existing segment ID
    SELECT ID INTO v_existing_segment_id
    FROM LineSegment
    WHERE ROWNUM = 1;

    -- Get the rail line ID for that segment
    SELECT RailLineID INTO v_rail_line_id
    FROM LineSegment
    WHERE ID = v_existing_segment_id;

    BEGIN
        v_result := AddSegmentToLine(
            p_segment_id => v_existing_segment_id,
            p_rail_line_id => v_rail_line_id,
            p_max_weight => 100.0,
            p_length => 1000.0,
            p_number_of_tracks => 1,
            p_speed_limit => 50.0,
            p_order_num => 999
        );
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20004 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20004, got ' || SQLCODE);
            END IF;
    END;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('  Result: SKIPPED - No existing segments found');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 7: Invalid segment attributes (negative values)
-- Expected: Should raise error -20005
DECLARE
    v_rail_line_id NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 7: Invalid segment attributes - negative length');

    SELECT ID INTO v_rail_line_id
    FROM RailLine
    WHERE ROWNUM = 1;

    BEGIN
        v_result := AddSegmentToLine(
            p_segment_id => 99995,
            p_rail_line_id => v_rail_line_id,
            p_max_weight => 100.0,
            p_length => -500.0,  -- Invalid: negative length
            p_number_of_tracks => 1,
            p_speed_limit => 50.0,
            p_order_num => 1
        );
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20005 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20005, got ' || SQLCODE);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 8: Invalid siding parameters - position without length
-- Expected: Should raise error -20006
DECLARE
    v_rail_line_id NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 8: Invalid siding - position without length');

    SELECT ID INTO v_rail_line_id
    FROM RailLine
    WHERE ROWNUM = 1;

    BEGIN
        v_result := AddSegmentToLine(
            p_segment_id => 99996,
            p_rail_line_id => v_rail_line_id,
            p_max_weight => 100.0,
            p_length => 1000.0,
            p_number_of_tracks => 1,
            p_speed_limit => 50.0,
            p_order_num => 1,
            p_siding_position => 500.0,
            p_siding_length => NULL  -- Invalid: position provided but length missing
        );
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20006 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20006, got ' || SQLCODE);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- Test 9: Invalid siding position - outside segment bounds
-- Expected: Should raise error -20007
DECLARE
    v_rail_line_id NUMBER;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 9: Invalid siding position - outside segment bounds');

    SELECT ID INTO v_rail_line_id
    FROM RailLine
    WHERE ROWNUM = 1;

    BEGIN
        v_result := AddSegmentToLine(
            p_segment_id => 99997,
            p_rail_line_id => v_rail_line_id,
            p_max_weight => 100.0,
            p_length => 1000.0,
            p_number_of_tracks => 1,
            p_speed_limit => 50.0,
            p_order_num => 1,
            p_siding_position => 1500.0,  -- Invalid: position beyond segment length
            p_siding_length => 200.0
        );
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Should have raised exception');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20007 THEN
                DBMS_OUTPUT.PUT_LINE('  Result: PASSED');
            ELSE
                DBMS_OUTPUT.PUT_LINE('  Result: FAILED - Expected -20007, got ' || SQLCODE);
            END IF;
    END;

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  Result: FAILED - ' || SQLERRM);
        ROLLBACK;
END;
/

-- ============================================================================
-- VALIDATION QUERIES
-- ============================================================================

-- Validation Query 1: Verify segment registration and association with rail line
-- Expected: Shows segment details and confirms association with rail line
SELECT
    'Segment Registration Validation' AS validation_type,
    ls.ID AS segment_id,
    ls.RailLineID AS rail_line_id,
    rl.StartFacilityID,
    rl.EndFacilityID,
    ls.length,
    ls.numberOfTracks,
    ls.speedLimit,
    ls.maxWeight,
    ls.orderNum,
    CASE WHEN s.ID IS NOT NULL THEN 'Has Siding' ELSE 'No Siding' END AS siding_status
FROM LineSegment ls
JOIN RailLine rl ON ls.RailLineID = rl.ID
LEFT JOIN Siding s ON ls.ID = s.LineSegmentID
ORDER BY ls.ID;

-- Validation Query 2: Verify siding associations
-- Expected: Shows all sidings and their associated segments
SELECT
    'Siding Association Validation' AS validation_type,
    s.ID AS siding_id,
    s.LineSegmentID AS segment_id,
    ls.RailLineID AS rail_line_id,
    s.position,
    s.length,
    ls.length AS segment_length,
    CASE
        WHEN s.position BETWEEN 0 AND ls.length THEN 'Valid Position'
        ELSE 'Invalid Position'
    END AS position_validation
FROM Siding s
JOIN LineSegment ls ON s.LineSegmentID = ls.ID
ORDER BY s.ID;

-- Validation Query 3: Referential integrity check
-- Expected: Should return no rows if referential integrity is maintained
SELECT
    'Referential Integrity Check' AS validation_type,
    'Orphaned Siding' AS issue_type,
    s.ID AS siding_id,
    s.LineSegmentID AS segment_id
FROM Siding s
LEFT JOIN LineSegment ls ON s.LineSegmentID = ls.ID
WHERE ls.ID IS NULL
UNION ALL
SELECT
    'Referential Integrity Check' AS validation_type,
    'Orphaned Segment' AS issue_type,
    ls.ID AS segment_id,
    ls.RailLineID AS rail_line_id
FROM LineSegment ls
LEFT JOIN RailLine rl ON ls.RailLineID = rl.ID
WHERE rl.ID IS NULL;

COMMIT;
