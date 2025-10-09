package wms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a physical box of a specific SKU stored in a warehouse.
 * A box may have an expiry date (nullable) and a received timestamp used for FEFO dispatching.
 */
public class Box {
    private final String boxId;
    private final String sku;
    private final LocalDate expiryDate; // nullable means no expiry
    private final LocalDateTime receivedAt;
    private int quantity; // number of units in the box; dispatch consumes whole boxes in this demo

    private String warehouseId;
    private int aisle;
    private int bay;

    /**
     * Creates a box instance.
     * @param boxId unique identifier of the box within a warehouse
     * @param sku SKU of the product in the box
     * @param expiryDate optional expiry date; null means non-expiring
     * @param receivedAt timestamp when the box was received
     * @param quantity number of units inside the box
     * @param warehouseId warehouse where the box is located
     * @param aisle aisle number where the box resides
     * @param bay bay number within the aisle
     */
    public Box(String boxId,
               String sku,
               LocalDate expiryDate,
               LocalDateTime receivedAt,
               int quantity,
               String warehouseId,
               int aisle,
               int bay) {
        this.boxId = boxId;
        this.sku = sku;
        this.expiryDate = expiryDate;
        this.receivedAt = receivedAt;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
        this.aisle = aisle;
        this.bay = bay;
    }

    public String getBoxId() {
        return boxId;
    }

    public String getSku() {
        return sku;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public int getAisle() {
        return aisle;
    }

    public void setAisle(int aisle) {
        this.aisle = aisle;
    }

    public int getBay() {
        return bay;
    }

    public void setBay(int bay) {
        this.bay = bay;
    }
}