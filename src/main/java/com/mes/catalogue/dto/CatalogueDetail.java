package com.mes.catalogue.dto;

import com.mes.models.FieldMetadata;

import java.time.Instant;
import java.util.List;

/** Full catalogue entry for the detail view — includes per-field metadata. */
public record CatalogueDetail(
        Long id,
        String title,
        String description,
        String dataSteward,
        boolean piiData,
        boolean pciData,
        int dataRetentionYears,
        List<String> tags,
        String sourceFileName,
        String fileFormat,
        int totalRecords,
        int totalFields,
        int piiFieldsCount,
        int npiFieldsCount,
        int pciFieldsCount,
        Instant createdAt,
        List<FieldMetadata> fields) {
}
