package com.mes.catalogue.dto;

import com.mes.models.FieldMetadata;

import java.util.List;

/**
 * Edits to an existing catalogue entry. Any field left {@code null} is left
 * unchanged; supplying {@code fields} replaces the per-field metadata (which
 * also recomputes the compliance counts and the denormalized tag set).
 */
public class CatalogueUpdateRequest {

    private String title;
    private String description;
    private List<FieldMetadata> fields;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<FieldMetadata> getFields() { return fields; }
    public void setFields(List<FieldMetadata> fields) { this.fields = fields; }
}
