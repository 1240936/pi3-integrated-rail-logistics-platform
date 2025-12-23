package main.repositories;

import main.domain.Facility;
import main.domain.Freight;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.jdbc.OracleTypes;

/**
 * Repository for Freight entities
 */
public class FreightRepository {
    private final Connection connection;
    private final FacilityRepository facilityRepository;

    public FreightRepository(Connection connection, FacilityRepository facilityRepository) {
        this.connection = connection;
        this.facilityRepository = facilityRepository;
    }

    /**
     * Get all freight for a specific route.
     * Based on USBD31: Freight is linked to a route through Assigned_Freight -> Planned_Train -> Route
     * 
     * IMPORTANT: Filters by both RouteID and startDate to ensure we're looking at the correct journey.
     * A train can be planned for multiple routes with different start dates, so we must match both.
     * 
     * The join path is: Freight -> Assigned_Freight -> Planned_Train -> Route
     * 
     * This method uses PL/SQL function GET_FREIGHT_BY_ROUTE to access the database.
     * 
     * @param routeId the ID of the route
     * @param startDate the scheduled start date for this route (from Planned_Train)
     * @return list of freights assigned to this route
     * @throws SQLException if there is a database error
     */
    public List<Freight> getByRouteId(int routeId, java.time.LocalDateTime startDate) throws SQLException {
        List<Freight> freightList = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_FREIGHT_BY_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, routeId);
            java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(startDate);
            stmt.setTimestamp(3, timestamp);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    Facility origin = facilityRepository.getById(rs.getInt("OriginFacilityID"));
                    Facility destination = facilityRepository.getById(rs.getInt("DestinationFacilityID"));
                    
                    Freight freight = new Freight(
                        rs.getInt("ID"),
                        routeId, // RouteID from parameter (for domain model compatibility)
                        origin,
                        destination
                    );
                    freightList.add(freight);
                }
            }
        }
        return freightList;
    }
    
    /**
     * Get all freight for a specific route (backward compatibility method).
     * This method retrieves the startDate from Planned_Train and calls the main method.
     * 
     * IMPORTANT: This method should only be used when the startDate is not available from the Route object.
     * Always prefer using getByRouteId(routeId, startDate) with the exact startDate from the Route.
     * 
     * This method uses PL/SQL function GET_START_DATE_BY_ROUTE_ID to access the database.
     * 
     * @param routeId the ID of the route
     * @return list of freights assigned to this route
     * @throws SQLException if there is a database error
     */
    public List<Freight> getByRouteId(int routeId) throws SQLException {
        // Get the startDate for this route using PL/SQL function
        java.sql.Timestamp timestamp = null;
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_START_DATE_BY_ROUTE_ID(?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.TIMESTAMP);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            timestamp = stmt.getTimestamp(1);
            if (timestamp != null) {
                java.time.LocalDateTime startDate = timestamp.toLocalDateTime();
                return getByRouteId(routeId, startDate);
            }
        }
        // If no startDate found, return empty list
        return new ArrayList<>();
    }

    /**
     * Get wagon IDs for a freight (from both Assigned_Freight and Unassigned_Freight)
     * 
     * This method uses PL/SQL function GET_WAGON_IDS_BY_FREIGHT_ID to access the database.
     */
    public List<Integer> getWagonIdsByFreightId(int freightId) throws SQLException {
        List<Integer> wagonIds = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_WAGON_IDS_BY_FREIGHT_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, freightId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    wagonIds.add(rs.getInt("WagonID"));
                }
            }
        }
        return wagonIds;
    }

    /**
     * Get map of facility ID to list of freight pickups (freight that originates at this facility).
     * Based on USBD31: Uses Assigned_Freight to find freight for a route.
     * 
     * IMPORTANT: Filters by both RouteID and startDate to ensure we're looking at the correct journey.
     * 
     * This method uses PL/SQL function GET_FREIGHT_PICKUPS_BY_FACILITY to access the database.
     * 
     * @param routeId the ID of the route
     * @param startDate the scheduled start date for this route (from Planned_Train)
     * @return map of facility ID to list of freights that originate at that facility
     * @throws SQLException if there is a database error
     */
    public Map<Integer, List<Freight>> getPickupsByFacility(int routeId, java.time.LocalDateTime startDate) throws SQLException {
        Map<Integer, List<Freight>> pickups = new HashMap<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_FREIGHT_PICKUPS_BY_FACILITY(?, ?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, routeId);
            java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(startDate);
            stmt.setTimestamp(3, timestamp);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    int originId = rs.getInt("OriginFacilityID");
                    Facility origin = facilityRepository.getById(originId);
                    Facility destination = facilityRepository.getById(rs.getInt("DestinationFacilityID"));
                    
                    Freight freight = new Freight(
                        rs.getInt("ID"),
                        routeId,
                        origin,
                        destination
                    );
                    
                    pickups.computeIfAbsent(originId, k -> new ArrayList<>()).add(freight);
                }
            }
        }
        return pickups;
    }
    
    /**
     * Get map of facility ID to list of freight pickups (backward compatibility method).
     * This method retrieves the startDate from Planned_Train and calls the main method.
     * 
     * This method uses PL/SQL function GET_START_DATE_BY_ROUTE_ID to access the database.
     */
    public Map<Integer, List<Freight>> getPickupsByFacility(int routeId) throws SQLException {
        // Get the startDate for this route using PL/SQL function
        java.sql.Timestamp timestamp = null;
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_START_DATE_BY_ROUTE_ID(?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.TIMESTAMP);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            timestamp = stmt.getTimestamp(1);
            if (timestamp != null) {
                java.time.LocalDateTime startDate = timestamp.toLocalDateTime();
                return getPickupsByFacility(routeId, startDate);
            }
        }
        // If no startDate found, return empty map
        return new HashMap<>();
    }

    /**
     * Get map of facility ID to list of freight deliveries (freight that ends at this facility).
     * Based on USBD31: Uses Assigned_Freight to find freight for a route.
     * 
     * IMPORTANT: Filters by both RouteID and startDate to ensure we're looking at the correct journey.
     * 
     * This method uses PL/SQL function GET_FREIGHT_DELIVERIES_BY_FACILITY to access the database.
     * 
     * @param routeId the ID of the route
     * @param startDate the scheduled start date for this route (from Planned_Train)
     * @return map of facility ID to list of freights that end at that facility
     * @throws SQLException if there is a database error
     */
    public Map<Integer, List<Freight>> getDeliveriesByFacility(int routeId, java.time.LocalDateTime startDate) throws SQLException {
        Map<Integer, List<Freight>> deliveries = new HashMap<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_FREIGHT_DELIVERIES_BY_FACILITY(?, ?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, routeId);
            java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(startDate);
            stmt.setTimestamp(3, timestamp);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    int destinationId = rs.getInt("DestinationFacilityID");
                    Facility origin = facilityRepository.getById(rs.getInt("OriginFacilityID"));
                    Facility destination = facilityRepository.getById(destinationId);
                    
                    Freight freight = new Freight(
                        rs.getInt("ID"),
                        routeId,
                        origin,
                        destination
                    );
                    
                    deliveries.computeIfAbsent(destinationId, k -> new ArrayList<>()).add(freight);
                }
            }
        }
        return deliveries;
    }
    
    /**
     * Get map of facility ID to list of freight deliveries (backward compatibility method).
     * This method retrieves the startDate from Planned_Train and calls the main method.
     * 
     * This method uses PL/SQL function GET_START_DATE_BY_ROUTE_ID to access the database.
     */
    public Map<Integer, List<Freight>> getDeliveriesByFacility(int routeId) throws SQLException {
        // Get the startDate for this route using PL/SQL function
        java.sql.Timestamp timestamp = null;
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_START_DATE_BY_ROUTE_ID(?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.TIMESTAMP);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            timestamp = stmt.getTimestamp(1);
            if (timestamp != null) {
                java.time.LocalDateTime startDate = timestamp.toLocalDateTime();
                return getDeliveriesByFacility(routeId, startDate);
            }
        }
        // If no startDate found, return empty map
        return new HashMap<>();
    }

    /**
     * Get all freight that is not yet assigned to a route.
     * Based on USBD31: Freight is considered unassigned if it exists in Unassigned_Freight table.
     * 
     * This method uses PL/SQL function GET_UNASSIGNED_FREIGHT to access the database.
     */
    public List<Freight> getUnassignedFreight() throws SQLException {
        List<Freight> freightList = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_UNASSIGNED_FREIGHT()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    Facility origin = facilityRepository.getById(rs.getInt("OriginFacilityID"));
                    Facility destination = facilityRepository.getById(rs.getInt("DestinationFacilityID"));
                    
                    Freight freight = new Freight(
                        rs.getInt("ID"),
                        0, // RouteID is 0 for unassigned (USBD31 doesn't have RouteID column)
                        origin,
                        destination
                    );
                    freightList.add(freight);
                }
            }
        }
        return freightList;
    }

    /**
     * Assign freight to a route.
     * Moves freight wagons from Unassigned_Freight to Assigned_Freight.
     * Also moves wagons from Parked_Wagon to Assigned_Wagon for the planned train.
     * 
     * Note: In USBD31 (move-based approach), freight is linked to routes indirectly:
     * Freight -> Assigned_Freight -> Planned_Train -> Route
     * Wagons are managed through Assigned_Wagon/Parked_Wagon tables (schedule-based assignments)
     * 
     * This method uses PL/SQL function ASSIGN_FREIGHT_TO_ROUTE to access the database.
     * 
     * @param freightId the ID of the freight to assign
     * @param routeId the ID of the route to assign the freight to
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if the route doesn't have a Planned_Train entry or freight has no unassigned wagons
     */
    public void assignFreightToRoute(int freightId, int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL ASSIGN_FREIGHT_TO_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.setInt(2, freightId);
            stmt.setInt(3, routeId);
            stmt.execute();
            
            // Check return value (should be 1 on success)
            int result = stmt.getInt(1);
            if (result != 1) {
                throw new SQLException("Failed to assign freight to route. Return value: " + result);
            }
        } catch (SQLException e) {
            // Convert Oracle error codes to more descriptive exceptions
            if (e.getErrorCode() == 20001) {
                throw new IllegalArgumentException(e.getMessage(), e);
            } else if (e.getErrorCode() == 20002) {
                throw new IllegalArgumentException(e.getMessage(), e);
            } else if (e.getErrorCode() == 20003) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get freight by ID.
     * Based on USBD31: Freight table doesn't have RouteID column.
     * 
     * This method uses PL/SQL function GET_FREIGHT_BY_ID to access the database.
     */
    public Freight getById(int freightId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_FREIGHT_BY_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, freightId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                if (rs.next()) {
                    Facility origin = facilityRepository.getById(rs.getInt("OriginFacilityID"));
                    Facility destination = facilityRepository.getById(rs.getInt("DestinationFacilityID"));
                    
                    // In USBD31, we need to find route through junction tables
                    // For now, return routeId as 0 (unassigned)
                    // To get actual route, use getByRouteId and check if freight is in result
                    return new Freight(
                        rs.getInt("ID"),
                        0, // RouteID not directly stored in USBD31
                        origin,
                        destination
                    );
                }
            }
        }
        return null;
    }
}

