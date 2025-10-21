package main.domain;

import java.time.LocalDate;

/**
 * Represents a single order line request to allocate.
 */
public class OrderLine {
    private final String orderId;
    private final int lineNo;
    private final String sku;
    private final int requestedQty;
    private final int priority; // lower is higher priority
    private final LocalDate dueDate;

    public OrderLine(String orderId, int lineNo, String sku, int requestedQty, int priority, LocalDate dueDate) {
        this.orderId = orderId;
        this.lineNo = lineNo;
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public String getOrderId() { return orderId; }
    public int getLineNo() { return lineNo; }
    public String getSku() { return sku; }
    public int getRequestedQty() { return requestedQty; }
    public int getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
}


