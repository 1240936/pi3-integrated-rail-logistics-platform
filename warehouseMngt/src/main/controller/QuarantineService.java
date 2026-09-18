package main.controller;

import main.domain.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


// Recebe devoluções, inspeciona pelo motivo, reabastece os que podem (se tiver bay disponivel, se nao descarta)
// descarta os que não podem, e regista tudo no ficheiro de auditoria.

/**
 * Service for managing returned goods in quarantine.
 * Handles quarantine operations, inspection processing, and audit logging.
 */
public class QuarantineService {
    private final InventoryService inventoryService;
    private final List<Return> quarantineQueue;
    private final String auditLogPath;
    private final DateTimeFormatter timestampFormatter;

    /**
     * Creates a new quarantine service.
     * @param inventoryService the inventory service for restocking operations
     * @param auditLogPath path to the audit log file
     */
    public QuarantineService(InventoryService inventoryService, String auditLogPath) {
        this.inventoryService = inventoryService;
        this.quarantineQueue = new ArrayList<>();
        this.auditLogPath = auditLogPath;
        this.timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    }

    /**
     * Adds a return to quarantine for inspection.
     * @param returnItem the return to quarantine
     */
    public void addToQuarantine(Return returnItem) {
        quarantineQueue.add(returnItem);
        System.out.println("Return " + returnItem.getReturnId() + " added to quarantine for inspection.");
    }

    /**
     * Gets all items currently in quarantine, sorted by timestamp (latest first).
     * @return list of returns in quarantine, sorted by timestamp descending, then by returnId ascending
     */
    public List<Return> getQuarantineQueue() {
        List<Return> sortedQueue = new ArrayList<>(quarantineQueue); // -> sortedQueue para usar no procssQuarantine
        sortedQueue.sort(Comparator
                .comparing(Return::getTimestamp, Comparator.reverseOrder()) // cria uma copia da quarantienQueue, e coloca em ordem decresente de tempo de chegada e ordem crescente de id se tiver o mesmo tempo
                .thenComparing(Return::getReturnId));
        return sortedQueue;
    }

    /**
     * Processes all items in quarantine through inspection.
     * Items are processed in reverse order of arrival (latest first).
     * @param warehouseId the warehouse where items should be restocked
     * @param aisle the aisle where items should be restocked
     * @return list of inspection results
     */
    public List<InspectionResult> processQuarantine(String warehouseId, int aisle) {
        List<InspectionResult> results = new ArrayList<>();
        List<Return> sortedQueue = getQuarantineQueue();

        for (Return returnItem : sortedQueue) {
            InspectionResult result = inspectReturn(returnItem, warehouseId, aisle);
            results.add(result);
            logInspectionResult(result); // adiciona ao audit o resultado
        }

        // Clear quarantine after processing
        quarantineQueue.clear();
        return results;
    }

    /**
     * Inspects a single return and determines the appropriate action.
     * @param returnItem the return to inspect
     * @param warehouseId the warehouse for restocking
     * @param aisle the aisle for restocking
     * @return the inspection result
     */
    private InspectionResult inspectReturn(Return returnItem, String warehouseId, int aisle) {
        String returnId = returnItem.getReturnId();
        String sku = returnItem.getSku();
        int totalQuantity = returnItem.getQuantity();

        // Determine action based on return reason
        if (returnItem.shouldBeDiscarded()) {
            // Discard the entire return
            return new InspectionResult(returnId, sku, InspectionAction.DISCARDED,
                    totalQuantity, 0, totalQuantity);
        } else if (returnItem.canBeRestocked()) {
            // Try to restock the return
            int restockedQty = restockReturn(returnItem, warehouseId, aisle);
            int discardedQty = totalQuantity - restockedQty;

            if (restockedQty == totalQuantity) {
                // Fully restocked
                return new InspectionResult(returnId, sku, InspectionAction.RESTOCKED,
                        totalQuantity, restockedQty, 0);
            } else if (restockedQty > 0) {
                // Partially restocked
                return new InspectionResult(returnId, sku, InspectionAction.RESTOCKED,
                        totalQuantity, restockedQty, discardedQty);
            } else {
                // Could not restock any, discard all
                return new InspectionResult(returnId, sku, InspectionAction.DISCARDED,
                        totalQuantity, 0, totalQuantity);
            }
        } else {
            // Unknown reason, discard
            return new InspectionResult(returnId, sku, InspectionAction.DISCARDED,
                    totalQuantity, 0, totalQuantity);
        }
    }

    /**
     * Attempts to restock a return by creating a new box and inserting it into inventory.
     * @param returnItem the return to restock
     * @param warehouseId the warehouse for restocking
     * @param aisle the aisle for restocking
     * @return the quantity that was successfully restocked
     */
    private int restockReturn(Return returnItem, String warehouseId, int aisle) {
        try {
            // Create new box for restocked items
            String newBoxId = "RET-" + returnItem.getReturnId();

            // Find an appropriate bay for the SKU
            int bayNumber = findAvailableBay(warehouseId, aisle, returnItem.getSku());
            if (bayNumber == -1) { // evitar dae restock antes de ter uma warehouse loaded
                System.out.println("No available bay found for SKU " + returnItem.getSku() + ". Make sure to load initial CSV data (bays) first.");
                return 0;
            }

            // Create the restocked box using the original return timestamp for proper FEFO ordering
            Box restockedBox = new Box(
                    newBoxId,
                    returnItem.getSku(),
                    returnItem.getExpiryDate(),
                    returnItem.getTimestamp(), // Use original return timestamp, not current time
                    returnItem.getQuantity(),
                    warehouseId,
                    aisle,
                    bayNumber
            );

            // Insert into inventory using FEFO rules
            inventoryService.insertBox(restockedBox);

            System.out.println("Successfully restocked " + returnItem.getQuantity() + " units of " + returnItem.getSku() + " from return " + returnItem.getReturnId());

            return returnItem.getQuantity();

        } catch (Exception e) {
            System.out.println("Failed to restock return " + returnItem.getReturnId() + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Finds an available bay for a given SKU in the specified warehouse and aisle.
     * Only uses existing bays that were loaded from CSV data.
     * @param warehouseId the warehouse ID
     * @param aisle the aisle number
     * @param sku the SKU to find a bay for
     * @return the bay number, or -1 if no bay is available
     */
    private int findAvailableBay(String warehouseId, int aisle, String sku) {
        // Only look for existing bays that were loaded from CSV data
        for (int bayNumber = 1; bayNumber <= 50; bayNumber++) { // Check reasonable range
            if (inventoryService.bayExists(warehouseId, aisle, bayNumber)) {
                if (inventoryService.bayHasCapacity(warehouseId, aisle, bayNumber)) {
                    return bayNumber;
                }
            }
        }

        // No available bay found - this is expected if CSV data wasn't loaded first
        return -1;
    }

    /**
     * Logs an inspection result to the audit log file.
     * @param result the inspection result to log
     */
    private void logInspectionResult(InspectionResult result) {
        try (FileWriter writer = new FileWriter(auditLogPath, true)) {
            String timestamp = LocalDateTime.now().format(timestampFormatter);
            String logLine = String.format("%s | returnId=%s | sku=%s | action=%s | qty=%d", timestamp, result.getReturnId(), result.getSku(), result.getAction(), result.getTotalQuantity());

            // Add partial restock details if applicable
            if (result.isPartialRestock()) {
                logLine += String.format(" (qtyRestocked=%d, qtyDiscarded=%d)", result.getQuantityRestocked(), result.getQuantityDiscarded());
            }

            writer.write(logLine + "\n");
            writer.flush();

        } catch (IOException e) {
            System.err.println("Failed to write to audit log: " + e.getMessage());
        }
    }

    /**
     * Gets the number of items currently in quarantine.
     * @return the quarantine queue size
     */
    public int getQuarantineSize() {
        return quarantineQueue.size();
    }

    /**
     * Clears all items from quarantine without processing them.
     * Use with caution - this bypasses the normal inspection process.
     */
    public void clearQuarantine() {
        quarantineQueue.clear();
        System.out.println("Quarantine queue cleared.");
    }
}