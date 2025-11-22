package main.controller;

import main.domain.*;
import main.repositories.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controller for train dispatch operations
 */
public class TrainDispatchController {
    private final Connection connection;
    private final TrainSchedulerService schedulerService;
    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final FacilityRepository facilityRepository;

    public TrainDispatchController(Connection connection) {
        this.connection = connection;
        this.schedulerService = new TrainSchedulerService(connection);
        LocomotiveRepository locoRepo = new LocomotiveRepository(connection);
        WagonRepository wagonRepo = new WagonRepository(connection);
        this.trainRepository = new TrainRepository(connection, locoRepo, wagonRepo);
        FacilityRepository facilityRepo = new FacilityRepository(connection);
        this.routeRepository = new RouteRepository(connection, facilityRepo);
        this.facilityRepository = facilityRepo;
    }

    /**
     * Dispatch a train: create route, define path, and schedule
     */
    public SchedulingResult dispatchTrain(int trainId, int startFacilityId, 
                                                                 int endFacilityId, 
                                                                 LocalDateTime startDate,
                                                                 List<Integer> pathFacilityIds) throws SQLException {
        // Validate train exists
        Train train = trainRepository.getById(trainId);
        if (train == null) {
            throw new IllegalArgumentException("Train not found: " + trainId);
        }

        // Validate facilities exist
        Facility startFacility = facilityRepository.getById(startFacilityId);
        Facility endFacility = facilityRepository.getById(endFacilityId);
        if (startFacility == null || endFacility == null) {
            throw new IllegalArgumentException("Start or end facility not found");
        }

        // Check if train already has overlapping routes
        List<Route> existingRoutes = routeRepository.getByTrainId(trainId);
        if (!existingRoutes.isEmpty()) {
            // Calculate estimated end time for new route
            Route tempRoute = new Route(0, trainId, startFacility, endFacility, startDate);
            int seqNumber = 1;
            for (Integer facilityId : pathFacilityIds) {
                Facility facility = facilityRepository.getById(facilityId);
                if (facility != null) {
                    tempRoute.addPathPoint(facility, seqNumber++);
                }
            }
            
            List<TrainEvent> tempEvents = schedulerService.calculateRouteTimes(tempRoute, train);
            LocalDateTime newRouteEndTime = startDate;
            if (!tempEvents.isEmpty()) {
                newRouteEndTime = tempEvents.get(tempEvents.size() - 1).getEventTime();
            }
            
            // Check for overlaps with existing routes
            for (Route existingRoute : existingRoutes) {
                List<TrainEvent> existingEvents = schedulerService.calculateRouteTimes(existingRoute, train);
                LocalDateTime existingRouteStart = existingRoute.getStartDate();
                LocalDateTime existingRouteEnd = existingRouteStart;
                if (!existingEvents.isEmpty()) {
                    existingRouteEnd = existingEvents.get(existingEvents.size() - 1).getEventTime();
                }
                
                // Check if time intervals overlap
                // Two intervals overlap if they share any time point
                // [start1, end1] overlaps [start2, end2] if: end1 >= start2 && end2 >= start1
                boolean overlaps = !newRouteEndTime.isBefore(existingRouteStart) && 
                                 !existingRouteEnd.isBefore(startDate);
                
                if (overlaps) {
                    throw new IllegalArgumentException(
                        String.format("Train %d already has a scheduled route (Route ID: %d) that overlaps with the requested schedule.\n" +
                                    "Existing route: %s to %s (Departure: %s)\n" +
                                    "New route would be: %s to %s (Departure: %s)\n" +
                                    "Please delete the existing route or choose a different departure time.",
                                    trainId, existingRoute.getId(),
                                    existingRoute.getStartFacility().getName(),
                                    existingRoute.getEndFacility().getName(),
                                    existingRouteStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                    startFacility.getName(),
                                    endFacility.getName(),
                                    startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                }
            }
        }

        // Create route
        int routeId = routeRepository.createRoute(trainId, startFacilityId, endFacilityId, startDate);

        // Add path points manually defined by Freight Manager
        int seqNumber = 1;
        for (Integer facilityId : pathFacilityIds) {
            Facility facility = facilityRepository.getById(facilityId);
            if (facility != null) {
                routeRepository.addPathPoint(routeId, facilityId, seqNumber++);
            }
        }

        // Reload route with path points
        Route route = routeRepository.getById(routeId);

        // Schedule the route (calculate times and detect crossings)
        SchedulingResult result = schedulerService.scheduleRoute(route);

        // Commit transaction
        connection.commit();

        return result;
    }

    /**
     * Get all available trains
     */
    public List<Train> getAllTrains() throws SQLException {
        return trainRepository.getAll();
    }

    /**
     * Get all available facilities
     */
    public List<Facility> getAllFacilities() throws SQLException {
        return facilityRepository.getAll();
    }

    /**
     * Get all routes for a train
     */
    public List<Route> getRoutesByTrainId(int trainId) throws SQLException {
        return routeRepository.getByTrainId(trainId);
    }

    /**
     * Get scheduling result for a route
     */
    public SchedulingResult getScheduleForRoute(int routeId) throws SQLException {
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Route not found: " + routeId);
        }
        return schedulerService.scheduleRoute(route);
    }

    /**
     * Delete a route
     */
    public boolean deleteRoute(int routeId) throws SQLException {
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Route not found: " + routeId);
        }
        
        boolean deleted = routeRepository.deleteRoute(routeId);
        if (deleted) {
            connection.commit();
        }
        return deleted;
    }

    /**
     * Parse date time string
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) throws DateTimeParseException {
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        throw new DateTimeParseException("Unable to parse date time: " + dateTimeStr, dateTimeStr, 0);
    }
}

