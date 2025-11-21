package main.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a route for a train with a manually defined path
 */
public class Route {
    private final int id;
    private final int trainId;
    private final Facility startFacility;
    private final Facility endFacility;
    private final LocalDateTime startDate;
    private final List<RoutePathPoint> path; // manually defined path

    public Route(int id, int trainId, Facility startFacility, Facility endFacility, 
                LocalDateTime startDate) {
        this.id = id;
        this.trainId = trainId;
        this.startFacility = startFacility;
        this.endFacility = endFacility;
        this.startDate = startDate;
        this.path = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getTrainId() {
        return trainId;
    }

    public Facility getStartFacility() {
        return startFacility;
    }

    public Facility getEndFacility() {
        return endFacility;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public List<RoutePathPoint> getPath() {
        return path;
    }

    public void addPathPoint(Facility facility, int sequenceNumber) {
        path.add(new RoutePathPoint(facility, sequenceNumber));
        // Sort by sequence number
        path.sort((a, b) -> Integer.compare(a.getSequenceNumber(), b.getSequenceNumber()));
    }

    /**
     * Represents a point in the route path
     */
    public static class RoutePathPoint {
        private final Facility facility;
        private final int sequenceNumber;

        public RoutePathPoint(Facility facility, int sequenceNumber) {
            this.facility = facility;
            this.sequenceNumber = sequenceNumber;
        }

        public Facility getFacility() {
            return facility;
        }

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

