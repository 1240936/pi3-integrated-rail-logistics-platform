package main.repositories;

import main.domain.Locomotive;
import main.domain.Train;
import main.domain.Wagon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
     * Get train by ID with all locomotives and wagons
     */
    public Train getById(int trainId) throws SQLException {
        String sql = "SELECT ID, TrainOperatorID FROM Train WHERE ID = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, trainId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Train train = new Train(
                        rs.getInt("ID"),
                        rs.getInt("TrainOperatorID")
                    );
                    
                    // Load locomotives
                    List<Locomotive> locomotives = locomotiveRepository.getByTrainId(trainId);
                    for (Locomotive loco : locomotives) {
                        train.addLocomotive(loco);
                    }
                    
                    // Load wagons
                    List<Wagon> wagons = wagonRepository.getByTrainId(trainId);
                    for (Wagon wagon : wagons) {
                        train.addWagon(wagon);
                    }
                    
                    return train;
                }
            }
        }
        return null;
    }

    /**
     * Get all trains
     */
    public List<Train> getAll() throws SQLException {
        String sql = "SELECT ID, TrainOperatorID FROM Train ORDER BY ID";
        java.util.List<Train> trains = new java.util.ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int trainId = rs.getInt("ID");
                Train train = getById(trainId);
                if (train != null) {
                    trains.add(train);
                }
            }
        }
        return trains;
    }
}

