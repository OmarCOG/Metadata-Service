package com.mes.models;

import java.util.List;

public class FieldMetadata {
    private String fieldName;
    private String dataType;
    private boolean nullable;
    private int nullCount;
    private int uniqueCount;
    private List<String> sampleValues;
    private List<String> tags;
    private String description;
    // Multi-label sensitive-data flags (a field may be true for several at once).
    private boolean piiData;        // Personally Identifiable Information (NIST 800-122)
    private boolean npiData;        // Nonpublic Personal Information — financial (GLBA)
    private boolean pciData;        // Payment card / cardholder data (PCI-DSS)
    private boolean phiData;        // Dormant — not surfaced (kept for back-compat)

    // Getters and Setters
    public String getFieldName() {
        return fieldName; }
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName; }
    public String getDataType() {
        return dataType; }
    public void setDataType(String dataType) {
        this.dataType = dataType; }
    public boolean isNullable() {
        return nullable; }
    public void setNullable(boolean nullable) {
        this.nullable = nullable; }
    public int getNullCount() {
        return nullCount; }
    public void setNullCount(int nullCount) {
        this.nullCount = nullCount; }
    public int getUniqueCount() {
        return uniqueCount; }
    public void setUniqueCount(int uniqueCount) {
        this.uniqueCount = uniqueCount; }
    public List<String> getSampleValues() {
        return sampleValues; }
    public void setSampleValues(List<String> sampleValues) {
        this.sampleValues = sampleValues; }
    public List<String> getTags() {
        return tags; }
    public void setTags(List<String> tags) {
        this.tags = tags; }
    public String getDescription() {
        return description; }
    public void setDescription(String description) {
        this.description = description; }
    public boolean isPiiData() {
        return piiData; }
    public void setPiiData(boolean piiData) {
        this.piiData = piiData; }
    public boolean isPciData() {
        return pciData; }
    public void setPciData(boolean pciData) {
        this.pciData = pciData; }
    public boolean isNpiData() {
        return npiData; }
    public void setNpiData(boolean npiData) {
        this.npiData = npiData; }
    public boolean isPhiData() {
        return phiData; }
    public void setPhiData(boolean phiData) {
        this.phiData = phiData; }
}