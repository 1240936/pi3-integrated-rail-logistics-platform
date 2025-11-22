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
     * Create a new train event
     */
    public void createEvent(int routeId, int trainId, int facilityId, LocalDateTime eventTime) throws SQLException {
        String sql = "INSERT INTO TrainEvent (RouteID, TrainID, FacilityID, eventTime) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            stmt.setInt(2, trainId);
            stmt.setInt(3, facilityId);
            stmt.setTimestamp(4, Timestamp.valueOf(eventTime));
            stmt.executeUpdate();
        }
    }

    /**
     * Delete all events for a route (to recalculate)
     */
    public void deleteByRouteId(int routeId) throws SQLException {
        String sql = "DELETE FROM TrainEvent WHERE RouteID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            stmt.executeUpdate();
        }
    }
}

