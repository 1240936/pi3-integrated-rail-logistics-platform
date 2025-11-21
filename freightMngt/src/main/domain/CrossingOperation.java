package main.domain;

import java.time.LocalDateTime;

/**
 * Represents a crossing operation between two trains on a single track segment
 */
public class CrossingOperation {
    private final Train train1;
    private final Train train2;
    private final Facility crossingLocation; // facility where crossing will occur
    private final Siding siding; // siding used for crossing (nullable)
    private final LocalDateTime crossingTime;

    public CrossingOperation(Train train1, Train train2, Facility crossingLocation, 
                            Siding siding, LocalDateTime crossingTime) {
        this.train1 = train1;
        this.train2 = train2;
        this.crossingLocation = crossingLocation;
        this.siding = siding;
        this.crossingTime = crossingTime;
    }

    public Train getTrain1() {
        return train1;
    }

    public Train getTrain2() {
        return train2;
    }

    public Facility getCrossingLocation() {
        return crossingLocation;
    }

    public Siding getSiding() {
        return siding;
    }

    public boolean usesSiding() {
        return siding != null;
    }

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

