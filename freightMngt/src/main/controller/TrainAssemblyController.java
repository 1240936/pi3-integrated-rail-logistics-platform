package main.controller;

import main.domain.LocomotiveForAssembly;
import main.domain.Route;
import main.domain.Train;
import main.domain.WagonForAssembly;
import main.repositories.LocomotiveRepository;
import main.repositories.RouteRepository;
import main.repositories.TrainRepository;
import main.repositories.WagonRepository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.OracleTypes;

/**
 * Controller for train assembly operations.
 * Handles assembling trains by selecting and assigning locomotives and wagons to routes.
 */
public class TrainAssemblyController {
    private final Connection connection;
    private final RouteRepository routeRepository;
    private final TrainRepository trainRepository;
    private final LocomotiveRepository locomotiveRepository;
    private final WagonRepository wagonRepository;

    public TrainAssemblyController(Connection connection,
                                   RouteRepository routeRepository,
                                   TrainRepository trainRepository,
                                   LocomotiveRepository locomotiveRepository,
                                   WagonRepository wagonRepository) {
        this.connection = connection;
        this.routeRepository = routeRepository;
        this.trainRepository = trainRepository;
        this.locomotiveRepository = locomotiveRepository;
        this.wagonRepository = wagonRepository;
    }

    /**
     * Get locomotives available for assembly for a specific route at a specific date/time.
     * The list distinguishes between locomotives in-transit and parked,
     * and orders parked locomotives by distance from the route start (descending).
     * 
     * @param routeId the route ID
     * @param requestedStartDate the requested start date/time for the planned train
     * @return list of LocomotiveForAssembly objects
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if the route does not exist
     */
    public List<LocomotiveForAssembly> getAvailableLocomotives(int routeId, LocalDateTime requestedStartDate) throws SQLException {
        // Validate route exists
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Route with ID " + routeId + " does not exist.");
        }

        Timestamp timestamp = Timestamp.valueOf(requestedStartDate);
        return locomotiveRepository.getForAssembly(routeId, timestamp);
    }

    /**
     * Get wagons available for assembly for a specific route at a specific date/time.
     * The list distinguishes between wagons in-transit and parked,
     * and orders parked wagons by distance from the route start (descending).
     * 
     * @param routeId the route ID
     * @param requestedStartDate the requested start date/time for the planned train
     * @return list of WagonForAssembly objects
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if the route does not exist
     */
    public List<WagonForAssembly> getAvailableWagons(int routeId, LocalDateTime requestedStartDate) throws SQLException {
        // Validate route exists
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Route with ID " + routeId + " does not exist.");
        }

        Timestamp timestamp = Timestamp.valueOf(requestedStartDate);
        return wagonRepository.getForAssembly(routeId, timestamp);
    }

    /**
     * Check if a date/time conflicts with an existing planned train for a route.
     * 
     * @param routeId the route ID
     * @param startDate the start date/time to check
     * @return true if conflict exists, false otherwise
     * @throws SQLException if there is a database error
     */
    public boolean checkDateConflict(int routeId, LocalDateTime startDate) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL CHECK_PLANNED_TRAIN_DATE_CONFLICT(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, routeId);
            stmt.setTimestamp(3, Timestamp.valueOf(startDate));
            stmt.execute();
            
            int result = stmt.getInt(1);
            return result > 0;
        }
    }

    /**
     * Assign a locomotive to a route (planned train).
     * Validates that the locomotive is available at the requested time before assigning.
     * Uses PL/SQL function ASSIGN_LOCOMOTIVE_TO_ROUTE (USLP09).
     * 
     * @param locomotiveId the locomotive ID to assign
     * @param routeId the route ID
     * @param requestedStartDate the requested start date/time for the planned train
     * @return the train ID that the locomotive was assigned to
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails or locomotive is not available at the requested time
     */
    public int assignLocomotiveToRoute(int locomotiveId, int routeId, LocalDateTime requestedStartDate) throws SQLException {
        // Check if locomotive is available at the requested time
        List<LocomotiveForAssembly> available = getAvailableLocomotives(routeId, requestedStartDate);
        LocomotiveForAssembly selected = available.stream()
            .filter(l -> l.getLocomotive().getId() == locomotiveId)
            .findFirst()
            .orElse(null);
        
        if (selected == null) {
            throw new IllegalArgumentException("Locomotive " + locomotiveId + " is not available at the requested time.");
        }
        
        if (selected.isInTransit()) {
            throw new IllegalArgumentException("Locomotive " + locomotiveId + " is IN-TRANSIT at the requested time and cannot be assigned.");
        }
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL ASSIGN_LOCOMOTIVE_TO_ROUTE(?, ?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, locomotiveId);
            stmt.setInt(3, routeId);
            stmt.setTimestamp(4, Timestamp.valueOf(requestedStartDate));
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Assign a wagon to a route (planned train).
     * Validates that the wagon is available at the requested time before assigning.
     * Uses PL/SQL function ASSIGN_WAGON_TO_ROUTE (USLP09).
     * 
     * @param wagonId the wagon ID to assign
     * @param routeId the route ID
     * @param requestedStartDate the requested start date/time for the planned train
     * @return the train ID that the wagon was assigned to
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails or wagon is not available at the requested time
     */
    public int assignWagonToRoute(int wagonId, int routeId, LocalDateTime requestedStartDate) throws SQLException {
        // Check if wagon is available at the requested time
        List<WagonForAssembly> available = getAvailableWagons(routeId, requestedStartDate);
        WagonForAssembly selected = available.stream()
            .filter(w -> w.getWagon().getId() == wagonId)
            .findFirst()
            .orElse(null);
        
        if (selected == null) {
            throw new IllegalArgumentException("Wagon " + wagonId + " is not available at the requested time.");
        }
        
        if (selected.isInTransit()) {
            throw new IllegalArgumentException("Wagon " + wagonId + " is IN-TRANSIT at the requested time and cannot be assigned.");
        }
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL ASSIGN_WAGON_TO_ROUTE(?, ?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, wagonId);
            stmt.setInt(3, routeId);
            stmt.setTimestamp(4, Timestamp.valueOf(requestedStartDate));
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Remove a locomotive from a route (planned train).
     * Uses PL/SQL function REMOVE_LOCOMOTIVE_FROM_ROUTE (USLP09).
     * 
     * @param locomotiveId the locomotive ID to remove
     * @param routeId the route ID
     * @return the train ID that the locomotive was removed from
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails
     */
    public int removeLocomotiveFromRoute(int locomotiveId, int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL REMOVE_LOCOMOTIVE_FROM_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, locomotiveId);
            stmt.setInt(3, routeId);
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Remove a wagon from a route (planned train).
     * Uses PL/SQL function REMOVE_WAGON_FROM_ROUTE (USLP09).
     * 
     * @param wagonId the wagon ID to remove
     * @param routeId the route ID
     * @return the train ID that the wagon was removed from
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails
     */
    public int removeWagonFromRoute(int wagonId, int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL REMOVE_WAGON_FROM_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, wagonId);
            stmt.setInt(3, routeId);
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get the train assigned to a route with all its locomotives and wagons.
     * 
     * @param routeId the route ID
     * @return the Train object with locomotives and wagons, or null if no train is assigned
     * @throws SQLException if there is a database error
     */
    public Train getTrainForRoute(int routeId) throws SQLException {
        return trainRepository.getTrainForRoute(routeId);
    }

    /**
     * Get all available routes.
     * 
     * @return list of all Route objects in the system
     * @throws SQLException if there is a database error
     */
    public List<Route> getAllRoutes() throws SQLException {
        return routeRepository.getAll();
    }

    /**
     * Ensure Planned_Train exists for a route with the specified start date/time.
     * Creates it if it doesn't exist, or validates the date matches if it does.
     * 
     * @param routeId the route ID
     * @param startDate the start date/time for the planned train
     * @return the train ID for the planned train
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if date conflicts with existing planned train
     */
    public int ensurePlannedTrain(int routeId, LocalDateTime startDate) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL ENSURE_PLANNED_TRAIN_FOR_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, routeId);
            stmt.setTimestamp(3, Timestamp.valueOf(startDate));
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get all planned trains with route information.
     * 
     * @return list of planned train information (route ID, train ID, start date, facilities)
     * @throws SQLException if there is a database error
     */
    public List<PlannedTrainInfo> getAllPlannedTrains() throws SQLException {
        List<PlannedTrainInfo> plannedTrains = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_PLANNED_TRAINS()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (java.sql.ResultSet rs = (java.sql.ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    plannedTrains.add(new PlannedTrainInfo(
                        rs.getInt("RouteID"),
                        rs.getInt("TrainID"),
                        rs.getTimestamp("startDate").toLocalDateTime(),
                        rs.getInt("StartFacilityID"),
                        rs.getString("StartFacilityName"),
                        rs.getInt("EndFacilityID"),
                        rs.getString("EndFacilityName")
                    ));
                }
            }
        }
        return plannedTrains;
    }

    /**
     * Delete a planned train for a route at a specific date/time.
     * Moves all assigned locomotives and wagons back to parked status.
     * 
     * @param routeId the route ID
     * @param startDate the start date/time of the planned train to delete
     * @return true if successful, false if planned train not found
     * @throws SQLException if there is a database error
     */
    public boolean deletePlannedTrain(int routeId, LocalDateTime startDate) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL DELETE_PLANNED_TRAIN(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, routeId);
            stmt.setTimestamp(3, Timestamp.valueOf(startDate));
            stmt.execute();
            
            int result = stmt.getInt(1);
            return result > 0;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Simple data class for planned train information
     */
    public static class PlannedTrainInfo {
        private final int routeId;
        private final int trainId;
        private final LocalDateTime startDate;
        private final int startFacilityId;
        private final String startFacilityName;
        private final int endFacilityId;
        private final String endFacilityName;

        public PlannedTrainInfo(int routeId, int trainId, LocalDateTime startDate,
                               int startFacilityId, String startFacilityName,
                               int endFacilityId, String endFacilityName) {
            this.routeId = routeId;
            this.trainId = trainId;
            this.startDate = startDate;
            this.startFacilityId = startFacilityId;
            this.startFacilityName = startFacilityName;
            this.endFacilityId = endFacilityId;
            this.endFacilityName = endFacilityName;
        }

        public int getRouteId() { return routeId; }
        public int getTrainId() { return trainId; }
        public LocalDateTime getStartDate() { return startDate; }
        public int getStartFacilityId() { return startFacilityId; }
        public String getStartFacilityName() { return startFacilityName; }
        public int getEndFacilityId() { return endFacilityId; }
        public String getEndFacilityName() { return endFacilityName; }
    }
}

