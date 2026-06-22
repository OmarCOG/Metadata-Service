package com.mes.skills;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Detects the logical file format from a file name (extension) and/or its
 * MIME / content type.
 *
 * <p>Supported formats: {@code json}, {@code csv}, {@code xml}, {@code xlsx}.
 * When the format cannot be determined the literal {@code "unknown"} is
 * returned; routing decisions are left to the orchestrator.</p>
 */
@Component
public class FileFormatDetectionSkill {

    public static final String UNKNOWN = "unknown";

    private static final Set<String> SUPPORTED = Set.of("json", "csv", "xml", "xlsx");

    /** Maps a file extension to the canonical format identifier. */
    private static final Map<String, String> EXTENSION_FORMATS = Map.of(
            "json", "json",
            "csv", "csv",
            "xml", "xml",
            "xlsx", "xlsx"
    );

    /** Maps common MIME / content types to the canonical format identifier. */
    private static final Map<String, String> MIME_FORMATS = Map.of(
            "application/json", "json",
            "text/csv", "csv",
            "application/csv", "csv",
            "application/xml", "xml",
            "text/xml", "xml",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"
    );

    /**
     * Detects the format using the file name first, then falling back to the
     * supplied content type.
     *
     * @param fileName    original file name (may be {@code null})
     * @param contentType MIME / content type (may be {@code null})
     * @return a supported format identifier, or {@link #UNKNOWN}
     */
    public String detect(String fileName, String contentType) {
        String byExtension = detectFromExtension(fileName);
        if (isSupported(byExtension)) {
            return byExtension;
        }

        String byMime = detectFromMimeType(contentType);
        if (isSupported(byMime)) {
            return byMime;
        }

        return UNKNOWN;
    }

    /** Detects the format purely from the file-name extension. */
    public String detectFromExtension(String fileName) {
        if (fileName == null) {
            return UNKNOWN;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return UNKNOWN;
        }
        String ext = fileName.substring(dot + 1).trim().toLowerCase();
        return EXTENSION_FORMATS.getOrDefault(ext, UNKNOWN);
    }

    /** Detects the format purely from the MIME / content type. */
    public String detectFromMimeType(String contentType) {
        if (contentType == null) {
            return UNKNOWN;
        }
        // Strip any parameters such as "; charset=utf-8".
        String normalized = contentType.split(";")[0].trim().toLowerCase();
        return MIME_FORMATS.getOrDefault(normalized, UNKNOWN);
    }

    public boolean isSupported(String format) {
        return format != null && SUPPORTED.contains(format);
    }
}
