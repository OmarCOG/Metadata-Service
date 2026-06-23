package com.bluebolt.fileparser.orchestrator;

import com.bluebolt.fileparser.model.ParsedFile;
import com.bluebolt.fileparser.parser.CsvParserAgent;
import com.bluebolt.fileparser.parser.JsonParserAgent;
import com.bluebolt.fileparser.skill.FileFormatDetectionSkill;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileParserOrchestrator {

    @Autowired
    private JsonParserAgent jsonParser;

    @Autowired
    private CsvParserAgent csvParser;

    @Autowired
    private FileFormatDetectionSkill detectionSkill;

    public ParsedFile parseFile(MultipartFile file) {

        try {
            String fileName = file.getOriginalFilename();

            // ✅ Step 1: Detect format using skill
            String format = detectionSkill.detectFormat(fileName);

            // ✅ Step 2: Route to correct parser
            switch (format) {
                case "json":
                    return jsonParser.parse(file.getInputStream(), fileName);

                case "csv":
                    return csvParser.parse(file.getInputStream(), fileName);

                case "xml":
                case "xlsx":
                    throw new UnsupportedOperationException(
                            "Parser not yet implemented for format: " + format
                    );

                default:
                    throw new UnsupportedOperationException(
                            "Unsupported file format: " + format
                    );
            }

        } catch (Exception e) {
            throw new RuntimeException("Parsing failed: " + e.getMessage(), e);
        }
    }
}