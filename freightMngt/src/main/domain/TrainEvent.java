package main.domain;

import java.time.LocalDateTime;

/**
 * Represents a train event (estimated or actual passage time at a facility)
 */
public class TrainEvent {
    private final int id;
    private final int routeId;
    private final int trainId;
    private final Facility facility;
    private final LocalDateTime eventTime;

    public TrainEvent(int id, int routeId, int trainId, Facility facility, LocalDateTime eventTime) {
        this.id = id;
        this.routeId = routeId;
        this.trainId = trainId;
        this.facility = facility;
        this.eventTime = eventTime;
    }

    public int getId() {
        return id;
    }

    public int getRouteId() {
        return routeId;
    }

    public int getTrainId() {
        return trainId;
    }

    public Facility getFacility() {
        return facility;
    }

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

