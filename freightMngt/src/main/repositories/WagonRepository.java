package main.repositories;

import main.domain.Wagon;
import main.domain.WagonForAssembly;
import main.domain.WagonSpecs;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.OracleTypes;

/**
 * Repository for Wagon entities
 */
public class WagonRepository {
    private final Connection connection;

    public WagonRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get all wagons with their specifications
     * Uses PL/SQL function GET_ALL_WAGONS (USLP09).
     */
    public List<Wagon> getAll() throws SQLException {
        List<Wagon> wagons = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_WAGONS()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    WagonSpecs specs = new WagonSpecs(
                        rs.getInt("VehicleModelID"),
                        rs.getInt("WagonTypeID"),
                        rs.getDouble("volumeCapacity"),
                        rs.getDouble("payload")
                    );
                    Wagon wagon = new Wagon(
                        rs.getInt("ID"),
                        rs.getInt("VehicleModelID"),
                        rs.getInt("TrainOperatorID"),
                        specs,
                        rs.getDouble("tare")
                    );
                    // For getAll(), wagons have no train context, so they cannot be considered loaded
                    wagon.setLoaded(false);
                    wagons.add(wagon);
                }
            }
        }
        return wagons;
    }

    /**
     * Get wagons for a specific train (for a specific planned trip via routeId)
     * Uses PL/SQL function GET_WAGONS_BY_ROUTE_ID (USLP09/USLP10).
     */
    public List<Wagon> getByRouteId(int routeId) throws SQLException {
        List<Wagon> wagons = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_WAGONS_BY_ROUTE_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    WagonSpecs specs = new WagonSpecs(
                        rs.getInt("VehicleModelID"),
                        rs.getInt("WagonTypeID"),
                        rs.getDouble("volumeCapacity"),
                        rs.getDouble("payload")
                    );
                    Wagon wagon = new Wagon(
                        rs.getInt("ID"),
                        rs.getInt("VehicleModelID"),
                        rs.getInt("TrainOperatorID"),
                        specs,
                        rs.getDouble("tare")
                    );
                    // For getByRouteId(), we don't have specific train/startDate context,
                    // so we cannot determine if wagon is loaded for a specific train
                    wagon.setLoaded(false);
                    wagons.add(wagon);
                }
            }
        }
        return wagons;
    }

    /**
     * Get wagons for a specific train (for a specific planned trip via trainId and startDate)
     * Uses PL/SQL function GET_WAGONS_BY_TRAIN_ID (USLP09/USLP10).
     */
    public List<Wagon> getByTrainId(int trainId, java.sql.Timestamp startDate) throws SQLException {
        List<Wagon> wagons = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_WAGONS_BY_TRAIN_ID(?, ?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, trainId);
            stmt.setTimestamp(3, startDate);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    WagonSpecs specs = new WagonSpecs(
                        rs.getInt("VehicleModelID"),
                        rs.getInt("WagonTypeID"),
                        rs.getDouble("volumeCapacity"),
                        rs.getDouble("payload")
                    );
                    Wagon wagon = new Wagon(
                        rs.getInt("ID"),
                        rs.getInt("VehicleModelID"),
                        rs.getInt("TrainOperatorID"),
                        specs,
                        rs.getDouble("tare")
                    );
                    wagon.setLoaded(isWagonLoaded(rs.getInt("ID"), trainId, startDate));
                    wagons.add(wagon);
                }
            }
        }
        return wagons;
    }

    /**
     * Check if a wagon is loaded (has freight assigned where freight origin matches route start)
     * Uses PL/SQL function IS_WAGON_LOADED (USLP09).
     * A wagon is loaded if it has freight assigned to the train where the freight's origin facility
     * matches the route's start facility.
     */
    private boolean isWagonLoaded(int wagonId, int trainId, java.sql.Timestamp startDate) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL IS_WAGON_LOADED(?, ?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, wagonId);
            stmt.setInt(3, trainId);
            stmt.setTimestamp(4, startDate);
            stmt.execute();
            
            int result = stmt.getInt(1);
            return result > 0;
        }
    }

    /**
     * Get wagons available for assembly for a specific route at a specific date/time.
     * Returns wagons with their status (in-transit or parked) and location information.
     * Uses PL/SQL function GET_WAGONS_FOR_ASSEMBLY (USLP09).
     * 
     * @param routeId the route ID for which to get available wagons
     * @param requestedStartDate the requested start date/time for the planned train
     * @return list of WagonForAssembly objects, ordered by status (in-transit first) and distance
     * @throws SQLException if there is a database error
     */
    public List<WagonForAssembly> getForAssembly(int routeId, java.sql.Timestamp requestedStartDate) throws SQLException {
        List<WagonForAssembly> wagons = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_WAGONS_FOR_ASSEMBLY(?, ?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, routeId);
            stmt.setTimestamp(3, requestedStartDate);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    WagonSpecs specs = new WagonSpecs(
                        rs.getInt("VehicleModelID"),
                        rs.getInt("WagonTypeID"),
                        rs.getDouble("volumeCapacity"),
                        rs.getDouble("payload")
                    );
                    
                    Wagon wagon = new Wagon(
                        rs.getInt("ID"),
                        rs.getInt("VehicleModelID"),
                        rs.getInt("TrainOperatorID"),
                        specs,
                        rs.getDouble("tare")
                    );
                    
                    // For getForAssembly(), wagons are available for assembly, not yet assigned to a train,
                    // so they cannot be considered loaded for a specific train
                    wagon.setLoaded(false);
                    
                    // Check if in-transit or parked
                    Integer routeIdValue = rs.getObject("RouteID") != null ? rs.getInt("RouteID") : null;
                    boolean inTransit = routeIdValue != null;
                    
                    Integer destinationFacilityId = rs.getObject("DestinationFacilityID") != null ? rs.getInt("DestinationFacilityID") : null;
                    String destinationFacilityName = rs.getString("DestinationFacilityName");
                    
                    Integer parkedFacilityId = rs.getInt("InitialFacilityID");
                    String parkedFacilityName = rs.getString("ParkedFacilityName");
                    
                    // Get distance (may be null for in-transit wagons)
                    Double distanceFromStartKm = rs.getObject("DistanceFromStartKm") != null ? rs.getDouble("DistanceFromStartKm") : null;
                    
                    wagons.add(new WagonForAssembly(
                        wagon,
                        inTransit,
                        routeIdValue,
                        destinationFacilityId,
                        destinationFacilityName,
                        parkedFacilityId,
                        parkedFacilityName,
                        distanceFromStartKm
                    ));
                }
            }
        }
        return wagons;
    }
}

