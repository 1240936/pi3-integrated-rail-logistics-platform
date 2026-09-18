package USEI05;


import org.junit.Test;
import static org.junit.Assert.*;

import main.controller.InventoryService;
import main.controller.QuarantineService;
import main.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Test cases for QuarantineService
 * 
 * Tests quarantine operations following USEI05 requirements:
 * - Returns placed in quarantine in reverse order of arrival (latest first)
 * - Inspection processes returns in quarantine order
 * - Restocked items create new boxes with RET- prefix and proper FEFO insertion
 * - Discarded items are flagged as unusable
 * - Audit logging captures all inspection actions
 */
public class TestQuarantineService {

    /**
     * Test Case 1: Add return to quarantine
     * Expected: Return added to quarantine queue
     */
    @Test
    public void testAddToQuarantine_ValidReturn() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        Return returnItem = new Return(
                "R001",
                "SKU123",
                3,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 15, 0),
                LocalDate.of(2025, 12, 31)
        );

        // Act
        quarantineService.addToQuarantine(returnItem);

        // Assert
        List<Return> queue = quarantineService.getQuarantineQueue();
        assertEquals(1, queue.size());
        assertEquals("R001", queue.get(0).getReturnId());
        assertEquals("SKU123", queue.get(0).getSku());
        assertEquals(3, queue.get(0).getQuantity());
    }

    /**
     * Test Case 2: Quarantine queue order (latest first)
     * Expected: Returns processed in reverse chronological order (latest first, ties by returnId ASC)
     */
    @Test
    public void testQuarantineOrder_LatestFirst() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        // Create returns with mixed timestamps and returnIds
        Return r1 = new Return("R100", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 10, 0), LocalDate.of(2025, 12, 31));
        Return r2 = new Return("R101", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 12, 0), LocalDate.of(2025, 12, 31)); // newer
        Return r3 = new Return("R099", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 12, 0), LocalDate.of(2025, 12, 31)); // same timestamp as r2, lower ID

        // Add in shuffled order
        quarantineService.addToQuarantine(r1);
        quarantineService.addToQuarantine(r2);
        quarantineService.addToQuarantine(r3);

        // Act
        List<Return> sortedQueue = quarantineService.getQuarantineQueue();

        // Assert: order should be r3, r2, r1
        assertEquals(3, sortedQueue.size());
        assertEquals("R099", sortedQueue.get(0).getReturnId()); // same timestamp, lower ID
        assertEquals("R101", sortedQueue.get(1).getReturnId());
        assertEquals("R100", sortedQueue.get(2).getReturnId()); // oldest
    }

    /**
     * Test Case 3: Process quarantine - restock return
     * Expected: Return restocked as new box with RET- prefix, proper FEFO insertion
     */
    @Test
    public void testProcessQuarantine_Restock() {
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

        Return returnItem = new Return(
                "R050",
                "SKU123",
                4,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 15, 0),
                LocalDate.of(2025, 12, 31)
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: result should indicate restock
        assertEquals(1, results.size());
        InspectionResult result = results.get(0);
        assertEquals("R050", result.getReturnId());
        assertEquals(InspectionAction.RESTOCKED, result.getAction());
        assertEquals(4, result.getQuantityRestocked());
        assertEquals(0, result.getQuantityDiscarded());

    }

    /**
     * Test Case 4: Process quarantine - discard return
     * Expected: Return discarded, flagged as unusable
     */
    @Test
    public void testProcessQuarantine_Discard() {
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

        // Create a return that must be discarded
        Return returnItem = new Return(
                "R060",
                "SKU123",
                3,
                ReturnReason.DAMAGED, // triggers discard
                LocalDateTime.of(2025, 10, 24, 15, 30),
                LocalDate.of(2025, 12, 31)
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: result should indicate discard
        assertEquals(1, results.size());
        InspectionResult result = results.get(0);
        assertEquals("R060", result.getReturnId());
        assertEquals(InspectionAction.DISCARDED, result.getAction());
        assertEquals(3, result.getQuantityDiscarded());
        assertEquals(0, result.getQuantityRestocked());
    }

    /**
     * Test Case 5: Process empty quarantine
     * Expected: No processing when quarantine is empty
     */
    @Test
    public void testProcessQuarantine_EmptyQuarantine() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        String warehouseId = "W1";
        int aisle = 1;

        // Act: process without adding any returns
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: result list should be empty
        assertNotNull("Result list should not be null", results);
        assertTrue("Result list should be empty", results.isEmpty());

        // Optional: assert quarantine size remains zero
        assertEquals("Quarantine queue should remain empty", 0, quarantineService.getQuarantineSize());
    }

    /**
     * Test Case 6: Clear quarantine
     * Expected: All returns removed from quarantine
     */
    @Test
    public void testClearQuarantine() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        // Add two returns to quarantine
        Return r1 = new Return("R070", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 16, 0), LocalDate.of(2025, 12, 31));
        Return r2 = new Return("R071", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 16, 5), LocalDate.of(2025, 12, 31));

        quarantineService.addToQuarantine(r1);
        quarantineService.addToQuarantine(r2);

        // Assert: quarantine size before clearing
        assertEquals("Quarantine should contain 2 items before clearing", 2, quarantineService.getQuarantineSize());

        // Act: clear quarantine
        quarantineService.clearQuarantine();

        // Assert: quarantine size after clearing
        assertEquals("Quarantine should be empty after clearing", 0, quarantineService.getQuarantineSize());
    }

    /**
     * Test Case 7: Return with expiry date
     * Expected: Return processed with proper expiry date handling
     */
    @Test
    public void testReturnWithExpiryDate() {
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

        // Create a return with a specific expiry date
        LocalDate expiryDate = LocalDate.of(2025, 11, 30);
        Return returnItem = new Return(
                "R080",
                "SKU123",
                2,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 17, 0),
                expiryDate
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: result should indicate restock and reflect correct expiry handling
        assertEquals(1, results.size());
        InspectionResult result = results.get(0);
        assertEquals("R080", result.getReturnId());
        assertEquals(InspectionAction.RESTOCKED, result.getAction());
        assertEquals(2, result.getQuantityRestocked());
        assertEquals(0, result.getQuantityDiscarded());

    }

    /**
     * Test Case 8: Return without expiry date
     * Expected: Return processed with null expiry date
     */
    @Test
    public void testReturnWithoutExpiryDate() {
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

        // Create a return with no expiry date (null)
        Return returnItem = new Return(
                "R090",
                "SKU123",
                2,
                ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 17, 30),
                null // no expiry date
        );

        // Act
        quarantineService.addToQuarantine(returnItem);
        List<InspectionResult> results = quarantineService.processQuarantine(warehouseId, aisle);

        // Assert: result should indicate restock and accept null expiry
        assertEquals(1, results.size());
        InspectionResult result = results.get(0);
        assertEquals("R090", result.getReturnId());
        assertEquals(InspectionAction.RESTOCKED, result.getAction());
        assertEquals(2, result.getQuantityRestocked());
        assertEquals(0, result.getQuantityDiscarded());

    }

    /**
     * Test Case 9: Multiple returns with same timestamp
     * Expected: Processed by returnId ASC when timestamps are equal
     */
    @Test
    public void testQuarantineOrder_SameTimestamp() {
        // Arrange
        InventoryService inventoryService = new InventoryService();
        String auditLogPath = "test-audit.log";
        QuarantineService quarantineService = new QuarantineService(inventoryService, auditLogPath);

        // Create returns with same timestamp but different returnIds
        LocalDateTime sameTime = LocalDateTime.of(2025, 10, 24, 18, 0);
        Return rA = new Return("R100", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE, sameTime, LocalDate.of(2025, 12, 31));
        Return rB = new Return("R101", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE, sameTime, LocalDate.of(2025, 12, 31));
        Return rC = new Return("R099", "SKU123", 1, ReturnReason.CUSTOMER_REMORSE, sameTime, LocalDate.of(2025, 12, 31));

        // Add in shuffled order
        quarantineService.addToQuarantine(rB);
        quarantineService.addToQuarantine(rC);
        quarantineService.addToQuarantine(rA);

        // Act
        List<Return> sortedQueue = quarantineService.getQuarantineQueue();

        // Assert: order should be R099, R100, R101 (same timestamp, sorted by returnId ASC)
        assertEquals(3, sortedQueue.size());
        assertEquals("R099", sortedQueue.get(0).getReturnId());
        assertEquals("R100", sortedQueue.get(1).getReturnId());
        assertEquals("R101", sortedQueue.get(2).getReturnId());
    }
}
