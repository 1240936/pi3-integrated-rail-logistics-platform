package main.controller;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class CsvReader {

    /**
     * Reads a CSV file and applies a line handler for each data line (skipping header).
     * @param csvPath path to the CSV file
     * @param handler callback(lineNo, fields) for each non-empty line
     * @throws IOException if file cannot be read
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
     * Splits a CSV line supporting both "," and ";" delimiters and quoted fields.
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

    private static String trimQuotes(String s) {
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t;
    }
}
