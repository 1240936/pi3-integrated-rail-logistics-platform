package main.domain;

import java.util.List;
import java.util.Set;

/**
 * Result of topological sort computation for USEI11.
 * Encapsulates the result of computing upgrade order for railway stations.
 */
public class TopologicalSortResult {
    private final List<Station> topologicalOrder;  // null se houver ciclos
    private final Set<Station> cycleStations;      // null se não houver ciclos
    private final boolean hasCycle;
    
    public TopologicalSortResult(List<Station> topologicalOrder, 
                                Set<Station> cycleStations,
                                boolean hasCycle) {
        this.topologicalOrder = topologicalOrder;
        this.cycleStations = cycleStations;
        this.hasCycle = hasCycle;
    }
    
    public List<Station> getTopologicalOrder() {
        return topologicalOrder;
    }
    
    public Set<Station> getCycleStations() {
        return cycleStations;
    }
    
    public boolean hasCycle() {
        return hasCycle;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== USEI11 - Directed Line Upgrade Plan ===\n\n");
        
        if (hasCycle) {
            sb.append("Graph contains cycles. Not possible to find an order.\n\n");
            
            if (cycleStations != null && !cycleStations.isEmpty()) {
                sb.append("Stations in cycles:\n");
                for (Station station : cycleStations) {
                    sb.append("- ").append(station.getName()).append("\n");
                }
            }
        } else {
            sb.append("Grafo é DAG (sem ciclos)\n\n");
            
            if (topologicalOrder != null && !topologicalOrder.isEmpty()) {
                sb.append("Topological Upgrade Order:\n");
                int index = 1;
                for (Station station : topologicalOrder) {
                    sb.append(index).append(". ").append(station.getName()).append("\n");
                    index++;
                }
                sb.append("\nTotal stations: ").append(topologicalOrder.size()).append("\n");
            }
        }
        
        return sb.toString();
    }
}

