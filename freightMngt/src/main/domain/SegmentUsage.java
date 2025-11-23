package main.domain;

import java.time.LocalDateTime;

/**
 * Helper class to track segment usage by trains for crossing detection.
 * This class records when a specific train on a specific route uses a
 * line segment, including the start and end times. This information is
 * used to detect conflicts on single-track segments.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class SegmentUsage {
    /** The route that uses this segment */
    private final Route route;
    
    /** The train that uses this segment */
    private final Train train;
    
    /** The time when the train enters this segment */
    private final LocalDateTime startTime;
    
    /** The time when the train exits this segment */
    private final LocalDateTime endTime;

    /**
     * Constructs a SegmentUsage object.
     * 
     * @param route the route that uses this segment
     * @param train the train that uses this segment
     * @param startTime the time when the train enters this segment
     * @param endTime the time when the train exits this segment
     */
    public SegmentUsage(Route route, Train train, LocalDateTime startTime, LocalDateTime endTime) {
        this.route = route;
        this.train = train;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Gets the route that uses this segment.
     * 
     * @return the route
     */
    public Route getRoute() {
        return route;
    }

    /**
     * Gets the train that uses this segment.
     * 
     * @return the train
     */
    public Train getTrain() {
        return train;
    }

    /**
     * Gets the time when the train enters this segment.
     * 
     * @return the start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Gets the time when the train exits this segment.
     * 
     * @return the end time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
}

