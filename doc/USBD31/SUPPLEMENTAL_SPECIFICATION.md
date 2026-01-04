# Supplemental Specification

## Introduction

This document describes constraints, business rules, and validation requirements that cannot be directly modeled in the relational data model but must be enforced through application logic, triggers, or stored procedures.

## Constraint Categories

### 1. Temporal Constraints

#### 1.1 Train Movement Temporal Integrity
- **Constraint**: Departure time must be after arrival time
- **Location**: USBD42 - RegisterTrainMovement
- **Enforcement**: Application logic in stored procedure
- **Description**: When registering a train movement with both arrival and departure times, the departure time must be chronologically after the arrival time. This temporal constraint cannot be expressed as a simple CHECK constraint because it involves two different DATE values in potentially different rows.

#### 1.2 Train Scheduling Conflicts
- **Constraint**: A locomotive cannot be assigned to multiple trains with overlapping schedules
- **Location**: USBD39 - AssociateLocomotiveWithPlannedTrain
- **Enforcement**: Application logic in stored procedure
- **Description**: Before associating a locomotive with a planned train, the system must verify that the locomotive is not already assigned to another train. This requires checking the Planned_Train table for overlapping schedules, which cannot be enforced through referential integrity alone.

### 2. Case and Space Insensitive Uniqueness Constraints

#### 2.1 Facility Name Uniqueness
- **Constraint**: Facility names must be unique case-insensitively and space-insensitively
- **Location**: USBD35 - RegisterFacility
- **Enforcement**: Application logic in stored procedure
- **Description**: "Test Space", "TestSpace", and "test space" should be considered duplicates. This normalization requirement (removing all spaces and converting to uppercase) cannot be enforced through a standard UNIQUE constraint without a functional index or trigger.

#### 2.2 Vehicle Model Name Uniqueness
- **Constraint**: Vehicle model names must be unique case-insensitively and space-insensitively
- **Location**: USBD43 - ADD_ELECTRIC_LOCOMOTIVE_MODEL
- **Enforcement**: Application logic in stored procedure
- **Description**: Model names are compared after trimming spaces and converting to uppercase, ensuring variations in case and spacing are treated as the same name.

#### 2.3 Train Operator Name Matching
- **Constraint**: Train operator names must be matched case-insensitively and space-insensitively
- **Location**: USBD37 - RegisterWagon
- **Enforcement**: Application logic in stored procedure
- **Description**: When registering a wagon, the train operator name lookup uses UPPER(TRIM()) for matching, allowing flexible input while maintaining referential integrity.

#### 2.4 Building Type Matching
- **Constraint**: Building types must be matched case-insensitively and space-insensitively
- **Location**: USBD36 - AddBuildingToFacility
- **Enforcement**: Application logic in stored procedure
- **Description**: Building type names are compared using UPPER(TRIM()) to ensure case and space variations are handled correctly.

### 3. Cardinality and Existence Constraints

#### 3.1 Minimum Segment Requirement for Rail Lines
- **Constraint**: A rail line must have at least one segment
- **Location**: USBD45 - AddNewLine
- **Enforcement**: Application logic in stored procedure
- **Description**: When creating a new rail line, at least one line segment must be created simultaneously. This constraint ensures data integrity but cannot be enforced through a simple NOT NULL constraint since segments are in a separate table.

#### 3.2 Minimum Gauge Requirement for Vehicle Models
- **Constraint**: A vehicle model must have at least one registered gauge before wagons can be registered
- **Location**: USBD37 - RegisterWagon
- **Enforcement**: Application logic in stored procedure
- **Description**: Before registering a wagon with a vehicle model, the system must verify that the vehicle model has at least one entry in the VehicleModel_Gauge table. This is a business rule that cannot be modeled as a simple foreign key constraint.

### 4. Cross-Table Validation Rules

#### 4.1 Wagon Assignment Uniqueness
- **Constraint**: A wagon cannot be assigned to multiple trains simultaneously
- **Location**: USBD34 - AssociateFreightWithTrain
- **Enforcement**: Application logic in stored procedure
- **Description**: When associating a freight (and its wagons) with a train, the system must check that wagons are not already assigned to a different train. This requires querying the Assigned_Wagon and Planned_Train tables to detect conflicts, which cannot be enforced through referential integrity alone.

#### 4.2 Facility Difference Constraint
- **Constraint**: Start and End facilities for a rail line must be different
- **Location**: USBD45 - AddNewLine
- **Enforcement**: Application logic in stored procedure
- **Description**: A rail line cannot have the same facility as both start and end points. This is a business rule that involves two columns of the same table but cannot be enforced through a CHECK constraint that compares two columns in a straightforward manner.

#### 4.3 Siding Position Validation
- **Constraint**: Siding position must be within the segment length boundaries
- **Location**: USBD44 - AddSegmentToLine
- **Enforcement**: Application logic in stored procedure
- **Description**: When adding a segment with a siding, the siding position must be between 0 and the segment length. This requires comparing values from the Siding table with the parent LineSegment table, which cannot be enforced through a simple CHECK constraint.

#### 4.4 Siding Parameter Consistency
- **Constraint**: If siding position is provided, siding length must also be provided
- **Location**: USBD44 - AddSegmentToLine
- **Enforcement**: Application logic in stored procedure
- **Description**: Siding information must be complete - if a position is specified, the length must also be specified. This conditional requirement cannot be enforced through NULL constraints alone.

### 5. Atomic Transaction Requirements

#### 5.1 Freight-Wagon Association Atomicity
- **Constraint**: All wagons in a freight must be associated atomically or not at all
- **Location**: USBD34 - AssociateFreightWithTrain
- **Enforcement**: Transaction management in stored procedure
- **Description**: When associating a freight with a train, all wagons must be processed successfully. If any wagon fails (e.g., already assigned to another train), the transaction should be atomic, but individual wagon failures within a freight should be handled gracefully (skip failed wagons, continue with others).

#### 5.2 Multi-Table Insert Atomicity
- **Constraint**: Electric locomotive model registration must insert into multiple tables atomically
- **Location**: USBD43 - ADD_ELECTRIC_LOCOMOTIVE_MODEL
- **Enforcement**: Transaction management in stored procedure
- **Description**: Creating an electric locomotive model requires inserting into VehicleModel, LocomotiveSpecs, ElectricalType, and VehicleModel_Gauge tables. All inserts must succeed or all must rollback.

#### 5.3 Line and Segment Creation Atomicity
- **Constraint**: Rail line and its first segment must be created atomically
- **Location**: USBD45 - AddNewLine
- **Enforcement**: Transaction management in stored procedure
- **Description**: When creating a new rail line, the line and its mandatory first segment must be created in a single atomic transaction.

### 6. Business Logic Constraints

#### 6.1 Freight Removal Cascading
- **Constraint**: Removing a freight from a train must automatically detach all associated wagons
- **Location**: USBD41 - RemoveFreightFromTrain
- **Enforcement**: Application logic in stored procedure
- **Description**: When removing a freight assignment, all wagons linked to that freight must be removed from the train. This is a business rule that defines the behavior of the removal operation, not a data integrity constraint.

#### 6.2 Planned Train Selection Rule
- **Constraint**: If multiple planned trains exist for a route, select the earliest one
- **Location**: USBD34 - AssociateFreightWithTrain
- **Enforcement**: Application logic in stored procedure
- **Description**: When associating freight with a route that has multiple planned trains, the system must select the train with the earliest start date. This is a business rule for train selection logic.

#### 6.3 Wagon Parking Initialization
- **Constraint**: Newly registered wagons must be automatically parked at their initial facility
- **Location**: USBD37 - RegisterWagon
- **Enforcement**: Application logic in stored procedure
- **Description**: When registering a wagon, it must automatically be inserted into the Parked_Wagon table at the specified initial facility. This is a business rule that ensures data consistency but requires application logic to enforce.

#### 6.4 Locomotive Parking Status Update
- **Constraint**: When associating a locomotive with a train, it must be removed from parked status
- **Location**: USBD39 - AssociateLocomotiveWithPlannedTrain
- **Enforcement**: Application logic in stored procedure
- **Description**: When moving a locomotive from parked to assigned status, it must be deleted from the Parked_Locomotive table. This state transition cannot be enforced through referential integrity alone.

### 7. Data Type and Format Validation

#### 7.1 Gauge ID Parsing
- **Constraint**: Comma-separated gauge IDs must be parsed and validated
- **Location**: USBD43 - ADD_ELECTRIC_LOCOMOTIVE_MODEL
- **Enforcement**: Application logic in stored procedure
- **Description**: The procedure accepts gauge IDs as a comma-separated string (e.g., "1,2,3") and must parse, validate, and insert each gauge individually. This parsing logic cannot be expressed in the data model.

#### 7.2 String Trimming and Normalization
- **Constraint**: String inputs must be trimmed and normalized before storage
- **Location**: Multiple USBD implementations
- **Enforcement**: Application logic in stored procedures
- **Description**: Facility names, model names, and other string fields are trimmed (leading/trailing spaces removed) before storage, but the original trimmed value is stored. This normalization cannot be enforced through the data model alone.

### 8. State Transition Rules

#### 8.1 Freight Assignment State Management
- **Constraint**: Wagons must be moved from Unassigned_Freight to Assigned_Freight upon train assignment
- **Location**: USBD34 - AssociateFreightWithTrain
- **Enforcement**: Application logic in stored procedure
- **Description**: When associating freight with a train, wagons must be removed from Unassigned_Freight and added to Assigned_Freight. This state transition ensures data consistency across multiple tables but requires application logic.

#### 8.2 Duplicate Association Handling
- **Constraint**: Attempting to associate already-associated entities should be handled gracefully
- **Location**: USBD34, USBD39
- **Enforcement**: Application logic in stored procedures
- **Description**: When attempting to associate entities that are already associated, the system should detect and handle this case appropriately (either skip silently or raise an error), depending on the business rule. This behavior cannot be modeled in the data model.

## Summary

These constraints require enforcement through:
1. **Stored Procedures/Functions**: Primary mechanism for complex validation
2. **Application Logic**: Business rules that require conditional processing
3. **Transaction Management**: Ensuring atomicity across multiple table operations
4. **Trigger Logic**: May be used for additional enforcement (though not implemented in current USBD specifications)

## Notes

- All constraints listed above are currently enforced through PL/SQL stored procedures and functions
- The database schema maintains referential integrity through foreign keys where applicable
- Complex business rules are implemented in application logic rather than database constraints to maintain flexibility and clarity
- Transaction boundaries ensure data consistency for multi-step operations

