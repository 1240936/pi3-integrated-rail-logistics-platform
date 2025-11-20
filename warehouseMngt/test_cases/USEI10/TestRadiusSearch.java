package USEI10;

import main.controller.StationService;
import main.domain.*;
import main.repositories.CsvValidatorResult;
import main.repositories.StationsCsvLoader;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test cases for USEI10: Find all stations within a radius R (km) of a target point.
 * 
 * Tests radius search functionality:
 * - Load stations from CSV file
 * - Build balanced 2D-tree
 * - Perform radius search queries with various parameters
 * - Validate results, distances, summary statistics, and result tree ordering
 */
public class TestRadiusSearch {

    /**
     * Helper method to create a temporary CSV file with test data.
     * Uses the CSV format provided for testing.
     * TODO: Add test data from the provided CSV format when needed
     */
    private Path createTestCsvFile() throws IOException {
        String csvContent = String.join("\n",
                "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",

                "FR,\"('Europe/Paris',)\",CET,Macau,45.0040981,-0.6200068,False,False,False",
                "FR,\"('Europe/Paris',)\",CET,Pauillac,45.2038438,-0.7551342,False,False,False",
                "FR,\"('Europe/Paris',)\",CET,Facture-Biganos,44.637384,-0.96621,False,False,False",
                "FR,\"('Europe/Paris',)\",CET,Lesparre,45.3035166,-0.9454015,False,False,False",
                "FR,\"('Europe/Paris',)\",CET,Gaillan,45.316667,-0.95,False,False,False",
                "FR,\"('Europe/Paris',)\",CET,Queyrac,45.366667,-0.983333,False,False,False",
                "FR,\"('Europe/Paris',)\",CET,Soulac-sur-Mer,45.5081769,-1.1175894,False,True,False",
                "FR,\"('Europe/Paris',)\",CET,Le Verdon,45.5492065,-1.0650925,False,True,False",
                "FR,\"('Europe/Paris',)\",CET,Pointe-de-Grave,45.567067,-1.0658575,False,False,False",
                "FR,\"('Europe/Paris',)\",CET,Cauderan-Merignac,44.84288,-0.6274337,False,False,False",

                "CH,\"('Europe/Zurich',)\",CET,Vernayaz,46.130069,7.045007,False,False,False",

                "FR,\"('Europe/Paris',)\",CET,Chedde,45.9263464,6.7197662,False,False,False",

                "ES,\"('Europe/Madrid',)\",CET,Santa Ana de Pusa,39.76216,-4.71296,True,False,False",
                "ES,\"('Europe/Madrid',)\",CET,Santa Barbara de Casa,37.7988799,-7.188727,True,False,False",
                "ES,\"('Europe/Madrid',)\",CET,Santacara,42.3753822,-1.5517379,True,False,False",
                "ES,\"('Europe/Madrid',)\",CET,Santa Cilia,42.560188,-0.71438,True,False,False",
                "ES,\"('Europe/Madrid',)\",CET,Santa Coloma de Queralt,41.5364821,1.3873009,True,False,False"
        );

        Path tempFile = Files.createTempFile("stations-", ".csv");
        Files.writeString(tempFile, csvContent, StandardCharsets.UTF_8);
        return tempFile;
    }

    /**
     * Test Case 1: Radius search with small radius
     * Expected: Returns stations within the specified radius, sorted by distance
     */
    @Test
    public void testRadiusSearch_SmallRadius() throws IOException {
        // 1. Create the CSV file
        StationService stationService = new StationService();
        Path csvFile = createTestCsvFile();
        // 2. Load stations and insert into AVL tree
        CsvValidatorResult<Station> result = stationService.loadStationsAndCreateAVLTrees(csvFile.toAbsolutePath().toString());
        // 3. Create balanced 2D tree
        stationService.buildBalanced2DTree();
        double targetLat = 45.0;
        double targetLon = -1.0;
        double radiusKm = 50.0;
        // 4. Search with small radius: service.radiusSearch(targetLat, targetLon, radiusKm)
        AVL<StationWithDistanceComparable> foundStations =  stationService.radiusSearch(targetLat, targetLon, radiusKm).getResultTree();
        // 5. Verify all returned stations are within radius
        for (StationWithDistanceComparable s : foundStations.inOrder()) {
            double distance = s.getDistanceKm();
            assertTrue("Station " + s.getStation().getName()
                    + " is outside the radius: " + distance + " km", distance <= radiusKm);
        }
        // 6. Verify result tree contains correct stations
        // List expected station names within ~50km of (45.0, -1.0)
        Set<String> expectedStations = Set.of(
                "Macau",
                "Pauillac",
                "Facture-Biganos",
                "Lesparre",
                "Gaillan",
                "Queyrac",
                "Cauderan-Merignac"
        );

        Set<String> foundStationNames = new HashSet<>();
        for (StationWithDistanceComparable s : foundStations.inOrder()) {
            foundStationNames.add(s.getStation().getName());
        }
        assertEquals("Returned stations do not match expected stations",
                expectedStations, foundStationNames);
        // 7. Clean up temporary CSV file
        Files.deleteIfExists(csvFile);
    }

    /**
     * Test Case 2: Radius search with large radius
     * Expected: Returns more stations, all within the specified radius
     */
    @Test
    public void testRadiusSearch_LargeRadius() throws IOException {
        // 1. Create the CSV file
        StationService stationService = new StationService();
        Path csvFile = createTestCsvFile();

        try {
            // 2. Load stations and insert into AVL tree
            CsvValidatorResult<Station> result = stationService
                    .loadStationsAndCreateAVLTrees(csvFile.toAbsolutePath().toString());

            // 3. Create balanced 2D tree
            stationService.buildBalanced2DTree();

            // 4. Define target location and large search radius
            double targetLat = 45.0;
            double targetLon = -1.0;
            double radiusKm = 600.0; // much larger radius to include all stations

            // 5. Perform radius search
            AVL<StationWithDistanceComparable> foundStations =
                    stationService.radiusSearch(targetLat, targetLon, radiusKm).getResultTree();

            // 6. Verify all returned stations are within radius
            for (StationWithDistanceComparable s : foundStations.inOrder()) {
                double distance = s.getDistanceKm();
                assertTrue("Station " + s.getStation().getName()
                        + " is outside the radius: " + distance + " km", distance <= radiusKm);
            }

            // 7. Verify result tree contains all stations in the CSV
            Set<String> expectedStations = Set.of(
                    "Facture-Biganos",
                    "Gaillan",
                    "Macau",
                    "Santa Cilia",
                    "Santa Coloma de Queralt",
                    "Pointe-de-Grave",
                    "Le Verdon",
                    "Soulac-sur-Mer",
                    "Queyrac",
                    "Pauillac",
                    "Santacara",
                    "Lesparre",
                    "Cauderan-Merignac"
            );

            Set<String> foundStationNames = new HashSet<>();
            for (StationWithDistanceComparable s : foundStations.inOrder()) {
                foundStationNames.add(s.getStation().getName());
            }

            assertEquals("Returned stations do not match expected stations",
                    expectedStations, foundStationNames);

        } finally {
            // 8. Clean up temporary CSV file
            Files.deleteIfExists(csvFile);
        }
    }


    /**
     * Test Case 3: Result tree ordering (distance ASC, name DESC)
     * Expected: Results sorted by distance ascending, then by name descending
     */
    @Test
    public void testRadiusSearch_ResultTreeOrdering() throws IOException {
        // 1. Create CSV file and load stations
        StationService stationService = new StationService();
        Path csvFile = createTestCsvFile();

        try {
            CsvValidatorResult<Station> result = stationService
                    .loadStationsAndCreateAVLTrees(csvFile.toAbsolutePath().toString());

            // 2. Build balanced 2D tree
            stationService.buildBalanced2DTree();

            // 3. Define target location and radius
            double targetLat = 45.0;
            double targetLon = -1.0;
            double radiusKm = 50.0; // small radius to get multiple stations nearby

            // 4. Perform radius search
            AVL<StationWithDistanceComparable> foundStations =
                    stationService.radiusSearch(targetLat, targetLon, radiusKm).getResultTree();

            // 5. Iterate through inOrder() to verify ordering
            StationWithDistanceComparable previous = null;
            for (StationWithDistanceComparable current : foundStations.inOrder()) {
                if (previous != null) {
                    double prevDistance = previous.getDistanceKm();
                    double currDistance = current.getDistanceKm();

                    // Primary ordering: distance ascending
                    assertTrue("Distance ordering incorrect: " + previous.getStation().getName() +
                                    " (" + prevDistance + " km) before " +
                                    current.getStation().getName() + " (" + currDistance + " km)",
                            currDistance >= prevDistance);

                    // Secondary ordering: name descending if distances are equal
                    if (Double.compare(prevDistance, currDistance) == 0) {
                        String prevName = previous.getStation().getName();
                        String currName = current.getStation().getName();
                        assertTrue("Name ordering incorrect for equal distances: " + prevName +
                                        " before " + currName,
                                prevName.compareTo(currName) >= 0); // DESC
                    }
                }
                previous = current;
            }

        } finally {
            // 6. Clean up temporary CSV file
            Files.deleteIfExists(csvFile);
        }
    }

    /**
     * Test Case 4: Summary statistics by country
     * Expected: Summary contains correct counts for each country
     */
    @Test
    public void testRadiusSearch_SummaryByCountry() throws IOException {
        // TODO: Implement test
        // 1. Load stations from multiple countries
        // 2. Build tree
        // 3. Perform radius search
        // 4. Verify summaryByCountry contains correct counts
        // 5. Verify sum of counts matches totalStations
    }

    /**
     * Test Case 5: Summary statistics by isCity
     * Expected: Summary contains correct counts for cities and non-cities
     */
    @Test
    public void testRadiusSearch_SummaryByIsCity() throws IOException {
        // TODO: Implement test
        // 1. Load stations with mix of cities and non-cities
        // 2. Build tree
        // 3. Perform radius search
        // 4. Verify summaryByIsCity contains correct counts
        // 5. Verify sum of counts matches totalStations
    }

    /**
     * Test Case 6: Radius search with zero radius
     * Expected: Returns empty result
     */
    @Test
    public void testRadiusSearch_ZeroRadius() throws IOException {
        // TODO: Implement test
        // 1. Load stations and build tree
        // 2. Search with zero radius
        // 3. Verify empty result (0 stations)
        // 4. Verify empty result tree and summaries
    }

    /**
     * Test Case 7: Radius search with negative radius
     * Expected: Returns empty result (invalid radius)
     */
    @Test
    public void testRadiusSearch_NegativeRadius() throws IOException {
        // TODO: Implement test
        // 1. Load stations and build tree
        // 2. Search with negative radius
        // 3. Verify empty result is returned
    }

    /**
     * Test Case 8: Distance calculation using Haversine formula
     * Expected: Distances are accurately calculated using Haversine formula
     */
    @Test
    public void testRadiusSearch_HaversineDistance() throws IOException {
        // TODO: Implement test
        // 1. Load stations including one at a known location
        // 2. Search at that exact location with appropriate radius
        // 3. Verify the station at that location has distance ≈ 0
        // 4. Verify distance calculations are accurate
    }

    /**
     * Test Case 9: Empty tree handling
     * Expected: Returns empty result when tree is empty
     */
    @Test
    public void testRadiusSearch_EmptyTree() {
        // TODO: Implement test
        // 1. Create StationService without loading stations
        // 2. Perform radius search
        // 3. Verify empty result is returned
    }
}

