package USEI03;

import main.controller.PickingService;
import main.domain.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for PickingService.createPickPlan() method
 *
 * Tests picking plan creation with different packing heuristics:
 * - First Fit (FF): Place item in first available trolley where it fits
 * - First Fit Decreasing (FFD): Sort by weight, then first fit
 * - Best Fit Decreasing (BFD): Sort by weight, then tightest fit
 */
public class TestCreatePickPlan {

    /**
     * Test Case 1: First Fit (FF) - Basic functionality
     * Expected: Items placed in first available trolley where they fit
     */
    @Test
    public void testCreatePickPlan_FirstFitBasic() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 10.0;
        boolean allowSplitting = false;

        // Each item has total weight = qty * 1.0 (default in service)
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU1", 4, "BOX1", 1, 1); // weight 4
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU2", 3, "BOX2", 1, 2); // weight 3
        AllocationRow row3 = new AllocationRow("ORD1", 3, "SKU3", 5, "BOX3", 1, 3); // weight 5

        List<AllocationRow> allocations = Arrays.asList(row1, row2, row3);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        assertEquals("First Fit", plan.getPackingHeuristic());
        assertEquals(2, plan.getTrolleyCount()); // Expected: 2 trolleys
        assertEquals(3, plan.getTotalItems());   // 3 items in total
        assertEquals(0, plan.getSkippedItems().size()); // No skipped items

        // Check item distribution
        List<Trolley> trolleys = plan.getTrolleys();
        double totalWeight = trolleys.stream().mapToDouble(Trolley::getCurrentWeight).sum();
        assertEquals(12.0, totalWeight, 0.01); // 4 + 3 + 5 = 12kg

        // Check that the first trolley has at least two items (4 + 3 <= 10)
        Trolley first = trolleys.get(0);
        assertTrue(first.getItems().size() >= 2);
    }

    /**
     * Test Case 2: First Fit (FF) - Scan order
     * Expected: Scan order follows input order of allocation rows
     */
    @Test
    public void testCreatePickPlan_FirstFitScanOrder() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 10.0;
        boolean allowSplitting = false;

        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-A", 2, "BOX1", 1, 1); // weight 2
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU-B", 3, "BOX2", 1, 2); // weight 3
        AllocationRow row3 = new AllocationRow("ORD1", 3, "SKU-C", 4, "BOX3", 1, 3); // weight 4

        List<AllocationRow> allocations = Arrays.asList(row1, row2, row3);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        List<Trolley> trolleys = plan.getTrolleys();
        assertFalse(trolleys.isEmpty());

        // Collect all SKUs in the order they were added to trolleys
        List<String> scannedSkus = trolleys.stream()
                .flatMap(t -> t.getItems().stream())
                .map(PickItem::getSku)
                .toList();

        // Check if SKU order matches AllocationRow input order
        List<String> expectedOrder = Arrays.asList("SKU-A", "SKU-B", "SKU-C");
        assertEquals(expectedOrder, scannedSkus);
    }

    /**
     * Test Case 3: First Fit Decreasing (FFD) - Weight sorting
     * Expected: Items sorted by weight from largest to smallest
     */
    @Test
    public void testCreatePickPlan_FirstFitDecreasingWeightSorting() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 10.0;
        boolean allowSplitting = false;

        // Create AllocationRows with different quantities (weight = qty * 1.0)
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-X", 2, "BOX1", 1, 1); // weight 2
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU-Y", 5, "BOX2", 1, 2); // weight 5
        AllocationRow row3 = new AllocationRow("ORD1", 3, "SKU-Z", 3, "BOX3", 1, 3); // weight 3

        List<AllocationRow> allocations = Arrays.asList(row1, row2, row3);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT_DECREASING, trolleyCapacity, allowSplitting);

        // Assert
        List<Trolley> trolleys = plan.getTrolleys();
        assertFalse(trolleys.isEmpty());

        // Extract SKUs in the order they were placed into trolleys
        List<String> sortedSkus = trolleys.stream()
                .flatMap(t -> t.getItems().stream())
                .map(PickItem::getSku)
                .toList();

        // Expected: descending weight order → SKU-Y (5), SKU-Z (3), SKU-X (2)
        List<String> expectedOrder = Arrays.asList("SKU-Y", "SKU-Z", "SKU-X");
        assertEquals(expectedOrder, sortedSkus);
    }

    /**
     * Test Case 4: Best Fit Decreasing (BFD) - Tightest fit
     * Expected: Items placed in trolley with smallest remaining capacity
     */
    @Test
    public void testCreatePickPlan_BestFitDecreasingTightestFit() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 10.0;
        boolean allowSplitting = false;

        // Adjusted weights to force expected grouping
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-A", 5, "BOX1", 1, 1); // weight 5
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU-B", 6, "BOX2", 1, 2); // weight 6
        AllocationRow row3 = new AllocationRow("ORD1", 3, "SKU-C", 5, "BOX3", 1, 3); // weight 5

        List<AllocationRow> allocations = Arrays.asList(row1, row2, row3);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.BEST_FIT_DECREASING, trolleyCapacity, allowSplitting);

        // Assert
        List<Trolley> trolleys = plan.getTrolleys();
        assertEquals(2, trolleys.size()); // Expected: 2 trolleys

        // Check that SKU-B (6kg) is placed alone
        boolean skuBAlone = trolleys.stream()
                .anyMatch(t -> t.getItems().size() == 1 &&
                        t.getItems().get(0).getSku().equals("SKU-B"));
        assertTrue("SKU-B should be placed alone", skuBAlone);

        // Check that SKU-A and SKU-C are grouped together (5 + 5 = 10)
        boolean skuAandCGrouped = trolleys.stream()
                .anyMatch(t -> t.getItems().stream().anyMatch(i -> i.getSku().equals("SKU-A")) &&
                        t.getItems().stream().anyMatch(i -> i.getSku().equals("SKU-C")));
        assertTrue("SKU-A and SKU-C should be grouped together", skuAandCGrouped);

        // Ensure no items were skipped
        assertEquals(0, plan.getSkippedItems().size());
    }



    /**
     * Test Case 5: Trolley capacity constraints
     * Expected: Trolley capacity constraints are enforced
     */
    @Test
    public void testCreatePickPlan_TrolleyCapacityConstraints() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 10.0;
        boolean allowSplitting = false;

        // Create items that exceed a single trolley's capacity
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-A", 6, "BOX1", 1, 1); // weight 6
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU-B", 5, "BOX2", 1, 2); // weight 5
        AllocationRow row3 = new AllocationRow("ORD1", 3, "SKU-C", 4, "BOX3", 1, 3); // weight 4

        List<AllocationRow> allocations = Arrays.asList(row1, row2, row3);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        List<Trolley> trolleys = plan.getTrolleys();
        assertFalse(trolleys.isEmpty());

        // Ensure no trolley exceeds capacity
        for (Trolley trolley : trolleys) {
            double weight = trolley.getCurrentWeight();
            assertTrue("Trolley " + trolley.getTrolleyId() + " exceeded capacity!", weight <= trolleyCapacity);
        }

        // Ensure all items were allocated
        List<String> allSkus = trolleys.stream()
                .flatMap(t -> t.getItems().stream())
                .map(PickItem::getSku)
                .toList();

        assertTrue(allSkus.containsAll(Arrays.asList("SKU-A", "SKU-B", "SKU-C")));
        assertEquals(0, plan.getSkippedItems().size());
    }

    /**
     * Test Case 6: Item splitting - allowSplitting=true
     * Expected: Items split across trolleys when they don't fit in single trolley
     */
    @Test
    public void testCreatePickPlan_ItemSplittingAllowed() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 5.0;
        boolean allowSplitting = true;

        // Create an item that exceeds a single trolley's capacity
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-BIG", 10, "BOX1", 1, 1); // weight 10

        List<AllocationRow> allocations = Arrays.asList(row1);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        List<Trolley> trolleys = plan.getTrolleys();
        assertTrue("Expected multiple trolleys", trolleys.size() > 1);

        int totalQty = trolleys.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(i -> i.getSku().equals("SKU-BIG"))
                .mapToInt(PickItem::getQuantity)
                .sum();

        // Ensure total quantity is preserved
        assertEquals(10, totalQty);

        // Ensure no trolley exceeds capacity
        for (Trolley trolley : trolleys) {
            assertTrue("Trolley exceeded capacity!", trolley.getCurrentWeight() <= trolleyCapacity);
        }

        // Ensure no items were skipped
        assertEquals(0, plan.getSkippedItems().size());
    }

    /**
     * Test Case 7: Item splitting - allowSplitting=false
     * Expected: Items deferred to next trolley when they don't fit
     */
    @Test
    public void testCreatePickPlan_ItemSplittingNotAllowed() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 5.0;
        boolean allowSplitting = false;

        // Create an item that exceeds a single trolley's capacity
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-BIG", 10, "BOX1", 1, 1); // weight 10

        List<AllocationRow> allocations = Arrays.asList(row1);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        List<Trolley> trolleys = plan.getTrolleys();

        // Ensure the item was not allocated (doesn't fit and can't be split)
        List<String> allSkus = trolleys.stream()
                .flatMap(t -> t.getItems().stream())
                .map(PickItem::getSku)
                .toList();

        assertFalse("Item should not have been allocated", allSkus.contains("SKU-BIG"));

        // Ensure the item was marked as skipped
        List<String> skipped = plan.getSkippedItems();
        assertEquals(1, skipped.size());
        assertTrue(skipped.get(0).contains("SKU-BIG"));

        // Ensure no trolleys were created
        assertEquals(0, trolleys.size());
    }

    /**
     * Test Case 8: Total number of trolleys calculation
     * Expected: Correct total number of trolleys calculated
     */
    @Test
    public void testCreatePickPlan_TotalTrolleysCalculation() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 5.0;
        boolean allowSplitting = false;

        // Create items that require multiple trolleys
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-1", 3, "BOX1", 1, 1); // weight 3
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU-2", 2, "BOX2", 1, 2); // weight 2
        AllocationRow row3 = new AllocationRow("ORD1", 3, "SKU-3", 4, "BOX3", 1, 3); // weight 4
        AllocationRow row4 = new AllocationRow("ORD1", 4, "SKU-4", 3, "BOX4", 1, 4); // weight 3

        List<AllocationRow> allocations = Arrays.asList(row1, row2, row3, row4);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        int expectedTrolleys = 3; // Example: (3+2), (4), (3) → 3 trolleys
        assertEquals(expectedTrolleys, plan.getTrolleyCount());

        // Ensure no trolley exceeds capacity
        for (Trolley trolley : plan.getTrolleys()) {
            assertTrue("Trolley exceeded capacity!", trolley.getCurrentWeight() <= trolleyCapacity);
        }

        // Ensure all items were allocated
        List<String> allSkus = plan.getTrolleys().stream()
                .flatMap(t -> t.getItems().stream())
                .map(PickItem::getSku)
                .toList();

        assertTrue(allSkus.containsAll(Arrays.asList("SKU-1", "SKU-2", "SKU-3", "SKU-4")));
        assertEquals(0, plan.getSkippedItems().size());
    }


    /**
     * Test Case 9: Trolley utilization calculation
     * Expected: Trolley utilization (usedWeight/capacityWeight) calculated correctly
     */
    @Test
    public void testCreatePickPlan_TrolleyUtilizationCalculation() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 10.0;
        boolean allowSplitting = false;

        // Create items with total weight of 15kg
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-A", 5, "BOX1", 1, 1); // weight 5
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU-B", 6, "BOX2", 1, 2); // weight 6
        AllocationRow row3 = new AllocationRow("ORD1", 3, "SKU-C", 4, "BOX3", 1, 3); // weight 4

        List<AllocationRow> allocations = Arrays.asList(row1, row2, row3);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        double expectedUsedWeight = 15.0;
        int trolleyCount = plan.getTrolleyCount();
        double expectedTotalCapacity = trolleyCount * trolleyCapacity;
        double expectedUtilization = expectedUsedWeight / expectedTotalCapacity;

        // Verify that the calculation is correct
        assertEquals(expectedUsedWeight, plan.getTotalWeight(), 0.01);
        assertEquals(expectedUtilization, plan.getWeightUtilization(), 0.01);

        // Verify that all items were allocated
        assertEquals(0, plan.getSkippedItems().size());
    }

    /**
     * Test Case 10: Picking plan structure for each trolley
     * Expected: Each trolley contains correct picking plan with all required fields
     */
    @Test
    public void testCreatePickPlan_PickingPlanStructure() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 10.0;
        boolean allowSplitting = false;

        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-A", 3, "BOX1", 1, 1); // weight 3
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU-B", 4, "BOX2", 2, 2); // weight 4
        AllocationRow row3 = new AllocationRow("ORD1", 3, "SKU-C", 2, "BOX3", 3, 3); // weight 2

        List<AllocationRow> allocations = Arrays.asList(row1, row2, row3);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        List<Trolley> trolleys = plan.getTrolleys();
        assertFalse("No trolley was created", trolleys.isEmpty());

        for (Trolley trolley : trolleys) {
            // Verify ID and capacity
            assertNotNull("Trolley ID is null", trolley.getTrolleyId());
            assertTrue("Invalid capacity", trolley.getMaxWeightCapacity() > 0);
            assertTrue("Negative current weight", trolley.getCurrentWeight() >= 0);
            assertTrue("Weight exceeds capacity", trolley.getCurrentWeight() <= trolley.getMaxWeightCapacity());

            // Verify items
            List<PickItem> items = trolley.getItems();
            assertFalse("Trolley has no items", items.isEmpty());

            for (PickItem item : items) {
                assertNotNull("SKU is null", item.getSku());
                assertTrue("Invalid quantity", item.getQuantity() > 0);
                assertTrue("Invalid weight", item.getWeight() > 0);
                assertNotNull("Box ID is null", item.getBoxId());
                assertTrue("Invalid aisle", item.getAisle() >= 0);
                assertTrue("Invalid bay", item.getBay() >= 0);
            }
        }

        // Verify that all items were allocated
        assertEquals(0, plan.getSkippedItems().size());
    }

    /**
     * Test Case 11: Partial allocation logging
     * Expected: Partial allocations logged correctly
     */
    @Test
    public void testCreatePickPlan_PartialAllocationLogging() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 5.0;
        boolean allowSplitting = true;

        // Create item that exceeds trolley capacity
        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-SPLIT", 10, "BOX1", 1, 1); // weight 10

        List<AllocationRow> allocations = Arrays.asList(row1);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        List<Trolley> trolleys = plan.getTrolleys();
        assertTrue("Expected multiple trolleys", trolleys.size() > 1);

        // Verify that at least one trolley contains partial allocation log
        boolean hasPartialLog = trolleys.stream()
                .flatMap(t -> t.getLogs().stream())
                .anyMatch(log -> log.contains("PARTIAL ALLOCATION") && log.contains("SKU-SPLIT"));

        assertTrue("Partial allocation was not logged", hasPartialLog);

        // Verify that total quantity is preserved
        int totalQty = trolleys.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(i -> i.getSku().equals("SKU-SPLIT"))
                .mapToInt(PickItem::getQuantity)
                .sum();

        assertEquals(10, totalQty);
        assertEquals(0, plan.getSkippedItems().size());
    }

    /**
     * Test Case 12: Empty allocation rows handling
     * Expected: Empty allocation rows handled gracefully
     */
    @Test
    public void testCreatePickPlan_EmptyAllocationRows() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 10.0;
        boolean allowSplitting = false;

        List<AllocationRow> emptyAllocations = Collections.emptyList();

        // Act
        PickPlan plan = service.createPickPlan(emptyAllocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        assertNotNull("Pick plan should not be null", plan);
        assertTrue("Trolley list should be empty", plan.getTrolleys().isEmpty());
        assertEquals("Total weight should be zero", 0.0, plan.getTotalWeight(), 0.01);
        assertEquals("Weight utilization should be zero", 0.0, plan.getWeightUtilization(), 0.01);
        assertTrue("Skipped items list should be empty", plan.getSkippedItems().isEmpty());
    }

    /**
     * Test Case 13: Zero trolley capacity handling
     * Expected: Zero trolley capacity handled appropriately
     */
    @Test
    public void testCreatePickPlan_ZeroTrolleyCapacity() {
        // Arrange
        PickingService service = new PickingService();
        double trolleyCapacity = 0.0;
        boolean allowSplitting = false;

        AllocationRow row1 = new AllocationRow("ORD1", 1, "SKU-A", 2, "BOX1", 1, 1); // weight 2
        AllocationRow row2 = new AllocationRow("ORD1", 2, "SKU-B", 3, "BOX2", 1, 2); // weight 3

        List<AllocationRow> allocations = Arrays.asList(row1, row2);

        // Act
        PickPlan plan = service.createPickPlan(allocations, PackingHeuristic.FIRST_FIT, trolleyCapacity, allowSplitting);

        // Assert
        // No trolleys should be created
        assertTrue("No trolleys should be created", plan.getTrolleys().isEmpty());

        // All items should be skipped
        List<String> skipped = plan.getSkippedItems();
        assertEquals(2, skipped.size());
        assertTrue(skipped.get(0).contains("SKU-A"));
        assertTrue(skipped.get(1).contains("SKU-B"));

        // Metrics should reflect an empty plan
        assertEquals(0.0, plan.getTotalWeight(), 0.01);
        assertEquals(0.0, plan.getWeightUtilization(), 0.01);
    }
}
