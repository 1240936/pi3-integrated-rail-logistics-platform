-- USLP08: PL/SQL Functions for Route Planner Database Access
-- This script contains all PL/SQL functions required for USLP08 route planner functionality.
-- These functions replace direct SQL queries in Java repositories.

-- Function: Get all unassigned freight
-- Returns a cursor with freight information (ID, OriginFacilityID, DestinationFacilityID)
CREATE OR REPLACE FUNCTION GET_UNASSIGNED_FREIGHT
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT DISTINCT f.ID, f.OriginFacilityID, f.DestinationFacilityID
        FROM Freight f
        INNER JOIN Unassigned_Freight uf ON f.ID = uf.FreightID
        ORDER BY f.ID;
    RETURN v_cursor;
END;
/

-- Function: Get freight by route ID and start date
-- Returns a cursor with freight information for a specific route
CREATE OR REPLACE FUNCTION GET_FREIGHT_BY_ROUTE(
    p_route_id IN NUMBER,
    p_start_date IN DATE
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT DISTINCT f.ID, f.OriginFacilityID, f.DestinationFacilityID
        FROM Freight f
        INNER JOIN Assigned_Freight af ON f.ID = af.FreightID
        INNER JOIN Planned_Train pt ON af.PlannedTrainID = pt.TrainID 
                                      AND af.PlannedTrainStartDate = pt.startDate
        WHERE pt.RouteID = p_route_id 
          AND pt.startDate = p_start_date
        ORDER BY f.ID;
    RETURN v_cursor;
END;
/

-- Function: Get freight pickups by facility for a route
-- Returns a cursor with freight information grouped by origin facility
CREATE OR REPLACE FUNCTION GET_FREIGHT_PICKUPS_BY_FACILITY(
    p_route_id IN NUMBER,
    p_start_date IN DATE
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT DISTINCT f.ID, f.OriginFacilityID, f.DestinationFacilityID
        FROM Freight f
        INNER JOIN Assigned_Freight af ON f.ID = af.FreightID
        INNER JOIN Planned_Train pt ON af.PlannedTrainID = pt.TrainID 
                                      AND af.PlannedTrainStartDate = pt.startDate
        WHERE pt.RouteID = p_route_id 
          AND pt.startDate = p_start_date
        ORDER BY f.OriginFacilityID, f.ID;
    RETURN v_cursor;
END;
/

-- Function: Get freight deliveries by facility for a route
-- Returns a cursor with freight information grouped by destination facility
CREATE OR REPLACE FUNCTION GET_FREIGHT_DELIVERIES_BY_FACILITY(
    p_route_id IN NUMBER,
    p_start_date IN DATE
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT DISTINCT f.ID, f.OriginFacilityID, f.DestinationFacilityID
        FROM Freight f
        INNER JOIN Assigned_Freight af ON f.ID = af.FreightID
        INNER JOIN Planned_Train pt ON af.PlannedTrainID = pt.TrainID 
                                      AND af.PlannedTrainStartDate = pt.startDate
        WHERE pt.RouteID = p_route_id 
          AND pt.startDate = p_start_date
        ORDER BY f.DestinationFacilityID, f.ID;
    RETURN v_cursor;
END;
/

-- Function: Get freight by ID
-- Returns a cursor with freight information
CREATE OR REPLACE FUNCTION GET_FREIGHT_BY_ID(
    p_freight_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, OriginFacilityID, DestinationFacilityID
        FROM Freight
        WHERE ID = p_freight_id;
    RETURN v_cursor;
END;
/

-- Function: Get wagon IDs for a freight
-- Returns a cursor with wagon IDs (from both Assigned_Freight and Unassigned_Freight)
CREATE OR REPLACE FUNCTION GET_WAGON_IDS_BY_FREIGHT_ID(
    p_freight_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT WagonID
        FROM Assigned_Freight
        WHERE FreightID = p_freight_id
        UNION
        SELECT WagonID
        FROM Unassigned_Freight
        WHERE FreightID = p_freight_id
        ORDER BY WagonID;
    RETURN v_cursor;
END;
/

-- Function: Assign freight to route
-- Moves freight from Unassigned_Freight to Assigned_Freight
-- Also ensures wagons are in Assigned_Wagon (moves from Parked_Wagon if needed)
-- Returns 1 on success, raises exception on failure
CREATE OR REPLACE FUNCTION ASSIGN_FREIGHT_TO_ROUTE(
    p_freight_id IN NUMBER,
    p_route_id IN NUMBER
)
RETURN NUMBER
AS
    v_planned_train_id NUMBER;
    v_planned_train_start_date DATE;
    v_wagon_id NUMBER;
    v_wagon_exists NUMBER;
    CURSOR c_unassigned_wagons IS
        SELECT WagonID
        FROM Unassigned_Freight
        WHERE FreightID = p_freight_id;
BEGIN
    -- Get Planned_Train info for this route
    SELECT TrainID, startDate
    INTO v_planned_train_id, v_planned_train_start_date
    FROM Planned_Train
    WHERE RouteID = p_route_id
      AND ROWNUM = 1;
    
    IF v_planned_train_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Route ' || p_route_id || 
            ' does not have a Planned_Train entry. Cannot assign freight.');
    END IF;
    
    -- Process each unassigned wagon for this freight
    FOR wagon_rec IN c_unassigned_wagons LOOP
        v_wagon_id := wagon_rec.WagonID;
        
        -- Check if wagon is already in Assigned_Wagon for this planned train
        SELECT COUNT(*)
        INTO v_wagon_exists
        FROM Assigned_Wagon
        WHERE PlannedTrainID = v_planned_train_id
          AND PlannedTrainStartDate = v_planned_train_start_date
          AND WagonID = v_wagon_id;
        
        -- If not, move it from Parked_Wagon to Assigned_Wagon
        IF v_wagon_exists = 0 THEN
            DELETE FROM Parked_Wagon WHERE WagonID = v_wagon_id;
            INSERT INTO Assigned_Wagon (WagonID, PlannedTrainID, PlannedTrainStartDate)
            VALUES (v_wagon_id, v_planned_train_id, v_planned_train_start_date);
        END IF;
        
        -- Delete from Unassigned_Freight
        DELETE FROM Unassigned_Freight
        WHERE FreightID = p_freight_id
          AND WagonID = v_wagon_id;
        
        -- Insert into Assigned_Freight
        INSERT INTO Assigned_Freight (FreightID, WagonID, PlannedTrainID, PlannedTrainStartDate)
        VALUES (p_freight_id, v_wagon_id, v_planned_train_id, v_planned_train_start_date);
    END LOOP;
    
    -- Check if any wagons were processed
    SELECT COUNT(*)
    INTO v_wagon_exists
    FROM Assigned_Freight
    WHERE FreightID = p_freight_id
      AND PlannedTrainID = v_planned_train_id
      AND PlannedTrainStartDate = v_planned_train_start_date;
    
    IF v_wagon_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Freight ' || p_freight_id || 
            ' has no unassigned wagons. It may already be assigned or has no wagons.');
    END IF;
    
    RETURN 1;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20003, 'Route ' || p_route_id || 
            ' does not have a Planned_Train entry. Cannot assign freight.');
END;
/

-- Function: Get route by ID with basic info
-- Returns a cursor with route information (ID, StartFacilityID, EndFacilityID, TrainID, startDate)
CREATE OR REPLACE FUNCTION GET_ROUTE_BY_ID(
    p_route_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT r.ID, r.StartFacilityID, r.EndFacilityID, pt.TrainID, pt.startDate
        FROM Route r
        LEFT JOIN Planned_Train pt ON r.ID = pt.RouteID
        WHERE r.ID = p_route_id
          AND ROWNUM = 1;
    RETURN v_cursor;
END;
/

-- Function: Get path points for a route
-- Returns a cursor with path point information (FacilityID, seqNumber)
CREATE OR REPLACE FUNCTION GET_PATH_POINTS_BY_ROUTE_ID(
    p_route_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT FacilityID, seqNumber
        FROM Path
        WHERE RouteID = p_route_id
        ORDER BY seqNumber;
    RETURN v_cursor;
END;
/

-- Function: Get routes by train ID
-- Returns a cursor with route IDs for a specific train
CREATE OR REPLACE FUNCTION GET_ROUTES_BY_TRAIN_ID(
    p_train_id IN NUMBER
)
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT DISTINCT RouteID
        FROM Planned_Train
        WHERE TrainID = p_train_id
        ORDER BY RouteID;
    RETURN v_cursor;
END;
/

-- Function: Create a new route
-- Creates Route entry and Planned_Train entry
-- Returns the new route ID
CREATE OR REPLACE FUNCTION CREATE_ROUTE(
    p_train_id IN NUMBER,
    p_start_facility_id IN NUMBER,
    p_end_facility_id IN NUMBER,
    p_start_date IN DATE
)
RETURN NUMBER
AS
    v_route_id NUMBER;
BEGIN
    -- Get next Route ID
    SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID) + 1 END
    INTO v_route_id
    FROM Route;
    
    -- Insert into Route table
    INSERT INTO Route (ID, StartFacilityID, EndFacilityID)
    VALUES (v_route_id, p_start_facility_id, p_end_facility_id);
    
    -- Insert into Planned_Train
    INSERT INTO Planned_Train (TrainID, startDate, RouteID)
    VALUES (p_train_id, p_start_date, v_route_id);
    
    RETURN v_route_id;
EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
        -- If ID collision, get a new one and retry
        SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID) + 1 END
        INTO v_route_id
        FROM Route;
        
        INSERT INTO Route (ID, StartFacilityID, EndFacilityID)
        VALUES (v_route_id, p_start_facility_id, p_end_facility_id);
        
        INSERT INTO Planned_Train (TrainID, startDate, RouteID)
        VALUES (p_train_id, p_start_date, v_route_id);
        
        RETURN v_route_id;
END;
/

-- Function: Add a path point to a route
-- Returns 1 on success
CREATE OR REPLACE FUNCTION ADD_PATH_POINT(
    p_route_id IN NUMBER,
    p_facility_id IN NUMBER,
    p_sequence_number IN NUMBER
)
RETURN NUMBER
AS
BEGIN
    INSERT INTO Path (RouteID, FacilityID, seqNumber)
    VALUES (p_route_id, p_facility_id, p_sequence_number);
    
    RETURN 1;
END;
/

-- Function: Get start date for a route
-- Returns the startDate from Planned_Train for a given route
CREATE OR REPLACE FUNCTION GET_START_DATE_BY_ROUTE_ID(
    p_route_id IN NUMBER
)
RETURN DATE
AS
    v_start_date DATE;
BEGIN
    SELECT startDate INTO v_start_date
    FROM Planned_Train
    WHERE RouteID = p_route_id
      AND ROWNUM = 1;
    
    RETURN v_start_date;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END;
/

-- Function: Get all route IDs
-- Returns a cursor with all route IDs
CREATE OR REPLACE FUNCTION GET_ALL_ROUTE_IDS
RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT ID
        FROM Route
        ORDER BY ID;
    RETURN v_cursor;
END;
/

-- Function: Delete a route and all related records
-- Deletes route, planned trains, path points, moves wagons/locomotives back to parked, and moves freight back to unassigned
-- Returns 1 on success, 0 if route not found
CREATE OR REPLACE FUNCTION DELETE_ROUTE(
    p_route_id IN NUMBER
)
RETURN NUMBER
AS
    v_train_id NUMBER;
    v_start_date DATE;
    v_freight_id NUMBER;
    v_wagon_id NUMBER;
    v_locomotive_id NUMBER;
    v_end_facility_id NUMBER;
    CURSOR c_planned_trains IS
        SELECT pt.TrainID, pt.startDate, r.EndFacilityID
        FROM Planned_Train pt
        JOIN Route r ON pt.RouteID = r.ID
        WHERE pt.RouteID = p_route_id;
    CURSOR c_assigned_freight(p_train_id NUMBER, p_start_date DATE) IS
        SELECT FreightID, WagonID
        FROM Assigned_Freight
        WHERE PlannedTrainID = p_train_id
          AND PlannedTrainStartDate = p_start_date;
    CURSOR c_assigned_wagons(p_train_id NUMBER, p_start_date DATE) IS
        SELECT WagonID
        FROM Assigned_Wagon
        WHERE PlannedTrainID = p_train_id
          AND PlannedTrainStartDate = p_start_date;
    CURSOR c_assigned_locomotives(p_train_id NUMBER, p_start_date DATE) IS
        SELECT LocomotiveID
        FROM Assigned_Locomotive
        WHERE PlannedTrainID = p_train_id
          AND PlannedTrainStartDate = p_start_date;
BEGIN
    -- Process each planned train for this route
    FOR planned_train_rec IN c_planned_trains LOOP
        v_train_id := planned_train_rec.TrainID;
        v_start_date := planned_train_rec.startDate;
        v_end_facility_id := planned_train_rec.EndFacilityID;
        
        -- Move Assigned_Freight back to Unassigned_Freight
        FOR freight_rec IN c_assigned_freight(v_train_id, v_start_date) LOOP
            v_freight_id := freight_rec.FreightID;
            v_wagon_id := freight_rec.WagonID;
            
            -- Try to insert into Unassigned_Freight (ignore if already exists)
            BEGIN
                INSERT INTO Unassigned_Freight (FreightID, WagonID)
                VALUES (v_freight_id, v_wagon_id);
            EXCEPTION
                WHEN DUP_VAL_ON_INDEX THEN
                    NULL;  -- Already exists, ignore
            END;
        END LOOP;
        
        -- Delete from Assigned_Freight
        DELETE FROM Assigned_Freight
        WHERE PlannedTrainID = v_train_id
          AND PlannedTrainStartDate = v_start_date;
        
        -- Move wagons from Assigned_Wagon to Parked_Wagon (at route end facility)
        FOR wagon_rec IN c_assigned_wagons(v_train_id, v_start_date) LOOP
            v_wagon_id := wagon_rec.WagonID;
            DELETE FROM Assigned_Wagon WHERE WagonID = v_wagon_id;
            INSERT INTO Parked_Wagon (WagonID, FacilityID) VALUES (v_wagon_id, v_end_facility_id);
        END LOOP;
        
        -- Move locomotives from Assigned_Locomotive to Parked_Locomotive (at route end facility)
        FOR loco_rec IN c_assigned_locomotives(v_train_id, v_start_date) LOOP
            v_locomotive_id := loco_rec.LocomotiveID;
            DELETE FROM Assigned_Locomotive WHERE LocomotiveID = v_locomotive_id;
            INSERT INTO Parked_Locomotive (LocomotiveID, FacilityID) VALUES (v_locomotive_id, v_end_facility_id);
        END LOOP;
    END LOOP;
    
    -- Delete Path points
    DELETE FROM Path
    WHERE RouteID = p_route_id;
    
    -- Delete Planned_Train
    DELETE FROM Planned_Train
    WHERE RouteID = p_route_id;
    
    -- Delete TrainEvent by eventType (contains route ID)
    DELETE FROM TrainEvent
    WHERE eventType = 'ROUTE_' || p_route_id;
    
    -- Finally delete the Route
    DELETE FROM Route
    WHERE ID = p_route_id;
    
    -- Check if route was actually deleted
    IF SQL%ROWCOUNT > 0 THEN
        RETURN 1;
    ELSE
        RETURN 0;
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        -- If any error occurs, rollback would be needed (but we can't do that in a function)
        -- Return 0 to indicate failure
        RETURN 0;
END;
/

-- Function: Create a train event
-- Returns 1 on success
CREATE OR REPLACE FUNCTION CREATE_TRAIN_EVENT(
    p_route_id IN NUMBER,
    p_train_id IN NUMBER,
    p_facility_id IN NUMBER,
    p_event_time IN DATE
)
RETURN NUMBER
AS
    v_event_type VARCHAR2(255);
BEGIN
    v_event_type := 'ROUTE_' || p_route_id;
    
    INSERT INTO TrainEvent (TrainID, FacilityID, eventTime, eventType)
    VALUES (p_train_id, p_facility_id, p_event_time, v_event_type);
    
    RETURN 1;
END;
/

-- Function: Delete train events by route ID
-- Deletes events identified by eventType containing route ID
-- Returns number of deleted rows
CREATE OR REPLACE FUNCTION DELETE_TRAIN_EVENTS_BY_ROUTE_ID(
    p_route_id IN NUMBER
)
RETURN NUMBER
AS
    v_deleted_count NUMBER;
BEGIN
    DELETE FROM TrainEvent
    WHERE eventType = 'ROUTE_' || p_route_id;
    
    v_deleted_count := SQL%ROWCOUNT;
    
    -- If no rows deleted, try alternative approach: delete by train ID
    IF v_deleted_count = 0 THEN
        DELETE FROM TrainEvent
        WHERE TrainID IN (
            SELECT TrainID
            FROM Planned_Train
            WHERE RouteID = p_route_id
        );
        v_deleted_count := SQL%ROWCOUNT;
    END IF;
    
    RETURN v_deleted_count;
END;
/

