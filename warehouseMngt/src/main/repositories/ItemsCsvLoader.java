package main.repositories;

import main.controller.CsvReader;
import main.domain.Item;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility class to load items from a CSV file into {@link Item} objects.
 *
 * <p>Validates the CSV structure, numeric fields, and mandatory SKU.
 * Records any errors encountered and returns both valid items and errors.</p>
 */
public class ItemsCsvLoader {

    /**
     * Loads items from the given CSV file path.
     *
     * <p>Expected CSV columns (in order):
     * <ul>
     *   <li>SKU (String, required)</li>
     *   <li>Name (String)</li>
     *   <li>Category (String)</li>
     *   <li>Unit (String)</li>
     *   <li>Volume (double)</li>
     *   <li>Unit Weight (double)</li>
     * </ul>
     * </p>
     *
     * @param csvPath path to the CSV file
     * @return {@link CsvValidatorResult} containing valid {@link Item} records and any errors
     * @throws IOException if reading the CSV file fails
     */
    public static CsvValidatorResult<Item> load(String csvPath) throws IOException {
        CsvValidatorResult<Item> result = new CsvValidatorResult<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine(); // Skip header
            String line;
            int lineNo = 1;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;

                String[] f = CsvReader.splitFlexible(line);

                if (f.length < 6) {
                    result.addError("items.csv line " + lineNo + ": expected 6 columns, got " + f.length);
                    continue;
                }

                String sku = f[0].trim();
                String name = f[1].trim();
                String category = f[2].trim();
                String unit = f[3].trim();

                double volume;
                double unitWeight;

                try {
                    volume = Double.parseDouble(f[4].trim());
                    unitWeight = Double.parseDouble(f[5].trim());
                } catch (Exception e) {
                    result.addError("items.csv line " + lineNo + ": invalid numeric values: " + e.getMessage());
                    continue;
                }

                if (sku.isEmpty()) {
                    result.addError("items.csv line " + lineNo + ": missing SKU");
                    continue;
                }

                result.addRecord(new Item(sku, name, category, unit, volume, unitWeight));
            }
        }

        return result;
    }
}
