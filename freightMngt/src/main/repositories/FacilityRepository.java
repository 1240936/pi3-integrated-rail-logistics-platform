package main.repositories;

import main.domain.Facility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for Facility entities
 */
public class FacilityRepository {
    private final Connection connection;

    public FacilityRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get all facilities
     */
    public List<Facility> getAll() throws SQLException {
        String sql = "SELECT ID, name FROM Facility ORDER BY ID";
        List<Facility> facilities = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                facilities.add(new Facility(
                    rs.getInt("ID"),
                    rs.getString("name")
                ));
            }
        }
        return facilities;
    }

    /**
     * Get facility by ID
     */
    public Facility getById(int id) throws SQLException {
        String sql = "SELECT ID, name FROM Facility WHERE ID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Facility(
                        rs.getInt("ID"),
                        rs.getString("name")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Get all facilities as a map (id -> Facility) for quick lookups
     */
    public Map<Integer, Facility> getAllAsMap() throws SQLException {
        Map<Integer, Facility> map = new HashMap<>();
        for (Facility facility : getAll()) {
            map.put(facility.getId(), facility);
        }
        return map;
    }
}

