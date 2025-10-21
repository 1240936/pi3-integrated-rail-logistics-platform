package main.domain;

import java.util.List;

/**
 * Container for allocation planning results: line eligibilities and allocation rows.
 */
public class AllocationResult {
    private final List<LineEligibility> eligibilities;
    private final List<AllocationRow> allocations;

    public AllocationResult(List<LineEligibility> eligibilities, List<AllocationRow> allocations) {
        this.eligibilities = eligibilities;
        this.allocations = allocations;
    }

    public List<LineEligibility> getEligibilities() { return eligibilities; }
    public List<AllocationRow> getAllocations() { return allocations; }
}

