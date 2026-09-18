package main.domain;

/**
 * Represents a segment of a rail line.
 * A rail line is composed of multiple segments in order. Each segment has
 * properties such as length, maximum weight capacity, number of tracks,
 * and optional speed limit. Single-track segments (numberOfTracks == 1)
 * require crossing detection when multiple trains need to use them.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class LineSegment {
    /** The unique identifier for this line segment */
    private final int id;
    
    /** The ID of the rail line this segment belongs to */
    private final int railLineId;
    
    /** The maximum weight capacity in tons */
    private final double maxWeight;
    
    /** The length of the segment in meters */
    private final double length;
    
    /** The number of parallel tracks (1 = single track, 2+ = multi-track) */
    private final int numberOfTracks;
    
    /** The speed limit in km/h (null if no speed limit specified) */
    private final Double speedLimit;
    
    /** The order number indicating position in the rail line */
    private final int orderNum;

    /**
     * Constructs a LineSegment object.
     * 
     * @param id the unique identifier for this line segment
     * @param railLineId the ID of the rail line this segment belongs to
     * @param maxWeight the maximum weight capacity in tons
     * @param length the length of the segment in meters
     * @param numberOfTracks the number of parallel tracks (1 = single track, 2+ = multi-track)
     * @param speedLimit the speed limit in km/h (null if no speed limit)
     * @param orderNum the order number indicating position in the rail line
     */
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

    /**
     * Gets the unique identifier for this line segment.
     * 
     * @return the segment ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the ID of the rail line this segment belongs to.
     * 
     * @return the rail line ID
     */
    public int getRailLineId() {
        return railLineId;
    }

    /**
     * Gets the maximum weight capacity of this segment.
     * 
     * @return the maximum weight in tons
     */
    public double getMaxWeight() {
        return maxWeight;
    }

    /**
     * Gets the length of this segment.
     * 
     * @return the length in meters
     */
    public double getLength() {
        return length;
    }

    /**
     * Gets the number of parallel tracks on this segment.
     * 
     * @return the number of tracks (1 = single track, 2+ = multi-track)
     */
    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    /**
     * Checks if this segment is a single-track segment.
     * Single-track segments require crossing detection when multiple trains use them.
     * 
     * @return true if this segment has only one track, false otherwise
     */
    public boolean isSingleTrack() {
        return numberOfTracks == 1;
    }

    /**
     * Gets the speed limit for this segment.
     * 
     * @return the speed limit in km/h, or null if no speed limit is specified
     */
    public Double getSpeedLimit() {
        return speedLimit;
    }

    /**
     * Gets the order number indicating position in the rail line.
     * 
     * @return the order number
     */
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

