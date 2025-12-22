package main.repositories;

import main.domain.Locomotive;
import main.domain.Train;
import main.domain.Wagon;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
     * Get all trains
     * Uses PL/SQL function GET_ALL_TRAINS (USLP09/USLP10).
     */
    public List<Train> getAll() throws SQLException {
        java.util.List<Train> trains = new java.util.ArrayList<>();
        
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
        }
        return trains;
    }
}

