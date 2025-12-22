package main.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a route plan that shows the sequence of stations/facilities
 * along with the cargo (freight) operations to be performed at each station.
 * A route plan can be simple (direct route) or complex (with intermediate stops).
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class RoutePlan {
    /** The route this plan is based on */
    private final Route route;
    
    /** The ordered list of station cargo operations */
    private final List<StationCargoOperation> stationOperations;

    /**
     * Constructs a RoutePlan object.
     * 
     * @param route the route this plan is based on
     */
    public RoutePlan(Route route) {
        this.route = route;
        this.stationOperations = new ArrayList<>();
    }

    /**
     * Gets the route this plan is based on.
     * 
     * @return the route
     */
    public Route getRoute() {
        return route;
    }

    /**
     * Gets the ordered list of station cargo operations.
     * 
     * @return an unmodifiable list of station operations in sequence order
     */
    public List<StationCargoOperation> getStationOperations() {
        return Collections.unmodifiableList(stationOperations);
    }

    /**
     * Adds a station cargo operation to this route plan.
     * Operations are automatically sorted by sequence number.
     * 
     * @param operation the station operation to add
     */
    public void addStationOperation(StationCargoOperation operation) {
        if (operation != null) {
            stationOperations.add(operation);
            // Sort by sequence number to maintain order
            stationOperations.sort((a, b) -> 
                Integer.compare(a.getSequenceNumber(), b.getSequenceNumber()));
        }
    }

    /**
     * Gets the station cargo operation for a specific facility.
     * 
     * @param facilityId the ID of the facility
     * @return the station operation, or null if not found
     */
    public StationCargoOperation getOperationForFacility(int facilityId) {
        return stationOperations.stream()
                .filter(op -> op.getFacility().getId() == facilityId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if this route plan is simple or complex based on the number of freight items.
     * 
     * @return true if exactly one freight item is assigned (simple),
     *         false if more than one freight item is assigned (complex)
     */
    public boolean isSimple() {
        // Simple route: exactly one freight item
        // Complex route: more than one freight item
        return getAllFreight().size() == 1;
    }

    /**
     * Gets all freight items associated with this route plan.
     * Returns all unique freight items that are loaded at any station.
     * 
     * @return list of all unique freight items that will be transported on this route
     */
    public List<Freight> getAllFreight() {
        java.util.Set<Freight> allFreightSet = new java.util.HashSet<>();
        for (StationCargoOperation op : stationOperations) {
            allFreightSet.addAll(op.getFreightToLoad());
        }
        // Also include freight that are unloaded (in case they're not loaded on this route)
        for (StationCargoOperation op : stationOperations) {
            allFreightSet.addAll(op.getFreightToUnload());
        }
        return new ArrayList<>(allFreightSet);
    }

    @Override
    public String toString() {
        return "RoutePlan{" +
                "routeId=" + route.getId() +
                ", isSimple=" + isSimple() +
                ", stations=" + stationOperations.size() +
                ", totalFreight=" + getAllFreight().size() +
                '}';
    }
}

