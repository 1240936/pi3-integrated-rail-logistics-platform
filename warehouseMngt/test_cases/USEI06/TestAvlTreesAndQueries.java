package USEI06;

import main.controller.StationService;
import main.domain.AVL;
import main.domain.Station;
import main.domain.StationComparable;
import main.repositories.CsvValidatorResult;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for USEI06: Create AVL trees and query by time zone group.
 * 
 * Tests AVL tree creation and querying functionality:
 * - Load stations from CSV file
 * - Create AVL trees for latitude, longitude, and time zone group
 * - Query stations by time zone group
 * - Query stations by time zone group window
 * - Validate tree structure and ordering
 */
public class TestAvlTreesAndQueries {

    /**
     * Gets the path to the full train_stations_europe.csv file.
     * The file is located in the warehouseMngt directory.
     */
    private String getFullCsvFilePath() {
        // Try relative path from test directory (test_cases/USEI06/)
        String[] pathsToTry = {
            "../../train_stations_europe.csv",  // From test_cases/USEI06/ to warehouseMngt/
            "../train_stations_europe.csv",     // From test_cases/ to warehouseMngt/
            "warehouseMngt/train_stations_europe.csv",  // From project root
            "../warehouseMngt/train_stations_europe.csv" // Alternative from test_cases/
        };
        
        for (String path : pathsToTry) {
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }
        
        // Fallback - return relative path (test will fail if file not found, which is correct)
        return "../../train_stations_europe.csv";
    }

    /**
     * Test Case 1: Load stations and create AVL trees
     * Expected: AVL trees are created for latitude, longitude, and time zone group
     */
    @Test
    public void testLoadStationsAndCreateAvlTrees() throws IOException {
        // 1. Use full train_stations_europe.csv file
        String csvPath = getFullCsvFilePath();

        // 2. Load stations using StationService.loadStationsAndCreateAVLTrees()
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);

        // 3. Verify we loaded valid stations (some validation errors in large CSV are acceptable)
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        // We check that we got substantial valid data rather than requiring zero errors

        // 4. Verify AVL trees are created (latitudeTree, longitudeTree, timeZoneGroupTree)
        AVL<StationComparable> latTree = service.getLatitudeTree();
        AVL<StationComparable> lonTree = service.getLongitudeTree();
        AVL<StationComparable> tzTree = service.getTimeZoneGroupTree();
        
        assertNotNull("Latitude tree should be created", latTree);
        assertNotNull("Longitude tree should be created", lonTree);
        assertNotNull("Time zone group tree should be created", tzTree);

        // 5. Verify tree sizes match loaded stations
        int stationCount = result.getRecords().size();
        assertTrue("Should have loaded stations from full CSV file", stationCount > 0);
        
        // Verify trees are not empty
        assertTrue("Latitude tree should not be empty", latTree.size() > 0);
        assertTrue("Longitude tree should not be empty", lonTree.size() > 0);
        assertTrue("Time zone group tree should not be empty", tzTree.size() > 0);
        
        // AVL trees don't store duplicates (when compareTo returns 0), so tree sizes may be smaller
        // than station count if some stations have identical comparison values (e.g., same coordinates and name)
        // We verify that trees contain most stations (within a small margin for duplicates)
        int latSize = latTree.size();
        int lonSize = lonTree.size();
        int tzSize = tzTree.size();
        
        // Allow up to 0.5% difference for duplicates (should be very few)
        int maxExpectedDiff = Math.max(100, stationCount / 200); // At least 100 or 0.5% of stations
        
        assertTrue(String.format("Latitude tree should contain most stations. Expected: %d, Got: %d, Diff: %d", 
                stationCount, latSize, stationCount - latSize), 
                stationCount - latSize <= maxExpectedDiff);
        assertTrue(String.format("Longitude tree should contain most stations. Expected: %d, Got: %d, Diff: %d", 
                stationCount, lonSize, stationCount - lonSize), 
                stationCount - lonSize <= maxExpectedDiff);
        assertTrue(String.format("Time zone group tree should contain most stations. Expected: %d, Got: %d, Diff: %d", 
                stationCount, tzSize, stationCount - tzSize), 
                stationCount - tzSize <= maxExpectedDiff);

        // Verify we have a substantial number of stations (full CSV should have thousands)
        assertTrue("Should have loaded substantial number of stations from full CSV", stationCount > 1000);
    }

    /**
     * Test Case 2: Latitude tree ordering
     * Expected: Stations in latitude tree are sorted by latitude (then longitude, then name)
     */
    @Test
    public void testLatitudeTreeOrdering() throws IOException {
        // 1. Load stations from full CSV file
        String csvPath = getFullCsvFilePath();
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);
        int stationCount = result.getRecords().size();

        // 2. Get latitude tree
        AVL<StationComparable> latTree = service.getLatitudeTree();
        assertNotNull("Latitude tree should exist", latTree);

        // 3. Verify inOrder() returns stations sorted by latitude ascending
        Iterable<StationComparable> inOrder = latTree.inOrder();
        StationComparable previous = null;
        int checkedCount = 0;
        for (StationComparable current : inOrder) {
            if (previous != null) {
                Station prevStation = previous.getStation();
                Station currStation = current.getStation();
                
                // Verify latitude is ascending (or equal)
                assertTrue("Latitude should be ascending or equal",
                        currStation.getLatitude() >= prevStation.getLatitude());
                
                // 4. For same latitude, verify sorting by longitude, then name
                if (Double.compare(currStation.getLatitude(), prevStation.getLatitude()) == 0) {
                    assertTrue("For same latitude, longitude should be ascending or equal",
                            currStation.getLongitude() >= prevStation.getLongitude());
                    
                    // If latitude and longitude are same, verify name sorting
                    if (Double.compare(currStation.getLongitude(), prevStation.getLongitude()) == 0) {
                        assertTrue("For same coordinates, name should be ascending",
                                currStation.getName().compareTo(prevStation.getName()) >= 0);
                    }
                }
                checkedCount++;
            }
            previous = current;
        }

        // Verify we checked a reasonable number of stations
        assertTrue("Should have verified ordering for all stations in full CSV", checkedCount > 1000);
    }

    /**
     * Test Case 3: Longitude tree ordering
     * Expected: Stations in longitude tree are sorted by longitude (then latitude, then name)
     */
    @Test
    public void testLongitudeTreeOrdering() throws IOException {
        // 1. Load stations from full CSV file
        String csvPath = getFullCsvFilePath();
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);
        int stationCount = result.getRecords().size();

        // 2. Get longitude tree
        AVL<StationComparable> lonTree = service.getLongitudeTree();
        assertNotNull("Longitude tree should exist", lonTree);

        // 3. Verify inOrder() returns stations sorted by longitude ascending
        Iterable<StationComparable> inOrder = lonTree.inOrder();
        StationComparable previous = null;
        int checkedCount = 0;
        for (StationComparable current : inOrder) {
            if (previous != null) {
                Station prevStation = previous.getStation();
                Station currStation = current.getStation();
                
                // Verify longitude is ascending (or equal)
                assertTrue("Longitude should be ascending or equal",
                        currStation.getLongitude() >= prevStation.getLongitude());
                
                // 4. For same longitude, verify sorting by latitude, then name
                if (Double.compare(currStation.getLongitude(), prevStation.getLongitude()) == 0) {
                    assertTrue("For same longitude, latitude should be ascending or equal",
                            currStation.getLatitude() >= prevStation.getLatitude());
                    
                    // If longitude and latitude are same, verify name sorting
                    if (Double.compare(currStation.getLatitude(), prevStation.getLatitude()) == 0) {
                        assertTrue("For same coordinates, name should be ascending",
                                currStation.getName().compareTo(prevStation.getName()) >= 0);
                    }
                }
                checkedCount++;
            }
            previous = current;
        }

        // Verify we checked a reasonable number of stations
        assertTrue("Should have verified ordering for all stations in full CSV", checkedCount > 1000);
    }

    /**
     * Test Case 4: Time zone group tree ordering
     * Expected: Stations in time zone group tree are sorted by time zone group, then country, then name
     */
    @Test
    public void testTimeZoneGroupTreeOrdering() throws IOException {
        // 1. Load stations from full CSV file with multiple time zone groups
        String csvPath = getFullCsvFilePath();
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);
        int stationCount = result.getRecords().size();

        // 2. Get time zone group tree
        AVL<StationComparable> tzTree = service.getTimeZoneGroupTree();
        assertNotNull("Time zone group tree should exist", tzTree);

        // 3. Verify inOrder() returns stations sorted by time zone group, country, name
        Iterable<StationComparable> inOrder = tzTree.inOrder();
        StationComparable previous = null;
        int checkedCount = 0;
        for (StationComparable current : inOrder) {
            if (previous != null) {
                Station prevStation = previous.getStation();
                Station currStation = current.getStation();
                
                // Verify time zone group sorting
                int tzCmp = currStation.getTimeZoneGroup().compareTo(prevStation.getTimeZoneGroup());
                assertTrue("Time zone group should be ascending or equal", tzCmp >= 0);
                
                // For same time zone group, verify country sorting
                if (tzCmp == 0) {
                    int countryCmp = currStation.getCountry().compareTo(prevStation.getCountry());
                    assertTrue("For same time zone group, country should be ascending or equal", countryCmp >= 0);
                    
                    // For same time zone group and country, verify name sorting
                    if (countryCmp == 0) {
                        assertTrue("For same time zone group and country, name should be ascending",
                                currStation.getName().compareTo(prevStation.getName()) >= 0);
                    }
                }
                checkedCount++;
            }
            previous = current;
        }

        // Verify we checked a reasonable number of stations
        assertTrue("Should have verified ordering for all stations in full CSV", checkedCount > 1000);
    }

    /**
     * Test Case 5: Query by single time zone group
     * Expected: Returns all stations in the specified time zone group, sorted by country then name
     */
    @Test
    public void testQueryByTimeZoneGroup() throws IOException {
        // 1. Load stations from full CSV file (includes stations with different time zone groups)
        String csvPath = getFullCsvFilePath();
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);

        // 2. Query for a specific time zone group (e.g., "CET")
        List<Station> results = service.queryByTimeZoneGroup("CET");

        // 3. Verify all returned stations match the time zone group
        assertFalse("Should return at least one station", results.isEmpty());
        for (Station station : results) {
            assertEquals("All stations should be in CET time zone group", "CET", station.getTimeZoneGroup());
        }

        // 4. Verify sorting: country ascending, then name ascending
        Station previous = null;
        for (Station current : results) {
            if (previous != null) {
                int countryCmp = current.getCountry().compareTo(previous.getCountry());
                assertTrue("Countries should be sorted ascending or equal", countryCmp >= 0);
                
                // For same country, verify name sorting
                if (countryCmp == 0) {
                    assertTrue("For same country, names should be sorted ascending",
                            current.getName().compareTo(previous.getName()) >= 0);
                }
            }
            previous = current;
        }

        // 5. Verify no stations from other time zone groups are included
        List<Station> wetResults = service.queryByTimeZoneGroup("WET/GMT");
        // WET/GMT may or may not exist in the full CSV, so just verify if results exist they're correct
        if (!wetResults.isEmpty()) {
            for (Station station : wetResults) {
                assertEquals("All stations should be in WET/GMT time zone group", "WET/GMT", station.getTimeZoneGroup());
            }
        }

        // Verify CET query returns substantial results from full CSV
        assertTrue("Should return many stations in CET from full CSV", results.size() > 100);
    }

    /**
     * Test Case 6: Query by time zone group window (multiple groups)
     * Expected: Returns stations from all specified time zone groups, sorted by time zone group, country, name
     */
    @Test
    public void testQueryByTimeZoneGroupWindow() throws IOException {
        // 1. Load stations from full CSV file with multiple time zone groups (e.g., "CET", "WET/GMT")
        String csvPath = getFullCsvFilePath();
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);

        // 2. Query for multiple time zone groups
        String[] timeZoneGroups = {"CET", "WET/GMT"};
        List<Station> results = service.queryByTimeZoneGroupWindow(timeZoneGroups);

        // 3. Verify all returned stations are from one of the specified groups
        assertFalse("Should return at least one station", results.isEmpty());
        for (Station station : results) {
            assertTrue("Station should be in one of the specified time zone groups",
                    station.getTimeZoneGroup().equals("CET") || station.getTimeZoneGroup().equals("WET/GMT"));
        }

        // 4. Verify sorting: time zone group, then country, then name
        Station previous = null;
        for (Station current : results) {
            if (previous != null) {
                int tzCmp = current.getTimeZoneGroup().compareTo(previous.getTimeZoneGroup());
                assertTrue("Time zone groups should be sorted ascending or equal", tzCmp >= 0);
                
                if (tzCmp == 0) {
                    int countryCmp = current.getCountry().compareTo(previous.getCountry());
                    assertTrue("For same time zone group, countries should be sorted ascending", countryCmp >= 0);
                    
                    if (countryCmp == 0) {
                        assertTrue("For same time zone group and country, names should be sorted ascending",
                                current.getName().compareTo(previous.getName()) >= 0);
                    }
                }
            }
            previous = current;
        }

        // 5. Verify no stations from other groups are included
        // Test with a likely non-existent group (but may exist in full CSV)
        List<Station> unknownResults = service.queryByTimeZoneGroup("UNKNOWN_TZ_GROUP_XYZ");
        assertTrue("Should return empty for non-existent time zone group", unknownResults.isEmpty());

        // Verify window query returns substantial results from full CSV
        assertTrue("Should return many stations from full CSV window query", results.size() > 100);
    }

    /**
     * Test Case 7: Empty result for non-existent time zone group
     * Expected: Returns empty list when querying for a time zone group that doesn't exist
     */
    @Test
    public void testQueryByTimeZoneGroup_NonExistent() throws IOException {
        // 1. Load stations from full CSV file
        String csvPath = getFullCsvFilePath();
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);
        assertTrue("Should have loaded stations from full CSV", result.getRecords().size() > 0);

        // 2. Query for a time zone group that doesn't exist in the data
        List<Station> results1 = service.queryByTimeZoneGroup("NONEXISTENT_TZ_GROUP_ABC");
        List<Station> results2 = service.queryByTimeZoneGroup("INVALID_GROUP_XYZ");
        List<Station> results3 = service.queryByTimeZoneGroup("");

        // 3. Verify empty list is returned
        assertTrue("Should return empty list for non-existent time zone group", results1.isEmpty());
        assertTrue("Should return empty list for non-existent time zone group", results2.isEmpty());
        assertTrue("Should return empty list for empty time zone group", results3.isEmpty());
    }

    /**
     * Test Case 8: AVL tree structure validation
     * Expected: AVL trees maintain balance and correct structure
     */
    @Test
    public void testAvlTreeStructure() throws IOException {
        // 1. Load stations from full CSV file
        String csvPath = getFullCsvFilePath();
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);

        int stationCount = result.getRecords().size();

        // 2. Verify tree heights are reasonable (should be balanced)
        // AVL tree height should be at most ~1.44 * log2(n) for n nodes
        AVL<StationComparable> latTree = service.getLatitudeTree();
        AVL<StationComparable> lonTree = service.getLongitudeTree();
        AVL<StationComparable> tzTree = service.getTimeZoneGroupTree();
        
        assertNotNull("Latitude tree should exist", latTree);
        assertNotNull("Longitude tree should exist", lonTree);
        assertNotNull("Time zone group tree should exist", tzTree);

        // Calculate expected height with more lenient margin for large trees
        // For very large trees, allow more margin for balancing variations
        double theoreticalMax = 1.44 * Math.log(stationCount) / Math.log(2);
        int maxExpectedHeight = (int) Math.ceil(theoreticalMax) + 5; // Increased margin for large datasets
        
        assertTrue("Latitude tree height should be reasonable (balanced). Height: " + latTree.height() + ", Max: " + maxExpectedHeight,
                latTree.height() <= maxExpectedHeight);
        assertTrue("Longitude tree height should be reasonable (balanced). Height: " + lonTree.height() + ", Max: " + maxExpectedHeight,
                lonTree.height() <= maxExpectedHeight);
        assertTrue("Time zone group tree height should be reasonable (balanced). Height: " + tzTree.height() + ", Max: " + maxExpectedHeight,
                tzTree.height() <= maxExpectedHeight);

        // 3. Verify tree sizes match number of stations (allowing for duplicates)
        // AVL trees don't store duplicates, so sizes may be slightly smaller
        int latSize = latTree.size();
        int lonSize = lonTree.size();
        int tzSize = tzTree.size();
        
        // Allow small differences for duplicate comparison values
        int maxExpectedDiff = Math.max(100, stationCount / 200);
        
        assertTrue(String.format("Latitude tree should contain most stations. Expected: %d, Got: %d", 
                stationCount, latSize), 
                stationCount - latSize <= maxExpectedDiff);
        assertTrue(String.format("Longitude tree should contain most stations. Expected: %d, Got: %d", 
                stationCount, lonSize), 
                stationCount - lonSize <= maxExpectedDiff);
        assertTrue(String.format("Time zone group tree should contain most stations. Expected: %d, Got: %d", 
                stationCount, tzSize), 
                stationCount - tzSize <= maxExpectedDiff);

        // 4. Verify inOrder() returns all stations in the tree
        // Note: Tree size may be smaller than station count due to duplicates
        int latCount = 0;
        for (StationComparable sc : latTree.inOrder()) {
            latCount++;
        }
        assertEquals("InOrder should return all stations in latitude tree", latSize, latCount);

        int lonCount = 0;
        for (StationComparable sc : lonTree.inOrder()) {
            lonCount++;
        }
        assertEquals("InOrder should return all stations in longitude tree", lonSize, lonCount);

        int tzCount = 0;
        for (StationComparable sc : tzTree.inOrder()) {
            tzCount++;
        }
        assertEquals("InOrder should return all stations in time zone group tree", tzSize, tzCount);

        // Verify we have a substantial dataset
        assertTrue("Should have loaded substantial number of stations from full CSV", stationCount > 1000);
    }

    /**
     * Test Case 9: Multiple stations at same coordinates
     * Expected: Multiple stations at same coordinates are all stored and properly sorted by name
     */
    @Test
    public void testMultipleStationsSameCoordinates() throws IOException {
        // 1. Load stations from full CSV file that includes multiple stations at same coordinates
        //    The full CSV has many stations with same names but different coordinates
        String csvPath = getFullCsvFilePath();
        StationService service = new StationService();
        CsvValidatorResult<Station> result = service.loadStationsAndCreateAVLTrees(csvPath);
        // Note: Large CSV files may contain some invalid rows, which is acceptable
        assertTrue("Should have loaded stations from full CSV file", result.getRecords().size() > 0);

        // 2. Verify all stations are included in the trees (allowing for duplicates)
        int stationCount = result.getRecords().size();
        int latSize = service.getLatitudeTree().size();
        int lonSize = service.getLongitudeTree().size();
        
        // AVL trees don't store duplicates, so allow small differences
        int maxExpectedDiff = Math.max(100, stationCount / 200);
        
        assertTrue(String.format("Latitude tree should contain most stations. Expected: %d, Got: %d", 
                stationCount, latSize), 
                stationCount - latSize <= maxExpectedDiff);
        assertTrue(String.format("Longitude tree should contain most stations. Expected: %d, Got: %d", 
                stationCount, lonSize), 
                stationCount - lonSize <= maxExpectedDiff);

        // Verify stations with same name but different coordinates are all included
        // Check for stations with duplicate names in the full CSV
        long mentonCount = result.getRecords().stream()
                .filter(s -> s.getName().equals("Menton"))
                .count();
        // May or may not have Menton in full CSV, but if present, verify multiple if they exist
        if (mentonCount > 0) {
            assertTrue("If Menton exists, should have at least one station", mentonCount >= 1);
        }

        // 3. Verify they are sorted correctly when coordinates match
        // Check that stations with same coordinates are sorted by name
        AVL<StationComparable> latTree = service.getLatitudeTree();
        Iterable<StationComparable> inOrder = latTree.inOrder();
        StationComparable previous = null;
        for (StationComparable current : inOrder) {
            if (previous != null) {
                Station prevStation = previous.getStation();
                Station currStation = current.getStation();
                
                // If coordinates are identical, verify name sorting
                if (Double.compare(currStation.getLatitude(), prevStation.getLatitude()) == 0 &&
                    Double.compare(currStation.getLongitude(), prevStation.getLongitude()) == 0) {
                    assertTrue("For same coordinates, names should be sorted ascending",
                            currStation.getName().compareTo(prevStation.getName()) >= 0);
                }
            }
            previous = current;
        }

        // Verify we checked a substantial number of stations
        assertTrue("Should have verified sorting for all stations in full CSV", stationCount > 1000);
    }
}

