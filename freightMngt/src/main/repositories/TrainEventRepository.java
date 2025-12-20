package main.repositories;

import main.domain.Facility;
import main.domain.TrainEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for TrainEvent entities
 */
public class TrainEventRepository {
    private final Connection connection;
    private final FacilityRepository facilityRepository;

    public TrainEventRepository(Connection connection, FacilityRepository facilityRepository) {
        this.connection = connection;
        this.facilityRepository = facilityRepository;
    }

    /**
     * Create a new train event.
     * Based on USBD31: TrainEvent table has ID, FacilityID, TrainID, eventTime, eventType (NO RouteID).
     * RouteID parameter is kept for interface compatibility but not stored in database.
     */
    public void createEvent(int routeId, int trainId, int facilityId, LocalDateTime eventTime) throws SQLException {
        // In USBD31, TrainEvent doesn't have RouteID column
        // We store: TrainID, FacilityID, eventTime, eventType
        // eventType can be used to store route information or just be a generic value
        String sql = "INSERT INTO TrainEvent (TrainID, FacilityID, eventTime, eventType) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, trainId);
            stmt.setInt(2, facilityId);
            stmt.setTimestamp(3, Timestamp.valueOf(eventTime));
            stmt.setString(4, "ROUTE_" + routeId); // Store route ID in eventType for identification
            stmt.executeUpdate();
        }
    }

    /**
     * Delete all events for a route (to recalculate).
     * Based on USBD31: Since TrainEvent doesn't have RouteID, we delete by TrainID.
     * Note: This deletes ALL events for the train, not just for this route.
     * In USBD31, a train can have multiple routes via Planned_Train, so this is a limitation.
     */
    public void deleteByRouteId(int routeId) throws SQLException {
        // In USBD31, TrainEvent doesn't have RouteID column
        // We need to find the train ID for this route from Planned_Train
        // Then delete events for that train
        // Since we can identify events by eventType containing route ID, we can delete those
        String sql = "DELETE FROM TrainEvent WHERE eventType = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "ROUTE_" + routeId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // If eventType approach doesn't work, try deleting by train ID from Planned_Train
            String trainSql = "SELECT TrainID FROM Planned_Train WHERE RouteID = ? AND ROWNUM = 1";
            try (PreparedStatement trainStmt = connection.prepareStatement(trainSql)) {
                trainStmt.setInt(1, routeId);
                try (ResultSet rs = trainStmt.executeQuery()) {
                    if (rs.next()) {
                        int trainId = rs.getInt("TrainID");
                        // Delete all events for this train (since we can't filter by route)
                        String deleteSql = "DELETE FROM TrainEvent WHERE TrainID = ?";
                        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                            deleteStmt.setInt(1, trainId);
                            deleteStmt.executeUpdate();
                        }
                    }
                }
            }
        }
    }
}

