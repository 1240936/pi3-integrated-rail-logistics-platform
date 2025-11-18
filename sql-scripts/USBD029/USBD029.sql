DECLARE
    v_percentage NUMBER;
BEGIN
    SELECT 
        ROUND(
            (electric.total * 100.0) / all_loco.total, 
            2
        )
    INTO v_percentage
    FROM (
        SELECT COUNT(*) AS total
        FROM Locomotive l
        JOIN ElectricalType e ON e.VehicleModelID = l.VehicleModelID
        WHERE l.TrainOperatorID = :operator_id
    ) electric,
    (
        SELECT COUNT(*) AS total
        FROM Locomotive l
        WHERE l.TrainOperatorID = :operator_id
    ) all_loco;

    DBMS_OUTPUT.PUT_LINE('Percentagem de locomotivas elétricas = ' || v_percentage || '%');
END;