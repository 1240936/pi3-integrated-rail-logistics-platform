package main.controller;

import main.domain.KD2DTree;
import main.domain.Station;
import main.domain.StationComparable;
import main.repositories.CsvValidatorResult;
import main.repositories.StationsCsvLoader;
import main.domain.AVL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service to manage railway stations and build balanced 2D-trees.
 * USEI07: Implements the construction of a balanced 2D-tree using AVL trees.
 * 
 * NOTE: Uses PL.AVL (from university provided classes) which requires Comparable<E>
 */
public class StationService {
    private KD2DTree tree;  // The 2D-tree that indexes stations by coordinates
    private List<Station> stations;  // List of all loaded stations
    private AVL<StationComparable> latitudeTree;  // USEI06: AVL tree sorted by latitude
    private AVL<StationComparable> longitudeTree;  // USEI06: AVL tree sorted by longitude
    private AVL<StationComparable> timeZoneGroupTree;  // USEI06: AVL tree sorted by time zone group and country

    /**
     * Constructs a new station service.
     */
    public StationService() {
        this.tree = new KD2DTree();
        this.stations = null;
        this.latitudeTree = null;
        this.longitudeTree = null;
        this.timeZoneGroupTree = null;
    }

    /**
     * USEI06: Loads stations from CSV and creates AVL trees for latitude, longitude, and time zone group.
     * This must be called before building the 2D-tree (USEI07).
     *
     * @param csvPath path to the stations CSV file
     * @return validation result with loaded stations and errors found
     * @throws IOException if there is an error reading the CSV file
     */
    public CsvValidatorResult<Station> loadStationsAndCreateAVLTrees(String csvPath) throws IOException {
        // Load and validate stations from CSV
        CsvValidatorResult<Station> result = StationsCsvLoader.load(csvPath);
        
        if (!result.getRecords().isEmpty()) {
            this.stations = result.getRecords();
            
            System.out.println("Creating AVL trees for latitude, longitude, and time zone group...");
            long startTime = System.currentTimeMillis();
            
            // Create AVL trees (USEI06)
            this.latitudeTree = createLatitudeTree();
            this.longitudeTree = createLongitudeTree();
            this.timeZoneGroupTree = createTimeZoneGroupTree();
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("AVL trees created in " + formatDuration(elapsedTime) + ".");
            System.out.println("  - Latitude tree: " + latitudeTree.size() + " stations");
            System.out.println("  - Longitude tree: " + longitudeTree.size() + " stations");
            System.out.println("  - Time zone group tree: " + timeZoneGroupTree.size() + " stations");
        }
        
        return result;
    }

    /**
     * USEI07: Builds a balanced 2D-tree using the AVL trees created in USEI06.
     * Requires that loadStationsAndCreateAVLTrees() has been called first.
     *
     * @throws IllegalStateException if AVL trees have not been created yet
     */
    public void buildBalanced2DTree() {
        if (latitudeTree == null || longitudeTree == null || stations == null) {
            throw new IllegalStateException("AVL trees must be created first. Call loadStationsAndCreateAVLTrees() before building the 2D-tree.");
        }
        
        System.out.println("Building balanced 2D-tree using pre-created AVL trees (this may take a while)...");
        long startTimeMillis = System.currentTimeMillis();
        // Build balanced 2D-tree using the pre-created AVL trees
        tree.buildBalancedUsingPreCreatedAVLTrees(stations, latitudeTree, longitudeTree);
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        System.out.println("2D-tree build finished in " + formatDuration(elapsedMillis) + ".");
    }

    /**
     * Builds a balanced 2D-tree from a list of stations.
     * Uses the balanced construction strategy with AVL trees.
     *
     * @param stations list of stations
     */
    public void buildTree(List<Station> stations) {
        this.stations = stations;
        tree.buildBalancedUsingBST(stations);
    }

    private String formatDuration(long elapsedMillis) {
        long seconds = elapsedMillis / 1000;
        long milliseconds = elapsedMillis % 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }
        if (seconds > 0) {
            return remainingSeconds + "s " + milliseconds + "ms";
        }
        return milliseconds + "ms";
    }

    /**
     * Returns the 2D-tree.
     *
     * @return the 2D-tree
     */
    public KD2DTree getTree() {
        return tree;
    }

    /**
     * Returns the size of the tree (total number of stations).
     *
     * @return size
     */
    public int getTreeSize() {
        return tree.size();
    }

    /**
     * Returns the height of the tree.
     *
     * @return height
     */
    public int getTreeHeight() {
        return tree.height();
    }

    /**
     * Returns all distinct bucket sizes (number of stations at each coordinate).
     *
     * @return set of distinct bucket sizes
     */
    public Set<Integer> getDistinctBucketSizes() {
        return tree.getDistinctBucketSizes();
    }

    /**
     * Performs a range query to find stations within a rectangular region.
     *
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLon minimum longitude
     * @param maxLon maximum longitude
     * @return list of stations in the range
     */
    public List<Station> rangeQuery(double minLat, double maxLat, double minLon, double maxLon) {
        return tree.rangeQuery(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Performs a range query with optional filters.
     *
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLon minimum longitude
     * @param maxLon maximum longitude
     * @param isCityFilter optional filter for isCity (null = no filter)
     * @param isMainStationFilter optional filter for isMainStation (null = no filter)
     * @param countryFilter optional filter for country ("PT", "ES", or "all" = no filter)
     * @return list of stations in the range matching the filters
     */
    public List<Station> rangeQueryWithFilters(double minLat, double maxLat, double minLon, double maxLon,
                                               Boolean isCityFilter, Boolean isMainStationFilter, String countryFilter) {
        return tree.rangeQueryWithFilters(minLat, maxLat, minLon, maxLon, isCityFilter, isMainStationFilter, countryFilter);
    }

    /**
     * Returns the loaded stations.
     *
     * @return list of stations
     */
    public List<Station> getStations() {
        return stations;
    }

    /**
     * USEI06: Returns the AVL tree sorted by latitude.
     *
     * @return latitude tree, or null if not created yet
     */
    public AVL<StationComparable> getLatitudeTree() {
        return latitudeTree;
    }

    /**
     * USEI06: Returns the AVL tree sorted by longitude.
     *
     * @return longitude tree, or null if not created yet
     */
    public AVL<StationComparable> getLongitudeTree() {
        return longitudeTree;
    }

    /**
     * USEI06: Returns the AVL tree sorted by time zone group and country.
     *
     * @return time zone group tree, or null if not created yet
     */
    public AVL<StationComparable> getTimeZoneGroupTree() {
        return timeZoneGroupTree;
    }

    /**
     * USEI06: Queries stations by time zone group.
     * Returns all stations in the specified time zone group, sorted by country (ascending).
     *
     * @param timeZoneGroup time zone group to search for (e.g., "CET", "WET/GMT")
     * @return list of stations in the time zone group, sorted by country then name
     */
    public List<Station> queryByTimeZoneGroup(String timeZoneGroup) {
        if (timeZoneGroupTree == null) {
            throw new IllegalStateException("Time zone group tree not created. Call loadStationsAndCreateAVLTrees() first.");
        }
        
        List<Station> results = new ArrayList<>();
        
        Iterable<StationComparable> inOrderResult = timeZoneGroupTree.inOrder();
        for (StationComparable sc : inOrderResult) {
            Station station = sc.getStation();
            if (station.getTimeZoneGroup().equals(timeZoneGroup)) {
                results.add(station);
            }
        }
        
        return results;
    }

    /**
     * USEI06: Queries stations by time zone group window.
     * Returns all stations in any of the specified time zone groups, sorted by time zone group, then country, then name.
     *
     * @param timeZoneGroups array of time zone groups (e.g., ["CET", "WET/GMT"])
     * @return list of stations in the time zone groups, sorted by time zone group, country, then name
     */
    public List<Station> queryByTimeZoneGroupWindow(String[] timeZoneGroups) {
        if (timeZoneGroupTree == null) {
            throw new IllegalStateException("Time zone group tree not created. Call loadStationsAndCreateAVLTrees() first.");
        }
        
        Set<String> tzSet = new HashSet<>();
        for (String tz : timeZoneGroups) {
            tzSet.add(tz);
        }
        
        List<Station> results = new ArrayList<>();
        
        Iterable<StationComparable> inOrderResult = timeZoneGroupTree.inOrder();
        for (StationComparable sc : inOrderResult) {
            Station station = sc.getStation();
            if (tzSet.contains(station.getTimeZoneGroup())) {
                results.add(station);
            }
        }
        
        return results;
    }

    /**
     * USEI06: Creates an AVL tree sorted by latitude.
     * Multiple stations at the same coordinates are preserved, sorted by name.
     *
     * @return AVL tree sorted by latitude (then longitude, then name)
     */
    public AVL<StationComparable> createLatitudeTree() {
        AVL<StationComparable> latTree = new AVL<>();
        if (stations != null && !stations.isEmpty()) {
            for (Station station : stations) {
                latTree.insert(new StationComparable(station, StationComparable.ComparisonType.BY_LATITUDE));
            }
        }
        return latTree;
    }

    /**
     * USEI06: Creates an AVL tree sorted by longitude.
     * Multiple stations at the same coordinates are preserved, sorted by name.
     *
     * @return AVL tree sorted by longitude (then latitude, then name)
     */
    public AVL<StationComparable> createLongitudeTree() {
        AVL<StationComparable> lonTree = new AVL<>();
        if (stations != null && !stations.isEmpty()) {
            for (Station station : stations) {
                lonTree.insert(new StationComparable(station, StationComparable.ComparisonType.BY_LONGITUDE));
            }
        }
        return lonTree;
    }

    /**
     * USEI06: Creates an AVL tree sorted by time zone group and country.
     * Allows efficient retrieval of stations by time zone group in ascending order of country.
     *
     * @return AVL tree sorted by time zone group, then country, then name
     */
    public AVL<StationComparable> createTimeZoneGroupTree() {
        AVL<StationComparable> tzTree = new AVL<>();
        if (stations != null && !stations.isEmpty()) {
            for (Station station : stations) {
                tzTree.insert(new StationComparable(station, StationComparable.ComparisonType.BY_TIMEZONE_GROUP));
            }
        }
        return tzTree;
    }

}

