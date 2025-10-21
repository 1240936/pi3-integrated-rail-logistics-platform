package main.domain;

/**
 * Allocation planning mode:
 * - STRICT: a line is ELIGIBLE only if fully allocated; otherwise UNDISPATCHABLE and allocations are discarded
 * - PARTIAL: keep partial allocations and mark PARTIAL when 0 < allocated < requested
 */
public enum AllocationMode {
    STRICT,
    PARTIAL
}


