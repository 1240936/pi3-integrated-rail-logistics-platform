package main.repositories;

import main.domain.Facility;
import main.domain.Route;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.OracleTypes;

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
     * 
     * This method uses PL/SQL functions GET_ROUTE_BY_ID and GET_PATH_POINTS_BY_ROUTE_ID to access the database.
     */
    public Route getById(int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ROUTE_BY_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
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
                    
                    // Load path points using PL/SQL function
                    loadPathPoints(route);
                    
                    return route;
                }
            }
        }
        return null;
    }

    /**
     * Get all routes
     * Uses PL/SQL function GET_ALL_ROUTE_IDS to access the database.
     */
    public List<Route> getAll() throws SQLException {
        List<Route> routes = new java.util.ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_ROUTE_IDS()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
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
     * Get routes for a specific train.
     * Based on USBD31: Uses Planned_Train to find routes for a train.
     * 
     * This method uses PL/SQL function GET_ROUTES_BY_TRAIN_ID to access the database.
     */
    public List<Route> getByTrainId(int trainId) throws SQLException {
        List<Route> routes = new java.util.ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ROUTES_BY_TRAIN_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, trainId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
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
     * 
     * This method uses PL/SQL function GET_PATH_POINTS_BY_ROUTE_ID to access the database.
     */
    private void loadPathPoints(Route route) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_PATH_POINTS_BY_ROUTE_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, route.getId());
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
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
     * 
     * This method uses PL/SQL function CREATE_ROUTE to access the database.
     */
    public int createRoute(int trainId, int startFacilityId, int endFacilityId, 
                          LocalDateTime startDate) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL CREATE_ROUTE(?, ?, ?, ?)}")) {
            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.setInt(2, trainId);
            stmt.setInt(3, startFacilityId);
            stmt.setInt(4, endFacilityId);
            stmt.setTimestamp(5, Timestamp.valueOf(startDate));
            stmt.execute();
            
            return stmt.getInt(1);
        }
    }

    /**
     * Add a path point to a route
     * 
     * This method uses PL/SQL function ADD_PATH_POINT to access the database.
     */
    public void addPathPoint(int routeId, int facilityId, int sequenceNumber) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL ADD_PATH_POINT(?, ?, ?)}")) {
            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.setInt(2, routeId);
            stmt.setInt(3, facilityId);
            stmt.setInt(4, sequenceNumber);
            stmt.execute();
            
            // Check return value (should be 1 on success)
            int result = stmt.getInt(1);
            if (result != 1) {
                throw new SQLException("Failed to add path point. Return value: " + result);
            }
        }
    }

    /**
     * Delete a route and all its associated path points and train events
     * Uses PL/SQL function DELETE_ROUTE to access the database.
     */
    public boolean deleteRoute(int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL DELETE_ROUTE(?)}")) {
            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            int result = stmt.getInt(1);
            return result > 0;
        }
    }
    
}

