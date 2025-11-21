package main.repositories;

import main.domain.Wagon;
import main.domain.WagonSpecs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     */
    public List<Wagon> getAll() throws SQLException {
        String sql = "SELECT w.ID, w.VehicleModelID, w.TrainOperatorID, " +
                     "ws.WagonTypeID, ws.volumeCapacity, ws.payload, vm.tare " +
                     "FROM Wagon w " +
                     "JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID " +
                     "JOIN VehicleModel vm ON w.VehicleModelID = vm.ID";
        List<Wagon> wagons = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
                // Check if wagon is loaded (has freight assigned)
                wagon.setLoaded(isWagonLoaded(rs.getInt("ID")));
                wagons.add(wagon);
            }
        }
        return wagons;
    }

    /**
     * Get wagons for a specific train
     */
    public List<Wagon> getByTrainId(int trainId) throws SQLException {
        String sql = "SELECT w.ID, w.VehicleModelID, w.TrainOperatorID, " +
                     "ws.WagonTypeID, ws.volumeCapacity, ws.payload, vm.tare " +
                     "FROM Wagon w " +
                     "JOIN WagonSpecs ws ON w.VehicleModelID = ws.VehicleModelID " +
                     "JOIN VehicleModel vm ON w.VehicleModelID = vm.ID " +
                     "JOIN Wagon_Train wt ON w.ID = wt.WagonID " +
                     "WHERE wt.TrainID = ?";
        List<Wagon> wagons = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, trainId);
            try (ResultSet rs = stmt.executeQuery()) {
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
                    wagon.setLoaded(isWagonLoaded(rs.getInt("ID")));
                    wagons.add(wagon);
                }
            }
        }
        return wagons;
    }

    /**
     * Check if a wagon is loaded (has freight assigned)
     */
    private boolean isWagonLoaded(int wagonId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM Freight_Wagon WHERE WagonID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, wagonId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }

    /**
     * Get loaded wagons (wagons with freight assigned)
     */
    public Set<Integer> getLoadedWagonIds() throws SQLException {
        String sql = "SELECT DISTINCT WagonID FROM Freight_Wagon";
        Set<Integer> loadedIds = new HashSet<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                loadedIds.add(rs.getInt("WagonID"));
            }
        }
        return loadedIds;
    }
}

