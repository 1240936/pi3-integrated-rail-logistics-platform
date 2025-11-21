package main.domain;

/**
 * Represents a siding (side line) on a line segment for train crossings
 */
public class Siding {
    private final int id;
    private final int lineSegmentId;
    private final double position; // position along the segment in meters
    private final double length; // length of the siding in meters

    public Siding(int id, int lineSegmentId, double position, double length) {
        this.id = id;
        this.lineSegmentId = lineSegmentId;
        this.position = position;
        this.length = length;
    }

    public int getId() {
        return id;
    }

    public int getLineSegmentId() {
        return lineSegmentId;
    }

    public double getPosition() {
        return position;
    }

    public double getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "Siding{" +
                "id=" + id +
                ", lineSegmentId=" + lineSegmentId +
                ", position=" + position +
                ", length=" + length +
                '}';
    }
}

