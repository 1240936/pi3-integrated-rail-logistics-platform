package USEI13;

import main.controller.HubCentralityService;
import main.domain.HubScoreResult;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for USEI13: Hub Centrality Analysis.
 *
 * Tests hub centrality computation functionality:
 * - Load stations and connections from CSV files
 * - Compute centrality measures (betweenness, harmonic closeness, strength/degree)
 * - Calculate composite HubScore using formula: 0.35 * betweenness + 0.35 * harmonicCloseness + 0.30 * strength
 * - Verify results are sorted by hubScore descending
 * - Verify normalized measures are in [0,1] range
 */
public class TestHubCentralityService {

    /**
     * Gets the path to stations.csv file.
     * The file is located in the warehouseMngt directory.
     */
    private String getStationsCsvPath() {
        String[] pathsToTry = {
            "../../stations.csv",  // From test_cases/USEI13/ to warehouseMngt/
                "testFiles/stations.csv",     // From test_cases/ to warehouseMngt/
            "warehouseMngt/stations.csv",  // From project root
            "../warehouseMngt/stations.csv" // Alternative from test_cases/
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
            "../../lines.csv",  // From test_cases/USEI13/ to warehouseMngt/
                "testFiles/lines.csv",     // From test_cases/ to warehouseMngt/
            "warehouseMngt/lines.csv",  // From project root
            "../warehouseMngt/lines.csv" // Alternative from test_cases/
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
     * Test Case 1: Verify hub centrality computation returns valid results
     * Expected: Returns non-empty list of hub scores sorted by hubScore descending
     */
    @Test
    public void testComputeHubCentrality_ValidResults() throws IOException {
        // 1. Get CSV file paths
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // 2. Create service and compute hub centrality
        HubCentralityService service = new HubCentralityService();
        List<HubScoreResult> results = service.computeHubCentrality(stationsPath, connectionsPath);

        // 3. Verify results are not null and not empty
        assertNotNull("Results should not be null", results);
        assertFalse("Results should not be empty", results.isEmpty());
        assertTrue("Results should contain at least one station", results.size() > 0);
    }

    /**
     * Test Case 2: Verify results are sorted by hubScore descending
     * Expected: Each hub score is greater than or equal to the next one
     */
    @Test
    public void testComputeHubCentrality_SortedByHubScoreDescending() throws IOException {
        // 1. Get CSV file paths
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // 2. Create service and compute hub centrality
        HubCentralityService service = new HubCentralityService();
        List<HubScoreResult> results = service.computeHubCentrality(stationsPath, connectionsPath);

        // 3. Verify sorting order (allowing for ties)
        for (int i = 0; i < results.size() - 1; i++) {
            double currentScore = results.get(i).getHubScore();
            double nextScore = results.get(i + 1).getHubScore();
            assertTrue("Results should be sorted by hubScore descending (current: " + currentScore + 
                      " >= next: " + nextScore + ")", currentScore >= nextScore);
        }
    }

    /**
     * Test Case 3: Verify normalized measures are in [0,1] range
     * Expected: All betweenness, harmonicCloseness, and hubScore values are between 0 and 1 (inclusive)
     */
    @Test
    public void testComputeHubCentrality_NormalizedMeasuresInRange() throws IOException {
        // 1. Get CSV file paths
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // 2. Create service and compute hub centrality
        HubCentralityService service = new HubCentralityService();
        List<HubScoreResult> results = service.computeHubCentrality(stationsPath, connectionsPath);

        // 3. Verify normalized measures are in [0,1] range
        for (HubScoreResult result : results) {
            double betweenness = result.getBetweenness();
            double harmonicCloseness = result.getHarmonicCloseness();
            double hubScore = result.getHubScore();

            assertTrue("Betweenness should be in [0,1] range, got: " + betweenness,
                      betweenness >= 0.0 && betweenness <= 1.0);
            assertTrue("HarmonicCloseness should be in [0,1] range, got: " + harmonicCloseness,
                      harmonicCloseness >= 0.0 && harmonicCloseness <= 1.0);
            assertTrue("HubScore should be in [0,1] range, got: " + hubScore,
                      hubScore >= 0.0 && hubScore <= 1.0);
        }
    }

    /**
     * Test Case 4: Verify hub score formula (0.35 * betweenness + 0.35 * harmonicCloseness + 0.30 * strengthNorm)
     * Expected: Hub score matches the weighted combination of normalized measures
     */
    @Test
    public void testComputeHubCentrality_HubScoreFormula() throws IOException {
        // 1. Get CSV file paths
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // 2. Create service and compute hub centrality
        HubCentralityService service = new HubCentralityService();
        List<HubScoreResult> results = service.computeHubCentrality(stationsPath, connectionsPath);

        // 3. Verify hub score formula for each result
        // Formula: hubScore = 0.35 * betweenness + 0.35 * harmonicCloseness + 0.30 * strengthNorm
        // Note: We need to compute strengthNorm from strength values
        // First, find min and max strength to normalize
        double minStrength = results.stream()
                .mapToDouble(HubScoreResult::getStrength)
                .min()
                .orElse(0.0);
        double maxStrength = results.stream()
                .mapToDouble(HubScoreResult::getStrength)
                .max()
                .orElse(1.0);

        for (HubScoreResult result : results) {
            double betweenness = result.getBetweenness();
            double harmonicCloseness = result.getHarmonicCloseness();
            double strength = result.getStrength();
            double expectedHubScore = result.getHubScore();

            // Calculate normalized strength
            double strengthNorm = (maxStrength - minStrength) > 0 ?
                    (strength - minStrength) / (maxStrength - minStrength) : 0.0;

            // Calculate expected hub score using the formula
            double calculatedHubScore = 0.35 * betweenness + 0.35 * harmonicCloseness + 0.30 * strengthNorm;

            // Allow for small floating point precision errors
            assertEquals("HubScore should match formula (0.35*betweenness + 0.35*harmonicCloseness + 0.30*strengthNorm)",
                        calculatedHubScore, expectedHubScore, 1e-9);
        }
    }

    /**
     * Test Case 5: Verify all stations have valid identifiers and names
     * Expected: Each result has non-null, non-empty station ID and name
     */
    @Test
    public void testComputeHubCentrality_ValidStationIdentifiers() throws IOException {
        // 1. Get CSV file paths
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // 2. Create service and compute hub centrality
        HubCentralityService service = new HubCentralityService();
        List<HubScoreResult> results = service.computeHubCentrality(stationsPath, connectionsPath);

        // 3. Verify each result has valid station identifiers
        for (HubScoreResult result : results) {
            String stationId = result.getStationId();
            String stationName = result.getStationName();

            assertNotNull("Station ID should not be null", stationId);
            assertNotNull("Station name should not be null", stationName);
            assertFalse("Station ID should not be empty", stationId.isEmpty());
            assertFalse("Station name should not be empty", stationName.isEmpty());
        }
    }

    /**
     * Test Case 6: Verify centrality measures are non-negative
     * Expected: Degree, strength, betweenness, harmonicCloseness, and hubScore are all >= 0
     */
    @Test
    public void testComputeHubCentrality_NonNegativeMeasures() throws IOException {
        // 1. Get CSV file paths
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // 2. Create service and compute hub centrality
        HubCentralityService service = new HubCentralityService();
        List<HubScoreResult> results = service.computeHubCentrality(stationsPath, connectionsPath);

        // 3. Verify all measures are non-negative
        for (HubScoreResult result : results) {
            assertTrue("Degree should be non-negative, got: " + result.getDegree(),
                      result.getDegree() >= 0);
            assertTrue("Strength should be non-negative, got: " + result.getStrength(),
                      result.getStrength() >= 0.0);
            assertTrue("Betweenness should be non-negative, got: " + result.getBetweenness(),
                      result.getBetweenness() >= 0.0);
            assertTrue("HarmonicCloseness should be non-negative, got: " + result.getHarmonicCloseness(),
                      result.getHarmonicCloseness() >= 0.0);
            assertTrue("HubScore should be non-negative, got: " + result.getHubScore(),
                      result.getHubScore() >= 0.0);
        }
    }

    /**
     * Test Case 7: Verify top hub station has highest hub score
     * Expected: The first station in the sorted list has the highest hub score
     */
    @Test
    public void testComputeHubCentrality_TopHubStation() throws IOException {
        // 1. Get CSV file paths
        String stationsPath = getStationsCsvPath();
        String connectionsPath = getLinesCsvPath();

        // 2. Create service and compute hub centrality
        HubCentralityService service = new HubCentralityService();
        List<HubScoreResult> results = service.computeHubCentrality(stationsPath, connectionsPath);

        // 3. Verify first station has the highest hub score
        if (results.size() > 1) {
            double topHubScore = results.get(0).getHubScore();
            
            for (int i = 1; i < results.size(); i++) {
                double currentScore = results.get(i).getHubScore();
                assertTrue("Top hub station should have highest score. Top: " + topHubScore + 
                          ", Station " + i + ": " + currentScore,
                          topHubScore >= currentScore);
            }
        }
    }
}
