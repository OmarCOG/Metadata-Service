package com.bluebolt.fileparser.model;

import java.util.List;
import java.util.Map;

public class ParsedFile {

    private String fileFormat;
    private List<String> fieldNames;
    private List<Map<String, Object>> records;

    public ParsedFile(String fileFormat, List<String> fieldNames, List<Map<String, Object>> records) {
        this.fileFormat = fileFormat;
        this.fieldNames = fieldNames;
        this.records = records;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public List<Map<String, Object>> getRecords() {
        return records;
    }
}