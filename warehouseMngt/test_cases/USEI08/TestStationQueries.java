package USEI08;

import main.controller.StationService;
import main.domain.Station;
import java.util.stream.Collectors;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test cases for USEI08: Range queries with filters on stations.
 *
 * Tests range query functionality:
 * - Load stations from CSV file
 * - Build balanced 2D-tree (requires USEI06 and USEI07)
 * - Perform range queries within geographic bounds
 * - Apply filters (isCity, isMainStation, country)
 * - Validate query results and filtering behavior
 */
public class TestStationQueries {

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
     * Test Case 1: Basic range query without filters
     * Expected: Returns all stations within the specified geographic bounds (Belgium/Netherlands region).
     */
    @Test
    public void testRangeQuery_BasicQuery() throws IOException {
        Path tempFile = null;

        // Define the bounds that contain 4 known stations in the Belgium/Netherlands region
        final double MIN_LAT = 50.0;
        final double MAX_LAT = 52.0;
        final double MIN_LON = 3.0;
        final double MAX_LON = 6.0;
        final Set<String> EXPECTED_STATIONS = Set.of(
                "Nieuwkerken-Waas",
                "Balegem-Dorp",
                "Maastricht Aachen Airport",
                "Liege-Guillemins"
        );
        final int EXPECTED_COUNT = EXPECTED_STATIONS.size();

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Perform the range query
            List<Station> results = service.rangeQuery(MIN_LAT, MAX_LAT, MIN_LON, MAX_LON);

            // 3. Verification: Check the size of the result list
            assertNotNull("The result list must not be null.", results);
            assertEquals("The number of stations returned must be exactly " + EXPECTED_COUNT + ".",
                    EXPECTED_COUNT,
                    results.size());

            // 4. Verification: Check if ALL expected stations are present in the results
            Set<String> actualNames = results.stream()
                    .map(Station::getName)
                    .collect(Collectors.toSet());

            assertTrue("All expected stations must be present in the results.",
                    actualNames.containsAll(EXPECTED_STATIONS));

            // 5. Verification: Check if ALL returned stations are strictly within the bounds
            for (Station station : results) {
                double lat = station.getLatitude();
                double lon = station.getLongitude();

                assertTrue("Station " + station.getName() + " has invalid latitude.",
                        lat >= MIN_LAT && lat <= MAX_LAT);
                assertTrue("Station " + station.getName() + " has invalid longitude.",
                        lon >= MIN_LON && lon <= MAX_LON);
            }

        } finally {
            // 6. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 2: Range query with isCity filter
     * Expected: Returns only stations matching the isCity filter within bounds
     */
    @Test
    public void testRangeQuery_WithCityFilter() throws IOException {
        Path tempFile = null;

        // Define the bounds that contain a mix of isCity=True and isCity=False stations (South France region)
        final double MIN_LAT = 43.0;
        final double MAX_LAT = 45.0;
        final double MIN_LON = 5.0;
        final double MAX_LON = 8.0;

        // We filter for stations that ARE CITIES (isCityFilter = True)
        final Boolean IS_CITY_FILTER = true;

        // Expected result: Only one station in this rectangle is marked as isCity=True
        final Set<String> EXPECTED_STATIONS = Set.of("Chateau-Arnoux-St-Auban"); // The 'city' entry
        final int EXPECTED_COUNT = EXPECTED_STATIONS.size();

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Perform the range query with the isCity filter
            List<Station> results = service.rangeQueryWithFilters(
                    MIN_LAT, MAX_LAT, MIN_LON, MAX_LON,
                    IS_CITY_FILTER, null, null // Filter only by isCity=True
            );

            // 3. Verification: Check the size of the result list
            assertNotNull("The result list must not be null.", results);
            assertEquals("The number of stations returned must be exactly " + EXPECTED_COUNT +
                            " (Only the 'isCity=True' station).",
                    EXPECTED_COUNT,
                    results.size());

            // 4. Verification: Check if ALL expected stations are present in the results
            Set<String> actualNames = results.stream()
                    .map(Station::getName)
                    .collect(Collectors.toSet());

            assertEquals("The actual stations returned must match the expected set of city stations.",
                    EXPECTED_STATIONS,
                    actualNames);

            // 5. Verification: Check if ALL returned stations match the filter and are strictly within bounds
            for (Station station : results) {
                // Check Filter: isCity must be TRUE
                assertTrue("All returned stations must match the isCityFilter (isCity=True).",
                        station.isCity());

                // Check Bounds
                double lat = station.getLatitude();
                double lon = station.getLongitude();
                assertTrue("Station " + station.getName() + " has invalid latitude (out of range).",
                        lat >= MIN_LAT && lat <= MAX_LAT);
                assertTrue("Station " + station.getName() + " has invalid longitude (out of range).",
                        lon >= MIN_LON && lon <= MAX_LON);
            }

        } finally {
            // 6. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 3: Range query with isMainStation filter
     * Expected: Returns only stations matching the isMainStation filter within bounds
     */
    @Test
    public void testRangeQuery_WithMainStationFilter() throws IOException {
        Path tempFile = null;

        // Define the bounds that contain a mix of main stations and non-main stations (South France region)
        final double MIN_LAT = 43.0;
        final double MAX_LAT = 45.0;
        final double MIN_LON = 5.0;
        final double MAX_LON = 8.0;

        // We filter for stations that ARE NOT MAIN STATIONS (isMainStationFilter = False)
        final Boolean IS_MAIN_STATION_FILTER = false;

        // Expected result: Only one station in this rectangle is marked as isMainStation=False
        final Set<String> EXPECTED_STATIONS = Set.of("Chateau-Arnoux-St-Auban"); // The 'city' entry, which is not a main station
        final int EXPECTED_COUNT = EXPECTED_STATIONS.size();

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Perform the range query with the isMainStation filter
            List<Station> results = service.rangeQueryWithFilters(
                    MIN_LAT, MAX_LAT, MIN_LON, MAX_LON,
                    null, IS_MAIN_STATION_FILTER, null // Filter only by isMainStation=False
            );

            // 3. Verification: Check the size of the result list
            assertNotNull("The result list must not be null.", results);
            assertEquals("The number of stations returned must be exactly " + EXPECTED_COUNT +
                            " (Only the 'isMainStation=False' station).",
                    EXPECTED_COUNT,
                    results.size());

            // 4. Verification: Check if ALL expected stations are present in the results
            Set<String> actualNames = results.stream()
                    .map(Station::getName)
                    .collect(Collectors.toSet());

            assertEquals("The actual stations returned must match the expected set of non-main stations.",
                    EXPECTED_STATIONS,
                    actualNames);

            // 5. Verification: Check if ALL returned stations match the filter and are strictly within bounds
            for (Station station : results) {
                // Check Filter: isMainStation must be FALSE
                assertFalse("All returned stations must match the isMainStationFilter (isMainStation=False).",
                        station.isMainStation());

                // Check Bounds
                double lat = station.getLatitude();
                double lon = station.getLongitude();
                assertTrue("Station " + station.getName() + " has invalid latitude (out of range).",
                        lat >= MIN_LAT && lat <= MAX_LAT);
                assertTrue("Station " + station.getName() + " has invalid longitude (out of range).",
                        lon >= MIN_LON && lon <= MAX_LON);
            }

        } finally {
            // 6. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 4: Range query with country filter
     * Expected: Returns only stations from the specified country within bounds.
     */
    @Test
    public void testRangeQuery_WithCountryFilter() throws IOException {
        Path tempFile = null;

        // Define the bounds that contain a mix of Portuguese (PT) and Spanish (ES) stations
        final double MIN_LAT = 36.0;
        final double MAX_LAT = 42.0;
        final double MIN_LON = -10.0;
        final double MAX_LON = 1.0;
        final int TOTAL_EXPECTED_COUNT_ALL = 6;

        // Expected stations if NO filter is applied (or filter="all")
        final Set<String> ALL_EXPECTED_NAMES = Set.of(
                "Ferreiras", "Ginjal (Belmonte)", "Aeroporto de Lisboa Humberto Delgado", // PT
                "Torre del Mar", "Trevelez", "Valladolid Campo Grande" // ES
        );

        // Expected stations if filtered by "ES"
        final Set<String> ES_EXPECTED_NAMES = Set.of(
                "Torre del Mar", "Trevelez", "Valladolid Campo Grande"
        );
        final int ES_EXPECTED_COUNT = ES_EXPECTED_NAMES.size();


        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // --- PART 1: Filter by Specific Country (ES) ---

            // 2. Execution: Perform the range query with countryFilter="ES"
            List<Station> results_es = service.rangeQueryWithFilters(
                    MIN_LAT, MAX_LAT, MIN_LON, MAX_LON,
                    null, null, "ES"
            );

            // 3. Verification (ES): Check the size and content
            assertNotNull("The result list for ES filter must not be null.", results_es);
            assertEquals("Expected exactly " + ES_EXPECTED_COUNT + " stations for country 'ES' within bounds.",
                    ES_EXPECTED_COUNT,
                    results_es.size());

            Set<String> actualNames_es = results_es.stream().map(Station::getName).collect(Collectors.toSet());
            assertEquals("The actual stations returned for ES must match the expected set.",
                    ES_EXPECTED_NAMES,
                    actualNames_es);

            // 4. Verification (ES): Check if ALL returned stations are from the specified country
            for (Station station : results_es) {
                assertEquals("All returned stations must be from the specified country ('ES').",
                        "ES",
                        station.getCountry());
            }

            // --- PART 2: Filter by "all" (No Filter) ---

            // 5. Execution: Perform the range query with countryFilter="all"
            List<Station> results_all = service.rangeQueryWithFilters(
                    MIN_LAT, MAX_LAT, MIN_LON, MAX_LON,
                    null, null, "all"
            );

            // 6. Verification ("all"): Check the size and content
            assertNotNull("The result list for 'all' filter must not be null.", results_all);
            assertEquals("Expected exactly " + TOTAL_EXPECTED_COUNT_ALL + " stations for country 'all' within bounds.",
                    TOTAL_EXPECTED_COUNT_ALL,
                    results_all.size());

            Set<String> actualNames_all = results_all.stream().map(Station::getName).collect(Collectors.toSet());
            assertEquals("The actual stations returned for 'all' must match the expected set (PT + ES).",
                    ALL_EXPECTED_NAMES,
                    actualNames_all);


        } finally {
            // 7. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 5: Range query with multiple filters combined
     * Expected: Returns stations matching all specified filters within bounds.
     * This test uses a combination of filters (isCity=True AND isMainStation=True AND Country=FR)
     * that should yield ZERO results in the defined boundary, proving the strict AND logic.
     */
    @Test
    public void testRangeQuery_WithMultipleFilters() throws IOException {
        Path tempFile = null;

        // Define the bounds (South France region)
        final double MIN_LAT = 43.0;
        final double MAX_LAT = 45.0;
        final double MIN_LON = 5.0;
        final double MAX_LON = 8.0;

        // Define the strict combined filters
        final Boolean IS_CITY_FILTER = true;
        final Boolean IS_MAIN_STATION_FILTER = true;
        final String COUNTRY_FILTER = "FR";

        // Expected result for this strict combination in this area
        final int EXPECTED_COUNT = 0;

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Perform the range query with ALL filters applied
            List<Station> results = service.rangeQueryWithFilters(
                    MIN_LAT, MAX_LAT, MIN_LON, MAX_LON,
                    IS_CITY_FILTER, IS_MAIN_STATION_FILTER, COUNTRY_FILTER
            );

            // 3. Verification: Check the size of the result list
            assertNotNull("The result list must not be null.", results);
            assertEquals("The number of stations returned must be exactly " + EXPECTED_COUNT +
                            " (The strict AND combination should exclude all stations).",
                    EXPECTED_COUNT,
                    results.size());

            // 4. Verification: Ensure the list is empty
            assertTrue("The list of results should be empty, confirming all stations were excluded by the strict filters.",
                    results.isEmpty());


        } finally {
            // 5. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 6: Range query with null filters (no filtering)
     * Expected: Returns all stations within bounds, same as basic range query.
     */
    @Test
    public void testRangeQuery_WithNullFilters() throws IOException {
        Path tempFile = null;

        // Define the bounds (Belgium/Netherlands region, 4 expected stations)
        final double MIN_LAT = 50.0;
        final double MAX_LAT = 52.0;
        final double MIN_LON = 3.0;
        final double MAX_LON = 6.0;

        final int EXPECTED_COUNT = 4;

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // --- PART 1: Query using rangeQueryWithFilters (with all filters = null) ---
            List<Station> results_filtered = service.rangeQueryWithFilters(
                    MIN_LAT, MAX_LAT, MIN_LON, MAX_LON,
                    null, null, null // All filters set to null
            );

            // --- PART 2: Query using the basic rangeQuery method ---
            List<Station> results_basic = service.rangeQuery(MIN_LAT, MAX_LAT, MIN_LON, MAX_LON);


            // 2. Verification: Check the size of both results
            assertNotNull("The filtered result list must not be null.", results_filtered);
            assertNotNull("The basic result list must not be null.", results_basic);

            assertEquals("Both queries must return the same number of stations (" + EXPECTED_COUNT + ").",
                    results_basic.size(),
                    results_filtered.size());
            assertEquals("The size of the basic query must be correct.",
                    EXPECTED_COUNT,
                    results_basic.size());


            // 3. Verification: Check if both lists contain the exact same stations

            // Convert to Set<String> of names for easy comparison, ignoring order
            Set<String> names_filtered = results_filtered.stream().map(Station::getName).collect(Collectors.toSet());
            Set<String> names_basic = results_basic.stream().map(Station::getName).collect(Collectors.toSet());

            assertEquals("The set of station names returned by both methods must be identical.",
                    names_basic,
                    names_filtered);
        } finally {
            // 4. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 7: Range query with empty result set
     * Expected: Returns empty list when no stations match the criteria (empty geographic bounds).
     */
    @Test
    public void testRangeQuery_EmptyResult() throws IOException {
        Path tempFile = null;

        // Define bounds in the Atlantic Ocean (far from all European stations)
        final double MIN_LAT = 30.0;
        final double MAX_LAT = 35.0;
        final double MIN_LON = -20.0;
        final double MAX_LON = -15.0;

        final int EXPECTED_COUNT = 0;

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Perform the range query with bounds containing no stations
            List<Station> results = service.rangeQuery(MIN_LAT, MAX_LAT, MIN_LON, MAX_LON);

            // 3. Verification: Check the size of the result list
            assertNotNull("The result list must not be null.", results);
            assertEquals("The number of stations returned must be exactly " + EXPECTED_COUNT +
                            " as the geographic bounds contain no stations.",
                    EXPECTED_COUNT,
                    results.size());

            // 4. Verification: Ensure the list is empty
            assertTrue("The list of results should be empty.",
                    results.isEmpty());


        } finally {
            // 5. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 8: Range query boundary conditions
     * Expected: Correctly handles stations exactly on the boundary (min/max lat/lon).
     */
    @Test
    public void testRangeQuery_BoundaryConditions() throws IOException {
        Path tempFile = null;

        // --- CONSTANTS ---
        // Coordinates for Nieuwkerken-Waas (BE)
        final double TEST_LAT = 51.185342;
        final double TEST_LON = 4.185314;
        final String TEST_STATION_NAME = "Nieuwkerken-Waas";
        final double DELTA = 0.000001;
        // -----------------


        try {
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // SCENARIO 1: Inclusion Test (Bounds = Single Point)
            List<Station> results_inclusion = service.rangeQuery(
                    TEST_LAT, TEST_LAT,
                    TEST_LON, TEST_LON
            );

            assertEquals("SCENARIO 1: The query must return exactly 1 station when bounds equal the single point.",
                    1,
                    results_inclusion.size());
            assertEquals("SCENARIO 1: The returned station must be the expected one.",
                    TEST_STATION_NAME,
                    results_inclusion.get(0).getName());


            // SCENARIO 2: Exclusion Test (Bounds Just Outside: maxLat < Lat)
            List<Station> results_exclusion_max_lat = service.rangeQuery(
                    TEST_LAT - DELTA, TEST_LAT - DELTA,
                    TEST_LON, TEST_LON
            );

            assertEquals("SCENARIO 2: Query must return 0 stations when maxLat is slightly below the station's latitude.",
                    0,
                    results_exclusion_max_lat.size());


            // SCENARIO 3: Exclusion Test (Invalid Range: minLon > maxLon)
            List<Station> results_invalid_range = service.rangeQuery(
                    TEST_LAT - 1.0, TEST_LAT + 1.0,
                    TEST_LON + 1.0, TEST_LON - 1.0
            );

            assertEquals("SCENARIO 3: Query must return 0 stations when the range is invalid (min > max).",
                    0,
                    results_invalid_range.size());


        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 9: Range query with invalid bounds (min > max)
     * Expected: Returns empty list when bounds are invalid.
     */
    @Test
    public void testRangeQuery_InvalidBounds() throws IOException {
        Path tempFile = null;

        // --- CONSTANTES ---
        // Bounds that normally contain stations (Belgium/Netherlands region)
        final double VALID_MIN = 50.0;
        final double VALID_MAX = 52.0;
        final double VALID_LON_MIN = 3.0;
        final double VALID_LON_MAX = 6.0;

        // Invalid bounds setup: minLat > maxLat
        final double INVALID_MIN_LAT = VALID_MAX; // 52.0
        final double INVALID_MAX_LAT = VALID_MIN; // 50.0

        final int EXPECTED_COUNT = 0;
        // -----------------

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Perform the range query with minLat > maxLat
            List<Station> results = service.rangeQuery(
                    INVALID_MIN_LAT, INVALID_MAX_LAT, // minLat (52.0) > maxLat (50.0)
                    VALID_LON_MIN, VALID_LON_MAX
            );

            // 3. Verification: Check the size of the result list
            assertNotNull("The result list must not be null.", results);
            assertEquals("The number of stations returned must be exactly " + EXPECTED_COUNT +
                            " as the latitude range is invalid (min > max).",
                    EXPECTED_COUNT,
                    results.size());

            // 4. Verification: Ensure the list is empty
            assertTrue("The list of results should be empty.",
                    results.isEmpty());

        } finally {
            // 5. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 10: Range query with country filter set to "all"
     * Expected: Returns stations from all countries within bounds.
     */
    @Test
    public void testRangeQuery_CountryFilterAll() throws IOException {
        Path tempFile = null;

        // --- CONSTANTES ---
        // Bounds targeting Iberian Peninsula (PT and ES stations)
        final double MIN_LAT = 36.0;
        final double MAX_LAT = 42.0;
        final double MIN_LON = -10.0;
        final double MAX_LON = 1.0;

        final String COUNTRY_FILTER = "all";

        // Expected stations (PT + ES)
        final Set<String> ALL_EXPECTED_NAMES = Set.of(
                "Ferreiras", "Ginjal (Belmonte)", "Aeroporto de Lisboa Humberto Delgado",
                "Torre del Mar", "Trevelez", "Valladolid Campo Grande"
        );
        final int EXPECTED_COUNT = ALL_EXPECTED_NAMES.size();
        // -----------------

        try {
            // 1. Setup: Load stations and build tree
            tempFile = createTestCsvFile();
            StationService service = new StationService();
            service.loadStationsAndCreateAVLTrees(tempFile.toAbsolutePath().toString());
            service.buildBalanced2DTree();

            // 2. Execution: Perform the range query with countryFilter="all"
            List<Station> results_all = service.rangeQueryWithFilters(
                    MIN_LAT, MAX_LAT, MIN_LON, MAX_LON,
                    null, null, COUNTRY_FILTER
            );

            // 3. Verification: Check the size and content
            assertNotNull("The result list for 'all' filter must not be null.", results_all);
            assertEquals("Expected exactly " + EXPECTED_COUNT + " stations when countryFilter='all'.",
                    EXPECTED_COUNT,
                    results_all.size());

            Set<String> actualNames = results_all.stream().map(Station::getName).collect(Collectors.toSet());
            assertEquals("The actual stations returned for 'all' must match the expected set (PT + ES).",
                    ALL_EXPECTED_NAMES,
                    actualNames);

            // 4. Verification: Ensure multiple countries are represented
            Set<String> countriesFound = results_all.stream()
                    .map(Station::getCountry)
                    .collect(Collectors.toSet());

            assertTrue("The results must contain stations from Portugal (PT).", countriesFound.contains("PT"));
            assertTrue("The results must contain stations from Spain (ES).", countriesFound.contains("ES"));

            // 5. Verification (Implicit Check: Same as null filter):
            // Since EXPECTED_COUNT and ALL_EXPECTED_NAMES match the check in Test Case 6 (null filter),
            // this confirms that "all" behaves identically to null.

        } finally {
            // 6. Teardown: Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}

