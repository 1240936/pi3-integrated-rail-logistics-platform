package main.controller;

import main.domain.ShortestPathResult;
import main.domain.Station;
import main.graph.Edge;
import main.graph.Graph;
import main.graph.map.MapGraph;
import main.repositories.CsvValidatorResult;
import main.repositories.StationToStationCsvLoader;
import main.repositories.StationsWithIdCsvLoader;

import java.io.IOException;
import java.util.*;

/**
 * Service for computing risk-aware shortest paths between stations.
 * USEI15: Implements Bellman-Ford algorithm to handle negative edge costs
 * and detect negative cycles.
 * 
 * This service computes shortest paths using edge costs that combine distance
 * and penalties/bonuses. Negative costs are allowed, which requires algorithms
 * beyond Dijkstra (hence Bellman-Ford).
 */
public class RiskAwarePathService {
    
    /**
     * Computes the shortest path between two stations using risk-aware costs.
     * Uses Bellman-Ford algorithm to handle negative edge costs and detect negative cycles.
     *
     * @param stationsCsvPath path to CSV file with stations (format: Station id,Station,Lat,Lon,CoordX,CoordY)
     * @param connectionsCsvPath path to CSV file with connections (format: departure_stid,arrival_stid,dist,capacity,cost)
     * @param sourceStationId station ID of the source station
     * @param targetStationId station ID of the target station
     * @return ShortestPathResult with path, costs, and complexity metrics
     * @throws IOException if there is an error reading CSV files
     * @throws IllegalArgumentException if stations are not found
     */
    public ShortestPathResult computeShortestPath(
            String stationsCsvPath,
            String connectionsCsvPath,
            String sourceStationId,
            String targetStationId) throws IOException {
        
        long startTime = System.currentTimeMillis();
        
        // 1. Load stations
        Map<String, Station> stationMap = StationsWithIdCsvLoader.loadWithIdMap(stationsCsvPath);
        if (stationMap.isEmpty()) {
            throw new IllegalArgumentException("No stations loaded from: " + stationsCsvPath);
        }
        
        // 2. Validate source and target stations
        Station sourceStation = stationMap.get(sourceStationId);
        Station targetStation = stationMap.get(targetStationId);
        
        if (sourceStation == null) {
            throw new IllegalArgumentException("Source station not found: " + sourceStationId);
        }
        if (targetStation == null) {
            throw new IllegalArgumentException("Target station not found: " + targetStationId);
        }
        
        // 3. Load connections (edges)
        CsvValidatorResult<Edge<Station, Double>> edgesResult =
                StationToStationCsvLoader.load(connectionsCsvPath, stationMap);
        
        if (edgesResult.hasErrors()) {
            System.err.println("Warnings loading edges:");
            for (String error : edgesResult.getErrors()) {
                System.err.println("  " + error);
            }
        }
        
        if (edgesResult.getRecords().isEmpty()) {
            throw new IllegalArgumentException("No valid edges loaded from: " + connectionsCsvPath);
        }
        
        // 4. Build directed graph
        MapGraph<Station, Double> graph = new MapGraph<>(true); // true = directed
        
        // Add all stations
        for (Station station : stationMap.values()) {
            graph.addVertex(station);
        }
        
        // Add edges
        for (Edge<Station, Double> edge : edgesResult.getRecords()) {
            graph.addEdge(edge.getVOrig(), edge.getVDest(), edge.getWeight());
        }
        
        // 5. Run Bellman-Ford algorithm
        return bellmanFord(graph, sourceStation, targetStation, startTime);
    }
    
    /**
     * Implements Bellman-Ford algorithm to find shortest paths with negative edge costs.
     * Also detects negative cycles.
     *
     * @param graph the graph
     * @param source source vertex
     * @param target target vertex
     * @param startTime start time for complexity analysis
     * @return ShortestPathResult
     */
    private ShortestPathResult bellmanFord(Graph<Station, Double> graph,
                                          Station source,
                                          Station target,
                                          long startTime) {
        
        int verticesCount = graph.numVertices();
        int edgesCount = graph.numEdges();
        
        // Initialize distances: all vertices start at infinity except source
        Map<Station, Double> distances = new HashMap<>();
        Map<Station, Station> predecessors = new HashMap<>();
        
        for (Station vertex : graph.vertices()) {
            distances.put(vertex, Double.POSITIVE_INFINITY);
            predecessors.put(vertex, null);
        }
        distances.put(source, 0.0);
        
        // Relax edges (V-1) times
        for (int i = 0; i < verticesCount - 1; i++) {
            boolean changed = false;
            for (Edge<Station, Double> edge : graph.edges()) {
                Station u = edge.getVOrig();
                Station v = edge.getVDest();
                Double weight = edge.getWeight();
                
                if (distances.get(u) != Double.POSITIVE_INFINITY) {
                    double newDist = distances.get(u) + weight;
                    if (newDist < distances.get(v)) {
                        distances.put(v, newDist);
                        predecessors.put(v, u);
                        changed = true;
                    }
                }
            }
            // Early termination if no changes (optimization)
            if (!changed) {
                break;
            }
        }
        
        // Check for negative cycles
        // Run one more iteration to detect if any distance can still be improved
        Set<Station> affectedVertices = new HashSet<>();
        
        for (Edge<Station, Double> edge : graph.edges()) {
            Station u = edge.getVOrig();
            Station v = edge.getVDest();
            Double weight = edge.getWeight();
            
            if (distances.get(u) != Double.POSITIVE_INFINITY) {
                double newDist = distances.get(u) + weight;
                if (newDist < distances.get(v)) {
                    // Negative cycle detected!
                    // Mark affected vertices
                    affectedVertices.add(v);
                    // Update distance to negative infinity to mark as reachable from negative cycle
                    distances.put(v, Double.NEGATIVE_INFINITY);
                }
            }
        }
        
        // If negative cycle detected, propagate negative infinity and find cycle
        if (!affectedVertices.isEmpty()) {
            // Propagate negative infinity to all vertices reachable from negative cycle
            boolean changed = true;
            while (changed) {
                changed = false;
                for (Edge<Station, Double> edge : graph.edges()) {
                    Station u = edge.getVOrig();
                    Station v = edge.getVDest();
                    if (distances.get(u) == Double.NEGATIVE_INFINITY && 
                        distances.get(v) != Double.NEGATIVE_INFINITY) {
                        distances.put(v, Double.NEGATIVE_INFINITY);
                        affectedVertices.add(v);
                        changed = true;
                    }
                }
            }
            
            // Extract a negative cycle
            List<Station> cycleStations = findNegativeCycle(graph, predecessors, affectedVertices);
            List<Edge<Station, Double>> cycleEdges = extractCycleEdges(graph, cycleStations);
            
            long executionTime = System.currentTimeMillis() - startTime;
            ShortestPathResult.NegativeCycle negativeCycle =
                    new ShortestPathResult.NegativeCycle(cycleStations, cycleEdges);
            
            return new ShortestPathResult(negativeCycle, executionTime, verticesCount, edgesCount);
        }
        
        // No negative cycle - check if path exists
        long executionTime = System.currentTimeMillis() - startTime;
        
        if (distances.get(target) == Double.POSITIVE_INFINITY) {
            // No path exists
            return new ShortestPathResult(executionTime, verticesCount, edgesCount);
        }
        
        // Build path from source to target
        List<ShortestPathResult.PathStep> path = buildPath(source, target, predecessors, distances);
        double totalCost = distances.get(target);
        
        return new ShortestPathResult(path, totalCost, executionTime, verticesCount, edgesCount);
    }
    
    /**
     * Builds the path from source to target using predecessors map.
     *
     * @param source source station
     * @param target target station
     * @param predecessors map of predecessors
     * @param distances map of distances
     * @return list of path steps
     */
    private List<ShortestPathResult.PathStep> buildPath(Station source,
                                                        Station target,
                                                        Map<Station, Station> predecessors,
                                                        Map<Station, Double> distances) {
        List<ShortestPathResult.PathStep> path = new ArrayList<>();
        
        // Reconstruct path backwards
        List<Station> reversePath = new ArrayList<>();
        Station current = target;
        
        while (current != null) {
            reversePath.add(current);
            current = predecessors.get(current);
        }
        
        // Reverse to get forward path
        Collections.reverse(reversePath);
        
        // Build path steps with costs
        for (Station station : reversePath) {
            double cost = distances.get(station);
            path.add(new ShortestPathResult.PathStep(station, cost));
        }
        
        return path;
    }
    
    /**
     * Finds a negative cycle in the graph by following predecessors from affected vertices.
     *
     * @param graph the graph
     * @param predecessors map of predecessors
     * @param affectedVertices vertices reachable from negative cycle
     * @return list of stations in the cycle
     */
    private List<Station> findNegativeCycle(Graph<Station, Double> graph,
                                            Map<Station, Station> predecessors,
                                            Set<Station> affectedVertices) {
        if (affectedVertices.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Start from an affected vertex and follow predecessors
        Station start = affectedVertices.iterator().next();
        Map<Station, Integer> visited = new HashMap<>(); // vertex -> position in path
        List<Station> path = new ArrayList<>();
        Station current = start;
        int position = 0;
        
        // Follow predecessors until we find a cycle
        while (current != null) {
            if (visited.containsKey(current)) {
                // Found a cycle! Extract it
                int cycleStart = visited.get(current);
                List<Station> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                cycle.add(current); // Close the cycle
                return cycle;
            }
            
            visited.put(current, position);
            path.add(current);
            current = predecessors.get(current);
            position++;
            
            // Safety limit to avoid infinite loops
            if (position > graph.numVertices()) {
                break;
            }
        }
        
        // Fallback: return the affected vertices as a cycle indicator
        return new ArrayList<>(affectedVertices);
    }
    
    /**
     * Extracts edges that form the negative cycle.
     *
     * @param graph the graph
     * @param cycleStations stations in the cycle
     * @return list of edges in the cycle
     */
    private List<Edge<Station, Double>> extractCycleEdges(Graph<Station, Double> graph,
                                                          List<Station> cycleStations) {
        List<Edge<Station, Double>> cycleEdges = new ArrayList<>();
        
        for (int i = 0; i < cycleStations.size() - 1; i++) {
            Station from = cycleStations.get(i);
            Station to = cycleStations.get(i + 1);
            Edge<Station, Double> edge = graph.edge(from, to);
            if (edge != null) {
                cycleEdges.add(edge);
            }
        }
        
        return cycleEdges;
    }
}

