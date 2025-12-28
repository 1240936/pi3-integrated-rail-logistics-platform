package main.repositories;

import main.domain.Locomotive;
import main.domain.Train;
import main.domain.Wagon;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import oracle.jdbc.OracleTypes;

/**
 * Repository for Train entities
 */
public class TrainRepository {
    private final Connection connection;
    private final LocomotiveRepository locomotiveRepository;
    private final WagonRepository wagonRepository;

    public TrainRepository(Connection connection, 
                          LocomotiveRepository locomotiveRepository,
                          WagonRepository wagonRepository) {
        this.connection = connection;
        this.locomotiveRepository = locomotiveRepository;
        this.wagonRepository = wagonRepository;
    }

    /**
     * Get train by ID (without locomotives and wagons - use getTrainForRoute for a specific planned trip)
     * Uses PL/SQL function GET_TRAIN_BY_ID (USLP09/USLP10).
     */
    public Train getById(int trainId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_TRAIN_BY_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, trainId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                if (rs.next()) {
                    return new Train(
                        rs.getInt("ID"),
                        rs.getInt("TrainOperatorID")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Get train for a specific route (planned trip) with all locomotives and wagons for that trip
     * Uses PL/SQL functions GET_TRAIN_ID_BY_ROUTE_ID, GET_LOCOMOTIVES_BY_ROUTE_ID, GET_WAGONS_BY_ROUTE_ID (USLP09/USLP10).
     */
    public Train getTrainForRoute(int routeId) throws SQLException {
        // Get the train ID from Planned_Train using PL/SQL function
        Integer trainId = null;
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_TRAIN_ID_BY_ROUTE_ID(?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            trainId = stmt.getObject(1) != null ? stmt.getInt(1) : null;
            if (trainId == null) {
                return null; // No planned train for this route
            }
        }
        
        // Get the train
        Train train = getById(trainId);
        if (train == null) {
            return null;
        }
        
        // Load locomotives for this route (uses PL/SQL function)
        List<Locomotive> locomotives = locomotiveRepository.getByRouteId(routeId);
        for (Locomotive loco : locomotives) {
            train.addLocomotive(loco);
        }
        
        // Load wagons for this route (uses PL/SQL function)
        List<Wagon> wagons = wagonRepository.getByRouteId(routeId);
        for (Wagon wagon : wagons) {
            train.addWagon(wagon);
        }
        
        return train;
    }

    /**
     * Get train for a specific planned train (route and start date combination) with all locomotives and wagons.
     * 
     * @param routeId the route ID
     * @param startDate the start date/time for the planned train
     * @return the Train object with locomotives and wagons, or null if no train is assigned
     * @throws SQLException if there is a database error
     */
    public Train getTrainForRoute(int routeId, LocalDateTime startDate) throws SQLException {
        // Get all planned trains to find the one matching routeId and startDate
        // We need to query Planned_Train to get trainId
        Integer trainId = null;
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(
                "SELECT TrainID FROM Planned_Train WHERE RouteID = ? AND startDate = ?")) {
            stmt.setInt(1, routeId);
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(startDate));
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    trainId = rs.getInt("TrainID");
                } else {
                    return null; // No planned train found for this route and start date
                }
            }
        }
        
        // Get the train
        Train train = getById(trainId);
        if (train == null) {
            return null;
        }
        
        // Load locomotives for this planned train (uses PL/SQL function with trainId and startDate)
        List<Locomotive> locomotives = locomotiveRepository.getByTrainId(trainId, java.sql.Timestamp.valueOf(startDate));
        for (Locomotive loco : locomotives) {
            train.addLocomotive(loco);
        }
        
        // Load wagons for this planned train (uses PL/SQL function with trainId and startDate)
        List<Wagon> wagons = wagonRepository.getByTrainId(trainId, java.sql.Timestamp.valueOf(startDate));
        for (Wagon wagon : wagons) {
            train.addWagon(wagon);
        }
        
        return train;
    }

    /**
     * Get all trains
     * Uses PL/SQL function GET_ALL_TRAINS (USLP09/USLP10).
     * Falls back to direct SQL if PL/SQL function is not available.
     */
    public List<Train> getAll() throws SQLException {
        java.util.List<Train> trains = new java.util.ArrayList<>();
        
        // Try PL/SQL function first
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_TRAINS()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    Train train = new Train(
                        rs.getInt("ID"),
                        rs.getInt("TrainOperatorID")
                    );
                    trains.add(train);
                }
            }
        } catch (SQLException e) {
            // If PL/SQL function fails, fall back to direct SQL query
            // This handles cases where the function doesn't exist or isn't accessible
            try (java.sql.PreparedStatement stmt = connection.prepareStatement(
                    "SELECT ID, TrainOperatorID FROM Train ORDER BY ID")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Train train = new Train(
                            rs.getInt("ID"),
                            rs.getInt("TrainOperatorID")
                        );
                        trains.add(train);
                    }
                }
            }
        }
        
        return trains;
    }
}

