INSERT INTO Facility (ID, name) VALUES (1, 'São Romão');
INSERT INTO Facility (ID, name) VALUES (2, 'Tamel');
INSERT INTO Facility (ID, name) VALUES (3, 'Senhora das Dores');
INSERT INTO Facility (ID, name) VALUES (4, 'Lousado');
INSERT INTO Facility (ID, name) VALUES (5, 'Porto Campanhã');
INSERT INTO Facility (ID, name) VALUES (6, 'Leandro');
INSERT INTO Facility (ID, name) VALUES (7, 'Porto São Bento');
INSERT INTO Facility (ID, name) VALUES (8, 'Barcelos');
INSERT INTO Facility (ID, name) VALUES (9, 'Vila Nova da Cerveira');
INSERT INTO Facility (ID, name) VALUES (10, 'Midões');
INSERT INTO Facility (ID, name) VALUES (11, 'Valença');
INSERT INTO Facility (ID, name) VALUES (12, 'Darque');
INSERT INTO Facility (ID, name) VALUES (13, 'Contumil');
INSERT INTO Facility (ID, name) VALUES (14, 'Ermesinde');
INSERT INTO Facility (ID, name) VALUES (15, 'São Frutuoso');
INSERT INTO Facility (ID, name) VALUES (16, 'São Pedro da Torre');
INSERT INTO Facility (ID, name) VALUES (17, 'Viana do Castelo');
INSERT INTO Facility (ID, name) VALUES (18, 'Famalicão');
INSERT INTO Facility (ID, name) VALUES (19, 'Barroselas');
INSERT INTO Facility (ID, name) VALUES (20, 'Nine');
INSERT INTO Facility (ID, name) VALUES (21, 'Caminha');
INSERT INTO Facility (ID, name) VALUES (22, 'Carvalha');
INSERT INTO Facility (ID, name) VALUES (23, 'Carreço');
INSERT INTO Facility (ID, name) VALUES (30, 'Braga');
INSERT INTO Facility (ID, name) VALUES (31, 'Manzagão');
INSERT INTO Facility (ID, name) VALUES (32, 'Cerqueiral');
INSERT INTO Facility (ID, name) VALUES (33, 'Gemieira');
INSERT INTO Facility (ID, name) VALUES (35, 'Paredes de Coura');
INSERT INTO Facility (ID, name) VALUES (43, 'São Gemil');
INSERT INTO Facility (ID, name) VALUES (45, 'São Mamede de Infesta');
INSERT INTO Facility (ID, name) VALUES (48, 'Leça do Balio');
INSERT INTO Facility (ID, name) VALUES (50, 'Leixões');

INSERT INTO Owner (ID, name) VALUES (1, 'Infraestruturas de Portugal, SA');

INSERT INTO TrainOperator (ID, name) VALUES (1, 'Medway');
INSERT INTO TrainOperator (ID, name) VALUES (2, 'Captrain');

INSERT INTO Gauge (ID, width, name) VALUES (1, 1668, 'Iberian Gauge');
INSERT INTO Gauge (ID, width, name) VALUES (2, 1435, 'Standard Gauge');

INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (1, 1, 7, 5, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (2, 1, 5, 13, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (3, 1, 13, 20, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (4, 1, 20, 8, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (5, 1, 8, 12, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (6, 1, 12, 17, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (7, 1, 17, 21, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (8, 1, 21, 16, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (9, 1, 16, 11, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (21, 1, 13, 43, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (22, 1, 43, 45, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (23, 1, 45, 48, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (24, 1, 48, 50, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (30, 1, 31, 30, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (31, 1, 20, 31, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (32, 1, 31, 32, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (35, 1, 32, 33, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (36, 1, 33, 35, 1, 1);
INSERT INTO RailLine (ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified)
VALUES (37, 1, 35, 11, 1, 1);

INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (1, 1, 8000, 2618, 4, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (3, 2, 8000, 2443, 4, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (10, 3, 8000, 26560, 2, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (11, 3, 8000, 10000, 2, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (15, 4, 8000, 5286, 2, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (16, 4, 8000, 6000, 2, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (14, 5, 8000, 10387, 2, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (12, 5, 8000, 12000, 2, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (13, 5, 8000, 3100, 2, 110, 3);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (20, 6, 6400, 4890, 2, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (18, 7, 8000, 6000, 1, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (21, 7, 8000, 5000, 1, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (22, 7, 8000, 12000, 1, 110, 3);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (25, 8, 8000, 20829, 1, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (26, 9, 8000, 4264, 1, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (30, 21, 8000, 3883, 2, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (31, 22, 8400, 1174, 2, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (32, 22, 8000, 2534, 2, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (33, 23, 8000, 1566, 2, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (34, 23, 8000, 1453, 2, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (35, 24, 8100, 3597, 2, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (36, 24, 8000, 4334, 2, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (50, 30, 8000, 3555, 1, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (51, 31, 8000, 1222, 1, 110, 5);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (52, 31, 8000, 1760, 1, 110, 4);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (53, 31, 8000, 1720, 1, 110, 3);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (54, 31, 8000, 3350, 1, 110, 2);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (55, 31, 8000, 3470, 1, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (58, 32, 8000, 8050, 1, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (59, 35, 8000, 22320, 1, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (60, 36, 8000, 16310, 1, 110, 1);
INSERT INTO LineSegment (ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum)
VALUES (61, 37, 8000, 15200, 1, 110, 1);

INSERT INTO Siding (ID, LineSegmentID, position, length) VALUES (1, 21, 2000, 864);
INSERT INTO Siding (ID, LineSegmentID, position, length) VALUES (2, 25, 11000, 266);

INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (985, 'Lgs 22 94 441 6', 13.86, 2.85, 1.06, 11.9, 1987);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (987, 'Sgnss 12 94 455 2', 18.116, 2.95, 1.03, 21.6, 1987);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (988, 'Sgnss 12 94 455 2', 18.116, 2.95, 1.03, 21.6, 1987);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (1104, 'Regmms 32 94 356 3', 14.04, 3.104, 2.535, 21.2, 1987);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (1212, 'Kbs 41 94 333', 14.02, 2.842, 3.3, 14.2, 2005);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (1245, 'Tadgs 32 94 082 3', 17.24, 3.072, 4.27, 24, 1990);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (1278, 'Tdgs 41 94 074 1', 9.64, 3.12, 4.166, 13.8, 1977);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (1325, 'Gabs 81 94 181 1', 21.7, 3.18, 4.17, 29.8, 1977);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (1523, 'Zaes 81 94 788', 13.8, 2.95, 4.226, 22.9, 2005);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (1525, 'Zaes 81 94 788', 13.8, 2.95, 4.226, 22.9, 2005);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (1903, 'CP 1900', 19.084, 3.062, 4.31, 117, 1981);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (5034, 'E4000', 23.02, 3.0, 4.264, 124, 2017);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (5036, 'E4000', 23.02, 3.0, 4.264, 124, 2017);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (5621, 'Eurosprinter', 19.2, 3.0, 4.375, 87, 1995);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (5623, 'Eurosprinter', 19.2, 3.0, 4.375, 87, 1995);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (5630, 'Eurosprinter', 19.2, 3.0, 4.375, 87, 1996);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (335001, 'E4000', 23.02, 3.0, 4.264, 124, 2019);
INSERT INTO VehicleModel (ID, modelName, length, width, height, tare, yearEIS)
VALUES (335003, 'E4000', 23.02, 3.0, 4.264, 124, 2019);

INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
VALUES (1903, 'Sorefame - Alsthom', 1623, 0.3, 42.5 , 100, 12);
INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
VALUES (5034, 'Stadler', 3178, 0.45, 100, 120, 12);
INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
VALUES (5036, 'Stadler', 3178, 0.45, 100, 120, 12);
INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
VALUES (5621, 'Siemens', 5600, 0.55, 70, 220, 8);
INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
VALUES (5623, 'Siemens', 5600, 0.55, 70, 220, 8);
INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
VALUES (5630, 'Siemens', 5600, 0.55, 70, 220, 8);
INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
VALUES (335001, 'Stadler', 3178, 0.45, 100, 120, 12);
INSERT INTO LocomotiveSpecs (VehicleModelID, make, power, acceleration, operationalSpeed, maxSpeed, numberOfWheels)
VALUES (335003, 'Stadler', 3178, 0.45, 100, 120, 12);

INSERT INTO DieselType (VehicleModelID, fuelCapacity) VALUES (1903, 4882);
INSERT INTO DieselType (VehicleModelID, fuelCapacity) VALUES (5034, 6700);
INSERT INTO DieselType (VehicleModelID, fuelCapacity) VALUES (5036, 6700);
INSERT INTO DieselType (VehicleModelID, fuelCapacity) VALUES (335001, 6700);
INSERT INTO DieselType (VehicleModelID, fuelCapacity) VALUES (335003, 6700);

INSERT INTO ElectricalType (VehicleModelID, frequency, voltage) VALUES (5621, 50, 25000);
INSERT INTO ElectricalType (VehicleModelID, frequency, voltage) VALUES (5623, 50, 25000);
INSERT INTO ElectricalType (VehicleModelID, frequency, voltage) VALUES (5630, 50, 25000);

INSERT INTO WagonType (ID, type) VALUES (1, 'Cereal wagon');
INSERT INTO WagonType (ID, type) VALUES (2, 'Covered wagon with sliding door');
INSERT INTO WagonType (ID, type) VALUES (3, 'Container wagon (max 40'' HC)');
INSERT INTO WagonType (ID, type) VALUES (4, 'Biodiesel wagon');
INSERT INTO WagonType (ID, type) VALUES (5, 'Wood wagon');

INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (985, 3, 76.3, 28.1);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (987, 3, 76.3, 68.4);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (988, 3, 76.3, 68.4);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (1104, 3, 76.3, 60.6);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (1212, 5, 62.7, 25.8);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (1245, 1, 75, 56);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (1278, 1, 38, 26.2);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (1325, 2, 110, 50.2);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (1523, 4, 64.6, 57.1);
INSERT INTO WagonSpecs (VehicleModelID, WagonTypeID, volumeCapacity, payload)
VALUES (1525, 4, 64.6, 57.1);

INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (985, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (987, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (988, 2);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (1104, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (1212, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (1245, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (1278, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (1325, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (1523, 2);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (1525, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (1903, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (5034, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (5036, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (5621, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (5623, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (5630, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (335001, 1);
INSERT INTO VehicleModel_Gauge (VehicleModelID, GaugeID) VALUES (335003, 1);

INSERT INTO Train (ID, TrainOperatorID, maxLength) VALUES (5421, 1, 500);
INSERT INTO Train (ID, TrainOperatorID, maxLength) VALUES (5435, 1, 500);
INSERT INTO Train (ID, TrainOperatorID, maxLength) VALUES (5437, 2, 500);

INSERT INTO Route (ID, StartFacilityID, EndFacilityID) VALUES (1, 50, 11);
INSERT INTO Route (ID, StartFacilityID, EndFacilityID) VALUES (2, 11, 50);
INSERT INTO Route (ID, StartFacilityID, EndFacilityID) VALUES (3, 11, 50);

INSERT INTO Planned_Train (TrainID, startDate, RouteID)
VALUES (5421, TO_DATE('2025-10-03 09:45:00', 'YYYY-MM-DD HH24:MI:SS'), 1);
INSERT INTO Planned_Train (TrainID, startDate, RouteID)
VALUES (5435, TO_DATE('2025-10-03 18:00:00', 'YYYY-MM-DD HH24:MI:SS'), 2);
INSERT INTO Planned_Train (TrainID, startDate, RouteID)
VALUES (5437, TO_DATE('2025-10-06 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), 3);

INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 48, 2);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 45, 3);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 43, 4);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 13, 5);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 20, 6);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 8, 7);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 12, 8);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 17, 9);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 21, 10);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (1, 16, 11);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 16, 2);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 21, 3);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 17, 4);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 12, 5);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 8, 6);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 20, 7);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 13, 8);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 43, 9);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 45, 10);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (2, 48, 11);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 16, 2);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 21, 3);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 17, 4);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 12, 5);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 8, 6);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 20, 7);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 13, 8);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 43, 9);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 45, 10);
INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (3, 48, 11);

INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2001, 50, 12);
INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2002, 13, 11);
INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2003, 50, 11);
INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2004, 50, 21);
INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2005, 20, 11);
INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2006, 12, 50);
INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2007, 50, 5);
INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2050, 12, 50);
INSERT INTO Freight (ID, OriginFacilityID, DestinationFacilityID) VALUES (2051, 11, 50);

INSERT INTO Locomotive (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (1, 5621, 1, 50);
INSERT INTO Locomotive (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (2, 5623, 1, 50);
INSERT INTO Locomotive (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3, 5630, 1, 50);
INSERT INTO Locomotive (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (4, 1903, 1, 50);
INSERT INTO Locomotive (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (5, 5034, 1, 50);
INSERT INTO Locomotive (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (6, 5036, 1, 50);
INSERT INTO Locomotive (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (7, 335001, 2, 50);
INSERT INTO Locomotive (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (8, 335003, 2, 50);

INSERT INTO Locomotive_Train (LocomotiveID, TrainID) VALUES (1, 5421);
INSERT INTO Locomotive_Train (LocomotiveID, TrainID) VALUES (1, 5437);
INSERT INTO Locomotive_Train (LocomotiveID, TrainID) VALUES (2, 5421);
INSERT INTO Locomotive_Train (LocomotiveID, TrainID) VALUES (2, 5435);

INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563077, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563078, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563079, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563080, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563081, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563082, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563083, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563084, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563085, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563086, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563087, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563088, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563089, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563090, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563091, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3563092, 1104, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0823045, 1245, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0823046, 1245, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0823047, 1245, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0823048, 1245, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0741001, 1278, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0741002, 1278, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0741003, 1278, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0741004, 1278, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0741005, 1278, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (0741006, 1278, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (1811010, 1325, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (1811011, 1325, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (1811012, 1325, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (1811013, 1325, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (1811014, 1325, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330001, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330002, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330003, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330004, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330005, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330006, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330007, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330008, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330009, 1212, 1, 50);
INSERT INTO Wagon (ID, VehicleModelID, TrainOperatorID, InitialFacilityID) VALUES (3330010, 1212, 1, 50);

INSERT INTO Freight_Wagon VALUES (2001, 3330001);
INSERT INTO Freight_Wagon VALUES (2001, 3330002);
INSERT INTO Freight_Wagon VALUES (2001, 3330004);
INSERT INTO Freight_Wagon VALUES (2001, 3330005);
INSERT INTO Freight_Wagon VALUES (2001, 3330006);
INSERT INTO Freight_Wagon VALUES (2002, 3563089);
INSERT INTO Freight_Wagon VALUES (2003, 1811011);
INSERT INTO Freight_Wagon VALUES (2003, 1811012);
INSERT INTO Freight_Wagon VALUES (2004, 1811013);
INSERT INTO Freight_Wagon VALUES (2005, 3563077);
INSERT INTO Freight_Wagon VALUES (2005, 3563078);
INSERT INTO Freight_Wagon VALUES (2005, 3563079);
INSERT INTO Freight_Wagon VALUES (2005, 3563080);
INSERT INTO Freight_Wagon VALUES (2006, 3330003);
INSERT INTO Freight_Wagon VALUES (2006, 3330007);
INSERT INTO Freight_Wagon VALUES (2007, 3563090);
INSERT INTO Freight_Wagon VALUES (2007, 3563091);
INSERT INTO Freight_Wagon VALUES (2007, 3563092);
INSERT INTO Freight_Wagon VALUES (2050, 3330001);
INSERT INTO Freight_Wagon VALUES (2050, 3330002);
INSERT INTO Freight_Wagon VALUES (2050, 3330004);
INSERT INTO Freight_Wagon VALUES (2050, 3330005);
INSERT INTO Freight_Wagon VALUES (2050, 3330006);
INSERT INTO Freight_Wagon VALUES (2051, 1811011);
INSERT INTO Freight_Wagon VALUES (2051, 1811012);
INSERT INTO Freight_Wagon VALUES (2051, 3563077);
INSERT INTO Freight_Wagon VALUES (2051, 3563078);
INSERT INTO Freight_Wagon VALUES (2051, 3563079);
INSERT INTO Freight_Wagon VALUES (2051, 3563080);

INSERT INTO Train_Wagon VALUES (5421, 3330001);
INSERT INTO Train_Wagon VALUES (5421, 3330002);
INSERT INTO Train_Wagon VALUES (5421, 3330004);
INSERT INTO Train_Wagon VALUES (5421, 3330005);
INSERT INTO Train_Wagon VALUES (5421, 3330006);
INSERT INTO Train_Wagon VALUES (5421, 3563089);
INSERT INTO Train_Wagon VALUES (5421, 1811011);
INSERT INTO Train_Wagon VALUES (5421, 1811012);
INSERT INTO Train_Wagon VALUES (5421, 1811013);
INSERT INTO Train_Wagon VALUES (5421, 3563077);
INSERT INTO Train_Wagon VALUES (5421, 3563078);
INSERT INTO Train_Wagon VALUES (5421, 3563079);
INSERT INTO Train_Wagon VALUES (5421, 3563080);
INSERT INTO Train_Wagon VALUES (5435, 3330003);
INSERT INTO Train_Wagon VALUES (5435, 3330007);
INSERT INTO Train_Wagon VALUES (5437, 3330001);
INSERT INTO Train_Wagon VALUES (5437, 3330002);
INSERT INTO Train_Wagon VALUES (5437, 3330004);
INSERT INTO Train_Wagon VALUES (5437, 3330005);
INSERT INTO Train_Wagon VALUES (5437, 3330006);
INSERT INTO Train_Wagon VALUES (5437, 1811011);
INSERT INTO Train_Wagon VALUES (5437, 1811012);
INSERT INTO Train_Wagon VALUES (5437, 3563077);
INSERT INTO Train_Wagon VALUES (5437, 3563078);
INSERT INTO Train_Wagon VALUES (5437, 3563079);
INSERT INTO Train_Wagon VALUES (5437, 3563080);

INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (1, 5421, 50, TO_DATE('2025-10-03 09:45:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (2, 5421, 48, TO_DATE('2025-10-03 10:15:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (3, 5421, 45, TO_DATE('2025-10-03 10:45:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (4, 5421, 43, TO_DATE('2025-10-03 11:15:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (5, 5421, 43, TO_DATE('2025-10-03 11:30:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (6, 5421, 13, TO_DATE('2025-10-03 11:50:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (7, 5421, 20, TO_DATE('2025-10-03 12:15:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (8, 5421, 20, TO_DATE('2025-10-03 12:30:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (9, 5421, 8, TO_DATE('2025-10-03 12:50:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (10, 5421, 12, TO_DATE('2025-10-03 13:15:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (11, 5421, 17, TO_DATE('2025-10-03 13:45:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (12, 5421, 17, TO_DATE('2025-10-03 14:00:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (13, 5421, 21, TO_DATE('2025-10-03 14:30:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (14, 5421, 16, TO_DATE('2025-10-03 14:50:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (15, 5421, 11, TO_DATE('2025-10-03 15:15:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (16, 5435, 11, TO_DATE('2025-10-03 18:00:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (17, 5435, 16, TO_DATE('2025-10-03 18:30:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (18, 5435, 21, TO_DATE('2025-10-03 19:00:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (19, 5435, 17, TO_DATE('2025-10-03 19:30:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (20, 5435, 17, TO_DATE('2025-10-03 19:45:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (21, 5435, 12, TO_DATE('2025-10-03 20:15:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (22, 5435, 8, TO_DATE('2025-10-03 20:45:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (23, 5435, 20, TO_DATE('2025-10-03 21:15:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (24, 5435, 13, TO_DATE('2025-10-03 21:45:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (25, 5435, 43, TO_DATE('2025-10-03 22:15:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (26, 5435, 45, TO_DATE('2025-10-03 22:45:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (27, 5435, 48, TO_DATE('2025-10-03 23:15:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (28, 5435, 50, TO_DATE('2025-10-03 23:30:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (29, 5437, 11, TO_DATE('2025-10-06 10:00:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (30, 5437, 16, TO_DATE('2025-10-06 10:30:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (31, 5437, 21, TO_DATE('2025-10-06 11:00:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (32, 5437, 17, TO_DATE('2025-10-06 11:30:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (33, 5437, 17, TO_DATE('2025-10-06 11:45:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (34, 5437, 12, TO_DATE('2025-10-06 12:15:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (35, 5437, 12, TO_DATE('2025-10-06 12:30:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (36, 5437, 8, TO_DATE('2025-10-06 12:50:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (37, 5437, 20, TO_DATE('2025-10-06 13:15:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (38, 5437, 20, TO_DATE('2025-10-06 13:30:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (39, 5437, 13, TO_DATE('2025-10-06 13:50:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (40, 5437, 43, TO_DATE('2025-10-06 14:15:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (41, 5437, 43, TO_DATE('2025-10-06 14:30:00','YYYY-MM-DD HH24:MI:SS'), 'Departure');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (42, 5437, 45, TO_DATE('2025-10-06 14:50:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (43, 5437, 48, TO_DATE('2025-10-06 15:15:00','YYYY-MM-DD HH24:MI:SS'), 'Passing');
INSERT INTO TrainEvent (ID, TrainID, FacilityID, eventTime, eventType) VALUES (44, 5437, 50, TO_DATE('2025-10-06 15:30:00','YYYY-MM-DD HH24:MI:SS'), 'Arrival');