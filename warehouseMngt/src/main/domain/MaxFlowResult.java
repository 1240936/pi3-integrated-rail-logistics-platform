package main.domain;

/**
 * Encapsulates the result of computing maximum flow between two stations.
 * USEI14: Contains source, sink, and maximum flow value.
 */
public class MaxFlowResult {
    private final Station source;
    private final Station sink;
    private final double maxFlowValue;

    /**
     * Constructs a MaxFlowResult.
     *
     * @param source source station
     * @param sink sink station
     * @param maxFlowValue maximum flow value (trains per day)
     */
    public MaxFlowResult(Station source, Station sink, double maxFlowValue) {
        this.source = source;
        this.sink = sink;
        this.maxFlowValue = maxFlowValue;
    }

    public Station getSource() {
        return source;
    }

    public Station getSink() {
        return sink;
    }

    public double getMaxFlowValue() {
        return maxFlowValue;
    }

    @Override
    public String toString() {
        return String.format(
                "Maximum Flow Result:\n" +
                        "  Source: %s\n" +
                        "  Sink: %s\n" +
                        "  Maximum Flow: %.2f trains/day",
                source.getName(),
                sink.getName(),
                maxFlowValue
        );
    }
}