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

    public ParserController(FileParserOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Accepts a multipart file upload and returns the parsed, normalized result.
     *
     * @param file the uploaded file (form field {@code file})
     * @return {@code 200} with the {@link ParsedFile}, or an error status with a
     *         JSON error body
     */
    @PostMapping(value = "/upload")
    public ResponseEntity<ParsedFile> upload(@RequestParam("file") MultipartFile file) {
        ParsedFile parsed = orchestrator.parse(file);
        return ResponseEntity.ok(parsed);
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
