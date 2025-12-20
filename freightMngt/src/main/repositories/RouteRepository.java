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
     * Get route by ID with path points.
     * Based on USBD31: Route table doesn't have TrainID or startDate.
     * These are in Planned_Train table, so we join to get them.
     */
    public Route getById(int routeId) throws SQLException {
        // In USBD31, Route table has: ID, StartFacilityID, EndFacilityID
        // TrainID and startDate are in Planned_Train table
        // We join to get the train and startDate, using the first Planned_Train entry for this route
        String sql = "SELECT r.ID, r.StartFacilityID, r.EndFacilityID, pt.TrainID, pt.startDate " +
                     "FROM Route r " +
                     "LEFT JOIN Planned_Train pt ON r.ID = pt.RouteID " +
                     "WHERE r.ID = ? " +
                     "AND ROWNUM = 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Facility startFacility = facilityRepository.getById(rs.getInt("StartFacilityID"));
                    Facility endFacility = facilityRepository.getById(rs.getInt("EndFacilityID"));
                    
                    // TrainID and startDate may be NULL if no Planned_Train entry exists
                    Integer trainId = rs.getObject("TrainID") != null ? rs.getInt("TrainID") : 0;
                    Timestamp startDate = rs.getTimestamp("startDate");
                    LocalDateTime startDateTime = startDate != null ? startDate.toLocalDateTime() : null;
                    
                    Route route = new Route(
                        rs.getInt("ID"),
                        trainId,
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
     * Get routes for a specific train.
     * Based on USBD31: Uses Planned_Train to find routes for a train.
     */
    public List<Route> getByTrainId(int trainId) throws SQLException {
        // In USBD31, train-to-route relationship is in Planned_Train table
        String sql = "SELECT DISTINCT RouteID FROM Planned_Train WHERE TrainID = ? ORDER BY RouteID";
        List<Route> routes = new java.util.ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, trainId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Route route = getById(rs.getInt("RouteID"));
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
     * Create a new route.
     * Based on USBD31: Creates Route entry and Planned_Train entry separately.
     */
    public int createRoute(int trainId, int startFacilityId, int endFacilityId, 
                          LocalDateTime startDate) throws SQLException {
        // In USBD31, Route table has: ID, StartFacilityID, EndFacilityID
        // TrainID and startDate go in Planned_Train table
        // First, get next Route ID from Route table
        // In Oracle, unquoted identifiers are stored in uppercase
        int routeId = 1; // Default starting ID
        String routeIdSql = "SELECT NVL(MAX(ID), 0) + 1 AS NEXT_ID FROM Route";
        try (PreparedStatement stmt = connection.prepareStatement(routeIdSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                routeId = rs.getInt("NEXT_ID");
            }
        } catch (SQLException e) {
            // If query fails (e.g., table doesn't exist), start with ID 1
            routeId = 1;
        }
        
        // Insert into Route table
        String sql = "INSERT INTO Route (ID, StartFacilityID, EndFacilityID) " +
                     "VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            stmt.setInt(2, startFacilityId);
            stmt.setInt(3, endFacilityId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // If insertion fails due to duplicate ID (error code 1), get a new one and retry
            if (e.getErrorCode() == 1) { // Unique constraint violation
                String maxIdSql = "SELECT NVL(MAX(ID), 0) FROM Route";
                try (PreparedStatement maxStmt = connection.prepareStatement(maxIdSql);
                     ResultSet maxRs = maxStmt.executeQuery()) {
                    if (maxRs.next()) {
                        routeId = maxRs.getInt(1) + 1;
                    }
                }
                // Retry insertion with new ID
                try (PreparedStatement retryStmt = connection.prepareStatement(sql)) {
                    retryStmt.setInt(1, routeId);
                    retryStmt.setInt(2, startFacilityId);
                    retryStmt.setInt(3, endFacilityId);
                    retryStmt.executeUpdate();
                }
            } else {
                throw e;
            }
        }
        
        // Insert into Planned_Train to link train and startDate to route
        String plannedTrainSql = "INSERT INTO Planned_Train (TrainID, startDate, RouteID) " +
                                  "VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(plannedTrainSql)) {
            stmt.setInt(1, trainId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            stmt.setInt(3, routeId);
            stmt.executeUpdate();
        }
        
        return routeId;
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

    /**
     * Delete a route and all its associated path points and train events
     */
    public boolean deleteRoute(int routeId) throws SQLException {
        // First delete path points (foreign key constraint)
        String deletePathSql = "DELETE FROM Path WHERE RouteID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deletePathSql)) {
            stmt.setInt(1, routeId);
            stmt.executeUpdate();
        }
        
        // Delete planned trains (foreign key constraint)
        String deletePlannedTrainSql = "DELETE FROM Planned_Train WHERE RouteID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deletePlannedTrainSql)) {
            stmt.setInt(1, routeId);
            stmt.executeUpdate();
        }
        
        // Delete train events for this route
        // In USBD31, TrainEvent doesn't have RouteID, so we delete by eventType (which stores route ID)
        try {
            String deleteEventsSql = "DELETE FROM TrainEvent WHERE eventType = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteEventsSql)) {
                stmt.setString(1, "ROUTE_" + routeId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // If that fails, try deleting by train ID from Planned_Train
            try {
                String trainSql = "SELECT TrainID FROM Planned_Train WHERE RouteID = ? AND ROWNUM = 1";
                try (PreparedStatement trainStmt = connection.prepareStatement(trainSql)) {
                    trainStmt.setInt(1, routeId);
                    try (ResultSet rs = trainStmt.executeQuery()) {
                        if (rs.next()) {
                            int trainId = rs.getInt("TrainID");
                            String deleteSql = "DELETE FROM TrainEvent WHERE TrainID = ?";
                            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                                deleteStmt.setInt(1, trainId);
                                deleteStmt.executeUpdate();
                            }
                        }
                    }
                }
            } catch (SQLException e2) {
                // If deletion fails, continue - route deletion should still proceed
            }
        }
        
        // Finally delete the route
        String deleteRouteSql = "DELETE FROM Route WHERE ID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteRouteSql)) {
            stmt.setInt(1, routeId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
}

