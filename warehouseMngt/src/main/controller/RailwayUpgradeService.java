package main.controller;

import main.domain.Station;
import main.domain.TopologicalSortResult;
import main.repositories.CsvValidatorResult;
import main.repositories.StationToStationCsvLoader;
import main.repositories.StationsWithIdCsvLoader;
import main.graph.Edge;
import main.graph.map.MapGraph;
import main.graph.Algorithms;

import java.io.IOException;
import java.util.*;

/**
 * Service for computing topological order of station upgrades (USEI11).
 * Implements the Directed Line Upgrade Plan functionality.
 */
public class RailwayUpgradeService {
    
    /**
     * Computes topological order for station upgrades (USEI11).
     * 
     * @param stationsCsvPath path to CSV with stations (format: Station id,Station,Lat,Lon,CoordX,CoordY)
     * @param linesCsvPath path to CSV with station connections (format: departure_stid,arrival_stid,dist,capacity,cost)
     * @return result with topological order or cycle information
     * @throws IOException if there is an error reading the CSV file
     */
    public TopologicalSortResult computeUpgradeOrder(String stationsCsvPath, String linesCsvPath) 
            throws IOException {
        
        // 1. Carregar estações do CSV com IDs
        Map<String, Station> stationMap = StationsWithIdCsvLoader.loadWithIdMap(stationsCsvPath);
        
        if (stationMap.isEmpty()) {
            throw new IllegalStateException("No stations loaded from CSV file: " + stationsCsvPath);
        }
        
        // 2. Carregar conexões do CSV
        CsvValidatorResult<Edge<Station, Double>> edgesResult = 
            StationToStationCsvLoader.load(linesCsvPath, stationMap);
        
        if (edgesResult.hasErrors()) {
            System.err.println("Warnings loading edges:");
            for (String error : edgesResult.getErrors()) {
                System.err.println("  " + error);
            }
        }
        
        if (edgesResult.getRecords().isEmpty()) {
            throw new IllegalStateException("No valid edges found in CSV file. Cannot compute upgrade order.");
        }
        
        // 3. Construir grafo direcionado
        MapGraph<Station, Double> graph = new MapGraph<>(true); // true = directed
        
        // Adicionar todas as estações do mapa
        for (Station station : stationMap.values()) {
            graph.addVertex(station);
        }
        
        // Adicionar arestas
        for (Edge<Station, Double> edge : edgesResult.getRecords()) {
            graph.addEdge(edge.getVOrig(), edge.getVDest(), edge.getWeight());
        }
        
        // 4. Executar ordenação topológica
        LinkedList<Station> topologicalOrder = 
            Algorithms.topologicalSort(graph);
        
        // 5. Verificar se há ciclos
        if (topologicalOrder == null) {
            // Há ciclos - identificar vértices
            Set<Station> cycleStations = Algorithms.findCycleVertices(graph);
            
            return new TopologicalSortResult(
                null,
                cycleStations,
                true
            );
        } else {
            // Sem ciclos - retornar ordem
            return new TopologicalSortResult(
                topologicalOrder,
                null,
                false
            );
        }
    }
}

