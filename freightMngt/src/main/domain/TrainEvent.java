package main.domain;

import java.time.LocalDateTime;

/**
 * Represents a train event (estimated or actual passage time at a facility).
 * Train events record when a train arrives at or passes through a facility
 * along its route. These events are used to calculate passage times and
 * detect potential conflicts.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class TrainEvent {
    /** The unique identifier for this train event */
    private final int id;
    
    /** The ID of the route this event belongs to */
    private final int routeId;
    
    /** The ID of the train involved in this event */
    private final int trainId;
    
    /** The facility where this event occurs */
    private final Facility facility;
    
    /** The date and time when this event occurs */
    private final LocalDateTime eventTime;

    /**
     * Constructs a TrainEvent object.
     * 
     * @param id the unique identifier for this train event
     * @param routeId the ID of the route this event belongs to
     * @param trainId the ID of the train involved in this event
     * @param facility the facility where this event occurs
     * @param eventTime the date and time when this event occurs
     */
    public TrainEvent(int id, int routeId, int trainId, Facility facility, LocalDateTime eventTime) {
        this.id = id;
        this.routeId = routeId;
        this.trainId = trainId;
        this.facility = facility;
        this.eventTime = eventTime;
    }

    /**
     * Gets the unique identifier for this train event.
     * 
     * @return the event ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the ID of the route this event belongs to.
     * 
     * @return the route ID
     */
    public int getRouteId() {
        return routeId;
    }

    /**
     * Gets the ID of the train involved in this event.
     * 
     * @return the train ID
     */
    public int getTrainId() {
        return trainId;
    }

    /**
     * Gets the facility where this event occurs.
     * 
     * @return the facility
     */
    public Facility getFacility() {
        return facility;
    }

    /**
     * Gets the date and time when this event occurs.
     * 
     * @return the event time
     */
    public LocalDateTime getEventTime() {
        return eventTime;
    }

    @Override
    public String toString() {
        return "TrainEvent{" +
                "id=" + id +
                ", routeId=" + routeId +
                ", trainId=" + trainId +
                ", facility=" + facility.getName() +
                ", eventTime=" + eventTime +
                '}';
    }
}

