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
-- Creates a new RailLine and its first mandatory LineSegment.
--
-- Parameters:
--   p_owner_id          - Owner ID (required)
--   p_start_facility_id - Start Facility ID (required)
--   p_end_facility_id   - End Facility ID (required)
--   p_gauge_id          - Gauge ID (required)
--   p_is_electrified    - Electrified flag (required)
--
-- Returns:
--   The ID of the newly created RailLine
--
-- Exceptions:
--   -20050: Owner ID cannot be NULL
--   -20051: Start Facility ID cannot be NULL
--   -20052: End Facility ID cannot be NULL
--   -20053: Gauge ID cannot be NULL
--   -20054: Start and End Facility cannot be the same
--   -20055: Owner does not exist
--   -20056: Start Facility does not exist
--   -20057: End Facility does not exist
--   -20058: Gauge does not exist
-- ============================================================================

CREATE OR REPLACE FUNCTION AddNewLine(
    p_owner_id          IN NUMBER,
    p_start_facility_id IN NUMBER,
    p_end_facility_id   IN NUMBER,
    p_gauge_id          IN NUMBER,
    p_is_electrified    IN NUMBER
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

    INSERT INTO LineSegment (ID, RailLineID, orderNum)
    VALUES (v_new_segment_id, v_new_line_id, 1);

    ---------------------------------------------------------------------------
    -- 5. Return new RailLine ID
    ---------------------------------------------------------------------------
    RETURN v_new_line_id;

END AddNewLine;
/
