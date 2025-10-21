package main.repositories;

import main.controller.CsvReader;
import main.domain.*;
import main.controller.InventoryService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WagonCsvLoader {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ISO_DATE_TIME;

    public static CsvValidatorResult<Box> load(
            String csvPath,
            String defaultWarehouseId,
            int defaultAisle,
            InventoryService inv
    ) throws IOException {

        CsvValidatorResult<Box> result = new CsvValidatorResult<>();
        List<Box> allBoxes = new ArrayList<>(); // Collect all boxes first for global FEFO sorting

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine(); // skip header
            String line;
            int lineNo = 1;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;

                String[] f = CsvReader.splitFlexible(line);  // Reuse shared CSV parsing
                if (f.length < 6) {
                    result.addError("wagons.csv line " + lineNo + ": expected at least 6 columns");
                    continue;
                }

                // Expected structure: wagonId, boxId, sku, qty, expiryDate (optional), receivedAt [, aisle, bay]
                String wagonId = trimmer(f, 0);
                String boxId = trimmer(f, 1);
                String sku = trimmer(f, 2);
                String qtyRaw = trimmer(f, 3);
                String expiryRaw = trimmer(f, 4);
                String receivedRaw = trimmer(f, 5);
                // Note: aisle and bay will be assigned globally after FEFO sorting

                if (sku.isEmpty()) {
                    result.addError("wagons.csv line " + lineNo + ": missing SKU (wagon " + wagonId + ")");
                    continue;
                }
                if (boxId.isEmpty()) {
                    result.addError("wagons.csv line " + lineNo + ": missing boxId (wagon " + wagonId + ")");
                    continue;
                }

                LocalDate expiry = parseOptionalExpiry(expiryRaw);
                if (expiryRaw != null && !expiryRaw.trim().isEmpty() && expiry == null) {
                    result.addError("wagons.csv line " + lineNo + ": invalid expiryDate (box " + boxId + ")");
                    continue;
                }

                LocalDateTime receivedAt;
                try {
                    receivedAt = LocalDateTime.parse(receivedRaw.trim(), DATETIME);
                } catch (Exception e) {
                    result.addError("wagons.csv line " + lineNo + ": missing/invalid receivedAt (box " + boxId + ")");
                    continue;
                }

                int qty;
                try {
                    qty = Integer.parseInt(qtyRaw.trim());
                    if (qty <= 0) throw new IllegalArgumentException("quantity <= 0");
                } catch (Exception e) {
                    result.addError("wagons.csv line " + lineNo + ": invalid quantity (box " + boxId + ")");
                    continue;
                }

                // Validate SKU against known items
                if (!inv.isKnownSku(defaultWarehouseId, sku)) {
                    result.addError("wagons.csv line " + lineNo + ": unknown SKU '" + sku + "' (wagon " + wagonId + ")");
                    continue;
                }

                // Create box with temporary aisle/bay (will be assigned later)
                try {
                    Box box = new Box(boxId, sku, expiry, receivedAt, qty, defaultWarehouseId, 0, 0);
                    allBoxes.add(box);
                } catch (Exception e) {
                    result.addError("wagons.csv line " + lineNo + ": " + e.getMessage() + " (box " + boxId + ")");
                }
            }
        }

        // Sort all boxes globally using FEFO ordering
        allBoxes.sort(new BoxFefoComparator());

        // Now assign boxes to aisles and bays in FEFO order
        for (Box box : allBoxes) {
            int aisle = defaultAisle;
            if (box.getAisle() != 0) { // If aisle was specified in CSV
                aisle = box.getAisle();
            } else {
                // If no aisle specified, distribute boxes across available aisles
                aisle = distributeAcrossAisles(inv, defaultWarehouseId, 0);
            }

            int bay = autoAssignBay(inv, defaultWarehouseId, aisle);

            // Update box with final aisle and bay
            box.setAisle(aisle);
            box.setBay(bay);

            try {
                inv.insertBox(box);
                result.addRecord(box);
            } catch (Exception e) {
                result.addError("Error inserting box " + box.getBoxId() + ": " + e.getMessage());
            }
        }

        return result;
    }

    private static int distributeAcrossAisles(InventoryService inv, String warehouseId, int lineNo) {
        // Get all available aisles from the warehouse
        var warehouse = inv.getOrCreateWarehouse(warehouseId);
        var aisles = warehouse.getAisles();
        
        if (aisles.isEmpty()) {
            return 1; // fallback to aisle 1
        }
        
        // Find the first aisle that has available space (sequential filling)
        for (var aisleEntry : aisles.entrySet()) {
            int aisleNum = aisleEntry.getKey();
            var bays = aisleEntry.getValue();
            
            // Check if this aisle has any bays with available space
            for (var bay : bays.values()) {
                if (bay.hasSpace()) {
                    return aisleNum; // Return the first aisle with available space
                }
            }
        }
        
        // If all aisles are full, return the first aisle as fallback
        return aisles.keySet().iterator().next();
    }

    private static int autoAssignBay(InventoryService inv, String warehouseId, int aisle) {
        // Find the first bay with available capacity (sequential filling)
        int next = 1;
        while (next <= 100) { // reasonable limit to prevent infinite loop
            Bay bay = inv.getOrCreateBay(warehouseId, aisle, next);
            if (bay.hasSpace()) {
                return next; // Return the first bay with available space
            }
            next++;
        }
        // If no bay has space, return 1 as fallback
        return 1;
    }

    private static String trimmer(String[] f, int idx) {
        if (idx < f.length) {
            return f[idx].trim();
        } else {
            return "";
        }
    }

    private static LocalDate parseOptionalExpiry(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        String lower = t.toLowerCase();
        if ("null".equals(lower) || "na".equals(lower) || "-".equals(lower)) return null;

        try {
            if (t.contains("T")) {
                // Accept ISO datetime; use date part
                return LocalDateTime.parse(t, DATETIME).toLocalDate();
            }
            return LocalDate.parse(t, DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
