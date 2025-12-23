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
-- A locomotive is "in transit" if it exists in Assigned_Locomotive table.
-- A locomotive is "parked" if it exists in Parked_Locomotive table.
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
        SELECT 
            ID,
            VehicleModelID,
            TrainOperatorID,
            InitialFacilityID,
            make,
            power,
            acceleration,
            maxSpeed,
            numberOfWheels,
            RouteID,
            DestinationFacilityID,
            DestinationFacilityName,
            ParkedFacilityName
        FROM (
            SELECT 
                locos.*,
                ROW_NUMBER() OVER (PARTITION BY locos.ID ORDER BY 
                    CASE WHEN locos.RouteID IS NOT NULL THEN 1 ELSE 2 END,  -- In-transit first
                    locos.RouteID NULLS LAST  -- If multiple in-transit, pick one arbitrarily
                ) AS rn
            FROM (
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
                    r.EndFacilityID AS DestinationFacilityID,
                    destFacility.name AS DestinationFacilityName,
                    NULL AS ParkedFacilityName
                FROM Locomotive l
                         INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
                         INNER JOIN Assigned_Locomotive AL ON l.ID = AL.LocomotiveID
                         INNER JOIN Planned_Train pt ON AL.PlannedTrainID = pt.TrainID
                    AND AL.PlannedTrainStartDate = pt.startDate
                         INNER JOIN Route r ON pt.RouteID = r.ID
                         INNER JOIN Facility destFacility ON r.EndFacilityID = destFacility.ID
                WHERE pt.RouteID != p_route_id  -- Exclude locomotives already assigned to this route

                UNION ALL

                -- Parked locomotives (in Parked_Locomotive table)
                SELECT
                    l.ID,
                    l.VehicleModelID,
                    l.TrainOperatorID,
                    pl.FacilityID AS InitialFacilityID,  -- Use parking facility from Parked_Locomotive
                    ls.make,
                    ls.power,
                    ls.acceleration,
                    ls.maxSpeed,
                    ls.numberOfWheels,
                    NULL AS RouteID,
                    NULL AS DestinationFacilityID,
                    NULL AS DestinationFacilityName,
                    parkedFacility.name AS ParkedFacilityName
                FROM Locomotive l
                         INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
                         INNER JOIN Parked_Locomotive pl ON l.ID = pl.LocomotiveID
                         INNER JOIN Facility parkedFacility ON pl.FacilityID = parkedFacility.ID
            ) locos
        ) ranked_locos
        WHERE rn = 1  -- Only show one entry per locomotive
        ORDER BY
            CASE WHEN RouteID IS NOT NULL THEN 1 ELSE 2 END,  -- In-transit first
            CASE WHEN RouteID IS NULL THEN
                     -- For parked locomotives, calculate distance from start facility (using parking facility)
                     (SELECT CASE
                                 WHEN EXISTS (
                                     SELECT 1 FROM RailLine rl
                                     WHERE (rl.StartFacilityID = v_start_facility_id AND rl.EndFacilityID = ranked_locos.InitialFacilityID)
                                        OR (rl.EndFacilityID = v_start_facility_id AND rl.StartFacilityID = ranked_locos.InitialFacilityID)
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
                NULL AS DestinationFacilityID,
                NULL AS DestinationFacilityName,
                NULL AS ParkedFacilityName
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
-- A wagon is "in transit" if it exists in Assigned_Wagon table.
-- A wagon is "parked" if it exists in Parked_Wagon table.
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
        SELECT 
            ID,
            VehicleModelID,
            TrainOperatorID,
            InitialFacilityID,
            WagonTypeID,
            volumeCapacity,
            payload,
            tare,
            RouteID,
            DestinationFacilityID,
            DestinationFacilityName,
            ParkedFacilityName
        FROM (
            SELECT 
                wagons.*,
                ROW_NUMBER() OVER (PARTITION BY wagons.ID ORDER BY 
                    CASE WHEN wagons.RouteID IS NOT NULL THEN 1 ELSE 2 END,  -- In-transit first
                    wagons.RouteID NULLS LAST  -- If multiple in-transit, pick one arbitrarily
                ) AS rn
            FROM (
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
                    r.EndFacilityID AS DestinationFacilityID,
                    destFacility.name AS DestinationFacilityName,
                    NULL AS ParkedFacilityName
                FROM Wagon w
                         INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                         INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
                         INNER JOIN Assigned_Wagon AW ON w.ID = AW.WagonID
                         INNER JOIN Planned_Train pt ON AW.PlannedTrainID = pt.TrainID
                    AND AW.PlannedTrainStartDate = pt.startDate
                         INNER JOIN Route r ON pt.RouteID = r.ID
                         INNER JOIN Facility destFacility ON r.EndFacilityID = destFacility.ID
                WHERE pt.RouteID != p_route_id  -- Exclude wagons already assigned to this route

                UNION ALL

                -- Parked wagons (in Parked_Wagon table)
                SELECT
                    w.ID,
                    w.VehicleModelID,
                    w.TrainOperatorID,
                    pw.FacilityID AS InitialFacilityID,  -- Use parking facility from Parked_Wagon
                    ws.WagonTypeID,
                    ws.volumeCapacity,
                    ws.payload,
                    vm.tare,
                    NULL AS RouteID,
                    NULL AS DestinationFacilityID,
                    NULL AS DestinationFacilityName,
                    parkedFacility.name AS ParkedFacilityName
                FROM Wagon w
                         INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                         INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
                         INNER JOIN Parked_Wagon pw ON w.ID = pw.WagonID
                         INNER JOIN Facility parkedFacility ON pw.FacilityID = parkedFacility.ID
            ) wagons
        ) ranked_wagons
        WHERE rn = 1  -- Only show one entry per wagon
        ORDER BY
            CASE WHEN RouteID IS NOT NULL THEN 1 ELSE 2 END,  -- In-transit first
            CASE WHEN RouteID IS NULL THEN
                     -- For parked wagons, calculate distance from start facility
                     (SELECT CASE
                                 WHEN EXISTS (
                                     SELECT 1 FROM RailLine rl
                                     WHERE (rl.StartFacilityID = v_start_facility_id AND rl.EndFacilityID = ranked_wagons.InitialFacilityID)
                                        OR (rl.EndFacilityID = v_start_facility_id AND rl.StartFacilityID = ranked_wagons.InitialFacilityID)
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
                NULL AS DestinationFacilityID,
                NULL AS DestinationFacilityName,
                NULL AS ParkedFacilityName
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
                 INNER JOIN Assigned_Locomotive AL ON l.ID = AL.LocomotiveID
                 INNER JOIN Planned_Train pt ON AL.PlannedTrainID = pt.TrainID
            AND AL.PlannedTrainStartDate = pt.startDate
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
                 INNER JOIN Assigned_Locomotive AL ON l.ID = AL.LocomotiveID
        WHERE AL.PlannedTrainID = p_train_id
          AND AL.PlannedTrainStartDate = p_start_date;
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
                 INNER JOIN Assigned_Wagon AW ON w.ID = AW.WagonID
                 INNER JOIN Planned_Train pt ON AW.PlannedTrainID = pt.TrainID
            AND AW.PlannedTrainStartDate = pt.startDate
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
                 INNER JOIN Assigned_Wagon AW ON w.ID = AW.WagonID
        WHERE AW.PlannedTrainID = p_train_id
          AND AW.PlannedTrainStartDate = p_start_date;
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

-- ============================================================================
-- ASSIGNMENT FUNCTIONS (for train assembly)
-- ============================================================================

-- Function: Assign a wagon to a route (planned train)
-- Removes wagon from Parked_Wagon when assigning it to ensure data integrity
CREATE OR REPLACE FUNCTION ASSIGN_WAGON_TO_ROUTE(
    p_wagon_id IN NUMBER,
    p_route_id  IN NUMBER
) RETURN NUMBER
IS
    v_train_id NUMBER;
    v_start_date DATE;
    v_wagon_exists NUMBER;
    v_already_associated NUMBER;
    v_conflicting_train_id NUMBER;
    v_conflicting_start_date DATE;
BEGIN
    -- 1. Basic validation: wagon ID cannot be null
    IF p_wagon_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Wagon ID cannot be null.');
    END IF;

    -- 2. Basic validation: route ID cannot be null
    IF p_route_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20002, 'Route ID cannot be null.');
    END IF;

    -- 3. Validate that the planned train exists
    BEGIN
        SELECT TrainID, startDate
        INTO v_train_id, v_start_date
        FROM Planned_Train
        WHERE RouteID = p_route_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'Planned train with RouteID ' || p_route_id || ' does not exist.');
    END;

    -- 4. Validate that the wagon exists
    SELECT COUNT(*) INTO v_wagon_exists
    FROM Wagon
    WHERE ID = p_wagon_id;

    IF v_wagon_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Wagon with ID ' || p_wagon_id || ' does not exist.');
    END IF;

    -- 5. Check if wagon is already associated (in Assigned_Wagon)
    SELECT COUNT(*) INTO v_already_associated
    FROM Assigned_Wagon
    WHERE WagonID = p_wagon_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    IF v_already_associated > 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Wagon ' || p_wagon_id || ' is already associated with train ' || v_train_id || ' for the planned trip starting at ' || TO_CHAR(v_start_date, 'YYYY-MM-DD HH24:MI:SS') || '.');
    END IF;

    -- 6. Check if wagon is already assigned to a different train
    BEGIN
        SELECT AW.PlannedTrainID, AW.PlannedTrainStartDate
        INTO v_conflicting_train_id, v_conflicting_start_date
        FROM Assigned_Wagon AW
        JOIN Planned_Train PT ON AW.PlannedTrainID = PT.TrainID AND AW.PlannedTrainStartDate = PT.startDate
        WHERE AW.WagonID = p_wagon_id
          AND PT.RouteID != p_route_id;
        
        RAISE_APPLICATION_ERROR(-20006, 
            'Wagon ' || p_wagon_id || ' is already assigned to train ' || v_conflicting_train_id || 
            ' with a planned route starting at ' || TO_CHAR(v_conflicting_start_date, 'YYYY-MM-DD HH24:MI:SS') || '.');
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            NULL;
    END;

    -- 7. CRITICAL: Remove wagon from Parked_Wagon BEFORE assigning it
    -- This prevents wagons from appearing both parked and in transit
    DELETE FROM Parked_Wagon WHERE WagonID = p_wagon_id;

    -- 8. Insert the association into Assigned_Wagon
    INSERT INTO Assigned_Wagon (WagonID, PlannedTrainID, PlannedTrainStartDate)
    VALUES (p_wagon_id, v_train_id, v_start_date);

    RETURN v_train_id;
END ASSIGN_WAGON_TO_ROUTE;
/

-- Function: Assign a locomotive to a route (planned train)
-- Removes locomotive from Parked_Locomotive when assigning it to ensure data integrity
CREATE OR REPLACE FUNCTION ASSIGN_LOCOMOTIVE_TO_ROUTE(
    p_locomotive_id IN NUMBER,
    p_route_id      IN NUMBER
) RETURN NUMBER
IS
    v_train_id NUMBER;
    v_start_date DATE;
    v_locomotive_exists NUMBER;
    v_already_associated NUMBER;
    v_conflicting_train_id NUMBER;
    v_conflicting_start_date DATE;
BEGIN
    -- 1. Basic validation: locomotive ID cannot be null
    IF p_locomotive_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Locomotive ID cannot be null.');
    END IF;

    -- 2. Basic validation: route ID cannot be null
    IF p_route_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20002, 'Route ID cannot be null.');
    END IF;

    -- 3. Validate that the planned train exists
    BEGIN
        SELECT TrainID, startDate
        INTO v_train_id, v_start_date
        FROM Planned_Train
        WHERE RouteID = p_route_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'Planned train with RouteID ' || p_route_id || ' does not exist.');
    END;

    -- 4. Validate that the locomotive exists
    SELECT COUNT(*) INTO v_locomotive_exists
    FROM Locomotive
    WHERE ID = p_locomotive_id;

    IF v_locomotive_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Locomotive with ID ' || p_locomotive_id || ' does not exist.');
    END IF;

    -- 5. Check if locomotive is already associated (in Assigned_Locomotive)
    SELECT COUNT(*) INTO v_already_associated
    FROM Assigned_Locomotive
    WHERE LocomotiveID = p_locomotive_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    IF v_already_associated > 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Locomotive ' || p_locomotive_id || ' is already associated with train ' || v_train_id || ' for the planned trip starting at ' || TO_CHAR(v_start_date, 'YYYY-MM-DD HH24:MI:SS') || '.');
    END IF;

    -- 6. Check if locomotive is already assigned to a different train
    BEGIN
        SELECT AL.PlannedTrainID, AL.PlannedTrainStartDate
        INTO v_conflicting_train_id, v_conflicting_start_date
        FROM Assigned_Locomotive AL
        JOIN Planned_Train PT ON AL.PlannedTrainID = PT.TrainID AND AL.PlannedTrainStartDate = PT.startDate
        WHERE AL.LocomotiveID = p_locomotive_id
          AND PT.RouteID != p_route_id;
        
        RAISE_APPLICATION_ERROR(-20006, 
            'Locomotive ' || p_locomotive_id || ' is already assigned to train ' || v_conflicting_train_id || 
            ' with a planned route starting at ' || TO_CHAR(v_conflicting_start_date, 'YYYY-MM-DD HH24:MI:SS') || '.');
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            NULL;
    END;

    -- 7. CRITICAL: Remove locomotive from Parked_Locomotive BEFORE assigning it
    DELETE FROM Parked_Locomotive WHERE LocomotiveID = p_locomotive_id;

    -- 8. Insert the association into Assigned_Locomotive
    INSERT INTO Assigned_Locomotive (LocomotiveID, PlannedTrainID, PlannedTrainStartDate)
    VALUES (p_locomotive_id, v_train_id, v_start_date);

    RETURN v_train_id;
END ASSIGN_LOCOMOTIVE_TO_ROUTE;
/

