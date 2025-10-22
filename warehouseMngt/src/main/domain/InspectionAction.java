package main.domain;

/**
 * Enumeration of possible actions that can be taken during return inspection.
 */
public enum InspectionAction {
    /**
     * Items were restocked back into inventory.
     */
    RESTOCKED,
    
    /**
     * Items were discarded and will not be put back into stock.
     */
    DISCARDED
}
