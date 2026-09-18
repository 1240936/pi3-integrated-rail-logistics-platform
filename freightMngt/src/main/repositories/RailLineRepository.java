package main.repositories;

import main.domain.Facility;
import main.domain.RailLine;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.OracleTypes;

/**
 * Repository for RailLine entities
 */
public class RailLineRepository {
    private final Connection connection;
    private final FacilityRepository facilityRepository;

    public RailLineRepository(Connection connection, FacilityRepository facilityRepository) {
        this.connection = connection;
        this.facilityRepository = facilityRepository;
    }

    /**
     * Get all rail lines
     * Uses PL/SQL function GET_ALL_RAIL_LINES (USLP10).
     */
    public List<RailLine> getAll() throws SQLException {
        List<RailLine> railLines = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_RAIL_LINES()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    Facility startFacility = facilityRepository.getById(rs.getInt("StartFacilityID"));
                    Facility endFacility = facilityRepository.getById(rs.getInt("EndFacilityID"));
                    boolean isElectrified = rs.getInt("isElectrified") == 1;
                    
                    railLines.add(new RailLine(
                        rs.getInt("ID"),
                        rs.getInt("OwnerID"),
                        startFacility,
                        endFacility,
                        rs.getInt("GaugeID"),
                        isElectrified
                    ));
                }
            }
        }
        return railLines;
    }

    /**
     * Get rail line by ID
     * Uses PL/SQL function GET_RAIL_LINE_BY_ID (USLP10).
     */
    public RailLine getById(int id) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_RAIL_LINE_BY_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, id);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                if (rs.next()) {
                    Facility startFacility = facilityRepository.getById(rs.getInt("StartFacilityID"));
                    Facility endFacility = facilityRepository.getById(rs.getInt("EndFacilityID"));
                    boolean isElectrified = rs.getInt("isElectrified") == 1;
                    
                    return new RailLine(
                        rs.getInt("ID"),
                        rs.getInt("OwnerID"),
                        startFacility,
                        endFacility,
                        rs.getInt("GaugeID"),
                        isElectrified
                    );
                }
            }
        }
        return null;
    }

    /**
     * Find rail lines connecting two facilities
     * Uses PL/SQL function GET_CONNECTING_RAIL_LINES (USLP10).
     */
    public List<RailLine> findConnectingLines(int facilityId1, int facilityId2) throws SQLException {
        List<RailLine> railLines = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_CONNECTING_RAIL_LINES(?, ?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, facilityId1);
            stmt.setInt(3, facilityId2);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    Facility startFacility = facilityRepository.getById(rs.getInt("StartFacilityID"));
                    Facility endFacility = facilityRepository.getById(rs.getInt("EndFacilityID"));
                    boolean isElectrified = rs.getInt("isElectrified") == 1;
                    
                    railLines.add(new RailLine(
                        rs.getInt("ID"),
                        rs.getInt("OwnerID"),
                        startFacility,
                        endFacility,
                        rs.getInt("GaugeID"),
                        isElectrified
                    ));
                }
            }
        }
        return railLines;
    }

    /**
     * Get all facilities directly connected to a given facility via rail lines
     * (rail lines are bidirectional, so connections work both ways)
     * Uses PL/SQL function GET_CONNECTED_FACILITIES (USLP10).
     */
    public List<Facility> getConnectedFacilities(int facilityId) throws SQLException {
        List<Facility> connectedFacilities = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_CONNECTED_FACILITIES(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, facilityId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    int connectedFacilityId = rs.getInt("ConnectedFacilityID");
                    Facility facility = facilityRepository.getById(connectedFacilityId);
                    if (facility != null) {
                        connectedFacilities.add(facility);
                    }
                }
            }
        }
        return connectedFacilities;
    }

    /**
     * Check if there's a path from one facility to another using BFS (breadth-first search).
     * This helps ensure that a route can actually reach the destination.
     * 
     * @param fromFacilityId the starting facility ID
     * @param toFacilityId the destination facility ID
     * @return true if a path exists, false otherwise
     * @throws SQLException if there is a database error
     */
    public boolean hasPath(int fromFacilityId, int toFacilityId) throws SQLException {
        if (fromFacilityId == toFacilityId) {
            return true;
        }
        
        // BFS to find path
        java.util.Queue<Integer> queue = new java.util.LinkedList<>();
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        
        queue.offer(fromFacilityId);
        visited.add(fromFacilityId);
        
        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            List<Facility> connected = getConnectedFacilities(currentId);
            
            for (Facility next : connected) {
                int nextId = next.getId();
                if (nextId == toFacilityId) {
                    return true;
                }
                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    queue.offer(nextId);
                }
            }
        }
        
        return false;
    }
}

