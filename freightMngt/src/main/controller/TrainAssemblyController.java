package main.controller;

import main.domain.LocomotiveForAssembly;
import main.domain.Route;
import main.domain.Train;
import main.domain.WagonForAssembly;
import main.repositories.LocomotiveRepository;
import main.repositories.RouteRepository;
import main.repositories.TrainRepository;
import main.repositories.WagonRepository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for train assembly operations.
 * Handles assembling trains by selecting and assigning locomotives and wagons to routes.
 */
public class TrainAssemblyController {
    private final Connection connection;
    private final RouteRepository routeRepository;
    private final TrainRepository trainRepository;
    private final LocomotiveRepository locomotiveRepository;
    private final WagonRepository wagonRepository;

    public TrainAssemblyController(Connection connection,
                                   RouteRepository routeRepository,
                                   TrainRepository trainRepository,
                                   LocomotiveRepository locomotiveRepository,
                                   WagonRepository wagonRepository) {
        this.connection = connection;
        this.routeRepository = routeRepository;
        this.trainRepository = trainRepository;
        this.locomotiveRepository = locomotiveRepository;
        this.wagonRepository = wagonRepository;
    }

    /**
     * Get locomotives available for assembly for a specific route.
     * The list distinguishes between locomotives in-transit and parked,
     * and orders parked locomotives by distance from the route start (descending).
     * 
     * @param routeId the route ID
     * @return list of LocomotiveForAssembly objects
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if the route does not exist
     */
    public List<LocomotiveForAssembly> getAvailableLocomotives(int routeId) throws SQLException {
        // Validate route exists
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Route with ID " + routeId + " does not exist.");
        }

        return locomotiveRepository.getForAssembly(routeId);
    }

    /**
     * Get wagons available for assembly for a specific route.
     * The list distinguishes between wagons in-transit and parked,
     * and orders parked wagons by distance from the route start (descending).
     * 
     * @param routeId the route ID
     * @return list of WagonForAssembly objects
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if the route does not exist
     */
    public List<WagonForAssembly> getAvailableWagons(int routeId) throws SQLException {
        // Validate route exists
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Route with ID " + routeId + " does not exist.");
        }

        return wagonRepository.getForAssembly(routeId);
    }

    /**
     * Assign a locomotive to a route (planned train).
     * Uses PL/SQL function ASSIGN_LOCOMOTIVE_TO_ROUTE (USLP09).
     * 
     * @param locomotiveId the locomotive ID to assign
     * @param routeId the route ID
     * @return the train ID that the locomotive was assigned to
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails
     */
    public int assignLocomotiveToRoute(int locomotiveId, int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL ASSIGN_LOCOMOTIVE_TO_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, locomotiveId);
            stmt.setInt(3, routeId);
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Assign a wagon to a route (planned train).
     * Uses PL/SQL function ASSIGN_WAGON_TO_ROUTE (USLP09).
     * 
     * @param wagonId the wagon ID to assign
     * @param routeId the route ID
     * @return the train ID that the wagon was assigned to
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails
     */
    public int assignWagonToRoute(int wagonId, int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL ASSIGN_WAGON_TO_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, wagonId);
            stmt.setInt(3, routeId);
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Remove a locomotive from a route (planned train).
     * Uses PL/SQL function REMOVE_LOCOMOTIVE_FROM_ROUTE (USLP09).
     * 
     * @param locomotiveId the locomotive ID to remove
     * @param routeId the route ID
     * @return the train ID that the locomotive was removed from
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails
     */
    public int removeLocomotiveFromRoute(int locomotiveId, int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL REMOVE_LOCOMOTIVE_FROM_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, locomotiveId);
            stmt.setInt(3, routeId);
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Remove a wagon from a route (planned train).
     * Uses PL/SQL function REMOVE_WAGON_FROM_ROUTE (USLP09).
     * 
     * @param wagonId the wagon ID to remove
     * @param routeId the route ID
     * @return the train ID that the wagon was removed from
     * @throws SQLException if there is a database error
     * @throws IllegalArgumentException if validation fails
     */
    public int removeWagonFromRoute(int wagonId, int routeId) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall("{? = CALL REMOVE_WAGON_FROM_ROUTE(?, ?)}")) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, wagonId);
            stmt.setInt(3, routeId);
            stmt.execute();
            
            int trainId = stmt.getInt(1);
            return trainId;
        } catch (SQLException e) {
            // Re-throw with more context if it's an application error
            if (e.getErrorCode() >= 20000 && e.getErrorCode() < 30000) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Get the train assigned to a route with all its locomotives and wagons.
     * 
     * @param routeId the route ID
     * @return the Train object with locomotives and wagons, or null if no train is assigned
     * @throws SQLException if there is a database error
     */
    public Train getTrainForRoute(int routeId) throws SQLException {
        return trainRepository.getTrainForRoute(routeId);
    }

    /**
     * Get all available routes.
     * 
     * @return list of all Route objects in the system
     * @throws SQLException if there is a database error
     */
    public List<Route> getAllRoutes() throws SQLException {
        return routeRepository.getAll();
    }
}

