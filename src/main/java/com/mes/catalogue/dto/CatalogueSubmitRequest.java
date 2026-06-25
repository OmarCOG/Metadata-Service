package com.mes.catalogue.dto;

import com.mes.models.EnhancedMetadataResponse;

/**
 * The JSON {@code payload} part of a catalogue registration request. The
 * original file travels as a separate multipart {@code file} part.
 *
 * <p>{@code metadata} reuses {@link EnhancedMetadataResponse} (camelCase) so the
 * frontend sends the same field shape the backend already produces.
 */
public class CatalogueSubmitRequest {

    private String title;
    private String description;
    private String ownerName;
    private String ownerEmail;
    private String ownerRole;
    private EnhancedMetadataResponse metadata;

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

    public EnhancedMetadataResponse getMetadata() { return metadata; }
    public void setMetadata(EnhancedMetadataResponse metadata) { this.metadata = metadata; }
}
