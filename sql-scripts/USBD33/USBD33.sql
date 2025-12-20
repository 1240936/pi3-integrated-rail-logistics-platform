CREATE OR REPLACE TRIGGER TRG_VALIDATE_TRAIN_LENGTH_WAGON
BEFORE INSERT OR UPDATE ON Planned_Train_Wagon
FOR EACH ROW
DECLARE
    v_train_max_length NUMBER;
    v_total_wagons_length NUMBER := 0;
    v_total_locomotives_length NUMBER := 0;
    v_current_total_length NUMBER;
BEGIN
    -- Get the train's maximum length
    BEGIN
        SELECT maxLength
        INTO v_train_max_length
        FROM Train
        WHERE ID = :NEW.Planned_TrainTrainID;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20002, 'Train ' || :NEW.Planned_TrainTrainID || ' not found');
    END;
    
    -- Calculate total length of wagons already assigned to this planned trip (TrainID + startDate)
    -- For INSERT: new row not in table yet, so we'll add it manually
    IF INSERTING THEN
        -- Get existing wagons for this planned trip (new row not in table yet)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_total_wagons_length
        FROM Planned_Train_Wagon PTW
        JOIN Wagon W ON PTW.WagonID = W.ID
        JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE PTW.Planned_TrainTrainID = :NEW.Planned_TrainTrainID AND PTW.Planned_TrainstartDate = :NEW.Planned_TrainstartDate;
        
        -- Add the new wagon's length
        SELECT v_total_wagons_length + VM.length
        INTO v_total_wagons_length
        FROM Wagon W
        JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE W.ID = :NEW.WagonID;
    ELSE
        -- UPDATE: get all wagons for this planned trip (including current row)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_total_wagons_length
        FROM Planned_Train_Wagon PTW
        JOIN Wagon W ON PTW.WagonID = W.ID
        JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE PTW.Planned_TrainTrainID = :NEW.Planned_TrainTrainID AND PTW.Planned_TrainstartDate = :NEW.Planned_TrainstartDate;
    END IF;
    
    -- Calculate total length of locomotives assigned to this planned trip (TrainID + startDate)
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_locomotives_length
    FROM Planned_Train_Locomotive PTL
    JOIN Locomotive L ON PTL.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE PTL.Planned_TrainTrainID = :NEW.Planned_TrainTrainID AND PTL.Planned_TrainstartDate = :NEW.Planned_TrainstartDate;
    
    -- Total length = all wagons + all locomotives for this planned trip (both in meters)
    v_current_total_length := v_total_wagons_length + v_total_locomotives_length;
    
    -- Check if total length exceeds the train's defined limit
    IF v_current_total_length > v_train_max_length THEN
        RAISE_APPLICATION_ERROR(-20001, 
            'Cannot add wagon to train: Total length (' || 
            TO_CHAR(v_current_total_length) || 
            ') exceeds train maximum length (' || 
            TO_CHAR(v_train_max_length) || ') for Train ID ' || :NEW.Planned_TrainTrainID);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_VALIDATE_TRAIN_LENGTH_LOCOMOTIVE
BEFORE INSERT OR UPDATE ON Planned_Train_Locomotive
FOR EACH ROW
DECLARE
    v_train_max_length NUMBER;
    v_total_locomotives_length NUMBER := 0;
    v_total_wagons_length NUMBER := 0;
    v_current_total_length NUMBER;
BEGIN
    -- Get the train's maximum length
    BEGIN
        SELECT maxLength
        INTO v_train_max_length
        FROM Train
        WHERE ID = :NEW.Planned_TrainTrainID;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20004, 'Train ' || :NEW.Planned_TrainTrainID || ' not found');
    END;
    
    -- Calculate total length of locomotives already assigned to this planned trip (TrainID + startDate)
    IF INSERTING THEN
        -- Get existing locomotives for this planned trip (new row not in table yet)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_total_locomotives_length
        FROM Planned_Train_Locomotive PTL
        JOIN Locomotive L ON PTL.LocomotiveID = L.ID
        JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE PTL.Planned_TrainTrainID = :NEW.Planned_TrainTrainID AND PTL.Planned_TrainstartDate = :NEW.Planned_TrainstartDate;
        
        -- Add the new locomotive's length
        SELECT v_total_locomotives_length + VM.length
        INTO v_total_locomotives_length
        FROM Locomotive L
        JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE L.ID = :NEW.LocomotiveID;
    ELSE
        -- UPDATE: get all locomotives for this planned trip (including current row)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_total_locomotives_length
        FROM Planned_Train_Locomotive PTL
        JOIN Locomotive L ON PTL.LocomotiveID = L.ID
        JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE PTL.Planned_TrainTrainID = :NEW.Planned_TrainTrainID AND PTL.Planned_TrainstartDate = :NEW.Planned_TrainstartDate;
    END IF;
    
    -- Calculate total length of wagons assigned to this planned trip (TrainID + startDate)
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagons_length
    FROM Planned_Train_Wagon PTW
    JOIN Wagon W ON PTW.WagonID = W.ID
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE PTW.Planned_TrainTrainID = :NEW.Planned_TrainTrainID AND PTW.Planned_TrainstartDate = :NEW.Planned_TrainstartDate;
    
    -- Total length = all locomotives + all wagons for this planned trip (both in meters)
    v_current_total_length := v_total_locomotives_length + v_total_wagons_length;
    
    -- Check if total length exceeds the train's defined limit
    IF v_current_total_length > v_train_max_length THEN
        RAISE_APPLICATION_ERROR(-20003, 
            'Cannot add locomotive to train: Total length (' || 
            TO_CHAR(v_current_total_length) || 
            ') exceeds train maximum length (' || 
            TO_CHAR(v_train_max_length) || ') for Train ID ' || :NEW.Planned_TrainTrainID);
    END IF;
END;
/

-- Test: Try to add wagon 3563082 to train 5421 (should fail)
DECLARE
    v_train_id NUMBER := 5421;
    v_start_date DATE := TO_DATE('2025-10-03 09:45:00', 'YYYY-MM-DD HH24:MI:SS');  -- Route 1 startDate
    v_wagon_length NUMBER;
    v_max_length NUMBER;
    v_total_wagon_length NUMBER;
    v_total_locomotive_length NUMBER;
    v_total_length NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 3a: Current status of train 5421');
    
    -- Check current status of train
    SELECT maxLength
    INTO v_max_length
    FROM Train
    WHERE ID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagon_length
    FROM Planned_Train_Wagon PTW
    JOIN Wagon W ON PTW.WagonID = W.ID
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE PTW.Planned_TrainTrainID = v_train_id AND PTW.Planned_TrainstartDate = v_start_date;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_locomotive_length
    FROM Planned_Train_Locomotive PTL
    JOIN Locomotive L ON PTL.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE PTL.Planned_TrainTrainID = v_train_id AND PTL.Planned_TrainstartDate = v_start_date;
    
    v_total_length := v_total_wagon_length + v_total_locomotive_length;
    
    DBMS_OUTPUT.PUT_LINE('Train ' || v_train_id || ': Total=' || v_total_length || 
        'm, Max=' || v_max_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Remaining capacity: ' || (v_max_length - v_total_length) || 'm');
    
    -- Continue with test logic
    SELECT VM.length INTO v_wagon_length
    FROM Wagon W
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE W.ID = 3563082;
    
    INSERT INTO Planned_Train_Wagon (Planned_TrainTrainID, Planned_TrainstartDate, WagonID) 
    VALUES (v_train_id, v_start_date, 3563082);
    DBMS_OUTPUT.PUT_LINE('Insert succeeded');
    ROLLBACK;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        NULL;
    WHEN OTHERS THEN
        IF SQLCODE = -20001 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Trigger correctly prevented the insert');
        END IF;
        ROLLBACK;
END;
/

-- Test: Try to add a locomotive to train 5421 (should fail)
DECLARE
    v_train_id NUMBER := 5421;
    v_start_date DATE := TO_DATE('2025-10-03 09:45:00', 'YYYY-MM-DD HH24:MI:SS');  -- Route 1 startDate
    v_loco_id NUMBER;
    v_max_length NUMBER;
    v_total_wagon_length NUMBER;
    v_total_locomotive_length NUMBER;
    v_total_length NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 5: Current status of train 5421');
    
    -- Check current status of train
    SELECT maxLength
    INTO v_max_length
    FROM Train
    WHERE ID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagon_length
    FROM Planned_Train_Wagon PTW
    JOIN Wagon W ON PTW.WagonID = W.ID
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE PTW.Planned_TrainTrainID = v_train_id AND PTW.Planned_TrainstartDate = v_start_date;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_locomotive_length
    FROM Planned_Train_Locomotive PTL
    JOIN Locomotive L ON PTL.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE PTL.Planned_TrainTrainID = v_train_id AND PTL.Planned_TrainstartDate = v_start_date;
    
    v_total_length := v_total_wagon_length + v_total_locomotive_length;
    
    DBMS_OUTPUT.PUT_LINE('Train ' || v_train_id || ': Total=' || v_total_length || 
        'm, Max=' || v_max_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Remaining capacity: ' || (v_max_length - v_total_length) || 'm');
    
    -- Continue with test logic
    SELECT L.ID INTO v_loco_id
    FROM Locomotive L
    WHERE L.ID NOT IN (SELECT LocomotiveID FROM Planned_Train_Locomotive 
                       WHERE Planned_TrainTrainID = v_train_id AND Planned_TrainstartDate = v_start_date)
      AND ROWNUM = 1;
    
    INSERT INTO Planned_Train_Locomotive (Planned_TrainTrainID, Planned_TrainstartDate, LocomotiveID) 
    VALUES (v_train_id, v_start_date, v_loco_id);
    
    DBMS_OUTPUT.PUT_LINE('ERROR: Insert should have failed but it succeeded!');
    ROLLBACK;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        NULL;
    WHEN OTHERS THEN
        IF SQLCODE = -20003 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Trigger correctly prevented the insert');
        END IF;
        ROLLBACK;
END;
/

-- Test: Try to add a wagon to train 5437 (should pass if within limit)
DECLARE
    v_train_id NUMBER := 5437;
    v_start_date DATE := TO_DATE('2025-10-06 10:00:00', 'YYYY-MM-DD HH24:MI:SS');  -- Route 3 startDate
    v_wagon_id NUMBER;
    v_current_length NUMBER;
    v_max_length NUMBER;
    v_total_wagon_length NUMBER;
    v_total_locomotive_length NUMBER;
    v_total_length NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Test 6: Current status of train 5437');
    
    -- Check current status of train
    SELECT maxLength
    INTO v_max_length
    FROM Train
    WHERE ID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagon_length
    FROM Planned_Train_Wagon PTW
    JOIN Wagon W2 ON PTW.WagonID = W2.ID
    JOIN VehicleModel VM ON W2.VehicleModelID = VM.ID
    WHERE PTW.Planned_TrainTrainID = v_train_id AND PTW.PLANNED_TRAINSTARTDATE = v_start_date;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_locomotive_length
    FROM Planned_Train_Locomotive PTL
    JOIN Locomotive L ON PTL.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE PTL.Planned_TrainTrainID = v_train_id AND PTL.Planned_TrainstartDate = v_start_date;
    
    v_total_length := v_total_wagon_length + v_total_locomotive_length;
    
    DBMS_OUTPUT.PUT_LINE('Train ' || v_train_id || ': Total=' || v_total_length || 
        'm, Max=' || v_max_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Remaining capacity: ' || (v_max_length - v_total_length) || 'm');
    
    -- Continue with test logic
    SELECT W.ID
    INTO v_wagon_id
    FROM Wagon W
    WHERE W.ID NOT IN (SELECT WagonID FROM Planned_Train_Wagon 
                       WHERE Planned_TrainTrainID = v_train_id AND Planned_TrainstartDate = v_start_date)
      AND ROWNUM = 1;
    
    v_current_length := v_total_length;
    
    IF v_current_length <= v_max_length THEN
        INSERT INTO Planned_Train_Wagon (Planned_TrainTrainID, Planned_TrainstartDate, WagonID) 
        VALUES (v_train_id, v_start_date, v_wagon_id);
        DBMS_OUTPUT.PUT_LINE('Insert succeeded');
        ROLLBACK;
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        NULL;
    WHEN OTHERS THEN
        IF SQLCODE = -20001 THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Trigger correctly prevented the insert');
        END IF;
        ROLLBACK;
END;
/
