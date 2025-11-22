package main.domain;

/**
 * Represents freight that needs to be transported
 */
public class Freight {
    private final int id;
    private final int routeId;
    private final Facility originFacility;
    private final Facility destinationFacility;

    public Freight(int id, int routeId, Facility originFacility, Facility destinationFacility) {
        this.id = id;
        this.routeId = routeId;
        this.originFacility = originFacility;
        this.destinationFacility = destinationFacility;
    }

    public int getId() {
        return id;
    }

    public int getRouteId() {
        return routeId;
    }

    public Facility getOriginFacility() {
        return originFacility;
    }

    public Facility getDestinationFacility() {
        return destinationFacility;
    }
}

