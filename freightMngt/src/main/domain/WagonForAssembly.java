package main.domain;

/**
 * Represents a wagon available for train assembly with its status information.
 * This DTO includes whether the wagon is in-transit or parked, and the relevant facility information.
 */
public class WagonForAssembly {
    private final Wagon wagon;
    private final boolean inTransit;
    private final Integer routeId;  // null if parked
    private final Integer destinationFacilityId;  // null if parked
    private final String destinationFacilityName;  // null if parked
    private final Integer parkedFacilityId;  // null if in-transit
    private final String parkedFacilityName;  // null if in-transit

    /**
     * Constructs a WagonForAssembly object.
     * 
     * @param wagon the wagon entity
     * @param inTransit true if the wagon is in-transit, false if parked
     * @param routeId the route ID if in-transit, null if parked
     * @param destinationFacilityId the destination facility ID if in-transit, null if parked
     * @param destinationFacilityName the destination facility name if in-transit, null if parked
     * @param parkedFacilityId the facility ID where parked, null if in-transit
     * @param parkedFacilityName the facility name where parked, null if in-transit
     */
    public WagonForAssembly(Wagon wagon, boolean inTransit,
                           Integer routeId, Integer destinationFacilityId, String destinationFacilityName,
                           Integer parkedFacilityId, String parkedFacilityName) {
        this.wagon = wagon;
        this.inTransit = inTransit;
        this.routeId = routeId;
        this.destinationFacilityId = destinationFacilityId;
        this.destinationFacilityName = destinationFacilityName;
        this.parkedFacilityId = parkedFacilityId;
        this.parkedFacilityName = parkedFacilityName;
    }

    public Wagon getWagon() {
        return wagon;
    }

    public boolean isInTransit() {
        return inTransit;
    }

    public boolean isParked() {
        return !inTransit;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public Integer getDestinationFacilityId() {
        return destinationFacilityId;
    }

    public String getDestinationFacilityName() {
        return destinationFacilityName;
    }

    public Integer getParkedFacilityId() {
        return parkedFacilityId;
    }

    public String getParkedFacilityName() {
        return parkedFacilityName;
    }

    /**
     * Gets a description of the wagon's location/status.
     * 
     * @return a string describing where the wagon is or where it's heading
     */
    public String getLocationDescription() {
        if (inTransit) {
            return "In transit to: " + (destinationFacilityName != null ? destinationFacilityName : "Facility " + destinationFacilityId);
        } else {
            return "Parked at: " + (parkedFacilityName != null ? parkedFacilityName : "Facility " + parkedFacilityId);
        }
    }

    @Override
    public String toString() {
        return "WagonForAssembly{" +
                "wagon=" + wagon +
                ", inTransit=" + inTransit +
                ", location=" + getLocationDescription() +
                '}';
    }
}

