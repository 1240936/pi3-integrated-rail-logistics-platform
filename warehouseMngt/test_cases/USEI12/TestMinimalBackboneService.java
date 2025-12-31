package USEI12;

import main.controller.MinimalBackboneService;
import main.graph.Edge;
import org.junit.Before;
import org.junit.Test;


import main.domain.MinimalBackboneResult;
import main.domain.Station;
import main.graph.Graph;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.Assert.*;
import java.nio.file.Files;


/**
 * Test cases for USEI12: Minimal Backbone Network (MST using Prim's algorithm).
 *
 * TODO: Implement the following test cases based on the test intents below.
 * Use stations.csv and lines.csv files from the warehouseMngt directory.
 */
public class TestMinimalBackboneService {

    private MinimalBackboneService service;
    private String outputDir;

    @Before
    public void setUp() throws IOException {
        // Inicializa o serviço (o StationService pode ser null se não for usado no MST)
        service = new MinimalBackboneService(null);

        // Inicializa o diretório temporário
        Path tempPath = createTempOutputDir();
        outputDir = tempPath.toString();
    }

    /**
     * Gets the path to stations.csv file.
     * The file is located in the warehouseMngt directory.
     */
    private String getStationsCsvPath() {
        String[] pathsToTry = {
            "../../stations.csv",  // From test_cases/USEI12/ to warehouseMngt/
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
            "../../lines.csv",  // From test_cases/USEI12/ to warehouseMngt/
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
     * Creates a temporary output directory for test files.
     */
    private Path createTempOutputDir() throws IOException {
        return Files.createTempDirectory("mst-test-");
    }

    /**
     * Helper method to recursively delete a directory and its contents.
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * Test Case 1: Verify MST basic properties (vertices and bidirectional edges).
     */
    @Test
    public void testComputeMinimalBackbone_ValidGraph() throws IOException, InterruptedException {
        String stationsPath = getStationsCsvPath();
        String linesPath = getLinesCsvPath();

        MinimalBackboneResult result = service.computeMinimalBackbone(stationsPath, linesPath, outputDir);

        // Verify result exists
        assertNotNull("Result should not be null", result);
        Graph<Station, Double> mst = result.getMstGraph();
        int n = mst.numVertices();
        int e = mst.numEdges();

        // Check for (n-1)*2 edges because graph stores both directions (A->B and B->A)
        int expectedEdges = (n - 1) * 2;

        assertTrue("Graph should contain vertices", n > 0);
        assertEquals("MST should have (n-1)*2 edges (bidirectional)", expectedEdges, e);
        assertEquals("Edge list size should match graph count", e, result.getMstEdges().size());
        assertTrue("Total distance should be positive", result.getTotalDistance() > 0);
    }

    /**
     * Test Case 2: Verify that all vertices are connected (no isolated stations).
     */
    @Test
    public void testComputeMinimalBackbone_MSTConnectivity() throws IOException, InterruptedException {
        String stationsPath = getStationsCsvPath();
        String linesPath = getLinesCsvPath();

        MinimalBackboneResult result = service.computeMinimalBackbone(stationsPath, linesPath, outputDir);
        Graph<Station, Double> mst = result.getMstGraph();

        // Every station must have at least one connection
        if (mst.numVertices() > 1) {
            for (Station s : mst.vertices()) {
                assertTrue("Station " + s.getName() + " should be connected", mst.outDegree(s) > 0);
            }
        }
        assertEquals("Connected tree requires (n-1)*2 edges", (mst.numVertices() - 1) * 2, mst.numEdges());
    }

    /**
     * Test Case 3: Verify that the DOT file is generated and not empty.
     */
    @Test
    public void testComputeMinimalBackbone_DotFileGeneration() throws IOException, InterruptedException {
        String stationsPath = getStationsCsvPath();
        String linesPath = getLinesCsvPath();

        MinimalBackboneResult result = service.computeMinimalBackbone(stationsPath, linesPath, outputDir);

        String dotPath = result.getDotFilePath();
        assertNotNull("DOT path should not be null", dotPath);

        File dotFile = new File(dotPath);
        // Check if file exists on disk and has content
        assertTrue("DOT file should exist", dotFile.exists());
        assertTrue("DOT file should not be empty", dotFile.length() > 0);
        assertTrue("File extension should be .dot", dotPath.toLowerCase().endsWith(".dot"));
    }

    /**
     * Test Case 4: Verify weight consistency between total distance and edge list.
     */
    @Test
    public void testComputeMinimalBackbone_MinimumWeight() throws IOException, InterruptedException {
        String stationsPath = getStationsCsvPath();
        String linesPath = getLinesCsvPath();

        MinimalBackboneResult result = service.computeMinimalBackbone(stationsPath, linesPath, outputDir);
        double totalDist = result.getTotalDistance();

        // Calculate sum of edges manually to compare with result distance
        double sumOfEdges = 0;
        for (Edge<Station, Double> edge : result.getMstEdges()) {
            sumOfEdges += edge.getWeight();
        }

        // Use delta for double precision comparison
        assertEquals("Total distance must match edge weights sum", sumOfEdges, totalDist, 0.001);
        assertNotNull("Edge list should not be empty", result.getMstEdges());
    }
}
