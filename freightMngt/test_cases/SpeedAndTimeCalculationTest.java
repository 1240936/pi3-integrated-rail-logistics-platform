import main.controller.TrainSchedulerService;
import main.domain.*;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.time.Duration;

import static org.junit.Assert.*;

/**
 * Unit tests for TrainSchedulerService
 * Tests speed calculation and travel time calculation logic
 */
public class SpeedAndTimeCalculationTest {

    private Connection mockConnection;

    @Before
    public void setUp() {
        // For these tests, we focus on methods that don't require database access
        // calculateSpeed and calculateTravelTime are pure logic methods
        mockConnection = null; // Will be null for speed/time tests that don't use DB
    }

    @Test
    // Test: Calculate speed with locomotive operational speed constraint
    public void testCalculateSpeed_OperationalSpeedConstraint() {
        // Create train with one locomotive (operational speed 70 km/h, max speed 120 km/h, power 5600 kW)
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        
        // Add empty wagons (tare weight: 20 tons each)
        WagonSpecs wagonSpecs = new WagonSpecs(1, 1, 50.0, 30.0);
        Wagon wagon = new Wagon(1, 1, 1, wagonSpecs, 20.0);
        train.addWagon(wagon);

        // Segment with no speed limit (should use locomotive operational speed)
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);

        // Calculate speed - should be limited by locomotive operational speed
        double speed = new TrainSchedulerService(mockConnection).calculateSpeed(train, segment);
        
        // Speed should be limited by locomotive operational speed (70 km/h) or power/weight calculation
        // With power 5600 kW and weight ~107 tons (87 loco + 20 wagon), 
        // calculated speed = sqrt(5600/107) * 18 ≈ sqrt(52.3) * 18 ≈ 7.2 * 18 ≈ 130 km/h
        // But locomotive operational speed is 70, so result should be <= 70
        assertTrue("Speed should be positive", speed > 0);
        assertTrue("Speed should not exceed locomotive operational speed", speed <= 70.0);
    }

    @Test
    // Test: Calculate speed with track speed limit constraint
    public void testCalculateSpeed_SpeedLimitConstraint() {
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);

        // Segment with speed limit of 80 km/h
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, 80.0, 1);

        double speed = new TrainSchedulerService(mockConnection).calculateSpeed(train, segment);
        
        // Speed should be limited by track speed limit (80 km/h) or operational speed (70 km/h), whichever is lower
        assertTrue("Speed should be positive", speed > 0);
        assertTrue("Speed should not exceed track speed limit or operational speed", speed <= 70.0);
    }

    @Test
    // Test: Calculate speed with zero weight train
    public void testCalculateSpeed_ZeroWeight() {
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        // No wagons added - but locomotive still has weight in calculation

        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);

        // This should not crash, but speed will be based on locomotive weight (87 tons)
        double speed = new TrainSchedulerService(mockConnection).calculateSpeed(train, segment);
        assertTrue("Speed should be non-negative", speed >= 0);
    }

    @Test
    // Test: Calculate travel time for given speed and distance
    public void testCalculateTravelTime() {
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        WagonSpecs wagonSpecs = new WagonSpecs(1, 1, 50.0, 30.0);
        Wagon wagon = new Wagon(1, 1, 1, wagonSpecs, 20.0);
        train.addWagon(wagon);

        // Segment: 10 km (10000 meters), speed limit 100 km/h
        LineSegment segment = new LineSegment(1, 1, 1000.0, 10000.0, 2, 100.0, 1);

        TrainSchedulerService service = new TrainSchedulerService(mockConnection);
        Duration travelTime = service.calculateTravelTime(train, segment);
        
        // At 70 km/h (operational speed), 10 km should take approximately 514 seconds
        // But actual speed might be lower due to power/weight, so time should be >= 514 seconds
        assertNotNull("Travel time should not be null", travelTime);
        assertTrue("Travel time should be non-negative", travelTime.getSeconds() >= 0);
        // With segment of 10km and speed likely around 70 km/h or less, time should be reasonable
        assertTrue("Travel time for 10km should be reasonable (< 1 hour)", travelTime.getSeconds() <= 3600);
    }

    @Test
    // Test: Calculate travel time with zero speed returns zero duration
    public void testCalculateTravelTime_ZeroSpeed() {
        // Create train but without proper setup to get zero speed
        Train train = new Train(1, 1);
        // No locomotive - this will cause issues, but we test edge case
        
        LineSegment segment = new LineSegment(1, 1, 1000.0, 10000.0, 2, null, 1);

        TrainSchedulerService service = new TrainSchedulerService(mockConnection);
        Duration travelTime = service.calculateTravelTime(train, segment);
        
        // With no locomotive, max speed will be 0, so travel time should be zero
        assertEquals("Travel time should be zero when speed is zero", Duration.ZERO, travelTime);
    }

    @Test
    // Test: Calculate speed considers all constraints (loco operational speed, track limit, power/weight)
    public void testCalculateSpeed_AllConstraints() {
        // High power locomotive with operational speed 70 km/h, max speed 200 km/h
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 11200.0, 0.5, 70.0, 200.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);
        
        // Heavy train with many wagons
        WagonSpecs wagonSpecs = new WagonSpecs(1, 1, 50.0, 30.0);
        for (int i = 0; i < 10; i++) {
            Wagon wagon = new Wagon(i, 1, 1, wagonSpecs, 25.0);
            wagon.setLoaded(true); // Loaded wagon
            train.addWagon(wagon);
        }

        // Segment with moderate speed limit
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, 90.0, 1);

        double speed = new TrainSchedulerService(mockConnection).calculateSpeed(train, segment);
        
        // Speed should be the minimum of:
        // - Locomotive operational speed (70 km/h)
        // - Track limit (90 km/h)
        // - Power/weight calculation
        // Result should be <= 70 km/h (operational speed is the limiting factor)
        assertTrue("Speed should be positive", speed > 0);
        assertTrue("Speed should respect operational speed limit", speed <= 70.0);
    }
    
    @Test
    // Test: Speed calculation uses operationalSpeed, not maxSpeed
    public void testCalculateSpeed_OperationalSpeedVsMaxSpeed() {
        // Locomotive with different operational and max speeds
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 220.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        Train train = new Train(1, 1);
        train.addLocomotive(locomotive);

        // Segment with no speed limit
        LineSegment segment = new LineSegment(1, 1, 1000.0, 5000.0, 2, null, 1);

        double speed = new TrainSchedulerService(mockConnection).calculateSpeed(train, segment);
        
        // Speed should be limited by operational speed (70 km/h), not max speed (220 km/h)
        assertTrue("Speed should be positive", speed > 0);
        assertTrue("Speed should not exceed operational speed", speed <= 70.0);
        assertTrue("Speed should be less than max speed", speed < 220.0);
    }
}
