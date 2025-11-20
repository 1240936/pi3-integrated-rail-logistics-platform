DECLARE
    CURSOR c_segments IS
        WITH pairs AS (
            SELECT r.ID AS RouteID,
                   0 AS seqNum,
                   r.startFacilityID AS startF,
                   p.FacilityID AS endF
            FROM Route r
                     JOIN Path p ON p.RouteID = r.ID
            WHERE r.ID = :route_id
              AND p.seqNumber = (SELECT MIN(seqNumber) FROM Path WHERE RouteID = r.ID)

            UNION ALL

            SELECT p1.RouteID,
                   p1.seqNumber AS seqNum,
                   p1.FacilityID AS startF,
                   p2.FacilityID AS endF
            FROM Path p1
                     JOIN Path p2
                          ON p2.RouteID = p1.RouteID
                              AND p2.seqNumber = p1.seqNumber + 1
            WHERE p1.RouteID = :route_id

            UNION ALL

            SELECT r.ID AS RouteID,
                   (SELECT MAX(seqNumber) FROM Path WHERE RouteID = r.ID) + 1 AS seqNum,
                   p.FacilityID AS startF,
                   r.endFacilityID AS endF
            FROM Route r
                     JOIN Path p ON p.RouteID = r.ID
            WHERE r.ID = :route_id
              AND p.seqNumber = (SELECT MAX(seqNumber) FROM Path WHERE RouteID = r.ID)
        )
        SELECT
            ls.ID            AS lineSegmentID,
            ls.RailLineID    AS railLineID,
            rl.StartFacilityID,
            rl.EndFacilityID,
            ls.orderNum,
            ls.length,
            ls.maxWeight,
            ls.numberOfTracks,
            ls.speedLimit
        FROM pairs
                 JOIN RailLine rl
                      ON ( (rl.StartFacilityID = pairs.startF AND rl.EndFacilityID = pairs.endF)
                          OR (rl.StartFacilityID = pairs.endF AND rl.EndFacilityID = pairs.startF) )
                 JOIN LineSegment ls
                      ON ls.RailLineID = rl.ID
        ORDER BY pairs.seqNum, ls.orderNum;

    v_count INTEGER := 0;
BEGIN
    FOR rec IN c_segments LOOP
            v_count := v_count + 1;
            DBMS_OUTPUT.PUT_LINE(
                    'Segment ' || rec.lineSegmentID ||
                    ' | RailLine ' || rec.railLineID ||
                    ' | Facilities ' || rec.StartFacilityID || ' → ' || rec.EndFacilityID ||
                    ' | OrderNumber ' || rec.orderNum ||
                    ' | Length ' || rec.length ||
                    ' | MaxWeight ' || rec.maxWeight ||
                    ' | NumberOfTracks ' || rec.numberOfTracks ||
                    ' | SpeedLimit ' || rec.speedLimit
            );
        END LOOP;

    IF v_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('There are no segments for this route. Please select a valid route ID.');
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Error: No route data found.');
    WHEN TOO_MANY_ROWS THEN
        DBMS_OUTPUT.PUT_LINE('Error: More than expected.');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Unexpected error: ' || SQLERRM);
END;
