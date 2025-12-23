package main.repositories;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Repository for TrainEvent entities
 */
public class TrainEventRepository {
    private final Connection connection;

    public TrainEventRepository(Connection connection, FacilityRepository facilityRepository) {
        this.connection = connection;
    }

    /**
     * Create a new train event.
     * Based on USBD31: TrainEvent table has ID, FacilityID, TrainID, eventTime, eventType (NO RouteID).
     * RouteID parameter is kept for interface compatibility but not stored in database.
     * 
     * This method uses PL/SQL function CREATE_TRAIN_EVENT to access the database.
     */
    public void createEvent(int routeId, int trainId, int facilityId, LocalDateTime eventTime) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL CREATE_TRAIN_EVENT(?, ?, ?, ?)}")) {
            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.setInt(2, routeId);
            stmt.setInt(3, trainId);
            stmt.setInt(4, facilityId);
            stmt.setTimestamp(5, Timestamp.valueOf(eventTime));
            stmt.execute();
            
            // Check return value (should be 1 on success)
            int result = stmt.getInt(1);
            if (result != 1) {
                throw new SQLException("Failed to create train event. Return value: " + result);
            }
        }
    }

    /**
     * Delete all events for a route (to recalculate).
     * Based on USBD31: Since TrainEvent doesn't have RouteID, we delete by eventType containing route ID.
     * 
     * This method uses PL/SQL function DELETE_TRAIN_EVENTS_BY_ROUTE_ID to access the database.
     */
    public void deleteByRouteId(int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL DELETE_TRAIN_EVENTS_BY_ROUTE_ID(?)}")) {
            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            // The function returns the number of deleted rows, which we can ignore
            // (it's logged but not critical for the operation)
            int deletedCount = stmt.getInt(1);
        }
    }
}

