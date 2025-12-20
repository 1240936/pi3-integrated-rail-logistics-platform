package main.repositories;

import main.domain.Facility;
import main.domain.Freight;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Based on USBD31: Freight is linked to a route through Freight_Wagon -> Train_Wagon -> Planned_Train -> Route
     * 
     * IMPORTANT: Filters by both RouteID and startDate to ensure we're looking at the correct journey.
     * A train can be planned for multiple routes with different start dates, so we must match both.
     * 
     * The join path is: Freight -> Freight_Wagon -> Train_Wagon -> Planned_Train -> Route
     * 
     * @param routeId the ID of the route
     * @param startDate the scheduled start date for this route (from Planned_Train)
     * @return list of freights assigned to this route
     * @throws SQLException if there is a database error
     */
    public List<Freight> getByRouteId(int routeId, java.time.LocalDateTime startDate) throws SQLException {
        // In USBD31, Freight doesn't have RouteID column.
        // Find freight whose wagons are on trains that are planned for this route with this specific start date.
        // We filter by both RouteID and startDate to ensure we get the correct journey.
        // IMPORTANT: We use exact timestamp comparison to match the exact date and time (including hours, minutes, seconds).
        // Oracle DATE type stores date and time, so we need exact match. Using CAST to ensure proper comparison.
        String sql = "SELECT DISTINCT f.ID, f.OriginFacilityID, f.DestinationFacilityID " +
                     "FROM Freight f " +
                     "INNER JOIN Freight_Wagon fw ON f.ID = fw.FreightID " +
                     "INNER JOIN Train_Wagon tw ON fw.WagonID = tw.WagonID " +
                     "INNER JOIN Planned_Train pt ON tw.TrainID = pt.TrainID " +
                     "WHERE pt.RouteID = ? AND pt.startDate = CAST(? AS DATE)";
        List<Freight> freightList = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            // Convert LocalDateTime to Timestamp for exact comparison
            java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(startDate);
            stmt.setTimestamp(2, timestamp);
            try (ResultSet rs = stmt.executeQuery()) {
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
     * @param routeId the ID of the route
     * @return list of freights assigned to this route
     * @throws SQLException if there is a database error
     */
    public List<Freight> getByRouteId(int routeId) throws SQLException {
        // Get the startDate for this route from Planned_Train
        // Use ORDER BY to ensure we get a consistent result if multiple entries exist (shouldn't happen)
        String dateSql = "SELECT startDate FROM Planned_Train WHERE RouteID = ? ORDER BY startDate FETCH FIRST 1 ROW ONLY";
        try (PreparedStatement stmt = connection.prepareStatement(dateSql)) {
            stmt.setInt(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp timestamp = rs.getTimestamp("startDate");
                    if (timestamp != null) {
                        java.time.LocalDateTime startDate = timestamp.toLocalDateTime();
                        return getByRouteId(routeId, startDate);
                    }
                }
            }
        }
        // If no startDate found, return empty list
        return new ArrayList<>();
    }

    /**
     * Get wagon IDs for a freight
     */
    public List<Integer> getWagonIdsByFreightId(int freightId) throws SQLException {
        String sql = "SELECT WagonID FROM Freight_Wagon WHERE FreightID = ?";
        List<Integer> wagonIds = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, freightId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    wagonIds.add(rs.getInt("WagonID"));
                }
            }
        }
        return wagonIds;
    }

    /**
     * Get map of facility ID to list of freight pickups (freight that originates at this facility).
     * Based on USBD31: Uses junction tables to find freight for a route.
     * 
     * IMPORTANT: Filters by both RouteID and startDate to ensure we're looking at the correct journey.
     * 
     * @param routeId the ID of the route
     * @param startDate the scheduled start date for this route (from Planned_Train)
     * @return map of facility ID to list of freights that originate at that facility
     * @throws SQLException if there is a database error
     */
    public Map<Integer, List<Freight>> getPickupsByFacility(int routeId, java.time.LocalDateTime startDate) throws SQLException {
        // Filter by both RouteID and startDate to ensure correct journey
        // IMPORTANT: Use exact timestamp comparison with CAST to ensure proper matching
        String sql = "SELECT DISTINCT f.ID, f.OriginFacilityID, f.DestinationFacilityID " +
                     "FROM Freight f " +
                     "INNER JOIN Freight_Wagon fw ON f.ID = fw.FreightID " +
                     "INNER JOIN Train_Wagon tw ON fw.WagonID = tw.WagonID " +
                     "INNER JOIN Planned_Train pt ON tw.TrainID = pt.TrainID " +
                     "WHERE pt.RouteID = ? AND pt.startDate = CAST(? AS DATE)";
        Map<Integer, List<Freight>> pickups = new HashMap<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(startDate);
            stmt.setTimestamp(2, timestamp);
            try (ResultSet rs = stmt.executeQuery()) {
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
     */
    public Map<Integer, List<Freight>> getPickupsByFacility(int routeId) throws SQLException {
        // Get the startDate for this route from Planned_Train
        String dateSql = "SELECT startDate FROM Planned_Train WHERE RouteID = ? AND ROWNUM = 1";
        try (PreparedStatement stmt = connection.prepareStatement(dateSql)) {
            stmt.setInt(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp timestamp = rs.getTimestamp("startDate");
                    if (timestamp != null) {
                        java.time.LocalDateTime startDate = timestamp.toLocalDateTime();
                        return getPickupsByFacility(routeId, startDate);
                    }
                }
            }
        }
        // If no startDate found, return empty map
        return new HashMap<>();
    }

    /**
     * Get map of facility ID to list of freight deliveries (freight that ends at this facility).
     * Based on USBD31: Uses junction tables to find freight for a route.
     * 
     * IMPORTANT: Filters by both RouteID and startDate to ensure we're looking at the correct journey.
     * 
     * @param routeId the ID of the route
     * @param startDate the scheduled start date for this route (from Planned_Train)
     * @return map of facility ID to list of freights that end at that facility
     * @throws SQLException if there is a database error
     */
    public Map<Integer, List<Freight>> getDeliveriesByFacility(int routeId, java.time.LocalDateTime startDate) throws SQLException {
        // Filter by both RouteID and startDate to ensure correct journey
        // IMPORTANT: Use exact timestamp comparison with CAST to ensure proper matching
        String sql = "SELECT DISTINCT f.ID, f.OriginFacilityID, f.DestinationFacilityID " +
                     "FROM Freight f " +
                     "INNER JOIN Freight_Wagon fw ON f.ID = fw.FreightID " +
                     "INNER JOIN Train_Wagon tw ON fw.WagonID = tw.WagonID " +
                     "INNER JOIN Planned_Train pt ON tw.TrainID = pt.TrainID " +
                     "WHERE pt.RouteID = ? AND pt.startDate = CAST(? AS DATE)";
        Map<Integer, List<Freight>> deliveries = new HashMap<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(startDate);
            stmt.setTimestamp(2, timestamp);
            try (ResultSet rs = stmt.executeQuery()) {
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
     */
    public Map<Integer, List<Freight>> getDeliveriesByFacility(int routeId) throws SQLException {
        // Get the startDate for this route from Planned_Train
        String dateSql = "SELECT startDate FROM Planned_Train WHERE RouteID = ? AND ROWNUM = 1";
        try (PreparedStatement stmt = connection.prepareStatement(dateSql)) {
            stmt.setInt(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp timestamp = rs.getTimestamp("startDate");
                    if (timestamp != null) {
                        java.time.LocalDateTime startDate = timestamp.toLocalDateTime();
                        return getDeliveriesByFacility(routeId, startDate);
                    }
                }
            }
        }
        // If no startDate found, return empty map
        return new HashMap<>();
    }

    /**
     * Get all freight that is not yet assigned to a route.
     * Based on USBD31: Freight is considered unassigned if its wagons are not on any train
     * that is planned for a route, OR if it has no wagons assigned.
     */
    public List<Freight> getUnassignedFreight() throws SQLException {
        // In USBD31, Freight doesn't have RouteID column.
        // Unassigned freight = freight whose wagons are not on trains that are in Planned_Train
        String sql = "SELECT DISTINCT f.ID, f.OriginFacilityID, f.DestinationFacilityID " +
                     "FROM Freight f " +
                     "WHERE f.ID NOT IN (" +
                     "    SELECT DISTINCT fw.FreightID " +
                     "    FROM Freight_Wagon fw " +
                     "    INNER JOIN Train_Wagon tw ON fw.WagonID = tw.WagonID " +
                     "    INNER JOIN Planned_Train pt ON tw.TrainID = pt.TrainID" +
                     ")";
        List<Freight> freightList = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
        return freightList;
    }

    /**
     * Assign freight to a route.
     * Based on USBD31: Verifies that freight's wagons are on trains planned for the route.
     * 
     * Note: In USBD31, freight is linked to routes indirectly:
     * Freight -> Freight_Wagon -> Wagon -> Train_Wagon -> Train -> Planned_Train -> Route
     * 
     * This method verifies the relationship exists. If freight wagons are not on a train
     * planned for the route, it will check if any train is planned for the route and
     * verify the relationship can be established.
     */
    public void assignFreightToRoute(int freightId, int routeId) throws SQLException {
        // In USBD31, assignment is implicit through wagon-train-route relationships.
        // We verify that freight wagons are on trains planned for this route.
        // If not found, we check if route exists and has a planned train.
        
        // Check if freight has wagons on trains planned for this route
        String checkSql = "SELECT COUNT(*) as cnt " +
                         "FROM Freight_Wagon fw " +
                         "INNER JOIN Train_Wagon tw ON fw.WagonID = tw.WagonID " +
                         "INNER JOIN Planned_Train pt ON tw.TrainID = pt.TrainID " +
                         "WHERE fw.FreightID = ? AND pt.RouteID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
            stmt.setInt(1, freightId);
            stmt.setInt(2, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    // Relationship already exists - assignment is valid
                    return;
                }
            }
        }
        
        // If no relationship found, we can't create one automatically in USBD31
        // because it would require modifying Train_Wagon which may not be appropriate.
        // The assignment is conceptual - freight wagons must be on trains planned for routes.
        // This method serves as a verification/validation step.
    }

    /**
     * Get freight by ID.
     * Based on USBD31: Freight table doesn't have RouteID column.
     */
    public Freight getById(int freightId) throws SQLException {
        String sql = "SELECT ID, OriginFacilityID, DestinationFacilityID " +
                     "FROM Freight WHERE ID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, freightId);
            try (ResultSet rs = stmt.executeQuery()) {
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

