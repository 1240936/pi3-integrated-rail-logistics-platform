package main.domain;

/**
 * Represents the allocation eligibility status of an order line.
 */
public enum LineStatus {
    /** The order line can be fully fulfilled from available inventory. */
    ELIGIBLE,

    /** Only part of the requested quantity can be allocated. */
    PARTIAL,

    /** The order line cannot be allocated at all from current inventory. */
    UNDISPATCHABLE
}
