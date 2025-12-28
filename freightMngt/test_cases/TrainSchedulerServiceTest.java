import main.controller.TrainSchedulerService;
import main.domain.*;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.time.Duration;

import static org.junit.Assert.*;

/**
 * Comprehensive unit tests for TrainSchedulerService
 * Tests speed calculation logic, travel time calculation, and edge cases
 */
public class TrainSchedulerServiceTest {

    private Connection mockConnection;
    private TrainSchedulerService service;

    @Before
    public void setUp() {
        mockConnection = null; // Service methods tested don't require DB connection
        service = new TrainSchedulerService(mockConnection);
    }

    @Test
    public void testCalculateSpeed_PowerToWeightCalculation() {
        // Create train with known power and weight
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 100.0, 200.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        
        // Add wagons to create known weight
        WagonSpecs wagonSpecs = new WagonSpecs(1, 1, 50.0, 30.0);
        Wagon wagon = new Wagon(1, 1, 1, wagonSpecs, 20.0);
        train.addWagon(wagon);
        
        // Segment with no speed limit - speed should be based on power/weight
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);
        
        double speed = service.calculateSpeed(train, segment);
        
        // Weight: 87 (loco) + 20 (wagon) = 107 tons
        // Power: 5600 kW
        // Power/weight: 5600/107 ≈ 52.34 kW/ton
        // Calculated speed: sqrt(52.34) * 18 ≈ 7.23 * 18 ≈ 130 km/h
        // Operational speed: 100 km/h
        // Result should be min(100, 130) = 100 km/h
        assertTrue("Speed should be positive", speed > 0);
        assertTrue("Speed should not exceed operational speed", speed <= 100.0);
        assertEquals("Speed should be limited by operational speed", 100.0, speed, 0.1);
    }

    @Test
    public void testCalculateSpeed_WeightAffectsSpeed() {
        // Create train with high power locomotive
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 11200.0, 0.5, 100.0, 200.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);
        
        // Test with light train
        Train lightTrain = new Train(1, 1);
        lightTrain.addLocomotive(locomotive);
        WagonSpecs wagonSpecs = new WagonSpecs(1, 1, 50.0, 30.0);
        Wagon wagon1 = new Wagon(1, 1, 1, wagonSpecs, 20.0);
        lightTrain.addWagon(wagon1);
        
        double lightSpeed = service.calculateSpeed(lightTrain, segment);
        
        // Test with heavy train (loaded wagons)
        Train heavyTrain = new Train(2, 1);
        heavyTrain.addLocomotive(locomotive);
        Wagon wagon2 = new Wagon(2, 1, 1, wagonSpecs, 20.0);
        wagon2.setLoaded(true); // Add payload weight
        heavyTrain.addWagon(wagon2);
        
        double heavySpeed = service.calculateSpeed(heavyTrain, segment);
        
        // Heavy train should have lower speed due to higher weight
        assertTrue("Heavy train should have lower or equal speed", heavySpeed <= lightSpeed);
        assertTrue("Light speed should be positive", lightSpeed > 0);
        assertTrue("Heavy speed should be positive", heavySpeed > 0);
    }

    @Test
    public void testCalculateSpeed_TrackSpeedLimitLowerThanOperationalSpeed() {
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 100.0, 200.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        
        // Segment with speed limit lower than operational speed
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, 50.0, 1);
        
        double speed = service.calculateSpeed(train, segment);
        
        // Speed should be limited by track speed limit (50 km/h)
        assertTrue("Speed should be positive", speed > 0);
        assertTrue("Speed should not exceed track speed limit", speed <= 50.0);
        assertEquals("Speed should be limited by track speed limit", 50.0, speed, 0.1);
    }

    @Test
    public void testCalculateSpeed_MultipleLocomotives() {
        // Two locomotives with different operational speeds
        LocomotiveSpecs locoSpecs1 = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 200.0, 6);
        LocomotiveSpecs locoSpecs2 = new LocomotiveSpecs(2, "Test", 5600.0, 0.5, 60.0, 180.0, 6);
        
        Locomotive locomotive1 = new Locomotive(1, 1, 1, locoSpecs1);
        Locomotive locomotive2 = new Locomotive(2, 2, 1, locoSpecs2);
        
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive1);
        train.addLocomotive(locomotive2);
        
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);
        
        double speed = service.calculateSpeed(train, segment);
        
        // Operational speed should be minimum of both (60 km/h)
        // Power is sum of both (11200 kW)
        // But speed is limited by operational speed
        assertTrue("Speed should be positive", speed > 0);
        assertTrue("Speed should not exceed minimum operational speed", speed <= 60.0);
    }

    @Test
    public void testCalculateSpeed_ZeroWeight() {
        Train train = new Train(1, 1);
        // No locomotives, no wagons
        
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);
        
        double speed = service.calculateSpeed(train, segment);
        
        // With zero weight, speed should be 0
        assertEquals("Speed should be 0 with zero weight", 0.0, speed, 0.01);
    }

    @Test
    public void testCalculateTravelTime_StandardCalculation() {
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        
        // Segment: 5 km (5000 meters) at 70 km/h
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, 70.0, 1);
        
        Duration travelTime = service.calculateTravelTime(train, segment);
        
        // At 70 km/h, 5 km should take approximately 5/70 hours = 0.0714 hours = 257 seconds
        assertNotNull("Travel time should not be null", travelTime);
        assertTrue("Travel time should be positive", travelTime.getSeconds() > 0);
        // Should be approximately 257 seconds, but allow some tolerance for calculation differences
        assertTrue("Travel time should be reasonable", 
                   travelTime.getSeconds() >= 200 && travelTime.getSeconds() <= 350);
    }

    @Test
    public void testCalculateTravelTime_ZeroSpeed() {
        Train train = new Train(1, 1);
        // No locomotives - zero speed
        
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);
        
        Duration travelTime = service.calculateTravelTime(train, segment);
        
        // With zero speed, travel time should be zero
        assertEquals("Travel time should be zero with zero speed", Duration.ZERO, travelTime);
    }

    @Test
    public void testCalculateTravelTime_VeryLongSegment() {
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        
        // Very long segment: 100 km (100000 meters)
        LineSegment segment = new LineSegment(1, 1, 1000.0, 100000.0, 2, 70.0, 1);
        
        Duration travelTime = service.calculateTravelTime(train, segment);
        
        // At 70 km/h, 100 km should take approximately 1.43 hours = 5143 seconds
        assertNotNull("Travel time should not be null", travelTime);
        assertTrue("Travel time should be positive", travelTime.getSeconds() > 0);
        assertTrue("Travel time should be reasonable for 100km", 
                   travelTime.getSeconds() >= 5000 && travelTime.getSeconds() <= 5500);
    }

    @Test
    public void testCalculateSpeed_OperationalSpeedVsCalculatedSpeed() {
        // Low power locomotive that would calculate to lower speed than operational
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 1000.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        
        // Heavy train with many loaded wagons
        WagonSpecs wagonSpecs = new WagonSpecs(1, 1, 50.0, 30.0);
        for (int i = 0; i < 20; i++) {
            Wagon wagon = new Wagon(i, 1, 1, wagonSpecs, 25.0);
            wagon.setLoaded(true); // Each wagon: 25 + 50 = 75 tons
            train.addWagon(wagon);
        }
        // Total weight: 87 (loco) + 20*75 = 1587 tons
        // Power/weight: 1000/1587 ≈ 0.63 kW/ton
        // Calculated speed: sqrt(0.63) * 18 ≈ 0.79 * 18 ≈ 14.2 km/h
        // Operational speed: 70 km/h
        // Result should be min(70, 14.2) = 14.2 km/h (limited by calculated speed)
        
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);
        
        double speed = service.calculateSpeed(train, segment);
        
        // Speed should be limited by calculated speed, not operational speed
        assertTrue("Speed should be positive", speed > 0);
        assertTrue("Speed should be less than operational speed", speed < 70.0);
        assertTrue("Speed should be around calculated speed", speed >= 10.0 && speed <= 20.0);
    }
}

