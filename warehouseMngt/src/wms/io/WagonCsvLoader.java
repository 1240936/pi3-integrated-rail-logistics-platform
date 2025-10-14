package wms.io;

import java.util.ArrayList;
import java.util.List;

public class WagonCsvLoader {


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
