package main.controller;

import main.domain.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for creating picking plans using different packing heuristics.
 * Handles trolley assignment with weight capacity constraints and provides
 * pick path optimization strategies.
 */
public class PickingService {

    /**
     * Creates a pick plan from allocation results using the specified packing heuristic.
     *
     * @param allocations list of allocated inventory rows
     * @param heuristic algorithm strategy for packing items into trolleys
     * @param trolleyCapacity maximum weight each trolley can hold
     * @param allowSplitting if true, splits a pick item across multiple trolleys if needed
     * @return pick plan including trolleys and any skipped items
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
     * First Fit packing strategy.
     * Attempts to place a pick item in the first currently available trolley where it fits.
     * Optionally splits pick items if configured.
     *
     * @param items pick items to place into trolleys
     * @param trolleyCapacity allowed maximum weight
     * @param allowSplitting if true, permits splitting pick items across trolleys
     * @return resulting pick plan with filled and skipped items
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
     * First Fit Decreasing packing strategy.
     * Sorts items by descending weight before applying First Fit.
     *
     * @param items list of pick items to pack
     * @param trolleyCapacity allowed maximum weight
     * @param allowSplitting permit partial allocations if needed
     * @return pick plan with allocation results
     */
    private PickPlan createFirstFitDecreasingPlan(List<PickItem> items, double trolleyCapacity, boolean allowSplitting) {
        List<PickItem> sortedItems = items.stream()
                .sorted((a, b) -> Double.compare(b.getWeight(), a.getWeight()))
                .collect(Collectors.toList());

        return createFirstFitPlan(sortedItems, trolleyCapacity, allowSplitting);
    }

    /**
     * Best Fit Decreasing packing strategy.
     * Sorts items by descending weight and places them into the trolley that would
     * have the least remaining capacity afterward.
     *
     * @param items list of pick items
     * @param trolleyCapacity allowed maximum weight
     * @param allowSplitting if true, support splitting pick items across trolleys
     * @return pick plan produced by the heuristic
     */
    private PickPlan createBestFitDecreasingPlan(List<PickItem> items, double trolleyCapacity, boolean allowSplitting) {
        List<Trolley> trolleys = new ArrayList<>();
        List<String> skippedItems = new ArrayList<>();

        List<PickItem> sortedItems = items.stream()
                .sorted((a, b) -> Double.compare(b.getWeight(), a.getWeight()))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedItems.size(); i++) {
            PickItem item = sortedItems.get(i);
            Trolley bestTrolley = null;
            double bestRemainingCapacity = Double.MAX_VALUE;

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
     * Converts allocation rows into pickable items with weight assignment.
     * Default per-unit weight currently set to 1.0.
     *
     * @param allocations the allocation results to convert
     * @return converted list of pick items
     */
    private List<PickItem> convertAllocationsToPickItems(List<AllocationRow> allocations) {
        List<PickItem> pickItems = new ArrayList<>();

        for (AllocationRow allocation : allocations) {
            double weightPerUnit = 1.0;
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

    /**
     * Computes optimal pick path using Strategy A: Deterministic Sweep.
     *
     * @param pickPlan pick plan to optimize
     * @return optimal traversal result for picking
     */
    public PickSequenceResult computeDeterministicSweep(PickPlan pickPlan) {
        PickPathService pathService = new PickPathService();
        return pathService.computeDeterministicSweep(pickPlan);
    }

    /**
     * Computes optimal pick path using Strategy B: Nearest Neighbour (Greedy).
     *
     * @param pickPlan pick plan to optimize
     * @return traversal result using nearest neighbor logic
     */
    public PickSequenceResult computeNearestNeighbor(PickPlan pickPlan) {
        PickPathService pathService = new PickPathService();
        return pathService.computeNearestNeighbor(pickPlan);
    }
}
