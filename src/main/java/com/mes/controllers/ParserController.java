package com.mes.controllers;

import com.mes.models.ParsedFile;
import com.mes.orchestrator.FileParserOrchestrator;
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

    private final FileParserOrchestrator orchestrator;
    private final MetadataEnhancementSkill metadataEnhancementSkill;

    public ParserController(FileParserOrchestrator orchestrator,
                            MetadataEnhancementSkill metadataEnhancementSkill) {
        this.orchestrator = orchestrator;
        this.metadataEnhancementSkill = metadataEnhancementSkill;
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

    /** Empty file or otherwise invalid argument -> 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Unrecognized / unsupported format -> 415 Unsupported Media Type. */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupported(UnsupportedOperationException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    /** A malformed / unreadable file surfaces as a parsing failure -> 422. */
    @ExceptionHandler({UncheckedIOException.class, IllegalStateException.class, RuntimeException.class})
    public ResponseEntity<Map<String, Object>> handleParsingFailure(RuntimeException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY,
                "Failed to parse the uploaded file: " + ex.getMessage());
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
