package domain;

import java.util.List;

/**
 * Represents the result of pick path sequencing.
 * Contains the ordered sequence of coordinates and total distance.
 */
public class PickSequenceResult {
    private final List<Coordinate> sequence;
    private final double totalDistance;
    private final String strategyName;
    private final int totalBays;

    public PickSequenceResult(List<Coordinate> sequence, double totalDistance, String strategyName) {
        this.sequence = List.copyOf(sequence);
        this.totalDistance = totalDistance;
        this.strategyName = strategyName;
        this.totalBays = sequence.size();
    }

    public List<Coordinate> getSequence() {
        return sequence;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public int getTotalBays() {
        return totalBays;
    }

    @Override
    public String toString() {
        return String.format("PickSequenceResult{%s, distance=%.2f, bays=%d}", strategyName, totalDistance, totalBays);
    }
}
