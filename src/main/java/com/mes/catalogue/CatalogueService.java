package com.mes.catalogue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.catalogue.dto.CatalogueDetail;
import com.mes.catalogue.dto.CatalogueSubmitRequest;
import com.mes.catalogue.dto.CatalogueSummary;
import com.mes.catalogue.dto.CatalogueUpdateRequest;
import com.mes.catalogue.dto.PagedResponse;
import com.mes.models.EnhancedMetadataResponse;
import com.mes.models.FieldMetadata;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Persists registered datasets (metadata + original file) and serves them back
 * for the catalogue: searchable/paginated listing, detail view, original-file
 * download, plus update and delete.
 */
@Service
public class CatalogueService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
            throw new IllegalArgumentException("Dataset name is required");
        }
        if (req.getMetadata() == null) {
            throw new IllegalArgumentException("Dataset metadata is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Original dataset file is required");
        }

        String name = req.getTitle().trim();
        if (datasetRepository.existsByTitleIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A dataset named '" + name + "' already exists. Dataset names must be unique.");
        }
        String steward = req.getDataSteward() == null ? "" : req.getDataSteward().trim();
        if (steward.isEmpty() || !EMAIL.matcher(steward).matches()) {
            throw new IllegalArgumentException("A valid Data Steward email is required");
        }
        int retention = req.getDataRetentionYears() == null ? 7 : req.getDataRetentionYears();
        if (retention < 1 || retention > 9) {
            throw new IllegalArgumentException("Data retention period must be a number of years between 1 and 9");
        }
        List<String> tags = sanitizeTags(req.getTags());
        if (tags.size() > 10) {
            throw new IllegalArgumentException("A maximum of 10 tags is allowed");
        }

        EnhancedMetadataResponse meta = req.getMetadata();
        List<FieldMetadata> fields = meta.getFields() == null ? List.of() : meta.getFields();

        DatasetRecord record = new DatasetRecord();
        record.setTitle(name);
        record.setDescription(req.getDescription());
        record.setDataSteward(steward);
        record.setPiiData(req.isPiiData());
        record.setPciData(req.isPciData());
        record.setDataRetentionYears(retention);
        record.setDatasetTags(tags);
        record.setSourceFileName(meta.getFileName() != null ? meta.getFileName() : file.getOriginalFilename());
        record.setFileFormat(meta.getFileFormat());
        record.setTotalRecords(meta.getTotalRecords());
        record.setCreatedAt(Instant.now());
        applyFieldMetadata(record, fields);

        DatasetRecord saved = datasetRepository.save(record);

        try {
            fileStorage.store(saved.getId(), file.getOriginalFilename(),
                    file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded dataset file", e);
        }

        return toDetail(saved, fields);
    }

    /**
     * Searchable, paginated listing. Any of {@code q} (free text over
     * title/description/owner/source file), {@code tag}, or {@code format} may be
     * null/blank to skip that filter. Sorting/paging come from {@code pageable}.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CatalogueSummary> search(String q, String tag, String format, Pageable pageable) {
        Page<DatasetRecord> page = datasetRepository.findAll(buildSpec(q, tag, format), pageable);
        List<CatalogueSummary> content = page.getContent().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
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

    /** Updates a dataset's title/description and (optionally) its field metadata. */
    @Transactional
    public CatalogueDetail update(Long id, CatalogueUpdateRequest req) {
        DatasetRecord record = datasetRepository.findById(id)
                .orElseThrow(() -> new DatasetNotFoundException(id));

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            record.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) {
            record.setDescription(req.getDescription());
        }

        List<FieldMetadata> fields;
        if (req.getFields() != null) {
            fields = req.getFields();
            applyFieldMetadata(record, fields);
        } else {
            fields = readFields(record.getFieldsJson());
        }

        DatasetRecord saved = datasetRepository.save(record);
        return toDetail(saved, fields);
    }

    /** Deletes a dataset record and its stored original file. */
    @Transactional
    public void delete(Long id) {
        if (!datasetRepository.existsById(id)) {
            throw new DatasetNotFoundException(id);
        }
        fileStorage.delete(id);
        datasetRepository.deleteById(id);
    }

    // ── specification ──

    private Specification<DatasetRecord> buildSpec(String q, String tag, String format) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("dataSteward"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("sourceFileName"), "")), like)
                ));
            }
            if (format != null && !format.isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("fileFormat")), format.trim().toLowerCase()));
            }
            if (tag != null && !tag.isBlank()) {
                // match either the user-entered registration tags or the field-derived tags
                preds.add(cb.or(
                        cb.isMember(tag.trim(), root.<List<String>>get("datasetTags")),
                        cb.isMember(tag.trim(), root.<Set<String>>get("allTags"))
                ));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    // ── mapping helpers ──

    /** Sets the field-derived columns (JSON blob, tag set, counts) on a record. */
    private void applyFieldMetadata(DatasetRecord record, List<FieldMetadata> fields) {
        List<FieldMetadata> safe = fields == null ? List.of() : fields;
        record.setFieldsJson(writeFields(safe));
        record.setAllTags(collectTags(safe));
        record.setTotalFields(safe.size());
        record.setPiiFieldsCount((int) safe.stream().filter(FieldMetadata::isPiiData).count());
        record.setNpiFieldsCount((int) safe.stream().filter(FieldMetadata::isNpiData).count());
        record.setPciFieldsCount((int) safe.stream().filter(FieldMetadata::isPciData).count());
    }

    private CatalogueSummary toSummary(DatasetRecord r) {
        return new CatalogueSummary(
                r.getId(), r.getTitle(), r.getDescription(), r.getDataSteward(),
                r.getSourceFileName(), r.getFileFormat(), r.getTotalRecords(),
                r.getTotalFields(), r.getPiiFieldsCount(), r.getNpiFieldsCount(),
                r.getPciFieldsCount(),
                r.isPiiData(), r.isPciData(), r.getDataRetentionYears(),
                new ArrayList<>(r.getDatasetTags()), r.getCreatedAt());
    }

    private CatalogueDetail toDetail(DatasetRecord r, List<FieldMetadata> fields) {
        return new CatalogueDetail(
                r.getId(), r.getTitle(), r.getDescription(), r.getDataSteward(),
                r.isPiiData(), r.isPciData(), r.getDataRetentionYears(),
                new ArrayList<>(r.getDatasetTags()), r.getSourceFileName(),
                r.getFileFormat(), r.getTotalRecords(), r.getTotalFields(),
                r.getPiiFieldsCount(), r.getNpiFieldsCount(), r.getPciFieldsCount(),
                r.getCreatedAt(), fields);
    }

    /** Trims, drops blanks, and de-duplicates registration tags (preserving order). */
    private List<String> sanitizeTags(List<String> raw) {
        if (raw == null) return new ArrayList<>();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String t : raw) {
            if (t != null && !t.trim().isEmpty()) set.add(t.trim());
        }
        return new ArrayList<>(set);
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
