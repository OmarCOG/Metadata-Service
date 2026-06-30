package com.mes.models;

import java.util.List;

public class EnhancedMetadataResponse {
    private String fileName;
    private String fileFormat;
    private int totalRecords;
    private int totalFields;
    private int piiFieldsCount;
    private int npiFieldsCount;
    private int pciFieldsCount;
    private int phiFieldsCount;
    private List<FieldMetadata> fields;

    // Getters and Setters
    public String getFileName() {
        return fileName; }
    public void setFileName(String fileName) {
        this.fileName = fileName; }
    public String getFileFormat() {
        return fileFormat; }
    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat; }
    public int getTotalRecords() {
        return totalRecords; }
    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords; }
    public int getTotalFields() {
        return totalFields; }
    public void setTotalFields(int totalFields) {
        this.totalFields = totalFields; }
    public int getPiiFieldsCount() {
        return piiFieldsCount; }
    public void setPiiFieldsCount(int piiFieldsCount) {
        this.piiFieldsCount = piiFieldsCount; }
    public int getPciFieldsCount() {
        return pciFieldsCount; }
    public void setPciFieldsCount(int pciFieldsCount) {
        this.pciFieldsCount = pciFieldsCount; }
    public int getNpiFieldsCount() {
        return npiFieldsCount; }
    public void setNpiFieldsCount(int npiFieldsCount) {
        this.npiFieldsCount = npiFieldsCount; }
    public int getPhiFieldsCount() {
        return phiFieldsCount; }
    public void setPhiFieldsCount(int phiFieldsCount) {
        this.phiFieldsCount = phiFieldsCount; }
    public List<FieldMetadata> getFields() {
        return fields; }
    public void setFields(List<FieldMetadata> fields) {
        this.fields = fields; }
}