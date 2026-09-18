package main.domain;

/**
 * Enumeration of possible reasons for product returns.
 * Each reason determines how the return should be processed during inspection.
 */
public enum ReturnReason {
    /**
     * Customer returned item even though it's in good condition.
     * These items can often be restocked.
     */
    CUSTOMER_REMORSE,
    
    /**
     * Product is physically broken or unusable.
     * These items must be discarded.
     */
    DAMAGED,
    
    /**
     * Product has passed its expiry date.
     * These items cannot be sold and must be discarded.
     */
    EXPIRED,
    
    /**
     * Mismatch found during stock audit.
     * These items are not faulty but must be rechecked before restocking.
     */
    CYCLE_COUNT
}
