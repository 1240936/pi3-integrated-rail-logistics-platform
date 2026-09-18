package main.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a customer return that needs to be processed through quarantine.
 * Contains all necessary information for inspection and potential restocking.
 */
public class Return {
    private final String returnId;
    private final String sku;
    private final int quantity;
    private final ReturnReason reason;
    private final LocalDateTime timestamp;
    private final LocalDate expiryDate; // nullable - may be unknown for returns

    /**
     * Creates a new return instance.
     * @param returnId unique identifier for the return
     * @param sku SKU of the returned product
     * @param quantity number of units returned
     * @param reason reason for the return
     * @param timestamp when the return was received
     * @param expiryDate expiry date if known, null otherwise
     */
    public Return(String returnId, String sku, int quantity, ReturnReason reason, 
                  LocalDateTime timestamp, LocalDate expiryDate) {
        this.returnId = returnId;
        this.sku = sku;
        this.quantity = quantity;
        this.reason = reason;
        this.timestamp = timestamp;
        this.expiryDate = expiryDate;
    }

    public String getReturnId() {
        return returnId;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReturnReason getReason() {
        return reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    /**
     * Returns true if this return can potentially be restocked based on its reason.
     */
    public boolean canBeRestocked() {
        return reason == ReturnReason.CUSTOMER_REMORSE || reason == ReturnReason.CYCLE_COUNT;
    }

    /**
     * Returns true if this return should be discarded based on its reason.
     */
    public boolean shouldBeDiscarded() {
        return reason == ReturnReason.DAMAGED || reason == ReturnReason.EXPIRED;
    }
}
