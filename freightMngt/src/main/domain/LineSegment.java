package main.domain;

/**
 * Represents a segment of a rail line
 */
public class LineSegment {
    private final int id;
    private final int railLineId;
    private final double maxWeight; // in tons
    private final double length; // in meters
    private final int numberOfTracks;
    private final Double speedLimit; // in km/h (nullable)
    private final int orderNum;

    public LineSegment(int id, int railLineId, double maxWeight, double length, 
                      int numberOfTracks, Double speedLimit, int orderNum) {
        this.id = id;
        this.railLineId = railLineId;
        this.maxWeight = maxWeight;
        this.length = length;
        this.numberOfTracks = numberOfTracks;
        this.speedLimit = speedLimit;
        this.orderNum = orderNum;
    }

    public int getId() {
        return id;
    }

    public int getRailLineId() {
        return railLineId;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public double getLength() {
        return length;
    }

    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    public boolean isSingleTrack() {
        return numberOfTracks == 1;
    }

    public Double getSpeedLimit() {
        return speedLimit;
    }

    public int getOrderNum() {
        return orderNum;
    }

    @Override
    public String toString() {
        return "LineSegment{" +
                "id=" + id +
                ", railLineId=" + railLineId +
                ", length=" + length +
                ", tracks=" + numberOfTracks +
                ", speedLimit=" + speedLimit +
                '}';
    }
}

