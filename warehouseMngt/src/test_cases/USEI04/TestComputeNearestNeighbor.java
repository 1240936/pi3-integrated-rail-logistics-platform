package test_cases.USEI04;

import main.controller.PickPathService;
import main.domain.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for PickingService.computeNearestNeighbor() method
 * 
 * Tests Strategy B - Nearest-Neighbour (greedy):
 * - Sort bays using distance function D
 * - Calculate distance using distance function D
 * - Return path sequence and total distance
 */
public class TestComputeNearestNeighbor {

    /**
     * Test Case 1: Basic nearest-neighbor
     * Expected: Bays sorted using distance function D, greedy algorithm applied
     */
    @Test
    public void testComputeNearestNeighbor_Basic() {
        PickPathService service = new PickPathService();

        // Create PickItems with distinct coordinates
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 5);   // (1,5)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 4, 15);  // (4,15)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 2, 10);  // (2,10)

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify total bays visited
        assertEquals(3, result.getTotalBays());

        // Expected greedy order:
        // From (0,0) → (1,5): 3 + 5 = 8
        // (1,5) → (2,10): 5 + 3 + 10 = 18
        // (2,10) → (4,15): 10 + 6 + 15 = 31
        List<Coordinate> expectedOrder = List.of(
                new Coordinate(1, 5),
                new Coordinate(2, 10),
                new Coordinate(4, 15)
        );
        assertEquals(expectedOrder, sequence);

        // Verify total distance
        double expectedDistance = 8 + 18 + 31;
        assertEquals(expectedDistance, result.getTotalDistance(), 0.01);
    }

    /**
     * Test Case 2: Nearest-neighbor selection from entrance
     * Expected: First bay selected is nearest to entrance (0,0)
     */
    @Test
    public void testComputeNearestNeighbor_NearestFromEntrance() {
        PickPathService service = new PickPathService();

        // Create PickItems at varying distances from entrance
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 5);   // (1,5) → D = 3 + 5 = 8
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 3, 10);  // (3,10) → D = 9 + 10 = 19
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 2, 20);  // (2,20) → D = 6 + 20 = 26

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify that the first bay selected is the one closest to entrance (0,0)
        assertEquals(new Coordinate(1, 5), sequence.get(0));

        // Optional: verify total bays and sequence contains all expected coordinates
        assertEquals(3, result.getTotalBays());
        assertTrue(sequence.contains(new Coordinate(3, 10)));
        assertTrue(sequence.contains(new Coordinate(2, 20)));
    }

    /**
     * Test Case 3: Greedy algorithm progression
     * Expected: Each subsequent bay selected is nearest to current position
     */
    @Test
    public void testComputeNearestNeighbor_GreedyProgression() {
        PickPathService service = new PickPathService();

        // Create PickItems with known coordinates
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 5);   // (1,5)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 3, 10);  // (3,10)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 2, 6);   // (2,6)

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify total bays visited
        assertEquals(3, result.getTotalBays());

        // Expected greedy order:
        // From (0,0) → (1,5): 3 + 5 = 8
        // (1,5) → (2,6): 5 + 3 + 6 = 14
        // (2,6) → (3,10): 6 + 3 + 10 = 19
        List<Coordinate> expectedOrder = List.of(
                new Coordinate(1, 5),
                new Coordinate(2, 6),
                new Coordinate(3, 10)
        );

        // Verify sequence matches expected greedy progression
        assertEquals(expectedOrder, sequence);

        // Verify total distance
        double expectedDistance = 8 + 14 + 19;
        assertEquals(expectedDistance, result.getTotalDistance(), 0.01);
    }

    /**
     * Test Case 4: Distance function D application
     * Expected: Distance function D applied correctly for bay selection
     */
    @Test
    public void testComputeNearestNeighbor_DistanceFunctionApplication() {
        PickPathService service = new PickPathService();

        // Create PickItems with coordinates that require distance function D to be applied
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 2, 10);  // (2,10)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 4, 5);   // (4,5)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 1, 8);   // (1,8)

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan with one trolley
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify total bays visited
        assertEquals(3, result.getTotalBays());

        // From (0,0), distances using D = b1 + |a1 - a2| * 3 + b2:
        // (1,8): 3 + 8 = 11
        // (2,10): 6 + 10 = 16
        // (4,5): 12 + 5 = 17
        // Expected greedy order: (1,8) → (2,10) → (4,5)
        List<Coordinate> expectedOrder = List.of(
                new Coordinate(1, 8),
                new Coordinate(2, 10),
                new Coordinate(4, 5)
        );
        assertEquals(expectedOrder, sequence);

        // Verify total distance:
        // (0,0) → (1,8): 3 + 8 = 11
        // (1,8) → (2,10): 8 + 3 + 10 = 21
        // (2,10) → (4,5): 10 + 6 + 5 = 21
        double expectedDistance = 11 + 21 + 21;
        assertEquals(expectedDistance, result.getTotalDistance(), 0.01);
    }

    /**
     * Test Case 5: Total distance calculation
     * Expected: Total distance calculated correctly for greedy path
     */
    @Test
    public void testComputeNearestNeighbor_TotalDistanceCalculation() {
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

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);
        List<Coordinate> sequence = result.getSequence();

        // Expected greedy order:
        // From (0,0) → (1,5): 3 + 5 = 8
        // (1,5) → (2,10): 5 + 3 + 10 = 18
        // (2,10) → (4,15): 10 + 6 + 15 = 31
        double expectedDistance = 8 + 18 + 31;

        // Verify total distance
        assertEquals(expectedDistance, result.getTotalDistance(), 0.01);

        // Optional: verify sequence order
        List<Coordinate> expectedOrder = List.of(
                new Coordinate(1, 5),
                new Coordinate(2, 10),
                new Coordinate(4, 15)
        );
        assertEquals(expectedOrder, sequence);
    }

    /**
     * Test Case 6: Duplicate bay merging
     * Expected: Duplicate bays merged into single stop with quantity summation
     */
    @Test
    public void testComputeNearestNeighbor_DuplicateBayMerging() {
        PickPathService service = new PickPathService();

        // Create PickItems with the same aisle and bay (duplicate bay)
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 2, 10); // (2,10)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 2, 10); // same location

        // Create a trolley and add both items
        Trolley trolley = new Trolley("T1", 50.0);
        trolley.addItem(item1);
        trolley.addItem(item2);

        // Create PickPlan with one trolley
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify that only one coordinate is returned (duplicate bay merged)
        assertEquals(1, sequence.size());

        // Verify that the coordinate is correct
        Coordinate merged = sequence.get(0);
        assertEquals(2, merged.getAisle());
        assertEquals(10, merged.getBay());

        // Verify total distance: from (0,0) to (2,10) = |2-0|*3 + 10 = 6 + 10 = 16
        assertEquals(16.0, result.getTotalDistance(), 0.01);

        // Optional: verify that both items are still present in the trolley
        assertEquals(2, trolley.getItems().size());
    }

    /**
     * Test Case 7: Path sequence structure
     * Expected: Path sequence returned with correct bay coordinates in greedy order
     */
    @Test
    public void testComputeNearestNeighbor_PathSequenceStructure() {
        PickPathService service = new PickPathService();

        // Create PickItems with known coordinates
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 5);   // (1,5)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 3, 10);  // (3,10)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 2, 6);   // (2,6)

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        // Create PickPlan
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);
        List<Coordinate> sequence = result.getSequence();

        // Expected greedy order:
        // From (0,0) → (1,5): D = 3 + 5 = 8
        // (1,5) → (2,6): D = 5 + 3 + 6 = 14
        // (2,6) → (3,10): D = 6 + 3 + 10 = 19
        List<Coordinate> expectedOrder = List.of(
                new Coordinate(1, 5),
                new Coordinate(2, 6),
                new Coordinate(3, 10)
        );

        // Verify path sequence matches expected greedy order
        assertEquals(expectedOrder, sequence);
    }

    /**
     * Test Case 8: Total bays visited calculation
     * Expected: Total bays visited calculated correctly
     */
    @Test
    public void testComputeNearestNeighbor_TotalBaysVisited() {
        PickPathService service = new PickPathService();

        // Create PickItems in distinct bays
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 1, 5);   // (1,5)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 2, 10);  // (2,10)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 3, 15);  // (3,15)
        PickItem item4 = new PickItem("ORD4", 1, "SKU4", 2, 1.0, "BOX4", 2, 10);  // duplicate of (2,10)

        // Create a trolley and add items
        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);
        trolley.addItem(item4);

        // Create PickPlan
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);

        // Verify total bays visited (should merge duplicate (2,10))
        assertEquals(3, result.getTotalBays());

        // Verify sequence contains expected coordinates
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
    public void testComputeNearestNeighbor_SingleBay() {
        PickPathService service = new PickPathService();

        // Create a single PickItem located at (3,12)
        PickItem item = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 3, 12); // (aisle=3, bay=12)

        // Create a trolley and add the item
        Trolley trolley = new Trolley("T1", 50.0);
        trolley.addItem(item);

        // Create PickPlan with one trolley
        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run nearest-neighbor sweep
        PickSequenceResult result = service.computeNearestNeighbor(plan);
        List<Coordinate> sequence = result.getSequence();

        // Verify that only one bay is visited
        assertEquals(1, result.getTotalBays());

        // Verify that the coordinate is correct
        assertEquals(new Coordinate(3, 12), sequence.get(0));

        // Verify total distance: from (0,0) to (3,12) = |3-0|*3 + 12 = 9 + 12 = 21
        assertEquals(21.0, result.getTotalDistance(), 0.01);
    }

    /**
     * Test Case 10: Empty pick plan handling
     * Expected: Empty pick plan handled gracefully
     */
    @Test
    public void testComputeNearestNeighbor_EmptyPickPlan() {
        PickPathService service = new PickPathService();

        // Setup: Create an empty pick plan (no trolleys, no skipped items)
        PickPlan emptyPlan = new PickPlan(List.of(), List.of(), "N/A");

        // Action: Call computeNearestNeighbor
        PickSequenceResult result = service.computeNearestNeighbor(emptyPlan);

        // Verify: Empty pick plan handled gracefully
        assertNotNull(result);
        assertEquals(0, result.getTotalBays());
        assertEquals(0.0, result.getTotalDistance(), 0.01);
        assertTrue(result.getSequence().isEmpty());
        assertEquals("Nearest-Neighbour Greedy", result.getStrategyName());
    }

    /**
     * Test Case 11: Comparison with deterministic sweep
     * Expected: Nearest-neighbor may produce different path than deterministic sweep
     */
    @Test
    public void testComputeNearestNeighbor_ComparisonWithDeterministicSweep() {
        PickPathService service = new PickPathService();

        // Coordinates deliberately chosen to produce different paths under each strategy
        PickItem item1 = new PickItem("ORD1", 1, "SKU1", 5, 2.5, "BOX1", 4, 5);   // (4,5)
        PickItem item2 = new PickItem("ORD2", 1, "SKU2", 3, 1.5, "BOX2", 1, 15);  // (1,15)
        PickItem item3 = new PickItem("ORD3", 1, "SKU3", 4, 2.0, "BOX3", 2, 6);   // (2,6)

        Trolley trolley = new Trolley("T1", 100.0);
        trolley.addItem(item1);
        trolley.addItem(item2);
        trolley.addItem(item3);

        PickPlan plan = new PickPlan(List.of(trolley), List.of(), "N/A");

        // Run both strategies
        PickSequenceResult nearestResult = service.computeNearestNeighbor(plan);
        PickSequenceResult sweepResult = service.computeDeterministicSweep(plan);

        // Verify both strategies return same number of bays
        assertEquals(3, nearestResult.getTotalBays());
        assertEquals(3, sweepResult.getTotalBays());

        // Verify sequences are different
        List<Coordinate> nearestSequence = nearestResult.getSequence();
        List<Coordinate> sweepSequence = sweepResult.getSequence();
        assertNotEquals(nearestSequence, sweepSequence);

        // Verify distances are different
        double nearestDistance = nearestResult.getTotalDistance();
        double sweepDistance = sweepResult.getTotalDistance();
        assertNotEquals(nearestDistance, sweepDistance);
    }

}
