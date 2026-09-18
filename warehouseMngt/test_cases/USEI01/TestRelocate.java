package USEI01;

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
 * Test cases for InventoryService.relocate() method
 * 
 * Tests relocation operation:
 * - Updates box's warehouseId/aisle/bay
 * - Inserts box in new bay's FEFO position
 * - Maintains expiryDate and receivedAt unchanged
 */
public class TestRelocate {
    
    /**
     * Test Case 1: Relocate within same warehouse
     * Expected: Box location updated, inserted in new bay's FEFO position
     */
    @Test
    public void testRelocate_WithinSameWarehouse() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int sourceBay = 100;
        int targetBay = 101;
        String sku = "SKU_RELOC";

        // Load item master data
        Item item = new Item(sku, "Relocatable Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacities
        service.defineBayCapacity(warehouseId, aisle, sourceBay, 10);
        service.defineBayCapacity(warehouseId, aisle, targetBay, 10);

        LocalDateTime now = LocalDateTime.now();

        // Insert box into source bay
        Box boxToMove = new Box("BX-MOVE", sku, LocalDate.of(2025, 12, 1), now.minusDays(1), 5, warehouseId, aisle, sourceBay);
        service.insertBox(boxToMove);

        // Insert another box into target bay with earlier expiry
        Box boxExisting = new Box("BX-EXIST", sku, LocalDate.of(2025, 11, 1), now.minusDays(2), 5, warehouseId, aisle, targetBay);
        service.insertBox(boxExisting);

        // Relocate box to target bay
        service.relocate("BX-MOVE", warehouseId, aisle, targetBay);

        // Verify source bay no longer contains the box
        Bay source = service.getOrCreateBay(warehouseId, aisle, sourceBay);
        boolean stillInSource = source.getBoxes().stream()
                .anyMatch(b -> b.getBoxId().equals("BX-MOVE"));
        assertFalse(stillInSource);

        // Verify target bay contains both boxes in FEFO order
        Bay target = service.getOrCreateBay(warehouseId, aisle, targetBay);
        List<Box> boxes = target.getBoxes();
        assertEquals(2, boxes.size());
        assertEquals("BX-EXIST", boxes.get(0).getBoxId()); // earlier expiry
        assertEquals("BX-MOVE", boxes.get(1).getBoxId());  // later expiry
    }

    /**
     * Test Case 2: Relocate to different warehouse
     * Expected: Box warehouseId updated, inserted in new bay's FEFO position
     */
    @Test
    public void testRelocate_DifferentWarehouse() {
        InventoryService service = new InventoryService();
        String sourceWarehouse = "W1";
        String targetWarehouse = "W2";
        int aisle = 1;
        int sourceBay = 100;
        int targetBay = 101;
        String sku = "SKU_RELOC2";

        // Load item master data into both warehouses
        Item item = new Item(sku, "Relocatable Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(sourceWarehouse, List.of(item));
        service.loadItemsForWarehouse(targetWarehouse, List.of(item));

        // Define bay capacities
        service.defineBayCapacity(sourceWarehouse, aisle, sourceBay, 10);
        service.defineBayCapacity(targetWarehouse, aisle, targetBay, 10);

        LocalDateTime now = LocalDateTime.now();

        // Insert box into source warehouse
        Box boxToMove = new Box("BX-MOVE", sku, LocalDate.of(2025, 12, 1), now.minusDays(1), 5, sourceWarehouse, aisle, sourceBay);
        service.insertBox(boxToMove);

        // Insert another box into target warehouse with earlier expiry
        Box boxExisting = new Box("BX-EXIST", sku, LocalDate.of(2025, 11, 1), now.minusDays(2), 5, targetWarehouse, aisle, targetBay);
        service.insertBox(boxExisting);

        // Relocate box to target warehouse
        service.relocate("BX-MOVE", targetWarehouse, aisle, targetBay);

        // Verify source bay no longer contains the box
        Bay source = service.getOrCreateBay(sourceWarehouse, aisle, sourceBay);
        boolean stillInSource = source.getBoxes().stream()
                .anyMatch(b -> b.getBoxId().equals("BX-MOVE"));
        assertFalse(stillInSource);

        // Verify target bay contains both boxes in FEFO order
        Bay target = service.getOrCreateBay(targetWarehouse, aisle, targetBay);
        List<Box> boxes = target.getBoxes();
        assertEquals(2, boxes.size());
        assertEquals("BX-EXIST", boxes.get(0).getBoxId()); // earlier expiry
        assertEquals("BX-MOVE", boxes.get(1).getBoxId());  // later expiry

        // Verify warehouseId was updated
        Box relocatedBox = boxes.get(1);
        assertEquals(targetWarehouse, relocatedBox.getWarehouseId());
    }

    /**
     * Test Case 3: FEFO position in destination bay
     * Expected: Relocated box inserted in correct FEFO position in destination bay
     */
    @Test
    public void testRelocate_FefoPositionInDestinationBay() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int sourceBay = 100;
        int destinationBay = 101;
        String sku = "SKU_FEFO_RELOC";

        // Load item master data
        Item item = new Item(sku, "FEFO Relocatable Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacities
        service.defineBayCapacity(warehouseId, aisle, sourceBay, 10);
        service.defineBayCapacity(warehouseId, aisle, destinationBay, 10);

        LocalDateTime now = LocalDateTime.now();

        // Insert box to be relocated (later expiry)
        Box boxToRelocate = new Box("BX-RELOC", sku, LocalDate.of(2025, 12, 10), now.minusDays(1), 5, warehouseId, aisle, sourceBay);
        service.insertBox(boxToRelocate);

        // Insert two boxes into destination bay with earlier expiry dates
        Box box1 = new Box("BX1", sku, LocalDate.of(2025, 10, 10), now.minusDays(3), 5, warehouseId, aisle, destinationBay);
        Box box2 = new Box("BX2", sku, LocalDate.of(2025, 11, 10), now.minusDays(2), 5, warehouseId, aisle, destinationBay);
        service.insertBox(box1);
        service.insertBox(box2);

        // Relocate box to destination bay
        service.relocate("BX-RELOC", warehouseId, aisle, destinationBay);

        // Verify source bay no longer contains the box
        Bay source = service.getOrCreateBay(warehouseId, aisle, sourceBay);
        boolean stillInSource = false;
        for (Box b : source.getBoxes()) {
            if (b.getBoxId().equals("BX-RELOC")) {
                stillInSource = true;
                break;
            }
        }
        assertFalse(stillInSource);

        // Verify destination bay contains all boxes in correct FEFO order
        Bay destination = service.getOrCreateBay(warehouseId, aisle, destinationBay);
        List<Box> boxes = destination.getBoxes();
        assertEquals(3, boxes.size());
        assertEquals("BX1", boxes.get(0).getBoxId());      // earliest expiry
        assertEquals("BX2", boxes.get(1).getBoxId());      // middle expiry
        assertEquals("BX-RELOC", boxes.get(2).getBoxId()); // latest expiry
    }

    /**
     * Test Case 4: ExpiryDate and receivedAt unchanged
     * Expected: Box's expiryDate and receivedAt remain unchanged after relocation
     */
    @Test
    public void testRelocate_ExpiryAndReceivedUnchanged() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int sourceBay = 100;
        int targetBay = 101;
        String sku = "SKU_RELOC_TIME";

        // Load item master data
        Item item = new Item(sku, "Timestamped Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacities
        service.defineBayCapacity(warehouseId, aisle, sourceBay, 10);
        service.defineBayCapacity(warehouseId, aisle, targetBay, 10);

        // Create box with known expiry and receivedAt
        LocalDate expiryDate = LocalDate.of(2025, 12, 15);
        LocalDateTime receivedAt = LocalDateTime.of(2025, 10, 20, 14, 30);
        Box box = new Box("BX-TIME", sku, expiryDate, receivedAt, 5, warehouseId, aisle, sourceBay);
        service.insertBox(box);

        // Relocate box to target bay
        service.relocate("BX-TIME", warehouseId, aisle, targetBay);

        // Verify box is no longer in source bay
        Bay source = service.getOrCreateBay(warehouseId, aisle, sourceBay);
        boolean stillInSource = false;
        for (Box b : source.getBoxes()) {
            if (b.getBoxId().equals("BX-TIME")) {
                stillInSource = true;
                break;
            }
        }
        assertFalse(stillInSource);

        // Verify box is in target bay with unchanged expiryDate and receivedAt
        Bay target = service.getOrCreateBay(warehouseId, aisle, targetBay);
        Box relocatedBox = null;
        for (Box b : target.getBoxes()) {
            if (b.getBoxId().equals("BX-TIME")) {
                relocatedBox = b;
                break;
            }
        }

        assertNotNull(relocatedBox);
        assertEquals(expiryDate, relocatedBox.getExpiryDate());
        assertEquals(receivedAt, relocatedBox.getReceivedAt());
    }

    /**
     * Test Case 5: Non-existent box handling
     * Expected: Exception thrown when trying to relocate non-existent box
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRelocate_NonExistentBox() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int targetBay = 101;
        String nonExistentBoxId = "BX-NOT-FOUND";

        // Define bay capacity (target bay must exist)
        service.defineBayCapacity(warehouseId, aisle, targetBay, 10);

        // Attempt to relocate a box that doesn't exist
        service.relocate(nonExistentBoxId, warehouseId, aisle, targetBay);
    }

    /**
     * Test Case 6: Invalid destination handling
     * Expected: Exception thrown when trying to relocate to invalid destination
     */
    @Test(expected = IllegalStateException.class)
    public void testRelocate_InvalidDestination() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int sourceBay = 100;
        int invalidTargetBay = 999;
        String sku = "SKU_INVALID_DEST";

        // Load item master data
        Item item = new Item(sku, "Invalid Destination Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define only source bay
        service.defineBayCapacity(warehouseId, aisle, sourceBay, 10);

        // Insert box into source bay
        LocalDateTime now = LocalDateTime.now();
        Box box = new Box("BX-INVALID", sku, LocalDate.of(2025, 12, 1), now.minusDays(1), 5, warehouseId, aisle, sourceBay);
        service.insertBox(box);

        // Attempt to relocate to undefined bay (should throw)
        service.relocate("BX-INVALID", warehouseId, aisle, invalidTargetBay);
    }
}
