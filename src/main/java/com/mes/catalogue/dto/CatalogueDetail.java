package com.mes.catalogue.dto;

import com.mes.models.FieldMetadata;

import java.time.Instant;
import java.util.List;

/** Full catalogue entry for the detail view — includes per-field metadata. */
public record CatalogueDetail(
        Long id,
        String title,
        String description,
        String ownerName,
        String ownerEmail,
        String ownerRole,
        String sourceFileName,
        String fileFormat,
        int totalRecords,
        int totalFields,
        int pciFieldsCount,
        int npiFieldsCount,
        int phiFieldsCount,
        Instant createdAt,
        List<FieldMetadata> fields) {
}
