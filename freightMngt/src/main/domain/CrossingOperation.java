package main.domain;

import java.time.LocalDateTime;

/**
 * Represents a crossing operation between two trains on a single track segment.
 * A crossing operation is required when two trains need to use the same single-track
 * segment at overlapping times. The crossing specifies where and when the trains
 * should meet, and optionally which siding should be used.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class CrossingOperation {
    /** The first train involved in the crossing */
    private final Train train1;
    
    /** The second train involved in the crossing */
    private final Train train2;
    
    /** The route ID for train1 */
    private final int route1Id;
    
    /** The route ID for train2 */
    private final int route2Id;
    
    /** The facility where the crossing will occur */
    private final Facility crossingLocation;
    
    /** The siding used for the crossing (null if no siding available) */
    private final Siding siding;
    
    /** The scheduled time for the crossing operation */
    private final LocalDateTime crossingTime;

    /**
     * Constructs a CrossingOperation object.
     * 
     * @param train1 the first train involved in the crossing
     * @param train2 the second train involved in the crossing
     * @param route1Id the route ID for train1
     * @param route2Id the route ID for train2
     * @param crossingLocation the facility where the crossing will occur
     * @param siding the siding used for the crossing (null if no siding available)
     * @param crossingTime the scheduled time for the crossing operation
     */
    public CrossingOperation(Train train1, Train train2, int route1Id, int route2Id,
                            Facility crossingLocation, Siding siding, LocalDateTime crossingTime) {
        this.train1 = train1;
        this.train2 = train2;
        this.route1Id = route1Id;
        this.route2Id = route2Id;
        this.crossingLocation = crossingLocation;
        this.siding = siding;
        this.crossingTime = crossingTime;
    }

    /**
     * Gets the first train involved in the crossing.
     * 
     * @return train1
     */
    public Train getTrain1() {
        return train1;
    }

    /**
     * Gets the second train involved in the crossing.
     * 
     * @return train2
     */
    public Train getTrain2() {
        return train2;
    }

    /**
     * Gets the route ID for train1.
     * 
     * @return the route1 ID
     */
    public int getRoute1Id() {
        return route1Id;
    }

    /**
     * Gets the route ID for train2.
     * 
     * @return the route2 ID
     */
    public int getRoute2Id() {
        return route2Id;
    }

    /**
     * Gets the facility where the crossing will occur.
     * 
     * @return the crossing location facility
     */
    public Facility getCrossingLocation() {
        return crossingLocation;
    }

    /**
     * Gets the siding used for the crossing.
     * 
     * @return the siding, or null if no siding is available
     */
    public Siding getSiding() {
        return siding;
    }

    /**
     * Checks if this crossing operation uses a siding.
     * 
     * @return true if a siding is available and will be used, false otherwise
     */
    public boolean usesSiding() {
        return siding != null;
    }

    /**
     * Gets the scheduled time for the crossing operation.
     * 
     * @return the crossing time
     */
    public LocalDateTime getCrossingTime() {
        return crossingTime;
    }

    @Override
    public String toString() {
        return "CrossingOperation{" +
                "train1=" + train1.getId() +
                ", train2=" + train2.getId() +
                ", location=" + crossingLocation.getName() +
                ", usesSiding=" + usesSiding() +
                ", time=" + crossingTime +
                '}';
    }
}

