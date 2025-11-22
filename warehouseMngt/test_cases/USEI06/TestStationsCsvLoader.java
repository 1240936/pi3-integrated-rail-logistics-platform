package USEI06;

import main.domain.Station;
import main.repositories.CsvValidatorResult;
import main.repositories.StationsCsvLoader;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for loading stations from CSV file.
 *
 * Tests CSV loading and validation:
 * - Load stations from CSV file with the new format
 * - Validate CSV structure and data
 * - Handle edge cases (empty values, invalid coordinates, etc.)
 * - Parse time zone format: "('Europe/Paris',)" -> "Europe/Paris"
 */
public class TestStationsCsvLoader {

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
     * Test Case 1: Load stations with updated CSV format
     * Expected: Stations loaded successfully with correct parsing
     */
    @Test
    public void testLoadStationsWithUpdatedFormat() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with header and data rows using the provided format
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False",
                    "PT,\"('Europe/Lisbon',)\",WET/GMT,Ginjal (Belmonte),40.36623,-7.35269,True,False,False",
                    "ES,\"('Europe/Madrid',)\",CET,Torre del Mar,36.742,-4.09291,True,False,False"
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify no validation errors
            assertFalse("Should have no validation errors", result.hasErrors());
            assertEquals("Should have 3 stations", 3, result.getRecords().size());

            // 4. Verify stations are loaded correctly
            List<Station> stations = result.getRecords();

            // Verify first station: time zone parsing
            Station station1 = stations.get(0);
            assertEquals("Time zone should be normalized", "Europe/Paris", station1.getTimeZone());
            assertEquals("Country should be FR", "FR", station1.getCountry());
            assertEquals("Station name should match", "Chateau-Arnoux-St-Auban", station1.getName());
            assertEquals("Latitude should match", 44.08179, station1.getLatitude(), 0.0001);
            assertEquals("Longitude should match", 6.001625, station1.getLongitude(), 0.0001);
            assertTrue("isCity should be true", station1.isCity());
            assertFalse("isMainStation should be false", station1.isMainStation());
            assertFalse("isAirport should be false", station1.isAirport());

            // Verify second station: different time zone
            Station station2 = stations.get(1);
            assertEquals("Time zone should be normalized", "Europe/Lisbon", station2.getTimeZone());
            assertEquals("Time zone group should match", "WET/GMT", station2.getTimeZoneGroup());

            // Verify third station
            Station station3 = stations.get(2);
            assertEquals("Time zone should be normalized", "Europe/Madrid", station3.getTimeZone());

        } finally {
            // 5. Clean up temp file
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 2: Validate required fields
     * Expected: Validation errors for empty country, time zone group, or station name
     */
    @Test
    public void testValidation_RequiredFields() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with missing required fields
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    ",CET,TestStation,40.0,-7.0,True,False,False",  // Empty country
                    "FR,,CET,TestStation2,40.0,-7.0,True,False,False",  // Empty time zone group
                    "FR,CET,,TestStation3,40.0,-7.0,True,False,False",  // Empty station name
                    "FR,CET,CET,ValidStation,40.0,-7.0,True,False,False"  // Valid row
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify validation errors are reported
            assertTrue("Should have validation errors", result.hasErrors());
            assertTrue("Should have at least 3 errors", result.getErrors().size() >= 3);

            // Verify specific error messages
            List<String> errors = result.getErrors();
            boolean hasCountryError = errors.stream().anyMatch(e -> e.contains("country cannot be empty"));
            boolean hasTimeZoneGroupError = errors.stream().anyMatch(e -> e.contains("time zone group cannot be empty"));
            boolean hasStationNameError = errors.stream().anyMatch(e -> e.contains("station name cannot be empty"));

            assertTrue("Should have error for empty country", hasCountryError);
            assertTrue("Should have error for empty time zone group", hasTimeZoneGroupError);
            assertTrue("Should have error for empty station name", hasStationNameError);

            // 4. Verify invalid rows are not included in results
            assertEquals("Should only have 1 valid station", 1, result.getRecords().size());
            assertEquals("Valid station should be ValidStation", "ValidStation", result.getRecords().get(0).getName());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 3: Validate latitude range
     * Expected: Validation errors for latitude outside [-90, 90]
     */
    @Test
    public void testValidation_LatitudeRange() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with invalid latitude values
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,CET,CET,InvalidLatHigh,91.0,6.0,True,False,False",  // Latitude > 90
                    "FR,CET,CET,InvalidLatLow,-91.0,6.0,True,False,False",  // Latitude < -90
                    "FR,CET,CET,ValidStation,44.0,6.0,True,False,False",  // Valid latitude
                    "FR,CET,CET,BoundaryHigh,90.0,6.0,True,False,False",  // Boundary: 90
                    "FR,CET,CET,BoundaryLow,-90.0,6.0,True,False,False"  // Boundary: -90
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify validation errors are reported
            assertTrue("Should have validation errors", result.hasErrors());
            assertTrue("Should have at least 2 errors", result.getErrors().size() >= 2);

            List<String> errors = result.getErrors();
            boolean hasHighLatError = errors.stream().anyMatch(e -> e.contains("latitude must be in [-90, 90]") && e.contains("91"));
            boolean hasLowLatError = errors.stream().anyMatch(e -> e.contains("latitude must be in [-90, 90]") && e.contains("-91"));

            assertTrue("Should have error for latitude > 90", hasHighLatError);
            assertTrue("Should have error for latitude < -90", hasLowLatError);

            // 4. Verify invalid rows are not included in results
            assertEquals("Should have 3 valid stations (including boundaries)", 3, result.getRecords().size());
            List<Station> stations = result.getRecords();
            assertTrue("Should include valid station", stations.stream().anyMatch(s -> s.getName().equals("ValidStation")));
            assertTrue("Should include boundary high", stations.stream().anyMatch(s -> s.getName().equals("BoundaryHigh")));
            assertTrue("Should include boundary low", stations.stream().anyMatch(s -> s.getName().equals("BoundaryLow")));

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 4: Validate longitude range
     * Expected: Validation errors for longitude outside [-180, 180]
     */
    @Test
    public void testValidation_LongitudeRange() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with invalid longitude values
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,CET,CET,InvalidLonHigh,44.0,181.0,True,False,False",  // Longitude > 180
                    "FR,CET,CET,InvalidLonLow,44.0,-181.0,True,False,False",  // Longitude < -180
                    "FR,CET,CET,ValidStation,44.0,6.0,True,False,False",  // Valid longitude
                    "FR,CET,CET,BoundaryHigh,44.0,180.0,True,False,False",  // Boundary: 180
                    "FR,CET,CET,BoundaryLow,44.0,-180.0,True,False,False"  // Boundary: -180
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify validation errors are reported
            assertTrue("Should have validation errors", result.hasErrors());
            assertTrue("Should have at least 2 errors", result.getErrors().size() >= 2);

            List<String> errors = result.getErrors();
            boolean hasHighLonError = errors.stream().anyMatch(e -> e.contains("longitude must be in [-180, 180]") && e.contains("181"));
            boolean hasLowLonError = errors.stream().anyMatch(e -> e.contains("longitude must be in [-180, 180]") && e.contains("-181"));

            assertTrue("Should have error for longitude > 180", hasHighLonError);
            assertTrue("Should have error for longitude < -180", hasLowLonError);

            // Verify invalid rows are not included
            assertEquals("Should have 3 valid stations (including boundaries)", 3, result.getRecords().size());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 5: Parse boolean values
     * Expected: Correct parsing of True/False/true/false/1/0/yes
     */
    @Test
    public void testParseBooleanValues() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with various boolean representations
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,CET,CET,Station1,44.0,6.0,True,False,False",  // True, False
                    "FR,CET,CET,Station2,44.0,6.0,true,false,false",  // true, false
                    "FR,CET,CET,Station3,44.0,6.0,1,0,0",  // 1, 0
                    "FR,CET,CET,Station4,44.0,6.0,yes,no,no",  // yes, no
                    "FR,CET,CET,Station5,44.0,6.0,False,True,True"  // False, True, True
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify boolean fields are parsed correctly
            assertFalse("Should have no validation errors", result.hasErrors());
            assertEquals("Should have 5 stations", 5, result.getRecords().size());

            List<Station> stations = result.getRecords();

            // Station1: True, False, False
            Station s1 = stations.get(0);
            assertTrue("isCity should be true (True)", s1.isCity());
            assertFalse("isMainStation should be false (False)", s1.isMainStation());
            assertFalse("isAirport should be false (False)", s1.isAirport());

            // Station2: true, false, false
            Station s2 = stations.get(1);
            assertTrue("isCity should be true (true)", s2.isCity());
            assertFalse("isMainStation should be false (false)", s2.isMainStation());
            assertFalse("isAirport should be false (false)", s2.isAirport());

            // Station3: 1, 0, 0
            Station s3 = stations.get(2);
            assertTrue("isCity should be true (1)", s3.isCity());
            assertFalse("isMainStation should be false (0)", s3.isMainStation());
            assertFalse("isAirport should be false (0)", s3.isAirport());

            // Station4: yes, no, no
            Station s4 = stations.get(3);
            assertTrue("isCity should be true (yes)", s4.isCity());
            assertFalse("isMainStation should be false (no)", s4.isMainStation());
            assertFalse("isAirport should be false (no)", s4.isAirport());

            // Station5: False, True, True
            Station s5 = stations.get(4);
            assertFalse("isCity should be false (False)", s5.isCity());
            assertTrue("isMainStation should be true (True)", s5.isMainStation());
            assertTrue("isAirport should be true (True)", s5.isAirport());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 6: Parse time zone format
     * Expected: Time zone "('Europe/Paris',)" is parsed to "Europe/Paris"
     */
    @Test
    public void testParseTimeZoneFormat() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with time zone in format: "('Europe/Paris',)"
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False",
                    "PT,\"('Europe/Lisbon',)\",WET/GMT,Ginjal,40.36623,-7.35269,True,False,False",
                    "ES,\"('Europe/Madrid',)\",CET,Torre del Mar,36.742,-4.09291,True,False,False",
                    "BE,\"('Europe/Brussels',)\",CET,Nieuwkerken,51.185342,4.185314,False,False,False"
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify time zone is normalized to "Europe/Paris"
            assertFalse("Should have no validation errors", result.hasErrors());
            assertEquals("Should have 4 stations", 4, result.getRecords().size());

            List<Station> stations = result.getRecords();
            assertEquals("Time zone should be normalized", "Europe/Paris", stations.get(0).getTimeZone());
            assertEquals("Time zone should be normalized", "Europe/Lisbon", stations.get(1).getTimeZone());
            assertEquals("Time zone should be normalized", "Europe/Madrid", stations.get(2).getTimeZone());
            assertEquals("Time zone should be normalized", "Europe/Brussels", stations.get(3).getTimeZone());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 7: Handle empty latitude/longitude
     * Expected: Validation error for empty coordinates (if required) or default values
     */
    @Test
    public void testEmptyCoordinates() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with empty latitude/longitude fields
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,CET,CET,Vievola,,,True,False,False",  // Empty coordinates
                    "FR,CET,CET,ValidStation,44.0,6.0,True,False,False"  // Valid coordinates
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify appropriate handling (validation error or default)
            // Empty coordinates should cause NumberFormatException which is caught and reported as error
            assertTrue("Should have validation errors for empty coordinates", result.hasErrors());
            assertTrue("Should have at least 1 error", result.getErrors().size() >= 1);

            List<String> errors = result.getErrors();
            boolean hasCoordinateError = errors.stream().anyMatch(e ->
                    e.contains("invalid numeric value") || e.contains("error parsing station"));
            assertTrue("Should have error for empty coordinates", hasCoordinateError);

            // Verify invalid row is not included
            assertEquals("Should only have 1 valid station", 1, result.getRecords().size());
            assertEquals("Valid station should be ValidStation", "ValidStation", result.getRecords().get(0).getName());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 8: Handle multiple stations with same name
     * Expected: All stations loaded, even if they share the same name
     */
    @Test
    public void testMultipleStationsSameName() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with multiple stations having same name but different coordinates
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.0615651,5.9973734,False,True,False",
                    "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.05,6.0,False,False,False"
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify all stations are loaded
            assertFalse("Should have no validation errors", result.hasErrors());
            assertEquals("Should have 3 stations with same name", 3, result.getRecords().size());

            List<Station> stations = result.getRecords();
            // All should have the same name
            assertTrue("All stations should have same name",
                    stations.stream().allMatch(s -> s.getName().equals("Chateau-Arnoux-St-Auban")));

            // But different coordinates
            Station s1 = stations.get(0);
            Station s2 = stations.get(1);
            Station s3 = stations.get(2);

            assertNotEquals("Stations should have different coordinates",
                    s1.getLatitude(), s2.getLatitude(), 0.0001);
            assertNotEquals("Stations should have different coordinates",
                    s2.getLatitude(), s3.getLatitude(), 0.0001);

            // Verify different properties
            assertTrue("First station should be city", s1.isCity());
            assertTrue("Second station should be main station", s2.isMainStation());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Test Case 9: Handle invalid CSV structure
     * Expected: Validation errors for rows with insufficient columns
     */
    @Test
    public void testInvalidCsvStructure() throws IOException {
        Path tempFile = null;
        try {
            // 1. Create CSV file with rows that have fewer than 9 columns
            String csvContent = String.join("\n",
                    "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport",
                    "FR,CET,CET,Station1",  // Only 4 columns
                    "FR,CET,CET,Station2,44.0,6.0",  // Only 6 columns
                    "FR,CET,CET,Station3,44.0,6.0,True,False",  // Only 8 columns
                    "FR,CET,CET,ValidStation,44.0,6.0,True,False,False"  // Valid: 9 columns
            );
            tempFile = createTestCsvFile(csvContent);

            // 2. Load stations using StationsCsvLoader.load()
            CsvValidatorResult<Station> result = StationsCsvLoader.load(tempFile.toString());

            // 3. Verify validation errors are reported
            assertTrue("Should have validation errors", result.hasErrors());
            assertTrue("Should have at least 3 errors", result.getErrors().size() >= 3);

            List<String> errors = result.getErrors();
            boolean hasColumnError = errors.stream().anyMatch(e ->
                    e.contains("expected at least 9 columns"));
            assertTrue("Should have error about insufficient columns", hasColumnError);

            // Verify only valid row is included
            assertEquals("Should only have 1 valid station", 1, result.getRecords().size());
            assertEquals("Valid station should be ValidStation", "ValidStation", result.getRecords().get(0).getName());

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}

