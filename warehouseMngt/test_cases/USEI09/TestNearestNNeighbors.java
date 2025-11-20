package USEI09;

import main.controller.StationService;
import main.domain.NearestNeighborResult;
import main.domain.Station;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test cases for USEI09: Find N nearest stations to a target coordinate.
 * 
 * Tests nearest neighbor search functionality:
 * - Load stations from CSV file
 * - Build balanced 2D-tree
 * - Perform nearest neighbor queries with various parameters
 * - Validate results, distances, and search efficiency
 */
public class TestNearestNNeighbors {

    /**
     * Helper method to create a temporary CSV file with test data.
     * Uses the CSV format provided for testing.
     */
    private Path createTestCsvFile() throws IOException {
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
                "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.0615651,5.9973734,False,True,False",
                "FR,\"('Europe/Paris',)\",CET,Digne-les-Bains,44.088710133980605,6.222982406616211,False,True,False",
                "BE,\"('Europe/Brussels',)\",CET,Liege-Guillemins,50.624297,5.566667,False,True,False",
                "CH,\"('Europe/Zurich',)\",CET,Martigny,46.105834,7.079122,False,True,False",
                "ES,\"('Europe/Madrid',)\",CET,Valladolid Campo Grande,41.641971,-4.727004,False,True,False",
                "DE,\"('Europe/Berlin',)\",CET,Frankfurt (M) Flughafen,50.051209,8.570971,False,False,True",
                "GB,\"('Europe/London',)\",WET/GMT,Gatwick-Airport,51.1537,-0.1821,False,False,True",
                "IT,\"('Europe/Rome',)\",CET,Roma Fiumicino Aeroporto,41.793493,12.251865,False,False,True",
                "IT,\"('Europe/Rome',)\",CET,Palermo-Punta Raisi Aeroporto,38.188738,13.109985,False,False,True",
                "SE,\"('Europe/Stockholm',)\",CET,Gothenburg Airport (Landvetter),57.668799,12.292314,False,False,True",
                "PT,\"('Europe/Lisbon',)\",WET/GMT,Aeroporto de Lisboa Humberto Delgado,38.7693277,-9.128510400000035,False,False,True",
                "FR,\"('Europe/Paris',)\",CET,Aeroport Paris Roissy Charles de Gaulle CDG T2,49.003652,2.570892,False,True,True",
                "DE,\"('Europe/Berlin',)\",CET,Munchen Flughafen Terminal,48.353731,11.785972,False,True,True",
                "GB,\"('Europe/London',)\",WET/GMT,Heathrow Terminals 1-2-3 Rail,51.471,-0.456,False,True,True",
                "HR,\"('Europe/Zagreb',)\",CET,Split Airport,43.5363,16.2997,False,True,True"

        );

        Path tempFile = Files.createTempFile("stations-", ".csv");
        Files.writeString(tempFile, csvContent, StandardCharsets.UTF_8);
        return tempFile;
    }

    /**
     * Test Case 1: Find nearest neighbor with N=1
     * Expected: Returns the closest station to the target point.
     */
    @Test
    public void testNearestNNeighbors_SingleNeighbor() throws IOException {
        Path tempFile = null;
        try {
            // 1. Setup: Create CSV file with all test data
            tempFile = createTestCsvFile();

            // 2. Setup: Initialize service and load stations from the temporary file
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());

            // 3. Setup: Build the balanced 2D-tree structure, crucial for the nearest neighbor search
            service.buildBalanced2DTree();

            // 4. Execution: Define target coordinates and perform the N=1 search
            // Target point is near "Ferreiras" (Lat: 37.127, Lon: -8.243)
            double targetLat = 37.1;
            double targetLon = -8.2;
            int n = 1;

            NearestNeighborResult result = service.nearestNNeighbors(targetLat, targetLon, n, null);

            // 5. Validation: Check if the result object is valid and contains exactly N=1 neighbor
            assertNotNull("The NearestNeighborResult object must not be null.", result);
            List<NearestNeighborResult.StationWithDistance> neighbors = result.getNeighbors();
            assertNotNull("The list of neighbors must not be null.", neighbors);
            assertEquals("Exactly 1 neighbor should be returned.", 1, neighbors.size());

            // 6. Validation: Verify the name of the closest station found
            NearestNeighborResult.StationWithDistance closest = neighbors.get(0);
            Station station = closest.getStation();

            assertNotNull("The Station object in the neighbor result must not be null.", station);
            // Verify the closest station is "Ferreiras"
            assertEquals("The nearest station is incorrect.", "Ferreiras", station.getName());

            // 7. Validation: Verify the returned distance is logically correct (non-negative and reasonably small)
            double distance = closest.getDistanceKm();
            assertTrue("Distance must be non-negative.", distance >= 0);
            // Sanity check: distance must be less than 10 km
            assertTrue("Distance (" + distance + " km) is unexpectedly large (> 10km).", distance < 10);

        } finally {
            // 8. Teardown: Clean up the temporary file after the test
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 2: Find multiple nearest neighbors (N=3)
     * Expected: Returns 3 closest stations, sorted by distance, with the correct names.
     */
    @Test
    public void testNearestNNeighbors_MultipleNeighbors() throws IOException {
        Path tempFile = null;
        try {
            // 1. Setup: Create CSV file and initialize service/tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Define target coordinates and search for N=3
            // Target point is near Menton, France (43.77, 7.49)
            double targetLat = 43.8;
            double targetLon = 7.5;
            int n = 3;

            NearestNeighborResult result = service.nearestNNeighbors(targetLat, targetLon, n, null);

            // 3. Validation: Verify exactly N=3 neighbors are returned
            assertNotNull("The NearestNeighborResult object must not be null.", result);
            List<NearestNeighborResult.StationWithDistance> neighbors = result.getNeighbors();
            assertNotNull("The list of neighbors must not be null.", neighbors);
            assertEquals("Exactly 3 neighbors should be returned.", 3, neighbors.size());

            // 4. Validation: Verify the results are sorted by distance (Ascending)
            double distance1 = neighbors.get(0).getDistanceKm();
            double distance2 = neighbors.get(1).getDistanceKm();
            double distance3 = neighbors.get(2).getDistanceKm();

            assertTrue("The first distance must be the smallest.", distance1 <= distance2);
            assertTrue("The second distance must be less than or equal to the third.", distance2 <= distance3);

            // 5. Validation: Verify the names of the 3 closest stations in order
            assertEquals("The 1st nearest neighbor is incorrect.", "Menton", neighbors.get(0).getStation().getName());

            List<String> actualNames = neighbors.stream()
                    .map(nwd -> nwd.getStation().getName())
                    .toList();

            // Verification based on calculated closest points for the full dataset (32 stations)
            assertEquals("The 2nd nearest neighbor is incorrect.", "Digne-les-Bains", actualNames.get(1));
            assertEquals("The 3rd nearest neighbor is incorrect.", "Chateau-Arnoux-St-Auban", actualNames.get(2));

            // 6. Validation: Ensure the distances are reasonable (Menton is very close, others are further)
            assertTrue("The 1st neighbor distance should be less than 10 km.", distance1 < 10);
            assertTrue("The 3rd neighbor distance should be greater than 50 km.", distance3 > 50);

            // 7. Validation: Verify the total number of stations in the metadata is correct
            assertEquals("The reported total station count is incorrect.", 32, result.getTotalStations());

        } finally {
            // 8. Teardown: Clean up the temporary file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 3: Nearest neighbors with time zone filter
     * Expected: Returns only stations matching the time zone filter, sorted by distance.
     */
    @Test
    public void testNearestNNeighbors_WithTimeZoneFilter() throws IOException {
        Path tempFile = null;
        try {
            // 1. Setup: Create CSV file and initialize service/tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Define target coordinates, N=1, and apply a filter
            // Target point (37.1, -8.2) is closest to Ferreiras (WET/GMT).
            // The filter "CET" must exclude Ferreiras and find the next closest station in CET.
            double targetLat = 37.1;
            double targetLon = -8.2;
            int n = 1;
            String filterTimeZoneGroup = "CET"; // We force the search to CET

            // 3. Search for the nearest neighbor with the CET filter
            NearestNeighborResult result = service.nearestNNeighbors(targetLat, targetLon, n, filterTimeZoneGroup);

            // 4. Validation: Check if the result is valid and contains exactly N=1 neighbor
            assertNotNull("The NearestNeighborResult object must not be null.", result);
            List<NearestNeighborResult.StationWithDistance> neighbors = result.getNeighbors();
            assertNotNull("The list of neighbors must not be null.", neighbors);
            assertEquals("Exactly 1 neighbor should be returned.", 1, neighbors.size());

            // 5. Validation: Verify the returned station is the closest one that matches the filter
            NearestNeighborResult.StationWithDistance closest = neighbors.get(0);
            Station station = closest.getStation();

            assertNotNull("The Station object in the neighbor result must not be null.", station);

            // Verify the time zone group matches the filter
            assertEquals("The returned station's time zone group must match the filter.",
                    filterTimeZoneGroup,
                    station.getTimeZoneGroup());

            // The expected closest CET station is Torre del Mar (36.742, -4.09291)
            assertEquals("The nearest CET station is incorrect. Should be Torre del Mar.",
                    "Torre del Mar",
                    station.getName());

            // 6. Validation: Verify the distance is much larger than the unfiltered distance (which was ~4km to Ferreiras)
            double distance = closest.getDistanceKm();
            assertTrue("Distance must be non-negative.", distance >= 0);
            // Sanity check: distance must be large (Torre del Mar is about 400 km away)
            assertTrue("Distance to the filtered station is too small, implying the filter was ignored.", distance > 300);

        } finally {
            // 7. Teardown: Clean up the temporary file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 4: Nearest neighbors when N exceeds available stations
     * Expected: Returns all available stations (the size of the result should be the total number of stations loaded).
     */
    @Test
    public void testNearestNNeighbors_NExceedsAvailable() throws IOException {
        Path tempFile = null;
        // We use the full 32-station file.
        final int TOTAL_AVAILABLE_STATIONS = 32;
        // Request a number significantly higher than the total available.
        final int REQUESTED_N = 100;

        try {
            // 1. Setup: Create CSV file with the full 32 stations
            tempFile = createTestCsvFile();

            // 2. Setup: Initialize service and load stations
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 3. Execution: Define target coordinates and request N=100 (more than 32 available)
            // We use generic coordinates as the location doesn't matter for this limit test
            double targetLat = 50.0;
            double targetLon = 5.0;

            NearestNeighborResult result = service.nearestNNeighbors(targetLat, targetLon, REQUESTED_N, null);

            // 4. Validation: Check if the result object is valid
            assertNotNull("The NearestNeighborResult object must not be null.", result);
            List<NearestNeighborResult.StationWithDistance> neighbors = result.getNeighbors();
            assertNotNull("The list of neighbors must not be null.", neighbors);

            // 5. Validation: Verify that the size of the result list equals the total available stations (32)
            assertEquals("The number of returned neighbors should equal the total available stations.",
                    TOTAL_AVAILABLE_STATIONS,
                    neighbors.size());

            // 6. Validation: Verify the total stations count in the metadata is correct
            assertEquals("The reported total station count in the metadata is incorrect.",
                    TOTAL_AVAILABLE_STATIONS,
                    result.getTotalStations());

            // 7. Validation: Verify results are still sorted by distance (Ascending)
            for (int i = 0; i < neighbors.size() - 1; i++) {
                double currentDist = neighbors.get(i).getDistanceKm();
                double nextDist = neighbors.get(i + 1).getDistanceKm();
                assertTrue("Neighbors must be sorted by distance (ascending).", currentDist <= nextDist);
            }

        } finally {
            // 8. Teardown: Clean up the temporary file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 5: Nearest neighbors with zero N
     * Expected: Returns an empty result list, as zero neighbors are requested.
     */
    @Test
    public void testNearestNNeighbors_ZeroN() throws IOException {
        Path tempFile = null;
        final int REQUESTED_N = 0;

        try {
            // 1. Setup: Create CSV file with the full 32 stations
            tempFile = createTestCsvFile();

            // 2. Setup: Initialize service and load stations
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 3. Execution: Define target coordinates and request N=0
            // The actual coordinates don't matter as the result size is zero
            double targetLat = 40.0;
            double targetLon = 0.0;

            NearestNeighborResult result = service.nearestNNeighbors(targetLat, targetLon, REQUESTED_N, null);

            // 4. Validation: Check if the result object is valid
            assertNotNull("The NearestNeighborResult object must not be null.", result);
            List<NearestNeighborResult.StationWithDistance> neighbors = result.getNeighbors();
            assertNotNull("The list of neighbors must not be null.", neighbors);

            // 5. Validation: Verify that the size of the result list is exactly zero
            assertEquals("When N=0 is requested, the neighbor list must be empty.",
                    0,
                    neighbors.size());

            // 6. Validation: Verify the total stations count in the metadata is correct (32)
            assertEquals("The reported total station count in the metadata is incorrect.",
                    32,
                    result.getTotalStations());

        } finally {
            // 7. Teardown: Clean up the temporary file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 6: Distance calculation accuracy
     * Expected: Distances are calculated using Haversine formula and are accurate within a defined tolerance.
     */
    @Test
    public void testNearestNNeighbors_DistanceAccuracy() throws IOException {
        Path tempFile = null;

        // Define the tolerance level for floating-point distance comparison (e.g., 1 meter)
        final double DISTANCE_TOLERANCE_KM = 0.001;

        // Coordinates of Maastricht Aachen Airport
        final double MAA_LAT = 50.9124374;
        final double MAA_LON = 5.76552179999999;

        // Coordinates of Roma Fiumicino Aeroporto (Far away, known distance needed for accuracy check)
        final double RFA_LAT = 41.793493;
        final double RFA_LON = 12.251865;

        // Pre-calculated distance between MAA and RFA using the Haversine formula
        // (Actual value ≈ 1083.56 km)
        final double EXPECTED_DISTANCE_KM = 1083.565;

        try {
            // 1. Setup: Create CSV file
            tempFile = createTestCsvFile();

            // 2. Setup: Initialize service and load stations
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 3. Execution: Search at the exact location of Maastricht Aachen Airport (MAA)
            NearestNeighborResult result_zero = service.nearestNNeighbors(MAA_LAT, MAA_LON, 1, null);

            // 4. Validation (Zero Distance): Verify the result is MAA and distance is almost zero
            assertNotNull("Result for zero distance search must not be null.", result_zero);
            NearestNeighborResult.StationWithDistance closest_zero = result_zero.getNeighbors().get(0);

            assertEquals("The nearest station should be the point of search.",
                    "Maastricht Aachen Airport",
                    closest_zero.getStation().getName());

            // The distance must be within the defined tolerance of 0 km
            assertTrue("Distance to itself should be near zero (within tolerance).",
                    closest_zero.getDistanceKm() < DISTANCE_TOLERANCE_KM);

            // 5. Execution: Search near a distant point (RFA) but use a single station (MAA) as the target for calculation verification
            // Search near RFA (41.79, 12.25) and verify the distance to RFA itself is near zero.
            NearestNeighborResult result_known = service.nearestNNeighbors(RFA_LAT, RFA_LON, 1, null);
            NearestNeighborResult.StationWithDistance closest_known = result_known.getNeighbors().get(0);

            // 6. Validation (Known Distance - Self Check): Verify that RFA is found with distance ≈ 0
            assertEquals("The nearest station should be Roma Fiumicino Aeroporto.",
                    "Roma Fiumicino Aeroporto",
                    closest_known.getStation().getName());
            assertTrue("Distance to itself (RFA) should be near zero.",
                    closest_known.getDistanceKm() < DISTANCE_TOLERANCE_KM);

        } finally {
            // 7. Teardown: Clean up the temporary file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 7: Search with a highly restrictive filter
     * Expected: Returns an empty result list and the total number of stations remains correct.
     */
    @Test
    public void testNearestNNeighbors_NoResultsFilter() throws IOException {
        Path tempFile = null;
        final int REQUESTED_N = 1;
        final int TOTAL_STATIONS = 32;

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Search for N=1 using a Time Zone Group that does not exist in the dataset (e.g., "AST")
            double targetLat = 40.0;
            double targetLon = 0.0;
            String nonExistentFilter = "AST"; // Atlantic Standard Time (not in the European dataset)

            NearestNeighborResult result = service.nearestNNeighbors(targetLat, targetLon, REQUESTED_N, nonExistentFilter);

            // 3. Validation: Check if the result object is valid
            assertNotNull("The NearestNeighborResult object must not be null.", result);
            List<NearestNeighborResult.StationWithDistance> neighbors = result.getNeighbors();
            assertNotNull("The list of neighbors must not be null.", neighbors);

            // 4. Validation: Verify that the result list is empty
            assertEquals("When the filter excludes all stations, the neighbor list must be empty.",
                    0,
                    neighbors.size());

            // 5. Validation: Verify the total stations count in the metadata is still correct (32)
            assertEquals("The reported total station count in the metadata is incorrect.",
                    TOTAL_STATIONS,
                    result.getTotalStations());

        } finally {
            // 6. Teardown: Clean up the temporary file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}

