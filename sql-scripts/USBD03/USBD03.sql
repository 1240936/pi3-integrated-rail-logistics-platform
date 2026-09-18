CREATE TABLE Endpoint (
  name varchar2(255) NOT NULL, 
  ID   number(10) NOT NULL, 
  PRIMARY KEY (name, 
  ID));
CREATE TABLE Endpoint_RailLine (
  EndpointName varchar2(255) NOT NULL, 
  RailLineID   number NOT NULL, 
  EndpointId   number(10) NOT NULL, 
  PRIMARY KEY (EndpointName, 
  RailLineID, 
  EndpointId));
CREATE TABLE Gauge (
  width double precision NOT NULL, 
  PRIMARY KEY (width));
CREATE TABLE LineSegment (
  ID             number(10), 
  RailLineID     number NOT NULL, 
  OwnerID        varchar2(255) NOT NULL, 
  GaugeWidth     double precision NOT NULL, 
  isElectrified  number(1) NOT NULL, 
  maxWeight      number(10) NOT NULL, 
  speedLimit     number(10), 
  length         number(10) NOT NULL, 
  numberOfTracks number(10) NOT NULL, 
  PRIMARY KEY (ID));
CREATE TABLE Locomotive (
  ID                number, 
  ModelName         varchar2(255) NOT NULL, 
  TrainOperatorName varchar2(255) NOT NULL, 
  PRIMARY KEY (ID));
CREATE TABLE LocomotiveModel (
  name           varchar2(255) NOT NULL, 
  LocomotiveType varchar2(255) NOT NULL, 
  yearEIS        number(10) NOT NULL, 
  make           varchar2(255) NOT NULL, 
  power          double precision NOT NULL, 
  acceleration   double precision, 
  maxSpeed       double precision NOT NULL, 
  length         double precision NOT NULL, 
  width          double precision NOT NULL, 
  height         double precision NOT NULL, 
  maxWeight      double precision NOT NULL, 
  fuelCapacity   double precision, 
  numberOfBogies number(10) NOT NULL, 
  PRIMARY KEY (name));
CREATE TABLE LocomotiveModel_Gauge (
  LocomotiveModelname varchar2(255) NOT NULL, 
  Gaugewidth          double precision NOT NULL, 
  PRIMARY KEY (LocomotiveModelname, 
  Gaugewidth));
CREATE TABLE LocomotiveType (
  type varchar2(255) NOT NULL, 
  PRIMARY KEY (type));
CREATE TABLE Owner (
  name varchar2(255) NOT NULL, 
  PRIMARY KEY (name));
CREATE TABLE RailLine (
  ID              number, 
  OwnerName       varchar2(255) NOT NULL, 
  startEndpoint   varchar2(255) NOT NULL, 
  startEndpointID number(10) NOT NULL, 
  endEndpoint     varchar2(255) NOT NULL, 
  endEndpointID   number(10) NOT NULL, 
  PRIMARY KEY (ID));
CREATE TABLE TrainOperator (
  name varchar2(255) NOT NULL, 
  PRIMARY KEY (name));
CREATE TABLE Wagon (
  ID              number, 
  TrainOperatorID varchar2(255) NOT NULL, 
  WagonModelID    number(10) NOT NULL, 
  PRIMARY KEY (ID));
CREATE TABLE WagonModel (
  ID             number(10), 
  GaugeWidth     double precision NOT NULL, 
  WagonType      varchar2(255) NOT NULL, 
  model          varchar2(255) NOT NULL, 
  payload        double precision NOT NULL, 
  length         double precision NOT NULL, 
  width          double precision NOT NULL, 
  height         double precision NOT NULL, 
  volumeCapacity double precision NOT NULL, 
  tare           double precision NOT NULL, 
  PRIMARY KEY (ID));
CREATE TABLE WagonType (
  type        varchar2(255) NOT NULL, 
  tankcarType varchar2(255), 
  PRIMARY KEY (type));
ALTER TABLE LineSegment ADD CONSTRAINT FKLineSegmen950356 FOREIGN KEY (RailLineID) REFERENCES RailLine (ID);
ALTER TABLE RailLine ADD CONSTRAINT FKRailLine322173 FOREIGN KEY (OwnerName) REFERENCES Owner (name);
ALTER TABLE Wagon ADD CONSTRAINT FKWagon521723 FOREIGN KEY (TrainOperatorID) REFERENCES TrainOperator (name);
ALTER TABLE Locomotive ADD CONSTRAINT FKLocomotive120042 FOREIGN KEY (TrainOperatorName) REFERENCES TrainOperator (name);
ALTER TABLE LineSegment ADD CONSTRAINT FKLineSegmen453413 FOREIGN KEY (GaugeWidth) REFERENCES Gauge (width);
ALTER TABLE LineSegment ADD CONSTRAINT FKLineSegmen577138 FOREIGN KEY (OwnerID) REFERENCES Owner (name);
ALTER TABLE Locomotive ADD CONSTRAINT FKLocomotive939774 FOREIGN KEY (ModelName) REFERENCES LocomotiveModel (name);
ALTER TABLE LocomotiveModel ADD CONSTRAINT FKLocomotive499361 FOREIGN KEY (LocomotiveType) REFERENCES LocomotiveType (type);
ALTER TABLE Endpoint_RailLine ADD CONSTRAINT FKEndpoint_R215130 FOREIGN KEY (EndpointName, EndpointId) REFERENCES Endpoint (name, ID);
ALTER TABLE Endpoint_RailLine ADD CONSTRAINT FKEndpoint_R716888 FOREIGN KEY (RailLineID) REFERENCES RailLine (ID);
ALTER TABLE Wagon ADD CONSTRAINT FKWagon749059 FOREIGN KEY (WagonModelID) REFERENCES WagonModel (ID);
ALTER TABLE LocomotiveModel_Gauge ADD CONSTRAINT FKLocomotive905740 FOREIGN KEY (LocomotiveModelname) REFERENCES LocomotiveModel (name);
ALTER TABLE LocomotiveModel_Gauge ADD CONSTRAINT FKLocomotive524742 FOREIGN KEY (Gaugewidth) REFERENCES Gauge (width);
ALTER TABLE WagonModel ADD CONSTRAINT FKWagonModel811269 FOREIGN KEY (GaugeWidth) REFERENCES Gauge (width);
ALTER TABLE RailLine ADD CONSTRAINT FKRailLine854990 FOREIGN KEY (startEndpoint, startEndpointID) REFERENCES Endpoint (name, ID);
ALTER TABLE RailLine ADD CONSTRAINT FKRailLine677830 FOREIGN KEY (endEndpoint, endEndpointID) REFERENCES Endpoint (name, ID);
ALTER TABLE WagonModel ADD CONSTRAINT FKWagonModel182639 FOREIGN KEY (WagonType) REFERENCES WagonType (type);