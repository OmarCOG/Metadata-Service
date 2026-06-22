package com.mes.skills;

import com.mes.models.ParsedFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Validates and normalizes the {@link ParsedFile} output of any parser so that
 * every downstream consumer receives a consistent structure.
 *
 * <p>Guarantees enforced by {@link #normalize(ParsedFile)}:</p>
 * <ul>
 *   <li>{@code fileFormat} is always set (non-null, non-blank).</li>
 *   <li>{@code fieldNames} is never {@code null} or empty &mdash; when a parser
 *       does not supply them they are derived from the record keys.</li>
 *   <li>{@code records} is a consistent {@code List<Map<String, Object>>}: every
 *       record contains exactly the same set of keys (missing values are filled
 *       with {@code null}).</li>
 * </ul>
 */
@Component
public class DataNormalizationSkill {

    /**
     * Validates and returns a normalized copy-in-place of the supplied parsed file.
     *
     * @param parsedFile the parser output to normalize
     * @return the same instance, normalized
     * @throws IllegalArgumentException if {@code parsedFile} is {@code null}
     * @throws IllegalStateException    if the file format is missing or no field
     *                                  names can be determined
     */
    public ParsedFile normalize(ParsedFile parsedFile) {
        if (parsedFile == null) {
            throw new IllegalArgumentException("ParsedFile must not be null");
        }

        validateFileFormat(parsedFile);

        // Defensively replace nulls with empty collections.
        List<Map<String, Object>> records = parsedFile.getRecords() != null
                ? parsedFile.getRecords()
                : new ArrayList<>();

        List<String> fieldNames = parsedFile.getFieldNames() != null
                ? new ArrayList<>(parsedFile.getFieldNames())
                : new ArrayList<>();

        // Build the canonical, de-duplicated, ordered set of field names. Start
        // with any names the parser declared, then union in keys seen on records.
        LinkedHashSet<String> canonical = new LinkedHashSet<>(fieldNames);
        for (Map<String, Object> record : records) {
            if (record != null) {
                canonical.addAll(record.keySet());
            }
        }

        if (canonical.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot normalize ParsedFile: no field names declared and no record keys found");
        }

        // Rebuild every record so each one carries the full, ordered key set.
        List<Map<String, Object>> normalizedRecords = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            Map<String, Object> consistent = new LinkedHashMap<>();
            for (String field : canonical) {
                consistent.put(field, record != null ? record.get(field) : null);
            }
            normalizedRecords.add(consistent);
        }

        parsedFile.setFieldNames(new ArrayList<>(canonical));
        parsedFile.setRecords(normalizedRecords);
        return parsedFile;
    }

    private void validateFileFormat(ParsedFile parsedFile) {
        String format = parsedFile.getFileFormat();
        if (format == null || format.isBlank()) {
            throw new IllegalStateException("ParsedFile.fileFormat must be set by the parser");
        }
    }
}
