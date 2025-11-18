package main.domain;

import main.domain.AVL;
import java.util.*;

public class KD2DTree {
    private KD2DNode root;
    private int size;
    private int height;
    private static final Comparator<Station> NAME_COMPARATOR = new Comparator<Station>() {
        @Override
        public int compare(Station first, Station second) {
            return first.getName().compareTo(second.getName());
        }
    };
    /**
     * Constructs an empty 2D-tree.
     */
    public KD2DTree() {
        this.root = null;
        this.size = 0;
        this.height = -1;
    }

    /**
     * @param stations list of stations to insert
     */
    public void buildBalancedUsingBST(List<Station> stations) {
        if (stations == null || stations.isEmpty()) {
            this.root = null;
            this.size = 0;
            this.height = -1;
            return;
        }

        Map<String, List<Station>> coordinateMap = new HashMap<>();
        for (Station station : stations) {
            String key = station.getLatitude() + "," + station.getLongitude();
            List<Station> group = coordinateMap.get(key);
            if (group == null) {
                group = new ArrayList<>();
                coordinateMap.put(key, group);
            }
            group.add(station);
        }

        List<Station> representatives = new ArrayList<>(coordinateMap.size());
        for (List<Station> group : coordinateMap.values()) {
            group.sort(NAME_COMPARATOR);
            representatives.add(group.get(0));
        }

        AVL<StationComparable> latTree = createAVLTree(representatives, StationComparable.ComparisonType.BY_LATITUDE);
        AVL<StationComparable> lonTree = createAVLTree(representatives, StationComparable.ComparisonType.BY_LONGITUDE);

        this.root = buildFromAVLTrees(latTree, lonTree, coordinateMap, true);
        this.size = stations.size();
        this.height = calculateHeight(root);
    }

    /**
     * @param stations list of all stations
     * @param latTree pre-created AVL tree sorted by latitude
     * @param lonTree pre-created AVL tree sorted by longitude
     */
    public void buildBalancedUsingPreCreatedAVLTrees(List<Station> stations, 
                                                      AVL<StationComparable> latTree, 
                                                      AVL<StationComparable> lonTree) {
        if (stations == null || stations.isEmpty()) {
            this.root = null;
            this.size = 0;
            this.height = -1;
            return;
        }
        if (latTree == null || lonTree == null) {
            throw new IllegalArgumentException("Latitude and longitude AVL trees must not be null.");
        }

        Map<String, List<Station>> coordinateMap = new HashMap<>();
        for (Station station : stations) {
            String key = station.getLatitude() + "," + station.getLongitude();
            List<Station> group = coordinateMap.get(key);
            if (group == null) {
                group = new ArrayList<>();
                coordinateMap.put(key, group);
            }
            group.add(station);
        }

        Set<Station> representatives = new HashSet<>();
        for (List<Station> group : coordinateMap.values()) {
            group.sort(NAME_COMPARATOR);
            representatives.add(group.get(0));
        }
        
        this.root = buildFromAVLTreesWithFilter(latTree, lonTree, coordinateMap, representatives, true);
        this.size = stations.size();
        this.height = calculateHeight(root);
    }

    private KD2DNode buildFromAVLTreesWithFilter(AVL<StationComparable> latTree,
                                                 AVL<StationComparable> lonTree,
                                                 Map<String, List<Station>> coordinateMap,
                                                 Set<Station> representatives,
                                                 boolean splitByLatitude) {
        AVL<StationComparable> currentTree = splitByLatitude ? latTree : lonTree;
        if (currentTree.isEmpty()) {
            return null;
        }

        List<Station> filteredStations = new ArrayList<>();
        Iterable<StationComparable> inOrderResult = currentTree.inOrder();
        for (StationComparable sc : inOrderResult) {
            Station station = sc.getStation();
            if (representatives.contains(station)) {
                filteredStations.add(station);
            }
        }

        if (filteredStations.isEmpty()) {
            return null;
        }

        filteredStations.sort(splitByLatitude
                ? Comparator.comparingDouble(Station::getLatitude).thenComparingDouble(Station::getLongitude).thenComparing(Station::getName)
                : Comparator.comparingDouble(Station::getLongitude).thenComparingDouble(Station::getLatitude).thenComparing(Station::getName));

        int medianIndex = filteredStations.size() / 2;
        Station medianStation = filteredStations.get(medianIndex);

        String key = medianStation.getLatitude() + "," + medianStation.getLongitude();
        List<Station> medianGroup = coordinateMap.get(key);
        KD2DNode node = new KD2DNode(medianGroup.get(0), splitByLatitude);
        for (int i = 1; i < medianGroup.size(); i++) {
            node.addStation(medianGroup.get(i));
        }

        double medianValue = splitByLatitude ? medianStation.getLatitude() : medianStation.getLongitude();
        String medianName = medianStation.getName();

        Set<Station> leftSet = new HashSet<>();
        Set<Station> rightSet = new HashSet<>();

        for (Station station : filteredStations) {
            if (station == medianStation) continue;

            double value = splitByLatitude ? station.getLatitude() : station.getLongitude();
            int cmp = Double.compare(value, medianValue);

            if (cmp < 0 || (cmp == 0 && station.getName().compareTo(medianName) < 0)) {
                leftSet.add(station);
            } else if (cmp > 0 || station.getName().compareTo(medianName) > 0) {
                rightSet.add(station);
            }
        }

        node.setLeft(buildFromAVLTreesWithFilter(latTree, lonTree, coordinateMap, leftSet, !splitByLatitude));
        node.setRight(buildFromAVLTreesWithFilter(latTree, lonTree, coordinateMap, rightSet, !splitByLatitude));

        return node;
    }

    /**
     * Recursively builds the 2D-tree using AVL trees only (no intermediate sorted arrays).
     */
    private KD2DNode buildFromAVLTrees(AVL<StationComparable> latTree,
                                       AVL<StationComparable> lonTree,
                                       Map<String, List<Station>> coordinateMap,
                                       boolean splitByLatitude) {
        AVL<StationComparable> currentTree = splitByLatitude ? latTree : lonTree;
        if (currentTree.isEmpty()) {
            return null;
        }

        List<StationComparable> currentOrder = new ArrayList<>();
        Iterable<StationComparable> inOrderResult = currentTree.inOrder();
        for (StationComparable comparable : inOrderResult) {
            currentOrder.add(comparable);
        }

        int treeSize = currentOrder.size();
        if (treeSize == 0) {
            return null;
        }

        int medianIndex = treeSize / 2;
        Station medianStation = currentOrder.get(medianIndex).getStation();
        if (medianStation == null) {
            return null;
        }

        String key = medianStation.getLatitude() + "," + medianStation.getLongitude();
        List<Station> medianGroup = coordinateMap.get(key);
        KD2DNode node = new KD2DNode(medianGroup.get(0), splitByLatitude);
        for (int i = 1; i < medianGroup.size(); i++) {
            node.addStation(medianGroup.get(i));
        }

        double medianValue = splitByLatitude ? medianStation.getLatitude() : medianStation.getLongitude();
        String medianName = medianStation.getName();

        AVL<StationComparable> otherTree = splitByLatitude ? lonTree : latTree;
        List<StationComparable> otherOrder = new ArrayList<>();
        Iterable<StationComparable> otherInOrder = otherTree.inOrder();
        for (StationComparable sc : otherInOrder) {
            otherOrder.add(sc);
        }

        List<Station> leftStations = new ArrayList<>();
        List<Station> rightStations = new ArrayList<>();

        for (StationComparable sc : otherOrder) {
            Station station = sc.getStation();
            if (station == null || station == medianStation) continue;

            double value = splitByLatitude ? station.getLatitude() : station.getLongitude();
            int cmp = Double.compare(value, medianValue);

            if (cmp < 0 || (cmp == 0 && station.getName().compareTo(medianName) < 0)) {
                leftStations.add(station);
            } else {
                rightStations.add(station);
            }
        }

        AVL<StationComparable> leftLatTree = createAVLTree(leftStations, StationComparable.ComparisonType.BY_LATITUDE);
        AVL<StationComparable> leftLonTree = createAVLTree(leftStations, StationComparable.ComparisonType.BY_LONGITUDE);
        AVL<StationComparable> rightLatTree = createAVLTree(rightStations, StationComparable.ComparisonType.BY_LATITUDE);
        AVL<StationComparable> rightLonTree = createAVLTree(rightStations, StationComparable.ComparisonType.BY_LONGITUDE);

        node.setLeft(buildFromAVLTrees(leftLatTree, leftLonTree, coordinateMap, !splitByLatitude));
        node.setRight(buildFromAVLTrees(rightLatTree, rightLonTree, coordinateMap, !splitByLatitude));

        return node;
    }

    /**
     * Calculates the height of the tree.
     *
     * @param node root node
     * @return height
     */
    private int calculateHeight(KD2DNode node) {
        if (node == null) {
            return -1;
        }
        return 1 + Math.max(calculateHeight(node.getLeft()), calculateHeight(node.getRight()));
    }

    /**
     * Returns the root node of the tree.
     *
     * @return root node
     */
    public KD2DNode getRoot() {
        return root;
    }

    /**
     * Returns the total number of stations in the tree.
     *
     * @return size
     */
    public int size() {
        return size;
    }

    /**
     * Returns the height of the tree.
     *
     * @return height
     */
    public int height() {
        return height;
    }

    /**
     * Returns all distinct bucket sizes (number of stations at each coordinate).
     *
     * @return set of distinct bucket sizes
     */
    public Set<Integer> getDistinctBucketSizes() {
        Set<Integer> bucketSizes = new HashSet<>();
        collectBucketSizes(root, bucketSizes);
        return bucketSizes;
    }

    /**
     * Helper to create an AVL tree for the provided stations using the desired comparison type.
     *
     * @param stations collection of stations to insert (may be empty)
     * @param comparisonType ordering to apply
     * @return populated AVL tree (never null)
     */
    private AVL<StationComparable> createAVLTree(Collection<Station> stations,
                                                 StationComparable.ComparisonType comparisonType) {
        AVL<StationComparable> tree = new AVL<>();
        if (stations == null || stations.isEmpty()) {
            return tree;
        }
        for (Station station : stations) {
            tree.insert(new StationComparable(station, comparisonType));
        }
        return tree;
    }

    private void collectBucketSizes(KD2DNode node, Set<Integer> bucketSizes) {
        if (node != null) {
            bucketSizes.add(node.getStationCount());
            collectBucketSizes(node.getLeft(), bucketSizes);
            collectBucketSizes(node.getRight(), bucketSizes);
        }
    }

    /**
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLon minimum longitude
     * @param maxLon maximum longitude
     * @return list of stations in the range
     */
    public List<Station> rangeQuery(double minLat, double maxLat, double minLon, double maxLon) {
        List<Station> result = new ArrayList<>();
        rangeQuery(root, minLat, maxLat, minLon, maxLon, result);
        return result;
    }

    /**
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
        List<Station> result = new ArrayList<>();
        rangeQueryWithFilters(root, minLat, maxLat, minLon, maxLon, isCityFilter, isMainStationFilter, countryFilter, result);
        return result;
    }

    private void rangeQuery(KD2DNode node, double minLat, double maxLat, double minLon, double maxLon,
                           List<Station> result) {
        if (node == null) {
            return;
        }

        double lat = node.getLatitude();
        double lon = node.getLongitude();

        if (lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon) {
            result.addAll(node.getStations());
        }

        if (node.isSplitByLatitude()) {
            if (minLat <= lat) {
                rangeQuery(node.getLeft(), minLat, maxLat, minLon, maxLon, result);
            }
            if (maxLat > lat) {
                rangeQuery(node.getRight(), minLat, maxLat, minLon, maxLon, result);
            }
        } else {
            if (minLon <= lon) {
                rangeQuery(node.getLeft(), minLat, maxLat, minLon, maxLon, result);
            }
            if (maxLon > lon) {
                rangeQuery(node.getRight(), minLat, maxLat, minLon, maxLon, result);
            }
        }
    }

    private void rangeQueryWithFilters(KD2DNode node, double minLat, double maxLat, double minLon, double maxLon,
                                      Boolean isCityFilter, Boolean isMainStationFilter, String countryFilter,
                                      List<Station> result) {
        if (node == null) {
            return;
        }

        double lat = node.getLatitude();
        double lon = node.getLongitude();

        if (lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon) {
            for (Station station : node.getStations()) {
                boolean matches = true;

                if (isCityFilter != null && station.isCity() != isCityFilter) {
                    matches = false;
                }

                if (matches && isMainStationFilter != null && station.isMainStation() != isMainStationFilter) {
                    matches = false;
                }

                if (matches && countryFilter != null && !countryFilter.equals("all")) {
                    if (!countryFilter.equals(station.getCountry())) {
                        matches = false;
                    }
                }

                if (matches) {
                    result.add(station);
                }
            }
        }
        if (node.isSplitByLatitude()) {
            if (minLat <= lat) {
                rangeQueryWithFilters(node.getLeft(), minLat, maxLat, minLon, maxLon, 
                                     isCityFilter, isMainStationFilter, countryFilter, result);
            }
            if (maxLat > lat) {
                rangeQueryWithFilters(node.getRight(), minLat, maxLat, minLon, maxLon, 
                                     isCityFilter, isMainStationFilter, countryFilter, result);
            }
        } else {
            if (minLon <= lon) {
                rangeQueryWithFilters(node.getLeft(), minLat, maxLat, minLon, maxLon, 
                                     isCityFilter, isMainStationFilter, countryFilter, result);
            }
            if (maxLon > lon) {
                rangeQueryWithFilters(node.getRight(), minLat, maxLat, minLon, maxLon, 
                                     isCityFilter, isMainStationFilter, countryFilter, result);
            }
        }
    }


    /**
     * Earth's radius in kilometers (used for Haversine distance calculation).
     */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates the Haversine distance between two points on Earth's surface.
     * Uses the standard Haversine formula with Earth radius = 6371 km.
     *
     * @param lat1 latitude of first point in degrees
     * @param lon1 longitude of first point in degrees
     * @param lat2 latitude of second point in degrees
     * @param lon2 longitude of second point in degrees
     * @return distance in kilometers
     */
    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Finds the N nearest stations to a target coordinate using the 2D-tree.
     * Uses Haversine distance (km) with Earth radius for accurate distance calculation.
     * The search minimizes explored nodes by using the 2D-tree structure efficiently.
     *
     * @param targetLat target latitude in degrees
     * @param targetLon target longitude in degrees
     * @param n number of nearest neighbors to find
     * @param timeZoneFilter optional time zone group filter (null = no filter)
     * @return result containing nearest neighbors with distances and complexity metrics
     */
    public NearestNeighborResult nearestNNeighbors(double targetLat, double targetLon, int n, String timeZoneFilter) {
        if (root == null || n <= 0) {
            return new NearestNeighborResult(new ArrayList<>(), 0, size);
        }

        // Priority queue to maintain N nearest neighbors (max-heap: farthest at top)
        PriorityQueue<NearestNeighborResult.StationWithDistance> nearestQueue =
                new PriorityQueue<>(n + 1, (a, b) -> Double.compare(b.getDistanceKm(), a.getDistanceKm()));

        // Track explored nodes for complexity analysis
        int[] exploredCount = new int[1]; // Use array to pass by reference

        // Perform nearest-N search
        nearestNNeighborsRecursive(root, targetLat, targetLon, n, timeZoneFilter, nearestQueue, exploredCount);

        // Convert priority queue to sorted list (closest first)
        List<NearestNeighborResult.StationWithDistance> result = new ArrayList<>(nearestQueue);
        result.sort((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()));

        return new NearestNeighborResult(result, exploredCount[0], size);
    }

    /**
     * Recursive helper for nearest-N search.
     * Uses the 2D-tree structure to minimize explored nodes by pruning branches.
     *
     * @param node current node being explored
     * @param targetLat target latitude
     * @param targetLon target longitude
     * @param n number of neighbors to find
     * @param timeZoneFilter optional time zone filter
     * @param nearestQueue priority queue maintaining N nearest neighbors
     * @param exploredCount counter for explored nodes
     */
    private void nearestNNeighborsRecursive(KD2DNode node, double targetLat, double targetLon, int n,
                                            String timeZoneFilter,
                                            PriorityQueue<NearestNeighborResult.StationWithDistance> nearestQueue,
                                            int[] exploredCount) {
        if (node == null) {
            return;
        }

        exploredCount[0]++;

        // Get node coordinates
        double nodeLat = node.getLatitude();
        double nodeLon = node.getLongitude();

        // Check all stations at this node
        for (Station station : node.getStations()) {
            // Apply time zone filter if specified
            if (timeZoneFilter != null && !timeZoneFilter.equals(station.getTimeZoneGroup())) {
                continue;
            }

            // Calculate exact distance for this station (same coordinate, but for clarity)
            double stationDistance = haversineDistance(targetLat, targetLon, station.getLatitude(), station.getLongitude());

            // Add to queue if we have space or if it's closer than the farthest in queue
            if (nearestQueue.size() < n) {
                nearestQueue.offer(new NearestNeighborResult.StationWithDistance(station, stationDistance));
            } else if (stationDistance < nearestQueue.peek().getDistanceKm()) {
                nearestQueue.poll(); // Remove farthest
                nearestQueue.offer(new NearestNeighborResult.StationWithDistance(station, stationDistance));
            }
        }

        // Determine which child to explore first (closer side)
        double currentMaxDistance = nearestQueue.size() == n ? nearestQueue.peek().getDistanceKm() : Double.MAX_VALUE;

        if (node.isSplitByLatitude()) {
            // Split by latitude
            double splitValue = nodeLat;
            double targetValue = targetLat;
            KD2DNode nearChild = (targetValue < splitValue) ? node.getLeft() : node.getRight();
            KD2DNode farChild = (targetValue < splitValue) ? node.getRight() : node.getLeft();

            // Always explore the near child
            nearestNNeighborsRecursive(nearChild, targetLat, targetLon, n, timeZoneFilter, nearestQueue, exploredCount);

            // Update current max distance after exploring near child
            currentMaxDistance = nearestQueue.size() == n ? nearestQueue.peek().getDistanceKm() : Double.MAX_VALUE;

            // Check if we need to explore far child
            // Calculate minimum possible distance to far child's region
            double minDistToFarRegion = Math.abs(targetValue - splitValue);
            // Convert to approximate km (rough approximation: 1 degree latitude ≈ 111 km)
            double minDistKm = minDistToFarRegion * 111.0;

            if (nearestQueue.size() < n || minDistKm < currentMaxDistance) {
                // We might find closer points in the far child, so explore it
                nearestNNeighborsRecursive(farChild, targetLat, targetLon, n, timeZoneFilter, nearestQueue, exploredCount);
            }
        } else {
            // Split by longitude
            double splitValue = nodeLon;
            double targetValue = targetLon;
            KD2DNode nearChild = (targetValue < splitValue) ? node.getLeft() : node.getRight();
            KD2DNode farChild = (targetValue < splitValue) ? node.getRight() : node.getLeft();

            // Always explore the near child
            nearestNNeighborsRecursive(nearChild, targetLat, targetLon, n, timeZoneFilter, nearestQueue, exploredCount);

            // Update current max distance after exploring near child
            currentMaxDistance = nearestQueue.size() == n ? nearestQueue.peek().getDistanceKm() : Double.MAX_VALUE;

            // Check if we need to explore far child
            // Calculate minimum possible distance to far child's region
            // For longitude, need to account for latitude (use average)
            double minDistToFarRegion = Math.abs(targetValue - splitValue);
            // Convert to approximate km (1 degree longitude ≈ 111 km * cos(latitude))
            double avgLat = (targetLat + nodeLat) / 2.0;
            double minDistKm = minDistToFarRegion * 111.0 * Math.cos(Math.toRadians(avgLat));

            if (nearestQueue.size() < n || minDistKm < currentMaxDistance) {
                // We might find closer points in the far child, so explore it
                nearestNNeighborsRecursive(farChild, targetLat, targetLon, n, timeZoneFilter, nearestQueue, exploredCount);
            }
        }
    }
}


