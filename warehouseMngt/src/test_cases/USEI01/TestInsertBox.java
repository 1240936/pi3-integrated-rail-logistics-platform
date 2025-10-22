package test_cases.USEI01;

import main.controller.InventoryService;
import main.domain.Bay;
import main.domain.Box;
import main.domain.Item;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for InventoryService.insertBox() method
 * 
 * Tests FEFO ordering logic for box insertion:
 * 1. Expiry date (earliest first; null last)
 * 2. Received date (oldest first)
 * 3. Box ID ASC (tie-break)
 */
public class TestInsertBox {
    
    /**
     * Test Case 1: Boxes with different expiry dates
     * Expected: Boxes ordered by expiry date (earliest first)
     */
    @Test
    public void testInsertBox_DifferentExpiryDates() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKUEXP";

        // Load item master data
        Item item = new Item(sku, "Expiry Item", "Perishable", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        LocalDateTime now = LocalDateTime.now();

        // Insert boxes with different expiry dates
        Box box1 = new Box("BX1", sku, LocalDate.of(2025, 12, 1), now.minusDays(3), 5, warehouseId, aisle, bayNumber);
        Box box2 = new Box("BX2", sku, LocalDate.of(2025, 11, 1), now.minusDays(2), 5, warehouseId, aisle, bayNumber);
        Box box3 = new Box("BX3", sku, LocalDate.of(2025, 10, 1), now.minusDays(1), 5, warehouseId, aisle, bayNumber);

        service.insertBox(box1);
        service.insertBox(box2);
        service.insertBox(box3);

        // Verify FEFO order in bay
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        List<Box> boxes = bay.getBoxes();

        assertEquals(3, boxes.size());
        assertEquals("BX3", boxes.get(0).getBoxId()); // earliest expiry
        assertEquals("BX2", boxes.get(1).getBoxId());
        assertEquals("BX1", boxes.get(2).getBoxId()); // latest expiry
    }

    /**
     * Test Case 2: Boxes with same expiry date, different receivedAt
     * Expected: Boxes ordered by receivedAt (oldest first)
     */
    @Test
    public void testInsertBox_SameExpiryDifferentReceived() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU_FEFO_RECEIVED";

        // Load item master data
        Item item = new Item(sku, "Received Order Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        LocalDate expiryDate = LocalDate.of(2025, 12, 1);

        // Insert boxes with same expiry date but different receivedAt timestamps
        Box box1 = new Box("BX1", sku, expiryDate, LocalDateTime.of(2025, 10, 1, 8, 0), 5, warehouseId, aisle, bayNumber); // oldest
        Box box2 = new Box("BX2", sku, expiryDate, LocalDateTime.of(2025, 10, 2, 8, 0), 5, warehouseId, aisle, bayNumber);
        Box box3 = new Box("BX3", sku, expiryDate, LocalDateTime.of(2025, 10, 3, 8, 0), 5, warehouseId, aisle, bayNumber); // newest

        service.insertBox(box3);
        service.insertBox(box2);
        service.insertBox(box1);

        // Verify FEFO order: oldest receivedAt first
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        List<Box> boxes = bay.getBoxes();

        assertEquals(3, boxes.size());
        assertEquals("BX1", boxes.get(0).getBoxId()); // oldest received
        assertEquals("BX2", boxes.get(1).getBoxId());
        assertEquals("BX3", boxes.get(2).getBoxId()); // newest received
    }

    /**
     * Test Case 3: Boxes with null expiry dates
     * Expected: Null expiry boxes placed at end of list
     */
    @Test
    public void testInsertBox_NullExpiryDates() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU_NULL_EXP";

        // Load item master data
        Item item = new Item(sku, "Non-expiring Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        LocalDateTime now = LocalDateTime.now();

        // Insert boxes with and without expiry dates
        Box box1 = new Box("BX1", sku, LocalDate.of(2025, 10, 1), now.minusDays(3), 5, warehouseId, aisle, bayNumber);
        Box box2 = new Box("BX2", sku, null, now.minusDays(2), 5, warehouseId, aisle, bayNumber); // no expiry
        Box box3 = new Box("BX3", sku, LocalDate.of(2025, 11, 1), now.minusDays(1), 5, warehouseId, aisle, bayNumber);

        service.insertBox(box1);
        service.insertBox(box2);
        service.insertBox(box3);

        // Verify FEFO order: box1 (earliest expiry), box3, then box2 (null expiry)
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        List<Box> boxes = bay.getBoxes();

        assertEquals(3, boxes.size());
        assertEquals("BX1", boxes.get(0).getBoxId()); // earliest expiry
        assertEquals("BX3", boxes.get(1).getBoxId()); // later expiry
        assertEquals("BX2", boxes.get(2).getBoxId()); // null expiry
    }

    /**
     * Test Case 4: Tie-breaking by boxId
     * Expected: When expiry and receivedAt are same, order by boxId ASC
     */
    @Test
    public void testInsertBox_TieBreakByBoxId() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU_TIE";

        // Load item master data
        Item item = new Item(sku, "Tie Break Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        LocalDate expiry = LocalDate.of(2025, 12, 1);
        LocalDateTime received = LocalDateTime.of(2025, 10, 1, 8, 0);

        // Insert boxes with same expiry and receivedAt, different boxIds
        Box boxB = new Box("BXB", sku, expiry, received, 5, warehouseId, aisle, bayNumber);
        Box boxA = new Box("BXA", sku, expiry, received, 5, warehouseId, aisle, bayNumber);
        Box boxC = new Box("BXC", sku, expiry, received, 5, warehouseId, aisle, bayNumber);

        service.insertBox(boxB);
        service.insertBox(boxA);
        service.insertBox(boxC);

        // Verify order: BXA, BXB, BXC
        Bay bay = service.getOrCreateBay(warehouseId, aisle, bayNumber);
        List<Box> boxes = bay.getBoxes();

        assertEquals(3, boxes.size());
        assertEquals("BXA", boxes.get(0).getBoxId());
        assertEquals("BXB", boxes.get(1).getBoxId());
        assertEquals("BXC", boxes.get(2).getBoxId());
    }


    /**
     * Test Case 5: Duplicate boxId handling
     * Expected: IllegalArgumentException thrown for duplicate boxId
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInsertBox_DuplicateBoxId() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU_DUP";

        // Load item master data
        Item item = new Item(sku, "Duplicate Box Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 10);

        LocalDateTime now = LocalDateTime.now();

        // Insert first box
        Box box1 = new Box("DUP1", sku, LocalDate.of(2025, 12, 1), now.minusDays(1), 5, warehouseId, aisle, bayNumber);
        service.insertBox(box1);

        // Attempt to insert second box with same boxId
        Box box2 = new Box("DUP1", sku, LocalDate.of(2025, 11, 1), now.minusDays(2), 5, warehouseId, aisle, bayNumber);
        service.insertBox(box2); // should throw IllegalArgumentException
    }

    /**
     * Test Case 6: Bay capacity overflow
     * Expected: Exception thrown when bay capacity is exceeded
     */
    @Test(expected = IllegalStateException.class)
    public void testInsertBox_BayCapacityOverflow() {
        InventoryService service = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;
        int bayNumber = 101;
        String sku = "SKU_OVERFLOW";

        // Load item master data
        Item item = new Item(sku, "Overflow Item", "General", "unit", 1.0, 0.5);
        service.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define bay capacity to 2 boxes
        service.defineBayCapacity(warehouseId, aisle, bayNumber, 2);

        LocalDateTime now = LocalDateTime.now();

        // Insert two boxes (within capacity)
        Box box1 = new Box("BX1", sku, LocalDate.of(2025, 12, 1), now.minusDays(1), 5, warehouseId, aisle, bayNumber);
        Box box2 = new Box("BX2", sku, LocalDate.of(2025, 11, 1), now.minusDays(2), 5, warehouseId, aisle, bayNumber);
        service.insertBox(box1);
        service.insertBox(box2);

        // Attempt to insert third box (should exceed capacity)
        Box box3 = new Box("BX3", sku, LocalDate.of(2025, 10, 1), now.minusDays(3), 5, warehouseId, aisle, bayNumber);
        service.insertBox(box3); // should throw IllegalStateException
    }

}
