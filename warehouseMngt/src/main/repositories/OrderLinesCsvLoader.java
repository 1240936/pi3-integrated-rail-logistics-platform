package main.repositories;

import main.controller.CsvReader;
import main.domain.OrderHeader;
import main.domain.OrderLine;

import java.io.IOException;
import java.util.Map;

/**
 * Utility class to load order lines from a CSV file and validate them
 * against existing order headers.
 *
 * <p>Each row in the CSV should contain the following columns in order:
 * <ul>
 *   <li>orderId (String, must exist in headersById)</li>
 *   <li>lineNo (int, positive)</li>
 *   <li>sku (String, required)</li>
 *   <li>qty (int, positive)</li>
 * </ul>
 * </p>
 *
 * <p>Any row that fails validation is recorded in the errors list of the
 * returned {@link CsvValidatorResult}.</p>
 */
public class OrderLinesCsvLoader {

    /**
     * Loads order lines from the CSV file, validating references to order headers.
     *
     * @param csvPath path to the CSV file containing order lines
     * @param headersById map of orderId to {@link OrderHeader} for validation
     * @return {@link CsvValidatorResult} containing valid {@link OrderLine} records and errors
     * @throws IOException if reading the CSV file fails
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
