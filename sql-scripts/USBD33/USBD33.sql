CREATE OR REPLACE TRIGGER TRG_VALIDATE_TRAIN_LENGTH_WAGON
BEFORE INSERT OR UPDATE ON Train_Wagon
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
        WHERE ID = :NEW.TrainID;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20002, 'Train ' || :NEW.TrainID || ' not found');
    END;
    
    -- Calculate total length of wagons already assigned to this train
    -- For INSERT: new row not in table yet, so we'll add it manually
    -- For UPDATE: exclude the current row if TrainID unchanged, or include all if TrainID changed
    IF INSERTING THEN
        -- Get existing wagons (new row not in table yet)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_total_wagons_length
        FROM Train_Wagon TW
        JOIN Wagon W ON TW.WagonID = W.ID
        JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE TW.TrainID = :NEW.TrainID;
        
        -- Add the new wagon's length
        SELECT v_total_wagons_length + VM.length
        INTO v_total_wagons_length
        FROM Wagon W
        JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE W.ID = :NEW.WagonID;
    ELSE
        -- UPDATE: get all wagons (including current row if TrainID unchanged)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_total_wagons_length
        FROM Train_Wagon TW
        JOIN Wagon W ON TW.WagonID = W.ID
        JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
        WHERE TW.TrainID = :NEW.TrainID;
    END IF;
    
    -- For UPDATE, if TrainID changed, also check the old train
    IF UPDATING AND :OLD.TrainID IS NOT NULL AND :OLD.TrainID != :NEW.TrainID THEN
        DECLARE
            v_old_train_max_length NUMBER;
            v_old_total_wagons_length NUMBER := 0;
            v_old_total_locomotives_length NUMBER := 0;
            v_old_current_total_length NUMBER;
        BEGIN
            SELECT maxLength
            INTO v_old_train_max_length
            FROM Train
            WHERE ID = :OLD.TrainID;
            
            SELECT SUM(VM.length)
            INTO v_old_total_wagons_length
            FROM Train_Wagon TW
            JOIN Wagon W ON TW.WagonID = W.ID
            JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
            WHERE TW.TrainID = :OLD.TrainID;
            
            SELECT SUM(VM.length)
            INTO v_old_total_locomotives_length
            FROM Locomotive_Train LT
            JOIN Locomotive L ON LT.LocomotiveID = L.ID
            JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
            WHERE LT.TrainID = :OLD.TrainID;
            
            IF v_old_total_wagons_length IS NULL THEN
                v_old_total_wagons_length := 0;
            END IF;
            IF v_old_total_locomotives_length IS NULL THEN
                v_old_total_locomotives_length := 0;
            END IF;
            
            v_old_current_total_length := v_old_total_wagons_length + v_old_total_locomotives_length;
            
            IF v_old_current_total_length > v_old_train_max_length THEN
                RAISE_APPLICATION_ERROR(-20001, 
                    'Cannot move wagon from train: Total length (' || 
                    TO_CHAR(v_old_current_total_length) || 
                    ') exceeds train maximum length (' || 
                    TO_CHAR(v_old_train_max_length) || ') for Train ID ' || :OLD.TrainID);
            END IF;
        END;
    END IF;
    
    -- Calculate total length of ALL locomotives assigned to this train
    SELECT SUM(VM.length)
    INTO v_total_locomotives_length
    FROM Locomotive_Train LT
    JOIN Locomotive L ON LT.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE LT.TrainID = :NEW.TrainID;
    
    -- Total length = all wagons + all locomotives (both in meters)
    v_current_total_length := v_total_wagons_length + v_total_locomotives_length;
    
    -- Check if total length exceeds the train's defined limit
    IF v_current_total_length > v_train_max_length THEN
        RAISE_APPLICATION_ERROR(-20001, 
            'Cannot add wagon to train: Total length (' || 
            TO_CHAR(v_current_total_length) || 
            ') exceeds train maximum length (' || 
            TO_CHAR(v_train_max_length) || ') for Train ID ' || :NEW.TrainID);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_VALIDATE_TRAIN_LENGTH_LOCOMOTIVE
BEFORE INSERT OR UPDATE ON Locomotive_Train
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
        WHERE ID = :NEW.TrainID;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20004, 'Train ' || :NEW.TrainID || ' not found');
    END;
    
    -- Calculate total length of locomotives assigned to this train
    IF INSERTING THEN
        -- Get existing locomotives (new row not in table yet)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_total_locomotives_length
        FROM Locomotive_Train LT
        JOIN Locomotive L ON LT.LocomotiveID = L.ID
        JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE LT.TrainID = :NEW.TrainID;
        
        -- Add the new locomotive's length
        SELECT v_total_locomotives_length + VM.length
        INTO v_total_locomotives_length
        FROM Locomotive L
        JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE L.ID = :NEW.LocomotiveID;
    ELSE
        -- UPDATE: get all locomotives (including current row if TrainID unchanged)
        SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
        INTO v_total_locomotives_length
        FROM Locomotive_Train LT
        JOIN Locomotive L ON LT.LocomotiveID = L.ID
        JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
        WHERE LT.TrainID = :NEW.TrainID;
    END IF;
    
    -- For UPDATE, if TrainID changed, also check the old train
    IF UPDATING AND :OLD.TrainID IS NOT NULL AND :OLD.TrainID != :NEW.TrainID THEN
        DECLARE
            v_old_train_max_length NUMBER;
            v_old_total_locomotives_length NUMBER := 0;
            v_old_total_wagons_length NUMBER := 0;
            v_old_current_total_length NUMBER;
        BEGIN
            SELECT maxLength
            INTO v_old_train_max_length
            FROM Train
            WHERE ID = :OLD.TrainID;
            
            SELECT SUM(VM.length)
            INTO v_old_total_locomotives_length
            FROM Locomotive_Train LT
            JOIN Locomotive L ON LT.LocomotiveID = L.ID
            JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
            WHERE LT.TrainID = :OLD.TrainID;
            
            SELECT SUM(VM.length)
            INTO v_old_total_wagons_length
            FROM Train_Wagon TW
            JOIN Wagon W ON TW.WagonID = W.ID
            JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
            WHERE TW.TrainID = :OLD.TrainID;
            
            IF v_old_total_locomotives_length IS NULL THEN
                v_old_total_locomotives_length := 0;
            END IF;
            IF v_old_total_wagons_length IS NULL THEN
                v_old_total_wagons_length := 0;
            END IF;
            
            v_old_current_total_length := v_old_total_locomotives_length + v_old_total_wagons_length;
            
            IF v_old_current_total_length > v_old_train_max_length THEN
                RAISE_APPLICATION_ERROR(-20003, 
                    'Cannot move locomotive from train: Total length (' || 
                    TO_CHAR(v_old_current_total_length) || 
                    ') exceeds train maximum length (' || 
                    TO_CHAR(v_old_train_max_length) || ') for Train ID ' || :OLD.TrainID);
            END IF;
        END;
    END IF;
    
    -- Calculate total length of wagons assigned to this train
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagons_length
    FROM Train_Wagon TW
    JOIN Wagon W ON TW.WagonID = W.ID
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE TW.TrainID = :NEW.TrainID;
    
    -- Total length = all locomotives + all wagons (both in meters)
    v_current_total_length := v_total_locomotives_length + v_total_wagons_length;
    
    -- Check if total length exceeds the train's defined limit
    IF v_current_total_length > v_train_max_length THEN
        RAISE_APPLICATION_ERROR(-20003, 
            'Cannot add locomotive to train: Total length (' || 
            TO_CHAR(v_current_total_length) || 
            ') exceeds train maximum length (' || 
            TO_CHAR(v_train_max_length) || ') for Train ID ' || :NEW.TrainID);
    END IF;
END;
/

-- Test: Try to add wagon 3563082 to train 5421 (should fail)
DECLARE
    v_train_id NUMBER := 5421;
    v_wagon_length NUMBER;
    v_max_length NUMBER;
    v_total_wagon_length NUMBER;
    v_total_locomotive_length NUMBER;
    v_total_length NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== Test: Try to add wagon 3563082 to train 5421 ===');
    
    -- Check current status of train
    SELECT maxLength
    INTO v_max_length
    FROM Train
    WHERE ID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagon_length
    FROM Train_Wagon TW
    JOIN Wagon W ON TW.WagonID = W.ID
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE TW.TrainID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_locomotive_length
    FROM Locomotive_Train LT
    JOIN Locomotive L ON LT.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE LT.TrainID = v_train_id;
    
    v_total_length := v_total_wagon_length + v_total_locomotive_length;
    
    DBMS_OUTPUT.PUT_LINE('Train ' || v_train_id || ': WagonLength=' || v_total_wagon_length || 
        'm, LocoLength=' || v_total_locomotive_length || 'm, Total=' || v_total_length || 
        'm, Max=' || v_max_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Remaining capacity: ' || (v_max_length - v_total_length) || 'm');
    
    -- Continue with test logic
    SELECT VM.length INTO v_wagon_length
    FROM Wagon W
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE W.ID = 3563082;
    
    INSERT INTO Train_Wagon (TrainID, WagonID) VALUES (v_train_id, 3563082);
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
    v_loco_id NUMBER;
    v_max_length NUMBER;
    v_total_wagon_length NUMBER;
    v_total_locomotive_length NUMBER;
    v_total_length NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== Test: Try to add a locomotive to train 5421 ===');
    
    -- Check current status of train
    SELECT maxLength
    INTO v_max_length
    FROM Train
    WHERE ID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagon_length
    FROM Train_Wagon TW
    JOIN Wagon W ON TW.WagonID = W.ID
    JOIN VehicleModel VM ON W.VehicleModelID = VM.ID
    WHERE TW.TrainID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_locomotive_length
    FROM Locomotive_Train LT
    JOIN Locomotive L ON LT.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE LT.TrainID = v_train_id;
    
    v_total_length := v_total_wagon_length + v_total_locomotive_length;
    
    DBMS_OUTPUT.PUT_LINE('Train ' || v_train_id || ': WagonLength=' || v_total_wagon_length || 
        'm, LocoLength=' || v_total_locomotive_length || 'm, Total=' || v_total_length || 
        'm, Max=' || v_max_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Remaining capacity: ' || (v_max_length - v_total_length) || 'm');
    
    -- Continue with test logic
    SELECT L.ID INTO v_loco_id
    FROM Locomotive L
    WHERE L.ID NOT IN (SELECT LocomotiveID FROM Locomotive_Train WHERE TrainID = v_train_id)
      AND ROWNUM = 1;
    
    INSERT INTO Locomotive_Train (LocomotiveID, TrainID) 
    VALUES (v_loco_id, v_train_id);
    
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

-- Test: Try to add a wagon to train 5437 (should pass)
DECLARE
    v_train_id NUMBER := 5437;
    v_wagon_id NUMBER;
    v_current_length NUMBER;
    v_max_length NUMBER;
    v_loco_total NUMBER;
    v_total_wagon_length NUMBER;
    v_total_locomotive_length NUMBER;
    v_total_length NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== Test: Try to add a wagon to train 5437 ===');
    
    -- Check current status of train
    SELECT maxLength
    INTO v_max_length
    FROM Train
    WHERE ID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_wagon_length
    FROM Train_Wagon TW
    JOIN Wagon W2 ON TW.WagonID = W2.ID
    JOIN VehicleModel VM ON W2.VehicleModelID = VM.ID
    WHERE TW.TrainID = v_train_id;
    
    SELECT CASE WHEN SUM(VM.length) IS NULL THEN 0 ELSE SUM(VM.length) END
    INTO v_total_locomotive_length
    FROM Locomotive_Train LT
    JOIN Locomotive L ON LT.LocomotiveID = L.ID
    JOIN VehicleModel VM ON L.VehicleModelID = VM.ID
    WHERE LT.TrainID = v_train_id;
    
    v_total_length := v_total_wagon_length + v_total_locomotive_length;
    
    DBMS_OUTPUT.PUT_LINE('Train ' || v_train_id || ': WagonLength=' || v_total_wagon_length || 
        'm, LocoLength=' || v_total_locomotive_length || 'm, Total=' || v_total_length || 
        'm, Max=' || v_max_length || 'm');
    DBMS_OUTPUT.PUT_LINE('Remaining capacity: ' || (v_max_length - v_total_length) || 'm');
    
    -- Continue with test logic
    SELECT W.ID
    INTO v_wagon_id
    FROM Wagon W
    WHERE W.ID NOT IN (SELECT WagonID FROM Train_Wagon WHERE TrainID = v_train_id)
      AND ROWNUM = 1;
    
    v_current_length := v_total_length;
    
    IF v_current_length <= v_max_length THEN
        INSERT INTO Train_Wagon (TrainID, WagonID) 
        VALUES (v_train_id, v_wagon_id);
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
