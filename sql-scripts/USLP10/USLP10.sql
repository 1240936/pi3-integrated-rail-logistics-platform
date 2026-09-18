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

-- ============================================================================
-- TRAIN SCHEDULING FUNCTIONS (for dispatch and scheduling operations)
-- ============================================================================

-- Function: Get trains available for dispatch
-- Returns trains that have locomotives and wagons assigned but no route scheduled yet
CREATE OR REPLACE FUNCTION GET_TRAINS_AVAILABLE_FOR_DISPATCH
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT DISTINCT
            T.ID AS TrainID,
            T.TrainOperatorID,
            COUNT(DISTINCT AL.LocomotiveID) AS LocomotiveCount,
            COUNT(DISTINCT AW.WagonID) AS WagonCount
        FROM Train T
        INNER JOIN Assigned_Locomotive AL ON T.ID = AL.PlannedTrainID
        INNER JOIN Assigned_Wagon AW ON T.ID = AW.PlannedTrainID
        WHERE NOT EXISTS (
            SELECT 1
            FROM Planned_Train PT
            WHERE PT.TrainID = T.ID
        )
        GROUP BY T.ID, T.TrainOperatorID
        HAVING COUNT(DISTINCT AL.LocomotiveID) > 0
           AND COUNT(DISTINCT AW.WagonID) > 0
        ORDER BY T.ID;
    RETURN v_cursor;
END;
/

-- Function: Get passage times (train events) for a route
-- Returns all train events (estimated passage times) for a given route
-- Note: TrainEvent doesn't have RouteID, so we use Planned_Train to link Route to TrainID
CREATE OR REPLACE FUNCTION GET_PASSAGE_TIMES_BY_ROUTE_ID(
    p_route_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            TE.ID,
            p_route_id AS RouteID,
            TE.TrainID,
            TE.FacilityID,
            TE.eventTime
        FROM TrainEvent TE
        INNER JOIN Planned_Train PT ON TE.TrainID = PT.TrainID
        WHERE PT.RouteID = p_route_id
        ORDER BY TE.eventTime;
    RETURN v_cursor;
END;
/

-- Function: Get all routes with their passage times
-- Returns all routes with their first and last passage times
-- Note: TrainEvent doesn't have RouteID, so we use Planned_Train to link Route to TrainID
CREATE OR REPLACE FUNCTION GET_ALL_ROUTES_WITH_SCHEDULE
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            R.ID AS RouteID,
            R.StartFacilityID,
            R.EndFacilityID,
            PT.TrainID,
            PT.startDate,
            MIN(TE.eventTime) AS FirstPassageTime,
            MAX(TE.eventTime) AS LastPassageTime,
            COUNT(TE.ID) AS EventCount
        FROM Route R
        LEFT JOIN Planned_Train PT ON R.ID = PT.RouteID
        LEFT JOIN TrainEvent TE ON PT.TrainID = TE.TrainID
        GROUP BY R.ID, R.StartFacilityID, R.EndFacilityID, PT.TrainID, PT.startDate
        ORDER BY PT.startDate NULLS LAST, R.ID;
    RETURN v_cursor;
END;
/

-- Function: Calculate shortest path between two facilities
-- Returns a cursor with facility IDs in order (for automatic path calculation)
-- Uses a recursive CTE to find the shortest path
-- Note: Path parsing uses SUBSTR/INSTR instead of regex
CREATE OR REPLACE FUNCTION CALCULATE_SHORTEST_PATH(
    p_start_facility_id IN NUMBER,
    p_end_facility_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        WITH PathSearch AS (
            -- Base case: start facility
            SELECT
                p_start_facility_id AS FacilityID,
                0 AS PathLength,
                CAST(p_start_facility_id AS VARCHAR2(4000)) AS PathString
            FROM DUAL
            UNION ALL
            -- Recursive case: find next facilities
            SELECT
                CASE
                    WHEN RL.StartFacilityID = ps.FacilityID THEN RL.EndFacilityID
                    ELSE RL.StartFacilityID
                END AS FacilityID,
                ps.PathLength + 1 AS PathLength,
                ps.PathString || ',' ||
                CASE
                    WHEN RL.StartFacilityID = ps.FacilityID THEN TO_CHAR(RL.EndFacilityID)
                    ELSE TO_CHAR(RL.StartFacilityID)
                END AS PathString
            FROM PathSearch ps
            INNER JOIN RailLine RL ON (
                RL.StartFacilityID = ps.FacilityID
                OR RL.EndFacilityID = ps.FacilityID
            )
            WHERE ps.PathLength < 20  -- Prevent infinite loops (max 20 hops)
              AND ps.FacilityID != p_end_facility_id
              AND INSTR(ps.PathString,
                  CASE
                      WHEN RL.StartFacilityID = ps.FacilityID THEN TO_CHAR(RL.EndFacilityID)
                      ELSE TO_CHAR(RL.StartFacilityID)
                  END) = 0  -- Avoid cycles
        ),
        ShortestPath AS (
            SELECT PathString
            FROM PathSearch
            WHERE FacilityID = p_end_facility_id
              AND PathLength = (SELECT MIN(PathLength) FROM PathSearch WHERE FacilityID = p_end_facility_id)
            AND ROWNUM = 1
        )
        SELECT
            TO_NUMBER(
                CASE
                    WHEN LEVEL = 1 THEN
                        SUBSTR(PathString, 1,
                            CASE
                                WHEN INSTR(PathString, ',') = 0 THEN LENGTH(PathString)
                                ELSE INSTR(PathString, ',') - 1
                            END)
                    WHEN INSTR(PathString, ',', 1, LEVEL) = 0 THEN
                        SUBSTR(PathString, INSTR(PathString, ',', 1, LEVEL - 1) + 1)
                    ELSE
                        SUBSTR(PathString,
                            INSTR(PathString, ',', 1, LEVEL - 1) + 1,
                            INSTR(PathString, ',', 1, LEVEL) - INSTR(PathString, ',', 1, LEVEL - 1) - 1)
                END
            ) AS FacilityID,
            LEVEL AS SequenceNumber
        FROM ShortestPath
        WHERE PathString IS NOT NULL
        CONNECT BY LEVEL <= (LENGTH(PathString) - LENGTH(REPLACE(PathString, ',', '')) + 1)
          AND PRIOR PathString = PathString
          AND PRIOR SYS_GUID() IS NOT NULL;
    RETURN v_cursor;
EXCEPTION
    WHEN OTHERS THEN
        -- Return empty cursor if path not found
        OPEN v_cursor FOR
            SELECT NULL AS FacilityID, NULL AS SequenceNumber FROM DUAL WHERE 1=0;
        RETURN v_cursor;
END;
/

-- Function: Get crossings for a specific route
-- Returns crossing operations involving the specified route
CREATE OR REPLACE FUNCTION GET_CROSSINGS_BY_ROUTE_ID(
    p_route_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    -- This function returns crossings detected for a route
    -- Note: Actual crossing detection logic is complex and done in Java
    -- This function returns a placeholder structure
    OPEN v_cursor FOR
        SELECT
            R1.ID AS Route1ID,
            R2.ID AS Route2ID,
            PT1.TrainID AS Train1ID,
            PT2.TrainID AS Train2ID,
            NULL AS CrossingFacilityID,
            NULL AS SidingID,
            NULL AS CrossingTime
        FROM Route R1
        CROSS JOIN Route R2
        INNER JOIN Planned_Train PT1 ON R1.ID = PT1.RouteID
        INNER JOIN Planned_Train PT2 ON R2.ID = PT2.RouteID
        WHERE R1.ID = p_route_id
          AND R1.ID < R2.ID  -- Avoid duplicates
          AND PT1.TrainID != PT2.TrainID
        AND ROWNUM = 0;  -- Return empty for now - actual logic in Java
    RETURN v_cursor;
END;
/

-- Function: Create a train event
-- Returns 1 on success
-- This function generates an ID before inserting to avoid NULL ID errors
CREATE OR REPLACE FUNCTION CREATE_TRAIN_EVENT(
    p_route_id IN NUMBER,
    p_train_id IN NUMBER,
    p_facility_id IN NUMBER,
    p_event_time IN DATE
)
RETURN NUMBER
AS
    v_event_type VARCHAR2(255);
    v_event_id NUMBER;
BEGIN
    v_event_type := 'ROUTE_' || p_route_id;

    -- Get next TrainEvent ID
    SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID) + 1 END
    INTO v_event_id
    FROM TrainEvent;

    INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType)
    VALUES (v_event_id, p_train_id, p_facility_id, p_event_time, v_event_type);

    RETURN 1;
EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
        -- If ID collision, get a new one and retry
        SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID) + 1 END
        INTO v_event_id
        FROM TrainEvent;

        INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType)
        VALUES (v_event_id, p_train_id, p_facility_id, p_event_time, v_event_type);

        RETURN 1;
END;
/

