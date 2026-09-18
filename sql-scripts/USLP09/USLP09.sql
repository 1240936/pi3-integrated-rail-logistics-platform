-- USLP09: PL/SQL Functions for Train Assembly
-- This script contains all PL/SQL functions required for USLP09 train assembly functionality.
-- These functions replace direct SQL queries in Java repositories.

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

-- Function: Calculate distance (in km) between two facilities along a route path
-- If route_id is provided, calculates distance along the route path from facility1 (route start) to facility2
-- If facility2 is on the route path, sums all LineSegment lengths from route start to facility2
-- If facility2 connects to a facility on the route path, calculates: route distance to connection point + direct connection
-- Otherwise checks for direct RailLine connection
-- Returns NULL if facilities are not connected
CREATE OR REPLACE FUNCTION CALCULATE_DISTANCE_BETWEEN_FACILITIES(
    p_facility_id1 IN NUMBER,
    p_facility_id2 IN NUMBER,
    p_route_id IN NUMBER DEFAULT NULL
)
    RETURN NUMBER
AS
    v_distance                   NUMBER := NULL;
    v_route_start_id             NUMBER;
    v_target_seq                 NUMBER;
    v_route_end_id               NUMBER;
    v_connected_facility_id      NUMBER;
    v_direct_connection_distance NUMBER;
BEGIN
    -- If same facility, distance is 0
    IF p_facility_id1 = p_facility_id2 THEN
        RETURN 0;
    END IF;

    -- If route_id provided, calculate distance along route path
    IF p_route_id IS NOT NULL THEN
        BEGIN
            -- Get route start and end facilities
            SELECT StartFacilityID, EndFacilityID
            INTO v_route_start_id, v_route_end_id
            FROM Route
            WHERE ID = p_route_id;

            -- Facility1 should be the route start facility
            IF p_facility_id1 != v_route_start_id THEN
                -- If facility1 is not route start, this is not a route path calculation
                -- Fall through to direct connection check
                RAISE NO_DATA_FOUND;
            END IF;

            -- Check if facility2 is the route start (distance = 0, already handled above)
            IF p_facility_id2 = v_route_start_id THEN
                RETURN 0;
            END IF;

            -- First check if facility2 is directly on the route path
            BEGIN
                IF p_facility_id2 = v_route_end_id THEN
                    v_target_seq := 999999; -- Use high number to include all segments
                ELSE
                    SELECT MIN(seqNumber)
                    INTO v_target_seq
                    FROM Path
                    WHERE RouteID = p_route_id
                      AND FacilityID = p_facility_id2;
                END IF;

                -- Facility is on route path - calculate distance along route
                NULL; -- Will calculate below
            EXCEPTION
                WHEN NO_DATA_FOUND THEN
                    -- Facility2 not directly on route path - check if it connects to a route facility
                    BEGIN
                        -- Find if facility2 has a direct RailLine connection to any facility on the route path
                        SELECT DISTINCT CASE
                                            WHEN p_facility_id2 = rl.StartFacilityID THEN rl.EndFacilityID
                                            WHEN p_facility_id2 = rl.EndFacilityID THEN rl.StartFacilityID
                                            END
                        INTO v_connected_facility_id
                        FROM RailLine rl
                        WHERE (rl.StartFacilityID = p_facility_id2 OR rl.EndFacilityID = p_facility_id2)
                          AND EXISTS (
                            -- Check if the connected facility is on the route path
                            SELECT 1
                            FROM Route r
                            WHERE r.ID = p_route_id
                              AND (r.StartFacilityID = CASE
                                                           WHEN p_facility_id2 = rl.StartFacilityID
                                                               THEN rl.EndFacilityID
                                                           ELSE rl.StartFacilityID
                                END
                                OR r.EndFacilityID = CASE
                                                         WHEN p_facility_id2 = rl.StartFacilityID THEN rl.EndFacilityID
                                                         ELSE rl.StartFacilityID
                                    END
                                OR EXISTS (SELECT 1
                                           FROM Path p
                                           WHERE p.RouteID = p_route_id
                                             AND p.FacilityID = CASE
                                                                    WHEN p_facility_id2 = rl.StartFacilityID
                                                                        THEN rl.EndFacilityID
                                                                    ELSE rl.StartFacilityID
                                               END)
                                ))
                          AND ROWNUM = 1;
                        -- Take first connection found

                        -- Get the sequence number of the connected facility on the route
                        IF v_connected_facility_id = v_route_end_id THEN
                            v_target_seq := 999999;
                        ELSE
                            SELECT MIN(seqNumber)
                            INTO v_target_seq
                            FROM Path
                            WHERE RouteID = p_route_id
                              AND FacilityID = v_connected_facility_id;
                        END IF;

                        -- Calculate direct connection distance from facility2 to connected facility
                        SELECT COALESCE(SUM(ls.length) / 1000.0, 0)
                        INTO v_direct_connection_distance
                        FROM RailLine rl
                                 JOIN LineSegment ls ON ls.RailLineID = rl.ID
                        WHERE (rl.StartFacilityID = p_facility_id2 AND rl.EndFacilityID = v_connected_facility_id)
                           OR (rl.StartFacilityID = v_connected_facility_id AND rl.EndFacilityID = p_facility_id2);

                    EXCEPTION
                        WHEN NO_DATA_FOUND THEN
                            -- No connection to route path found
                            RAISE;
                    END;
            END;

            -- Calculate cumulative distance along route path from start to target facility (or connection point)
            SELECT COALESCE(SUM(ls.length) / 1000.0, 0)
            INTO v_distance
            FROM (
                     -- Get all route segments (facility pairs) up to the target facility
                     -- Segment 1: Route start -> First path point
                     SELECT v_route_start_id                                                                 AS from_fac,
                            (SELECT FacilityID
                             FROM Path
                             WHERE RouteID = p_route_id
                               AND seqNumber = (SELECT MIN(seqNumber) FROM Path WHERE RouteID = p_route_id)) AS to_fac,
                            0                                                                                AS seg_seq
                     FROM DUAL
                     WHERE (SELECT MIN(seqNumber) FROM Path WHERE RouteID = p_route_id) <= v_target_seq

                     UNION ALL

                     -- Segments between consecutive path points (up to target)
                     SELECT p1.FacilityID AS from_fac,
                            p2.FacilityID AS to_fac,
                            p1.seqNumber  AS seg_seq
                     FROM Path p1
                              JOIN Path p2 ON p2.RouteID = p1.RouteID AND p2.seqNumber = p1.seqNumber + 1
                     WHERE p1.RouteID = p_route_id
                       AND p1.seqNumber < v_target_seq

                     UNION ALL

                     -- Last segment: Last path point before target -> Target facility (or connection point)
                     SELECT p.FacilityID AS from_fac,
                            CASE
                                WHEN v_target_seq >= 999999 THEN v_route_end_id
                                WHEN v_connected_facility_id IS NOT NULL THEN v_connected_facility_id
                                ELSE p_facility_id2
                                END      AS to_fac,
                            p.seqNumber  AS seg_seq
                     FROM Path p
                     WHERE p.RouteID = p_route_id
                       AND p.seqNumber = v_target_seq - 1
                       AND v_target_seq < 999999
                       AND (v_connected_facility_id IS NOT NULL OR p_facility_id2 != v_route_end_id)) route_segments
                     JOIN RailLine rl ON (
                (rl.StartFacilityID = route_segments.from_fac AND rl.EndFacilityID = route_segments.to_fac)
                    OR (rl.StartFacilityID = route_segments.to_fac AND rl.EndFacilityID = route_segments.from_fac)
                )
                     JOIN LineSegment ls ON ls.RailLineID = rl.ID
            ORDER BY route_segments.seg_seq, ls.orderNum;

            -- Add direct connection distance if facility connects to route via intermediate facility
            IF v_direct_connection_distance IS NOT NULL AND v_direct_connection_distance > 0 THEN
                v_distance := COALESCE(v_distance, 0) + v_direct_connection_distance;
            END IF;

            IF v_distance > 0 THEN
                RETURN v_distance;
            END IF;

        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                -- Facility not on route path, fall through to direct connection check
                NULL;
            WHEN OTHERS THEN
                -- Any error, fall through to direct connection check
                NULL;
        END;
    END IF;

    -- Fallback: Check for direct RailLine connection
    BEGIN
        SELECT COALESCE(SUM(ls.length) / 1000.0, 0)
        INTO v_distance
        FROM RailLine rl
                 JOIN LineSegment ls ON ls.RailLineID = rl.ID
        WHERE (rl.StartFacilityID = p_facility_id1 AND rl.EndFacilityID = p_facility_id2)
           OR (rl.StartFacilityID = p_facility_id2 AND rl.EndFacilityID = p_facility_id1);

        IF v_distance > 0 THEN
            RETURN v_distance;
        END IF;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            NULL;
    END;

    -- If no connection found, return NULL
    RETURN NULL;
END;
/

-- Function: Check if a date/time conflicts with existing planned train for a route
-- Returns 1 if conflict exists, 0 otherwise
CREATE OR REPLACE FUNCTION CHECK_PLANNED_TRAIN_DATE_CONFLICT(
    p_route_id IN NUMBER,
    p_start_date IN DATE
)
    RETURN NUMBER
AS
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM Planned_Train
    WHERE RouteID = p_route_id
      AND startDate = p_start_date;

    RETURN CASE WHEN v_count > 0 THEN 1 ELSE 0 END;
END;
/

-- ============================================================================
-- TRAIN ASSEMBLY FUNCTIONS
-- ============================================================================

-- Function: Get locomotives available for assembly
-- Returns locomotives with:
-- - For in-transit: RouteID and EndFacilityID (final destination of the route)
-- - For parked: InitialFacilityID (where the locomotive is parked) and distance in km
-- Results are ordered: in-transit first, then parked by distance from route start (descending)
-- A locomotive is "in transit" if it exists in Assigned_Locomotive table at the requested time.
-- A locomotive is "parked" if it exists in Parked_Locomotive table.
-- Only shows locomotives that are available (parked) or in-transit at the requested time.
CREATE OR REPLACE FUNCTION GET_LOCOMOTIVES_FOR_ASSEMBLY(
    p_route_id IN NUMBER,
    p_requested_start_date IN DATE
)
    RETURN SYS_REFCURSOR
AS
    v_cursor            SYS_REFCURSOR;
    v_start_facility_id NUMBER;
BEGIN
    -- Get the start facility for the route
    SELECT StartFacilityID
    INTO v_start_facility_id
    FROM Route
    WHERE ID = p_route_id;

    -- Ensure completed locomotives (more than 1 hour since train start) are in Parked_Locomotive
    -- We don't delete from Assigned_Locomotive to preserve historical assignments for viewing
    -- But we update/insert them to Parked_Locomotive at the route's end facility so they're available for new assignments
    -- Use MERGE to update existing parked locomotives or insert new ones
    -- For locomotives that completed multiple trains, use the most recent completed train's route end facility
    MERGE INTO Parked_Locomotive PL
    USING (SELECT LocomotiveID, RouteEndFacilityID
           FROM (SELECT AL.LocomotiveID,
                        r.EndFacilityID                                                             AS RouteEndFacilityID,
                        ROW_NUMBER() OVER (PARTITION BY AL.LocomotiveID ORDER BY pt.startDate DESC) AS rn
                 FROM Assigned_Locomotive AL
                          INNER JOIN Planned_Train pt ON AL.PlannedTrainID = pt.TrainID
                     AND AL.PlannedTrainStartDate = pt.startDate
                          INNER JOIN Route r ON pt.RouteID = r.ID
                 WHERE pt.startDate + 1 / 24 < p_requested_start_date -- More than 1 hour has passed
                )
           WHERE rn = 1 -- Only the most recent completed train for each locomotive
    ) completed_trains
    ON (PL.LocomotiveID = completed_trains.LocomotiveID)
    WHEN MATCHED THEN
        UPDATE SET PL.FacilityID = completed_trains.RouteEndFacilityID
    WHEN NOT MATCHED THEN
        INSERT (LocomotiveID, FacilityID) VALUES (completed_trains.LocomotiveID, completed_trains.RouteEndFacilityID);

    OPEN v_cursor FOR
        SELECT ID,
               VehicleModelID,
               TrainOperatorID,
               InitialFacilityID,
               make,
               power,
               acceleration,
               operationalSpeed,
               maxSpeed,
               numberOfWheels,
               RouteID,
               DestinationFacilityID,
               DestinationFacilityName,
               ParkedFacilityName,
               DistanceFromStartKm
        FROM (SELECT locos.*,
                     ROW_NUMBER() OVER (PARTITION BY locos.ID ORDER BY
                         CASE
                             WHEN locos.RouteID IS NOT NULL AND locos.IsInTransitAtRequestedTime = 1 THEN 1
                             ELSE 2 END, -- In-transit at requested time first
                         locos.RouteID NULLS LAST -- If multiple in-transit, pick one arbitrarily
                         ) AS rn
              FROM (
                       -- In-transit locomotives at the requested time (assigned to a planned train at the requested time)
                       -- Show ALL in-transit locomotives, even if assigned to the same route (they're just not selectable)
                       SELECT l.ID,
                              l.VehicleModelID,
                              l.TrainOperatorID,
                              l.InitialFacilityID,
                              ls.make,
                              ls.power,
                              ls.acceleration,
                              ls.operationalSpeed,
                              ls.maxSpeed,
                              ls.numberOfWheels,
                              pt.RouteID        AS RouteID,
                              r.EndFacilityID   AS DestinationFacilityID,
                              destFacility.name AS DestinationFacilityName,
                              NULL              AS ParkedFacilityName,
                              NULL              AS DistanceFromStartKm,
                              1                 AS IsInTransitAtRequestedTime
                       FROM Locomotive l
                                INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
                                INNER JOIN Assigned_Locomotive AL ON l.ID = AL.LocomotiveID
                                INNER JOIN Planned_Train pt ON AL.PlannedTrainID = pt.TrainID
                           AND AL.PlannedTrainStartDate = pt.startDate
                                INNER JOIN Route r ON pt.RouteID = r.ID
                                INNER JOIN Facility destFacility ON r.EndFacilityID = destFacility.ID
                       WHERE pt.startDate <= p_requested_start_date          -- Show as in-transit if assigned to train starting on or before requested time
                         AND pt.startDate + 1 / 24 >= p_requested_start_date -- Locomotives/wagons become available 1 hour after train start

                       UNION ALL

                       -- Parked locomotives (in Parked_Locomotive table) - available for selection
                       SELECT l.ID,
                              l.VehicleModelID,
                              l.TrainOperatorID,
                              pl.FacilityID                                     AS InitialFacilityID, -- Use parking facility from Parked_Locomotive
                              ls.make,
                              ls.power,
                              ls.acceleration,
                              ls.operationalSpeed,
                              ls.maxSpeed,
                              ls.numberOfWheels,
                              NULL                                              AS RouteID,
                              NULL                                              AS DestinationFacilityID,
                              NULL                                              AS DestinationFacilityName,
                              parkedFacility.name                               AS ParkedFacilityName,
                              CALCULATE_DISTANCE_BETWEEN_FACILITIES(v_start_facility_id, pl.FacilityID,
                                                                    p_route_id) AS DistanceFromStartKm,
                              0                                                 AS IsInTransitAtRequestedTime
                       FROM Locomotive l
                                INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
                                INNER JOIN Parked_Locomotive pl ON l.ID = pl.LocomotiveID
                                INNER JOIN Facility parkedFacility ON pl.FacilityID = parkedFacility.ID
                       WHERE NOT EXISTS (
                           -- Exclude if locomotive is assigned to a planned train that overlaps with requested time
                           -- (assigned to train starting on or before requested time, and train hasn't completed yet)
                           SELECT 1
                           FROM Assigned_Locomotive AL2
                                    INNER JOIN Planned_Train pt2 ON AL2.PlannedTrainID = pt2.TrainID
                               AND AL2.PlannedTrainStartDate = pt2.startDate
                           WHERE AL2.LocomotiveID = l.ID
                             AND pt2.startDate <= p_requested_start_date
                             AND pt2.startDate + 1 / 24 >= p_requested_start_date -- Locomotives/wagons become available 1 hour after train start
                       )

                       UNION ALL

                       -- Locomotives not in Parked_Locomotive but also not assigned at requested time
                       -- These are available but need to use InitialFacilityID from Locomotive table
                       SELECT l.ID,
                              l.VehicleModelID,
                              l.TrainOperatorID,
                              l.InitialFacilityID, -- Use initial facility from Locomotive table
                              ls.make,
                              ls.power,
                              ls.acceleration,
                              ls.operationalSpeed,
                              ls.maxSpeed,
                              ls.numberOfWheels,
                              NULL                                              AS RouteID,
                              NULL                                              AS DestinationFacilityID,
                              NULL                                              AS DestinationFacilityName,
                              parkedFacility2.name                              AS ParkedFacilityName,
                              CALCULATE_DISTANCE_BETWEEN_FACILITIES(v_start_facility_id, l.InitialFacilityID,
                                                                    p_route_id) AS DistanceFromStartKm,
                              0                                                 AS IsInTransitAtRequestedTime
                       FROM Locomotive l
                                INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
                                INNER JOIN Facility parkedFacility2 ON l.InitialFacilityID = parkedFacility2.ID
                                LEFT JOIN Parked_Locomotive pl2 ON l.ID = pl2.LocomotiveID
                       WHERE pl2.LocomotiveID IS NULL -- Not in Parked_Locomotive
                         AND NOT EXISTS (
                           -- Exclude if locomotive is assigned to a planned train that overlaps with requested time
                           SELECT 1
                           FROM Assigned_Locomotive AL3
                                    INNER JOIN Planned_Train pt3 ON AL3.PlannedTrainID = pt3.TrainID
                               AND AL3.PlannedTrainStartDate = pt3.startDate
                           WHERE AL3.LocomotiveID = l.ID
                             AND pt3.startDate <= p_requested_start_date
                             AND pt3.startDate + 1 / 24 >= p_requested_start_date -- Locomotives/wagons become available 1 hour after train start
                       )
                         AND NOT EXISTS (
                           -- Exclude if already shown as in-transit
                           SELECT 1
                           FROM Assigned_Locomotive AL4
                                    INNER JOIN Planned_Train pt4 ON AL4.PlannedTrainID = pt4.TrainID
                               AND AL4.PlannedTrainStartDate = pt4.startDate
                           WHERE AL4.LocomotiveID = l.ID
                             AND pt4.startDate <= p_requested_start_date
                             AND pt4.startDate + 1 / 24 >= p_requested_start_date -- Locomotives/wagons become available 1 hour after train start
                             AND pt4.RouteID != p_route_id)) locos) ranked_locos
        WHERE rn = 1 -- Only show one entry per locomotive
        ORDER BY CASE WHEN RouteID IS NOT NULL THEN 1 ELSE 2 END, -- In-transit first
                 CASE
                     WHEN RouteID IS NULL THEN
                         -- For parked locomotives, order by distance from start facility (furthest first, closest last)
                         -- Use DistanceFromStartKm: NULL or highest values = furthest, 0 = at start facility
                         COALESCE(ranked_locos.DistanceFromStartKm, 999999) -- NULL distances treated as very far
                     END DESC NULLS LAST,                         -- Parked by distance descending (furthest first, closest last)
                 InitialFacilityID; -- Secondary sort by facility ID to ensure consistent ordering when distances are equal

    RETURN v_cursor;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        -- If route not found, return empty cursor
        OPEN v_cursor FOR
            SELECT l.ID,
                   l.VehicleModelID,
                   l.TrainOperatorID,
                   l.InitialFacilityID,
                   ls.make,
                   ls.power,
                   ls.acceleration,
                   ls.operationalSpeed,
                   ls.maxSpeed,
                   ls.numberOfWheels,
                   NULL AS RouteID,
                   NULL AS DestinationFacilityID,
                   NULL AS DestinationFacilityName,
                   NULL AS ParkedFacilityName,
                   NULL AS DistanceFromStartKm
            FROM Locomotive l
                     INNER JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID
            WHERE 1 = 0; -- Return empty result
        RETURN v_cursor;
END;
/

-- Function: Get wagons available for assembly
-- Returns wagons with:
-- - For in-transit: RouteID and EndFacilityID (final destination of the route)
-- - For parked: InitialFacilityID (where the wagon is parked - this is the delivery station where freight was unloaded) and distance in km
-- Results are ordered: in-transit first, then parked by distance from route start (descending)
-- A wagon is "in transit" if it exists in Assigned_Wagon table at the requested time.
-- A wagon is "parked" if it exists in Parked_Wagon table.
-- Only shows wagons that are available (parked) or in-transit at the requested time.
CREATE OR REPLACE FUNCTION GET_WAGONS_FOR_ASSEMBLY(
    p_route_id IN NUMBER,
    p_requested_start_date IN DATE
)
    RETURN SYS_REFCURSOR
AS
    v_cursor            SYS_REFCURSOR;
    v_start_facility_id NUMBER;
BEGIN
    -- Get the start facility for the route
    SELECT StartFacilityID
    INTO v_start_facility_id
    FROM Route
    WHERE ID = p_route_id;

    -- Ensure completed wagons (more than 1 hour since train start) are in Parked_Wagon
    -- We don't delete from Assigned_Wagon to preserve historical assignments for viewing
    -- But we update/insert them to Parked_Wagon so they're available for new assignments
    -- Wagons are parked at the facility where their associated freight is delivered
    -- If a wagon has multiple freight items, use the first destination facility (MIN)
    -- For wagons that completed multiple trains, use the most recent completed train's delivery facility
    MERGE INTO Parked_Wagon PW
    USING (SELECT WagonID, DeliveryFacilityID
           FROM (SELECT AW.WagonID,
                        COALESCE(MIN(f.DestinationFacilityID), r.EndFacilityID)                AS DeliveryFacilityID,
                        ROW_NUMBER() OVER (PARTITION BY AW.WagonID ORDER BY pt.startDate DESC) AS rn
                 FROM Assigned_Wagon AW
                          INNER JOIN Planned_Train pt ON AW.PlannedTrainID = pt.TrainID
                     AND AW.PlannedTrainStartDate = pt.startDate
                          INNER JOIN Route r ON pt.RouteID = r.ID
                          LEFT JOIN Assigned_Freight AF ON AW.WagonID = AF.WagonID
                     AND AW.PlannedTrainID = AF.PlannedTrainID
                     AND AW.PlannedTrainStartDate = AF.PlannedTrainStartDate
                          LEFT JOIN Freight f ON AF.FreightID = f.ID
                 WHERE pt.startDate + 1 / 24 < p_requested_start_date -- More than 1 hour has passed
                 GROUP BY AW.WagonID, pt.startDate, r.EndFacilityID)
           WHERE rn = 1 -- Only the most recent completed train for each wagon
    ) completed_wagons
    ON (PW.WagonID = completed_wagons.WagonID)
    WHEN MATCHED THEN
        UPDATE SET PW.FacilityID = completed_wagons.DeliveryFacilityID
    WHEN NOT MATCHED THEN
        INSERT (WagonID, FacilityID) VALUES (completed_wagons.WagonID, completed_wagons.DeliveryFacilityID);

    OPEN v_cursor FOR
        SELECT ID,
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
               ParkedFacilityName,
               DistanceFromStartKm
        FROM (SELECT wagons.*,
                     ROW_NUMBER() OVER (PARTITION BY wagons.ID ORDER BY
                         CASE
                             WHEN wagons.RouteID IS NOT NULL AND wagons.IsInTransitAtRequestedTime = 1 THEN 1
                             ELSE 2 END, -- In-transit at requested time first
                         wagons.RouteID NULLS LAST -- If multiple in-transit, pick one arbitrarily
                         ) AS rn
              FROM (
                       -- In-transit wagons at the requested time (assigned to a planned train that overlaps with requested time)
                       SELECT w.ID,
                              w.VehicleModelID,
                              w.TrainOperatorID,
                              w.InitialFacilityID,
                              ws.WagonTypeID,
                              ws.volumeCapacity,
                              ws.payload,
                              vm.tare,
                              pt.RouteID        AS RouteID,
                              r.EndFacilityID   AS DestinationFacilityID,
                              destFacility.name AS DestinationFacilityName,
                              NULL              AS ParkedFacilityName,
                              NULL              AS DistanceFromStartKm,
                              1                 AS IsInTransitAtRequestedTime
                       FROM Wagon w
                                INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                                INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
                                INNER JOIN Assigned_Wagon AW ON w.ID = AW.WagonID
                                INNER JOIN Planned_Train pt ON AW.PlannedTrainID = pt.TrainID
                           AND AW.PlannedTrainStartDate = pt.startDate
                                INNER JOIN Route r ON pt.RouteID = r.ID
                                INNER JOIN Facility destFacility ON r.EndFacilityID = destFacility.ID
                       WHERE pt.startDate <= p_requested_start_date          -- Show as in-transit if assigned to train starting on or before requested time
                         AND pt.startDate + 1 / 24 >= p_requested_start_date -- Locomotives/wagons become available 1 hour after train start

                       UNION ALL

                       -- Parked wagons (in Parked_Wagon table) - available for selection
                       SELECT w.ID,
                              w.VehicleModelID,
                              w.TrainOperatorID,
                              pw.FacilityID                                     AS InitialFacilityID, -- Use parking facility from Parked_Wagon
                              ws.WagonTypeID,
                              ws.volumeCapacity,
                              ws.payload,
                              vm.tare,
                              NULL                                              AS RouteID,
                              NULL                                              AS DestinationFacilityID,
                              NULL                                              AS DestinationFacilityName,
                              parkedFacility.name                               AS ParkedFacilityName,
                              CALCULATE_DISTANCE_BETWEEN_FACILITIES(v_start_facility_id, pw.FacilityID,
                                                                    p_route_id) AS DistanceFromStartKm,
                              0                                                 AS IsInTransitAtRequestedTime
                       FROM Wagon w
                                INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                                INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
                                INNER JOIN Parked_Wagon pw ON w.ID = pw.WagonID
                                INNER JOIN Facility parkedFacility ON pw.FacilityID = parkedFacility.ID
                       WHERE NOT EXISTS (
                           -- Exclude if wagon is assigned to a planned train at the requested time
                           SELECT 1
                           FROM Assigned_Wagon AW2
                                    INNER JOIN Planned_Train pt2 ON AW2.PlannedTrainID = pt2.TrainID
                               AND AW2.PlannedTrainStartDate = pt2.startDate
                           WHERE AW2.WagonID = w.ID
                             AND pt2.startDate <= p_requested_start_date
                             AND pt2.startDate + 1 / 24 >= p_requested_start_date -- Locomotives/wagons become available 1 hour after train start
                       )

                       UNION ALL

                       -- Wagons not in Parked_Wagon but also not assigned at requested time
                       -- These are available but need to use InitialFacilityID from Wagon table
                       SELECT w.ID,
                              w.VehicleModelID,
                              w.TrainOperatorID,
                              w.InitialFacilityID, -- Use initial facility from Wagon table
                              ws.WagonTypeID,
                              ws.volumeCapacity,
                              ws.payload,
                              vm.tare,
                              NULL                                              AS RouteID,
                              NULL                                              AS DestinationFacilityID,
                              NULL                                              AS DestinationFacilityName,
                              parkedFacility2.name                              AS ParkedFacilityName,
                              CALCULATE_DISTANCE_BETWEEN_FACILITIES(v_start_facility_id, w.InitialFacilityID,
                                                                    p_route_id) AS DistanceFromStartKm,
                              0                                                 AS IsInTransitAtRequestedTime
                       FROM Wagon w
                                INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                                INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
                                INNER JOIN Facility parkedFacility2 ON w.InitialFacilityID = parkedFacility2.ID
                                LEFT JOIN Parked_Wagon pw2 ON w.ID = pw2.WagonID
                       WHERE pw2.WagonID IS NULL -- Not in Parked_Wagon
                         AND NOT EXISTS (
                           -- Exclude if wagon is assigned to a planned train at the requested time
                           SELECT 1
                           FROM Assigned_Wagon AW3
                                    INNER JOIN Planned_Train pt3 ON AW3.PlannedTrainID = pt3.TrainID
                               AND AW3.PlannedTrainStartDate = pt3.startDate
                           WHERE AW3.WagonID = w.ID
                             AND pt3.startDate <= p_requested_start_date
                             AND pt3.startDate + 25 / 24 >= p_requested_start_date -- Conservative: within 25 hours window
                       )
                         AND NOT EXISTS (
                           -- Exclude if already shown as in-transit
                           SELECT 1
                           FROM Assigned_Wagon AW4
                                    INNER JOIN Planned_Train pt4 ON AW4.PlannedTrainID = pt4.TrainID
                               AND AW4.PlannedTrainStartDate = pt4.startDate
                           WHERE AW4.WagonID = w.ID
                             AND pt4.startDate <= p_requested_start_date
                             AND pt4.startDate + 25 / 24 >= p_requested_start_date -- Conservative: within 25 hours window
                             AND pt4.RouteID != p_route_id)) wagons) ranked_wagons
        WHERE rn = 1 -- Only show one entry per wagon
        ORDER BY CASE WHEN RouteID IS NOT NULL THEN 1 ELSE 2 END, -- In-transit first
                 CASE
                     WHEN RouteID IS NULL THEN
                         -- For parked wagons, order by distance from start facility (furthest first, closest last)
                         -- Use DistanceFromStartKm: NULL or highest values = furthest, 0 = at start facility
                         COALESCE(ranked_wagons.DistanceFromStartKm, 999999) -- NULL distances treated as very far
                     END DESC NULLS LAST,                         -- Parked by distance descending (furthest first, closest last)
                 InitialFacilityID; -- Secondary sort by facility ID to ensure consistent ordering when distances are equal

    RETURN v_cursor;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        -- If route not found, return empty cursor
        OPEN v_cursor FOR
            SELECT w.ID,
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
                   NULL AS ParkedFacilityName,
                   NULL AS DistanceFromStartKm
            FROM Wagon w
                     INNER JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID
                     INNER JOIN VehicleModel vm ON w.VehicleModelID = vm.ID
            WHERE 1 = 0; -- Return empty result
        RETURN v_cursor;
END;
/

-- Function: Check if wagon is loaded (has freight assigned where freight origin matches route start)
-- A wagon is loaded if it has freight assigned to the train where the freight's origin facility
-- matches the route's start facility
CREATE OR REPLACE FUNCTION IS_WAGON_LOADED(
    p_wagon_id IN NUMBER,
    p_train_id IN NUMBER,
    p_start_date IN DATE
)
    RETURN NUMBER
AS
    v_count NUMBER;
BEGIN
    -- Check if wagon has freight assigned to this train where freight origin matches route start
    -- We need to find the route for this planned train and check if the freight origin matches the route start
    SELECT COUNT(*)
    INTO v_count
    FROM Assigned_Freight AF
             INNER JOIN Freight F ON AF.FreightID = F.ID
             INNER JOIN Planned_Train PT ON AF.PlannedTrainID = PT.TrainID
        AND AF.PlannedTrainStartDate = PT.startDate
             INNER JOIN Route R ON PT.RouteID = R.ID
    WHERE AF.WagonID = p_wagon_id
      AND AF.PlannedTrainID = p_train_id
      AND AF.PlannedTrainStartDate = p_start_date
      AND F.OriginFacilityID = R.StartFacilityID;

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
        SELECT l.ID,
               l.VehicleModelID,
               l.TrainOperatorID,
               l.InitialFacilityID,
               ls.make,
               ls.power,
               ls.acceleration,
               ls.operationalSpeed,
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
        SELECT l.ID,
               l.VehicleModelID,
               l.TrainOperatorID,
               l.InitialFacilityID,
               ls.make,
               ls.power,
               ls.acceleration,
               ls.operationalSpeed,
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
        SELECT w.ID,
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
        SELECT w.ID,
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
    SELECT TrainID
    INTO v_train_id
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
        SELECT l.ID,
               l.VehicleModelID,
               l.TrainOperatorID,
               l.InitialFacilityID,
               ls.make,
               ls.power,
               ls.acceleration,
               ls.operationalSpeed,
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
        SELECT w.ID,
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

-- Function: Create or ensure Planned_Train exists for a route with a specific date/time
-- Creates Planned_Train entry if it doesn't exist, otherwise ensures it matches
-- Generates a unique TrainID for each new planned train
-- Returns the TrainID for the planned train
CREATE OR REPLACE FUNCTION ENSURE_PLANNED_TRAIN_FOR_ROUTE(
    p_route_id IN NUMBER,
    p_start_date IN DATE
)
    RETURN NUMBER
    IS
    v_train_id          NUMBER;
    v_new_train_id      NUMBER;
    v_train_operator_id NUMBER;
BEGIN
    -- Check if Planned_Train already exists for this exact route and date combination
    BEGIN
        SELECT TrainID
        INTO v_train_id
        FROM Planned_Train
        WHERE RouteID = p_route_id
          AND startDate = p_start_date;

        -- Planned train exists with matching route and date, return train ID
        RETURN v_train_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            -- Planned train doesn't exist for this route and date combination
            -- Generate a unique TrainID by finding the maximum TrainID from both Train and Planned_Train tables and adding 1
            BEGIN
                SELECT GREATEST(
                               COALESCE((SELECT MAX(ID) FROM Train), 0),
                               COALESCE((SELECT MAX(TrainID) FROM Planned_Train), 0)
                       ) + 1
                INTO v_new_train_id
                FROM DUAL;
            EXCEPTION
                WHEN OTHERS THEN
                    -- If no records exist, start from 1
                    v_new_train_id := 1;
            END;

            -- Ensure Train exists with this ID (create if doesn't exist)
            -- Get a default TrainOperatorID (use 1 if exists, otherwise we'll need to handle this)
            BEGIN
                SELECT MIN(ID) INTO v_train_operator_id FROM TrainOperator WHERE ROWNUM = 1;
            EXCEPTION
                WHEN NO_DATA_FOUND THEN
                    v_train_operator_id := 1; -- Fallback - should exist in database
            END;

            -- Insert Train if it doesn't exist (with maxLength = 250 as default, same as other trains)
            BEGIN
                INSERT INTO Train (ID, TrainOperatorID, maxLength)
                VALUES (v_new_train_id, v_train_operator_id, 250);
            EXCEPTION
                WHEN DUP_VAL_ON_INDEX THEN
                    -- Train already exists, that's fine
                    NULL;
            END;

            -- Create new Planned_Train entry with unique TrainID
            INSERT INTO Planned_Train (TrainID, startDate, RouteID)
            VALUES (v_new_train_id, p_start_date, p_route_id);
            RETURN v_new_train_id;
    END;
END;
/

-- Function: Get all planned trains with route information
CREATE OR REPLACE FUNCTION GET_ALL_PLANNED_TRAINS
    RETURN SYS_REFCURSOR
AS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT pt.RouteID,
               pt.TrainID,
               pt.startDate,
               r.StartFacilityID,
               startFacility.name AS StartFacilityName,
               r.EndFacilityID,
               endFacility.name   AS EndFacilityName
        FROM Planned_Train pt
                 INNER JOIN Route r ON pt.RouteID = r.ID
                 INNER JOIN Facility startFacility ON r.StartFacilityID = startFacility.ID
                 INNER JOIN Facility endFacility ON r.EndFacilityID = endFacility.ID
        ORDER BY pt.RouteID, pt.startDate;

    RETURN v_cursor;
END;
/

-- Function: Delete a planned train for a route
-- Moves all assigned locomotives and wagons back to parked status at route start facility
-- Returns 1 if successful, 0 if planned train not found
CREATE OR REPLACE FUNCTION DELETE_PLANNED_TRAIN(
    p_route_id IN NUMBER,
    p_start_date IN DATE
)
    RETURN NUMBER
    IS
    v_train_id                NUMBER;
    v_route_start_facility_id NUMBER;
BEGIN
    -- Get the train ID and route start facility
    BEGIN
        SELECT pt.TrainID, r.StartFacilityID
        INTO v_train_id, v_route_start_facility_id
        FROM Planned_Train pt
                 INNER JOIN Route r ON pt.RouteID = r.ID
        WHERE pt.RouteID = p_route_id
          AND pt.startDate = p_start_date;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN 0; -- Planned train not found
    END;

    -- Move assigned locomotives back to parked at route start facility
    FOR loco_rec IN (
        SELECT AL.LocomotiveID
        FROM Assigned_Locomotive AL
        WHERE AL.PlannedTrainID = v_train_id
          AND AL.PlannedTrainStartDate = p_start_date
        )
        LOOP
            -- Delete from Assigned_Locomotive
            DELETE
            FROM Assigned_Locomotive
            WHERE LocomotiveID = loco_rec.LocomotiveID
              AND PlannedTrainID = v_train_id
              AND PlannedTrainStartDate = p_start_date;

            -- Insert or update Parked_Locomotive at route start facility
            BEGIN
                INSERT INTO Parked_Locomotive (LocomotiveID, FacilityID)
                VALUES (loco_rec.LocomotiveID, v_route_start_facility_id);
            EXCEPTION
                WHEN DUP_VAL_ON_INDEX THEN
                    -- Already parked, update facility
                    UPDATE Parked_Locomotive
                    SET FacilityID = v_route_start_facility_id
                    WHERE LocomotiveID = loco_rec.LocomotiveID;
            END;
        END LOOP;

    -- Move assigned wagons back to parked at route start facility
    FOR wagon_rec IN (
        SELECT AW.WagonID
        FROM Assigned_Wagon AW
        WHERE AW.PlannedTrainID = v_train_id
          AND AW.PlannedTrainStartDate = p_start_date
        )
        LOOP
            -- Delete from Assigned_Wagon
            DELETE
            FROM Assigned_Wagon
            WHERE WagonID = wagon_rec.WagonID
              AND PlannedTrainID = v_train_id
              AND PlannedTrainStartDate = p_start_date;

            -- Insert or update Parked_Wagon at route start facility
            BEGIN
                INSERT INTO Parked_Wagon (WagonID, FacilityID)
                VALUES (wagon_rec.WagonID, v_route_start_facility_id);
            EXCEPTION
                WHEN DUP_VAL_ON_INDEX THEN
                    -- Already parked, update facility
                    UPDATE Parked_Wagon
                    SET FacilityID = v_route_start_facility_id
                    WHERE WagonID = wagon_rec.WagonID;
            END;
        END LOOP;

    -- Delete the Planned_Train entry
    DELETE
    FROM Planned_Train
    WHERE RouteID = p_route_id
      AND startDate = p_start_date;

    RETURN 1;
END;
/

-- Function: Assign a wagon to a route (planned train)
-- Removes wagon from Parked_Wagon when assigning it to ensure data integrity
CREATE OR REPLACE FUNCTION ASSIGN_WAGON_TO_ROUTE(
    p_wagon_id IN NUMBER,
    p_route_id IN NUMBER,
    p_start_date IN DATE
) RETURN NUMBER
    IS
    v_train_id               NUMBER;
    v_start_date             DATE;
    v_wagon_exists           NUMBER;
    v_already_associated     NUMBER;
    v_conflicting_train_id   NUMBER;
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

    -- 3. Validate that the planned train exists for this route and start date
    v_start_date := p_start_date;
    BEGIN
        SELECT TrainID
        INTO v_train_id
        FROM Planned_Train
        WHERE RouteID = p_route_id
          AND startDate = p_start_date;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'Planned train with RouteID ' || p_route_id || ' and start date ' ||
                                            TO_CHAR(p_start_date, 'YYYY-MM-DD HH24:MI:SS') || ' does not exist.');
    END;

    -- 4. Validate that the wagon exists
    SELECT COUNT(*)
    INTO v_wagon_exists
    FROM Wagon
    WHERE ID = p_wagon_id;

    IF v_wagon_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Wagon with ID ' || p_wagon_id || ' does not exist.');
    END IF;

    -- 5. Check if wagon is already associated (in Assigned_Wagon)
    SELECT COUNT(*)
    INTO v_already_associated
    FROM Assigned_Wagon
    WHERE WagonID = p_wagon_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    IF v_already_associated > 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Wagon ' || p_wagon_id || ' is already associated with train ' || v_train_id ||
                                        ' for the planned trip starting at ' ||
                                        TO_CHAR(v_start_date, 'YYYY-MM-DD HH24:MI:SS') || '.');
    END IF;

    -- 6. Check train length constraint: ensure adding this wagon doesn't exceed train maxLength
    DECLARE
        v_train_max_length       NUMBER;
        v_current_total_length   NUMBER;
        v_wagon_length           NUMBER;
        v_projected_total_length NUMBER;
        v_wagons_length          NUMBER := 0;
        v_locomotives_length     NUMBER := 0;
    BEGIN
        -- Get train's maximum length
        SELECT maxLength
        INTO v_train_max_length
        FROM Train
        WHERE ID = v_train_id;

        -- Calculate total length of wagons already assigned
        SELECT NVL(SUM(VM.length), 0)
        INTO v_wagons_length
        FROM Assigned_Wagon AW
                 JOIN Wagon W ON AW.WagonID = W.ID
                 JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE AW.PlannedTrainID = v_train_id
          AND AW.PlannedTrainStartDate = v_start_date;

        -- Calculate total length of locomotives already assigned
        SELECT NVL(SUM(VM.length), 0)
        INTO v_locomotives_length
        FROM Assigned_Locomotive AL
                 JOIN Locomotive L ON AL.LocomotiveID = L.ID
                 JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE AL.PlannedTrainID = v_train_id
          AND AL.PlannedTrainStartDate = v_start_date;

        -- Total length = wagons + locomotives
        v_current_total_length := v_wagons_length + v_locomotives_length;

        -- Get the length of the wagon being added
        SELECT VM.length
        INTO v_wagon_length
        FROM Wagon W
                 JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE W.ID = p_wagon_id;

        -- Calculate projected total length
        v_projected_total_length := v_current_total_length + v_wagon_length;

        -- Check if adding this wagon would exceed the train's maximum length
        IF v_projected_total_length > v_train_max_length THEN
            RAISE_APPLICATION_ERROR(-20007,
                                    'Cannot add wagon to train: Adding wagon ' || p_wagon_id ||
                                    ' would exceed train ' || v_train_id || ' maximum length. ' ||
                                    'Current length: ' || v_current_total_length || 'm, ' ||
                                    'Wagon length: ' || v_wagon_length || 'm, ' ||
                                    'Projected total: ' || v_projected_total_length || 'm, ' ||
                                    'Maximum allowed: ' || v_train_max_length || 'm.');
        END IF;
    END;

    -- 7. Check if wagon is already assigned to a train with overlapping time
    -- A wagon is in-transit for 1 hour after train start, so check for time overlap
    -- The composite key allows multiple assignments, so we only need to check for time conflicts
    BEGIN
        SELECT AW.PlannedTrainID, AW.PlannedTrainStartDate
        INTO v_conflicting_train_id, v_conflicting_start_date
        FROM Assigned_Wagon AW
                 JOIN Planned_Train PT ON AW.PlannedTrainID = PT.TrainID AND AW.PlannedTrainStartDate = PT.startDate
        WHERE AW.WagonID = p_wagon_id
          -- Check if the requested time overlaps with existing assignment
          -- Existing train: starts at PT.startDate, in-transit until PT.startDate + 1/24
          -- Requested train: starts at p_start_date, in-transit until p_start_date + 1/24
          -- Overlap if: (PT.startDate <= p_start_date + 1/24) AND (p_start_date <= PT.startDate + 1/24)
          AND PT.startDate <= p_start_date + 1 / 24
          AND p_start_date <= PT.startDate + 1 / 24;

        RAISE_APPLICATION_ERROR(-20006,
                                'Wagon ' || p_wagon_id || ' is already assigned to train ' || v_conflicting_train_id ||
                                ' with a planned route starting at ' ||
                                TO_CHAR(v_conflicting_start_date, 'YYYY-MM-DD HH24:MI:SS') ||
                                ' which overlaps with the requested start time.');
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            NULL;
    END;

    -- 8. CRITICAL: Remove wagon from Parked_Wagon BEFORE assigning it
    -- This prevents wagons from appearing both parked and in transit
    DELETE FROM Parked_Wagon WHERE WagonID = p_wagon_id;

    -- 9. Insert the association into Assigned_Wagon
    INSERT INTO Assigned_Wagon (WagonID, PlannedTrainID, PlannedTrainStartDate)
    VALUES (p_wagon_id, v_train_id, v_start_date);

    RETURN v_train_id;
END ASSIGN_WAGON_TO_ROUTE;
/

-- Function: Assign a locomotive to a route (planned train)
-- Removes locomotive from Parked_Locomotive when assigning it to ensure data integrity
CREATE OR REPLACE FUNCTION ASSIGN_LOCOMOTIVE_TO_ROUTE(
    p_locomotive_id IN NUMBER,
    p_route_id IN NUMBER,
    p_start_date IN DATE
) RETURN NUMBER
    IS
    v_train_id               NUMBER;
    v_start_date             DATE;
    v_locomotive_exists      NUMBER;
    v_already_associated     NUMBER;
    v_conflicting_train_id   NUMBER;
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

    -- 3. Validate that the planned train exists for this route and start date
    v_start_date := p_start_date;
    BEGIN
        SELECT TrainID
        INTO v_train_id
        FROM Planned_Train
        WHERE RouteID = p_route_id
          AND startDate = p_start_date;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'Planned train with RouteID ' || p_route_id || ' and start date ' ||
                                            TO_CHAR(p_start_date, 'YYYY-MM-DD HH24:MI:SS') || ' does not exist.');
    END;

    -- 4. Validate that the locomotive exists
    SELECT COUNT(*)
    INTO v_locomotive_exists
    FROM Locomotive
    WHERE ID = p_locomotive_id;

    IF v_locomotive_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Locomotive with ID ' || p_locomotive_id || ' does not exist.');
    END IF;

    -- 5. Check if locomotive is already associated (in Assigned_Locomotive)
    SELECT COUNT(*)
    INTO v_already_associated
    FROM Assigned_Locomotive
    WHERE LocomotiveID = p_locomotive_id
      AND PlannedTrainID = v_train_id
      AND PlannedTrainStartDate = v_start_date;

    IF v_already_associated > 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Locomotive ' || p_locomotive_id || ' is already associated with train ' ||
                                        v_train_id || ' for the planned trip starting at ' ||
                                        TO_CHAR(v_start_date, 'YYYY-MM-DD HH24:MI:SS') || '.');
    END IF;

    -- 6. Check train length constraint: ensure adding this locomotive doesn't exceed train maxLength
    DECLARE
        v_train_max_length       NUMBER;
        v_current_total_length   NUMBER;
        v_locomotive_length      NUMBER;
        v_projected_total_length NUMBER;
        v_wagons_length          NUMBER := 0;
        v_locomotives_length     NUMBER := 0;
    BEGIN
        -- Get train's maximum length
        SELECT maxLength
        INTO v_train_max_length
        FROM Train
        WHERE ID = v_train_id;

        -- Calculate total length of wagons already assigned
        SELECT NVL(SUM(VM.length), 0)
        INTO v_wagons_length
        FROM Assigned_Wagon AW
                 JOIN Wagon W ON AW.WagonID = W.ID
                 JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE AW.PlannedTrainID = v_train_id
          AND AW.PlannedTrainStartDate = v_start_date;

        -- Calculate total length of locomotives already assigned
        SELECT NVL(SUM(VM.length), 0)
        INTO v_locomotives_length
        FROM Assigned_Locomotive AL
                 JOIN Locomotive L ON AL.LocomotiveID = L.ID
                 JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE AL.PlannedTrainID = v_train_id
          AND AL.PlannedTrainStartDate = v_start_date;

        -- Total length = wagons + locomotives
        v_current_total_length := v_wagons_length + v_locomotives_length;

        -- Get the length of the locomotive being added
        SELECT VM.length
        INTO v_locomotive_length
        FROM Locomotive L
                 JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE L.ID = p_locomotive_id;

        -- Calculate projected total length
        v_projected_total_length := v_current_total_length + v_locomotive_length;

        -- Check if adding this locomotive would exceed the train's maximum length
        IF v_projected_total_length > v_train_max_length THEN
            RAISE_APPLICATION_ERROR(-20007,
                                    'Cannot add locomotive to train: Adding locomotive ' || p_locomotive_id ||
                                    ' would exceed train ' || v_train_id || ' maximum length. ' ||
                                    'Current length: ' || v_current_total_length || 'm, ' ||
                                    'Locomotive length: ' || v_locomotive_length || 'm, ' ||
                                    'Projected total: ' || v_projected_total_length || 'm, ' ||
                                    'Maximum allowed: ' || v_train_max_length || 'm.');
        END IF;
    END;

    -- 7. Check if locomotive is already assigned to a train with overlapping time
    -- A locomotive is in-transit for 1 hour after train start, so check for time overlap
    -- The composite key allows multiple assignments, so we only need to check for time conflicts
    BEGIN
        SELECT AL.PlannedTrainID, AL.PlannedTrainStartDate
        INTO v_conflicting_train_id, v_conflicting_start_date
        FROM Assigned_Locomotive AL
                 JOIN Planned_Train PT ON AL.PlannedTrainID = PT.TrainID AND AL.PlannedTrainStartDate = PT.startDate
        WHERE AL.LocomotiveID = p_locomotive_id
          -- Check if the requested time overlaps with existing assignment
          -- Existing train: starts at PT.startDate, in-transit until PT.startDate + 1/24
          -- Requested train: starts at p_start_date, in-transit until p_start_date + 1/24
          -- Overlap if: (PT.startDate <= p_start_date + 1/24) AND (p_start_date <= PT.startDate + 1/24)
          AND PT.startDate <= p_start_date + 1 / 24
          AND p_start_date <= PT.startDate + 1 / 24;

        RAISE_APPLICATION_ERROR(-20006,
                                'Locomotive ' || p_locomotive_id || ' is already assigned to train ' ||
                                v_conflicting_train_id ||
                                ' with a planned route starting at ' ||
                                TO_CHAR(v_conflicting_start_date, 'YYYY-MM-DD HH24:MI:SS') ||
                                ' which overlaps with the requested start time.');
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            NULL;
    END;

    -- 8. CRITICAL: Remove locomotive from Parked_Locomotive BEFORE assigning it
    DELETE FROM Parked_Locomotive WHERE LocomotiveID = p_locomotive_id;

    -- 9. Insert the association into Assigned_Locomotive
    INSERT INTO Assigned_Locomotive (LocomotiveID, PlannedTrainID, PlannedTrainStartDate)
    VALUES (p_locomotive_id, v_train_id, v_start_date);

    RETURN v_train_id;
END ASSIGN_LOCOMOTIVE_TO_ROUTE;
/
