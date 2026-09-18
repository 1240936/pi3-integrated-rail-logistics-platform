package main.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a trolley/cart used for picking items in the warehouse.
 * Each trolley has a weight capacity that must be respected.
 */
public class Trolley {
    private final String trolleyId;
    private final double maxWeightCapacity;
    private double currentWeight;
    private final List<PickItem> items;
    private final List<String> logs;

    /**
     * Constructs a trolley with a given ID and maximum weight capacity.
     * @param trolleyId unique identifier of the trolley
     * @param maxWeightCapacity maximum weight the trolley can carry
     */
    public Trolley(String trolleyId, double maxWeightCapacity) {
        this.trolleyId = trolleyId;
        this.maxWeightCapacity = maxWeightCapacity;
        this.currentWeight = 0.0;
        this.items = new ArrayList<>();
        this.logs = new ArrayList<>();
    }

    public String getTrolleyId() {
        return trolleyId;
    }

    public double getMaxWeightCapacity() {
        return maxWeightCapacity;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public double getRemainingCapacity() {
        return maxWeightCapacity - currentWeight;
    }

    public List<PickItem> getItems() {
        return new ArrayList<>(items);
    }

    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    /**
     * Adds an item to this trolley if it fits entirely.
     * @param item the item to add
     * @return true if the item was added, false otherwise
     */
    public boolean addItem(PickItem item) {
        if (canFit(item)) {
            items.add(item);
            currentWeight += item.getWeight();
            logs.add("Added " + item.getSku() + " (qty=" + item.getQuantity() + ", weight=" + item.getWeight() + ")");
            return true;
        }
        return false;
    }

    /**
     * Adds a partial quantity of an item to the trolley if it can fit.
     * @param item the item to add partially
     * @param partialQuantity the quantity to add
     * @return the remaining quantity that could not fit
     */
    public int addPartialItem(PickItem item, int partialQuantity) {
        if (partialQuantity <= 0) return item.getQuantity();

        double partialWeight = (item.getWeight() / item.getQuantity()) * partialQuantity;
        PickItem partialItem = new PickItem(
                item.getOrderId(), item.getLineNo(), item.getSku(),
                partialQuantity, partialWeight, item.getBoxId(),
                item.getAisle(), item.getBay()
        );

        if (addItem(partialItem)) {
            logs.add("PARTIAL ALLOCATION: " + item.getSku() + " (qty=" + partialQuantity + "/" + item.getQuantity() + ")");
            return item.getQuantity() - partialQuantity;
        }
        return item.getQuantity();
    }

    /**
     * Checks if an item can fully fit in the trolley.
     * @param item the item to check
     * @return true if the item fits, false otherwise
     */
    public boolean canFit(PickItem item) {
        return currentWeight + item.getWeight() <= maxWeightCapacity;
    }

    /**
     * Checks if a partial quantity of an item can fit in the trolley.
     * @param item the item to check
     * @param quantity quantity to check
     * @return true if the quantity fits, false otherwise
     */
    public boolean canFitPartial(PickItem item, int quantity) {
        if (quantity <= 0) return false;
        double partialWeight = (item.getWeight() / item.getQuantity()) * quantity;
        return currentWeight + partialWeight <= maxWeightCapacity;
    }

    /**
     * Computes the maximum quantity of an item that can fit in the trolley.
     * @param item the item to check
     * @return maximum quantity that fits
     */
    public int getMaxQuantityThatFits(PickItem item) {
        if (item.getWeight() <= 0) return item.getQuantity();
        double remainingCapacity = getRemainingCapacity();
        return (int) Math.floor((remainingCapacity / item.getWeight()) * item.getQuantity());
    }

    /** @return true if the trolley contains no items */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public String toString() {
        return "Trolley " + trolleyId + " (weight: " + currentWeight + "/" + maxWeightCapacity + " kg)";
    }
}
