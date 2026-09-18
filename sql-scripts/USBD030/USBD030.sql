DECLARE
    CURSOR c_facilities IS
        SELECT f.ID, f.name
        FROM Facility f
        WHERE f.ID IN (SELECT gs.FacilityID
                       FROM GrainSilo gs
                       MINUS
                       SELECT w.FacilityID
                       FROM Warehouse w)
        ORDER BY f.ID;
    v_count INTEGER := 0;
BEGIN
    FOR rec IN c_facilities
        LOOP
            v_count := v_count + 1;
            DBMS_OUTPUT.PUT_LINE(
                    'Facility ' || rec.ID ||
                    ' | Name: ' || rec.name
            );
        END LOOP;

    IF v_count = 0 THEN
        DBMS_OUTPUT.PUT_LINE('There are no facilities with grain silos, but no warehouses.');
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Error: No facility data found.');
    WHEN TOO_MANY_ROWS THEN
        DBMS_OUTPUT.PUT_LINE('Error: More rows than expected.');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Unexpected error: ' || SQLERRM);
END;