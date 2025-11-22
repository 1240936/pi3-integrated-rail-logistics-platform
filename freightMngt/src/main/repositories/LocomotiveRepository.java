package main.repositories;

import main.domain.Locomotive;
import main.domain.LocomotiveSpecs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for Locomotive entities
 */
public class LocomotiveRepository {
    private final Connection connection;

    public LocomotiveRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get all locomotives with their specifications
     */
    public List<Locomotive> getAll() throws SQLException {
        String sql = "SELECT l.ID, l.VehicleModelID, l.TrainOperatorID, " +
                     "ls.make, ls.power, ls.acceleration, ls.maxSpeed, ls.numberOfWheels " +
                     "FROM Locomotive l " +
                     "JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID";
        List<Locomotive> locomotives = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                LocomotiveSpecs specs = new LocomotiveSpecs(
                    rs.getInt("VehicleModelID"),
                    rs.getString("make"),
                    rs.getDouble("power"),
                    rs.getObject("acceleration", Double.class),
                    rs.getDouble("maxSpeed"),
                    rs.getInt("numberOfWheels")
                );
                locomotives.add(new Locomotive(
                    rs.getInt("ID"),
                    rs.getInt("VehicleModelID"),
                    rs.getInt("TrainOperatorID"),
                    specs
                ));
            }
        }
        return locomotives;
    }

    /**
     * Get locomotives for a specific train
     */
    public List<Locomotive> getByTrainId(int trainId) throws SQLException {
        String sql = "SELECT l.ID, l.VehicleModelID, l.TrainOperatorID, " +
                     "ls.make, ls.power, ls.acceleration, ls.maxSpeed, ls.numberOfWheels " +
                     "FROM Locomotive l " +
                     "JOIN LocomotiveSpecs ls ON l.VehicleModelID = ls.VehicleModelID " +
                     "JOIN Locomotive_Train lt ON l.ID = lt.LocomotiveID " +
                     "WHERE lt.TrainID = ?";
        List<Locomotive> locomotives = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, trainId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocomotiveSpecs specs = new LocomotiveSpecs(
                        rs.getInt("VehicleModelID"),
                        rs.getString("make"),
                        rs.getDouble("power"),
                        rs.getObject("acceleration", Double.class),
                        rs.getDouble("maxSpeed"),
                        rs.getInt("numberOfWheels")
                    );
                    locomotives.add(new Locomotive(
                        rs.getInt("ID"),
                        rs.getInt("VehicleModelID"),
                        rs.getInt("TrainOperatorID"),
                        specs
                    ));
                }
            }
        }
        return locomotives;
    }
}

