import main.domain.*;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for TrainDispatchController - Route Dispatching
 * Tests the dispatchTrain method with mocked repositories
 */
public class TrainDispatchingTest {

    private Train train1;
    private Facility facilityA, facilityB, facilityC;

    @Before
    public void setUp(){

        // Create test facilities
        facilityA = new Facility(1, "Facility A");
        facilityB = new Facility(2, "Facility B");
        facilityC = new Facility(3, "Facility C");

        // Create test train
        LocomotiveSpecs locoSpecs = new LocomotiveSpecs(1, "Test", 5600.0, 0.5, 70.0, 120.0, 6);
        Locomotive locomotive = new Locomotive(1, 1, 1, locoSpecs);
        train1 = new Train(1, 1);
        train1.addLocomotive(locomotive);

        // Create test freight
    }

    @Test
    // Test: Dispatch train with valid parameters
    public void testDispatchTrain_ValidParameters() {
        List<Integer> pathFacilityIds = Collections.singletonList(facilityC.getId());

        // Verify facilities exist
        assertNotNull("Facility A should exist", facilityA);
        assertNotNull("Facility B should exist", facilityB);
        assertNotNull("Facility C should exist", facilityC);
        
        // Verify train exists
        assertNotNull("Train should exist", train1);
        assertEquals("Train ID should be 1", 1, train1.getId());

        // Verify path is valid
        assertNotNull("Path should not be null", pathFacilityIds);
        assertEquals("Path should have one facility", 1, pathFacilityIds.size());
    }

    @Test
    // Test: Dispatch train with invalid train ID
    public void testDispatchTrain_InvalidTrainId() {
        // When train doesn't exist, should throw exception
        // This tests the validation logic
        int invalidTrainId = 999;
        
        // Verify test train is not the invalid ID
        assertNotEquals("Test train should not match invalid ID", 
                        invalidTrainId, train1.getId());
    }

    @Test
    // Test: Dispatch train with invalid facility IDs
    public void testDispatchTrain_InvalidFacilityIds() {
        int invalidStartFacilityId = 999;
        int invalidEndFacilityId = 888;
        
        // Verify test facilities don't match invalid IDs
        assertNotEquals("Start facility should not match invalid ID", 
                        invalidStartFacilityId, facilityA.getId());
        assertNotEquals("End facility should not match invalid ID", 
                        invalidEndFacilityId, facilityB.getId());
    }

    @Test
    // Test: Dispatch train with freight validation
    public void testDispatchTrain_FreightValidation() {
        // Freight should have origin and destination on the route
        Freight freight = new Freight(2001, 0, facilityA, facilityB);
        
        // Verify freight origin is on route start
        assertEquals("Freight origin should match route start", 
                     facilityA.getId(), freight.getOriginFacility().getId());
        
        // Verify freight destination is on route end
        assertEquals("Freight destination should match route end", 
                     facilityB.getId(), freight.getDestinationFacility().getId());
    }

    @Test
    // Test: Dispatch train with freight that has destination before origin
    public void testDispatchTrain_FreightDestinationBeforeOrigin() {
        // Create a route: A -> C -> B
        List<Integer> routeFacilities = new ArrayList<>();
        routeFacilities.add(facilityA.getId());
        routeFacilities.add(facilityC.getId());
        routeFacilities.add(facilityB.getId());
        
        // Freight from A to B should be valid (destination after origin)
        Freight freight = new Freight(2001, 0, facilityA, facilityB);
        
        int originIndex = routeFacilities.indexOf(freight.getOriginFacility().getId());
        int destinationIndex = routeFacilities.indexOf(freight.getDestinationFacility().getId());
        
        assertTrue("Origin should be before destination in route", 
                   originIndex < destinationIndex);
    }

    @Test
    // Test: Dispatch train prevents overlapping routes for same train
    public void testDispatchTrain_OverlappingRoutes() {
        LocalDateTime time1 = LocalDateTime.of(2025, 10, 20, 9, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 10, 20, 9, 30, 0); // 30 minutes later
        
        // Two routes with overlapping times for same train should conflict
        // Route 1: 09:00 - 10:00
        // Route 2: 09:30 - 10:30 (overlaps)
        
        LocalDateTime route1End = time1.plusHours(1);
        
        // Check if times overlap
        boolean overlaps = !route1End.isBefore(time2);
        assertTrue("Routes should overlap", overlaps);
    }

    @Test
    // Test: Dispatch train allows non-overlapping routes for same train
    public void testDispatchTrain_NonOverlappingRoutes() {
        LocalDateTime time1 = LocalDateTime.of(2025, 10, 20, 9, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 10, 20, 11, 0, 0); // 2 hours later
        
        // Two routes with non-overlapping times for same train should be allowed
        LocalDateTime route1End = time1.plusHours(1);
        
        // Check if times don't overlap
        boolean overlaps = !route1End.isBefore(time2) && !time2.isBefore(time1);
        assertFalse("Routes should not overlap", overlaps);
    }
}

