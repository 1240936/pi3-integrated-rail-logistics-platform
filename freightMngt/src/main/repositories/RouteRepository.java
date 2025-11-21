package main.repositories;

import main.domain.Facility;
import main.domain.Route;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Route entities
 */
public class RouteRepository {
    private final Connection connection;
    private final FacilityRepository facilityRepository;

    public RouteRepository(Connection connection, FacilityRepository facilityRepository) {
        this.connection = connection;
        this.facilityRepository = facilityRepository;
    }

    /**
     * Get route by ID with path points
     */
    public Route getById(int routeId) throws SQLException {
        String sql = "SELECT ID, StartFacilityID, EndFacilityID, TrainID, startDate " +
                     "FROM Route WHERE ID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Facility startFacility = facilityRepository.getById(rs.getInt("StartFacilityID"));
                    Facility endFacility = facilityRepository.getById(rs.getInt("EndFacilityID"));
                    Timestamp startDate = rs.getTimestamp("startDate");
                    LocalDateTime startDateTime = startDate != null ? startDate.toLocalDateTime() : null;
                    
                    Route route = new Route(
                        rs.getInt("ID"),
                        rs.getInt("TrainID"),
                        startFacility,
                        endFacility,
                        startDateTime
                    );
                    
                    // Load path points
                    loadPathPoints(route);
                    
                    return route;
                }
            }
        }
        return null;
    }

    /**
     * Get all routes
     */
    public List<Route> getAll() throws SQLException {
        String sql = "SELECT ID FROM Route ORDER BY ID";
        List<Route> routes = new java.util.ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Route route = getById(rs.getInt("ID"));
                if (route != null) {
                    routes.add(route);
                }
            }
        }
        return routes;
    }

    /**
     * Get routes for a specific train
     */
    public List<Route> getByTrainId(int trainId) throws SQLException {
        String sql = "SELECT ID FROM Route WHERE TrainID = ? ORDER BY ID";
        List<Route> routes = new java.util.ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, trainId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Route route = getById(rs.getInt("ID"));
                    if (route != null) {
                        routes.add(route);
                    }
                }
            }
        }
        return routes;
    }

    /**
     * Load path points for a route
     */
    private void loadPathPoints(Route route) throws SQLException {
        String sql = "SELECT FacilityID, seqNumber FROM Path " +
                     "WHERE RouteID = ? ORDER BY seqNumber";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, route.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Facility facility = facilityRepository.getById(rs.getInt("FacilityID"));
                    if (facility != null) {
                        route.addPathPoint(facility, rs.getInt("seqNumber"));
                    }
                }
            }
        }
    }

    /**
     * Create a new route
     */
    public int createRoute(int trainId, int startFacilityId, int endFacilityId, 
                          LocalDateTime startDate) throws SQLException {
        String sql = "INSERT INTO Route (StartFacilityID, EndFacilityID, TrainID, startDate) " +
                     "VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(
                sql, new String[]{"ID"})) {
            stmt.setInt(1, startFacilityId);
            stmt.setInt(2, endFacilityId);
            stmt.setInt(3, trainId);
            stmt.setTimestamp(4, Timestamp.valueOf(startDate));
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create route");
    }

    /**
     * Add a path point to a route
     */
    public void addPathPoint(int routeId, int facilityId, int sequenceNumber) throws SQLException {
        String sql = "INSERT INTO Path (RouteID, FacilityID, seqNumber) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            stmt.setInt(2, facilityId);
            stmt.setInt(3, sequenceNumber);
            stmt.executeUpdate();
        }
    }
}

