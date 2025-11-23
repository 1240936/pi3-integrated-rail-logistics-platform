package main.domain;

import java.util.List;

/**
 * Result of scheduling a route, containing the route, calculated events, and crossing operations.
 * This class encapsulates the complete result of a route scheduling operation,
 * including all passage times and any detected conflicts with other routes.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class SchedulingResult {
    /** The route that was scheduled */
    private final Route route;
    
    /** The list of train events representing passage times at each facility */
    private final List<TrainEvent> events;
    
    /** The list of crossing operations with other routes */
    private final List<CrossingOperation> crossings;

    /**
     * Constructs a SchedulingResult object.
     * 
     * @param route the route that was scheduled
     * @param events the list of train events representing passage times
     * @param crossings the list of crossing operations with other routes
     */
    public SchedulingResult(Route route, List<TrainEvent> events, List<CrossingOperation> crossings) {
        this.route = route;
        this.events = events;
        this.crossings = crossings;
    }

    /**
     * Gets the route that was scheduled.
     * 
     * @return the route
     */
    public Route getRoute() {
        return route;
    }

    /**
     * Gets the list of train events representing passage times at each facility.
     * 
     * @return the list of train events
     */
    public List<TrainEvent> getEvents() {
        return events;
    }

    /**
     * Gets the list of crossing operations with other routes.
     * 
     * @return the list of crossing operations
     */
    public List<CrossingOperation> getCrossings() {
        return crossings;
    }
}

