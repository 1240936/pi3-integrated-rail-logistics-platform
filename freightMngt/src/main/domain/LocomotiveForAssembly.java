package main.domain;

/**
 * Represents a locomotive available for train assembly with its status information.
 * This DTO includes whether the locomotive is in-transit or parked, and the relevant facility information.
 */
public class LocomotiveForAssembly {
    private final Locomotive locomotive;
    private final boolean inTransit;
    private final Integer routeId;  // null if parked
    private final Integer destinationFacilityId;  // null if parked
    private final String destinationFacilityName;  // null if parked
    private final Integer parkedFacilityId;  // null if in-transit
    private final String parkedFacilityName;  // null if in-transit
    private final Double distanceFromStartKm;  // Distance in km from route start facility (null if in-transit or cannot calculate)

    /**
     * Constructs a LocomotiveForAssembly object.
     * 
     * @param locomotive the locomotive entity
     * @param inTransit true if the locomotive is in-transit, false if parked
     * @param routeId the route ID if in-transit, null if parked
     * @param destinationFacilityId the destination facility ID if in-transit, null if parked
     * @param destinationFacilityName the destination facility name if in-transit, null if parked
     * @param parkedFacilityId the facility ID where parked, null if in-transit
     * @param parkedFacilityName the facility name where parked, null if in-transit
     * @param distanceFromStartKm distance in km from route start facility, null if in-transit or cannot calculate
     */
    public LocomotiveForAssembly(Locomotive locomotive, boolean inTransit,
                                 Integer routeId, Integer destinationFacilityId, String destinationFacilityName,
                                 Integer parkedFacilityId, String parkedFacilityName, Double distanceFromStartKm) {
        this.locomotive = locomotive;
        this.inTransit = inTransit;
        this.routeId = routeId;
        this.destinationFacilityId = destinationFacilityId;
        this.destinationFacilityName = destinationFacilityName;
        this.parkedFacilityId = parkedFacilityId;
        this.parkedFacilityName = parkedFacilityName;
        this.distanceFromStartKm = distanceFromStartKm;
    }

    public Locomotive getLocomotive() {
        return locomotive;
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

    public Double getDistanceFromStartKm() {
        return distanceFromStartKm;
    }

    /**
     * Gets a description of the locomotive's location/status.
     * 
     * @return a string describing where the locomotive is or where it's heading
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
        return "LocomotiveForAssembly{" +
                "locomotive=" + locomotive +
                ", inTransit=" + inTransit +
                ", location=" + getLocationDescription() +
                '}';
    }
}

