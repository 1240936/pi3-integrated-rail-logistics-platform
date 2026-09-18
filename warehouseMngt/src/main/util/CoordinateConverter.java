package main.util;

import main.domain.Station;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Converts geographic coordinates (latitude, longitude) to XY coordinates for visualization.
 * Used for positioning vertices in DOT/SVG files.
 */
public class CoordinateConverter {
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final double width;
    private final double height;
    private final Map<Station, double[]> coordinateCache;

    /**
     * Constructs a CoordinateConverter for a list of stations.
     *
     * @param stations list of stations to convert
     * @param width width of the output canvas in points
     * @param height height of the output canvas in points
     */
    public CoordinateConverter(List<Station> stations, double width, double height) {
        if (stations == null || stations.isEmpty()) {
            throw new IllegalArgumentException("Stations list cannot be null or empty");
        }
        
        // Find geographic bounds
        minLat = stations.stream().mapToDouble(Station::getLatitude).min().orElse(0.0);
        maxLat = stations.stream().mapToDouble(Station::getLatitude).max().orElse(0.0);
        minLon = stations.stream().mapToDouble(Station::getLongitude).min().orElse(0.0);
        maxLon = stations.stream().mapToDouble(Station::getLongitude).max().orElse(0.0);
        
        this.width = width;
        this.height = height;
        this.coordinateCache = new HashMap<>();
        
        // Pre-compute all coordinates
        for (Station station : stations) {
            coordinateCache.put(station, toXY(station));
        }
    }

    /**
     * Converts a station's geographic coordinates to XY coordinates.
     *
     * @param station the station to convert
     * @return array with [x, y] coordinates
     */
    public double[] toXY(Station station) {
        // Check cache first
        if (coordinateCache.containsKey(station)) {
            return coordinateCache.get(station);
        }
        
        double lat = station.getLatitude();
        double lon = station.getLongitude();
        
        // Normalize to [0, 1] range
        double normalizedX = (lon - minLon) / (maxLon - minLon);
        double normalizedY = (maxLat - lat) / (maxLat - minLat);  // Inverted for Y
        
        // Scale to canvas dimensions
        double x = normalizedX * width;
        double y = normalizedY * height;
        
        return new double[]{x, y};
    }

    /**
     * Gets cached XY coordinates for a station.
     *
     * @param station the station
     * @return array with [x, y] coordinates
     */
    public double[] getXY(Station station) {
        return coordinateCache.getOrDefault(station, toXY(station));
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public double getMinLon() {
        return minLon;
    }

    public double getMaxLon() {
        return maxLon;
    }
}

