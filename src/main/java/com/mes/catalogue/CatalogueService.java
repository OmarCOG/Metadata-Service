package com.mes.catalogue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.catalogue.dto.CatalogueDetail;
import com.mes.catalogue.dto.CatalogueSubmitRequest;
import com.mes.catalogue.dto.CatalogueSummary;
import com.mes.models.EnhancedMetadataResponse;
import com.mes.models.FieldMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Persists registered datasets (metadata + original file) and serves them back
 * for the catalogue listing, detail view, and original-file download.
 */
@Service
public class CatalogueService {

    private final DatasetRepository datasetRepository;
    private final DatasetFileStorage fileStorage;
    private final ObjectMapper objectMapper;

    public CatalogueService(DatasetRepository datasetRepository,
                            DatasetFileStorage fileStorage,
                            ObjectMapper objectMapper) {
        this.datasetRepository = datasetRepository;
        this.fileStorage = fileStorage;
        this.objectMapper = objectMapper;
    }

    /** Registers a dataset: saves its metadata, stores the original file bytes. */
    @Transactional
    public CatalogueDetail register(CatalogueSubmitRequest req, MultipartFile file) {
        if (req == null || req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("Dataset title is required");
        }
        if (req.getMetadata() == null) {
            throw new IllegalArgumentException("Dataset metadata is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Original dataset file is required");
        }

        EnhancedMetadataResponse meta = req.getMetadata();
        List<FieldMetadata> fields = meta.getFields() == null ? List.of() : meta.getFields();

        DatasetRecord record = new DatasetRecord();
        record.setTitle(req.getTitle().trim());
        record.setDescription(req.getDescription());
        record.setOwnerName(req.getOwnerName());
        record.setOwnerEmail(req.getOwnerEmail());
        record.setOwnerRole(req.getOwnerRole());
        record.setSourceFileName(meta.getFileName() != null ? meta.getFileName() : file.getOriginalFilename());
        record.setFileFormat(meta.getFileFormat());
        record.setTotalRecords(meta.getTotalRecords());
        record.setTotalFields(meta.getTotalFields());
        record.setPciFieldsCount(meta.getPciFieldsCount());
        record.setNpiFieldsCount(meta.getNpiFieldsCount());
        record.setPhiFieldsCount(meta.getPhiFieldsCount());
        record.setCreatedAt(Instant.now());
        record.setFieldsJson(writeFields(fields));
        record.setAllTags(collectTags(fields));

        DatasetRecord saved = datasetRepository.save(record);

        try {
            fileStorage.store(saved.getId(), file.getOriginalFilename(),
                    file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded dataset file", e);
        }

        return toDetail(saved, fields);
    }

    /** All datasets, newest first, as lightweight summaries. */
    @Transactional(readOnly = true)
    public List<CatalogueSummary> listSummaries() {
        return datasetRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /** Full detail for one dataset, including rehydrated per-field metadata. */
    @Transactional(readOnly = true)
    public CatalogueDetail getDetail(Long id) {
        DatasetRecord record = datasetRepository.findById(id)
                .orElseThrow(() -> new DatasetNotFoundException(id));
        return toDetail(record, readFields(record.getFieldsJson()));
    }

    /** The original uploaded file for a dataset. */
    @Transactional(readOnly = true)
    public DatasetFileStorage.StoredFile getFile(Long id) {
        if (!datasetRepository.existsById(id)) {
            throw new DatasetNotFoundException(id);
        }
        return fileStorage.load(id).orElseThrow(() -> new DatasetNotFoundException(id));
    }

    // ── mapping helpers ──

    private CatalogueSummary toSummary(DatasetRecord r) {
        return new CatalogueSummary(
                r.getId(), r.getTitle(), r.getDescription(), r.getOwnerName(),
                r.getSourceFileName(), r.getFileFormat(), r.getTotalRecords(),
                r.getTotalFields(), r.getPciFieldsCount(), r.getNpiFieldsCount(),
                r.getCreatedAt());
    }

    private CatalogueDetail toDetail(DatasetRecord r, List<FieldMetadata> fields) {
        return new CatalogueDetail(
                r.getId(), r.getTitle(), r.getDescription(), r.getOwnerName(),
                r.getOwnerEmail(), r.getOwnerRole(), r.getSourceFileName(),
                r.getFileFormat(), r.getTotalRecords(), r.getTotalFields(),
                r.getPciFieldsCount(), r.getNpiFieldsCount(), r.getPhiFieldsCount(),
                r.getCreatedAt(), fields);
    }

    private String writeFields(List<FieldMetadata> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize field metadata", e);
        }
    }

    private List<FieldMetadata> readFields(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FieldMetadata>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize field metadata", e);
        }
    }

    private Set<String> collectTags(List<FieldMetadata> fields) {
        return fields.stream()
                .map(FieldMetadata::getTags)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
