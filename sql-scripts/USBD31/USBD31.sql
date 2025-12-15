CREATE TABLE Building (
                          ID             number(10) NOT NULL,
                          FacilityID     number(10) NOT NULL,
                          BuildingTypeID number(10) NOT NULL,
                          PRIMARY KEY (ID));
CREATE TABLE BuildingType (
                              ID   number(10) NOT NULL,
                              type varchar2(255) NOT NULL UNIQUE,
                              PRIMARY KEY (ID));
CREATE TABLE DieselType (
                            VehicleModelID number(10) NOT NULL,
                            fuelCapacity   double precision NOT NULL,
                            PRIMARY KEY (VehicleModelID));
CREATE TABLE ElectricalType (
                                VehicleModelID number(10) NOT NULL,
                                frequency      double precision NOT NULL,
                                voltage        double precision NOT NULL,
                                PRIMARY KEY (VehicleModelID));
CREATE TABLE Facility (
                          ID   number(10) NOT NULL,
                          name varchar2(255) NOT NULL UNIQUE,
                          PRIMARY KEY (ID));
CREATE TABLE Freight (
                         ID                    number(10) NOT NULL,
                         OriginFacilityID      number(10) NOT NULL,
                         DestinationFacilityID number(10) NOT NULL,
                         PRIMARY KEY (ID));
CREATE TABLE Freight_Wagon (
                               FreightID number(10) NOT NULL,
                               WagonID   number NOT NULL,
                               PRIMARY KEY (FreightID,
                                            WagonID));
CREATE TABLE Gauge (
                       ID    number(10) NOT NULL,
                       width double precision NOT NULL UNIQUE,
                       name  varchar2(255) NOT NULL UNIQUE,
                       PRIMARY KEY (ID));
CREATE TABLE LineSegment (
                             ID             number(10) NOT NULL,
                             RailLineID     number(10) NOT NULL,
                             maxWeight      double precision NOT NULL,
                             length         double precision NOT NULL,
                             numberOfTracks number(10) NOT NULL,
                             speedLimit     double precision NOT NULL,
                             orderNum       number(10) NOT NULL,
                             PRIMARY KEY (ID));
CREATE TABLE Locomotive (
                            ID                number NOT NULL,
                            VehicleModelID    number(10) NOT NULL,
                            TrainOperatorID   number(10) NOT NULL,
                            InitialFacilityID number(10) NOT NULL,
                            PRIMARY KEY (ID));
CREATE TABLE Locomotive_Train (
                                  LocomotiveID number NOT NULL,
                                  TrainID      number(10) NOT NULL,
                                  PRIMARY KEY (LocomotiveID,
                                               TrainID));
CREATE TABLE LocomotiveSpecs (
                                 VehicleModelID   number(10) NOT NULL,
                                 make             varchar2(255) NOT NULL,
                                 power            double precision NOT NULL,
                                 acceleration     double precision NOT NULL,
                                 operationalSpeed double precision NOT NULL,
                                 maxSpeed         double precision NOT NULL,
                                 numberOfWheels   number(10) NOT NULL,
                                 PRIMARY KEY (VehicleModelID));
CREATE TABLE Owner (
                       ID   number(10) NOT NULL,
                       name varchar2(255) NOT NULL UNIQUE,
                       PRIMARY KEY (ID));
CREATE TABLE Path (
                      RouteID    number(10) NOT NULL,
                      seqNumber  number(10) NOT NULL,
                      FacilityID number(10) NOT NULL,
                      PRIMARY KEY (RouteID,
                                   seqNumber),
                      CONSTRAINT CHK_Path_seqNumber_GT_2
                          CHECK (seqNumber >= 2));
CREATE TABLE Planned_Train (
                               TrainID   number(10) NOT NULL,
                               startDate date NOT NULL,
                               RouteID   number(10) NOT NULL,
                               PRIMARY KEY (TrainID,
                                            startDate));
CREATE TABLE RailLine (
                          ID              number(10) NOT NULL,
                          OwnerID         number(10) NOT NULL,
                          StartFacilityID number(10) NOT NULL,
                          EndFacilityID   number(10) NOT NULL,
                          GaugeID         number(10) NOT NULL,
                          isElectrified   number(1) NOT NULL,
                          PRIMARY KEY (ID));
CREATE TABLE Route (
                       ID              number(10) NOT NULL,
                       StartFacilityID number(10) NOT NULL,
                       EndFacilityID   number(10) NOT NULL,
                       PRIMARY KEY (ID));
CREATE TABLE Siding (
                        ID            number(10) NOT NULL,
                        LineSegmentID number(10) NOT NULL,
                        position      double precision NOT NULL,
                        length        double precision NOT NULL,
                        PRIMARY KEY (ID));
CREATE TABLE Train (
                       ID              number(10) NOT NULL,
                       TrainOperatorID number(10) NOT NULL,
                       maxLength       number(10) NOT NULL,
                       PRIMARY KEY (ID));
CREATE TABLE Train_Wagon (
                             TrainID number(10) NOT NULL,
                             WagonID number NOT NULL,
                             PRIMARY KEY (TrainID,
                                          WagonID));
CREATE TABLE TrainEvent (
                            ID         number(10) NOT NULL,
                            FacilityID number(10) NOT NULL,
                            TrainID    number(10) NOT NULL,
                            eventTime  date NOT NULL,
                            eventType  varchar2(255) NOT NULL,
                            PRIMARY KEY (ID));
CREATE TABLE TrainOperator (
                               ID   number(10) NOT NULL,
                               name varchar2(255) NOT NULL UNIQUE,
                               PRIMARY KEY (ID));
CREATE TABLE VehicleModel (
                              ID        number(10) NOT NULL,
                              modelName varchar2(255) NOT NULL,
                              length    double precision NOT NULL,
                              width     double precision NOT NULL,
                              height    double precision NOT NULL,
                              tare      double precision NOT NULL,
                              yearEIS   number(10) NOT NULL,
                              PRIMARY KEY (ID));
CREATE TABLE VehicleModel_Gauge (
                                    VehicleModelID number(10) NOT NULL,
                                    GaugeID        number(10) NOT NULL,
                                    PRIMARY KEY (VehicleModelID,
                                                 GaugeID));
CREATE TABLE Wagon (
                       ID                number NOT NULL,
                       VehicleModelID    number(10) NOT NULL,
                       TrainOperatorID   number(10) NOT NULL,
                       InitialFacilityID number(10) NOT NULL,
                       PRIMARY KEY (ID));
CREATE TABLE WagonSpecs (
                            VehicleModelID number(10) NOT NULL,
                            WagonTypeID    number(10) NOT NULL,
                            volumeCapacity double precision NOT NULL,
                            payload        double precision NOT NULL,
                            PRIMARY KEY (VehicleModelID));
CREATE TABLE WagonType (
                           ID   number(10) NOT NULL,
                           type varchar2(255) NOT NULL,
                           PRIMARY KEY (ID));
ALTER TABLE LineSegment ADD CONSTRAINT FKLineSegmen950356 FOREIGN KEY (RailLineID) REFERENCES RailLine (ID);
ALTER TABLE RailLine ADD CONSTRAINT FKRailLine779458 FOREIGN KEY (OwnerID) REFERENCES Owner (ID);
ALTER TABLE RailLine ADD CONSTRAINT FKRailLine319081 FOREIGN KEY (GaugeID) REFERENCES Gauge (ID);
ALTER TABLE RailLine ADD CONSTRAINT FKRailLine763 FOREIGN KEY (StartFacilityID) REFERENCES Facility (ID);
ALTER TABLE RailLine ADD CONSTRAINT FKRailLine54241 FOREIGN KEY (EndFacilityID) REFERENCES Facility (ID);
ALTER TABLE WagonSpecs ADD CONSTRAINT FKWagonSpecs123471 FOREIGN KEY (WagonTypeID) REFERENCES WagonType (ID);
ALTER TABLE LocomotiveSpecs ADD CONSTRAINT FKLocomotive657284 FOREIGN KEY (VehicleModelID) REFERENCES VehicleModel (ID);
ALTER TABLE WagonSpecs ADD CONSTRAINT FKWagonSpecs676386 FOREIGN KEY (VehicleModelID) REFERENCES VehicleModel (ID);
ALTER TABLE VehicleModel_Gauge ADD CONSTRAINT FKVehicleMod97985 FOREIGN KEY (VehicleModelID) REFERENCES VehicleModel (ID);
ALTER TABLE VehicleModel_Gauge ADD CONSTRAINT FKVehicleMod96665 FOREIGN KEY (GaugeID) REFERENCES Gauge (ID);
ALTER TABLE Wagon ADD CONSTRAINT FKWagon418696 FOREIGN KEY (VehicleModelID) REFERENCES VehicleModel (ID);
ALTER TABLE Locomotive ADD CONSTRAINT FKLocomotive845159 FOREIGN KEY (VehicleModelID) REFERENCES VehicleModel (ID);
ALTER TABLE ElectricalType ADD CONSTRAINT FKElectrical429356 FOREIGN KEY (VehicleModelID) REFERENCES LocomotiveSpecs (VehicleModelID);
ALTER TABLE DieselType ADD CONSTRAINT FKDieselType540439 FOREIGN KEY (VehicleModelID) REFERENCES LocomotiveSpecs (VehicleModelID);
ALTER TABLE TrainEvent ADD CONSTRAINT FKTrainEvent624722 FOREIGN KEY (FacilityID) REFERENCES Facility (ID);
ALTER TABLE Train ADD CONSTRAINT FKTrain163173 FOREIGN KEY (TrainOperatorID) REFERENCES TrainOperator (ID);
ALTER TABLE Route ADD CONSTRAINT FKRoute882187 FOREIGN KEY (StartFacilityID) REFERENCES Facility (ID);
ALTER TABLE Route ADD CONSTRAINT FKRoute828709 FOREIGN KEY (EndFacilityID) REFERENCES Facility (ID);
ALTER TABLE Path ADD CONSTRAINT FKPath562795 FOREIGN KEY (FacilityID) REFERENCES Facility (ID);
ALTER TABLE Siding ADD CONSTRAINT FKSiding53285 FOREIGN KEY (LineSegmentID) REFERENCES LineSegment (ID);
ALTER TABLE Freight ADD CONSTRAINT FKFreight467435 FOREIGN KEY (OriginFacilityID) REFERENCES Facility (ID);
ALTER TABLE Freight ADD CONSTRAINT FKFreight533871 FOREIGN KEY (DestinationFacilityID) REFERENCES Facility (ID);
ALTER TABLE Locomotive_Train ADD CONSTRAINT FKLocomotive261026 FOREIGN KEY (LocomotiveID) REFERENCES Locomotive (ID);
ALTER TABLE Locomotive_Train ADD CONSTRAINT FKLocomotive809417 FOREIGN KEY (TrainID) REFERENCES Train (ID);
ALTER TABLE Freight_Wagon ADD CONSTRAINT FKFreight_Wa516226 FOREIGN KEY (FreightID) REFERENCES Freight (ID);
ALTER TABLE Freight_Wagon ADD CONSTRAINT FKFreight_Wa256803 FOREIGN KEY (WagonID) REFERENCES Wagon (ID);
ALTER TABLE Wagon ADD CONSTRAINT FKWagon893102 FOREIGN KEY (TrainOperatorID) REFERENCES TrainOperator (ID);
ALTER TABLE Locomotive ADD CONSTRAINT FKLocomotive466639 FOREIGN KEY (TrainOperatorID) REFERENCES TrainOperator (ID);
ALTER TABLE Building ADD CONSTRAINT FKBuilding925265 FOREIGN KEY (FacilityID) REFERENCES Facility (ID);
ALTER TABLE Building ADD CONSTRAINT FKBuilding466711 FOREIGN KEY (BuildingTypeID) REFERENCES BuildingType (ID);
ALTER TABLE Train_Wagon ADD CONSTRAINT FKTrain_Wago919747 FOREIGN KEY (TrainID) REFERENCES Train (ID);
ALTER TABLE Train_Wagon ADD CONSTRAINT FKTrain_Wago887851 FOREIGN KEY (WagonID) REFERENCES Wagon (ID);
ALTER TABLE Wagon ADD CONSTRAINT FKWagon691738 FOREIGN KEY (InitialFacilityID) REFERENCES Facility (ID);
ALTER TABLE Locomotive ADD CONSTRAINT FKLocomotive118202 FOREIGN KEY (InitialFacilityID) REFERENCES Facility (ID);
ALTER TABLE Planned_Train ADD CONSTRAINT FKPlanned_Tr574994 FOREIGN KEY (RouteID) REFERENCES Route (ID);
ALTER TABLE Planned_Train ADD CONSTRAINT FKPlanned_Tr449055 FOREIGN KEY (TrainID) REFERENCES Train (ID);
ALTER TABLE TrainEvent ADD CONSTRAINT FKTrainEvent928524 FOREIGN KEY (TrainID) REFERENCES Train (ID);
ALTER TABLE Path ADD CONSTRAINT FKPath33598 FOREIGN KEY (RouteID) REFERENCES Route (ID);
