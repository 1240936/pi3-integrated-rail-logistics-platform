package main.domain;

/**
 * Wrapper class to make Station with distance comparable for use with AVL tree.
 * Sorts by distance (ascending), then by station name (descending).
 * Used for USEI10 - Radius search results.
 */
public class StationWithDistanceComparable implements Comparable<StationWithDistanceComparable> {
    private final Station station;
    private final double distanceKm;

    public StationWithDistanceComparable(Station station, double distanceKm) {
        this.station = station;
        this.distanceKm = distanceKm;
    }

    public Station getStation() {
        return station;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    @Override
    public int compareTo(StationWithDistanceComparable other) {
        // First compare by distance (ascending)
        int distanceCmp = Double.compare(this.distanceKm, other.distanceKm);
        if (distanceCmp != 0) {
            return distanceCmp;
        }
        // If distances are equal, compare by station name (descending)
        return other.station.getName().compareTo(this.station.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StationWithDistanceComparable that = (StationWithDistanceComparable) obj;
        return Double.compare(that.distanceKm, distanceKm) == 0 &&
                station.equals(that.station);
    }

    @Override
    public int hashCode() {
        return station.hashCode() * 31 + Double.hashCode(distanceKm);
    }
}