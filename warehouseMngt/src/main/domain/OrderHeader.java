package main.domain;

import java.time.LocalDate;

/**
 * Represents an order header row from orders.csv
 */
public class OrderHeader {
    private final String orderId;
    private final LocalDate dueDate;
    private final int priority; // lower is higher priority

    public OrderHeader(String orderId, LocalDate dueDate, int priority) {
        this.orderId = orderId;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    public String getOrderId() { return orderId; }
    public LocalDate getDueDate() { return dueDate; }
    public int getPriority() { return priority; }
}


