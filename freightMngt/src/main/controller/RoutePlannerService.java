package main.controller;

import main.domain.*;
import main.repositories.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for route planning and management.
 * Allows Freight Managers to define routes (simple or complex) and assign
 * pending freights to accomplish transportation tasks.
 * 
 * Features:
 * <ul>
 *   <li>Get list of pending (unassigned) freights</li>
 *   <li>Create simple routes (direct from origin to destination)</li>
 *   <li>Create complex routes (with intermediate stops)</li>
 *   <li>Assign freights to routes</li>
 *   <li>Generate route plans showing sequence of stations and cargo operations</li>
 * </ul>
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class RoutePlannerService {
    private final Connection connection;
    private final FreightRepository freightRepository;
    private final RouteRepository routeRepository;
    private final FacilityRepository facilityRepository;

    /**
     * Constructs a RoutePlannerService with the given database connection.
     * 
     * @param connection the database connection to use for repository operations
     */
    public RoutePlannerService(Connection connection) {
        this.connection = connection;
        this.facilityRepository = new FacilityRepository(connection);
        this.freightRepository = new FreightRepository(connection, facilityRepository);
        this.routeRepository = new RouteRepository(connection, facilityRepository);
    }

    /**
     * Gets all pending (unassigned) freights.
     * Pending freights are those that have not been assigned to any route yet.
     * 
     * @return list of unassigned freights
     * @throws SQLException if there is a database error
     */
    public List<Freight> getPendingFreights() throws SQLException {
        return freightRepository.getUnassignedFreight();
    }

    /**
     * Creates a simple route (direct from start to end facility, no intermediate stops).
     * 
     * @param trainId the ID of the train assigned to this route
     * @param startFacilityId the ID of the starting facility
     * @param endFacilityId the ID of the ending facility
     * @param startDate the departure date and time from the start facility
     * @return the ID of the newly created route
     * @throws SQLException if there is a database error
     */
    public int createSimpleRoute(int trainId, int startFacilityId, int endFacilityId, 
                                  LocalDateTime startDate) throws SQLException {
        return routeRepository.createRoute(trainId, startFacilityId, endFacilityId, startDate);
    }

    /**
     * Creates a complex route (with intermediate stops).
     * The path is defined by providing a list of intermediate facilities in order.
     * 
     * @param trainId the ID of the train assigned to this route
     * @param startFacilityId the ID of the starting facility
     * @param endFacilityId the ID of the ending facility
     * @param intermediateFacilityIds list of intermediate facility IDs in order
     * @param startDate the departure date and time from the start facility
     * @return the ID of the newly created route
     * @throws SQLException if there is a database error
     */
    public int createComplexRoute(int trainId, int startFacilityId, int endFacilityId,
                                   List<Integer> intermediateFacilityIds, 
                                   LocalDateTime startDate) throws SQLException {
        // Create the base route
        int routeId = routeRepository.createRoute(trainId, startFacilityId, endFacilityId, startDate);
        
        // Add intermediate path points
        if (intermediateFacilityIds != null) {
            int sequenceNumber = 1;
            for (Integer facilityId : intermediateFacilityIds) {
                routeRepository.addPathPoint(routeId, facilityId, sequenceNumber);
                sequenceNumber++;
            }
        }
        
        return routeId;
    }

    /**
     * Assigns a freight to a route.
     * 
     * @param freightId the ID of the freight to assign
     * @param routeId the ID of the route to assign the freight to
     * @throws SQLException if there is a database error
     */
    public void assignFreightToRoute(int freightId, int routeId) throws SQLException {
        freightRepository.assignFreightToRoute(freightId, routeId);
    }

    /**
     * Assigns multiple freights to a route.
     * 
     * @param freightIds list of freight IDs to assign
     * @param routeId the ID of the route to assign the freights to
     * @throws SQLException if there is a database error
     */
    public void assignFreightsToRoute(List<Integer> freightIds, int routeId) throws SQLException {
        for (Integer freightId : freightIds) {
            freightRepository.assignFreightToRoute(freightId, routeId);
        }
    }

    /**
     * Generates a route plan that shows the sequence of stations and cargo operations
     * (load/unload) for a given route.
     * 
     * The route plan identifies:
     * <ul>
     *   <li>All facilities visited (start, intermediate, end)</li>
     *   <li>Which freight items should be loaded at each facility</li>
     *   <li>Which freight items should be unloaded at each facility</li>
     * </ul>
     * 
     * @param routeId the ID of the route
     * @return the route plan with station sequence and cargo operations
     * @throws SQLException if there is a database error
     */
    public RoutePlan generateRoutePlan(int routeId) throws SQLException {
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            return null;
        }

        // Get all freights assigned to this route
        // IMPORTANT: Use startDate to ensure we get freights for the correct journey
        List<Freight> routeFreights;
        if (route.getStartDate() != null) {
            routeFreights = freightRepository.getByRouteId(routeId, route.getStartDate());
        } else {
            // Fallback if startDate is not available
            routeFreights = freightRepository.getByRouteId(routeId);
        }

        // Build the route plan
        RoutePlan routePlan = new RoutePlan(route);

        // Get all facilities in sequence order
        List<Facility> facilitiesInOrder = getFacilitiesInOrder(route);

        // Create station operations for each facility
        // IMPORTANT: Show ALL facilities in the path, even if no cargo operations occur
        int sequenceNumber = 1;
        for (Facility facility : facilitiesInOrder) {
            StationCargoOperation operation = new StationCargoOperation(facility, sequenceNumber);

            // Find freights to load at this facility (freights that originate here)
            // Only include freights that are actually assigned to this route
            for (Freight freight : routeFreights) {
                if (freight.getOriginFacility().getId() == facility.getId()) {
                    operation.addFreightToLoad(freight);
                }
            }

            // Find freights to unload at this facility (freights that end here)
            // Only include freights that are actually assigned to this route
            for (Freight freight : routeFreights) {
                if (freight.getDestinationFacility().getId() == facility.getId()) {
                    operation.addFreightToUnload(freight);
                }
            }

            // ALWAYS add the station operation, even if no cargo operations occur
            // This ensures all stations in the path are displayed
            routePlan.addStationOperation(operation);
            sequenceNumber++;
        }

        return routePlan;
    }

    /**
     * Gets all facilities in the order they are visited in the route.
     * 
     * @param route the route
     * @return list of facilities in visit order (start, intermediates, end)
     */
    private List<Facility> getFacilitiesInOrder(Route route) {
        List<Facility> facilities = new ArrayList<>();
        
        // Add start facility
        facilities.add(route.getStartFacility());
        
        // Add intermediate facilities in sequence order
        List<Route.RoutePathPoint> pathPoints = route.getPath();
        for (Route.RoutePathPoint point : pathPoints) {
            facilities.add(point.getFacility());
        }
        
        // Add end facility (only if different from start)
        if (route.getEndFacility().getId() != route.getStartFacility().getId()) {
            facilities.add(route.getEndFacility());
        }
        
        return facilities;
    }

    /**
     * Gets a formatted string representation of the route plan showing the sequence
     * of stations and cargo operations (load/unload).
     * 
     * This satisfies the acceptance criteria: "A list with the sequence of stations
     * and the cargos to be loaded/unloaded should be presented"
     * 
     * @param routeId the ID of the route
     * @return formatted string with station sequence and cargo operations
     * @throws SQLException if there is a database error
     */
    public String presentRoutePlan(int routeId) throws SQLException {
        RoutePlan plan = generateRoutePlan(routeId);
        if (plan == null) {
            return "Route not found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Route Plan for Route ID: ").append(routeId).append(" ===\n");
        sb.append("Route Type: ").append(plan.isSimple() ? "Simple (Direct)" : "Complex (With Intermediate Stops)").append("\n");
        sb.append("Total Stations: ").append(plan.getStationOperations().size()).append("\n");
        sb.append("Total Cargos (Freight): ").append(plan.getAllFreight().size()).append("\n\n");

        sb.append("LIST OF STATIONS (in sequence order):\n");
        sb.append("=====================================\n");
        int stationNumber = 1;
        List<StationCargoOperation> stationsWithOperations = new ArrayList<>();
        for (StationCargoOperation operation : plan.getStationOperations()) {
            sb.append(String.format("%2d. %s (ID: %d)", 
                stationNumber, 
                operation.getFacility().getName(), 
                operation.getFacility().getId()));
            if (operation.hasOperations()) {
                int totalOps = operation.getFreightToLoad().size() + operation.getFreightToUnload().size();
                sb.append(String.format(" - %d cargo operation(s)", totalOps));
                stationsWithOperations.add(operation);
            } else {
                sb.append(" - Transit only (no cargo operations)");
            }
            sb.append("\n");
            stationNumber++;
        }
        if (stationsWithOperations.isEmpty()) {
            sb.append("\n  Note: No stations with cargo operations - this is a transit route.\n");
        }

        sb.append("\n");
        sb.append("LIST OF CARGOS TO BE LOADED/UNLOADED AT EACH STATION:\n");
        sb.append("=====================================================\n\n");

        // Show detailed cargo operations for ALL stations (even if no operations occur)
        stationNumber = 1;
        for (StationCargoOperation operation : plan.getStationOperations()) {

            sb.append("STATION ").append(stationNumber).append(": ").append(operation.getFacility().getName())
              .append(" (ID: ").append(operation.getFacility().getId()).append(")\n");
            sb.append("────────────────────────────────────────────────────────────────────────────\n");

            // List of cargos to be LOADED at this station
            // (Only show if there are actual loading operations)
            if (!operation.getFreightToLoad().isEmpty()) {
                sb.append("\n  LIST OF CARGOS TO BE LOADED (").append(operation.getFreightToLoad().size()).append(" cargo(s)):\n");
                sb.append("  ──────────────────────────────────────────────────────────────\n");
                int cargoIndex = 1;
                for (Freight freight : operation.getFreightToLoad()) {
                    try {
                        List<Integer> wagonIds = freightRepository.getWagonIdsByFreightId(freight.getId());
                        sb.append(String.format("    %d. Cargo ID: %d\n", cargoIndex, freight.getId()));
                        sb.append(String.format("       Origin:      %s (ID: %d)\n", 
                            freight.getOriginFacility().getName(), 
                            freight.getOriginFacility().getId()));
                        sb.append(String.format("       Destination: %s (ID: %d)\n", 
                            freight.getDestinationFacility().getName(),
                            freight.getDestinationFacility().getId()));
                        if (!wagonIds.isEmpty()) {
                            sb.append(String.format("       Wagons:      %s\n", 
                                wagonIds.toString().replaceAll("[\\[\\]]", "")));
                        }
                        sb.append("\n");
                        cargoIndex++;
                    } catch (SQLException e) {
                        sb.append(String.format("    %d. Cargo ID: %d (Error loading wagon details)\n\n", cargoIndex, freight.getId()));
                        cargoIndex++;
                    }
                }
            }

            // List of cargos to be UNLOADED at this station
            // (Only show if there are actual unloading operations)
            if (!operation.getFreightToUnload().isEmpty()) {
                sb.append("  LIST OF CARGOS TO BE UNLOADED (").append(operation.getFreightToUnload().size()).append(" cargo(s)):\n");
                sb.append("  ──────────────────────────────────────────────────────────────\n");
                int cargoIndex = 1;
                for (Freight freight : operation.getFreightToUnload()) {
                    try {
                        List<Integer> wagonIds = freightRepository.getWagonIdsByFreightId(freight.getId());
                        sb.append(String.format("    %d. Cargo ID: %d\n", cargoIndex, freight.getId()));
                        sb.append(String.format("       Origin:      %s (ID: %d)\n", 
                            freight.getOriginFacility().getName(), 
                            freight.getOriginFacility().getId()));
                        sb.append(String.format("       Destination: %s (ID: %d)\n", 
                            freight.getDestinationFacility().getName(),
                            freight.getDestinationFacility().getId()));
                        if (!wagonIds.isEmpty()) {
                            sb.append(String.format("       Wagons:      %s\n", 
                                wagonIds.toString().replaceAll("[\\[\\]]", "")));
                        }
                        sb.append("\n");
                        cargoIndex++;
                    } catch (SQLException e) {
                        sb.append(String.format("    %d. Cargo ID: %d (Error loading wagon details)\n\n", cargoIndex, freight.getId()));
                        cargoIndex++;
                    }
                }
            }

            // If no operations at this station, indicate it's transit only
            if (!operation.hasOperations()) {
                sb.append("  (Transit only - no cargo operations at this station)\n");
            }

            sb.append("\n");
            stationNumber++;
        }

        // Summary
        sb.append("================================================================================\n");
        sb.append("SUMMARY\n");
        sb.append("================================================================================\n");
        int totalLoads = 0;
        int totalUnloads = 0;
        for (StationCargoOperation operation : plan.getStationOperations()) {
            totalLoads += operation.getFreightToLoad().size();
            totalUnloads += operation.getFreightToUnload().size();
        }
        sb.append("Total cargo loading operations: ").append(totalLoads).append("\n");
        sb.append("Total cargo unloading operations: ").append(totalUnloads).append("\n");
        sb.append("================================================================================\n");

        return sb.toString();
    }

    /**
     * Gets a list representation of the route plan suitable for display.
     * Returns a list of strings, each representing a station stop with its operations.
     * 
     * @param routeId the ID of the route
     * @return list of formatted strings for each station stop
     * @throws SQLException if there is a database error
     */
    public List<String> getRoutePlanList(int routeId) throws SQLException {
        RoutePlan plan = generateRoutePlan(routeId);
        if (plan == null) {
            return Collections.singletonList("Route not found.");
        }

        List<String> result = new ArrayList<>();
        result.add("Route ID: " + routeId + " (" + (plan.isSimple() ? "Simple" : "Complex") + ")");
        result.add("");

        int stationNumber = 1;
        for (StationCargoOperation operation : plan.getStationOperations()) {
            StringBuilder stationInfo = new StringBuilder();
            stationInfo.append(stationNumber).append(". ").append(operation.getFacility().getName());

            List<String> operations = new ArrayList<>();
            if (!operation.getFreightToLoad().isEmpty()) {
                operations.add("LOAD " + operation.getFreightToLoad().size() + " cargo(es)");
            }
            if (!operation.getFreightToUnload().isEmpty()) {
                operations.add("UNLOAD " + operation.getFreightToUnload().size() + " cargo(es)");
            }

            if (!operations.isEmpty()) {
                stationInfo.append(" - ").append(String.join(", ", operations));
            } else {
                stationInfo.append(" - Transit only");
            }

            result.add(stationInfo.toString());

            // Add details for loaded cargo
            for (Freight freight : operation.getFreightToLoad()) {
                result.add("   [LOAD] Freight #" + freight.getId() + 
                          " → " + freight.getDestinationFacility().getName());
            }

            // Add details for unloaded cargo
            for (Freight freight : operation.getFreightToUnload()) {
                result.add("   [UNLOAD] Freight #" + freight.getId());
            }

            stationNumber++;
        }

        return result;
    }
}

