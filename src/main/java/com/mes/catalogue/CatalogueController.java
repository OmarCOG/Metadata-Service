package com.mes.catalogue;

import com.mes.catalogue.dto.CatalogueDetail;
import com.mes.catalogue.dto.CatalogueSubmitRequest;
import com.mes.catalogue.dto.CatalogueSubmitResponse;
import com.mes.catalogue.dto.CatalogueSummary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the dataset catalogue: register a dataset, list the
 * catalogue, view one entry's full metadata, and download its original file.
 */
@RestController
@RequestMapping("/api/catalogue")
public class CatalogueController {

    private final CatalogueService catalogueService;

    public CatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    /**
     * Registers a dataset. Multipart request with two parts:
     * {@code file} (the original upload) and {@code payload} (JSON
     * {@link CatalogueSubmitRequest}: title, description, owner, metadata).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CatalogueSubmitResponse> register(
            @RequestPart("file") MultipartFile file,
            @RequestPart("payload") CatalogueSubmitRequest payload) {

        CatalogueDetail saved = catalogueService.register(payload, file);
        CatalogueSubmitResponse body = new CatalogueSubmitResponse(
                saved.id(), exchangeId(saved.id()), saved.createdAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Lists all registered datasets, newest first. */
    @GetMapping
    public ResponseEntity<List<CatalogueSummary>> list() {
        return ResponseEntity.ok(catalogueService.listSummaries());
    }

    /** Full metadata for one registered dataset. */
    @GetMapping("/{id}")
    public ResponseEntity<CatalogueDetail> detail(@PathVariable Long id) {
        return ResponseEntity.ok(catalogueService.getDetail(id));
    }

    /** Downloads the original uploaded file for a dataset. */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        DatasetFileStorage.StoredFile f = catalogueService.getFile(id);
        String filename = f.originalFileName() != null ? f.originalFileName() : ("dataset-" + id);
        MediaType type = f.contentType() != null
                ? MediaType.parseMediaType(f.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(f.content());
    }

    /** Stable display id derived from the row id, e.g. EXC-000042. */
    private static String exchangeId(Long id) {
        return String.format("EXC-%06d", id);
    }

    // ── Error envelope (mirrors ParserController's shape) ──

    @ExceptionHandler(DatasetNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(DatasetNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message
        );
        return ResponseEntity.status(status).body(body);
    }
}
