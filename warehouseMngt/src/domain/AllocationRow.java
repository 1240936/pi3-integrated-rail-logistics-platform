package domain;

/**

 Represents a concrete allocation fragment from a specific box for an order line.*/
public class AllocationRow {
    private final String orderId;
    private final int lineNo;
    private final String sku;
    private final int qty;
    private final String boxId;
    private final int aisle;
    private final int bay;

    public AllocationRow(String orderId, int lineNo, String sku, int qty, String boxId, int aisle, int bay) {
        this.orderId = orderId;
        this.lineNo = lineNo;
        this.sku = sku;
        this.qty = qty;
        this.boxId = boxId;
        this.aisle = aisle;
        this.bay = bay;
    }

    public String getOrderId() { return orderId; }
    public int getLineNo() { return lineNo; }
    public String getSku() { return sku; }
    public int getQty() { return qty; }
    public String getBoxId() { return boxId; }
    public int getAisle() { return aisle; }
    public int getBay() { return bay; }
}