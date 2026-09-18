package main.domain;

/**
 * Represents the eligibility and allocation status of a single order line.
 * Captures requested and allocated quantities along with the line's status.
 */
public class LineEligibility {
    private final String orderId;
    private final int lineNo;
    private final String sku;
    private final int requestedQty;
    private final int allocatedQty;
    private final LineStatus status;

    /**
     * Constructs a LineEligibility record.
     *
     * @param orderId identifier of the order
     * @param lineNo line number within the order
     * @param sku SKU of the item
     * @param requestedQty quantity requested in the order line
     * @param allocatedQty quantity allocated to this line
     * @param status eligibility or allocation status of the line
     */
    public LineEligibility(String orderId, int lineNo, String sku, int requestedQty, int allocatedQty, LineStatus status) {
        this.orderId = orderId;
        this.lineNo = lineNo;
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.allocatedQty = allocatedQty;
        this.status = status;
    }

    /**
     * Returns the order ID this line belongs to.
     *
     * @return order ID string
     */
    public String getOrderId() { return orderId; }

    /**
     * Returns the line number within the order.
     *
     * @return line number
     */
    public int getLineNo() { return lineNo; }

    /**
     * Returns the SKU of the item for this order line.
     *
     * @return SKU string
     */
    public String getSku() { return sku; }

    /**
     * Returns the requested quantity for this line.
     *
     * @return requested quantity
     */
    public int getRequestedQty() { return requestedQty; }

    /**
     * Returns the quantity allocated to this line.
     *
     * @return allocated quantity
     */
    public int getAllocatedQty() { return allocatedQty; }

    /**
     * Returns the eligibility or allocation status of this line.
     *
     * @return LineStatus enum value
     */
    public LineStatus getStatus() { return status; }
}
