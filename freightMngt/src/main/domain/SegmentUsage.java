package main.domain;

import java.time.LocalDateTime;

/**
 * Helper class to track segment usage by trains for crossing detection
 */
public class SegmentUsage {
    private final Route route;
    private final Train train;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public SegmentUsage(Route route, Train train, LocalDateTime startTime, LocalDateTime endTime) {
        this.route = route;
        this.train = train;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Route getRoute() {
        return route;
    }

    public Train getTrain() {
        return train;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}

