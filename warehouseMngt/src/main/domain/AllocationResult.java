package main.domain;

import java.util.List;

/**
 * Represents the outcome of an allocation process including both
 * SKU eligibility results and specific allocated quantities.
 */
public class AllocationResult {
    private final List<LineEligibility> eligibilities;
    private final List<AllocationRow> allocations;

    /**
     * Constructs an allocation result with associated eligibility information and allocation rows.
     *
     * @param eligibilities list describing which order lines are eligible for allocation
     * @param allocations list of rows detailing how items were allocated
     */
    public AllocationResult(List<LineEligibility> eligibilities, List<AllocationRow> allocations) {
        this.eligibilities = eligibilities;
        this.allocations = allocations;
    }

    /**
     * Retrieves the eligibility results for each order line.
     *
     * @return list of line eligibility objects
     */
    public List<LineEligibility> getEligibilities() {
        return eligibilities;
    }

    /**
     * Retrieves detailed allocation information including item quantities and placement.
     *
     * @return list of allocation rows
     */
    public List<AllocationRow> getAllocations() {
        return allocations;
    }
}
