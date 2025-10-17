package domain;

/**

 Enumeration of available packing heuristics for trolley assignment.*/
public enum PackingHeuristic {
    FIRST_FIT("First Fit"),
    FIRST_FIT_DECREASING("First Fit Decreasing"),
    BEST_FIT_DECREASING("Best Fit Decreasing");

    private final String displayName;

    PackingHeuristic(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}