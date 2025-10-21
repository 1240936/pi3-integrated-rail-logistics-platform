package main.repositories;

import main.controller.CsvReader;
import main.domain.Item;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ItemsCsvLoader {
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
