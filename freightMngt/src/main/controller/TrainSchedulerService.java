package main.controller;

import main.domain.*;
import main.repositories.*;

import java.sql.Connection;
import java.time.Duration;
import java.util.*;

/**
 * Service for train scheduling including speed calculation, crossing detection, and time estimation
 */
public class TrainSchedulerService {
    private final FacilityRepository facilityRepository;
    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final LineSegmentRepository lineSegmentRepository;
    private final RailLineRepository railLineRepository;
    private final TrainEventRepository trainEventRepository;
    private final FreightRepository freightRepository;

    public TrainSchedulerService(Connection connection) {
        this.facilityRepository = new FacilityRepository(connection);
        LocomotiveRepository locoRepo = new LocomotiveRepository(connection);
        WagonRepository wagonRepo = new WagonRepository(connection);
        this.trainRepository = new TrainRepository(connection, locoRepo, wagonRepo);
        FacilityRepository facilityRepo = new FacilityRepository(connection);
        this.routeRepository = new RouteRepository(connection, facilityRepo);
        this.lineSegmentRepository = new LineSegmentRepository(connection);
        this.railLineRepository = new RailLineRepository(connection, facilityRepo);
        this.trainEventRepository = new TrainEventRepository(connection, facilityRepo);
        this.freightRepository = new FreightRepository(connection, facilityRepo);
    }

    /**
     * Calculate the maximum speed for a train on a line segment based on:
     * - Track speed limit
     * - Locomotive power and train weight
     * - Locomotive max speed
     */
    public double calculateSpeed(Train train, LineSegment segment) {
        // Start with locomotive's max speed
        double maxLocoSpeed = train.getMaxSpeed();
        
        // Apply track speed limit if present
        double speedLimit = segment.getSpeedLimit() != null ? segment.getSpeedLimit() : Double.MAX_VALUE;
        
        // Calculate speed based on power and weight
        // Simplified formula: speed = sqrt(power / weight) * factor
        // Power-to-weight ratio approach
        double totalPower = train.getTotalPower(); // in kW
        double totalWeight = train.getTotalWeight(); // in tons
        
        if (totalWeight == 0) {
            return 0;
        }
        
        // Power-to-weight ratio (kW per ton)
        double powerToWeightRatio = totalPower / totalWeight;
        
        // Empirical formula: v = k * sqrt(P/W) where k is a conversion factor
        // For trains, typical values: k ≈ 15-20 for km/h
        double calculatedSpeed = Math.sqrt(powerToWeightRatio) * 18.0; // km/h
        
        // Take the minimum of all constraints
        return Math.min(Math.min(maxLocoSpeed, speedLimit), calculatedSpeed);
    }

    /**
     * Calculate travel time for a train on a line segment
     */
    public Duration calculateTravelTime(Train train, LineSegment segment) {
        double speed = calculateSpeed(train, segment); // km/h
        if (speed == 0) {
            return Duration.ZERO;
        }
        
        // Convert segment length from meters to kilometers
        double lengthKm = segment.getLength() / 1000.0;
        
        // Calculate time in hours
        double timeHours = lengthKm / speed;
        
        // Convert to Duration
        long seconds = Math.round(timeHours * 3600);
        return Duration.ofSeconds(seconds);
    }
}

