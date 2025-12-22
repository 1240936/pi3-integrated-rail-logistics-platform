-- USLP10: PL/SQL Functions for Train Scheduling
-- This script contains all PL/SQL functions required for USLP10 train scheduling functionality.
-- These functions replace direct SQL queries in Java repositories.

-- ============================================================================
-- LINE SEGMENT FUNCTIONS (for speed calculation and track limitations)
-- ============================================================================

-- Function: Get all line segments
-- Returns all line segments with their properties (speed limit, max weight, etc.)
CREATE OR REPLACE FUNCTION GET_ALL_LINE_SEGMENTS
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT 
            ID,
            RailLineID,
            maxWeight,
            length,
            numberOfTracks,
            speedLimit,
            orderNum
        FROM LineSegment
        ORDER BY RailLineID, orderNum;
    RETURN v_cursor;
END;
/

-- Function: Get line segments by rail line ID
CREATE OR REPLACE FUNCTION GET_LINE_SEGMENTS_BY_RAIL_LINE_ID(
    p_rail_line_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT 
            ID,
            RailLineID,
            maxWeight,
            length,
            numberOfTracks,
            speedLimit,
            orderNum
        FROM LineSegment
        WHERE RailLineID = p_rail_line_id
        ORDER BY orderNum;
    RETURN v_cursor;
END;
/

-- Function: Get line segment by ID
CREATE OR REPLACE FUNCTION GET_LINE_SEGMENT_BY_ID(
    p_segment_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT 
            ID,
            RailLineID,
            maxWeight,
            length,
            numberOfTracks,
            speedLimit,
            orderNum
        FROM LineSegment
        WHERE ID = p_segment_id;
    RETURN v_cursor;
END;
/

-- Function: Get all sidings
CREATE OR REPLACE FUNCTION GET_ALL_SIDINGS
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT 
            ID,
            LineSegmentID,
            position,
            length
        FROM Siding;
    RETURN v_cursor;
END;
/

-- ============================================================================
-- FACILITY FUNCTIONS (for route planning and passage times)
-- ============================================================================

-- Function: Get all facilities
CREATE OR REPLACE FUNCTION GET_ALL_FACILITIES
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, name
        FROM Facility
        ORDER BY ID;
    RETURN v_cursor;
END;
/

-- Function: Get facility by ID
CREATE OR REPLACE FUNCTION GET_FACILITY_BY_ID(
    p_facility_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, name
        FROM Facility
        WHERE ID = p_facility_id;
    RETURN v_cursor;
END;
/

-- ============================================================================
-- RAIL LINE FUNCTIONS (for pathfinding and route planning)
-- ============================================================================

-- Function: Get all rail lines
CREATE OR REPLACE FUNCTION GET_ALL_RAIL_LINES
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT 
            ID,
            OwnerID,
            StartFacilityID,
            EndFacilityID,
            GaugeID,
            isElectrified
        FROM RailLine
        ORDER BY ID;
    RETURN v_cursor;
END;
/

-- Function: Get rail line by ID
CREATE OR REPLACE FUNCTION GET_RAIL_LINE_BY_ID(
    p_rail_line_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT 
            ID,
            OwnerID,
            StartFacilityID,
            EndFacilityID,
            GaugeID,
            isElectrified
        FROM RailLine
        WHERE ID = p_rail_line_id;
    RETURN v_cursor;
END;
/

-- Function: Find rail lines connecting two facilities
-- Returns rail lines that connect facility1 to facility2 (bidirectional)
CREATE OR REPLACE FUNCTION GET_CONNECTING_RAIL_LINES(
    p_facility_id1 IN NUMBER,
    p_facility_id2 IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT 
            ID,
            OwnerID,
            StartFacilityID,
            EndFacilityID,
            GaugeID,
            isElectrified
        FROM RailLine
        WHERE (StartFacilityID = p_facility_id1 AND EndFacilityID = p_facility_id2)
           OR (StartFacilityID = p_facility_id2 AND EndFacilityID = p_facility_id1);
    RETURN v_cursor;
END;
/

-- Function: Get all facilities directly connected to a given facility via rail lines
-- Rail lines are bidirectional, so connections work both ways
CREATE OR REPLACE FUNCTION GET_CONNECTED_FACILITIES(
    p_facility_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT DISTINCT 
            CASE 
                WHEN StartFacilityID = p_facility_id THEN EndFacilityID
                WHEN EndFacilityID = p_facility_id THEN StartFacilityID
            END AS ConnectedFacilityID
        FROM RailLine
        WHERE StartFacilityID = p_facility_id 
           OR EndFacilityID = p_facility_id;
    RETURN v_cursor;
END;
/

