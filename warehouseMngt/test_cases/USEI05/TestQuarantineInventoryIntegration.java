package USEI05;

import main.controller.InventoryService;
import main.controller.QuarantineService;
import main.domain.*;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.io.IOException;
import java.nio.file.*;

/**
 * Test cases for Quarantine-Inventory integration
 * 
 * Tests integration between quarantine and inventory systems:
 * - Restocked returns create proper boxes in inventory
 * - FEFO ordering maintained after restocking
 * - Bay capacity enforcement
 * - Global inventory reordering after quarantine processing
 */
public class TestQuarantineInventoryIntegration {
    
    /**
     * Test Case 1: Restock return creates box with RET- prefix
     * Expected: Restocked return creates box with boxId starting with "RET-"
     */
    @Test
    public void testRestock_CreatesRetBox() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log"; // dummy path for testing
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // First return: eligible for restock
        Return return1 = new Return(
                "R001",
                "SKU123",
                3,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now().minusDays(2),
                LocalDate.now().plusDays(10)
        );

        quarantineService.addToQuarantine(return1);
        List<InspectionResult> results1 = quarantineService.processQuarantine(warehouseId, aisle);

        assertEquals(1, results1.size());
        InspectionResult result1 = results1.get(0);
        assertEquals(InspectionAction.RESTOCKED, result1.getAction());
        assertTrue("Box ID should start with RET-", inventoryService.boxExists("RET-R001"));

        // Second return: also eligible for restock, different ID
        Return return2 = new Return(
                "R002",
                "SKU123",
                2,
                ReturnReason.CYCLE_COUNT,
                LocalDateTime.now().minusDays(1),
                LocalDate.now().plusDays(15)
        );

        quarantineService.addToQuarantine(return2);
        List<InspectionResult> results2 = quarantineService.processQuarantine(warehouseId, aisle);

        assertEquals(1, results2.size());
        InspectionResult result2 = results2.get(0);
        assertEquals(InspectionAction.RESTOCKED, result2.getAction());
        assertTrue("Box ID should start with RET-", inventoryService.boxExists("RET-R002"));
    }


    /**
     * Test Case 2: Restocked box uses return timestamp as receivedAt
     *  Expected: Restocked box uses return timestamp, not current time
     */
    @Test
    public void testRestock_UsesReturnTimestamp() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define a fixed timestamp for the return
        LocalDateTime returnTimestamp = LocalDateTime.of(2025, 10, 1, 14, 30);

        // Create return eligible for restock
        Return returnItem = new Return(
                "R003",
                "SKU123",
                4,
                ReturnReason.CUSTOMER_REMORSE,
                returnTimestamp,
                LocalDate.now().plusDays(20)
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: find the box and check its receivedAt timestamp
        Warehouse warehouse = inventoryService.getAllWarehouses().get(warehouseId);
        Bay targetBay = warehouse.getAisles().get(aisle).get(bay);
        Box restockedBox = targetBay.getBoxes().stream()
                .filter(b -> b.getBoxId().equals("RET-R003"))
                .findFirst()
                .orElse(null);

        assertNotNull("Restocked box should exist", restockedBox);
        assertEquals("Restocked box should use return timestamp as receivedAt",
                returnTimestamp, restockedBox.getReceivedAt());
    }


    /**
     * Test Case 3: Restocked box preserves expiry date
     * Expected: Restocked box uses return's expiry date
     */
    @Test
    public void testRestock_PreservesExpiryDate() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Define expiry date
        LocalDate expectedExpiry = LocalDate.of(2025, 12, 31);

        // Create return eligible for restock
        Return returnItem = new Return(
                "R004",
                "SKU123",
                4,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now().minusDays(1),
                expectedExpiry
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: find the box and check its expiry date
        Warehouse warehouse = inventoryService.getAllWarehouses().get(warehouseId);
        Bay targetBay = warehouse.getAisles().get(aisle).get(bay);
        Box restockedBox = targetBay.getBoxes().stream()
                .filter(b -> b.getBoxId().equals("RET-R004"))
                .findFirst()
                .orElse(null);

        assertNotNull("Restocked box should exist", restockedBox);
        assertEquals("Restocked box should preserve expiry date",
                expectedExpiry, restockedBox.getExpiryDate());
    }

    /**
     * Test Case 4: Restocked box with null expiry date
     * Expected: Restocked box has null expiry date when return has null expiry
     */
    @Test
    public void testRestock_NullExpiryDate() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create return with null expiry date
        Return returnItem = new Return(
                "R005",
                "SKU123",
                4,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now().minusDays(1),
                null // expiryDate is null
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: find the box and check its expiry date
        Warehouse warehouse = inventoryService.getAllWarehouses().get(warehouseId);
        Bay targetBay = warehouse.getAisles().get(aisle).get(bay);
        Box restockedBox = targetBay.getBoxes().stream()
                .filter(b -> b.getBoxId().equals("RET-R005"))
                .findFirst()
                .orElse(null);

        assertNotNull("Restocked box should exist", restockedBox);
        assertNull("Restocked box should have null expiry date", restockedBox.getExpiryDate());
    }

    /**
     * Test Case 5: Bay capacity enforcement
     * Expected: Restocked items respect bay capacity limits
     */
    @Test
    public void testRestock_BayCapacityEnforcement() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity to allow only 1 box
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 1);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // First return: should succeed
        Return return1 = new Return(
                "R006",
                "SKU123",
                3,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now().minusDays(1),
                LocalDate.now().plusDays(10)
        );

        quarantineService.addToQuarantine(return1);
        List<InspectionResult> results1 = quarantineService.processQuarantine(warehouseId, aisle);

        assertEquals(1, results1.size());
        InspectionResult result1 = results1.get(0);
        assertEquals(InspectionAction.RESTOCKED, result1.getAction());
        assertTrue("First box should exist", inventoryService.boxExists("RET-R006"));

        // Second return: should fail due to bay capacity
        Return return2 = new Return(
                "R007",
                "SKU123",
                2,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now(),
                LocalDate.now().plusDays(15)
        );

        quarantineService.addToQuarantine(return2);
        List<InspectionResult> results2 = quarantineService.processQuarantine(warehouseId, aisle);

        assertEquals(1, results2.size());
        InspectionResult result2 = results2.get(0);
        assertEquals("Second return should be discarded due to lack of space",
                InspectionAction.DISCARDED, result2.getAction());
        assertFalse("Second box should not exist", inventoryService.boxExists("RET-R007"));
    }

    /**
     * Test Case 6: No available bay for restock
     * Expected: Error when no bay has capacity for restock
     */
    @Test
    public void testRestock_NoAvailableBay() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;

        // Não definimos nenhum bay — simula ausência total de bays disponíveis
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Cria devolução válida para reabastecimento
        Return returnItem = new Return(
                "R008",
                "SKU123",
                5,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now().minusDays(1),
                LocalDate.now().plusDays(10)
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert
        assertEquals(1, results.size());
        InspectionResult result = results.get(0);
        assertEquals("Return should be discarded due to no available bay",
                InspectionAction.DISCARDED, result.getAction());
        assertFalse("Box should not exist since no bay was available",
                inventoryService.boxExists("RET-R008"));
    }

    /**
     * Test Case 7: FEFO ordering after restock
     * Expected: Restocked items participate in global FEFO ordering
     */
    @Test
    public void testRestock_FefoOrdering() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create two returns with different expiry dates
        Return return1 = new Return(
                "R009",
                "SKU123",
                3,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 1, 10, 0),
                LocalDate.of(2025, 12, 31)
        );

        Return return2 = new Return(
                "R010",
                "SKU123",
                3,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 2, 10, 0),
                LocalDate.of(2025, 11, 30) // expires earlier
        );

        // Act: process both returns
        quarantineService.addToQuarantine(return1);
        quarantineService.addToQuarantine(return2);
        quarantineService.processQuarantine(warehouseId, aisle);

        // Reorder inventory globally
        inventoryService.reorderInventoryFefo();

        // Assert: box with earlier expiry should come first
        Warehouse warehouse = inventoryService.getAllWarehouses().get(warehouseId);
        Bay targetBay = warehouse.getAisles().get(aisle).get(bay);
        List<Box> boxes = targetBay.getBoxes();

        assertEquals(2, boxes.size());
        assertEquals("RET-R010", boxes.get(0).getBoxId()); // expires earlier
        assertEquals("RET-R009", boxes.get(1).getBoxId()); // expires later
    }

    /**
     * Test Case 8: Multiple returns processing
     * Expected: Multiple returns processed in quarantine order
     */
    @Test
    public void testRestock_MultipleReturns() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 10);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create two returns with different timestamps
        Return olderReturn = new Return(
                "R011",
                "SKU123",
                2,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 1, 10, 0),
                LocalDate.of(2025, 12, 31)
        );

        Return newerReturn = new Return(
                "R012",
                "SKU123",
                3,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 2, 10, 0),
                LocalDate.of(2025, 12, 31)
        );

        // Add both to quarantine (older first, newer second)
        quarantineService.addToQuarantine(olderReturn);
        quarantineService.addToQuarantine(newerReturn);

        // Act
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: processed in reverse order of arrival (newer first)
        assertEquals(2, results.size());
        assertEquals("R012", results.get(0).getReturnId()); // newer
        assertEquals("R011", results.get(1).getReturnId()); // older
        assertEquals(InspectionAction.RESTOCKED, results.get(0).getAction());
        assertEquals(InspectionAction.RESTOCKED, results.get(1).getAction());
    }

    /**
     * Test Case 9: Mixed restock and discard
     * Expected: Some returns restocked, others discarded
     */
    @Test
    public void testQuarantine_MixedActions() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create one return eligible for restock
        Return restockable = new Return(
                "R013",
                "SKU123",
                3,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now().minusDays(1),
                LocalDate.now().plusDays(10)
        );

        // Create one return that must be discarded
        Return discardable = new Return(
                "R014",
                "SKU123",
                2,
                ReturnReason.DAMAGED,
                LocalDateTime.now().minusHours(5),
                LocalDate.now().plusDays(10)
        );

        // Add both to quarantine
        quarantineService.addToQuarantine(restockable);
        quarantineService.addToQuarantine(discardable);

        // Act
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert
        assertEquals(2, results.size());

        InspectionResult result1 = results.get(0);
        InspectionResult result2 = results.get(1);

        // Check that one was restocked and one discarded
        boolean foundRestocked = result1.getAction() == InspectionAction.RESTOCKED || result2.getAction() == InspectionAction.RESTOCKED;
        boolean foundDiscarded = result1.getAction() == InspectionAction.DISCARDED || result2.getAction() == InspectionAction.DISCARDED;

        assertTrue("At least one return should be restocked", foundRestocked);
        assertTrue("At least one return should be discarded", foundDiscarded);

        // Check box existence only for restocked return
        assertTrue("Restocked box should exist", inventoryService.boxExists("RET-R013"));
        assertFalse("Discarded box should not exist", inventoryService.boxExists("RET-R014"));
    }

    /**
     * Test Case 10: Global FEFO reordering after quarantine processing
     * Expected: Global FEFO reordering maintains proper order across all boxes
     */
    @Test
    public void testGlobalFefo_AfterQuarantineProcessing() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 10);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create three returns with different expiry and received timestamps
        Return r1 = new Return("R015", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 1, 10, 0), LocalDate.of(2025, 12, 31));
        Return r2 = new Return("R016", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 2, 10, 0), LocalDate.of(2025, 11, 30)); // expires earlier
        Return r3 = new Return("R017", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 3, 10, 0), LocalDate.of(2025, 12, 15)); // middle expiry

        // Add all to quarantine
        quarantineService.addToQuarantine(r1);
        quarantineService.addToQuarantine(r2);
        quarantineService.addToQuarantine(r3);

        // Process quarantine and reorder inventory
        quarantineService.processQuarantine(warehouseId, aisle);
        inventoryService.reorderInventoryFefo();

        // Assert: boxes should be ordered by expiry date ascending
        Warehouse warehouse = inventoryService.getAllWarehouses().get(warehouseId);
        Bay targetBay = warehouse.getAisles().get(aisle).get(bay);
        List<Box> boxes = targetBay.getBoxes();

        assertEquals(3, boxes.size());
        assertEquals("RET-R016", boxes.get(0).getBoxId()); // expires earliest
        assertEquals("RET-R017", boxes.get(1).getBoxId()); // middle expiry
        assertEquals("RET-R015", boxes.get(2).getBoxId()); // expires latest
    }

    /**
     * Test Case 11: Audit logging for restocked items
     * Expected: Restocked items are logged to audit file
     */
    @Test
    public void testAuditLogging_RestockedItems() throws IOException {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        Path auditPath = Files.createTempFile("audit-log", ".txt");
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditPath.toString());

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        Return returnItem = new Return(
                "R018",
                "SKU123",
                2,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now().minusDays(1),
                LocalDate.now().plusDays(10)
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: audit file should contain log entry for returnId=R018
        List<String> lines = Files.readAllLines(auditPath);
        System.out.println("Audit log contents:");
        lines.forEach(System.out::println);

        boolean found = lines.stream().anyMatch(line -> line.contains("returnId=R018"));
        assertTrue("Audit log should contain entry for returnId=R018", found);

        // Cleanup
        Files.deleteIfExists(auditPath);
    }


    /**
     * Test Case 12: Audit logging for discarded items
     * Expected: Discarded items are logged to audit file
     */
    @Test
    public void testAuditLogging_DiscardedItems() throws IOException {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        Path auditPath = Files.createTempFile("audit-log", ".txt");
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditPath.toString());

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity and load known SKU
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create a return that must be discarded
        Return returnItem = new Return(
                "R019",
                "SKU123",
                2,
                ReturnReason.DAMAGED,
                LocalDateTime.now().minusHours(2),
                LocalDate.now().plusDays(10)
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: audit file should contain log entry for returnId=R019 and action=DISCARDED
        List<String> lines = Files.readAllLines(auditPath);
        System.out.println("Audit log contents:");
        lines.forEach(System.out::println);

        boolean found = lines.stream().anyMatch(line ->
                line.contains("returnId=R019") && line.contains("action=DISCARDED"));
        assertTrue("Audit log should contain discarded entry for returnId=R019", found);

        // Cleanup
        Files.deleteIfExists(auditPath);
    }

    /**
     * Test Case 13: Partial restock with audit logging
     * Expected: Partial restocks are logged with correct quantities
     */
    @Test
    public void testAuditLogging_PartialRestock() throws IOException {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        Path auditPath = Files.createTempFile("audit-log", ".txt");
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditPath.toString());

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        // Define bay capacity to allow only 2 boxes (not units)
        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 2);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create return with 5 units (fits in one box)
        Return returnItem = new Return(
                "R020",
                "SKU123",
                5,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.now().minusHours(1),
                LocalDate.now().plusDays(10)
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: audit file should contain correct restocked quantity
        List<String> lines = Files.readAllLines(auditPath);
        System.out.println("Audit log contents:");
        lines.forEach(System.out::println);

        boolean found = lines.stream().anyMatch(line ->
                line.contains("returnId=R020") &&
                        line.contains("action=RESTOCKED") &&
                        line.contains("qty=5")); // all units accepted

        assertTrue("Audit log should reflect full restock of 5 units for returnId=R020", found);

        // Cleanup
        Files.deleteIfExists(auditPath);
    }

    /**
     * Test Case 14: Return processing order in quarantine
     * Expected: Returns processed in reverse chronological order
     */
    @Test
    public void testQuarantine_ProcessingOrder() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create three returns with increasing timestamps
        Return r1 = new Return("R021", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 1, 9, 0), LocalDate.of(2025, 12, 31));
        Return r2 = new Return("R022", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 1, 10, 0), LocalDate.of(2025, 12, 31));
        Return r3 = new Return("R023", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 1, 11, 0), LocalDate.of(2025, 12, 31));

        // Add to quarantine in chronological order
        quarantineService.addToQuarantine(r1);
        quarantineService.addToQuarantine(r2);
        quarantineService.addToQuarantine(r3);

        // Act
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: processed in reverse order of arrival
        assertEquals(3, results.size());
        assertEquals("R023", results.get(0).getReturnId()); // newest
        assertEquals("R022", results.get(1).getReturnId()); // middle
        assertEquals("R021", results.get(2).getReturnId()); // oldest
    }

    /**
     * Test Case 15: Return processing with same timestamp
     * Expected: Returns with same timestamp processed by returnId ASC
     */
    @Test
    public void testQuarantine_SameTimestampOrder() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;
        int bay = 1;

        inventoryService.defineBayCapacity(warehouseId, aisle, bay, 5);
        Item item = new Item("SKU123", "Produto Teste", "Medicamentos", "Unidade", 0.5, 0.2);
        inventoryService.loadItemsForWarehouse(warehouseId, List.of(item));

        // Create three returns with same timestamp
        LocalDateTime sameTime = LocalDateTime.of(2025, 10, 1, 10, 0);
        Return rA = new Return("R030", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE, sameTime, LocalDate.of(2025, 12, 31));
        Return rB = new Return("R031", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE, sameTime, LocalDate.of(2025, 12, 31));
        Return rC = new Return("R032", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE, sameTime, LocalDate.of(2025, 12, 31));

        // Add in shuffled order
        quarantineService.addToQuarantine(rC);
        quarantineService.addToQuarantine(rA);
        quarantineService.addToQuarantine(rB);

        // Act
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: processed in returnId ascending order
        assertEquals(3, results.size());
        assertEquals("R030", results.get(0).getReturnId()); // A
        assertEquals("R031", results.get(1).getReturnId()); // B
        assertEquals("R032", results.get(2).getReturnId()); // C
    }
}
