package main.repositories;

import main.domain.Locomotive;
import main.domain.LocomotiveSpecs;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.jdbc.OracleTypes;

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
     * Uses PL/SQL function GET_ALL_LOCOMOTIVES (USLP09).
     */
    public List<Locomotive> getAll() throws SQLException {
        List<Locomotive> locomotives = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_ALL_LOCOMOTIVES()}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    LocomotiveSpecs specs = new LocomotiveSpecs(
                        rs.getInt("VehicleModelID"),
                        rs.getString("make"),
                        rs.getDouble("power"),
                        rs.getDouble("acceleration"),
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

    /**
     * Get locomotives for a specific train (for a specific planned trip via routeId)
     * Uses PL/SQL function GET_LOCOMOTIVES_BY_ROUTE_ID (USLP09/USLP10).
     */
    public List<Locomotive> getByRouteId(int routeId) throws SQLException {
        List<Locomotive> locomotives = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_LOCOMOTIVES_BY_ROUTE_ID(?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, routeId);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    LocomotiveSpecs specs = new LocomotiveSpecs(
                        rs.getInt("VehicleModelID"),
                        rs.getString("make"),
                        rs.getDouble("power"),
                        rs.getDouble("acceleration"),
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

    /**
     * Get locomotives for a specific train (for a specific planned trip via trainId and startDate)
     * Uses PL/SQL function GET_LOCOMOTIVES_BY_TRAIN_ID (USLP09/USLP10).
     */
    public List<Locomotive> getByTrainId(int trainId, java.sql.Timestamp startDate) throws SQLException {
        List<Locomotive> locomotives = new ArrayList<>();
        
        try (CallableStatement stmt = connection.prepareCall("{? = CALL GET_LOCOMOTIVES_BY_TRAIN_ID(?, ?)}")) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setInt(2, trainId);
            stmt.setTimestamp(3, startDate);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    LocomotiveSpecs specs = new LocomotiveSpecs(
                        rs.getInt("VehicleModelID"),
                        rs.getString("make"),
                        rs.getDouble("power"),
                        rs.getDouble("acceleration"),
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

