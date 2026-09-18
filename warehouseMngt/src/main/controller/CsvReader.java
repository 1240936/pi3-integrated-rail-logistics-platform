package main.controller;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Utility class for reading CSV files with flexible delimiter handling.
 * This reader supports comma and semicolon separated fields, including quoted fields.
 * Empty lines are skipped and the first line (header) is ignored.
 */
public class CsvReader {

    /**
     * Reads a CSV file and invokes a handler for each non-empty data line.
     * The first line is treated as a header and skipped automatically.
     *
     * @param csvPath Path to the CSV file to read.
     * @param handler Callback to process each parsed line. The callback receives:
     *                1. The line number (starting at 2 after the header).
     *                2. An array of parsed field values.
     * @throws IOException If the file cannot be opened or read.
     */
    public static void readCsv(String csvPath, BiConsumer<Integer, String[]> handler) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine(); // skip header
            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] fields = splitFlexible(line);
                handler.accept(lineNo, fields);
            }
        }
    }

    /**
     * Splits a CSV line into fields, supporting comma and semicolon delimiters.
     * Quoted fields are preserved even if they contain delimiters.
     *
     * @param line A full CSV line.
     * @return An array of parsed and cleaned field values.
     */
    public static String[] splitFlexible(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && (c == ',' || c == ';')) {
                parts.add(trimQuotes(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(trimQuotes(current.toString()));
        return parts.toArray(new String[0]);
    }

    /**
     * Removes wrapping quotes from a field if present and trims whitespace.
     *
     * @param s The field string to clean.
     * @return A cleaned string without surrounding quotes.
     */
    private static String trimQuotes(String s) {
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t;
    }
}
