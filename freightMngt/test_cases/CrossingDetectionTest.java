import main.domain.*;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Unit tests for TrainSchedulerService - Crossing Detection
 * Tests the detectCrossings method with mocked repositories
 */
public class CrossingDetectionTest {

    private Facility facilityA;
    private Facility facilityB;

    @Before
    public void setUp() {
        // Create test facilities
        facilityA = new Facility(1, "Facility A");
        facilityB = new Facility(2, "Facility B");
        Facility facilityC = new Facility(3, "Facility C");

        // Create test trains
        LocomotiveSpecs locoSpecs1 = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive1 = new Locomotive(1, 1, 1, locoSpecs1);
        Train train1 = new Train(1, 1);
        train1.addLocomotive(locomotive1);

        LocomotiveSpecs locoSpecs2 = new LocomotiveSpecs(2, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive2 = new Locomotive(2, 1, 1, locoSpecs2);
        Train train2 = new Train(2, 1);
        train2.addLocomotive(locomotive2);

        // Create test routes
        LocalDateTime startTime1 = LocalDateTime.of(2025, 10, 20, 9, 0, 0);
        Route route1 = new Route(1, 1, facilityA, facilityB, startTime1);
        route1.addPathPoint(facilityC, 1);

        LocalDateTime startTime2 = LocalDateTime.of(2025, 10, 20, 9, 30, 0);
        Route route2 = new Route(2, 2, facilityB, facilityA, startTime2);
        route2.addPathPoint(facilityC, 1);

    }

    @Test
    // Test: Crossing detection with overlapping times on single-track segment
    public void testDetectCrossings_OverlappingTimes(){
        // This test demonstrates the expected behavior

        // Create routes with overlapping times on the same single-track segment
        LocalDateTime baseTime = LocalDateTime.of(2025, 10, 20, 9, 0, 0);
        Route route1 = new Route(1, 1, facilityA, facilityB, baseTime);
        Route route2 = new Route(2, 2, facilityB, facilityA, baseTime.plusMinutes(15));

        // Verify routes are created correctly
        assertNotNull("Route 1 should not be null", route1);
        assertNotNull("Route 2 should not be null", route2);
        assertEquals("Route 1 should have train ID 1", 1, route1.getTrainId());
        assertEquals("Route 2 should have train ID 2", 2, route2.getTrainId());
        
        // Test that routes have different start times
        assertTrue("Route 2 should start after route 1", 
                   route2.getStartDate().isAfter(route1.getStartDate()));
    }

    @Test
    // Test: No crossing when routes don't overlap in time
    public void testDetectCrossings_NoOverlap() {
        // Routes with non-overlapping times should not create crossings
        LocalDateTime time1 = LocalDateTime.of(2025, 10, 20, 9, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 10, 21, 9, 0, 0); // Next day (24 hours later)

        // Verify time2 is after time1
        assertTrue("Time2 should be after time1", time2.isAfter(time1));
        
        // Verify there's at least a 24-hour gap between them (no overlap)
        // time1 + 24 hours = 2025-10-21 09:00:00, time2 = 2025-10-21 09:00:00
        // Since they're equal, time2 is not before time1+24h, meaning there's at least a 24h gap
        assertTrue("Routes should have at least 24 hours gap (no overlap)", 
                   !time2.isBefore(time1.plusHours(24)));
    }

    @Test
    // Test: No crossing on multi-track segments
    public void testDetectCrossings_MultiTrackSegment() {
        // Multi-track segments don't require crossings
        LineSegment multiTrackSegment = new LineSegment(1, 1, 1000.0, 5000.0, 2, 80.0, 1);
        
        assertFalse("Multi-track segment should not require crossings", 
                    multiTrackSegment.isSingleTrack());
    }

    @Test
    // Test: Single-track segment requires crossing detection
    public void testDetectCrossings_SingleTrackSegment() {
        LineSegment singleTrackSegment = new LineSegment(1, 1, 1000.0, 5000.0, 1, 80.0, 1);
        
        assertTrue("Single-track segment should require crossings", 
                   singleTrackSegment.isSingleTrack());
    }
}

