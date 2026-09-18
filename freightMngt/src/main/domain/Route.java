package main.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a route for a train with a manually defined path.
 * A route consists of a start facility, an end facility, a departure time,
 * and an ordered list of intermediate facilities (path points) that the train
 * must pass through. The path is manually defined by the freight manager.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class Route {
    /** The unique identifier for this route */
    private final int id;
    
    /** The ID of the train assigned to this route */
    private final int trainId;
    
    /** The facility where the route starts */
    private final Facility startFacility;
    
    /** The facility where the route ends */
    private final Facility endFacility;
    
    /** The departure date and time from the start facility */
    private final LocalDateTime startDate;
    
    /** The manually defined path of intermediate facilities */
    private final List<RoutePathPoint> path;

    /**
     * Constructs a Route object.
     * 
     * @param id the unique identifier for this route
     * @param trainId the ID of the train assigned to this route
     * @param startFacility the facility where the route starts
     * @param endFacility the facility where the route ends
     * @param startDate the departure date and time from the start facility
     */
    public Route(int id, int trainId, Facility startFacility, Facility endFacility, 
                LocalDateTime startDate) {
        this.id = id;
        this.trainId = trainId;
        this.startFacility = startFacility;
        this.endFacility = endFacility;
        this.startDate = startDate;
        this.path = new ArrayList<>();
    }

    /**
     * Gets the unique identifier for this route.
     * 
     * @return the route ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the ID of the train assigned to this route.
     * 
     * @return the train ID
     */
    public int getTrainId() {
        return trainId;
    }

    /**
     * Gets the facility where the route starts.
     * 
     * @return the start facility
     */
    public Facility getStartFacility() {
        return startFacility;
    }

    /**
     * Gets the facility where the route ends.
     * 
     * @return the end facility
     */
    public Facility getEndFacility() {
        return endFacility;
    }

    /**
     * Gets the departure date and time from the start facility.
     * 
     * @return the start date and time
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    /**
     * Gets the manually defined path of intermediate facilities.
     * 
     * @return the list of path points (intermediate facilities) in order
     */
    public List<RoutePathPoint> getPath() {
        return path;
    }

    /**
     * Adds a path point (intermediate facility) to the route.
     * The path is automatically sorted by sequence number after adding.
     * 
     * @param facility the facility to add to the path
     * @param sequenceNumber the sequence number indicating the order in the path
     */
    public void addPathPoint(Facility facility, int sequenceNumber) {
        path.add(new RoutePathPoint(facility, sequenceNumber));
        // Sort by sequence number
        path.sort((a, b) -> Integer.compare(a.getSequenceNumber(), b.getSequenceNumber()));
    }

    /**
     * Represents a point in the route path (an intermediate facility).
     * Path points are ordered by their sequence number, indicating the order
     * in which the train should pass through intermediate facilities.
     * 
     * @author Freight Management System
     * @version 1.0
     */
    public static class RoutePathPoint {
        /** The facility at this path point */
        private final Facility facility;
        
        /** The sequence number indicating the order in the path */
        private final int sequenceNumber;

        /**
         * Constructs a RoutePathPoint object.
         * 
         * @param facility the facility at this path point
         * @param sequenceNumber the sequence number indicating the order in the path
         */
        public RoutePathPoint(Facility facility, int sequenceNumber) {
            this.facility = facility;
            this.sequenceNumber = sequenceNumber;
        }

        /**
         * Gets the facility at this path point.
         * 
         * @return the facility
         */
        public Facility getFacility() {
            return facility;
        }

        /**
         * Gets the sequence number indicating the order in the path.
         * 
         * @return the sequence number
         */
        public int getSequenceNumber() {
            return sequenceNumber;
        }
    }

    @Override
    public String toString() {
        return "Route{" +
                "id=" + id +
                ", trainId=" + trainId +
                ", start=" + startFacility.getName() +
                ", end=" + endFacility.getName() +
                ", startDate=" + startDate +
                ", pathPoints=" + path.size() +
                '}';
    }
}

