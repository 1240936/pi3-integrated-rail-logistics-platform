package main.domain;

import main.domain.AVL;
import java.util.*;

/**
 * 2D-tree (KD-tree with k=2) for spatial indexing of stations.
 * Supports efficient range queries and nearest neighbor searches.
 * Uses a balanced build strategy based on AVL trees of latitude and longitude.
 *
 * NOTE: Uses main.PL.AVL (from university provided classes) which requires Comparable<E>
 */
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
     * Builds a balanced 2D-tree from a list of stations.
     * USEI07: Uses AVL trees of latitude and longitude to build efficiently.
     * 
     * Strategy:
     * 1. Groups stations by coordinates (multiple stations can have the same coordinates)
     * 2. Creates AVL trees sorted by latitude and longitude
     * 3. Uses AVL trees to find medians and build the balanced tree
     * 4. Alternates between splitting by latitude and longitude at each level
     * 
     * NOTE: The method name mentions "BST", but in practice uses only AVLTree,
     * as AVLTree is already a balanced BST and provides all necessary functionality.
     *
     * @param stations list of stations to insert
     */
    public void buildBalancedUsingBST(List<Station> stations) {
        if (stations == null || stations.isEmpty()) {
            this.root = null;
            this.size = 0;
            this.height = -1;
            return;
        }

        // Step 1: Group stations by coordinates (key: "lat,lon")
        // Multiple stations can have the same coordinates (e.g., Lisbon)
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

        // Step 2: Extract representatives (one per coordinate, sorted by name for determinism)
        List<Station> representatives = new ArrayList<>(coordinateMap.size());
        for (List<Station> group : coordinateMap.values()) {
            group.sort(NAME_COMPARATOR);
            representatives.add(group.get(0));
        }

        // Step 3: Create AVL trees sorted by latitude and longitude
        AVL<StationComparable> latTree = createAVLTree(representatives, StationComparable.ComparisonType.BY_LATITUDE);
        AVL<StationComparable> lonTree = createAVLTree(representatives, StationComparable.ComparisonType.BY_LONGITUDE);

        // Step 4: Build the 2D-tree recursively using the AVL trees
        this.root = buildFromAVLTrees(latTree, lonTree, coordinateMap, true);
        this.size = stations.size();
        this.height = calculateHeight(root);
    }

    /**
     * Builds a balanced 2D-tree using pre-created AVL trees (from USEI06).
     * This is more efficient as it reuses the AVL trees already created.
     * 
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

        // Step 1: Group stations by coordinates (key: "lat,lon")
        // Multiple stations can have the same coordinates (e.g., Lisbon)
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

        // Step 2: Use pre-created trees directly, but filter to only representatives during build
        // Create a set of representative stations (one per coordinate, sorted by name)
        Set<Station> representatives = new HashSet<>();
        for (List<Station> group : coordinateMap.values()) {
            group.sort(NAME_COMPARATOR);
            representatives.add(group.get(0)); // First station is the representative
        }
        
        // Step 3: Build the 2D-tree recursively using the pre-created AVL trees
        this.root = buildFromAVLTreesWithFilter(latTree, lonTree, coordinateMap, representatives, true);
        this.size = stations.size();
        this.height = calculateHeight(root);
    }

    /**
     * Recursively builds the 2D-tree using the pre-created AVL trees, filtering to only representatives.
     * This approach keeps the construction tied directly to the PL-supplied trees.
     */
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
     * Performs a range query to find all stations within a rectangular region.
     *
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

    private void rangeQuery(KD2DNode node, double minLat, double maxLat, double minLon, double maxLon,
                           List<Station> result) {
        if (node == null) {
            return;
        }

        double lat = node.getLatitude();
        double lon = node.getLongitude();

        // Check if this node's point is in the range
        if (lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon) {
            result.addAll(node.getStations());
        }

        // Recursively search subtrees
        if (node.isSplitByLatitude()) {
            if (lat >= minLat) {
                rangeQuery(node.getLeft(), minLat, maxLat, minLon, maxLon, result);
            }
            if (lat <= maxLat) {
                rangeQuery(node.getRight(), minLat, maxLat, minLon, maxLon, result);
            }
        } else {
            if (lon >= minLon) {
                rangeQuery(node.getLeft(), minLat, maxLat, minLon, maxLon, result);
            }
            if (lon <= maxLon) {
                rangeQuery(node.getRight(), minLat, maxLat, minLon, maxLon, result);
            }
        }
    }
}

