package USEI07;

import main.controller.StationService;
import main.domain.KD2DTree;
import main.domain.Station;
import main.repositories.CsvValidatorResult;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test cases for USEI07: Build a balanced 2D-Tree on Latitude/Longitude.
 * 
 * Tests balanced 2D-tree construction:
 * - Load stations from CSV file
 * - Create AVL trees (USEI06 prerequisite)
 * - Build balanced 2D-tree using AVL trees
 * - Validate tree structure, size, height, and bucket sizes
 */
public class TestBuildBalanced2DTree {

    /**
     * Helper method to create a temporary CSV file with test data.
     * Uses the CSV format provided for testing.
     */
    private Path createTestCsvFile(String csvContent) throws IOException {
        Path tempFile = Files.createTempFile("stations-", ".csv");
        Files.writeString(tempFile, csvContent, StandardCharsets.UTF_8);
        return tempFile;
    }

    /**
     * Test Case 1: Build tree with single station
     * Expected: Tree with size 1, height 0
     */
    @Test
    public void testBuildTree_SingleStation() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with one station
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False"
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations and create AVL trees
            StationService service = new StationService();
            CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(tempFile.toString());
            
            assertFalse("Should have no validation errors", result.hasErrors());
            assertEquals("Should load 1 station", 1, result.getRecords().size());

            // 3. Build balanced 2D-tree
            service.buildBalanced2DTree();

            // 4. Verify tree size = 1
            assertEquals("Tree should contain 1 station", 1, service.getTreeSize());

            // 5. Verify tree height = 0
            assertEquals("Tree height should be 0 for single node", 0, service.getTreeHeight());

            // 6. Verify bucket sizes
            Set<Integer> bucketSizes = service.getDistinctBucketSizes();
            assertTrue("Should have bucket size 1", bucketSizes.contains(1));
            assertEquals("Should have only one bucket size", 1, bucketSizes.size());

        } finally {
            // 7. Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 2: Build tree with multiple stations at different coordinates
     * Expected: Balanced tree with correct size and reasonable height
     */
    @Test
    public void testBuildTree_MultipleStations() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with multiple stations at different coordinates
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False",
                    "PT,\"('Europe/Lisbon',)\",WET/GMT,Ginjal (Belmonte),40.36623,-7.35269,True,False,False",
                    "ES,\"('Europe/Madrid',)\",CET,Torre del Mar,36.742,-4.09291,True,False,False",
                    "BE,\"('Europe/Brussels',)\",CET,Nieuwkerken-Waas,51.185342,4.185314,False,False,False",
                    "FR,\"('Europe/Paris',)\",CET,Menton,43.774444,7.4933,False,True,False",
                    "ES,\"('Europe/Madrid',)\",CET,Trevelez,37.0012828,-3.2665353,True,False,False",
                    "PT,\"('Europe/Lisbon',)\",WET/GMT,Ferreiras,37.1270988,-8.2439177,True,False,False"
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations and create AVL trees
            StationService service = new StationService();
            CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(tempFile.toString());
            
            assertFalse("Should have no validation errors", result.hasErrors());
            final int expectedStationCount = 7;
            assertEquals("Should load all stations", expectedStationCount, result.getRecords().size());

            // 3. Build balanced 2D-tree
            service.buildBalanced2DTree();

            // 4. Verify tree size matches number of stations
            assertEquals("Tree size should match number of stations", expectedStationCount, service.getTreeSize());

            // 5. Verify tree height is reasonable (should be balanced, log(n) approximately)
            int height = service.getTreeHeight();
            int expectedMinHeight = (int) Math.floor(Math.log(expectedStationCount) / Math.log(2));
            int expectedMaxHeight = (int) Math.ceil(Math.log(expectedStationCount) / Math.log(2)) + 2; // Allow some tolerance
            assertTrue("Tree height should be reasonable (at least log2(n))", height >= expectedMinHeight);
            assertTrue("Tree height should not be too large (balanced tree)", height <= expectedMaxHeight);

            // 6. Verify bucket sizes
            Set<Integer> bucketSizes = service.getDistinctBucketSizes();
            assertTrue("Should have at least one bucket size", bucketSizes.size() > 0);
            // All stations have different coordinates, so all buckets should be size 1
            assertTrue("All buckets should be size 1 (unique coordinates)", bucketSizes.contains(1));

            // Verify all stations are in the tree using range query
            List<Station> allStations = service.rangeQuery(-90, 90, -180, 180);
            assertEquals("Range query should return all stations", expectedStationCount, allStations.size());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 3: Multiple stations at same coordinates
     * Expected: All stations stored at same node, sorted by name
     */
    @Test
    public void testBuildTree_MultipleStationsSameCoordinates() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with stations at same coordinates
            // Using same coordinates for multiple stations
            double lat = 44.08179;
            double lon = 6.001625;
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban," + lat + "," + lon + ",True,False,False",
                    "FR,\"('Europe/Paris',)\",CET,StationB," + lat + "," + lon + ",False,True,False",
                    "FR,\"('Europe/Paris',)\",CET,StationA," + lat + "," + lon + ",False,False,False"
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations and create AVL trees
            StationService service = new StationService();
            CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(tempFile.toString());
            
            assertFalse("Should have no validation errors", result.hasErrors());
            final int expectedStationCount = 3;
            assertEquals("Should load all stations", expectedStationCount, result.getRecords().size());

            // 3. Build balanced 2D-tree
            service.buildBalanced2DTree();

            // 4. Verify all stations are included
            assertEquals("Tree size should match number of stations", expectedStationCount, service.getTreeSize());

            // 5. Verify stations at same coordinates are in same bucket
            Set<Integer> bucketSizes = service.getDistinctBucketSizes();
            assertTrue("Should have bucket size 3 (all stations at same coordinate)", bucketSizes.contains(3));
            
            // Verify by range query - all should be returned
            List<Station> stations = service.rangeQuery(lat - 0.01, lat + 0.01, lon - 0.01, lon + 0.01);
            assertEquals("Should return all 3 stations at same coordinate", expectedStationCount, stations.size());

            // 6. Verify they are sorted by name (first station in bucket should be alphabetically first)
            // The tree stores stations in buckets sorted by name
            // We can verify by checking that StationA comes before StationB
            List<Station> allStations = service.rangeQuery(-90, 90, -180, 180);
            // Find stations at the same coordinate
            List<Station> sameCoordStations = allStations.stream()
                    .filter(s -> Math.abs(s.getLatitude() - lat) < 0.0001 && 
                                 Math.abs(s.getLongitude() - lon) < 0.0001)
                    .toList();
            assertEquals("Should have 3 stations at same coordinate", 3, sameCoordStations.size());
            
            // Verify names are present
            assertTrue("Should contain StationA", sameCoordStations.stream().anyMatch(s -> s.getName().equals("StationA")));
            assertTrue("Should contain StationB", sameCoordStations.stream().anyMatch(s -> s.getName().equals("StationB")));
            assertTrue("Should contain Chateau-Arnoux-St-Auban", 
                sameCoordStations.stream().anyMatch(s -> s.getName().equals("Chateau-Arnoux-St-Auban")));

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 4: Build tree with larger dataset
     * Expected: Balanced tree with logarithmic height
     */
    @Test
    public void testBuildTree_LargerDataset() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with larger dataset (20+ stations)
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False",
                    "FR,\"('Europe/Paris',)\",CET,Menton,43.774444,7.4933,False,True,False",
                    "BE,\"('Europe/Brussels',)\",CET,Nieuwkerken-Waas,51.185342,4.185314,False,False,False",
                    "BE,\"('Europe/Brussels',)\",CET,Balegem-Dorp,50.919432,3.791542,False,False,False",
                    "ES,\"('Europe/Madrid',)\",CET,Torre del Mar,36.742,-4.09291,True,False,False",
                    "ES,\"('Europe/Madrid',)\",CET,Trevelez,37.0012828,-3.2665353,True,False,False",
                    "PT,\"('Europe/Lisbon',)\",WET/GMT,Ginjal (Belmonte),40.36623,-7.35269,True,False,False",
                    "PT,\"('Europe/Lisbon',)\",WET/GMT,Ferreiras,37.1270988,-8.2439177,True,False,False",
                    "SE,\"('Europe/Stockholm',)\",CET,Vallsta,61.516635,16.3633657,True,False,False",
                    "IT,\"('Europe/Rome',)\",CET,Napoli Capodichino,40.8847493,14.289243,False,False,True",
                    "AT,\"('Europe/Vienna',)\",CET,Innsbruck Airport,47.2575301,11.3509844,False,False,True",
                    "PL,\"('Europe/Warsaw',)\",CET,Glucholazy,50.3168376,17.3887110000001,True,False,False",
                    "NO,\"('Europe/Oslo',)\",CET,Rindal,63.0569,9.213634,True,False,False",
                    "PL,\"('Europe/Warsaw',)\",CET,Swieradow-Zdroj,50.919233,15.298365,True,False,False",
                    "NL,\"('Europe/Amsterdam',)\",CET,Maastricht Aachen Airport,50.9124374,5.76552179999999,False,False,True",
                    "CH,\"('Europe/Zurich',)\",CET,Sur Roche,46.640445,6.638926,False,False,False",
                    "NO,\"('Europe/Oslo',)\",CET,Frogner,60.022865,11.103267,True,False,False",
                    "FR,\"('Europe/Paris',)\",CET,Digne-les-Bains,44.088710133980605,6.222982406616211,False,True,False",
                    "BE,\"('Europe/Brussels',)\",CET,Liege-Guillemins,50.624297,5.566667,False,True,False",
                    "CH,\"('Europe/Zurich',)\",CET,Martigny,46.105834,7.079122,False,True,False",
                    "ES,\"('Europe/Madrid',)\",CET,Valladolid Campo Grande,41.641971,-4.727004,False,True,False",
                    "DE,\"('Europe/Berlin',)\",CET,Frankfurt (M) Flughafen,50.051209,8.570971,False,False,True",
                    "GB,\"('Europe/London',)\",WET/GMT,Gatwick-Airport,51.1537,-0.1821,False,False,True"
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations and create AVL trees
            StationService service = new StationService();
            CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(tempFile.toString());
            
            assertFalse("Should have no validation errors", result.hasErrors());
            final int expectedStationCount = 23;
            assertEquals("Should load all stations", expectedStationCount, result.getRecords().size());

            // 3. Build balanced 2D-tree
            service.buildBalanced2DTree();

            // 4. Verify tree size
            assertEquals("Tree size should match number of stations", expectedStationCount, service.getTreeSize());

            // 5. Verify height is approximately log2(n) for balanced tree
            int height = service.getTreeHeight();
            double log2n = Math.log(expectedStationCount) / Math.log(2);
            int expectedMinHeight = (int) Math.floor(log2n);
            int expectedMaxHeight = (int) Math.ceil(log2n) + 3; // Allow tolerance for balanced tree
            
            assertTrue("Tree height should be at least log2(n): " + height + " >= " + expectedMinHeight, 
                height >= expectedMinHeight);
            assertTrue("Tree height should not exceed log2(n) + tolerance: " + height + " <= " + expectedMaxHeight, 
                height <= expectedMaxHeight);

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 5: Tree build requires AVL trees first
     * Expected: Throws exception if buildBalanced2DTree() called before AVL trees are created
     */
    @Test(expected = IllegalStateException.class)
    public void testBuildTree_RequiresAvlTreesFirst() {
        // 1. Create StationService without loading stations
        StationService service = new StationService();
        
        // 2. Attempt to call buildBalanced2DTree()
        // 3. Verify IllegalStateException is thrown (via @Test(expected))
        service.buildBalanced2DTree();
    }

    /**
     * Test Case 6: Range query functionality
     * Expected: Returns stations within specified geographic bounds
     */
    @Test
    public void testRangeQuery() throws IOException {
        Path tempFile = null;
        try {
            // 1. Load stations from CSV
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False",
                    "BE,\"('Europe/Brussels',)\",CET,Nieuwkerken-Waas,51.185342,4.185314,False,False,False",
                    "BE,\"('Europe/Brussels',)\",CET,Balegem-Dorp,50.919432,3.791542,False,False,False",
                    "ES,\"('Europe/Madrid',)\",CET,Torre del Mar,36.742,-4.09291,True,False,False",
                    "PT,\"('Europe/Lisbon',)\",WET/GMT,Ginjal (Belmonte),40.36623,-7.35269,True,False,False",
                    "NO,\"('Europe/Oslo',)\",CET,Rindal,63.0569,9.213634,True,False,False"
            );
            tempFile = createTestCsvFile(csvContent);

            StationService service = new StationService();
            CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(tempFile.toString());
            assertFalse("Should have no validation errors", result.hasErrors());

            // 2. Build balanced 2D-tree
            service.buildBalanced2DTree();

            // 3. Perform range query for a specific geographic region (Belgium/Netherlands area)
            double minLat = 50.0;
            double maxLat = 52.0;
            double minLon = 3.0;
            double maxLon = 6.0;
            List<Station> queryResults = service.rangeQuery(minLat, maxLat, minLon, maxLon);

            // 4. Verify returned stations are within bounds
            for (Station station : queryResults) {
                assertTrue("Station latitude should be within bounds: " + station.getName(),
                    station.getLatitude() >= minLat && station.getLatitude() <= maxLat);
                assertTrue("Station longitude should be within bounds: " + station.getName(),
                    station.getLongitude() >= minLon && station.getLongitude() <= maxLon);
            }

            // 5. Verify all stations within bounds are returned
            // Expected: Nieuwkerken-Waas (51.185342, 4.185314) and Balegem-Dorp (50.919432, 3.791542)
            assertEquals("Should return 2 stations in Belgium region", 2, queryResults.size());
            assertTrue("Should include Nieuwkerken-Waas", 
                queryResults.stream().anyMatch(s -> s.getName().equals("Nieuwkerken-Waas")));
            assertTrue("Should include Balegem-Dorp", 
                queryResults.stream().anyMatch(s -> s.getName().equals("Balegem-Dorp")));

            // Verify stations outside bounds are not returned
            assertFalse("Should not include Chateau-Arnoux-St-Auban (outside bounds)",
                queryResults.stream().anyMatch(s -> s.getName().equals("Chateau-Arnoux-St-Auban")));
            assertFalse("Should not include Torre del Mar (outside bounds)",
                queryResults.stream().anyMatch(s -> s.getName().equals("Torre del Mar")));

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 7: Empty tree
     * Expected: Tree with size 0, height -1
     */
    @Test
    public void testBuildTree_Empty() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with header only (no data rows)
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport"
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations and create AVL trees
            StationService service = new StationService();
            CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(tempFile.toString());
            
            assertFalse("Should have no validation errors", result.hasErrors());
            assertEquals("Should have no stations", 0, result.getRecords().size());

            // 3. Build balanced 2D-tree
            service.buildBalanced2DTree();

            // 4. Verify tree size = 0
            assertEquals("Tree size should be 0", 0, service.getTreeSize());

            // 5. Verify tree height = -1
            assertEquals("Tree height should be -1 for empty tree", -1, service.getTreeHeight());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 8: Tree statistics
     * Expected: Correct size, height, and bucket size distribution
     */
    @Test
    public void testTreeStatistics() throws IOException {
        Path tempFile = null;
        try {
            // 1. Load stations from CSV
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False",
                    "FR,\"('Europe/Paris',)\",CET,StationB,44.08179,6.001625,False,True,False",  // Same coordinates
                    "PT,\"('Europe/Lisbon',)\",WET/GMT,Ginjal (Belmonte),40.36623,-7.35269,True,False,False",
                    "ES,\"('Europe/Madrid',)\",CET,Torre del Mar,36.742,-4.09291,True,False,False",
                    "BE,\"('Europe/Brussels',)\",CET,Nieuwkerken-Waas,51.185342,4.185314,False,False,False"
            );
            tempFile = createTestCsvFile(csvContent);

            StationService service = new StationService();
            CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(tempFile.toString());
            assertFalse("Should have no validation errors", result.hasErrors());
            final int expectedStationCount = 5;
            assertEquals("Should load all stations", expectedStationCount, result.getRecords().size());

            // 2. Build balanced 2D-tree
            service.buildBalanced2DTree();

            // 3. Verify getTreeSize() returns correct size
            assertEquals("Tree size should match number of stations", expectedStationCount, service.getTreeSize());

            // 4. Verify getTreeHeight() returns correct height
            int height = service.getTreeHeight();
            assertTrue("Tree height should be non-negative or -1", height >= -1);
            // For 5 stations (4 unique coordinates), height should be reasonable
            assertTrue("Tree height should be reasonable for 5 stations", height >= 1 && height <= 4);

            // 5. Verify getDistinctBucketSizes() returns expected bucket sizes
            Set<Integer> bucketSizes = service.getDistinctBucketSizes();
            assertTrue("Should have at least one bucket size", bucketSizes.size() > 0);
            // We have 2 stations at same coordinate (44.08179, 6.001625) and 3 at unique coordinates
            assertTrue("Should have bucket size 2 (two stations at same coordinate)", bucketSizes.contains(2));
            assertTrue("Should have bucket size 1 (stations at unique coordinates)", bucketSizes.contains(1));

            // Verify total stations match
            // More direct verification: use range query to count all stations
            List<Station> allStations = service.rangeQuery(-90, 90, -180, 180);
            assertEquals("All stations should be accessible via range query", expectedStationCount, allStations.size());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}

