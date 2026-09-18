package main.domain;

import java.util.List;

/**
 * Represents the result of a pick path sequencing operation.
 * Stores the ordered sequence of bay coordinates to visit, the total distance traveled,
 * and metadata about the strategy used.
 */
public class PickSequenceResult {
    private final List<Coordinate> sequence;
    private final double totalDistance;
    private final String strategyName;
    private final int totalBays;

    /**
     * Constructs a PickSequenceResult.
     *
     * @param sequence ordered list of coordinates representing the pick path
     * @param totalDistance total travel distance for the path
     * @param strategyName name of the sequencing strategy used
     */
    public PickSequenceResult(List<Coordinate> sequence, double totalDistance, String strategyName) {
        this.sequence = List.copyOf(sequence);
        this.totalDistance = totalDistance;
        this.strategyName = strategyName;
        this.totalBays = sequence.size();
    }

    /** @return an unmodifiable list of coordinates in the pick path sequence */
    public List<Coordinate> getSequence() {
        return sequence;
    }

    /** @return the total distance traveled for this pick sequence */
    public double getTotalDistance() {
        return totalDistance;
    }

    /** @return the name of the pick sequencing strategy used */
    public String getStrategyName() {
        return strategyName;
    }

    /** @return total number of bays visited in this sequence */
    public int getTotalBays() {
        return totalBays;
    }

    @Override
    public String toString() {
        return String.format("PickSequenceResult{%s, distance=%.2f, bays=%d}", strategyName, totalDistance, totalBays);
    }
}
