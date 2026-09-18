package main.controller;

import main.domain.MaxFlowResult;
import main.domain.Station;
import main.graph.Edge;
import main.graph.Graph;
import main.graph.map.MapGraph;
import main.repositories.CsvValidatorResult;
import main.repositories.StationsWithIdCsvLoader;

import java.io.IOException;
import java.util.*;

/**
 * Service for computing maximum flow between two stations (USEI14).
 * Implements Edmonds-Karp algorithm for maximum flow computation.
 */
public class MaxFlowService {

    /**
     * Computes the maximum flow between source and sink stations.
     *
     * @param stationsWithIdCsvPath path to CSV file with stations (format: Station id,Station,Lat,Lon,CoordX,CoordY)
     * @param stationToStationCsvPath path to CSV file with connections (format: departure_stid,arrival_stid,dist,capacity,cost)
     * @param sourceStationId source station ID
     * @param sinkStationId sink station ID
     * @return MaxFlowResult with source, sink, and maximum flow value
     * @throws IOException if there is an error reading files
     * @throws IllegalArgumentException if stations are invalid or graph is not directed
     */
    public MaxFlowResult computeMaxFlow(
            String stationsWithIdCsvPath,
            String stationToStationCsvPath,
            String sourceStationId,
            String sinkStationId) throws IOException {

        // 1. Load stations
        Map<String, Station> stationMap = StationsWithIdCsvLoader.loadWithIdMap(stationsWithIdCsvPath);
        if (stationMap.isEmpty()) {
            throw new IllegalArgumentException("No stations loaded from: " + stationsWithIdCsvPath);
        }

        // 2. Get source and sink stations
        Station source = stationMap.get(sourceStationId);
        Station sink = stationMap.get(sinkStationId);

        if (source == null) {
            throw new IllegalArgumentException("Source station not found: " + sourceStationId);
        }
        if (sink == null) {
            throw new IllegalArgumentException("Sink station not found: " + sinkStationId);
        }
        if (source.equals(sink)) {
            throw new IllegalArgumentException("Source and sink must be different stations.");
        }

        // 3. Load edges with capacities
        CsvValidatorResult<EdgeWithCapacity> edgesResult =
                loadEdgesWithCapacity(stationToStationCsvPath, stationMap);

        if (edgesResult.hasErrors()) {
            System.err.println("Warnings loading edges:");
            for (String error : edgesResult.getErrors()) {
                System.err.println("  " + error);
            }
        }

        if (edgesResult.getRecords().isEmpty()) {
            throw new IllegalArgumentException("No valid edges found in CSV file. Cannot compute max flow.");
        }

        // 4. Build directed graph with capacities
        MapGraph<Station, Double> graph = new MapGraph<>(true); // true = directed

        // Add all stations
        for (Station station : stationMap.values()) {
            graph.addVertex(station);
        }

        // Add edges with capacity as weight
        Map<String, Double> capacityMap = new HashMap<>(); // Store capacity for each edge
        for (EdgeWithCapacity edgeWithCap : edgesResult.getRecords()) {
            Station from = edgeWithCap.getFrom();
            Station to = edgeWithCap.getTo();
            double capacity = edgeWithCap.getCapacity();

            graph.addEdge(from, to, capacity);

            // Store capacity in map for reference (key: "fromName|toName")
            String key = from.getName() + "|" + to.getName();
            capacityMap.put(key, capacity);
        }

        // 5. Validate graph is directed
        if (!graph.isDirected()) {
            throw new IllegalStateException("Graph must be directed for max flow computation.");
        }

        // 6. Compute maximum flow using Edmonds-Karp
        double maxFlow = edmondsKarp(graph, source, sink);

        return new MaxFlowResult(source, sink, maxFlow);
    }

    /**
     * Computes maximum flow using Edmonds-Karp algorithm.
     *
     * @param graph directed graph with capacities as edge weights
     * @param source source vertex
     * @param sink sink vertex
     * @return maximum flow value
     */
    private double edmondsKarp(Graph<Station, Double> graph, Station source, Station sink) {
        int n = graph.numVertices();

        // Residual graph: residual[u][v] = remaining capacity from u to v
        double[][] residual = new double[n][n];

        // Initialize residual graph from graph edges
        for (Edge<Station, Double> edge : graph.edges()) {
            int u = graph.key(edge.getVOrig());
            int v = graph.key(edge.getVDest());
            residual[u][v] = edge.getWeight(); // Capacity is stored as weight
        }

        int[] parent = new int[n]; // Parent array for BFS tree
        double maxFlow = 0.0;

        // Augment path while there is a path from source to sink
        while (bfs(graph, residual, source, sink, parent)) {
            // Find bottleneck capacity (minimum residual capacity along path)
            double bottleneck = Double.MAX_VALUE;
            int v = graph.key(sink);

            // Traverse path from sink to source to find bottleneck
            while (v != graph.key(source)) {
                int u = parent[v];
                bottleneck = Math.min(bottleneck, residual[u][v]);
                v = u;
            }

            // Add bottleneck to max flow
            maxFlow += bottleneck;

            // Update residual capacities
            v = graph.key(sink);
            while (v != graph.key(source)) {
                int u = parent[v];
                residual[u][v] -= bottleneck;  // Forward edge loses capacity
                residual[v][u] += bottleneck;  // Backward edge gains capacity
                v = u;
            }
        }

        return maxFlow;
    }

    /**
     * BFS on residual graph to find augmenting path from source to sink.
     *
     * @param graph the graph
     * @param residual residual capacity matrix
     * @param source source vertex
     * @param sink sink vertex
     * @param parent parent array to store BFS tree
     * @return true if path exists, false otherwise
     */
    private boolean bfs(Graph<Station, Double> graph, double[][] residual,
                        Station source, Station sink, int[] parent) {
        int n = graph.numVertices();
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();

        int s = graph.key(source);
        int t = graph.key(sink);

        queue.add(s);
        visited[s] = true;
        parent[s] = -1;

        while (!queue.isEmpty()) {
            int u = queue.poll();

            // Check all adjacent vertices
            Station uVertex = graph.vertex(u);
            Collection<Station> adjVertices = graph.adjVertices(uVertex);

            for (Station adj : adjVertices) {
                int v = graph.key(adj);

                // If not visited and has residual capacity
                if (!visited[v] && residual[u][v] > 0) {
                    queue.add(v);
                    parent[v] = u;
                    visited[v] = true;

                    // If sink reached, path found
                    if (v == t) {
                        return true;
                    }
                }
            }
        }

        return false; // No path found
    }

    /**
     * Loads edges from CSV with capacity information.
     *
     * @param csvPath path to CSV file
     * @param stationMap map from station ID to Station object
     * @return validation result with edges and capacities
     * @throws IOException if there is an error reading the CSV file
     */
    private CsvValidatorResult<EdgeWithCapacity> loadEdgesWithCapacity(
            String csvPath,
            Map<String, Station> stationMap) throws IOException {

        CsvValidatorResult<EdgeWithCapacity> result = new CsvValidatorResult<>();

        main.controller.CsvReader.readCsv(csvPath, (lineNo, fields) -> {
            if (fields.length < 5) {
                result.addError("Line " + lineNo + ": expected at least 5 columns (from, to, dist, capacity, cost), got " + fields.length);
                return;
            }

            try {
                String fromKey = fields[0].trim();
                String toKey = fields[1].trim();
                double dist = Double.parseDouble(fields[2].trim());
                int capacity = Integer.parseInt(fields[3].trim());
                @SuppressWarnings("unused")
                double cost = Double.parseDouble(fields[4].trim()); // Not used for max flow, but validated

                // Validate numeric values
                if (dist < 0) {
                    result.addError("Line " + lineNo + ": distance cannot be negative, got " + dist);
                    return;
                }
                if (capacity < 0) {
                    result.addError("Line " + lineNo + ": capacity cannot be negative, got " + capacity);
                    return;
                }

                // Look up stations
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

                // Create edge with capacity
                EdgeWithCapacity edgeWithCap = new EdgeWithCapacity(fromStation, toStation, capacity);
                result.addRecord(edgeWithCap);

            } catch (NumberFormatException e) {
                result.addError("Line " + lineNo + ": invalid number format: " + e.getMessage());
            } catch (Exception e) {
                result.addError("Line " + lineNo + ": error parsing connection: " + e.getMessage());
            }
        });

        return result;
    }

    /**
     * Helper class to store edge with capacity information during CSV loading.
     */
    private static class EdgeWithCapacity {
        private final Station from;
        private final Station to;
        private final double capacity;

        public EdgeWithCapacity(Station from, Station to, double capacity) {
            this.from = from;
            this.to = to;
            this.capacity = capacity;
        }

        public Station getFrom() {
            return from;
        }

        public Station getTo() {
            return to;
        }

        public double getCapacity() {
            return capacity;
        }
    }
}
