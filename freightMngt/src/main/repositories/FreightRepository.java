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
     * Get all freight for a specific route
     */
    public List<Freight> getByRouteId(int routeId) throws SQLException {
        String sql = "SELECT ID, RouteID, OriginFacilityID, DestinationFacilityID " +
                     "FROM Freight WHERE RouteID = ?";
        List<Freight> freightList = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Facility origin = facilityRepository.getById(rs.getInt("OriginFacilityID"));
                    Facility destination = facilityRepository.getById(rs.getInt("DestinationFacilityID"));
                    
                    Freight freight = new Freight(
                        rs.getInt("ID"),
                        rs.getInt("RouteID"),
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
     * Get map of facility ID to list of freight pickups (freight that originates at this facility)
     */
    public Map<Integer, List<Freight>> getPickupsByFacility(int routeId) throws SQLException {
        String sql = "SELECT ID, RouteID, OriginFacilityID, DestinationFacilityID " +
                     "FROM Freight WHERE RouteID = ?";
        Map<Integer, List<Freight>> pickups = new HashMap<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int originId = rs.getInt("OriginFacilityID");
                    Facility origin = facilityRepository.getById(originId);
                    Facility destination = facilityRepository.getById(rs.getInt("DestinationFacilityID"));
                    
                    Freight freight = new Freight(
                        rs.getInt("ID"),
                        rs.getInt("RouteID"),
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
     * Get map of facility ID to list of freight deliveries (freight that ends at this facility)
     */
    public Map<Integer, List<Freight>> getDeliveriesByFacility(int routeId) throws SQLException {
        String sql = "SELECT ID, RouteID, OriginFacilityID, DestinationFacilityID " +
                     "FROM Freight WHERE RouteID = ?";
        Map<Integer, List<Freight>> deliveries = new HashMap<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int destinationId = rs.getInt("DestinationFacilityID");
                    Facility origin = facilityRepository.getById(rs.getInt("OriginFacilityID"));
                    Facility destination = facilityRepository.getById(destinationId);
                    
                    Freight freight = new Freight(
                        rs.getInt("ID"),
                        rs.getInt("RouteID"),
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
     * Get all freight that is not yet assigned to a route (RouteID IS NULL)
     */
    public List<Freight> getUnassignedFreight() throws SQLException {
        String sql = "SELECT ID, RouteID, OriginFacilityID, DestinationFacilityID " +
                     "FROM Freight WHERE RouteID IS NULL";
        List<Freight> freightList = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Facility origin = facilityRepository.getById(rs.getInt("OriginFacilityID"));
                Facility destination = facilityRepository.getById(rs.getInt("DestinationFacilityID"));
                
                // RouteID is NULL for unassigned freight, which getInt() returns as 0
                Freight freight = new Freight(
                    rs.getInt("ID"),
                    0, // RouteID is NULL (unassigned)
                    origin,
                    destination
                );
                freightList.add(freight);
            }
        }
        return freightList;
    }

    /**
     * Assign freight to a route
     */
    public void assignFreightToRoute(int freightId, int routeId) throws SQLException {
        String sql = "UPDATE Freight SET RouteID = ? WHERE ID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            stmt.setInt(2, freightId);
            stmt.executeUpdate();
        }
    }

    /**
     * Get freight by ID
     */
    public Freight getById(int freightId) throws SQLException {
        String sql = "SELECT ID, RouteID, OriginFacilityID, DestinationFacilityID " +
                     "FROM Freight WHERE ID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, freightId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Facility origin = facilityRepository.getById(rs.getInt("OriginFacilityID"));
                    Facility destination = facilityRepository.getById(rs.getInt("DestinationFacilityID"));
                    Integer routeId = rs.getObject("RouteID") != null ? rs.getInt("RouteID") : null;
                    
                    return new Freight(
                        rs.getInt("ID"),
                        routeId != null ? routeId : 0,
                        origin,
                        destination
                    );
                }
            }
        }
        return null;
    }
}

