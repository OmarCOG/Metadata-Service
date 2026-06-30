package com.mes.catalogue.dto;

import java.time.Instant;
import java.util.List;

/** Lightweight catalogue-listing row — no field metadata, no file bytes. */
public record CatalogueSummary(
        Long id,
        String title,
        String description,
        String dataSteward,
        String sourceFileName,
        String fileFormat,
        int totalRecords,
        int totalFields,
        int piiFieldsCount,
        int npiFieldsCount,
        int pciFieldsCount,
        boolean piiData,
        boolean pciData,
        int dataRetentionYears,
        List<String> tags,
        Instant createdAt) {
}
