package main.repositories;

import main.controller.CsvReader;
import main.domain.OrderHeader;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class to load order headers from a CSV file.
 *
 * <p>Each row in the CSV should contain the following columns in order:
 * <ul>
 *   <li>orderId (String, required)</li>
 *   <li>dueDate (ISO date or ISO datetime, required)</li>
 *   <li>priority (int, required)</li>
 * </ul>
 * </p>
 *
 * <p>Invalid rows are captured as errors in the returned
 * {@link CsvValidatorResult}.</p>
 */
public class OrdersCsvLoader {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Loads order headers from the specified CSV file.
     *
     * @param csvPath path to the CSV file containing order headers
     * @return {@link CsvValidatorResult} containing valid {@link OrderHeader} records and any errors
     * @throws IOException if reading the CSV file fails
     */
    public static CsvValidatorResult<OrderHeader> load(String csvPath) throws IOException {
        CsvValidatorResult<OrderHeader> result = new CsvValidatorResult<>();

        CsvReader.readCsv(csvPath, (lineNo, f) -> {
            if (f.length < 3) {
                result.addError("orders.csv line " + lineNo + ": expected 3 columns, got " + f.length);
                return;
            }

            String orderId = f[0].trim();
            String dueRaw = f[1].trim();
            String priRaw = f[2].trim();

            if (orderId.isEmpty()) {
                result.addError("orders.csv line " + lineNo + ": missing orderId");
                return;
            }

            LocalDate dueDate;
            try {
                if (dueRaw.contains("T")) {
                    // Accept ISO datetime with or without offset; take the date component
                    try {
                        dueDate = OffsetDateTime.parse(dueRaw, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
                    } catch (Exception ex) {
                        dueDate = LocalDateTime.parse(dueRaw, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
                    }
                } else {
                    dueDate = LocalDate.parse(dueRaw, DATE);
                }
            } catch (Exception e) {
                result.addError("orders.csv line " + lineNo + ": invalid dueDate");
                return;
            }

            int priority;
            try {
                priority = Integer.parseInt(priRaw);
            } catch (Exception e) {
                result.addError("orders.csv line " + lineNo + ": invalid priority");
                return;
            }

            result.addRecord(new OrderHeader(orderId, dueDate, priority));
        });

        return result;
    }
}
