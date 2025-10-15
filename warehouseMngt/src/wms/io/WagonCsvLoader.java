package wms.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import wms.model.*;
import wms.service.InventoryService;

public class WagonCsvLoader {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ISO_DATE_TIME;

    public static CsvValidatorResult<Box> load(String csvPath, String defaultWarehouseId, int defaultAisle, InventoryService inv) throws IOException {
        CsvValidatorResult<Box> result = new CsvValidatorResult<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine();
            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] f = splitFlexible(line);
                if (f.length < 6) {
                    result.addError("wagons.csv line " + lineNo + ": expected at least 6 columns");
                    continue;
                }
                // Estrutura para ler: wagonId, boxId, sku, qty, expiryDate (optional), receivedAt
                String wagonId = trimmer(f, 0);
                String boxId = trimmer(f, 1);
                String sku = trimmer(f, 2);
                String qtyRaw = trimmer(f, 3);
                String expiryRaw = trimmer(f, 4);
                String receivedRaw = trimmer(f, 5);
                String aisleRaw;
                if (f.length > 6) {
                    aisleRaw = f[6];
                } else {
                    aisleRaw = "";
                }

                String bayRaw;
                if (f.length > 7) {
                    bayRaw = f[7];
                } else {
                    bayRaw = "";
                }

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

                int aisle = defaultAisle;
                if (!aisleRaw.trim().isEmpty()) {
                    try {
                        aisle = Integer.parseInt(aisleRaw.trim());
                    } catch (Exception e) {
                        result.addError("wagons.csv line " + lineNo + ": invalid aisle (box " + boxId + ")");
                        continue;
                    }
                }
                int bay;
                if (!bayRaw.trim().isEmpty()) {
                    try {
                        bay = Integer.parseInt(bayRaw.trim());
                    } catch (Exception e) {
                        result.addError("wagons.csv line " + lineNo + ": invalid bay (box " + boxId + ")");
                        continue;
                    }
                } else {
                    bay = autoAssignBay(inv, defaultWarehouseId, aisle);
                }

                // AC validar sku (item ja tem que estar carregado)
                if (!inv.isKnownSku(defaultWarehouseId, sku)) {
                    result.addError("wagons.csv line " + lineNo + ": unknown SKU '" + sku + "' (wagon " + wagonId + ")");
                    continue;
                }
                try {
                    Box box = new Box(boxId, sku, expiry, receivedAt, qty, defaultWarehouseId, aisle, bay);
                    inv.insertBox(box);
                    result.addRecord(box);
                } catch (Exception e) {
                    result.addError("wagons.csv line " + lineNo + ": " + e.getMessage() + " (box " + boxId + ")");
                }
            }
        }
        return result;
    }

    private static int autoAssignBay(InventoryService inv, String warehouseId, int aisle) {
        int next = 1;
        while (true) {
            if (inv.getOrCreateBay(warehouseId, aisle, next).getBoxes().isEmpty()) {
                return next;
            }
            next++;
        }
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

    private static String trimmer(String[] f, int idx) {
        if(idx < f.length){
            return f[idx].trim();
        } else{
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
                // Accept ISO datetime; use date component
                return LocalDateTime.parse(t, DATETIME).toLocalDate();
            }
            return LocalDate.parse(t, DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
