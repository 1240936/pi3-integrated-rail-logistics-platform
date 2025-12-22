package main.repositories;

import main.domain.Facility;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.jdbc.OracleTypes;

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
     * Uses PL/SQL function GET_ALL_FACILITIES (USLP10).
     */
    public List<Facility> getAll() throws SQLException {
        List<Facility> facilities = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_FACILITIES()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    facilities.add(new Facility(
                        rs.getInt("ID"),
                        rs.getString("name")
                    ));
                }
            }
        }
        return facilities;
    }

    /**
     * Get facility by ID
     * Uses PL/SQL function GET_FACILITY_BY_ID (USLP10).
     */
    public Facility getById(int id) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_FACILITY_BY_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, id);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
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

