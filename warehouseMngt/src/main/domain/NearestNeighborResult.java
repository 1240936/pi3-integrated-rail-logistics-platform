package main.domain;

import java.util.List;

/**
 * Result of a nearest-N neighbor search.
 * Contains the list of nearest stations with their distances and complexity metrics.
 */
public class NearestNeighborResult {
    private final List<StationWithDistance> neighbors;
    private final int exploredNodes;
    private final int totalStations;

    /**
     * Constructs a nearest neighbor result.
     *
     * @param neighbors list of nearest stations with their distances (sorted by distance, closest first)
     * @param exploredNodes number of tree nodes explored during the search
     * @param totalStations total number of stations in the tree
     */
    public NearestNeighborResult(List<StationWithDistance> neighbors, int exploredNodes, int totalStations) {
        this.neighbors = neighbors;
        this.exploredNodes = exploredNodes;
        this.totalStations = totalStations;
    }

    /**
     * Returns the list of nearest stations with their distances.
     *
     * @return list of stations with distances, sorted by distance (closest first)
     */
    public List<StationWithDistance> getNeighbors() {
        return neighbors;
    }

    /**
     * Returns the number of tree nodes explored during the search.
     *
     * @return number of explored nodes
     */
    public int getExploredNodes() {
        return exploredNodes;
    }

    /**
     * Returns the total number of stations in the tree.
     *
     * @return total stations
     */
    public int getTotalStations() {
        return totalStations;
    }

    /**
     * Represents a station with its distance from the target point.
     */
    public static class StationWithDistance {
        private final Station station;
        private final double distanceKm;

        /**
         * Constructs a station with distance.
         *
         * @param station the station
         * @param distanceKm distance in kilometers
         */
        public StationWithDistance(Station station, double distanceKm) {
            this.station = station;
            this.distanceKm = distanceKm;
        }

        /**
         * Returns the station.
         *
         * @return station
         */
        public Station getStation() {
            return station;
        }

        /**
         * Returns the distance in kilometers.
         *
         * @return distance in km
         */
        public double getDistanceKm() {
            return distanceKm;
        }
    }
}
