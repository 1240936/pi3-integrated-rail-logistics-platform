package wms.io;

import wms.model.Item;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemsCsvLoader {
    public static CsvValidatorResult<Item> load(String csvPath) throws IOException {
        CsvValidatorResult<Item> result = new CsvValidatorResult<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine();
            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] f = splitFlexible(line);
                if (f.length < 6) {
                    result.addError("items.csv line " + lineNo + ": expected 6 columns, got " + f.length);
                    continue;
                }
                String sku = f[0].trim();
                String name = f[1].trim();
                String category = f[2].trim();   // separar as string em nos seus headers
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

    private static String[] splitFlexible(String line) {  // flexible method for reading lines with both "," and ";"
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && (c == ',' || c == ';')) {  // encontrar pontos e virgulas, fora das aspas para saber que acabou a coluna
                parts.add(trimQuotes(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(trimQuotes(current.toString()));
        return parts.toArray(new String[0]);
    }

    private static String trimQuotes(String s) {  // retirar tudo que nao é necessario numa string
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t;
    }
}