package main.domain;

import main.graph.Edge;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of risk-aware shortest path computation for USEI15.
 * Encapsulates the result of computing shortest paths with edge costs that may be negative,
 * including negative cycle detection.
 */
public class ShortestPathResult {
    
    /**
     * Represents a step in the path with a station and the cost to reach it.
     */
    public static class PathStep {
        private final Station station;
        private final double cost;
        
        public PathStep(Station station, double cost) {
            this.station = station;
            this.cost = cost;
        }
        
        public Station getStation() {
            return station;
        }
        
        public double getCost() {
            return cost;
        }
        
        @Override
        public String toString() {
            return String.format("%s (cost: %.2f)", station.getName(), cost);
        }
    }
    
    /**
     * Represents a negative cycle with stations and edges involved.
     */
    public static class NegativeCycle {
        private final List<Station> stations;
        private final List<Edge<Station, Double>> edges;
        
        public NegativeCycle(List<Station> stations, List<Edge<Station, Double>> edges) {
            this.stations = new ArrayList<>(stations);
            this.edges = new ArrayList<>(edges);
        }
        
        public List<Station> getStations() {
            return new ArrayList<>(stations);
        }
        
        public List<Edge<Station, Double>> getEdges() {
            return new ArrayList<>(edges);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Negative Cycle Detected:\n");
            sb.append("  Stations: ");
            for (int i = 0; i < stations.size(); i++) {
                if (i > 0) sb.append(" -> ");
                sb.append(stations.get(i).getName());
            }
            sb.append("\n  Edges: ");
            for (int i = 0; i < edges.size(); i++) {
                if (i > 0) sb.append(", ");
                Edge<Station, Double> edge = edges.get(i);
                sb.append(String.format("%s->%s (cost: %.2f)", 
                    edge.getVOrig().getName(), 
                    edge.getVDest().getName(), 
                    edge.getWeight()));
            }
            return sb.toString();
        }
    }
    
    private final List<PathStep> path;  // Path from source to target (null if no path exists)
    private final double totalCost;      // Total cost to target (Double.POSITIVE_INFINITY if no path)
    private final NegativeCycle negativeCycle;  // null if no negative cycle detected
    private final boolean hasNegativeCycle;
    private final boolean pathExists;
    private final long executionTimeMs;  // Temporal analysis complexity
    private final int verticesProcessed;
    private final int edgesProcessed;
    
    /**
     * Constructs a ShortestPathResult for a successful path (no negative cycle).
     *
     * @param path path from source to target with costs
     * @param totalCost total cost to target
     * @param executionTimeMs execution time in milliseconds
     * @param verticesProcessed number of vertices processed
     * @param edgesProcessed number of edges processed
     */
    public ShortestPathResult(List<PathStep> path, double totalCost,
                             long executionTimeMs, int verticesProcessed, int edgesProcessed) {
        this.path = path != null ? new ArrayList<>(path) : null;
        this.totalCost = totalCost;
        this.negativeCycle = null;
        this.hasNegativeCycle = false;
        this.pathExists = (path != null && !path.isEmpty());
        this.executionTimeMs = executionTimeMs;
        this.verticesProcessed = verticesProcessed;
        this.edgesProcessed = edgesProcessed;
    }
    
    /**
     * Constructs a ShortestPathResult when a negative cycle is detected.
     *
     * @param negativeCycle the negative cycle detected
     * @param executionTimeMs execution time in milliseconds
     * @param verticesProcessed number of vertices processed
     * @param edgesProcessed number of edges processed
     */
    public ShortestPathResult(NegativeCycle negativeCycle,
                             long executionTimeMs, int verticesProcessed, int edgesProcessed) {
        this.path = null;
        this.totalCost = Double.NEGATIVE_INFINITY;  // Indicates negative cycle
        this.negativeCycle = negativeCycle;
        this.hasNegativeCycle = true;
        this.pathExists = false;
        this.executionTimeMs = executionTimeMs;
        this.verticesProcessed = verticesProcessed;
        this.edgesProcessed = edgesProcessed;
    }
    
    /**
     * Constructs a ShortestPathResult when no path exists (but no negative cycle).
     *
     * @param executionTimeMs execution time in milliseconds
     * @param verticesProcessed number of vertices processed
     * @param edgesProcessed number of edges processed
     */
    public ShortestPathResult(long executionTimeMs, int verticesProcessed, int edgesProcessed) {
        this.path = null;
        this.totalCost = Double.POSITIVE_INFINITY;
        this.negativeCycle = null;
        this.hasNegativeCycle = false;
        this.pathExists = false;
        this.executionTimeMs = executionTimeMs;
        this.verticesProcessed = verticesProcessed;
        this.edgesProcessed = edgesProcessed;
    }
    
    public List<PathStep> getPath() {
        return path != null ? new ArrayList<>(path) : null;
    }
    
    public double getTotalCost() {
        return totalCost;
    }
    
    public NegativeCycle getNegativeCycle() {
        return negativeCycle;
    }
    
    public boolean hasNegativeCycle() {
        return hasNegativeCycle;
    }
    
    public boolean pathExists() {
        return pathExists;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public int getVerticesProcessed() {
        return verticesProcessed;
    }
    
    public int getEdgesProcessed() {
        return edgesProcessed;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== USEI15 - Risk-Aware Shortest Path ===\n\n");
        
        if (hasNegativeCycle) {
            sb.append("ERROR: Negative cycle detected!\n\n");
            if (negativeCycle != null) {
                sb.append(negativeCycle.toString()).append("\n\n");
            }
            sb.append("The graph contains a negative cycle, making shortest path computation impossible.\n");
            sb.append("This indicates a configuration error in the track network.\n");
        } else if (!pathExists) {
            sb.append("No path exists between the source and target stations.\n");
        } else {
            sb.append("Shortest Path Found:\n\n");
            if (path != null && !path.isEmpty()) {
                for (int i = 0; i < path.size(); i++) {
                    PathStep step = path.get(i);
                    if (i == 0) {
                        sb.append("Depart station: ").append(step.getStation().getName());
                    } else {
                        sb.append(" -> ").append(step.getStation().getName());
                    }
                    sb.append(" (cost: ").append(String.format("%.2f", step.getCost())).append(")");
                    if (i < path.size() - 1) {
                        sb.append("\n");
                    }
                }
                sb.append("\n\nTotal Cost to Target: ").append(String.format("%.2f", totalCost)).append("\n");
            }
        }

        return sb.toString();
    }
}


