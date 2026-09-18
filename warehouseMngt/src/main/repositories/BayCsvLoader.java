package main.repositories;

import main.controller.CsvReader;
import main.controller.InventoryService;
import java.io.IOException;

/**
 * Loads bay data from a CSV file into the InventoryService.
 * Each line in the CSV should contain:
 * warehouseId, aisle, bay, capacity
 *
 * Validates the CSV format and values before loading.
 */
public class BayCsvLoader {

    /**
     * Reads bays from a CSV file and registers their capacity in the InventoryService.
     *
     * @param csvPath path to the CSV file
     * @param inv the InventoryService instance to define bays
     * @return CsvValidatorResult containing successfully added bays and any errors
     * @throws IOException if the CSV file cannot be read
     */
    public static CsvValidatorResult<String> load(String csvPath, InventoryService inv) throws IOException {
        CsvValidatorResult<String> result = new CsvValidatorResult<>();

        CsvReader.readCsv(csvPath, (lineNo, f) -> {
            if (f.length < 4) {
                result.addError("bays.csv line " + lineNo + ": expected 4 columns, got " + f.length);
                return;
            }

            String warehouseId = f[0].trim();
            int aisle, bay, capacity;
            try {
                aisle = Integer.parseInt(f[1].trim());
                bay = Integer.parseInt(f[2].trim());
                capacity = Integer.parseInt(f[3].trim());
            } catch (Exception e) {
                result.addError("bays.csv line " + lineNo + ": invalid integers: " + e.getMessage());
                return;
            }

            if (warehouseId.isEmpty() || aisle <= 0 || bay <= 0 || capacity < 0) {
                result.addError("bays.csv line " + lineNo + ": invalid values");
                return;
            }

            // Define bay capacity in the InventoryService
            inv.defineBayCapacity(warehouseId, aisle, bay, capacity);

            // Record successfully loaded bay
            result.addRecord(warehouseId + ":" + aisle + ":" + bay);
        });

        return result;
    }
}
