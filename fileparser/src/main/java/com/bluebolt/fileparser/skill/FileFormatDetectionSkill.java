package com.bluebolt.fileparser.skill;

import org.springframework.stereotype.Component;

@Component
public class FileFormatDetectionSkill {

    public String detectFormat(String fileName) {

        if (fileName == null || fileName.isEmpty()) {
            throw new RuntimeException("File name is missing");
        }

        String lowerFileName = fileName.toLowerCase();

        if (lowerFileName.endsWith(".json")) {
            return "json";
        } else if (lowerFileName.endsWith(".csv")) {
            return "csv";
        } else if (lowerFileName.endsWith(".xml")) {
            return "xml";
        } else if (lowerFileName.endsWith(".xlsx")) {
            return "xlsx";
        } else {
            throw new UnsupportedOperationException("Unsupported file format: " + fileName);
        }
    }
}
