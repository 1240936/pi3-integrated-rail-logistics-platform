package USEI05;

import org.junit.Test;
import static org.junit.Assert.*;

import main.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;


/**
 * Test cases for Return domain models
 * 
 * Tests domain model functionality:
 * - Return object creation and properties
 * - ReturnReason enum values
 * - InspectionResult object creation
 * - InspectionAction enum values
 * - Proper date/time handling
 */
public class TestReturnDomainModels {

    /**
     * Test Case 1: Return object creation with all fields
     * Expected: Return object created with all properties set correctly
     */
    @Test
    public void testReturn_CreationWithAllFields() {
        // Arrange
        String returnId = "R001";
        String sku = "SKU123";
        int quantity = 5;
        ReturnReason reason = ReturnReason.CUSTOMER_REMORSE;
        LocalDateTime timestamp = LocalDateTime.of(2025, 10, 24, 14, 30);
        LocalDate expiryDate = LocalDate.of(2025, 12, 31);

        // Act
        Return returnItem = new Return(returnId, sku, quantity, reason, timestamp, expiryDate);

        // Assert
        assertEquals("R001", returnItem.getReturnId());
        assertEquals("SKU123", returnItem.getSku());
        assertEquals(5, returnItem.getQuantity());
        assertEquals(ReturnReason.CUSTOMER_REMORSE, returnItem.getReason());
        assertEquals(LocalDateTime.of(2025, 10, 24, 14, 30), returnItem.getTimestamp());
        assertEquals(LocalDate.of(2025, 12, 31), returnItem.getExpiryDate());
    }

    /**
     * Test Case 2: Return object creation without expiry date
     * Expected: Return object created with null expiry date
     */
    @Test
    public void testReturn_CreationWithoutExpiryDate() {
        // Arrange
        String returnId = "R002";
        String sku = "SKU456";
        int quantity = 3;
        ReturnReason reason = ReturnReason.CYCLE_COUNT;
        LocalDateTime timestamp = LocalDateTime.of(2025, 10, 24, 15, 45);

        // Act
        Return returnItem = new Return(returnId, sku, quantity, reason, timestamp, null);

        // Assert
        assertEquals("R002", returnItem.getReturnId());
        assertEquals("SKU456", returnItem.getSku());
        assertEquals(3, returnItem.getQuantity());
        assertEquals(ReturnReason.CYCLE_COUNT, returnItem.getReason());
        assertEquals(LocalDateTime.of(2025, 10, 24, 15, 45), returnItem.getTimestamp());
        assertNull("Expiry date should be null", returnItem.getExpiryDate());
    }

    /**
     * Test Case 3: ReturnReason enum values
     * Expected: All return reason enum values are available
     */
    @Test
    public void testReturnReason_EnumValues() {
        List<ReturnReason> reasons = Arrays.asList(ReturnReason.values());

        assertEquals(4, reasons.size());
        assertTrue(reasons.contains(ReturnReason.CUSTOMER_REMORSE));
        assertTrue(reasons.contains(ReturnReason.DAMAGED));
        assertTrue(reasons.contains(ReturnReason.EXPIRED));
        assertTrue(reasons.contains(ReturnReason.CYCLE_COUNT));
    }


    /**
     * Test Case 4: ReturnReason string representation
     * Expected: ReturnReason enum values have proper string representation
     */
    @Test
    public void testReturnReason_StringRepresentation() {
        // Assert: default toString() returns enum name
        assertEquals("CUSTOMER_REMORSE", ReturnReason.CUSTOMER_REMORSE.toString());
        assertEquals("DAMAGED", ReturnReason.DAMAGED.toString());
        assertEquals("EXPIRED", ReturnReason.EXPIRED.toString());
        assertEquals("CYCLE_COUNT", ReturnReason.CYCLE_COUNT.toString());
    }

    /**
     * Test Case 5: InspectionAction enum values
     * Expected: All inspection action enum values are available
     */
    @Test
    public void testInspectionAction_EnumValues() {
        List<InspectionAction> actions = Arrays.asList(InspectionAction.values());

        assertEquals(2, actions.size());
        assertTrue(actions.contains(InspectionAction.RESTOCKED));
        assertTrue(actions.contains(InspectionAction.DISCARDED));
    }


    /**
     * Test Case 6: InspectionAction string representation
     * Expected: InspectionAction enum values have proper string representation
     */
    @Test
    public void testInspectionAction_StringRepresentation() {
        // Assert: default toString() returns enum name
        assertEquals("RESTOCKED", InspectionAction.RESTOCKED.toString());
        assertEquals("DISCARDED", InspectionAction.DISCARDED.toString());

    }

    /**
     * Test Case 7: InspectionResult creation for restocked item
     * Expected: InspectionResult created with correct properties for restocked item
     */
    @Test
    public void testInspectionResult_RestockedItem() {
        // Arrange
        String returnId = "R200";
        String sku = "SKU123";
        InspectionAction action = InspectionAction.RESTOCKED;
        int totalQuantity = 5;
        int quantityRestocked = 5;
        int quantityDiscarded = 0;

        // Act
        InspectionResult result = new InspectionResult(returnId, sku, action, totalQuantity, quantityRestocked, quantityDiscarded);

        // Assert
        assertEquals("R200", result.getReturnId());
        assertEquals("SKU123", result.getSku());
        assertEquals(InspectionAction.RESTOCKED, result.getAction());
        assertEquals(5, result.getTotalQuantity());
        assertEquals(5, result.getQuantityRestocked());
        assertEquals(0, result.getQuantityDiscarded());
        assertFalse("Should not be partial restock", result.isPartialRestock());
    }

    /**
     * Test Case 8: InspectionResult creation for discarded item
     * Expected: InspectionResult created with correct properties for discarded item
     */
    @Test
    public void testInspectionResult_DiscardedItem() {
        // Arrange
        String returnId = "R201";
        String sku = "SKU456";
        InspectionAction action = InspectionAction.DISCARDED;
        int totalQuantity = 4;
        int quantityRestocked = 0;
        int quantityDiscarded = 4;

        // Act
        InspectionResult result = new InspectionResult(returnId, sku, action, totalQuantity, quantityRestocked, quantityDiscarded);

        // Assert
        assertEquals("R201", result.getReturnId());
        assertEquals("SKU456", result.getSku());
        assertEquals(InspectionAction.DISCARDED, result.getAction());
        assertEquals(4, result.getTotalQuantity());
        assertEquals(0, result.getQuantityRestocked());
        assertEquals(4, result.getQuantityDiscarded());
        assertFalse("Should not be partial restock", result.isPartialRestock());
    }

    /**
     * Test Case 9: InspectionResult creation for partial processing
     * Expected: InspectionResult created with partial quantities
     */
    @Test
    public void testInspectionResult_PartialProcessing() {
        // Arrange
        String returnId = "R202";
        String sku = "SKU789";
        InspectionAction action = InspectionAction.RESTOCKED;
        int totalQuantity = 6;
        int quantityRestocked = 4;
        int quantityDiscarded = 2;

        // Act
        InspectionResult result = new InspectionResult(returnId, sku, action, totalQuantity, quantityRestocked, quantityDiscarded);

        // Assert
        assertEquals("R202", result.getReturnId());
        assertEquals("SKU789", result.getSku());
        assertEquals(InspectionAction.RESTOCKED, result.getAction());
        assertEquals(6, result.getTotalQuantity());
        assertEquals(4, result.getQuantityRestocked());
        assertEquals(2, result.getQuantityDiscarded());
        assertTrue("Should be partial restock", result.isPartialRestock());
    }
    
    /**
     * Test Case 10: Return object equality
     * Expected: Return objects with same properties are equal
     */
    @Test
    public void testReturn_Equality() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.of(2025, 10, 24, 16, 0);
        LocalDate expiry = LocalDate.of(2025, 12, 31);

        Return r1 = new Return("R300", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE, timestamp, expiry);
        Return r2 = new Return("R300", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE, timestamp, expiry);

        // Assert: compare field to field
        assertEquals(r1.getReturnId(), r2.getReturnId());
        assertEquals(r1.getSku(), r2.getSku());
        assertEquals(r1.getQuantity(), r2.getQuantity());
        assertEquals(r1.getReason(), r2.getReason());
        assertEquals(r1.getTimestamp(), r2.getTimestamp());
        assertEquals(r1.getExpiryDate(), r2.getExpiryDate());
    }


    /**
     * Test Case 11: Return object inequality
     * Expected: Return objects with different properties are not equal
     */
    @Test
    public void testReturn_Inequality() {
        // Arrange: create two Return objects with different returnId
        LocalDateTime timestamp = LocalDateTime.of(2025, 10, 24, 16, 0);
        LocalDate expiry = LocalDate.of(2025, 12, 31);

        Return r1 = new Return("R301", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE, timestamp, expiry);
        Return r2 = new Return("R302", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE, timestamp, expiry); // different returnId

        // Assert: objects are not the same instance
        assertNotSame(r1, r2);

        // Assert: at least one field is different
        assertNotEquals(r1.getReturnId(), r2.getReturnId());

        //Confirm other fields are equal
        assertEquals(r1.getSku(), r2.getSku());
        assertEquals(r1.getQuantity(), r2.getQuantity());
        assertEquals(r1.getReason(), r2.getReason());
        assertEquals(r1.getTimestamp(), r2.getTimestamp());
        assertEquals(r1.getExpiryDate(), r2.getExpiryDate());
    }

    /**
     * Test Case 12: Return object string representation
     * Expected: Return object has meaningful string representation
     */
    @Test
    public void testReturn_StringRepresentation() {
        // Arrange
        Return r = new Return("R400", "SKU123", 2, ReturnReason.CUSTOMER_REMORSE,
                LocalDateTime.of(2025, 10, 24, 16, 0), LocalDate.of(2025, 12, 31));

        // Act
        String representation = r.toString();

        // Assert: default toString() is not meaningful
        assertTrue("Default toString should contain class name", representation.contains("main.domain.Return"));
    }


    /**
     * Test Case 13: InspectionResult string representation
     * Expected: InspectionResult object has meaningful string representation
     */
    @Test
    public void testInspectionResult_StringRepresentation() {
        // Arrange
        InspectionResult result = new InspectionResult(
                "R500", "SKU123", InspectionAction.RESTOCKED,
                5, 5, 0
        );

        // Act
        String representation = result.toString();

        // Assert: default toString() contains class name
        assertTrue("Default toString should contain class name", representation.contains("main.domain.InspectionResult"));
    }


    /**
     * Test Case 14: Edge case - zero quantity
     * Expected: Return object handles zero quantity correctly
     */
    @Test
    public void testReturn_ZeroQuantity() {
        // Arrange: create a Return object with zero quantity
        LocalDateTime timestamp = LocalDateTime.of(2025, 10, 25, 10, 0);
        LocalDate expiry = LocalDate.of(2025, 12, 31);

        Return r = new Return("R999", "SKU000", 0, ReturnReason.CYCLE_COUNT, timestamp, expiry);

        // Assert: verify that all fields are correctly set
        assertEquals("R999", r.getReturnId());
        assertEquals("SKU000", r.getSku());
        assertEquals(0, r.getQuantity());
        assertEquals(ReturnReason.CYCLE_COUNT, r.getReason());
        assertEquals(timestamp, r.getTimestamp());
        assertEquals(expiry, r.getExpiryDate());
    }

    /**
     * Test Case 15: Edge case - large quantity
     * Expected: Return object handles large quantity correctly
     */
    @Test
    public void testReturn_LargeQuantity() {
        // Arrange: create a Return object with a very large quantity
        int largeQuantity = 1_000_000;
        LocalDateTime timestamp = LocalDateTime.of(2025, 10, 25, 10, 0);
        LocalDate expiry = LocalDate.of(2026, 1, 1);

        Return r = new Return("R1000000", "SKU999", largeQuantity, ReturnReason.DAMAGED, timestamp, expiry);

        // Assert: verify that all fields are correctly set
        assertEquals("R1000000", r.getReturnId());
        assertEquals("SKU999", r.getSku());
        assertEquals(largeQuantity, r.getQuantity());
        assertEquals(ReturnReason.DAMAGED, r.getReason());
        assertEquals(timestamp, r.getTimestamp());
        assertEquals(expiry, r.getExpiryDate());
    }
}
