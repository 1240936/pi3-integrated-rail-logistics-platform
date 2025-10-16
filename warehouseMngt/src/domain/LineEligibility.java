package domain;

/**
 * Eligibility result per order line.
 */
public class LineEligibility {
    private final String orderId;
    private final int lineNo;
    private final String sku;
    private final int requestedQty;
    private final int allocatedQty;
    private final LineStatus status;

    public LineEligibility(String orderId, int lineNo, String sku, int requestedQty, int allocatedQty, LineStatus status) {
        this.orderId = orderId;
        this.lineNo = lineNo;
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.allocatedQty = allocatedQty;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public int getLineNo() { return lineNo; }
    public String getSku() { return sku; }
    public int getRequestedQty() { return requestedQty; }
    public int getAllocatedQty() { return allocatedQty; }
    public LineStatus getStatus() { return status; }
}


