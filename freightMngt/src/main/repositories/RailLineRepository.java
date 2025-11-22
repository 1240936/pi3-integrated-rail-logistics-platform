package main.repositories;

import main.domain.Facility;
import main.domain.RailLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     */
    public List<RailLine> getAll() throws SQLException {
        String sql = "SELECT ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified " +
                     "FROM RailLine ORDER BY ID";
        List<RailLine> railLines = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
        return railLines;
    }

    /**
     * Get rail line by ID
     */
    public RailLine getById(int id) throws SQLException {
        String sql = "SELECT ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified " +
                     "FROM RailLine WHERE ID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
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
     */
    public List<RailLine> findConnectingLines(int facilityId1, int facilityId2) throws SQLException {
        String sql = "SELECT ID, OwnerID, StartFacilityID, EndFacilityID, GaugeID, isElectrified " +
                     "FROM RailLine WHERE (StartFacilityID = ? AND EndFacilityID = ?) " +
                     "OR (StartFacilityID = ? AND EndFacilityID = ?)";
        List<RailLine> railLines = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, facilityId1);
            stmt.setInt(2, facilityId2);
            stmt.setInt(3, facilityId2);
            stmt.setInt(4, facilityId1);
            try (ResultSet rs = stmt.executeQuery()) {
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
}

