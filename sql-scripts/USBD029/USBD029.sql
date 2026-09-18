DECLARE
    v_percentage NUMBER;
BEGIN
    SELECT CASE
               WHEN all_loco.total = 0 THEN NULL
               ELSE ROUND((electric.total * 100.0) / all_loco.total, 2)
               END
    INTO v_percentage
    FROM (SELECT COUNT(*) AS total
          FROM Locomotive l
                   JOIN ElectricalType e
                        ON e.VehicleModelID = l.VehicleModelID
          WHERE l.TrainOperatorID = :operator_id) electric,
         (SELECT COUNT(*) AS total
          FROM Locomotive l
          WHERE l.TrainOperatorID = :operator_id) all_loco;

    IF v_percentage IS NULL THEN
        DBMS_OUTPUT.PUT_LINE('This operator has no locomotives. Please select a valid operator ID.');
    ELSE
        DBMS_OUTPUT.PUT_LINE(
                'Electric locomotive percentage for the given operator = '
                    || v_percentage || '%'
        );
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Error: No operator data found.');
    WHEN TOO_MANY_ROWS THEN
        DBMS_OUTPUT.PUT_LINE('Error: More rows than expected.');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Unexpected error: ' || SQLERRM);
END;
