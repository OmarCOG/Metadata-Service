package com.mes.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalized representation of a parsed file.
 *
 * <p>Every parser (Excel, XML, and any future format) must return this same
 * structure so that the downstream Metadata Extraction Engine can operate
 * independently of the original file format.</p>
 */
public class ParsedFile {

    /** Format identifier the file was parsed as (e.g. {@code xlsx}, {@code xml}). */
    private String fileFormat;

    /** Ordered list of every field/column name discovered across all records. */
    private List<String> fieldNames = new ArrayList<>();

    /** One map per record; keys are field names, values are the cell/element values. */
    private List<Map<String, Object>> records = new ArrayList<>();

    public ParsedFile() {
    }

    public ParsedFile(String fileFormat, List<String> fieldNames, List<Map<String, Object>> records) {
        this.fileFormat = fileFormat;
        this.fieldNames = fieldNames != null ? fieldNames : new ArrayList<>();
        this.records = records != null ? records : new ArrayList<>();
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(List<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    public List<Map<String, Object>> getRecords() {
        return records;
    }

    public void setRecords(List<Map<String, Object>> records) {
        this.records = records;
    }

    @Override
    public String toString() {
        return "ParsedFile{" +
                "fileFormat='" + fileFormat + '\'' +
                ", fieldNames=" + fieldNames +
                ", records=" + records.size() + " record(s)" +
                '}';
    }

    /** Convenience copy of an existing record map preserving insertion order. */
    public static Map<String, Object> newRecord() {
        return new LinkedHashMap<>();
    }
}
