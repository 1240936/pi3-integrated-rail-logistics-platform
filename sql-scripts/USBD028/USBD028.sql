DECLARE
    CURSOR c_multiGaugeLocos IS
        SELECT l.ID             AS locomotiveID,
               l.VehicleModelID AS vehicleModelID,
               vm.modelName     AS modelName
        FROM Locomotive l
                 JOIN VehicleModel vm
                      ON vm.ID = l.VehicleModelID
        WHERE l.TrainOperatorID = :operator_id
          AND l.VehicleModelID IN (SELECT vg.VehicleModelID
                                   FROM VehicleModel_Gauge vg
                                   GROUP BY vg.VehicleModelID
                                   HAVING COUNT(DISTINCT vg.GaugeID) > 1)
        ORDER BY vm.modelName, l.ID;
    v_count INTEGER := 0;
BEGIN
    FOR rec IN c_multiGaugeLocos
        LOOP
            v_count := v_count + 1;
            DBMS_OUTPUT.PUT_LINE(
                    'Locomotive ' || rec.locomotiveID ||
                    ' | Model ' || rec.vehicleModelID ||
                    ' (' || rec.modelName || ')'
            );
        END LOOP;

    IF v_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('This operator does not have locomotives with multiple gauges.');
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Error: No operator data found.');
    WHEN TOO_MANY_ROWS THEN
        DBMS_OUTPUT.PUT_LINE('Error: More rows than expected.');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Unexpected error: ' || SQLERRM);
END;

