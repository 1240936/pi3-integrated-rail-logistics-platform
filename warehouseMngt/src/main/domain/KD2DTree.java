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
}

