package main.repositories;

import main.controller.CsvReader;
import main.domain.OrderHeader;
import main.domain.OrderLine;

import java.io.IOException;
import java.util.Map;

public class OrderLinesCsvLoader {

    /**
     * Loads order lines and validates cross-references with provided order headers.
     * Expected columns: orderId, lineNo, sku, qty
     */
    public static CsvValidatorResult<OrderLine> load(
            String csvPath,
            Map<String, OrderHeader> headersById
    ) throws IOException {

        CsvValidatorResult<OrderLine> result = new CsvValidatorResult<>();

        CsvReader.readCsv(csvPath, (lineNo, f) -> {
            if (f.length < 4) {
                result.addError("order lines.csv line " + lineNo + ": expected 4 columns, got " + f.length);
                return;
            }

            String orderId = f[0].trim();
            String lineNoRaw = f[1].trim();
            String sku = f[2].trim();
            String qtyRaw = f[3].trim();

            if (orderId.isEmpty()) {
                result.addError("order lines.csv line " + lineNo + ": missing orderId");
                return;
            }

            OrderHeader header = headersById.get(orderId);
            if (header == null) {
                result.addError("order lines.csv line " + lineNo + ": unknown orderId '" + orderId + "'");
                return;
            }

            int lineNoVal;
            try {
                lineNoVal = Integer.parseInt(lineNoRaw);
            } catch (Exception e) {
                result.addError("order lines.csv line " + lineNo + ": invalid lineNo");
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(qtyRaw);
                if (qty <= 0) throw new IllegalArgumentException("qty <= 0");
            } catch (Exception e) {
                result.addError("order lines.csv line " + lineNo + ": invalid qty");
                return;
            }

            if (sku.isEmpty()) {
                result.addError("order lines.csv line " + lineNo + ": missing sku");
                return;
            }

            // Build a valid OrderLine
            OrderLine ol = new OrderLine(orderId, lineNoVal, sku, qty,
                    header.getPriority(), header.getDueDate());

            result.addRecord(ol);
        });

        return result;
    }
}
