package main.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete picking plan for an order batch.
 * A pick plan contains multiple trolleys, tracks skipped items, and summarizes weights and item counts.
 */
public class PickPlan {
    private final List<Trolley> trolleys;
    private final List<String> skippedItems;
    private final String packingHeuristic;
    private final double totalWeight;
    private final int totalItems;

    /**
     * Constructs a PickPlan with the given trolleys, skipped items, and packing heuristic.
     * Calculates total weight and total item count automatically.
     *
     * @param trolleys list of trolleys in the pick plan
     * @param skippedItems list of items that could not be allocated due to capacity constraints
     * @param packingHeuristic name of the packing heuristic used
     */
    public PickPlan(List<Trolley> trolleys, List<String> skippedItems, String packingHeuristic) {
        this.trolleys = new ArrayList<>(trolleys);
        this.skippedItems = new ArrayList<>(skippedItems);
        this.packingHeuristic = packingHeuristic;

        // Compute totals
        this.totalWeight = trolleys.stream().mapToDouble(Trolley::getCurrentWeight).sum();
        this.totalItems = trolleys.stream().mapToInt(t -> t.getItems().size()).sum();
    }

    /** @return a copy of the list of trolleys in this pick plan */
    public List<Trolley> getTrolleys() {
        return new ArrayList<>(trolleys);
    }

    /** @return a copy of the list of skipped items */
    public List<String> getSkippedItems() {
        return new ArrayList<>(skippedItems);
    }

    /** @return the name of the packing heuristic used to generate this plan */
    public String getPackingHeuristic() {
        return packingHeuristic;
    }

    /** @return the total weight of all items in the plan */
    public double getTotalWeight() {
        return totalWeight;
    }

    /** @return the total number of pick items across all trolleys */
    public int getTotalItems() {
        return totalItems;
    }

    /** @return the number of trolleys in the pick plan */
    public int getTrolleyCount() {
        return trolleys.size();
    }

    /**
     * Computes the average weight per trolley.
     *
     * @return average trolley weight, or 0.0 if there are no trolleys
     */
    public double getAverageTrolleyWeight() {
        if (trolleys.isEmpty()) return 0.0;
        return totalWeight / trolleys.size();
    }

    /**
     * Computes the overall weight utilization of the plan.
     * Calculated as total weight divided by the sum of maximum trolley capacities.
     *
     * @return weight utilization ratio (0.0–1.0), or 0.0 if there are no trolleys
     */
    public double getWeightUtilization() {
        if (trolleys.isEmpty()) return 0.0;
        double totalCapacity = trolleys.stream().mapToDouble(Trolley::getMaxWeightCapacity).sum();
        return totalWeight / totalCapacity;
    }

    @Override
    public String toString() {
        return "PickPlan{" +
                "trolleys=" + trolleys.size() +
                ", totalWeight=" + totalWeight +
                ", totalItems=" + totalItems +
                ", heuristic=" + packingHeuristic +
                '}';
    }
}
