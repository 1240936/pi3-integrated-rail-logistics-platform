package main.domain;

import java.time.LocalDate;

/**
 * Represents a single line item in an order that requires allocation.
 * Contains the SKU, requested quantity, priority, and due date.
 */
public class OrderLine {
    /** Identifier of the order this line belongs to. */
    private final String orderId;

    /** Line number within the order for identification. */
    private final int lineNo;

    /** SKU of the item requested in this line. */
    private final String sku;

    /** Quantity requested for allocation. */
    private final int requestedQty;

    /** Priority of the line; lower numbers indicate higher priority. */
    private final int priority;

    /** Due date for this line item to be fulfilled. */
    private final LocalDate dueDate;

    /**
     * Constructs an OrderLine with the specified details.
     *
     * @param orderId identifier of the order
     * @param lineNo line number within the order
     * @param sku SKU of the item
     * @param requestedQty quantity requested
     * @param priority priority of the line (lower = higher priority)
     * @param dueDate due date for fulfillment
     */
    public OrderLine(String orderId, int lineNo, String sku, int requestedQty, int priority, LocalDate dueDate) {
        this.orderId = orderId;
        this.lineNo = lineNo;
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    /** @return the order ID this line belongs to */
    public String getOrderId() { return orderId; }

    /** @return the line number of this order line */
    public int getLineNo() { return lineNo; }

    /** @return the SKU of the requested item */
    public String getSku() { return sku; }

    /** @return the requested quantity */
    public int getRequestedQty() { return requestedQty; }

    /** @return the priority of this line (lower = higher priority) */
    public int getPriority() { return priority; }

    /** @return the due date for this line */
    public LocalDate getDueDate() { return dueDate; }
}
