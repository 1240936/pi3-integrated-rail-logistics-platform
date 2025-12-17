package main.controller;

import main.domain.HubScoreResult;
import main.domain.Station;
import main.graph.Edge;
import main.graph.map.MapGraph;
import main.repositories.CsvValidatorResult;
import main.controller.CsvReader;
import main.repositories.StationsWithIdCsvLoader;

import java.io.IOException;
import java.util.*;

/**
 * Service for computing rail hub centrality analysis (USEI13).
 * Computes centrality measures (betweenness, harmonic closeness, strength/degree)
 * and combines them into a composite HubScore.
 */
public class HubCentralityService {

    /**
     * Computes hub centrality measures for all stations in the network.
     * 
     * @param stationsCsvPath path to CSV with stations (format: Station id,Station,Lat,Lon,CoordX,CoordY)
     * @param connectionsCsvPath path to CSV with station connections (format: departure_stid,arrival_stid,dist,capacity,cost)
     * @return list of hub score results, sorted by hubScore descending
     * @throws IOException if there is an error reading the CSV files
     */
    public List<HubScoreResult> computeHubCentrality(String stationsCsvPath, String connectionsCsvPath) 
            throws IOException {
        
        // 1. Load stations with IDs
        Map<String, Station> stationMap = StationsWithIdCsvLoader.loadWithIdMap(stationsCsvPath);
        
        if (stationMap.isEmpty()) {
            throw new IllegalStateException("No stations loaded from CSV file: " + stationsCsvPath);
        }
        
        // 2. Load edges with distance as weight
        CsvValidatorResult<Edge<Station, Double>> edgesResult = loadEdgesWithDistance(connectionsCsvPath, stationMap);
        
        if (edgesResult.hasErrors()) {
            System.err.println("Warnings loading edges:");
            for (String error : edgesResult.getErrors()) {
                System.err.println("  " + error);
            }
        }
        
        if (edgesResult.getRecords().isEmpty()) {
            throw new IllegalStateException("No valid edges found in CSV file.");
        }
        
        // 3. Build undirected graph
        MapGraph<Station, Double> graph = buildUndirectedGraph(stationMap, edgesResult.getRecords());
        
        // 4. Create reverse mapping: Station -> ID
        Map<Station, String> stationToIdMap = new HashMap<>();
        for (Map.Entry<String, Station> entry : stationMap.entrySet()) {
            stationToIdMap.put(entry.getValue(), entry.getKey());
        }
        
        // 5. Get vertices list
        List<Station> vertices = graph.vertices();
        int n = vertices.size();
        
        // 6. Initialize distance and paths matrices
        double[][] dist = new double[n][n];
        int[][] paths = new int[n][n];
        
        // Initialize matrices
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    dist[i][j] = 0.0;
                    paths[i][j] = 1;
                } else {
                    dist[i][j] = Double.MAX_VALUE;
                    paths[i][j] = 0;
                }
            }
        }
        
        // Fill direct edges
        for (Edge<Station, Double> edge : graph.edges()) {
            int u = graph.key(edge.getVOrig());
            int v = graph.key(edge.getVDest());
            double weight = edge.getWeight();
            dist[u][v] = weight;
            dist[v][u] = weight; // undirected graph
            paths[u][v] = 1;
            paths[v][u] = 1;
        }
        
        // 7. Floyd-Warshall to compute shortest paths and count paths
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] != Double.MAX_VALUE && dist[k][j] != Double.MAX_VALUE) {
                        double newDist = dist[i][k] + dist[k][j];
                        if (dist[i][j] > newDist) {
                            dist[i][j] = newDist;
                            paths[i][j] = paths[i][k] * paths[k][j];
                        } else if (dist[i][j] == newDist && i != k && j != k) {
                            paths[i][j] += paths[i][k] * paths[k][j];
                        }
                    }
                }
            }
        }
        
        // 8. Compute betweenness centrality
        double[] betweenness = new double[n];
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (i == k || j == k) continue;
                    
                    if (dist[i][j] == Double.MAX_VALUE || paths[i][j] == 0) continue;
                    
                    if (dist[i][k] != Double.MAX_VALUE && dist[k][j] != Double.MAX_VALUE) {
                        double pathThroughK = dist[i][k] + dist[k][j];
                        if (Math.abs(dist[i][j] - pathThroughK) < 1e-9) {
                            if (paths[i][k] > 0 && paths[k][j] > 0 && paths[i][j] > 0) {
                                double contribution = (double) (paths[i][k] * paths[k][j]) / paths[i][j];
                                betweenness[k] += contribution;
                            }
                        }
                    }
                }
            }
        }
        
        // 9. Compute degree and strength for each vertex
        int[] degree = new int[n];
        double[] strength = new double[n];
        
        for (Station station : vertices) {
            int idx = graph.key(station);
            Collection<Edge<Station, Double>> outgoingEdges = graph.outgoingEdges(station);
            if (outgoingEdges != null) {
                degree[idx] = outgoingEdges.size();
                for (Edge<Station, Double> edge : outgoingEdges) {
                    strength[idx] += edge.getWeight();
                }
            }
        }
        
        // 10. Compute harmonic closeness centrality
        double[] harmonicCloseness = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j < n; j++) {
                if (i != j && dist[i][j] != Double.MAX_VALUE && dist[i][j] > 0) {
                    sum += 1.0 / dist[i][j];
                }
            }
            harmonicCloseness[i] = sum;
        }
        
        // 11. Normalize all measures to [0,1]
        double maxBetweenness = Arrays.stream(betweenness).max().orElse(1.0);
        double maxHarmonicCloseness = Arrays.stream(harmonicCloseness).max().orElse(1.0);
        double maxStrength = Arrays.stream(strength).max().orElse(1.0);
        double minStrength = Arrays.stream(strength).min().orElse(0.0);
        
        double[] betweennessNorm = new double[n];
        double[] harmonicClosenessNorm = new double[n];
        double[] strengthNorm = new double[n];
        
        for (int i = 0; i < n; i++) {
            betweennessNorm[i] = maxBetweenness > 0 ? betweenness[i] / maxBetweenness : 0.0;
            harmonicClosenessNorm[i] = maxHarmonicCloseness > 0 ? harmonicCloseness[i] / maxHarmonicCloseness : 0.0;
            strengthNorm[i] = (maxStrength - minStrength) > 0 ? 
                (strength[i] - minStrength) / (maxStrength - minStrength) : 0.0;
        }
        
        // 12. Compute HubScore: 0.35 * betw + 0.35 * harmonic_closeness + 0.30 * strengthNorm
        List<HubScoreResult> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Station station = vertices.get(i);
            String stationId = stationToIdMap.get(station);
            double hubScore = 0.35 * betweennessNorm[i] + 
                            0.35 * harmonicClosenessNorm[i] + 
                            0.30 * strengthNorm[i];
            
            results.add(new HubScoreResult(
                stationId,
                station.getName(),
                degree[i],
                strength[i],
                betweennessNorm[i],
                harmonicClosenessNorm[i],
                hubScore
            ));
        }
        
        // 13. Sort by hubScore descending
        results.sort((a, b) -> Double.compare(b.getHubScore(), a.getHubScore()));
        
        return results;
    }
    
    /**
     * Loads edges with distance as weight from CSV file.
     */
    private CsvValidatorResult<Edge<Station, Double>> loadEdgesWithDistance(
            String csvPath,
            Map<String, Station> stationMap) throws IOException {
        
        CsvValidatorResult<Edge<Station, Double>> result = new CsvValidatorResult<>();
        
        CsvReader.readCsv(csvPath, (lineNo, fields) -> {
            if (fields.length < 5) {
                result.addError("Line " + lineNo + ": expected at least 5 columns, got " + fields.length);
                return;
            }
            
            try {
                String fromKey = fields[0].trim();
                String toKey = fields[1].trim();
                double dist = Double.parseDouble(fields[2].trim());
                
                if (dist < 0) {
                    result.addError("Line " + lineNo + ": distance cannot be negative, got " + dist);
                    return;
                }
                
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
                
                Edge<Station, Double> edge = new Edge<>(fromStation, toStation, dist);
                result.addRecord(edge);
            } catch (NumberFormatException e) {
                result.addError("Line " + lineNo + ": invalid numeric value: " + e.getMessage());
            } catch (Exception e) {
                result.addError("Line " + lineNo + ": error parsing edge: " + e.getMessage());
            }
        });
        
        return result;
    }
    
    /**
     * Builds an undirected graph from the edges.
     * For undirected graphs, we need to ensure that if (A,B) exists, we don't duplicate it.
     */
    private MapGraph<Station, Double> buildUndirectedGraph(
            Map<String, Station> stationMap,
            List<Edge<Station, Double>> edges) {
        
        MapGraph<Station, Double> graph = new MapGraph<>(false); // false = undirected
        
        // Add all vertices
        for (Station station : stationMap.values()) {
            graph.addVertex(station);
        }
        
        // Use a map to track edges and keep only the minimum weight
        Map<String, Double> edgeMap = new HashMap<>();
        
        // Process edges
        for (Edge<Station, Double> edge : edges) {
            Station from = edge.getVOrig();
            Station to = edge.getVDest();
            Double weight = edge.getWeight();
            
            String key1 = from.getName() + "|" + to.getName();
            String key2 = to.getName() + "|" + from.getName();
            
            if (edgeMap.containsKey(key2)) {
                Double existingWeight = edgeMap.get(key2);
                if (weight < existingWeight) {
                    edgeMap.remove(key2);
                    edgeMap.put(key1, weight);
                }
            } else if (!edgeMap.containsKey(key1)) {
                edgeMap.put(key1, weight);
            } else {
                Double existingWeight = edgeMap.get(key1);
                if (weight < existingWeight) {
                    edgeMap.put(key1, weight);
                }
            }
        }
        
        // Add edges to undirected graph
        for (Map.Entry<String, Double> entry : edgeMap.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            if (parts.length == 2) {
                Station from = findStationByName(stationMap, parts[0]);
                Station to = findStationByName(stationMap, parts[1]);
                if (from != null && to != null) {
                    graph.addEdge(from, to, entry.getValue());
                }
            }
        }
        
        return graph;
    }
    
    /**
     * Finds a station by name in the station map.
     */
    private Station findStationByName(Map<String, Station> stationMap, String name) {
        for (Station station : stationMap.values()) {
            if (station.getName().equals(name)) {
                return station;
            }
        }
        return null;
    }
}


