package wms.model;

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
    private int capacityBoxes; // number of boxes that can be stored in this bay

    /**
     * Creates a bay identified by warehouse, aisle and bay number.
     * Capacity defaults to 0 (meaning unconfigured/unlimited until set).
     */
    public Bay(String warehouseId, int aisle, int bayNumber, Comparator<Box> fefoComparator) {
        this.warehouseId = warehouseId;
        this.aisle = aisle;
        this.bayNumber = bayNumber;
        this.boxes = new ArrayList<>();
        this.capacityBoxes = 0; // default until defined
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getAisle() {
        return aisle;
    }

    public int getBayNumber() {
        return bayNumber;
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    public int getCapacityBoxes() {
        return capacityBoxes;
    }

    public void setCapacityBoxes(int capacityBoxes) {
        this.capacityBoxes = capacityBoxes;
    }

    /**
     * Returns true if the bay can accept more boxes. A capacity of 0 is
     * treated as not yet configured (unlimited) and therefore considered to have space.
     */
    public boolean hasSpace() {
        return capacityBoxes <= 0 || boxes.size() < capacityBoxes; // capacity 0 means not configured yet
    }


    /**
     * Removes the specified box if its quantity is zero or below.
     * Returns the removed box, or null if it was not removed.
     */
    public Box removeFrontBoxIfEmpty(Box box) {
        if (box.getQuantity() <= 0) {
            boxes.remove(box);
            return box;
        }
        return null;
    }
}