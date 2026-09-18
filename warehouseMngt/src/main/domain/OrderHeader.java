package main.domain;

import java.time.LocalDate;

/**
 * Represents an order header row from orders.csv, containing order metadata.
 * Each header includes the order ID, due date, and priority for allocation.
 */
public class OrderHeader {
    /** Unique identifier for the order. */
    private final String orderId;

    /** Due date of the order for scheduling and prioritization. */
    private final LocalDate dueDate;

    /** Priority of the order; lower numbers indicate higher priority. */
    private final int priority;

    /**
     * Constructs an OrderHeader with given order ID, due date, and priority.
     *
     * @param orderId unique identifier of the order
     * @param dueDate expected fulfillment date
     * @param priority numeric priority (lower = higher priority)
     */
    public OrderHeader(String orderId, LocalDate dueDate, int priority) {
        this.orderId = orderId;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    /** @return the unique order ID */
    public String getOrderId() { return orderId; }

    /** @return the order's due date */
    public LocalDate getDueDate() { return dueDate; }

    /** @return the order's priority (lower value = higher priority) */
    public int getPriority() { return priority; }
}
