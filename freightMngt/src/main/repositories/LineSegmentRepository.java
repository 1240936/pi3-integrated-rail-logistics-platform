package main.repositories;

import main.domain.LineSegment;
import main.domain.Siding;

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
 * Repository for LineSegment and Siding entities
 */
public class LineSegmentRepository {
    private final Connection connection;

    public LineSegmentRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get all line segments
     * Uses PL/SQL function GET_ALL_LINE_SEGMENTS (USLP10).
     */
    public List<LineSegment> getAll() throws SQLException {
        List<LineSegment> segments = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_LINE_SEGMENTS()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    segments.add(new LineSegment(
                        rs.getInt("ID"),
                        rs.getInt("RailLineID"),
                        rs.getDouble("maxWeight"),
                        rs.getDouble("length"),
                        rs.getInt("numberOfTracks"),
                        rs.getDouble("speedLimit"),
                        rs.getInt("orderNum")
                    ));
                }
            }
        }
        return segments;
    }

    /**
     * Get line segments by rail line ID
     * Uses PL/SQL function GET_LINE_SEGMENTS_BY_RAIL_LINE_ID (USLP10).
     */
    public List<LineSegment> getByRailLineId(int railLineId) throws SQLException {
        List<LineSegment> segments = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_LINE_SEGMENTS_BY_RAIL_LINE_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, railLineId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    segments.add(new LineSegment(
                        rs.getInt("ID"),
                        rs.getInt("RailLineID"),
                        rs.getDouble("maxWeight"),
                        rs.getDouble("length"),
                        rs.getInt("numberOfTracks"),
                        rs.getObject("speedLimit", Double.class),
                        rs.getInt("orderNum")
                    ));
                }
            }
        }
        return segments;
    }

    /**
     * Get line segment by ID
     * Uses PL/SQL function GET_LINE_SEGMENT_BY_ID (USLP10).
     */
    public LineSegment getById(int id) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_LINE_SEGMENT_BY_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, id);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                if (rs.next()) {
                    return new LineSegment(
                        rs.getInt("ID"),
                        rs.getInt("RailLineID"),
                        rs.getDouble("maxWeight"),
                        rs.getDouble("length"),
                        rs.getInt("numberOfTracks"),
                        rs.getObject("speedLimit", Double.class),
                        rs.getInt("orderNum")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Get all sidings
     * Uses PL/SQL function GET_ALL_SIDINGS (USLP10).
     */
    public List<Siding> getAllSidings() throws SQLException {
        List<Siding> sidings = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_SIDINGS()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    sidings.add(new Siding(
                        rs.getInt("ID"),
                        rs.getInt("LineSegmentID"),
                        rs.getDouble("position"),
                        rs.getDouble("length")
                    ));
                }
            }
        }
        return sidings;
    }

    /**
     * Get a map of line segment ID to list of sidings
     */
    public Map<Integer, List<Siding>> getSidingsBySegmentMap() throws SQLException {
        Map<Integer, List<Siding>> map = new HashMap<>();
        List<Siding> allSidings = getAllSidings();
        for (Siding siding : allSidings) {
            map.computeIfAbsent(siding.getLineSegmentId(), k -> new ArrayList<>()).add(siding);
        }
        return map;
    }
}

