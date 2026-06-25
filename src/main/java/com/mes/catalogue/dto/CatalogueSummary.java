package com.mes.catalogue.dto;

import java.time.Instant;

/** Lightweight catalogue-listing row — no field metadata, no file bytes. */
public record CatalogueSummary(
        Long id,
        String title,
        String description,
        String ownerName,
        String sourceFileName,
        String fileFormat,
        int totalRecords,
        int totalFields,
        int pciFieldsCount,
        int npiFieldsCount,
        Instant createdAt) {
}
