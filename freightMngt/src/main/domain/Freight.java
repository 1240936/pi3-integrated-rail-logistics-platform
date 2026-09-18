package main.domain;

/**
 * Represents freight that needs to be transported.
 * Freight has an origin facility where it is picked up and a destination facility
 * where it is delivered. Freight can be assigned to a route (routeId != 0) or
 * remain unassigned (routeId == 0).
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class Freight {
    /** The unique identifier for this freight */
    private final int id;
    
    /** The ID of the route this freight is assigned to (0 if unassigned) */
    private final int routeId;
    
    /** The facility where this freight originates (pickup location) */
    private final Facility originFacility;
    
    /** The facility where this freight is delivered (destination) */
    private final Facility destinationFacility;

    /**
     * Constructs a Freight object.
     * 
     * @param id the unique identifier for this freight
     * @param routeId the ID of the route this freight is assigned to (0 if unassigned)
     * @param originFacility the facility where this freight originates
     * @param destinationFacility the facility where this freight is delivered
     */
    public Freight(int id, int routeId, Facility originFacility, Facility destinationFacility) {
        this.id = id;
        this.routeId = routeId;
        this.originFacility = originFacility;
        this.destinationFacility = destinationFacility;
    }

    /**
     * Gets the unique identifier for this freight.
     * 
     * @return the freight ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the ID of the route this freight is assigned to.
     * 
     * @return the route ID, or 0 if this freight is unassigned
     */
    public int getRouteId() {
        return routeId;
    }

    /**
     * Gets the facility where this freight originates (pickup location).
     * 
     * @return the origin facility
     */
    public Facility getOriginFacility() {
        return originFacility;
    }

    /**
     * Gets the facility where this freight is delivered (destination).
     * 
     * @return the destination facility
     */
    public Facility getDestinationFacility() {
        return destinationFacility;
    }
}

