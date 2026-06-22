package com.mes.orchestrator;

import com.mes.agents.ExcelParserAgent;
import com.mes.agents.XmlParserAgent;
import com.mes.models.ParsedFile;
import com.mes.skills.FileFormatDetectionSkill;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Coordinates file parsing: detects the format and routes the upload to the
 * appropriate parser agent, returning a normalized {@link ParsedFile}.
 */
@Service
public class FileParserOrchestrator {

    private final FileFormatDetectionSkill formatDetectionSkill;
    private final ExcelParserAgent excelParserAgent;
    private final XmlParserAgent xmlParserAgent;

    public FileParserOrchestrator(FileFormatDetectionSkill formatDetectionSkill,
                                  ExcelParserAgent excelParserAgent,
                                  XmlParserAgent xmlParserAgent) {
        this.formatDetectionSkill = formatDetectionSkill;
        this.excelParserAgent = excelParserAgent;
        this.xmlParserAgent = xmlParserAgent;
    }

    /**
     * Parses an uploaded multipart file.
     *
     * @param file the uploaded file
     * @return a normalized {@link ParsedFile}
     * @throws IllegalArgumentException      if the file is {@code null} or empty
     * @throws UnsupportedOperationException if the format is unrecognized or has
     *                                       no registered parser
     */
    public ParsedFile parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String format = formatDetectionSkill.detect(file.getOriginalFilename(), file.getContentType());

        try (InputStream in = file.getInputStream()) {
            switch (format) {
                case "xlsx":
                    return excelParserAgent.parse(in);
                case "xml":
                    return xmlParserAgent.parse(in);
                default:
                    throw new UnsupportedOperationException(
                            "Unsupported or unrecognized file format: '" + format + "'");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file stream", e);
        }
    }
}
