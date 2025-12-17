package main.domain;

/**
 * Represents the result of hub centrality analysis for a station.
 * Contains all centrality measures and the composite HubScore.
 */
public class HubScoreResult {
    private final String stationId;
    private final String stationName;
    private final int degree;
    private final double strength;
    private final double betweenness;
    private final double harmonicCloseness;
    private final double hubScore;

    /**
     * Constructs a hub score result.
     *
     * @param stationId station ID
     * @param stationName station name
     * @param degree degree centrality (number of connections)
     * @param strength strength centrality (sum of edge weights)
     * @param betweenness betweenness centrality (normalized to [0,1])
     * @param harmonicCloseness harmonic closeness centrality (normalized to [0,1])
     * @param hubScore composite hub score (normalized to [0,1])
     */
    public HubScoreResult(String stationId, String stationName, int degree, double strength,
                         double betweenness, double harmonicCloseness, double hubScore) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.degree = degree;
        this.strength = strength;
        this.betweenness = betweenness;
        this.harmonicCloseness = harmonicCloseness;
        this.hubScore = hubScore;
    }

    public String getStationId() {
        return stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public int getDegree() {
        return degree;
    }

    public double getStrength() {
        return strength;
    }

    public double getBetweenness() {
        return betweenness;
    }

    public double getHarmonicCloseness() {
        return harmonicCloseness;
    }

    public double getHubScore() {
        return hubScore;
    }

    @Override
    public String toString() {
        return String.format("HubScoreResult{id=%s, name=%s, degree=%d, strength=%.2f, " +
                "betweenness=%.4f, harmonicCloseness=%.4f, hubScore=%.4f}",
                stationId, stationName, degree, strength, betweenness, harmonicCloseness, hubScore);
    }
}


