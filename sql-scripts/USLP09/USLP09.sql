-- USLP09: PL/SQL Functions for Train Assembly
-- This script contains all PL/SQL functions required for USLP09 train assembly functionality.
-- These functions replace direct SQL queries in Java repositories.

-- ============================================================================
-- TRAIN ASSEMBLY FUNCTIONS
-- ============================================================================

-- Function: Get locomotives available for assembly
-- Returns locomotives with:
-- - For in-transit: RouteID and EndFacilityID (final destination of the route)
-- - For parked: InitialFacilityID (where the locomotive is parked)
-- Results are ordered: in-transit first, then parked by distance from route start (descending)
-- A locomotive is "in transit" if it exists in Planned_Train_Locomotive table.
-- A locomotive is "parked" if it does not exist in Planned_Train_Locomotive table.
CREATE OR REPLACE FUNCTION GET_LOCOMOTIVES_FOR_ASSEMBLY(
    p_route_id IN NUMBER
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
    v_start_facility_id NUMBER;
BEGIN
    -- Get the start facility for the route
    SELECT StartFacilityID INTO v_start_facility_id
    FROM Route
    WHERE ID = p_route_id;

    OPEN v_cursor FOR
        SELECT * FROM (
            -- In-transit locomotives (assigned to a planned train)
            SELECT
                l.ID,
                l.VehicleModelID,
                l.TrainOperatorID,
                l.InitialFacilityID,
                ls.make,
                ls.power,
                ls.acceleration,
                ls.maxSpeed,
                ls.numberOfWheels,
                pt.RouteID AS RouteID,
                r.EndFacilityID AS DestinationFacilityID
            FROM Locomotive l
                     INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
                     INNER JOIN Planned_Train_Locomotive ptl ON l.ID = ptl.LocomotiveID
                     INNER JOIN Planned_Train pt ON ptl.PlannedTrainID = pt.TrainID
                AND ptl.PlannedTrainStartDate = pt.startDate
                     INNER JOIN Route r ON pt.RouteID = r.ID
            WHERE pt.RouteID != p_route_id  -- Exclude locomotives already assigned to this route

            UNION ALL

            -- Parked locomotives (not in any planned train)
            SELECT
                l.ID,
                l.VehicleModelID,
                l.TrainOperatorID,
                l.InitialFacilityID,
                ls.make,
                ls.power,
                ls.acceleration,
                ls.maxSpeed,
                ls.numberOfWheels,
                NULL AS RouteID,
                NULL AS DestinationFacilityID
            FROM Locomotive l
                     INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
            WHERE l.ID NOT IN (
                SELECT DISTINCT ptl.LocomotiveID
                FROM Planned_Train_Locomotive ptl
                         INNER JOIN Planned_Train pt ON ptl.PlannedTrainID = pt.TrainID
                    AND ptl.PlannedTrainStartDate = pt.startDate
                WHERE pt.startDate > SYSDATE - 7  -- Consider trains from last 7 days as "active"
            )
        ) locos
        ORDER BY
            CASE WHEN locos.RouteID IS NOT NULL THEN 1 ELSE 2 END,  -- In-transit first
            CASE WHEN locos.RouteID IS NULL THEN
                     -- For parked locomotives, calculate distance from start facility
                     (SELECT CASE
                                 WHEN EXISTS (
                                     SELECT 1 FROM RailLine rl
                                     WHERE (rl.StartFacilityID = v_start_facility_id AND rl.EndFacilityID = locos.InitialFacilityID)
                                        OR (rl.EndFacilityID = v_start_facility_id AND rl.StartFacilityID = locos.InitialFacilityID)
                                 ) THEN 1  -- Direct connection = distance 1
                                 ELSE 999  -- Indirect or no connection = large distance (will appear last)
                                 END FROM DUAL)
                END DESC;  -- Parked by distance descending

    RETURN v_cursor;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        -- If route not found, return empty cursor
        OPEN v_cursor FOR
            SELECT
                l.ID,
                l.VehicleModelID,
                l.TrainOperatorID,
                l.InitialFacilityID,
                ls.make,
                ls.power,
                ls.acceleration,
                ls.maxSpeed,
                ls.numberOfWheels,
                NULL AS RouteID,
                NULL AS DestinationFacilityID
            FROM Locomotive l
                     INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
            WHERE 1 = 0;  -- Return empty result
        RETURN v_cursor;
END;
/

-- Function: Get wagons available for assembly
-- Returns wagons with:
-- - For in-transit: RouteID and EndFacilityID (final destination of the route)
-- - For parked: InitialFacilityID (where the wagon is parked - this is the delivery station where freight was unloaded)
-- Results are ordered: in-transit first, then parked by distance from route start (descending)
-- A wagon is "in transit" if it exists in Planned_Train_Wagon table.
-- A wagon is "parked" if it does not exist in Planned_Train_Wagon table.
-- Note: When freight is delivered, wagons move from Assigned_Freight to Unassigned_Freight,
-- but they remain in Planned_Train_Wagon until the train completes its journey.
CREATE OR REPLACE FUNCTION GET_WAGONS_FOR_ASSEMBLY(
    p_route_id IN NUMBER
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
    v_start_facility_id NUMBER;
BEGIN
    -- Get the start facility for the route
    SELECT StartFacilityID INTO v_start_facility_id
    FROM Route
    WHERE ID = p_route_id;

    OPEN v_cursor FOR
        SELECT * FROM (
            -- In-transit wagons (assigned to a planned train)
            SELECT
                w.ID,
                w.VehicleModelID,
                w.TrainOperatorID,
                w.InitialFacilityID,
                ws.WagonTypeID,
                ws.volumeCapacity,
                ws.payload,
                vm.tare,
                pt.RouteID AS RouteID,
                r.EndFacilityID AS DestinationFacilityID
            FROM Wagon w
                     INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                     INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
                     INNER JOIN Planned_Train_Wagon ptw ON w.ID = ptw.WagonID
                     INNER JOIN Planned_Train pt ON ptw.PlannedTrainID = pt.TrainID
                AND ptw.PlannedTrainStartDate = pt.startDate
                     INNER JOIN Route r ON pt.RouteID = r.ID
            WHERE pt.RouteID != p_route_id  -- Exclude wagons already assigned to this route

            UNION ALL

            -- Parked wagons (not in any planned train)
            SELECT
                w.ID,
                w.VehicleModelID,
                w.TrainOperatorID,
                w.InitialFacilityID,
                ws.WagonTypeID,
                ws.volumeCapacity,
                ws.payload,
                vm.tare,
                NULL AS RouteID,
                NULL AS DestinationFacilityID
            FROM Wagon w
                     INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                     INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
            WHERE w.ID NOT IN (
                SELECT DISTINCT ptw.WagonID
                FROM Planned_Train_Wagon ptw
                         INNER JOIN Planned_Train pt ON ptw.PlannedTrainID = pt.TrainID
                    AND ptw.PlannedTrainStartDate = pt.startDate
                WHERE pt.startDate > SYSDATE - 7  -- Consider trains from last 7 days as "active"
            )
        ) wagons
        ORDER BY
            CASE WHEN wagons.RouteID IS NOT NULL THEN 1 ELSE 2 END,  -- In-transit first
            CASE WHEN wagons.RouteID IS NULL THEN
                     -- For parked wagons, calculate distance from start facility
                     (SELECT CASE
                                 WHEN EXISTS (
                                     SELECT 1 FROM RailLine rl
                                     WHERE (rl.StartFacilityID = v_start_facility_id AND rl.EndFacilityID = wagons.InitialFacilityID)
                                        OR (rl.EndFacilityID = v_start_facility_id AND rl.StartFacilityID = wagons.InitialFacilityID)
                                 ) THEN 1  -- Direct connection = distance 1
                                 ELSE 999  -- Indirect or no connection = large distance (will appear last)
                                 END FROM DUAL)
                END DESC;  -- Parked by distance descending

    RETURN v_cursor;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        -- If route not found, return empty cursor
        OPEN v_cursor FOR
            SELECT
                w.ID,
                w.VehicleModelID,
                w.TrainOperatorID,
                w.InitialFacilityID,
                ws.WagonTypeID,
                ws.volumeCapacity,
                ws.payload,
                vm.tare,
                NULL AS RouteID,
                NULL AS DestinationFacilityID
            FROM Wagon w
                     INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                     INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
            WHERE 1 = 0;  -- Return empty result
        RETURN v_cursor;
END;
/

-- Function: Get locomotive by ID with specifications
-- Used for basic locomotive retrieval
CREATE OR REPLACE FUNCTION GET_LOCOMOTIVE_BY_ID(
    p_locomotive_id IN NUMBER
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            l.ID,
            l.VehicleModelID,
            l.TrainOperatorID,
            l.InitialFacilityID,
            ls.make,
            ls.power,
            ls.acceleration,
            ls.maxSpeed,
            ls.numberOfWheels
        FROM Locomotive l
                 INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
        WHERE l.ID = p_locomotive_id;
    RETURN v_cursor;
END;
/

-- Function: Get wagon by ID with specifications
-- Used for basic wagon retrieval
CREATE OR REPLACE FUNCTION GET_WAGON_BY_ID(
    p_wagon_id IN NUMBER
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            w.ID,
            w.VehicleModelID,
            w.TrainOperatorID,
            w.InitialFacilityID,
            ws.WagonTypeID,
            ws.volumeCapacity,
            ws.payload,
            vm.tare
        FROM Wagon w
                 INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                 INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
        WHERE w.ID = p_wagon_id;
    RETURN v_cursor;
END;
/

-- Function: Check if wagon is loaded (has freight assigned)
CREATE OR REPLACE FUNCTION IS_WAGON_LOADED(
    p_wagon_id IN NUMBER
)
    RETURN NUMBER
AS
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM (
             SELECT 1 FROM Assigned_Freight WHERE WagonID = p_wagon_id
             UNION ALL
             SELECT 1 FROM Unassigned_Freight WHERE WagonID = p_wagon_id
         );

    RETURN CASE WHEN v_count > 0 THEN 1 ELSE 0 END;
END;
/

-- Function: Get locomotives for a specific route (already assigned to the route)
CREATE OR REPLACE FUNCTION GET_LOCOMOTIVES_BY_ROUTE_ID(
    p_route_id IN NUMBER
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            l.ID,
            l.VehicleModelID,
            l.TrainOperatorID,
            l.InitialFacilityID,
            ls.make,
            ls.power,
            ls.acceleration,
            ls.maxSpeed,
            ls.numberOfWheels
        FROM Locomotive l
                 INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
                 INNER JOIN Planned_Train_Locomotive ptl ON l.ID = ptl.LocomotiveID
                 INNER JOIN Planned_Train pt ON ptl.PlannedTrainID = pt.TrainID
            AND ptl.PlannedTrainStartDate = pt.startDate
        WHERE pt.RouteID = p_route_id;
    RETURN v_cursor;
END;
/

-- Function: Get locomotives for a specific train (by train ID and start date)
CREATE OR REPLACE FUNCTION GET_LOCOMOTIVES_BY_TRAIN_ID(
    p_train_id IN NUMBER,
    p_start_date IN DATE
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            l.ID,
            l.VehicleModelID,
            l.TrainOperatorID,
            l.InitialFacilityID,
            ls.make,
            ls.power,
            ls.acceleration,
            ls.maxSpeed,
            ls.numberOfWheels
        FROM Locomotive l
                 INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
                 INNER JOIN Planned_Train_Locomotive ptl ON l.ID = ptl.LocomotiveID
        WHERE ptl.PlannedTrainID = p_train_id
          AND ptl.PlannedTrainStartDate = p_start_date;
    RETURN v_cursor;
END;
/

-- Function: Get wagons for a specific route (already assigned to the route)
CREATE OR REPLACE FUNCTION GET_WAGONS_BY_ROUTE_ID(
    p_route_id IN NUMBER
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            w.ID,
            w.VehicleModelID,
            w.TrainOperatorID,
            w.InitialFacilityID,
            ws.WagonTypeID,
            ws.volumeCapacity,
            ws.payload,
            vm.tare
        FROM Wagon w
                 INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                 INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
                 INNER JOIN Planned_Train_Wagon ptw ON w.ID = ptw.WagonID
                 INNER JOIN Planned_Train pt ON ptw.PlannedTrainID = pt.TrainID
            AND ptw.PlannedTrainStartDate = pt.startDate
        WHERE pt.RouteID = p_route_id;
    RETURN v_cursor;
END;
/

-- Function: Get wagons for a specific train (by train ID and start date)
CREATE OR REPLACE FUNCTION GET_WAGONS_BY_TRAIN_ID(
    p_train_id IN NUMBER,
    p_start_date IN DATE
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            w.ID,
            w.VehicleModelID,
            w.TrainOperatorID,
            w.InitialFacilityID,
            ws.WagonTypeID,
            ws.volumeCapacity,
            ws.payload,
            vm.tare
        FROM Wagon w
                 INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                 INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
                 INNER JOIN Planned_Train_Wagon ptw ON w.ID = ptw.WagonID
        WHERE ptw.PlannedTrainID = p_train_id
          AND ptw.PlannedTrainStartDate = p_start_date;
    RETURN v_cursor;
END;
/

-- Function: Get train ID for a route
CREATE OR REPLACE FUNCTION GET_TRAIN_ID_BY_ROUTE_ID(
    p_route_id IN NUMBER
)
    RETURN NUMBER
AS
    v_train_id NUMBER;
BEGIN
    SELECT TrainID INTO v_train_id
    FROM Planned_Train
    WHERE RouteID = p_route_id
      AND ROWNUM = 1;

    RETURN v_train_id;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END;
/

-- Function: Get train by ID
CREATE OR REPLACE FUNCTION GET_TRAIN_BY_ID(
    p_train_id IN NUMBER
)
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, TrainOperatorID
        FROM Train
        WHERE ID = p_train_id;
    RETURN v_cursor;
END;
/

-- Function: Get all trains
CREATE OR REPLACE FUNCTION GET_ALL_TRAINS
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, TrainOperatorID
        FROM Train
        ORDER BY ID;
    RETURN v_cursor;
END;
/

-- Function: Get all locomotives with specifications
CREATE OR REPLACE FUNCTION GET_ALL_LOCOMOTIVES
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            l.ID,
            l.VehicleModelID,
            l.TrainOperatorID,
            l.InitialFacilityID,
            ls.make,
            ls.power,
            ls.acceleration,
            ls.maxSpeed,
            ls.numberOfWheels
        FROM Locomotive l
                 INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
        ORDER BY l.ID;
    RETURN v_cursor;
END;
/

-- Function: Get all wagons with specifications
CREATE OR REPLACE FUNCTION GET_ALL_WAGONS
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT
            w.ID,
            w.VehicleModelID,
            w.TrainOperatorID,
            w.InitialFacilityID,
            ws.WagonTypeID,
            ws.volumeCapacity,
            ws.payload,
            vm.tare
        FROM Wagon w
                 INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                 INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
        ORDER BY w.ID;
    RETURN v_cursor;
END;
/

