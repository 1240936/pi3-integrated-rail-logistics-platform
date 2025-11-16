package main.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Node structure for 2D-tree (KD-tree with k=2).
 * Stores stations at a point (latitude, longitude).
 * Multiple stations at the same coordinates are stored in a sorted list by name.
 */
public class KD2DNode {
    private final double latitude;
    private final double longitude;
    private List<Station> stations; // stations at this exact coordinate, sorted by name
    private KD2DNode left; // left child (or "below" when splitting by latitude)
    private KD2DNode right; // right child (or "above" when splitting by latitude)
    private boolean splitByLatitude; // true if this level splits by latitude, false for longitude

    /**
     * Constructs a 2D-tree node with a station.
     * The node will contain a list of stations (starting with this one).
     *
     * @param station station to store
     * @param splitByLatitude whether this level splits by latitude (true) or longitude (false)
     */
    public KD2DNode(Station station, boolean splitByLatitude) {
        this.latitude = station.getLatitude();
        this.longitude = station.getLongitude();
        this.stations = new ArrayList<>();
        this.stations.add(station);
        this.left = null;
        this.right = null;
        this.splitByLatitude = splitByLatitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<Station> getStations() {
        return stations;
    }

    public void addStation(Station station) {
        stations.add(station);
        // Keep stations sorted by name
        stations.sort(Comparator.comparing(Station::getName));
    }

    public KD2DNode getLeft() {
        return left;
    }

    public void setLeft(KD2DNode left) {
        this.left = left;
    }

    public KD2DNode getRight() {
        return right;
    }

    public void setRight(KD2DNode right) {
        this.right = right;
    }

    public boolean isSplitByLatitude() {
        return splitByLatitude;
    }

    public void setSplitByLatitude(boolean splitByLatitude) {
        this.splitByLatitude = splitByLatitude;
    }

    /**
     * Returns the number of stations stored at this node.
     *
     * @return number of stations
     */
    public int getStationCount() {
        return stations.size();
    }
}

