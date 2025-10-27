package main.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A storage location within a warehouse aisle that holds boxes of goods.
 * Boxes are kept sorted according to a FEFO comparator to support efficient dispatch.
 */
public class Bay {
    private final String warehouseId;
    private final int aisle;
    private final int bayNumber;
    private final List<Box> boxes;
    private final Comparator<Box> fefoComparator;
    private int capacityBoxes; // number of boxes that can be stored in this bay

    /**
     * Creates a bay identified by warehouse, aisle and bay number.
     * Capacity defaults to 0 (meaning unconfigured/unlimited until set).
     *
     * @param warehouseId identifier of the warehouse
     * @param aisle aisle number
     * @param bayNumber bay number
     * @param fefoComparator comparator used to order boxes by FEFO
     */
    public Bay(String warehouseId, int aisle, int bayNumber, Comparator<Box> fefoComparator) {
        this.warehouseId = warehouseId;
        this.aisle = aisle;
        this.bayNumber = bayNumber;
        this.boxes = new ArrayList<>();
        this.fefoComparator = fefoComparator;
        this.capacityBoxes = 0; // default, unconfigured
    }

    /**
     * Returns the warehouse ID where this bay is located.
     *
     * @return warehouse identifier
     */
    public String getWarehouseId() {
        return warehouseId;
    }

    /**
     * Returns the aisle number of this bay.
     *
     * @return aisle number
     */
    public int getAisle() {
        return aisle;
    }

    /**
     * Returns the bay number within the aisle.
     *
     * @return bay number
     */
    public int getBayNumber() {
        return bayNumber;
    }

    /**
     * Returns the list of boxes currently stored in this bay.
     *
     * @return list of boxes
     */
    public List<Box> getBoxes() {
        return boxes;
    }

    /**
     * Returns the maximum number of boxes this bay can hold.
     * A value of 0 means unlimited/unconfigured.
     *
     * @return capacity in boxes
     */
    public int getCapacityBoxes() {
        return capacityBoxes;
    }

    /**
     * Sets the maximum number of boxes this bay can hold.
     *
     * @param capacityBoxes maximum number of boxes
     */
    public void setCapacityBoxes(int capacityBoxes) {
        this.capacityBoxes = capacityBoxes;
    }

    /**
     * Checks whether there is space available to store additional boxes.
     * A capacity of 0 is treated as unlimited/unconfigured.
     *
     * @return true if additional boxes can be stored
     */
    public boolean hasSpace() {
        return capacityBoxes <= 0 || boxes.size() < capacityBoxes;
    }

    /**
     * Inserts a box into the bay preserving FEFO ordering.
     * Uses binary search to find the correct position, and keeps stable ordering
     * if boxes are equal according to the FEFO comparator.
     *
     * @param box box to insert
     */
    public void insertBoxFefo(Box box) {
        BoxFefoComparator fefoComparator  = new BoxFefoComparator();
        int pos = Collections.binarySearch(boxes, box, fefoComparator);
        if (pos < 0) {
            pos = -pos - 1;
        } else {
            // Insert after equal boxes to maintain stable ordering
            while (pos < boxes.size() && fefoComparator.compare(boxes.get(pos), box) == 0) {
                pos++;
            }
        }
        boxes.add(pos, box);
    }

    /**
     * Removes the specified box if its quantity is zero or below.
     * Returns the removed box, or null if the box was not removed.
     *
     * @param box box to check and remove
     * @return removed box or null if not removed
     */
    public Box removeFrontBoxIfEmpty(Box box) {
        if (box.getQuantity() <= 0) {
            boxes.remove(box);
            return box;
        }
        return null;
    }
}
