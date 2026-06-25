package com.mes.catalogue;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A dataset registered into the Exchange catalogue. Holds the dataset-level
 * title/description, the submitter (owner), summary counts, and the full
 * per-field metadata serialized as JSON ({@code fieldsJson}). The original
 * uploaded file is stored separately in {@link DatasetFileBlob} so that
 * catalogue-listing queries never drag the raw bytes.
 */
@Entity
@Table(name = "dataset_record")
public class DatasetRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    private String ownerName;
    private String ownerEmail;
    private String ownerRole;

    private String sourceFileName;
    private String fileFormat;

    private int totalRecords;
    private int totalFields;
    private int pciFieldsCount;
    private int npiFieldsCount;
    private int phiFieldsCount;

    private Instant createdAt;

    /**
     * The List&lt;FieldMetadata&gt; serialized to JSON (rehydrated for detail views).
     * {@code @Lob} maps to a large-object type per dialect (H2 CLOB / MySQL LONGTEXT),
     * so large field lists are not truncated.
     */
    @Lob
    private String fieldsJson;

    /** Distinct tags across all fields — denormalized for future catalogue tag search. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dataset_tag", joinColumns = @JoinColumn(name = "dataset_id"))
    @Column(name = "tag")
    private Set<String> allTags = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getOwnerRole() { return ownerRole; }
    public void setOwnerRole(String ownerRole) { this.ownerRole = ownerRole; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getTotalFields() { return totalFields; }
    public void setTotalFields(int totalFields) { this.totalFields = totalFields; }

    public int getPciFieldsCount() { return pciFieldsCount; }
    public void setPciFieldsCount(int pciFieldsCount) { this.pciFieldsCount = pciFieldsCount; }

    public int getNpiFieldsCount() { return npiFieldsCount; }
    public void setNpiFieldsCount(int npiFieldsCount) { this.npiFieldsCount = npiFieldsCount; }

    public int getPhiFieldsCount() { return phiFieldsCount; }
    public void setPhiFieldsCount(int phiFieldsCount) { this.phiFieldsCount = phiFieldsCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }

    public Set<String> getAllTags() { return allTags; }
    public void setAllTags(Set<String> allTags) { this.allTags = allTags; }
}
