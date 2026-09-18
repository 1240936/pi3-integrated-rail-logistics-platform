package main.domain;

/**
 * Represents a concrete allocation fragment from a specific box for an order line.
 * Each instance indicates the quantity of a SKU assigned to a particular order line
 * and its storage location in the warehouse.
 */
public class AllocationRow {
    private final String orderId;
    private final int lineNo;
    private final String sku;
    private final int qty;
    private final String boxId;
    private final int aisle;
    private final int bay;

    /**
     * Constructs an allocation row for a specific order line and box.
     *
     * @param orderId identifier of the order
     * @param lineNo line number within the order
     * @param sku SKU of the allocated item
     * @param qty quantity of items allocated from this box
     * @param boxId identifier of the source box
     * @param aisle aisle number where the box is located
     * @param bay bay number where the box is located
     */
    public AllocationRow(String orderId, int lineNo, String sku, int qty, String boxId, int aisle, int bay) {
        this.orderId = orderId;
        this.lineNo = lineNo;
        this.sku = sku;
        this.qty = qty;
        this.boxId = boxId;
        this.aisle = aisle;
        this.bay = bay;
    }

    /**
     * Returns the order ID for this allocation.
     *
     * @return order ID
     */
    public String getOrderId() { return orderId; }

    /**
     * Returns the line number within the order.
     *
     * @return order line number
     */
    public int getLineNo() { return lineNo; }

    /**
     * Returns the SKU of the allocated item.
     *
     * @return item SKU
     */
    public String getSku() { return sku; }

    /**
     * Returns the quantity allocated from the box.
     *
     * @return allocated quantity
     */
    public int getQty() { return qty; }

    /**
     * Returns the ID of the box from which items are allocated.
     *
     * @return box ID
     */
    public String getBoxId() { return boxId; }

    /**
     * Returns the aisle number where the box is stored.
     *
     * @return aisle number
     */
    public int getAisle() { return aisle; }

    /**
     * Returns the bay number where the box is stored.
     *
     * @return bay number
     */
    public int getBay() { return bay; }
}
