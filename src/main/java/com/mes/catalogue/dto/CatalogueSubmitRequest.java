package com.mes.catalogue.dto;

import com.mes.models.EnhancedMetadataResponse;

import java.util.List;

/**
 * The JSON {@code payload} part of a catalogue registration request. The
 * original file travels as a separate multipart {@code file} part.
 *
 * <p>{@code metadata} reuses {@link EnhancedMetadataResponse} (camelCase) so the
 * frontend sends the same field shape the backend already produces.
 */
public class CatalogueSubmitRequest {

    /** Dataset Name — the unique common name (stored as the record's title). */
    private String title;
    private String description;
    /** Search tags entered at registration (up to 10). */
    private List<String> tags;
    /** Email of the data steward responsible for the dataset. */
    private String dataSteward;
    private boolean piiData;
    private boolean pciData;
    /** Retention period in years (1–9). */
    private Integer dataRetentionYears;
    private EnhancedMetadataResponse metadata;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getDataSteward() { return dataSteward; }
    public void setDataSteward(String dataSteward) { this.dataSteward = dataSteward; }

    public boolean isPiiData() { return piiData; }
    public void setPiiData(boolean piiData) { this.piiData = piiData; }

    public boolean isPciData() { return pciData; }
    public void setPciData(boolean pciData) { this.pciData = pciData; }

    public Integer getDataRetentionYears() { return dataRetentionYears; }
    public void setDataRetentionYears(Integer dataRetentionYears) { this.dataRetentionYears = dataRetentionYears; }

    public EnhancedMetadataResponse getMetadata() { return metadata; }
    public void setMetadata(EnhancedMetadataResponse metadata) { this.metadata = metadata; }
}
