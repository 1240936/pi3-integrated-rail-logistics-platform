package main.repositories;

import main.controller.CsvReader;
import main.domain.Station;
import main.graph.Edge;

import java.io.IOException;
import java.util.Map;

/**
 * Loads station-to-station connections from CSV file.
 * 
 * Expected CSV format:
 * departure_stid,arrival_stid,dist,capacity,cost
 * 
 * Where:
 * - departure_stid: Station ID (as String) of the origin station
 * - arrival_stid: Station ID (as String) of the destination station
 * - dist: Distance in kilometers (double, >= 0)
 * - capacity: Capacity (int, >= 0)
 * - cost: Cost (double, can be negative)
 * 
 * The loader uses a stationMap to look up Station objects by ID (as String).
 */
public class StationToStationCsvLoader {

    /**
     * Loads station-to-station connections from CSV.
     * 
     * @param csvPath path to CSV file with station connections (format: departure_stid,arrival_stid,dist,capacity,cost)
     * @param stationMap map from station ID (as String) to Station object
     * @return validation result with edges and errors
     * @throws IOException if there is an error reading the CSV file
     */
    public static CsvValidatorResult<Edge<Station, Double>> load(
            String csvPath, 
            Map<String, Station> stationMap) throws IOException {
        
        CsvValidatorResult<Edge<Station, Double>> result = new CsvValidatorResult<>();
        
        CsvReader.readCsv(csvPath, (lineNo, fields) -> {
            if (fields.length < 5) {
                result.addError("Line " + lineNo + ": expected at least 5 columns (from, to, dist, capacity, cost), got " + fields.length);
                return;
            }
            
            try {
                String fromKey = fields[0].trim();
                String toKey = fields[1].trim();
                double dist = Double.parseDouble(fields[2].trim());
                int capacity = Integer.parseInt(fields[3].trim());
                double cost = Double.parseDouble(fields[4].trim());
                
                // Validar valores numéricos
                if (dist < 0) {
                    result.addError("Line " + lineNo + ": distance cannot be negative, got " + dist);
                    return;
                }
                if (capacity < 0) {
                    result.addError("Line " + lineNo + ": capacity cannot be negative, got " + capacity);
                    return;
                }
                
                // Buscar estações no mapa
                Station fromStation = stationMap.get(fromKey);
                Station toStation = stationMap.get(toKey);
                
                if (fromStation == null) {
                    result.addError("Line " + lineNo + ": station not found: " + fromKey);
                    return;
                }
                if (toStation == null) {
                    result.addError("Line " + lineNo + ": station not found: " + toKey);
                    return;
                }
                
                // Criar aresta (usar cost como peso do grafo)
                Edge<Station, Double> edge = new Edge<>(fromStation, toStation, cost);
                result.addRecord(edge);
                
            } catch (NumberFormatException e) {
                result.addError("Line " + lineNo + ": invalid number format: " + e.getMessage());
            } catch (Exception e) {
                result.addError("Line " + lineNo + ": error parsing connection: " + e.getMessage());
            }
        });
        
        return result;
    }
}

