package main.controller;

import main.domain.*;
import main.repositories.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
    private final FreightRepository freightRepository;

    public TrainDispatchController(Connection connection) {
        this.connection = connection;
        this.schedulerService = new TrainSchedulerService(connection);
        LocomotiveRepository locoRepo = new LocomotiveRepository(connection);
        WagonRepository wagonRepo = new WagonRepository(connection);
        this.trainRepository = new TrainRepository(connection, locoRepo, wagonRepo);
        FacilityRepository facilityRepo = new FacilityRepository(connection);
        this.routeRepository = new RouteRepository(connection, facilityRepo);
        this.facilityRepository = facilityRepo;
        this.freightRepository = new FreightRepository(connection, facilityRepo);
    }

    /**
     * Dispatch a train: create route, define path, assign freight, and schedule
     */
    public SchedulingResult dispatchTrain(int trainId, int startFacilityId, 
                                                                 int endFacilityId, 
                                                                 LocalDateTime startDate,
                                                                 List<Integer> pathFacilityIds,
                                                                 List<Integer> freightIds) throws SQLException {
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

        // Validate and assign freight to route
        if (freightIds != null && !freightIds.isEmpty()) {
            validateAndAssignFreight(route, startFacilityId, endFacilityId, pathFacilityIds, freightIds);
        }

        // Schedule the route (calculate times and detect crossings)
        SchedulingResult result = schedulerService.scheduleRoute(route);

        // Commit transaction
        connection.commit();

        return result;
    }

    /**
     * Validate that freight can be assigned to route and assign it
     * Route must pass through both origin and destination facilities of each freight
     */
    private void validateAndAssignFreight(Route route, int startFacilityId, int endFacilityId,
                                         List<Integer> pathFacilityIds, List<Integer> freightIds) throws SQLException {
        // Build ordered list of all facilities in the route
        List<Integer> routeFacilities = new ArrayList<>();
        routeFacilities.add(startFacilityId);
        routeFacilities.addAll(pathFacilityIds);
        routeFacilities.add(endFacilityId);

        for (Integer freightId : freightIds) {
            Freight freight = freightRepository.getById(freightId);
            if (freight == null) {
                throw new IllegalArgumentException("Freight not found: " + freightId);
            }

            // Check if freight is already assigned
            if (freight.getRouteId() != 0 && freight.getRouteId() != route.getId()) {
                throw new IllegalArgumentException(
                    String.format("Freight %d is already assigned to route %d", freightId, freight.getRouteId()));
            }

            int originId = freight.getOriginFacility().getId();
            int destinationId = freight.getDestinationFacility().getId();

            // Check if origin is on the route
            int originIndex = routeFacilities.indexOf(originId);
            if (originIndex == -1) {
                throw new IllegalArgumentException(
                    String.format("Freight %d origin facility (%s, ID: %d) is not on the route. " +
                                "The route must pass through the freight's origin facility.",
                        freightId, freight.getOriginFacility().getName(), originId));
            }

            // Check if destination is on the route
            int destinationIndex = routeFacilities.indexOf(destinationId);
            if (destinationIndex == -1) {
                throw new IllegalArgumentException(
                    String.format("Freight %d destination facility (%s, ID: %d) is not on the route. " +
                                "The route must pass through the freight's destination facility.",
                        freightId, freight.getDestinationFacility().getName(), destinationId));
            }

            // Check that destination comes after origin in the route order
            if (destinationIndex <= originIndex) {
                throw new IllegalArgumentException(
                    String.format("Freight %d destination facility (%s, ID: %d) must come after origin facility " +
                                "(%s, ID: %d) in the route. Origin is at position %d, destination is at position %d.",
                        freightId,
                        freight.getDestinationFacility().getName(), destinationId,
                        freight.getOriginFacility().getName(), originId,
                        originIndex + 1, destinationIndex + 1));
            }

            // Assign freight to route
            freightRepository.assignFreightToRoute(freightId, route.getId());
        }
    }

    /**
     * Get all unassigned freight (freight not yet assigned to any route)
     */
    public List<Freight> getUnassignedFreight() throws SQLException {
        return freightRepository.getUnassignedFreight();
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

