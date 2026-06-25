package com.mes.catalogue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * The raw bytes of an uploaded dataset, stored separately from
 * {@link DatasetRecord} so catalogue listings never load blob content.
 * One row per registered dataset, keyed by {@code datasetId}.
 *
 * <p>On AWS this DB-backed storage can be swapped for S3 by providing an
 * alternate {@link DatasetFileStorage} implementation — see {@link DbFileStorage}.
 */
@Entity
@Table(name = "dataset_file_blob")
public class DatasetFileBlob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long datasetId;

    private String originalFileName;
    private String contentType;

    @Lob
    @Column(nullable = false)
    private byte[] content;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
}
