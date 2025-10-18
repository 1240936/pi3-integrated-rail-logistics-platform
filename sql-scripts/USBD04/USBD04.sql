INSERT INTO WAGON (ID, TRAINOPERATORID, WAGONMODELID) VALUES
(3563077, 'Medway', 1104),
(3563078, 'Medway', 1104),
(3563079, 'Medway', 1104),
(3563080, 'Medway', 1104),
(3563081, 'Medway', 1104),
(3563082, 'Medway', 1104),
(3563083, 'Medway', 1104),
(3563084, 'Medway', 1104),
(3563085, 'Medway', 1104),
(3563086, 'Medway', 1104),
(3563087, 'Medway', 1104),
(3563088, 'Medway', 1104),
(3563089, 'Medway', 1104),
(3563090, 'Medway', 1104),
(3563091, 'Medway', 1104),
(3563092, 'Medway', 1104),
(823045, 'Medway', 1245),
(823046, 'Medway', 1245),
(823047, 'Medway', 1245),
(823048, 'Medway', 1245),
(741001, 'Medway', 1278),
(741002, 'Medway', 1278),
(741003, 'Medway', 1278),
(741004, 'Medway', 1278),
(741005, 'Medway', 1278),
(741006, 'Medway', 1278),
(1811010, 'Medway', 1325),
(1811011, 'Medway', 1325),
(1811012, 'Medway', 1325),
(1811013, 'Medway', 1325),
(1811014, 'Medway', 1325)

INSERT INTO WAGONMODEL (ID, MODEL, GAUGEWIDTH, WAGONTYPE, PAYLOAD, LENGTH, WIDTH, HEIGHT, VOLUMECAPACITY, TARE) VALUES
(1245, 'Tadgs 32 94 082 3', 1668, 'Cereal wagon', 56.0, 17240, 3072, 4270, 75.0, 24.0),
(1278, 'Tdgs 41 94 074 1', 1668, 'Cereal wagon', 26.2, 9640, 3120, 4165.5, 38.0, 13.8),
(1325, 'Gabs 81 94 181 1', 1668, 'Covered wagon with sliding door', 50.2, 21700, 3180, 4170, 110.0, 29.8),
(1104, 'Regmms 32 94 356 3', 1668, 'Container wagon (max 40" HC)', 60.6, 14040, 3104, 2535, 76.3, 21.2),
(985,  'Lgs 22 94 441 6', 1668, 'Container wagon (max 40" HC)', 28.1, 13860, 2850, 1060, 76.3, 11.9),
(987,  'Sgnss 12 94 455 2', 1668, 'Container wagon (max 40" HC)', 68.4, 18116, 2950, 1030, 76.3, 21.6),
(988,  'Sgnss 12 94 455 2', 1435, 'Container wagon (max 40" HC)', 68.4, 18116, 2950, 1030, 76.3, 21.6)

INSERT INTO WAGONTYPE (TYPE, TANKCARTYPE) VALUES
('Cereal wagon', NULL),
('Covered wagon with sliding door', NULL),
('Container wagon (max 40" HC)', NULL)

INSERT INTO LOCOMOTIVE (ID, MODELNAME, TRAINOPERATORNAME) VALUES
(5621, 'Eurosprinter', 'Medway'),
(5623, 'Eurosprinter', 'Medway'),
(5630, 'Eurosprinter', 'Medway'),
(1903, 'CP 1900', 'Medway')

INSERT INTO LOCOMOTIVEMODEL_GAUGE (LOCOMOTIVEMODELNAME, GAUGEWIDTH) VALUES
('Eurosprinter', 1668),
('CP 1900', 1668)

INSERT INTO LocomotiveModel (name, locomotivetype, yearEIS, make, power, acceleration, length, width, height, maxweight, fuelCapacity, numberOfBogies) VALUES
('Eurosprinter', 'Electric', 1996, 'Siemens', 5600, NULL, 19.2, 3.0, 4.375, 87, NULL, 2),
('CP 1900', 'Diesel', 1981, 'Sorefame - Alsthom', 1623, NULL, 19.084, 3.062, 4.31, 117, 4882, 2)

INSERT INTO LOCOMOTIVETYPE (TYPE) VALUES
('Diesel'),
('Electric')

INSERT INTO TRAINOPERATOR(NAME) VALUES
('Medway')

INSERT INTO GAUGE(WIDTH) VALUES
(1668),
(1435)

INSERT INTO ENDPOINT_RAILLINE (ENDPOINTNAME, RAILLINEID, ENDPOINTID) VALUES
('Porto São Bento', 1, 7),
('Porto Campanhã', 1, 5),
('Porto Campanhã', 2, 5),
('Nine', 2, 20),
('Nine', 3, 20),
('Barcelos', 3, 8),
('Barcelos', 4, 8),
('Viana do Castelo', 4, 17),
('Viana do Castelo', 5, 17),
('Caminha', 5, 21),
('Caminha', 6, 21),
('São Pedro da Torre', 6, 16),
('São Pedro da Torre', 7, 16),
('Valença', 7, 11)

INSERT INTO LineSegment (ID, RailLineID, OwnerID, GaugeWidth, isElectrified, maxWeight, speedLimit, length, numberOfTracks) VALUES
(1, 1, 'IP', 1668, 1, 8000, NULL, 2618, 4),
(10, 2, 'IP', 1668, 1, 8000, NULL, 29003, 2),
(11, 2, 'IP', 1668, 1, 8000, NULL, 10000, 2),
(15, 3, 'IP', 1668, 1, 8000, NULL, 5286, 2),
(16, 3, 'IP', 1668, 1, 8000, NULL, 6000, 2),
(14, 4, 'IP', 1668, 1, 8000, NULL, 10387, 2),
(12, 4, 'IP', 1668, 1, 8000, NULL, 12000, 2),
(13, 4, 'IP', 1668, 1, 6400, NULL, 8000, 2),
(20, 5, 'IP', 1668, 1, 8000, NULL, 6000, 2),
(21, 5, 'IP', 1668, 1, 8000, NULL, 3000, 2),
(22, 5, 'IP', 1668, 1, 8000, NULL, 15000, 2),
(25, 6, 'IP', 1668, 1, 8000, NULL, 20829, 2),
(26, 7, 'IP', 1668, 1, 8000, NULL, 4264, 2)

INSERT INTO RAILLINE (ID, OwnerNAME, STARTENDPOINTID, STARTENDPOINT, ENDENDPOINTID, ENDENDPOINT) VALUES
(1, 'IP', 7, 'Porto São Bento', 5, 'Porto Campanhã'),
(2,'IP', 5, 'Porto Campanhã', 20, 'Nine'),
(3,'IP', 20, 'Nine', 8, 'Barcelos'),
(4, 'IP', 8, 'Barcelos', 17, 'Viana do Castelo'),
(5, 'IP', 17, 'Viana do Castelo', 21, 'Caminha'),
(6, 'IP', 21, 'Caminha', 16, 'São Pedro da Torre'),
(7, 'IP', 16, 'São Pedro da Torre', 11, 'Valença')

INSERT INTO Owner (Name) VALUES
('IP')

INSERT INTO ENDPOINT (ID, Name) VALUES
(1, 'São Romão'),
(2, 'Tamel'),
(3, 'Senhora das Dores'),
(4, 'Lousado'),
(5, 'Porto Campanhã'),
(6, 'Leandro'),
(7, 'Porto São Bento'),
(8, 'Barcelos'),
(9, 'Vila Nova da Cerveira'),
(10, 'Midões'),
(11, 'Valença'),
(12, 'Darque'),
(13, 'Contumil'),
(14, 'Ermesinde'),
(15, 'São Frutuoso'),
(16, 'São Pedro da Torre'),
(17, 'Viana do Castelo'),
(18, 'Famalicão'),
(19, 'Barroselas'),
(20, 'Nine'),
(21, 'Caminha'),
(22, 'Carvalha'),
(23, 'Carreço')