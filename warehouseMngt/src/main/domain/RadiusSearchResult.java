package main.domain;

import java.util.Map;

/**
 * Result of a radius search operation (USEI10).
 * Contains the AVL tree of stations sorted by distance (ASC) and name (DESC),
 * along with summary statistics by country and by isCity.
 */
public class RadiusSearchResult {
    private final AVL<StationWithDistanceComparable> resultTree;
    private final Map<String, Integer> summaryByCountry;
    private final Map<Boolean, Integer> summaryByIsCity;
    private final int totalStations;
    private final double radiusKm;
    private final double targetLat;
    private final double targetLon;

    /**
     * Constructs a radius search result.
     *
     * @param resultTree AVL tree sorted by distance (ASC) and station name (DESC)
     * @param summaryByCountry map of country -> count
     * @param summaryByIsCity map of isCity -> count
     * @param totalStations total number of stations found
     * @param radiusKm search radius in kilometers
     * @param targetLat target latitude
     * @param targetLon target longitude
     */
    public RadiusSearchResult(AVL<StationWithDistanceComparable> resultTree,
                              Map<String, Integer> summaryByCountry,
                              Map<Boolean, Integer> summaryByIsCity,
                              int totalStations,
                              double radiusKm,
                              double targetLat,
                              double targetLon) {
        this.resultTree = resultTree;
        this.summaryByCountry = summaryByCountry;
        this.summaryByIsCity = summaryByIsCity;
        this.totalStations = totalStations;
        this.radiusKm = radiusKm;
        this.targetLat = targetLat;
        this.targetLon = targetLon;
    }

    /**
     * Returns the AVL tree of results sorted by distance (ASC) and station name (DESC).
     *
     * @return AVL tree of stations with distance
     */
    public AVL<StationWithDistanceComparable> getResultTree() {
        return resultTree;
    }

    /**
     * Returns summary statistics by country.
     *
     * @return map of country code -> count
     */
    public Map<String, Integer> getSummaryByCountry() {
        return summaryByCountry;
    }

    /**
     * Returns summary statistics by isCity.
     *
     * @return map of isCity (true/false) -> count
     */
    public Map<Boolean, Integer> getSummaryByIsCity() {
        return summaryByIsCity;
    }

    /**
     * Returns the total number of stations found within the radius.
     *
     * @return total count
     */
    public int getTotalStations() {
        return totalStations;
    }

    /**
     * Returns the search radius in kilometers.
     *
     * @return radius in km
     */
    public double getRadiusKm() {
        return radiusKm;
    }

    /**
     * Returns the target latitude.
     *
     * @return latitude
     */
    public double getTargetLat() {
        return targetLat;
    }

    /**
     * Returns the target longitude.
     *
     * @return longitude
     */
    public double getTargetLon() {
        return targetLon;
    }
}
