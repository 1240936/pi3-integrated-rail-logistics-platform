package controller;

import domain.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for creating picking plans using different packing heuristics.
 * Handles trolley assignment with weight capacity constraints.
 */
public class PickingService {

    /**
     * Creates a picking plan from allocation results using the specified heuristic.
     */
    public PickPlan createPickPlan(List<AllocationRow> allocations,
                                   PackingHeuristic heuristic,
                                   double trolleyCapacity,
                                   boolean allowSplitting) {

        List<PickItem> pickItems = convertAllocationsToPickItems(allocations);

        switch (heuristic) {
            case FIRST_FIT:
                return createFirstFitPlan(pickItems, trolleyCapacity, allowSplitting);
            case FIRST_FIT_DECREASING:
                return createFirstFitDecreasingPlan(pickItems, trolleyCapacity, allowSplitting);
            case BEST_FIT_DECREASING:
                return createBestFitDecreasingPlan(pickItems, trolleyCapacity, allowSplitting);
            default:
                throw new IllegalArgumentException("Unknown packing heuristic: " + heuristic);
        }
    }

    /**
     * First Fit (FF): Place items in the first available trolley where they fit.
     */
    private PickPlan createFirstFitPlan(List<PickItem> items, double trolleyCapacity, boolean allowSplitting) {
        List<Trolley> trolleys = new ArrayList<>();
        List<String> skippedItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            PickItem item = items.get(i);
            boolean placed = false;

            // Try to place in existing trolleys
            for (Trolley trolley : trolleys) {
                if (trolley.canFit(item)) {
                    trolley.addItem(item);
                    placed = true;
                    break;
                } else if (allowSplitting && trolley.canFitPartial(item, 1)) {
                    int maxQuantity = trolley.getMaxQuantityThatFits(item);
                    if (maxQuantity > 0) {
                        int remaining = trolley.addPartialItem(item, maxQuantity);
                        if (remaining > 0) {
                            PickItem remainingItem = item.withQuantity(remaining);
                            items.add(i + 1, remainingItem);
                        }
                        placed = true;
                        break;
                    }
                }
            }

            // If not placed, create new trolley
            if (!placed) {
                Trolley newTrolley = new Trolley("T" + (trolleys.size() + 1), trolleyCapacity);
                if (newTrolley.canFit(item)) {
                    newTrolley.addItem(item);
                    trolleys.add(newTrolley);
                    placed = true;
                } else if (allowSplitting && newTrolley.canFitPartial(item, 1)) {
                    int maxQuantity = newTrolley.getMaxQuantityThatFits(item);
                    if (maxQuantity > 0) {
                        int remaining = newTrolley.addPartialItem(item, maxQuantity);
                        trolleys.add(newTrolley);
                        if (remaining > 0) {
                            PickItem remainingItem = item.withQuantity(remaining);
                            items.add(i + 1, remainingItem);
                        }
                        placed = true;
                    }
                }
            }

            if (!placed) {
                skippedItems.add("SKIPPED DUE TO CAPACITY: " + item);
            }
        }

        return new PickPlan(trolleys, skippedItems, "First Fit");
    }

    /**
     * First Fit Decreasing (FFD): Sort items by weight (largest first).
     */
    private PickPlan createFirstFitDecreasingPlan(List<PickItem> items, double trolleyCapacity, boolean allowSplitting) {
        List<PickItem> sortedItems = items.stream()
                .sorted((a, b) -> Double.compare(b.getWeight(), a.getWeight()))
                .collect(Collectors.toList());

        return createFirstFitPlan(sortedItems, trolleyCapacity, allowSplitting);
    }

    /**
     * Best Fit Decreasing (BFD): Sort items by weight (largest first) and place in trolley with smallest remaining capacity.
     */
    private PickPlan createBestFitDecreasingPlan(List<PickItem> items, double trolleyCapacity, boolean allowSplitting) {
        List<Trolley> trolleys = new ArrayList<>();
        List<String> skippedItems = new ArrayList<>();

        // Sort by weight descending
        List<PickItem> sortedItems = items.stream()
                .sorted((a, b) -> Double.compare(b.getWeight(), a.getWeight()))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedItems.size(); i++) {
            PickItem item = sortedItems.get(i);
            Trolley bestTrolley = null;
            double bestRemainingCapacity = Double.MAX_VALUE;

            // Find trolley with smallest remaining capacity that can fit
            for (Trolley trolley : trolleys) {
                if (trolley.canFit(item)) {
                    double remainingCapacity = trolley.getRemainingCapacity() - item.getWeight();
                    if (remainingCapacity < bestRemainingCapacity) {
                        bestRemainingCapacity = remainingCapacity;
                        bestTrolley = trolley;
                    }
                } else if (allowSplitting && trolley.canFitPartial(item, 1)) {
                    int maxQuantity = trolley.getMaxQuantityThatFits(item);
                    if (maxQuantity > 0) {
                        double partialWeight = (item.getWeight() / item.getQuantity()) * maxQuantity;
                        double remainingCapacity = trolley.getRemainingCapacity() - partialWeight;
                        if (remainingCapacity < bestRemainingCapacity) {
                            bestRemainingCapacity = remainingCapacity;
                            bestTrolley = trolley;
                        }
                    }
                }
            }

            boolean placed = false;

            if (bestTrolley != null) {
                if (bestTrolley.canFit(item)) {
                    bestTrolley.addItem(item);
                    placed = true;
                } else if (allowSplitting && bestTrolley.canFitPartial(item, 1)) {
                    int maxQuantity = bestTrolley.getMaxQuantityThatFits(item);
                    if (maxQuantity > 0) {
                        int remaining = bestTrolley.addPartialItem(item, maxQuantity);
                        if (remaining > 0) {
                            PickItem remainingItem = item.withQuantity(remaining);
                            sortedItems.add(i + 1, remainingItem);
                        }
                        placed = true;
                    }
                }
            }

            // If not placed, create a new trolley
            if (!placed) {
                Trolley newTrolley = new Trolley("T" + (trolleys.size() + 1), trolleyCapacity);
                if (newTrolley.canFit(item)) {
                    newTrolley.addItem(item);
                    trolleys.add(newTrolley);
                    placed = true;
                } else if (allowSplitting && newTrolley.canFitPartial(item, 1)) {
                    int maxQuantity = newTrolley.getMaxQuantityThatFits(item);
                    if (maxQuantity > 0) {
                        int remaining = newTrolley.addPartialItem(item, maxQuantity);
                        trolleys.add(newTrolley);
                        if (remaining > 0) {
                            PickItem remainingItem = item.withQuantity(remaining);
                            sortedItems.add(i + 1, remainingItem);
                        }
                        placed = true;
                    }
                }
            }

            if (!placed) {
                skippedItems.add("SKIPPED DUE TO CAPACITY: " + item);
            }
        }

        return new PickPlan(trolleys, skippedItems, "Best Fit Decreasing");
    }

    /**
     * Converts allocation rows to pick items.
     */
    private List<PickItem> convertAllocationsToPickItems(List<AllocationRow> allocations) {
        List<PickItem> pickItems = new ArrayList<>();

        for (AllocationRow allocation : allocations) {
            double weightPerUnit = 1.0; // Default weight per unit (should be configurable)
            double totalWeight = allocation.getQty() * weightPerUnit;

            PickItem pickItem = new PickItem(
                    allocation.getOrderId(),
                    allocation.getLineNo(),
                    allocation.getSku(),
                    allocation.getQty(),
                    totalWeight,
                    allocation.getBoxId(),
                    allocation.getAisle(),
                    allocation.getBay()
            );

            pickItems.add(pickItem);
        }

        return pickItems;
    }
}
