package USEI11;

import main.controller.RailwayUpgradeService;
import main.domain.Station;
import main.domain.TopologicalSortResult;
import main.repositories.StationsWithIdCsvLoader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test cases for USEI11: Directed Line Upgrade Plan (Topological Sort).
 *
 * Tests the RailwayUpgradeService functionality:
 * - Compute topological order for DAG (no cycles)
 * - Handle graphs with cycles
 * - Verify result structure consistency
 * - Verify all stations are included in topological order
 */
public class TestRailwayUpgradeService {

    private RailwayUpgradeService service;

    public TestRailwayUpgradeService() {
        this.service = new RailwayUpgradeService();
    }

    /**
     * Gets the path to stations.csv file.
     * The file is located in the warehouseMngt directory.
     */
    private String getStationsCsvPath() {
        String[] pathsToTry = {
                "../../stations.csv",  // From test_cases/USEI11/ to warehouseMngt/
                "../stations.csv",     // From test_cases/ to warehouseMngt/
                "warehouseMngt/stations.csv",  // From project root
                "../warehouseMngt/stations.csv", // Alternative from test_cases/
                "testFiles/stations.csv"  // From warehouseMngt/testFiles/
        };

        for (String path : pathsToTry) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        // Fallback - return relative path (test will fail if file not found, which is correct)
        return "../../stations.csv";
    }

    /**
     * Gets the path to lines.csv file.
     * The file is located in the warehouseMngt directory.
     */
    private String getLinesCsvPath() {
        String[] pathsToTry = {
                "../../lines.csv",  // From test_cases/USEI11/ to warehouseMngt/
                "../lines.csv",     // From test_cases/ to warehouseMngt/
                "warehouseMngt/lines.csv",  // From project root
                "../warehouseMngt/lines.csv", // Alternative from test_cases/
                "testFiles/lines.csv"  // From warehouseMngt/testFiles/
        };

        for (String path : pathsToTry) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        // Fallback - return relative path (test will fail if file not found, which is correct)
        return "../../lines.csv";
    }


    /**
     * Helper method to create a temporary CSV file with stations that form a cycle.
     */
    private java.nio.file.Path createCycleStationsCsv() throws IOException {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("stations_cycle", ".csv");
        String content = "Station id,Station,Lat,Lon,CoordX,CoordY\n" +
                "1,StationA,50.0,4.0,0,0\n" +
                "2,StationB,51.0,5.0,0,0\n" +
                "3,StationC,52.0,6.0,0,0\n";
        java.nio.file.Files.write(tempFile, content.getBytes());
        return tempFile;
    }

    /**
     * Helper method to create a temporary CSV file with connections that form a cycle.
     */
    private java.nio.file.Path createCycleConnectionsCsv() throws IOException {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("connections_cycle", ".csv");
        String content = "departure_stid,arrival_stid,dist,capacity,cost\n" +
                "1,2,10.0,100,10.0\n" +
                "2,3,20.0,100,20.0\n" +
                "3,1,30.0,100,30.0\n";  // Forms a cycle: 1->2->3->1
        java.nio.file.Files.write(tempFile, content.getBytes());
        return tempFile;
    }

    /**
     * Test Case 1 - Compute topological order (with real data)
     * Test Intent: Verify that the service returns a valid result structure when processing real CSV data.
     * Expected: Result is not null, and either has a valid topological order (if DAG) or identifies cycles.
     */
    @Test
    public void testComputeUpgradeOrder_DAG() throws IOException {
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // Verify test data files exist
        assertTrue("Stations CSV file should exist", new File(stationsPath).exists());
        assertTrue("Connections CSV file should exist", new File(connectionsPath).exists());

        // Compute upgrade order
        TopologicalSortResult result = service.computeUpgradeOrder(stationsPath, connectionsPath);

        // Verify result structure
        assertNotNull("Result should not be null", result);

        if (!result.hasCycle()) {
            // If no cycles, verify topological order exists and is valid
            assertNotNull("Topological order should not be null when no cycles", result.getTopologicalOrder());
            assertNull("Cycle stations should be null when no cycles", result.getCycleStations());

            List<Station> order = result.getTopologicalOrder();
            assertFalse("Topological order should not be empty", order.isEmpty());

            // Verify all stations in order are unique
            Set<Station> uniqueStations = new java.util.HashSet<>(order);
            assertEquals("All stations in topological order should be unique",
                    order.size(), uniqueStations.size());
        } else {
            // If cycles exist, verify cycle information
            assertNull("Topological order should be null when cycles exist", result.getTopologicalOrder());
            assertNotNull("Cycle stations should not be null when cycles exist", result.getCycleStations());
            assertFalse("Cycle stations set should not be empty", result.getCycleStations().isEmpty());
        }
    }

    /**
     * Test Case 2 - Handle graphs with cycles
     * Test Intent: Verify that when the graph contains cycles, the service correctly identifies cycle stations.
     * Expected: Result indicates cycles exist, cycleStations set is not null and contains stations involved in cycles.
     */
    @Test
    public void testComputeUpgradeOrder_WithCycles() throws IOException {
        // Create temporary CSV files with cycle data
        java.nio.file.Path stationsCyclePath = createCycleStationsCsv();
        java.nio.file.Path connectionsCyclePath = createCycleConnectionsCsv();

        try {
            // Compute upgrade order (should detect cycles)
            TopologicalSortResult result = service.computeUpgradeOrder(
                    stationsCyclePath.toString(),
                    connectionsCyclePath.toString()
            );

            // Verify result structure
            assertNotNull("Result should not be null", result);
            assertTrue("Result should indicate cycles exist", result.hasCycle());
            assertNull("Topological order should be null when cycles exist", result.getTopologicalOrder());
            assertNotNull("Cycle stations should not be null when cycles exist", result.getCycleStations());

            // Verify cycle stations set contains stations involved in cycle
            Set<Station> cycleStations = result.getCycleStations();
            assertFalse("Cycle stations set should not be empty", cycleStations.isEmpty());

            // Verify at least 3 stations are in cycle (1->2->3->1)
            assertTrue("Cycle should contain at least 3 stations", cycleStations.size() >= 3);

        } finally {
            // Clean up temporary files
            java.nio.file.Files.deleteIfExists(stationsCyclePath);
            java.nio.file.Files.deleteIfExists(connectionsCyclePath);
        }
    }

    /**
     * Test Case 3 - Verify result structure consistency
     * Test Intent: Verify that the result structure is consistent (if hasCycle is true, topologicalOrder is null, etc.).
     * Expected: Result fields are consistent with the hasCycle flag.
     */
    @Test
    public void testComputeUpgradeOrder_ResultStructure() throws IOException {
        // Test 1: Real data case
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        TopologicalSortResult realDataResult = service.computeUpgradeOrder(stationsPath, connectionsPath);

        // Verify structure consistency based on hasCycle flag
        if (realDataResult.hasCycle()) {
            // When hasCycle is true:
            assertNull("Cycle graph should have null topological order", realDataResult.getTopologicalOrder());
            assertNotNull("Cycle graph should have non-null cycle stations", realDataResult.getCycleStations());
            assertFalse("Cycle stations set should not be empty", realDataResult.getCycleStations().isEmpty());
        } else {
            // When hasCycle is false:
            assertNotNull("DAG should have non-null topological order", realDataResult.getTopologicalOrder());
            assertNull("DAG should have null cycle stations", realDataResult.getCycleStations());
            assertFalse("DAG topological order should not be empty", realDataResult.getTopologicalOrder().isEmpty());
        }

        // Test 2: Cycle case (using temporary test data)
        java.nio.file.Path stationsCyclePath = createCycleStationsCsv();
        java.nio.file.Path connectionsCyclePath = createCycleConnectionsCsv();

        try {
            TopologicalSortResult cycleResult = service.computeUpgradeOrder(
                    stationsCyclePath.toString(),
                    connectionsCyclePath.toString()
            );

            // When hasCycle is true:
            assertTrue("Cycle graph should have hasCycle = true", cycleResult.hasCycle());
            assertNull("Cycle graph should have null topological order", cycleResult.getTopologicalOrder());
            assertNotNull("Cycle graph should have non-null cycle stations", cycleResult.getCycleStations());
            assertFalse("Cycle stations set should not be empty", cycleResult.getCycleStations().isEmpty());

        } finally {
            // Clean up temporary files
            java.nio.file.Files.deleteIfExists(stationsCyclePath);
            java.nio.file.Files.deleteIfExists(connectionsCyclePath);
        }
    }

    /**
     * Test Case 4 - Verify all stations are included in topological order (if DAG)
     * Test Intent: When no cycles exist, verify that all stations from the graph appear in the topological order.
     * Expected: The size of topological order matches the number of stations in the graph.
     */
    @Test
    public void testComputeUpgradeOrder_AllStationsIncluded() throws IOException {
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // Load stations to count total number
        java.util.Map<String, Station> stationMap =
                StationsWithIdCsvLoader.loadWithIdMap(stationsPath);
        int totalStations = stationMap.size();

        assertTrue("Should have at least one station in test data", totalStations > 0);

        // Compute upgrade order
        TopologicalSortResult result = service.computeUpgradeOrder(stationsPath, connectionsPath);

        // Only proceed if no cycles (topological order only exists for DAGs)
        if (result.hasCycle()) {
            // Skip this test if cycles exist - topological order is only valid for DAGs
            return;
        }

        // Verify all stations are included
        List<Station> topologicalOrder = result.getTopologicalOrder();
        assertNotNull("Topological order should not be null", topologicalOrder);
        assertEquals("Topological order should contain all stations",
                totalStations, topologicalOrder.size());

        // Verify all stations from the map appear in the topological order
        Set<Station> orderSet = new java.util.HashSet<>(topologicalOrder);
        Set<Station> mapStations = new java.util.HashSet<>(stationMap.values());

        assertEquals("All stations from map should be in topological order",
                mapStations.size(), orderSet.size());
        assertTrue("Topological order should contain all stations from map",
                orderSet.containsAll(mapStations));
    }
}