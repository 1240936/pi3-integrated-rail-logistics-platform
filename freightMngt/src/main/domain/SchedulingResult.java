package main.domain;

import java.util.List;

/**
 * Result of scheduling a route, containing the route, events, and crossing operations
 */
public class SchedulingResult {
    private final Route route;
    private final List<TrainEvent> events;
    private final List<CrossingOperation> crossings;

    public SchedulingResult(Route route, List<TrainEvent> events, List<CrossingOperation> crossings) {
        this.route = route;
        this.events = events;
        this.crossings = crossings;
    }

    public Route getRoute() {
        return route;
    }

    public List<TrainEvent> getEvents() {
        return events;
    }

    public List<CrossingOperation> getCrossings() {
        return crossings;
    }
}

