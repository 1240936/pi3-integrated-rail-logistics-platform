package domain;

/*

Represents an item to be picked for a specific order line allocation.
Contains information about the order, SKU, quantity, weight, and location.*/
public class PickItem {
    private final String orderId;
    private final int lineNo;
    private final String sku;
    private final int quantity;
    private final double weight;
    private final String boxId;
    private final int aisle;
    private final int bay;

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

    public String getOrderId() {
        return orderId;
    }

    public int getLineNo() {
        return lineNo;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getWeight() {
        return weight;
    }

    public String getBoxId() {
        return boxId;
    }

    public int getAisle() {
        return aisle;
    }

    public int getBay() {
        return bay;
    }

    /*

    Creates a new PickItem with a different quantity.*/
    public PickItem withQuantity(int newQuantity) {
        double newWeight = (weight / quantity) * newQuantity;
        return new PickItem(orderId, lineNo, sku, newQuantity, newWeight, boxId, aisle, bay);}

    @Override
    public String toString() {
        return orderId + "#" + lineNo + " " + sku + " (qty=" + quantity + ", weight=" + weight + "kg)";
    }
}