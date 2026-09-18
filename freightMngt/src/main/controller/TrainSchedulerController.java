package main.controller;

import main.domain.*;
import main.repositories.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for train scheduling operations.
 * Handles dispatching trains, calculating passage times, and detecting crossings.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class TrainSchedulerController {
    private final Connection connection;
    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final FacilityRepository facilityRepository;
    private final TrainSchedulerService schedulerService;
    private final AutomaticPathService pathService;

    /**
     * Constructs a TrainSchedulerController with the given database connection.
     * 
     * @param connection the database connection to use
     */
    public TrainSchedulerController(Connection connection) {
        this.connection = connection;
        FacilityRepository facilityRepo = new FacilityRepository(connection);
        LocomotiveRepository locoRepo = new LocomotiveRepository(connection);
        WagonRepository wagonRepo = new WagonRepository(connection);
        this.trainRepository = new TrainRepository(connection, locoRepo, wagonRepo);
        this.routeRepository = new RouteRepository(connection, facilityRepo);
        this.facilityRepository = facilityRepo;
        this.schedulerService = new TrainSchedulerService(connection);
        this.pathService = new AutomaticPathService(connection);
    }

    /**
     * Get trains available for dispatch.
     * Returns all trains in the system. A train can be dispatched even if it doesn't have
     * locomotives/wagons assigned yet - those can be assigned during the dispatch process
     * or the system will show an error if required.
     * 
     * @return list of trains available for dispatch
     * @throws SQLException if there is a database error
     */
    public List<Train> getTrainsAvailableForDispatch() throws SQLException {
        // Return all trains - they can all potentially be dispatched
        // The dispatch process will validate if locomotives/wagons are needed
        return trainRepository.getAll();
    }

    /**
     * Dispatch a train with manual path definition.
     * 
     * @param trainId the ID of the train to dispatch
     * @param startFacilityId the starting facility ID
     * @param endFacilityId the ending facility ID
     * @param startDate the planned departure date/time
     * @param pathFacilityIds list of intermediate facility IDs (for manual path), or null/empty for direct route
     * @return SchedulingResult with route, events, and crossings
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails
     */
    public SchedulingResult dispatchTrainWithManualPath(
            int trainId,
            int startFacilityId,
            int endFacilityId,
            LocalDateTime startDate,
            List<Integer> pathFacilityIds) throws SQLException {
        
        // Validate train exists
        Train train = trainRepository.getById(trainId);
        if (train == null) {
            throw new IllegalArgumentException("Train not found: " + trainId);
        }
        
        // Create route
        int routeId = routeRepository.createRoute(trainId, startFacilityId, endFacilityId, startDate);
        
        // Add manual path points if provided
        if (pathFacilityIds != null && !pathFacilityIds.isEmpty()) {
            int seqNumber = 2; // Start from 2 (1 is start facility)
            for (Integer facilityId : pathFacilityIds) {
                routeRepository.addPathPoint(routeId, facilityId, seqNumber++);
            }
        }
        
        // Reload route with path points
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            throw new SQLException("Failed to create route");
        }
        
        // Schedule the route (calculate times and detect crossings)
        SchedulingResult result = schedulerService.scheduleRoute(route);
        
        // Commit transaction
        connection.commit();
        
        return result;
    }

    /**
     * Dispatch a train with automatic path calculation.
     * 
     * @param trainId the ID of the train to dispatch
     * @param startFacilityId the starting facility ID
     * @param endFacilityId the ending facility ID
     * @param startDate the planned departure date/time
     * @return SchedulingResult with route, events, and crossings
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails or path cannot be found
     */
    public SchedulingResult dispatchTrainWithAutomaticPath(
            int trainId,
            int startFacilityId,
            int endFacilityId,
            LocalDateTime startDate) throws SQLException {
        
        // Validate train exists
        Train train = trainRepository.getById(trainId);
        if (train == null) {
            throw new IllegalArgumentException("Train not found: " + trainId);
        }
        
        // Calculate automatic path
        List<Integer> pathFacilityIds = pathService.calculateShortestPath(startFacilityId, endFacilityId);
        
        if (pathFacilityIds == null || pathFacilityIds.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("No path found from facility %d to facility %d", 
                    startFacilityId, endFacilityId));
        }
        
        // Remove start and end facilities from path (they're route attributes)
        pathFacilityIds.removeIf(id -> id == startFacilityId || id == endFacilityId);
        
        // Dispatch with calculated path
        return dispatchTrainWithManualPath(trainId, startFacilityId, endFacilityId, 
                                          startDate, pathFacilityIds);
    }

    /**
     * Get passage times (train events) for a route.
     * 
     * @param routeId the route ID
     * @return list of train events representing passage times
     * @throws SQLException if there is a database error
     */
    public List<TrainEvent> getPassageTimes(int routeId) throws SQLException {
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            return new ArrayList<>();
        }
        
        Train train = trainRepository.getTrainForRoute(routeId);
        if (train == null) {
            return new ArrayList<>();
        }
        
        return schedulerService.calculateRouteTimes(route, train);
    }

    /**
     * Get crossings for a route.
     * 
     * @param routeId the route ID
     * @return list of crossing operations involving this route
     * @throws SQLException if there is a database error
     */
    public List<CrossingOperation> getCrossings(int routeId) throws SQLException {
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            return new ArrayList<>();
        }
        
        List<Route> allRoutes = routeRepository.getAll();
        List<CrossingOperation> allCrossings = schedulerService.detectCrossings(allRoutes);
        
        // Filter crossings involving this route
        List<CrossingOperation> routeCrossings = new ArrayList<>();
        for (CrossingOperation crossing : allCrossings) {
            if (crossing.getRoute1Id() == routeId || crossing.getRoute2Id() == routeId) {
                routeCrossings.add(crossing);
            }
        }
        
        return routeCrossings;
    }

    /**
     * Get all scheduled routes with their passage times.
     * 
     * @return list of routes with scheduling information
     * @throws SQLException if there is a database error
     */
    public List<Route> getAllScheduledRoutes() throws SQLException {
        return routeRepository.getAll();
    }
}

