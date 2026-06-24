package com.bluebolt.fileparser.skill;

import com.bluebolt.fileparser.model.ParsedFile;
import org.springframework.stereotype.Component;

@Component
public class DataNormalizationSkill {

    public ParsedFile normalize(ParsedFile parsedFile) {

        if (parsedFile.getFileFormat() == null) {
            throw new RuntimeException("Missing file format");
        }

        if (parsedFile.getFieldNames() == null || parsedFile.getFieldNames().isEmpty()) {
            throw new RuntimeException("Missing field names");
        }

        if (parsedFile.getRecords() == null) {
            throw new RuntimeException("Missing records");
        }

        return parsedFile;
    }
}