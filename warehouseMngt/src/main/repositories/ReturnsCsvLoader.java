package main.repositories;

import main.domain.Return;
import main.domain.ReturnReason;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for loading return records from a CSV file.
 *
 * <p>Each row in the CSV must contain the following columns in order:
 * <ol>
 *   <li>returnId (String, required)</li>
 *   <li>sku (String, required)</li>
 *   <li>quantity (int, required, >0)</li>
 *   <li>reason (String, required, one of "customer-remorse", "damaged", "expired", "cycle-count")</li>
 *   <li>timestamp (LocalDateTime, required, format yyyy-MM-ddTHH:mm:ss)</li>
 *   <li>expiryDate (LocalDate, optional, format yyyy-MM-dd or empty)</li>
 * </ol>
 * </p>
 *
 * <p>Invalid rows are captured as errors in the returned {@link CsvValidatorResult}.</p>
 */
public class ReturnsCsvLoader {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Loads returns from a CSV file and validates each row.
     *
     * @param filePath path to the CSV file containing return records
     * @return {@link CsvValidatorResult} containing valid {@link Return} records and any validation errors
     */
    public static CsvValidatorResult<Return> load(String filePath) {
        CsvValidatorResult<Return> result = new CsvValidatorResult<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Skip header
            if (line == null) {
                result.addError("Empty CSV file");
                return result;
            }

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    Return returnItem = parseReturnLine(line);
                    if (returnItem != null) {
                        result.addRecord(returnItem);
                    }
                } catch (Exception e) {
                    result.addError("Line " + lineNumber + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            result.addError("Error reading file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parses a single CSV line into a {@link Return} object.
     *
     * @param line the CSV line to parse
     * @return the parsed {@link Return} object
     * @throws Exception if the line cannot be parsed due to missing or invalid values
     */
    private static Return parseReturnLine(String line) throws Exception {
        String[] fields = line.split(",", -1); // -1 keeps empty fields at the end

        if (fields.length != 6) {
            throw new Exception("Expected 6 fields, found " + fields.length);
        }

        // Parse returnId
        String returnId = fields[0].trim();
        if (returnId.isEmpty()) {
            throw new Exception("Return ID cannot be empty");
        }

        // Parse SKU
        String sku = fields[1].trim();
        if (sku.isEmpty()) {
            throw new Exception("SKU cannot be empty");
        }

        // Parse quantity
        int quantity;
        try {
            String qtyStr = fields[2].trim();
            if (qtyStr.isEmpty()) {
                throw new Exception("Quantity cannot be empty");
            }
            quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) {
                throw new Exception("Quantity must be positive");
            }
        } catch (NumberFormatException e) {
            throw new Exception("Invalid quantity: " + fields[2]);
        }

        // Parse reason
        String reasonStr = fields[3].trim().toLowerCase();
        if (reasonStr.isEmpty()) {
            throw new Exception("Reason cannot be empty");
        }
        ReturnReason reason;
        switch (reasonStr) {
            case "customer-remorse":
                reason = ReturnReason.CUSTOMER_REMORSE;
                break;
            case "damaged":
                reason = ReturnReason.DAMAGED;
                break;
            case "expired":
                reason = ReturnReason.EXPIRED;
                break;
            case "cycle-count":
                reason = ReturnReason.CYCLE_COUNT;
                break;
            default:
                throw new Exception("Invalid reason: " + fields[3] + ". Must be one of: customer-remorse, damaged, expired, cycle-count");
        }

        // Parse timestamp
        LocalDateTime timestamp;
        try {
            String timestampStr = fields[4].trim();
            if (timestampStr.isEmpty()) {
                throw new Exception("Timestamp cannot be empty");
            }
            timestamp = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMATTER);
        } catch (Exception e) {
            throw new Exception("Invalid timestamp format: " + fields[4] + ". Expected: yyyy-MM-ddTHH:mm:ss");
        }

        // Parse expiry date (optional)
        LocalDate expiryDate = null;
        String expiryDateStr = fields[5].trim();
        if (!expiryDateStr.isEmpty()) {
            try {
                expiryDate = LocalDate.parse(expiryDateStr, DATE_FORMATTER);
            } catch (Exception e) {
                throw new Exception("Invalid expiry date format: " + fields[5] + ". Expected: yyyy-MM-dd or empty");
            }
        }

        return new Return(returnId, sku, quantity, reason, timestamp, expiryDate);
    }
}
