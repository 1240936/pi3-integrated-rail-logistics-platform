package USEI14;

import main.controller.MaxFlowService;
import main.domain.MaxFlowResult;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test cases for USEI14: Maximum Flow (Edmonds-Karp algorithm).
 *
 * Uses stations.csv and lines.csv files from the warehouseMngt directory.
 */
public class TestMaxFlowService {

    /**
     * Gets the path to stations.csv file.
     * The file is located in the warehouseMngt directory.
     */
    private String getStationsCsvPath() {
        String[] pathsToTry = {
                "../../stations.csv",
                "testFiles/stations.csv",
                "warehouseMngt/stations.csv",
                "../warehouseMngt/stations.csv"
        };

        for (String path : pathsToTry) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        return "../../stations.csv";
    }

    /**
     * Gets the path to lines.csv file.
     * The file is located in the warehouseMngt directory.
     */
    private String getLinesCsvPath() {
        String[] pathsToTry = {
                "../../lines.csv",
                "testFiles/lines.csv",
                "warehouseMngt/lines.csv",
                "../warehouseMngt/lines.csv"
        };

        for (String path : pathsToTry) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        return "../../lines.csv";
    }

    /**
     * Test Case 1 - Compute max flow for valid source and sink
     */
    @Test
    public void testComputeMaxFlow_ValidSourceAndSink() throws IOException {

        MaxFlowService service = new MaxFlowService();

        String sourceId = "6";
        String sinkId = "8";

        MaxFlowResult result = service.computeMaxFlow(
                getStationsCsvPath(),
                getLinesCsvPath(),
                sourceId,
                sinkId
        );

        assertNotNull("Result should not be null", result);
        assertNotNull("Source station should not be null", result.getSource());
        assertNotNull("Sink station should not be null", result.getSink());
        assertTrue("Max flow must be non-negative",
                result.getMaxFlowValue() >= 0);
    }

    /**
     * Test Case 2 - Verify result structure
     */
    @Test
    public void testComputeMaxFlow_ResultStructure() throws IOException {

        MaxFlowService service = new MaxFlowService();

        MaxFlowResult result = service.computeMaxFlow(
                getStationsCsvPath(),
                getLinesCsvPath(),
                "6",
                "8"
        );

        assertNotNull(result);
        assertNotNull("Source must not be null", result.getSource());
        assertNotNull("Sink must not be null", result.getSink());

        double maxFlow = result.getMaxFlowValue();

        assertFalse("Max flow must not be NaN", Double.isNaN(maxFlow));
        assertFalse("Max flow must not be infinite", Double.isInfinite(maxFlow));
        assertTrue("Max flow must be >= 0", maxFlow >= 0);
    }

    /**
     * Test Case 3 - Verify max flow properties
     */
    @Test
    public void testComputeMaxFlow_FlowProperties() throws IOException {

        MaxFlowService service = new MaxFlowService();

        MaxFlowResult result = service.computeMaxFlow(
                getStationsCsvPath(),
                getLinesCsvPath(),
                "6",
                "8"
        );

        double maxFlow = result.getMaxFlowValue();

        assertTrue("Max flow must be non-negative", maxFlow >= 0);

        // Defensive upper bound (detects algorithmic errors)
        assertTrue("Max flow value is unrealistically large",
                maxFlow < 1_000_000);
    }

    /**
     * Test Case 4 - Test with disconnected source and sink
     */
    @Test
    public void testComputeMaxFlow_DisconnectedSourceAndSink() throws IOException {

        MaxFlowService service = new MaxFlowService();

        String sourceId = "1";
        String sinkId = "999";

        try {
            MaxFlowResult result = service.computeMaxFlow(
                    getStationsCsvPath(),
                    getLinesCsvPath(),
                    sourceId,
                    sinkId
            );

            assertEquals("Max flow should be 0 for disconnected stations",
                    0.0, result.getMaxFlowValue(), 0.0001);

        } catch (IllegalArgumentException e) {
            // Valid behavior if station ID does not exist
            assertTrue(true);
        }
    }
}
