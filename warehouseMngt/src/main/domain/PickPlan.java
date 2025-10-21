package main.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete picking plan with multiple trolleys.
 * Contains the trolleys and summary statistics.
 */
public class PickPlan {
    private final List<Trolley> trolleys;
    private final List<String> skippedItems;
    private final String packingHeuristic;
    private final double totalWeight;
    private final int totalItems;

    public PickPlan(List<Trolley> trolleys, List<String> skippedItems, String packingHeuristic) {
        this.trolleys = new ArrayList<>(trolleys);
        this.skippedItems = new ArrayList<>(skippedItems);
        this.packingHeuristic = packingHeuristic;
        
        // Calculate totals
        this.totalWeight = trolleys.stream().mapToDouble(Trolley::getCurrentWeight).sum();
        this.totalItems = trolleys.stream().mapToInt(t -> t.getItems().size()).sum();
    }

    public List<Trolley> getTrolleys() {
        return new ArrayList<>(trolleys);
    }

    public List<String> getSkippedItems() {
        return new ArrayList<>(skippedItems);
    }

    public String getPackingHeuristic() {
        return packingHeuristic;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTrolleyCount() {
        return trolleys.size();
    }

    public double getAverageTrolleyWeight() {
        if (trolleys.isEmpty()) return 0.0;
        return totalWeight / trolleys.size();
    }

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
