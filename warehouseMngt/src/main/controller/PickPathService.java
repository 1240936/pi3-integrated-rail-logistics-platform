package main.controller;

import main.domain.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for computing optimal pick paths using different sequencing strategies.
 * Implements the geometric distance model for warehouse navigation.
 */
public class PickPathService {

    /**
     * Computes pick path sequence using Strategy A: Deterministic Sweep
     * Sorts bays ascending by aisle, then computes total distance.
     */
    public PickSequenceResult computeDeterministicSweep(PickPlan pickPlan) {
        List<Coordinate> mergedBays = mergeDuplicateBays(pickPlan);

        /* Sort by aisle ascending, then by bay ascending
        Creates a new list, compares and sorts the Coordinates by aisle
        then by bay and finally gathers all the elements from the stream and
        sends them to a list
         */
        List<Coordinate> sortedBays = mergedBays.stream()
                .sorted(Comparator.comparingInt(Coordinate::getAisle)
                        .thenComparingInt(Coordinate::getBay))
                .collect(Collectors.toList());

        double totalDistance = calculateTotalDistance(sortedBays);

        return new PickSequenceResult(sortedBays, totalDistance, "Deterministic Sweep");
    }

    /**
     * Computes pick path sequence using Strategy B: Nearest-Neighbour Greedy
     * Starting from (0,0), always pick the next unvisited bay with smallest distance.
     */
    public PickSequenceResult computeNearestNeighbor(PickPlan pickPlan) {
        List<Coordinate> mergedBays = mergeDuplicateBays(pickPlan);
        List<Coordinate> sequence = new ArrayList<>();
        Set<Coordinate> visited = new HashSet<>();
        Coordinate currentPosition = new Coordinate(0, 0); // Start from entrance

        // Find nearest neighbor until all bays are visited
        while (sequence.size() < mergedBays.size()) {
            Coordinate nearestBay = null;
            double minDistance = Double.MAX_VALUE;

            for (Coordinate bay : mergedBays) {
                if (!visited.contains(bay)) {
                    double distance = calculateDistance(currentPosition, bay);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestBay = bay;
                    }
                }
            }

            if (nearestBay != null) {
                sequence.add(nearestBay);
                visited.add(nearestBay);
                currentPosition = nearestBay;
            }
        }

        double totalDistance = calculateTotalDistance(sequence);

        return new PickSequenceResult(sequence, totalDistance, "Nearest-Neighbour Greedy");
    }

    /**
     * Merges duplicate bays by summing their total quantities.
     * Returns a list of unique coordinates representing all bays to visit.
     */
    private List<Coordinate> mergeDuplicateBays(PickPlan pickPlan) {
        Map<Coordinate, Integer> bayQuantities = new HashMap<>();

        // Collect all bays from all trolleys
        for (Trolley trolley : pickPlan.getTrolleys()) {
            for (PickItem item : trolley.getItems()) {
                Coordinate coordinate = new Coordinate(item.getAisle(), item.getBay());
                //Integer::sum is a reference to A + B
                bayQuantities.merge(coordinate, item.getQuantity(), Integer::sum);
            }
        }

        return new ArrayList<>(bayQuantities.keySet());
    }

    /**
     * Calculates the total distance for a sequence of coordinates starting from entrance (0,0).
     */
    private double calculateTotalDistance(List<Coordinate> sequence) {
        if (sequence.isEmpty()) {
            return 0.0;
        }

        double totalDistance = 0.0;
        Coordinate currentPosition = new Coordinate(0, 0); // Start from entrance

        for (Coordinate nextBay : sequence) {
            totalDistance += calculateDistance(currentPosition, nextBay);
            currentPosition = nextBay;
        }

        return totalDistance;
    }

    /**
     * Calculates distance between two coordinates using the geometric distance model:
     * - If same aisle (a1 == a2): D = |b1 - b2|
     * - If different aisles (a1 != a2): D = b1 + |a1 - a2| * 3 + b2
     */
    private double calculateDistance(Coordinate c1, Coordinate c2) {
        int a1 = c1.getAisle();
        int b1 = c1.getBay();
        int a2 = c2.getAisle();
        int b2 = c2.getBay();

        if (a1 == a2) {
            // Same aisle: D = |b1 - b2|
            return Math.abs(b1 - b2);
        } else {
            // Different aisles: D = b1 + |a1 - a2| * 3 + b2
            return b1 + Math.abs(a1 - a2) * 3 + b2;
        }
    }
}