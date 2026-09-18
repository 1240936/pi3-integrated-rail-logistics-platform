package main.controller;

import main.repositories.FacilityRepository;
import main.repositories.RailLineRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Service for automatic path calculation between facilities.
 * Uses breadth-first search (BFS) to find the shortest path.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class AutomaticPathService {
    private final RailLineRepository railLineRepository;
    private final FacilityRepository facilityRepository;

    /**
     * Constructs an AutomaticPathService with the given database connection.
     * 
     * @param connection the database connection to use
     */
    public AutomaticPathService(Connection connection) {
        FacilityRepository facilityRepo = new FacilityRepository(connection);
        this.railLineRepository = new RailLineRepository(connection, facilityRepo);
        this.facilityRepository = facilityRepo;
    }

    /**
     * Calculate the shortest path between two facilities using BFS.
     * 
     * @param startFacilityId the starting facility ID
     * @param endFacilityId the destination facility ID
     * @return list of facility IDs representing the path (including start and end), or null if no path exists
     * @throws SQLException if there is a database error
     */
    public List<Integer> calculateShortestPath(int startFacilityId, int endFacilityId) throws SQLException {
        if (startFacilityId == endFacilityId) {
            return Arrays.asList(startFacilityId);
        }
        
        // BFS to find shortest path
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> parentMap = new HashMap<>(); // child -> parent
        Set<Integer> visited = new HashSet<>();
        
        queue.offer(startFacilityId);
        visited.add(startFacilityId);
        parentMap.put(startFacilityId, null);
        
        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            
            if (currentId == endFacilityId) {
                // Reconstruct path
                return reconstructPath(parentMap, startFacilityId, endFacilityId);
            }
            
            // Get connected facilities
            List<Integer> connectedIds = new ArrayList<>();
            try {
                var connectedFacilities = railLineRepository.getConnectedFacilities(currentId);
                for (var facility : connectedFacilities) {
                    connectedIds.add(facility.getId());
                }
            } catch (SQLException e) {
                // If we can't get connected facilities, continue
                continue;
            }
            
            for (Integer nextId : connectedIds) {
                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    parentMap.put(nextId, currentId);
                    queue.offer(nextId);
                }
            }
        }
        
        // No path found
        return null;
    }

    /**
     * Reconstruct the path from parent map.
     * 
     * @param parentMap map of child -> parent
     * @param startId the starting facility ID
     * @param endId the ending facility ID
     * @return list of facility IDs in path order
     */
    private List<Integer> reconstructPath(Map<Integer, Integer> parentMap, int startId, int endId) {
        List<Integer> path = new ArrayList<>();
        Integer current = endId;
        
        while (current != null) {
            path.add(current);
            current = parentMap.get(current);
        }
        
        // Reverse to get path from start to end
        Collections.reverse(path);
        return path;
    }
}

