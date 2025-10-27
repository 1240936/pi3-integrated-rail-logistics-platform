package main.domain;

/**
 * Represents an item to be picked for a specific order line allocation.
 * Contains information about the order, SKU, quantity, weight, and location.
 */
public class PickItem {
    private final String orderId;
    private final int lineNo;
    private final String sku;
    private final int quantity;
    private final double weight;
    private final String boxId;
    private final int aisle;
    private final int bay;

    /**
     * Constructs a PickItem for a specific order line and box location.
     *
     * @param orderId the ID of the order
     * @param lineNo the line number within the order
     * @param sku the SKU of the item
     * @param quantity quantity of units to pick
     * @param weight total weight of this pick item
     * @param boxId ID of the box containing the item
     * @param aisle aisle number where the box is located
     * @param bay bay number where the box is located
     */
    public PickItem(String orderId, int lineNo, String sku, int quantity, double weight,
                    String boxId, int aisle, int bay) {
        this.orderId = orderId;
        this.lineNo = lineNo;
        this.sku = sku;
        this.quantity = quantity;
        this.weight = weight;
        this.boxId = boxId;
        this.aisle = aisle;
        this.bay = bay;
    }

    /** @return the order ID this pick item belongs to */
    public String getOrderId() {
        return orderId;
    }

    /** @return the line number of the order */
    public int getLineNo() {
        return lineNo;
    }

    /** @return the SKU of the item */
    public String getSku() {
        return sku;
    }

    /** @return the quantity of units to pick */
    public int getQuantity() {
        return quantity;
    }

    /** @return total weight of this pick item */
    public double getWeight() {
        return weight;
    }

    /** @return ID of the box containing the item */
    public String getBoxId() {
        return boxId;
    }

    /** @return aisle number of the box location */
    public int getAisle() {
        return aisle;
    }

    /** @return bay number of the box location */
    public int getBay() {
        return bay;
    }

    /**
     * Creates a new PickItem instance with a different quantity.
     * The weight is scaled proportionally to the new quantity.
     *
     * @param newQuantity the new quantity for the pick item
     * @return a new PickItem instance with updated quantity and weight
     */
    public PickItem withQuantity(int newQuantity) {
        double newWeight = (weight / quantity) * newQuantity;
        return new PickItem(orderId, lineNo, sku, newQuantity, newWeight, boxId, aisle, bay);
    }

    /**
     * Returns a string representation of the pick item including order line, SKU, quantity, and weight.
     *
     * @return string representation of the pick item
     */
    @Override
    public String toString() {
        return orderId + "#" + lineNo + " " + sku + " (qty=" + quantity + ", weight=" + weight + "kg)";
    }
}
