package wms.io;

import wms.service.InventoryService;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BayCsvLoader {
    public static CsvValidatorResult<String> load(String csvPath, InventoryService inv) throws IOException {
        CsvValidatorResult<String> result = new CsvValidatorResult<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine(); // header
            String line;
            int Nlinha = 1;
            while ((line = br.readLine()) != null) {
                Nlinha++;

                if (line.trim().isEmpty()) continue;

                String[] f = splitFlexible(line);
                if (f.length < 4) {
                    result.addError("bays.csv line " + Nlinha + ": expected 4 columns, got " + f.length);
                    continue;
                }

                String warehouseId = f[0].trim();
                int aisle;
                int bay;
                int capacity;

                try {
                    aisle = Integer.parseInt(f[1].trim());
                    bay = Integer.parseInt(f[2].trim());
                    capacity = Integer.parseInt(f[3].trim());
                } catch (Exception e) {
                    result.addError("bays.csv line " + Nlinha + ": invalid integers: " + e.getMessage());
                    continue;
                }
                if (warehouseId.isEmpty() || aisle <= 0 || bay <= 0 || capacity < 0) {
                    result.addError("bays.csv line " + Nlinha + ": invalid values");
                    continue;
                }

                inv.defineBayCapacity(warehouseId, aisle, bay, capacity);
                result.addRecord(warehouseId + ":" + aisle + ":" + bay);
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
