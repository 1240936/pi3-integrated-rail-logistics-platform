package main.domain;

/**
 * Enumeration of available packing heuristics for assigning items to trolleys.
 * Each heuristic defines a different strategy for how pick items are packed:
 * <ul>
 *     <li>{@link #FIRST_FIT} – Place items in the first trolley that can accommodate them.</li>
 *     <li>{@link #FIRST_FIT_DECREASING} – Sort items by descending weight and apply First Fit.</li>
 *     <li>{@link #BEST_FIT_DECREASING} – Sort items by descending weight and place into trolley with least remaining capacity.</li>
 * </ul>
 */
public enum PackingHeuristic {
    /** First Fit heuristic. */
    FIRST_FIT("First Fit"),

    /** First Fit Decreasing heuristic. */
    FIRST_FIT_DECREASING("First Fit Decreasing"),

    /** Best Fit Decreasing heuristic. */
    BEST_FIT_DECREASING("Best Fit Decreasing");

    /** Human-readable name for the heuristic. */
    private final String displayName;

    /**
     * Constructs a packing heuristic with a display name.
     *
     * @param displayName descriptive name of the heuristic
     */
    PackingHeuristic(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name of the heuristic.
     *
     * @return human-readable heuristic name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the display name as the string representation.
     *
     * @return display name of the heuristic
     */
    @Override
    public String toString() {
        return displayName;
    }
}
