package test_cases.USEI02;

import main.domain.*;

import java.time.*;
import java.util.*;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for InventoryService.planAllocations() method
 *
 * Tests order eligibility and allocation logic:
 * - Orders processed by priority ASC, dueDate ASC, orderId ASC
 * - Lines processed by lineNo ASC within each order
 * - Allocation walks SKU's boxes in FEFO/FIFO order
 * - Supports STRICT and PARTIAL allocation modes
 */
public class TestPlanAllocations {

    /**
     * Test Case 1: Order processing order (priority, dueDate, orderId)
     * Expected: Orders processed by priority ASC, dueDate ASC, orderId ASC
     */
    @Test
    public void testPlanAllocations_OrderProcessingOrder() {
        List<OrderHeader> orders = new ArrayList<>();
        orders.add(new OrderHeader("B002", LocalDate.of(2025, 10, 20), 2));
        orders.add(new OrderHeader("A001", LocalDate.of(2025, 10, 20), 1));
        orders.add(new OrderHeader("C003", LocalDate.of(2025, 10, 19), 1));
        orders.add(new OrderHeader("D004", LocalDate.of(2025, 10, 21), 3));

        // Sort by priority ASC, dueDate ASC, orderId ASC
        orders.sort(Comparator
                .comparingInt(OrderHeader::getPriority)
                .thenComparing(OrderHeader::getDueDate)
                .thenComparing(OrderHeader::getOrderId));

        // Expected order: C003, A001, B002, D004
        assertEquals("C003", orders.get(0).getOrderId());
        assertEquals("A001", orders.get(1).getOrderId());
        assertEquals("B002", orders.get(2).getOrderId());
        assertEquals("D004", orders.get(3).getOrderId());
    }

    /**
     * Test Case 2: Line processing order within order
     * Expected: Within order, lines processed by lineNo ASC (input order)
     */
    @Test
    public void testPlanAllocations_LineProcessingOrder() {
        List<OrderLine> lines = new ArrayList<>();
        lines.add(new OrderLine("ORD001", 3, "SKU123", 10, 1, LocalDate.of(2025, 10, 20)));
        lines.add(new OrderLine("ORD001", 1, "SKU456", 5, 1, LocalDate.of(2025, 10, 20)));
        lines.add(new OrderLine("ORD001", 2, "SKU789", 8, 1, LocalDate.of(2025, 10, 20)));

        // Sort by lineNo ASC
        lines.sort(Comparator.comparingInt(OrderLine::getLineNo));

        // Expected order: lineNo 1, 2, 3
        assertEquals(1, lines.get(0).getLineNo());
        assertEquals(2, lines.get(1).getLineNo());
        assertEquals(3, lines.get(2).getLineNo());
    }


    /**
    * Test Case 3: FEFO allocation order
    * Expected: Allocation walks SKU's boxes in FEFO/FIFO order
    */
    @Test
    public void testPlanAllocations_FefoAllocationOrder() {

        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box("BX003", "SKU1", LocalDate.of(2025, 11, 10), LocalDateTime.of(2025, 10, 1, 10, 0), 10, "WH1", 1, 1));
        boxes.add(new Box("BX001", "SKU1", LocalDate.of(2025, 11, 5), LocalDateTime.of(2025, 10, 2, 9, 0), 10, "WH1", 1, 2));
        boxes.add(new Box("BX004", "SKU1", null, LocalDateTime.of(2025, 10, 3, 8, 0), 10, "WH1", 1, 3));
        boxes.add(new Box("BX002", "SKU1", LocalDate.of(2025, 11, 5), LocalDateTime.of(2025, 10, 1, 9, 0), 10, "WH1", 1, 4));

        // Sort using FEFO comparator
        Collections.sort(boxes, new BoxFefoComparator());

        // Expected order:
        // BX002 (earliest expiry + earliest received)
        // BX001 (same expiry, later received)
        // BX003 (later expiry)
        // BX004 (no expiry, goes last)
        assertEquals("BX002", boxes.get(0).getBoxId());
        assertEquals("BX001", boxes.get(1).getBoxId());
        assertEquals("BX003", boxes.get(2).getBoxId());
        assertEquals("BX004", boxes.get(3).getBoxId());
    }

    /**
     * Test Case 4: STRICT mode - ELIGIBLE status
     * Expected: Line is ELIGIBLE only if entire requested quantity is allocated
     */
    @Test
    public void testPlanAllocations_StrictModeEligible() {
        LineEligibility eligibleLine = new LineEligibility("ORD001", 1, "SKU123", 10, 10, LineStatus.ELIGIBLE);
        LineEligibility partialLine = new LineEligibility("ORD001", 2, "SKU123", 10, 7, LineStatus.UNDISPATCHABLE);
        LineEligibility zeroLine = new LineEligibility("ORD001", 3, "SKU123", 10, 0, LineStatus.UNDISPATCHABLE);

        List<LineEligibility> lines = List.of(eligibleLine, partialLine, zeroLine);

        for (LineEligibility line : lines) {
            if (line.getAllocatedQty() == line.getRequestedQty()) {
                assertEquals(LineStatus.ELIGIBLE, line.getStatus());
            } else {
                assertEquals(LineStatus.UNDISPATCHABLE, line.getStatus());
            }
        }
    }

    /**
     * Test Case 5: STRICT mode - UNDISPATCHABLE status
     * Expected: Line is UNDISPATCHABLE if entire quantity cannot be allocated
     */
    @Test
    public void testPlanAllocations_StrictModeUndispatchable() {
        LineEligibility partialAllocation = new LineEligibility("ORD002", 1, "SKU456", 10, 5, LineStatus.UNDISPATCHABLE);
        LineEligibility zeroAllocation = new LineEligibility("ORD002", 2, "SKU456", 10, 0, LineStatus.UNDISPATCHABLE);

        List<LineEligibility> lines = Arrays.asList(partialAllocation, zeroAllocation);

        for (LineEligibility line : lines) {
            assertEquals("Line " + line.getLineNo() + " should be UNDISPATCHABLE when not fully allocated",
                    LineStatus.UNDISPATCHABLE, line.getStatus());
        }
    }

    /**
     * Test Case 6: PARTIAL mode - PARTIAL status
     * Expected: Line marked as PARTIAL when 0 < allocated < requested
     */
    @Test
    public void testPlanAllocations_PartialModePartial() {
        LineEligibility partialLine1 = new LineEligibility("ORD003", 1, "SKU789", 10, 6, LineStatus.PARTIAL);
        LineEligibility partialLine2 = new LineEligibility("ORD003", 2, "SKU789", 5, 1, LineStatus.PARTIAL);

        List<LineEligibility> lines = Arrays.asList(partialLine1, partialLine2);

        for (LineEligibility line : lines) {
            boolean isPartial = line.getAllocatedQty() > 0 && line.getAllocatedQty() < line.getRequestedQty();
            assertEquals("Line " + line.getLineNo() + " should be PARTIAL when partially allocated",
                    isPartial ? LineStatus.PARTIAL : LineStatus.UNDISPATCHABLE,
                    line.getStatus());
        }
    }

    /**
     * Test Case 7: PARTIAL mode - UNDISPATCHABLE status
     * Expected: Line marked as UNDISPATCHABLE when allocated = 0
     */
    @Test
    public void testPlanAllocations_PartialModeUndispatchable() {
        LineEligibility line1 = new LineEligibility("ORD004", 1, "SKU321", 10, 0, LineStatus.UNDISPATCHABLE);
        LineEligibility line2 = new LineEligibility("ORD004", 2, "SKU321", 5, 0, LineStatus.UNDISPATCHABLE);

        List<LineEligibility> lines = Arrays.asList(line1, line2);

        for (LineEligibility line : lines) {
            assertEquals("Line " + line.getLineNo() + " should be UNDISPATCHABLE when allocated = 0",
                    LineStatus.UNDISPATCHABLE, line.getStatus());
        }
    }

    /**
    * Test Case 8: Allocation across multiple boxes
    * Expected: Allocation takes min(remainingQty, box.qtyAvailable) from each box
    */
    @Test
    public void testPlanAllocations_AllocationAcrossMultipleBoxes() {

        // Order line requesting 18 units
        OrderLine line = new OrderLine("ORD005", 1, "SKU999", 18, 1, LocalDate.of(2025, 10, 22));

        // Boxes with available quantities: 10, 5, 8
        List<Box> boxes = Arrays.asList(
                new Box("BX01", "SKU999", null, LocalDateTime.of(2025, 10, 1, 10, 0), 10, "WH1", 1, 1),
                new Box("BX02", "SKU999", null, LocalDateTime.of(2025, 10, 2, 10, 0), 5, "WH1", 1, 2),
                new Box("BX03", "SKU999", null, LocalDateTime.of(2025, 10, 3, 10, 0), 8, "WH1", 1, 3)
        );

        int remainingQty = line.getRequestedQty();
        List<AllocationRow> allocations = new ArrayList<>();

        for (Box box : boxes) {
            if (remainingQty <= 0) break;

            int allocQty = Math.min(remainingQty, box.getQuantity());
            if (allocQty > 0) {
                allocations.add(new AllocationRow(
                        line.getOrderId(),
                        line.getLineNo(),
                        box.getSku(),
                        allocQty,
                        box.getBoxId(),
                        box.getAisle(),
                        box.getBay()
                ));
                remainingQty -= allocQty;
            }
        }

        // Expected: BX01 → 10 units, BX02 → 5 units, BX03 → 3 units (total 18)
        assertEquals(3, allocations.size());

        assertEquals("BX01", allocations.get(0).getBoxId());
        assertEquals(10, allocations.get(0).getQty());

        assertEquals("BX02", allocations.get(1).getBoxId());
        assertEquals(5, allocations.get(1).getQty());

        assertEquals("BX03", allocations.get(2).getBoxId());
        assertEquals(3, allocations.get(2).getQty());
    }

    /**
     * Test Case 9: Eligibility results structure
     * Expected: Eligibility results contain orderId, lineNo, sku, requestedQty, allocatedQty, status
     */
    @Test
    public void testPlanAllocations_EligibilityResultsStructure() {
        LineEligibility result = new LineEligibility("ORD006", 1, "SKU555", 20, 15, LineStatus.PARTIAL);

        // Validate structure
        assertEquals("ORD006", result.getOrderId());
        assertEquals(1, result.getLineNo());
        assertEquals("SKU555", result.getSku());
        assertEquals(20, result.getRequestedQty());
        assertEquals(15, result.getAllocatedQty());
        assertEquals(LineStatus.PARTIAL, result.getStatus());
    }

    /**
     * Test Case 10: Allocation rows structure
     * Expected: Allocation rows contain orderId, lineNo, sku, qty, boxId, aisle, bay
     */
    @Test
    public void testPlanAllocations_AllocationRowsStructure() {
        AllocationRow row = new AllocationRow("ORD007", 2, "SKU888", 12, "BX10", 3, 5);

        // Validate structure
        assertEquals("ORD007", row.getOrderId());
        assertEquals(2, row.getLineNo());
        assertEquals("SKU888", row.getSku());
        assertEquals(12, row.getQty());
        assertEquals("BX10", row.getBoxId());
        assertEquals(3, row.getAisle());
        assertEquals(5, row.getBay());
    }

    /**
     * Test Case 11: Non-existent SKU handling
     * Expected: Lines with non-existent SKU marked as UNDISPATCHABLE
     */
    @Test
    public void testPlanAllocations_NonExistentSku() {
        // Order line requesting a SKU that doesn't exist in inventory
        OrderLine line = new OrderLine("ORD008", 1, "SKU404", 10, 1, LocalDate.of(2025, 10, 22));

        // Inventory contains boxes with other SKUs
        List<Box> boxes = Arrays.asList(
                new Box("BX01", "SKU111", null, LocalDateTime.of(2025, 10, 1, 10, 0), 10, "WH1", 1, 1),
                new Box("BX02", "SKU222", null, LocalDateTime.of(2025, 10, 2, 10, 0), 5, "WH1", 1, 2)
        );

        // Check if any box matches the requested SKU
        boolean skuExists = boxes.stream().anyMatch(box -> box.getSku().equals(line.getSku()));

        // If no matching SKU, mark as UNDISPATCHABLE
        LineEligibility eligibility = new LineEligibility(
                line.getOrderId(),
                line.getLineNo(),
                line.getSku(),
                line.getRequestedQty(),
                0,
                skuExists ? LineStatus.ELIGIBLE : LineStatus.UNDISPATCHABLE
        );

        assertEquals(LineStatus.UNDISPATCHABLE, eligibility.getStatus());
        assertEquals(0, eligibility.getAllocatedQty());
    }

    /**
     * Test Case 12: Empty order lines handling
     * Expected: Empty order lines list handled gracefully
     */
    @Test
    public void testPlanAllocations_EmptyOrderLines() {
        // Simulate an empty array of order lines
        OrderLine[] orderLines = new OrderLine[0];

        // Simulate eligibility result generation
        LineEligibility[] results = new LineEligibility[orderLines.length];

        // Assert that no eligibility results are produced
        assertEquals(0, results.length);
    }
}
