package main.repositories;

import main.domain.LineSegment;
import main.domain.Siding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     */
    public List<LineSegment> getAll() throws SQLException {
        String sql = "SELECT ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum " +
                     "FROM LineSegment ORDER BY RailLineID, orderNum";
        List<LineSegment> segments = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
        return segments;
    }

    /**
     * Get line segments by rail line ID
     */
    public List<LineSegment> getByRailLineId(int railLineId) throws SQLException {
        String sql = "SELECT ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum " +
                     "FROM LineSegment WHERE RailLineID = ? ORDER BY orderNum";
        List<LineSegment> segments = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, railLineId);
            try (ResultSet rs = stmt.executeQuery()) {
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
     */
    public LineSegment getById(int id) throws SQLException {
        String sql = "SELECT ID, RailLineID, maxWeight, length, numberOfTracks, speedLimit, orderNum " +
                     "FROM LineSegment WHERE ID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
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
     */
    public List<Siding> getAllSidings() throws SQLException {
        String sql = "SELECT ID, LineSegmentID, position, length FROM Siding";
        List<Siding> sidings = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                sidings.add(new Siding(
                    rs.getInt("ID"),
                    rs.getInt("LineSegmentID"),
                    rs.getDouble("position"),
                    rs.getDouble("length")
                ));
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

