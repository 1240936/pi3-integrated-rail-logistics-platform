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
import java.util.Map;

/**
 * Loader for wagon/box data from CSV files.
 *
 * <p>Each CSV row represents a box in a wagon and must contain at least:
 * <ul>
 *   <li>wagonId (String)</li>
 *   <li>boxId (String)</li>
 *   <li>sku (String, must exist in inventory)</li>
 *   <li>quantity (int, &gt;0)</li>
 *   <li>expiryDate (optional, format yyyy-MM-dd or ISO datetime)</li>
 *   <li>receivedAt (ISO datetime, required)</li>
 * </ul>
 * Optional aisle/bay columns can be used, otherwise they are assigned automatically in FEFO order.
 * </p>
 *
 * <p>Boxes are globally sorted using FEFO (First Expiry First Out) before assignment to aisles and bays.</p>
 */
public class WagonCsvLoader {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Loads boxes from a CSV file into the inventory service.
     *
     * @param csvPath path to the wagons CSV file
     * @param defaultWarehouseId default warehouse ID to assign boxes to
     * @param defaultAisle default aisle number for boxes if not specified in CSV
     * @param inv InventoryService instance used for validation and insertion
     * @return CsvValidatorResult containing successfully loaded boxes and any validation errors
     * @throws IOException if the CSV file cannot be read
     */
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

                // Extract fields
                String wagonId = trimmer(f, 0);
                String boxId = trimmer(f, 1);
                String sku = trimmer(f, 2);
                String qtyRaw = trimmer(f, 3);
                String expiryRaw = trimmer(f, 4);
                String receivedRaw = trimmer(f, 5);

                // Validate mandatory fields
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

                // Validate SKU exists in inventory
                if (!inv.isKnownSku(defaultWarehouseId, sku)) {
                    result.addError("wagons.csv line " + lineNo + ": unknown SKU '" + sku + "' (wagon " + wagonId + ")");
                    continue;
                }

                // Create Box object (aisle/bay temporarily 0)
                try {
                    Box box = new Box(boxId, sku, expiry, receivedAt, qty, defaultWarehouseId, 0, 0);
                    allBoxes.add(box);
                } catch (Exception e) {
                    result.addError("wagons.csv line " + lineNo + ": " + e.getMessage() + " (box " + boxId + ")");
                }
            }
        }

        // Sort boxes globally using FEFO
        allBoxes.sort(new BoxFefoComparator());

        // Assign boxes to aisles and bays
        for (Box box : allBoxes) {
            int aisle = box.getAisle() != 0 ? box.getAisle() : distributeAcrossAisles(inv, defaultWarehouseId, 0);
            int bay = autoAssignBay(inv, defaultWarehouseId, aisle);

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

    /**
     * Finds an aisle with available space for new boxes.
     *
     * @param inv InventoryService instance
     * @param warehouseId warehouse to search
     * @param lineNo line number for error reporting (optional)
     * @return aisle number with available space, or fallback to first aisle
     */
    private static int distributeAcrossAisles(InventoryService inv, String warehouseId, int lineNo) {
        Warehouse warehouse = inv.getOrCreateWarehouse(warehouseId);
        Map<Integer, Map<Integer, Bay>> aisles = warehouse.getAisles();

        if (aisles.isEmpty()) return 1;

        for (Map.Entry<Integer, Map<Integer, Bay>> aisleEntry : aisles.entrySet()) {
            int aisleNum = aisleEntry.getKey();
            Map<Integer, Bay> bays = aisleEntry.getValue();
            for (Bay bay : bays.values()) {
                if (bay.hasSpace()) return aisleNum;
            }
        }

        return aisles.keySet().iterator().next();
    }

    /**
     * Finds a bay within an aisle with available space.
     *
     * @param inv InventoryService instance
     * @param warehouseId warehouse ID
     * @param aisle aisle number
     * @return bay number with available space, or 1 as fallback
     */
    private static int autoAssignBay(InventoryService inv, String warehouseId, int aisle) {
        int next = 1;
        while (next <= 100) {
            Bay bay = inv.getOrCreateBay(warehouseId, aisle, next);
            if (bay.hasSpace()) return next;
            next++;
        }
        return 1;
    }

    /**
     * Safely trims a CSV field by index.
     *
     * @param f CSV row fields
     * @param idx index
     * @return trimmed string, or empty if index is out of bounds
     */
    private static String trimmer(String[] f, int idx) {
        if (idx < f.length) return f[idx].trim();
        else return "";
    }

    /**
     * Parses an optional expiry date string.
     *
     * <p>Accepts formats:
     * <ul>
     *   <li>yyyy-MM-dd</li>
     *   <li>ISO datetime (yyyy-MM-ddTHH:mm:ss)</li>
     *   <li>null, NA, or "-" as empty</li>
     * </ul>
     * </p>
     *
     * @param raw raw string
     * @return parsed LocalDate or null if empty/invalid
     */
    private static LocalDate parseOptionalExpiry(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        String lower = t.toLowerCase();
        if ("null".equals(lower) || "na".equals(lower) || "-".equals(lower)) return null;

        try {
            if (t.contains("T")) {
                return LocalDateTime.parse(t, DATETIME).toLocalDate();
            }
            return LocalDate.parse(t, DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
