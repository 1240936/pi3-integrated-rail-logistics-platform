package test_cases.USEI01;

import main.controller.InventoryService;
import main.domain.Bay;
import main.domain.Box;
import main.domain.Item;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for InventoryService.dispatch() method
 * 
 * Tests dispatch operation following FEFO/FIFO behavior:
 * - Always consumes stock from "front" of bay list
 * - Supports partial dispatch across multiple bays
 * - Continues in next bay with ascending number when target bay runs out
 * - Only deletes empty boxes, not bays
 */
public class TestDispatch {
    
    /**
     * Test Case 1: Dispatch from single bay
     * Expected: Dispatch consumes from front of bay list (FEFO order)
     */
    @Test
    public void testDispatch_SingleBay() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU123";

        // Load item master data
        Item item = new Item(sku, "Test Item", "Food", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        // Insert boxes with different expiry dates (FEFO order should prioritize earliest expiry)
        LocalDateTime now = LocalDateTime.now();
        Box box1 = new Box("B1", sku, LocalDate.of(2025, 12, 1), now.minusDays(3), 5, warehouseId, aisle, bayNumber);
        Box box2 = new Box("B2", sku, LocalDate.of(2025, 11, 1), now.minusDays(2), 5, warehouseId, aisle, bayNumber);
        Box box3 = new Box("B3", sku, LocalDate.of(2025, 10, 1), now.minusDays(1), 5, warehouseId, aisle, bayNumber);

        service.insertBox(box1);
        service.insertBox(box2);
        service.insertBox(box3);

        // Dispatch 5 units
        int dispatched = service.dispatch(warehouseId, sku, aisle, 5);

        // Verify dispatch count
        assertEquals(5, dispatched);

        // Verify FEFO: box3 (earliest expiry) should be consumed first
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        List<Box> remainingBoxes = bay.getBoxes();

        // box3 should now be empty or removed
        boolean box3StillPresent = remainingBoxes.stream()
                .anyMatch(b -> b.getBoxId().equals("B3") && b.getQuantity() > 0);

        assertFalse(box3StillPresent);
    }

    /**
     * Test Case 2: Partial dispatch across multiple bays
     * Expected: When target bay runs out, continue in next bay (ascending number)
     */
    @Test
    public void testDispatch_PartialAcrossMultipleBays() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        String sku = "SKU456";

        // Load item master data
        Item item = new Item(sku, "MultiBay Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacities
        service.defineBayCapacity(warehouseId, aisle, 100, 10);
        service.defineBayCapacity(warehouseId, aisle, 101, 10);

        LocalDateTime now = LocalDateTime.now();

        // Bay 100: only 3 units
        Box boxA = new Box("BA", sku, LocalDate.of(2025, 10, 10), now.minusDays(3), 3, warehouseId, aisle, 100);
        service.insertBox(boxA);

        // Bay 101: 5 units
        Box boxB = new Box("BB", sku, LocalDate.of(2025, 10, 15), now.minusDays(2), 5, warehouseId, aisle, 101);
        service.insertBox(boxB);

        // Dispatch 6 units (more than bay 100 can provide)
        int dispatched = service.dispatch(warehouseId, sku, aisle, 6);

        // Verify total dispatched
        assertEquals(6, dispatched);

        // Verify bay 100 is empty
        Bay bay100 = service.getOrCreateBay(warehouseId, aisle, 100);
        boolean boxAExists = false;
        for (Box b : bay100.getBoxes()) {
            if (b.getBoxId().equals("BA") && b.getQuantity() > 0) {
                boxAExists = true;
                break;
            }
        }
        assertFalse(boxAExists);

        // Verify bay 101 has 2 units remaining
        Bay bay101 = service.getOrCreateBay(warehouseId, aisle, 101);
        Box boxBRemaining = null;
        for (Box b : bay101.getBoxes()) {
            if (b.getBoxId().equals("BB")) {
                boxBRemaining = b;
                break;
            }
        }
        assertNotNull(boxBRemaining);
        assertEquals(2, boxBRemaining.getQuantity());
    }


    /**
     * Test Case 3: Empty bay handling
     * Expected: Empty bays remain in WMS with empty box list
     */
    @Test
    public void testDispatch_EmptyBayRemains() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU789";

        // Load item master data
        Item item = new Item(sku, "EmptyBay Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        // Insert a single box
        LocalDateTime now = LocalDateTime.now();
        Box box = new Box("BX", sku, LocalDate.of(2025, 10, 20), now.minusDays(1), 5, warehouseId, aisle, bayNumber);
        service.insertBox(box);

        // Dispatch all units
        int dispatched = service.dispatch(warehouseId, sku, aisle, 5);
        assertEquals(5, dispatched);

        // Verify bay still exists
        boolean bayExists = service.bayExists(warehouseId, aisle, bayNumber);
        assertTrue(bayExists);

        // Verify bay is empty
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        assertTrue(bay.getBoxes().isEmpty());
    }

    /**
     * Test Case 4: Empty box deletion
     * Expected: Only empty boxes are deleted, not bays
     */
    @Test
    public void testDispatch_EmptyBoxDeletion() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU999";

        // Load item master data
        Item item = new Item(sku, "Disposable Box Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        // Insert a single box with 5 units
        LocalDateTime now = LocalDateTime.now();
        Box box = new Box("BX1", sku, LocalDate.of(2025, 11, 15), now.minusDays(1), 5, warehouseId, aisle, bayNumber);
        service.insertBox(box);

        // Dispatch all 5 units
        int dispatched = service.dispatch(warehouseId, sku, aisle, 5);
        assertEquals(5, dispatched);

        // Verify bay still exists
        assertTrue(service.bayExists(warehouseId, aisle, bayNumber));

        // Verify box is deleted (not present in bay)
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        boolean boxStillPresent = bay.getBoxes().stream()
                .anyMatch(b -> b.getBoxId().equals("BX1"));
        assertFalse(boxStillPresent);

        // Verify bay is empty
        assertTrue(bay.getBoxes().isEmpty());
    }

    /**
     * Test Case 5: Insufficient stock handling
     * Expected: Dispatch returns actual dispatched quantity when stock is insufficient
     */
    @Test
    public void testDispatch_InsufficientStock() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU321";

        // Load item master data
        Item item = new Item(sku, "Limited Stock Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        // Insert a single box with 4 units
        LocalDateTime now = LocalDateTime.now();
        Box box = new Box("BX-LIMIT", sku, LocalDate.of(2025, 12, 1), now.minusDays(1), 4, warehouseId, aisle, bayNumber);
        service.insertBox(box);

        // Attempt to dispatch 10 units (more than available)
        int dispatched = service.dispatch(warehouseId, sku, aisle, 10);

        // Verify only 4 units were dispatched
        assertEquals(4, dispatched);

        // Verify bay still exists
        assertTrue(service.bayExists(warehouseId, aisle, bayNumber));

        // Verify box is removed (since it should be empty)
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        boolean boxStillPresent = bay.getBoxes().stream()
                .anyMatch(b -> b.getBoxId().equals("BX-LIMIT"));
        assertFalse(boxStillPresent);
    }

    /**
     * Test Case 6: Non-existent SKU handling
     * Expected: Dispatch returns 0 for non-existent SKU
     */
    @Test
    public void testDispatch_NonExistentSku() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String knownSku = "SKU111";
        String unknownSku = "SKU999";

        // Load only one known item
        Item item = new Item(knownSku, "Known Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity and insert box for known SKU
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);
        LocalDateTime now = LocalDateTime.now();
        Box box = new Box("BX-KNOWN", knownSku, LocalDate.of(2025, 12, 1), now.minusDays(1), 5, warehouseId, aisle, bayNumber);
        service.insertBox(box);

        // Attempt to dispatch unknown SKU
        int dispatched = service.dispatch(warehouseId, unknownSku, aisle, 5);

        // Verify that nothing was dispatched
        assertEquals(0, dispatched);

        // Verify that the known box is still intact
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        Box foundBox = null;
        for (Box b : bay.getBoxes()) {
            if (b.getBoxId().equals("BX-KNOWN")) {
                foundBox = b;
                break;
            }
        }

        assertNotNull(foundBox);
        assertEquals(5, foundBox.getQuantity());
    }

}
