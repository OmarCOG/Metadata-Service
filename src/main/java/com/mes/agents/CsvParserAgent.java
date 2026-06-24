package com.mes.agents;

import com.mes.models.ParsedFile;
import com.mes.skills.DataNormalizationSkill;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses {@code .csv} documents into a normalized {@link ParsedFile}.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>The first non-empty line is treated as the header row, producing the
 *       field names; blank headers are replaced with {@code column_N}.</li>
 *   <li>Each remaining line becomes one record; missing trailing values are
 *       stored as {@code null}, as are empty cells.</li>
 *   <li>Values are quote-aware: a field wrapped in double quotes may contain
 *       commas and escaped quotes ({@code ""}).</li>
 * </ul>
 */
@Component
public class CsvParserAgent {

    private static final String FORMAT = "csv";

    private final DataNormalizationSkill normalizationSkill;

    public CsvParserAgent(DataNormalizationSkill normalizationSkill) {
        this.normalizationSkill = normalizationSkill;
    }

    public ParsedFile parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String headerLine = readNonEmptyLine(reader);
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV document is empty");
            }

            List<String> fieldNames = toFieldNames(splitRow(headerLine));

            List<Map<String, Object>> records = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue; // skip fully blank lines
                }
                List<String> values = splitRow(line);
                Map<String, Object> record = new LinkedHashMap<>();
                for (int i = 0; i < fieldNames.size(); i++) {
                    String value = i < values.size() ? values.get(i) : null;
                    record.put(fieldNames.get(i), normalizeCell(value));
                }
                records.add(record);
            }

            ParsedFile parsedFile = new ParsedFile(FORMAT, fieldNames, records);
            return normalizationSkill.normalize(parsedFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read csv document", e);
        }
    }

    /** Reads forward until a non-blank line is found (skips leading blank lines). */
    private String readNonEmptyLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                return line;
            }
        }
        return null;
    }

    /** Builds field names from header cells, substituting {@code column_N} for blanks. */
    private List<String> toFieldNames(List<String> headerCells) {
        List<String> fieldNames = new ArrayList<>(headerCells.size());
        for (int c = 0; c < headerCells.size(); c++) {
            String name = headerCells.get(c) != null ? headerCells.get(c).trim() : "";
            fieldNames.add(name.isEmpty() ? "column_" + c : name);
        }
        return fieldNames;
    }

    /** Splits a single CSV line into trimmed cell values, honouring quoted fields. */
    private List<String> splitRow(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                // A doubled quote inside a quoted field is a literal quote.
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString().trim());
        return cells;
    }

    /** Empty strings become {@code null}; everything else is kept verbatim. */
    private Object normalizeCell(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
