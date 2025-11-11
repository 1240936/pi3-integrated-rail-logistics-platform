package main.repositories;

import main.controller.CsvReader;
import main.domain.Station;

import java.io.IOException;

/**
 * Utility class to load railway stations from a CSV file into {@link Station} objects.
 *
 * <p>Validates the CSV structure and station data according to USEI07 requirements:
 * <ul>
 *   <li>Non-empty name, country, and time zone group</li>
 *   <li>Latitude ∈ [-90, 90]</li>
 *   <li>Longitude ∈ [-180, 180]</li>
 * </ul>
 * Invalid rows are rejected and errors are recorded.
 * </p>
 *
 * <p>Expected CSV columns (in order):
 * <ul>
 *   <li>Country (String, required, non-empty)</li>
 *   <li>Time zone (String, optional)</li>
 *   <li>Time zone group (String, required, non-empty)</li>
 *   <li>Station name (String, required, non-empty)</li>
 *   <li>Latitude (double, required, [-90, 90])</li>
 *   <li>Longitude (double, required, [-180, 180])</li>
 *   <li>isCity (boolean)</li>
 *   <li>isMainStation (boolean)</li>
 *   <li>isAirport (boolean)</li>
 * </ul>
 * </p>
 */
public class StationsCsvLoader {

    /**
     * Loads stations from a CSV file.
     * Validates each line according to USEI07 acceptance criteria.
     *
     * @param csvPath path to the CSV file
     * @return result with valid stations and errors found
     * @throws IOException if there is an error reading the file
     */
    public static CsvValidatorResult<Station> load(String csvPath) throws IOException {
        CsvValidatorResult<Station> result = new CsvValidatorResult<>();

        // Read CSV line by line
        CsvReader.readCsv(csvPath, (lineNo, fields) -> {
            // Check if it has at least 9 columns
            if (fields.length < 9) {
                result.addError("stations.csv line " + lineNo + ": expected at least 9 columns, got " + fields.length);
                return;
            }

            try {
                // Extract fields from CSV
                String country = fields[0].trim();
                String timeZone = normalizeTimeZone(fields[1]);
                String timeZoneGroup = fields[2].trim();
                String name = fields[3].trim();
                double latitude = Double.parseDouble(fields[4].trim());
                double longitude = Double.parseDouble(fields[5].trim());
                boolean isCity = parseBoolean(fields[6].trim());
                boolean isMainStation = parseBoolean(fields[7].trim());
                boolean isAirport = parseBoolean(fields[8].trim());

                // Validation according to USEI07 acceptance criteria
                if (country.isEmpty()) {
                    result.addError("stations.csv line " + lineNo + ": country cannot be empty");
                    return;
                }

                if (timeZoneGroup.isEmpty()) {
                    result.addError("stations.csv line " + lineNo + ": time zone group cannot be empty");
                    return;
                }

                if (name.isEmpty()) {
                    result.addError("stations.csv line " + lineNo + ": station name cannot be empty");
                    return;
                }

                // Validate latitude: must be between -90 and 90
                if (latitude < -90.0 || latitude > 90.0) {
                    result.addError("stations.csv line " + lineNo + ": latitude must be in [-90, 90], got " + latitude);
                    return;
                }

                // Validate longitude: must be between -180 and 180
                if (longitude < -180.0 || longitude > 180.0) {
                    result.addError("stations.csv line " + lineNo + ": longitude must be in [-180, 180], got " + longitude);
                    return;
                }

                // Create and add the valid station
                Station station = new Station(name, latitude, longitude, country, timeZone, timeZoneGroup,
                        isCity, isMainStation, isAirport);
                result.addRecord(station);

            } catch (NumberFormatException e) {
                result.addError("stations.csv line " + lineNo + ": invalid numeric value: " + e.getMessage());
            } catch (Exception e) {
                result.addError("stations.csv line " + lineNo + ": error parsing station: " + e.getMessage());
            }
        });

        return result;
    }

    /**
     * Parses a boolean value from a string.
     * Accepts "true", "1", "yes" (case-insensitive) as true, everything else as false.
     *
     * @param value string to parse
     * @return boolean value
     */
    private static boolean parseBoolean(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase().trim();
        return lower.equals("true") || lower.equals("1") || lower.equals("yes");
    }

    /**
     * Normalizes a time zone field that may contain tuple-like strings from the CSV.
     * Examples: "('Europe/Paris',)" -> "Europe/Paris"
     *
     * @param value the raw CSV value
     * @return a cleaned time zone identifier, or empty string if input is null
     */
    private static String normalizeTimeZone(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        // Remove surrounding parentheses if present.
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        // Remove trailing commas from tuple representations.
        if (cleaned.endsWith(",")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        // Remove surrounding single quotes.
        if (cleaned.startsWith("'") && cleaned.endsWith("'") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        return cleaned;
    }
}

