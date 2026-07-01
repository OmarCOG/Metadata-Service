package com.mes.controllers;

import com.mes.models.ParsedFile;
import com.mes.orchestrator.FileParserOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mes.models.EnhancedMetadataResponse;
import com.mes.skills.MetadataEnhancementSkill;

import java.io.UncheckedIOException;
import java.util.Map;

/**
 * REST endpoint for uploading a file and receiving its normalized
 * {@link ParsedFile} metadata as JSON.
 */
@RestController
@RequestMapping("/api")
public class ParserController {

    private static final Logger log = LoggerFactory.getLogger(ParserController.class);

    private final FileParserOrchestrator orchestrator;
    private final MetadataEnhancementSkill metadataEnhancementSkill;

    public ParserController(FileParserOrchestrator orchestrator,
                            MetadataEnhancementSkill metadataEnhancementSkill) {
        this.orchestrator = orchestrator;
        this.metadataEnhancementSkill = metadataEnhancementSkill;
    }

    /**
     * Accepts a multipart file, detects its format, routes it to the matching
     * parser agent, and returns the normalized {@link ParsedFile} as JSON. This
     * is the format-agnostic contract consumed by the downstream Metadata
     * Extraction Engine.
     */
    @PostMapping(value = "/upload")
    public ResponseEntity<ParsedFile> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(orchestrator.parse(file));
    }

    /**
     * Accepts multipart files, parses them, and runs automated compliance and description
     * inference via the Anthropic Claude API, matching the front-end layout schema.
     */
    @PostMapping(value = "/upload/enhance")
    public ResponseEntity<EnhancedMetadataResponse> uploadAndEnhance(@RequestParam("file") MultipartFile file) {
        // 1. Invoke the existing orchestrator framework parsing
        ParsedFile parsedFile = orchestrator.parse(file);

        // 2. Process metrics and integrate AI context evaluations
        EnhancedMetadataResponse enhancedResponse = metadataEnhancementSkill.enhance(parsedFile, file.getOriginalFilename());

        return ResponseEntity.ok(enhancedResponse);
    }

    /**
     * Empty file or otherwise invalid argument -> 400 Bad Request. The exact cause
     * is logged server-side; the client receives a generic message so internal
     * details (paths, library internals) are never leaked back over the API.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Upload rejected (bad request): {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "The uploaded file is empty or invalid.");
    }

    /** Unrecognized / unsupported format -> 415 Unsupported Media Type. */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupported(UnsupportedOperationException ex) {
        log.warn("Upload rejected (unsupported format): {}", ex.getMessage());
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported file type. Supported formats: xlsx, xml, csv, json, parquet.");
    }

    /** A malformed / unreadable file surfaces as a parsing failure -> 422. */
    @ExceptionHandler({UncheckedIOException.class, IllegalStateException.class, RuntimeException.class})
    public ResponseEntity<Map<String, Object>> handleParsingFailure(RuntimeException ex) {
        // Full stack trace stays server-side for the maintenance team; the client
        // only learns the file was unreadable, never why (no internal disclosure).
        log.error("Failed to parse uploaded file", ex);
        return error(HttpStatus.UNPROCESSABLE_ENTITY,
                "The file could not be parsed. Please verify it is a valid, well-formed file.");
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
