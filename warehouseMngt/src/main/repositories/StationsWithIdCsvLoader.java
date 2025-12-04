package main.repositories;

import main.controller.CsvReader;
import main.domain.Station;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads stations from CSV file with ID format.
 * 
 * Expected CSV format:
 * Station id,Station,Lat,Lon,CoordX,CoordY
 * 6,AALST,50.9453794204522,4.03099970664038,3185.96,131568.72
 * 
 * Note: This loader creates stations with default values for fields not in the CSV.
 * The country, timeZone, and timeZoneGroup are set to default values.
 */
public class StationsWithIdCsvLoader {

    /**
     * Loads stations from CSV file with ID format.
     * 
     * @param csvPath path to the CSV file
     * @return map from station ID (as String) to Station object
     * @throws IOException if there is an error reading the CSV file
     */
    public static Map<String, Station> loadWithIdMap(String csvPath) throws IOException {
        Map<String, Station> stationMap = new HashMap<>();
        
        CsvReader.readCsv(csvPath, (lineNo, fields) -> {
            if (fields.length < 6) {
                System.err.println("Warning: Line " + lineNo + " has less than 6 columns, skipping");
                return;
            }
            
            try {
                String idStr = fields[0].trim();
                String name = fields[1].trim();
                double latitude = Double.parseDouble(fields[2].trim());
                double longitude = Double.parseDouble(fields[3].trim());
                // CoordX and CoordY are ignored as they're not in Station class
                
                // Validate
                if (name.isEmpty()) {
                    System.err.println("Warning: Line " + lineNo + ": station name is empty, skipping");
                    return;
                }
                
                if (latitude < -90.0 || latitude > 90.0) {
                    System.err.println("Warning: Line " + lineNo + ": invalid latitude " + latitude + ", skipping");
                    return;
                }
                
                if (longitude < -180.0 || longitude > 180.0) {
                    System.err.println("Warning: Line " + lineNo + ": invalid longitude " + longitude + ", skipping");
                    return;
                }
                
                // Create station with default values for missing fields
                // Using "BE" as default country (Belgium), empty timezone, "CET" as default timezone group
                Station station = new Station(
                    name,
                    latitude,
                    longitude,
                    "BE",  // Default country
                    "",    // Empty timezone
                    "CET", // Default timezone group
                    false, // isCity
                    false, // isMainStation
                    false  // isAirport
                );
                
                stationMap.put(idStr, station);
                
            } catch (NumberFormatException e) {
                System.err.println("Warning: Line " + lineNo + ": invalid number format: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Warning: Line " + lineNo + ": error parsing station: " + e.getMessage());
            }
        });
        
        return stationMap;
    }
}

