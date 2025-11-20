package USEI04;

import main.controller.PickPathService;
import main.domain.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for PickingService.computeDeterministicSweep() method
 * 
 * Tests Strategy A - Deterministic Sweep:
 * - Sort bays ascending by aisle
 * - Calculate distance using distance function D
 * - Return path sequence and total distance
 */
public class TestComputeDeterministicSweep {
    
    /**
     * Test Case 1: Basic deterministic sweep
     * Expected: Bays sorted ascending by aisle, distance calculated correctly
     */
    @Test
    public void testComputeDeterministicSweep_Basic() {
        PickPathService service = new PickPathService();

        // Create PickItems with aisle and bay values
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 10, 5.0, "BOX1", 1, 10); // aisle 1, bay 10
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 5, 2.5, "BOX2", 2, 20); // aisle 2, bay 20
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 8, 4.0, "BOX3", 3, 30); // aisle 3, bay 30

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan with one trolley and no skipped items
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify sorting by aisle
        assertEquals(3, result.getTotalBays());
        assertEquals("Deterministic Sweep", result.getStrategyName());
        assertEquals(1, sequence.get(0).getAisle());
        assertEquals(2, sequence.get(1).getAisle());
        assertEquals(3, sequence.get(2).getAisle());

        // Verify total distance:
        // From (0,0) to (1,10): 0 + |1-0|*3 + 10 = 13
        // From (1,10) to (2,20): 10 + |2-1|*3 + 20 = 33
        // From (2,20) to (3,30): 20 + |3-2|*3 + 30 = 53
        double expectedDistance = 13 + 33 + 53;
        assertEquals(expectedDistance, result.getTotalDistance(), 0.01);
    }

    /**
     * Test Case 2: Same aisle distance calculation
     * Expected: Distance D = |b1 - b2| for same aisle
     */
    @Test
    public void testComputeDeterministicSweep_SameAisleDistance() {
        PickPathService service = new PickPathService();

        // Create PickItems in the same aisle but different bays
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 2, 10); // aisle 2, bay 10
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 2, 30); // aisle 2, bay 30

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 50.0);
        trolley.addItem(item1);
        trolley.addItem(item2);

        // Create PickPlan with one trolley
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify both coordinates are in aisle 2
        assertEquals(2, sequence.get(0).getAisle());
        assertEquals(2, sequence.get(1).getAisle());

        // Verify sorting by bay within same aisle
        assertEquals(10, sequence.get(0).getBay());
        assertEquals(30, sequence.get(1).getBay());

        // Distance from (0,0) to (2,10): 0 + |2-0|*3 + 10 = 16
        // Distance from (2,10) to (2,30): |30 - 10| = 20
        double expectedDistance = 16 + 20;
        assertEquals(expectedDistance, result.getTotalDistance(), 0.01);
    }

    /**
     * Test Case 3: Different aisles distance calculation
     * Expected: Distance D = b1 + |a1 - a2| * 3 + b2 for different aisles
     */
    @Test
    public void testComputeDeterministicSweep_DifferentAislesDistance() {
        PickPathService service = new PickPathService();

        // Create PickItems in different aisles
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 10); // aisle 1, bay 10
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 4, 20); // aisle 4, bay 20

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 50.0);
        trolley.addItem(item1);
        trolley.addItem(item2);

        // Create PickPlan with one trolley
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify sorting by aisle
        assertEquals(2, result.getTotalBays());
        assertEquals(1, sequence.get(0).getAisle());
        assertEquals(4, sequence.get(1).getAisle());

        // Verify total distance:
        // From (0,0) to (1,10): 0 + |1-0|*3 + 10 = 13
        // From (1,10) to (4,20): 10 + |4-1|*3 + 20 = 10 + 9 + 20 = 39
        double expectedDistance = 13 + 39;
        assertEquals(expectedDistance, result.getTotalDistance(), 0.01);
    }


    /**
     * Test Case 4: Entrance coordinate (0,0) handling
     * Expected: Path starts from entrance coordinate (0,0)
     */
    @Test
    public void testComputeDeterministicSweep_EntranceCoordinate() {
        PickPathService service = new PickPathService();

        // Create PickItems in different bays
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 10); // aisle 1, bay 10
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 2, 20); // aisle 2, bay 20

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 50.0);
        trolley.addItem(item1);
        trolley.addItem(item2);

        // Create PickPlan with one trolley
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify that the first distance is calculated from entrance (0,0) to first bay (aisle 1, bay 10)
        Coordinate firstBay = sequence.get(0);
        double expectedFirstLeg = Math.abs(firstBay.getAisle()) * 3 + firstBay.getBay(); // entrance to first bay
        double totalDistance = result.getTotalDistance();

        // Verify that total distance includes entrance-to-first-bay leg
        assertTrue("Total distance should be at least the entrance-to-first-bay leg",
                totalDistance >= expectedFirstLeg);

        // Optional: verify exact total distance if second bay is known
        Coordinate secondBay = sequence.get(1);
        double secondLeg = firstBay.getBay() + Math.abs(firstBay.getAisle() - secondBay.getAisle()) * 3 + secondBay.getBay();
        double expectedTotal = expectedFirstLeg + secondLeg;
        assertEquals(expectedTotal, totalDistance, 0.01);
    }

    /**
     * Test Case 5: Total distance calculation
     * Expected: Total distance = ΣD(Ci, Ci+1) calculated correctly
     */
    @Test
    public void testComputeDeterministicSweep_TotalDistanceCalculation() {
        PickPathService service = new PickPathService();

        // Create PickItems with known coordinates
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 5);   // (1,5)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 2, 10);  // (2,10)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 4, 15);  // (4,15)

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);
        List<Coordinate> sequence = result.getSequence();

        // Manually compute expected total distance:
        // From (0,0) to (1,5): 0 + |1-0|*3 + 5 = 8
        // From (1,5) to (2,10): 5 + |2-1|*3 + 10 = 5 + 3 + 10 = 18
        // From (2,10) to (4,15): 10 + |4-2|*3 + 15 = 10 + 6 + 15 = 31
        double expectedDistance = 8 + 18 + 31;

        // Verify total distance
        assertEquals(expectedDistance, result.getTotalDistance(), 0.01);

        // Optional: verify sequence order
        assertEquals(1, sequence.get(0).getAisle());
        assertEquals(2, sequence.get(1).getAisle());
        assertEquals(4, sequence.get(2).getAisle());
    }

    /**
     * Test Case 6: Duplicate bay merging
     * Expected: Duplicate bays merged into single stop with quantity summation
     */
    @Test
    public void testComputeDeterministicSweep_DuplicateBayMerging() {
        PickPathService service = new PickPathService();

        // Create PickItems with the same aisle and bay (duplicate bay)
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 2, 10); // aisle 2, bay 10
        PickItem item2 = new PickItem("ORD2", 2, "SKU2", 3, 1.5, "BOX2", 2, 10); // same location

        // Create a trolley and add both items
        Trolley trolley = new Trolley("T1", 50.0);
        trolley.addItem(item1);
        trolley.addItem(item2);

        // Create PickPlan with one trolley
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify that only one coordinate is returned (duplicate bay merged)
        assertEquals(1, sequence.size());

        // Verify that the coordinate is correct
        Coordinate merged = sequence.get(0);
        assertEquals(2, merged.getAisle());
        assertEquals(10, merged.getBay());

        // Verify total distance: from (0,0) to (2,10) = 0 + |2-0|*3 + 10 = 16
        assertEquals(16.0, result.getTotalDistance(), 0.01);
    }

    /**
     * Test Case 7: Path sequence structure
     * Expected: Path sequence returned with correct bay coordinates
     */
    @Test
    public void testComputeDeterministicSweep_PathSequenceStructure() {
        PickPathService service = new PickPathService();

        // Create PickItems with known coordinates
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 5);   // (1,5)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 2, 10);  // (2,10)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 4, 15);  // (4,15)

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify sequence structure and content
        assertEquals(3, sequence.size());

        // Expected order: sorted by aisle ascending
        assertEquals(new Coordinate(1, 5), sequence.get(0));
        assertEquals(new Coordinate(2, 10), sequence.get(1));
        assertEquals(new Coordinate(4, 15), sequence.get(2));
    }


    /**
     * Test Case 8: Total bays visited calculation
     * Expected: Total bays visited calculated correctly
     */
    @Test
    public void testComputeDeterministicSweep_TotalBaysVisited() {
        PickPathService service = new PickPathService();

        // Create PickItems in distinct bays
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 5);   // (1,5)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 2, 10);  // (2,10)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 3, 15);  // (3,15)

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);

        // Verify total bays visited
        assertEquals(3, result.getTotalBays());

        // Optional: verify sequence contains expected coordinates
        List<Coordinate> sequence = result.getSequence();
        assertTrue(sequence.contains(new Coordinate(1, 5)));
        assertTrue(sequence.contains(new Coordinate(2, 10)));
        assertTrue(sequence.contains(new Coordinate(3, 15)));
    }

    /**
     * Test Case 9: Single bay scenario
     * Expected: Single bay handled correctly
     */
    @Test
    public void testComputeDeterministicSweep_SingleBay() {
        PickPathService service = new PickPathService();

        // Create a single PickItem
        PickItem item = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 3, 12); // aisle 3, bay 12

        // Create a trolley and add the item
        Trolley trolley = new Trolley("T1", 50.0);
        trolley.addItem(item);

        // Create PickPlan with one trolley
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify that only one bay is visited
        assertEquals(1, result.getTotalBays());
        assertEquals(new Coordinate(3, 12), sequence.get(0));

        // Verify total distance: from (0,0) to (3,12) = |3-0|*3 + 12 = 9 + 12 = 21
        assertEquals(21.0, result.getTotalDistance(), 0.01);
    }

    /**
     * Test Case 10: Empty pick plan handling
     * Expected: Empty pick plan handled gracefully
     */
    @Test
    public void testComputeDeterministicSweep_EmptyPickPlan() {
        PickPathService service = new PickPathService();

        // Create an empty pick plan (no trolleys, no skipped items)
        PickPlan emptyPlan = new PickPlan(List.of(), List.of(), "N/A");

        // Run deterministic sweep
        PickSequenceResult result = service.computeDeterministicSweep(emptyPlan);

        // Verify result is handled gracefully
        assertNotNull(result);
        assertEquals(0, result.getTotalBays());
        assertEquals(0.0, result.getTotalDistance(), 0.01);
        assertTrue(result.getSequence().isEmpty());
        assertEquals("Deterministic Sweep", result.getStrategyName());
    }

}
