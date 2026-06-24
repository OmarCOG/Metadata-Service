package com.mes.skills;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.models.EnhancedMetadataResponse;
import com.mes.models.FieldMetadata;
import com.mes.models.ParsedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MetadataEnhancementSkill coordinates data cataloging and compliance discovery.
 * It calculates local profile statistics from parsed file contents to optimize token consumption,
 * dispatches schemas to the Anthropic Claude API (via the official Java SDK) for semantic enrichment,
 * and aggregates data security risks. If the Claude call fails for any reason, it degrades gracefully
 * to a local rules-based engine so the endpoint always returns a result.
 */
@Component
public class MetadataEnhancementSkill {

    private static final Logger log = LoggerFactory.getLogger(MetadataEnhancementSkill.class);

    /** Pre-built Anthropic client (endpoint + auth handled by the SDK). Null when no API key is configured. */
    private final AnthropicClient client;

    /** Model identifier used for enrichment (e.g., claude-sonnet-4-6). */
    private final String modelName;

    /** Core object handler used to deserialize Claude's JSON response. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetadataEnhancementSkill(@Value("${llm.api.key:}") String apiKey,
                                    @Value("${llm.model}") String modelName) {
        this.modelName = modelName;
        // Defer to the local fallback (rather than failing app startup) when no key is present.
        this.client = (apiKey == null || apiKey.isBlank())
                ? null
                : AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        if (this.client == null) {
            log.warn("ANTHROPIC_API_KEY is not set — Claude enrichment is disabled; "
                    + "the local rules-based fallback will be used for /api/upload/enhance.");
        }
    }

    /**
     * Entry pipeline orchestrating the complete metadata analysis and tracking loop.
     *
     * @param parsedFile       The raw structural fields and string value records parsed from the upload agent.
     * @param originalFileName The source tracking name of the uploaded document asset.
     * @return An EnhancedMetadataResponse object populated with profiling metrics and compliance data.
     */
    public EnhancedMetadataResponse enhance(ParsedFile parsedFile, String originalFileName) {

        // Initialize the top-level response wrapper and map file-level structural metadata
        EnhancedMetadataResponse response = new EnhancedMetadataResponse();
        response.setFileName(originalFileName);
        response.setFileFormat(parsedFile.getFileFormat());
        response.setTotalRecords(parsedFile.getRecords().size());
        response.setTotalFields(parsedFile.getFieldNames().size());

        List<FieldMetadata> fieldList = new ArrayList<>();

        // =========================================================================
        // PHASE 1: LOCAL PROFILING & MATHEMATICAL DATA PASS
        // =========================================================================
        for (String fieldName : parsedFile.getFieldNames()) {
            FieldMetadata meta = new FieldMetadata();
            meta.setFieldName(fieldName);

            int nullCount = 0;
            Set<String> uniqueValues = new LinkedHashSet<>();

            // Inspect every record row tracking cell content under the active column
            for (Map<String, Object> record : parsedFile.getRecords()) {
                Object value = record.get(fieldName);

                // Track missing/blank observations to evaluate field nullability
                if (value == null || value.toString().trim().isEmpty()) {
                    nullCount++;
                } else {
                    // Accumulate string formats into an ordered set to extract unique items
                    uniqueValues.add(value.toString().trim());
                }
            }

            meta.setNullCount(nullCount);
            meta.setNullable(nullCount > 0);
            meta.setUniqueCount(uniqueValues.size());

            // Extract up to 4 safe distinct data samples to feed structural context to the AI model
            List<String> samples = new ArrayList<>(uniqueValues).subList(0, Math.min(4, uniqueValues.size()));
            meta.setSampleValues(samples);

            // Run regex string-matching rules to infer the target data type
            meta.setDataType(inferDataType(samples, fieldName));

            fieldList.add(meta);
        }

        // =========================================================================
        // PHASE 2 & 3: SEMANTIC ENRICHMENT & ERROR HANDLING BOUNDARY
        // =========================================================================
        try {
            log.info(">>>> Dispatching metadata schema manifest context to Claude (model={})", modelName);
            enrichFieldsViaClaude(fieldList, originalFileName);
            log.info(">>>> Semantic AI metadata enrichment completed successfully.");
        } catch (Exception e) {
            log.error("Claude enrichment failed. Initiating Smart Local Rules Engine Fallback Strategy.", e);
            applyLocalFallback(fieldList);
        }

        // =========================================================================
        // PHASE 4: COMPLIANCE METRIC CARD ACCUMULATION
        // =========================================================================
        int pciCount = 0;
        int npiCount = 0;
        int phiCount = 0;
        for (FieldMetadata meta : fieldList) {
            if (meta.isPciData()) pciCount++;
            if (meta.isNpiData()) npiCount++;
            if (meta.isPhiData()) phiCount++;
        }

        // Bind finalized metric sums onto the response root to render top UI dashboard metrics
        response.setPciFieldsCount(pciCount);
        response.setNpiFieldsCount(npiCount);
        response.setPhiFieldsCount(phiCount);
        response.setFields(fieldList);

        return response;
    }

    /**
     * Local datatype heuristic evaluating string records against patterns to determine types.
     */
    private String inferDataType(List<String> samples, String fieldName) {
        String lowerName = fieldName.toLowerCase();
        if (lowerName.contains("date")) return "date";
        if (lowerName.contains("progress") || lowerName.contains("amount") || lowerName.contains("days")) return "float";
        if (samples.isEmpty()) return "string";

        String sample = samples.get(0);
        if (sample.equalsIgnoreCase("true") || sample.equalsIgnoreCase("false")) return "boolean";
        if (sample.matches("-?\\d+(\\.\\d+)?")) return "float";
        return "string";
    }

    /**
     * Serializes structural data profiles, calls the Anthropic Messages API via the official SDK,
     * and reconciles Claude's per-field enrichment back onto the local Java data models.
     */
    private void enrichFieldsViaClaude(List<FieldMetadata> localFields, String fileName) throws Exception {

        if (client == null) {
            throw new IllegalStateException("Anthropic API key not configured (ANTHROPIC_API_KEY).");
        }

        // Define explicit system behavior instructions tailored for optimal Claude parsing outputs
        String systemInstructions = """
            You are an advanced enterprise Data Catalog and Governance intelligence engine.
            Analyze the columns of an uploaded file collectively to determine its overall business domain.

            CRITICAL OUTPUT DIRECTIONS:
            Return ONLY a valid, raw, unescaped JSON object matching the requested structural schema contract.
            Do NOT wrap the JSON payload in markdown code blocks, do not include backticks, and do not append any introductory or concluding commentary text.

            DIRECTIONS:
            1. For each field, provide a high-quality human description detailing its business purpose. Do NOT use placeholder sentences.
            2. If a column is empty or named like 'column_X' with zero samples, classify it as: description: "Empty placeholder or unmapped spreadsheet column.", tags: ["empty"].
            3. Generate 2 to 4 precise lowercase business tags relevant to the deduced domain.

            COMPLIANCE METRICS:
            - pciData: true if it contains credit card fields or card payment data.
            - npiData: true if it contains personal info (PII) like employee/customer names, phone numbers, or emails.
            - phiData: true if it contains healthcare or diagnostic metrics.

            Response Structural JSON Contract:
            {
              "fields": [
                {
                  "fieldName": "exact_matching_column_name",
                  "description": "Functional business description.",
                  "tags": ["tag1", "tag2"],
                  "pciData": false,
                  "npiData": false,
                  "phiData": false
                }
              ]
            }
            """;

        // Assemble a unified prompt context structure showing the AI the entire schema layout at once
        Map<String, Object> datasetContext = new LinkedHashMap<>();
        datasetContext.put("targetFileName", fileName);

        List<Map<String, Object>> columnsSummary = new ArrayList<>();
        for (FieldMetadata f : localFields) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("fieldName", f.getFieldName());
            col.put("dataType", f.getDataType());
            col.put("sampleValues", f.getSampleValues());
            columnsSummary.add(col);
        }
        datasetContext.put("columns", columnsSummary);

        String userPromptPayload = objectMapper.writeValueAsString(datasetContext);

        // Call the Messages API. Effort LOW keeps this fast and cheap for a structured extraction task.
        MessageCreateParams params = MessageCreateParams.builder()
                .model(modelName)
                .maxTokens(8000L)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .system(systemInstructions)
                .addUserMessage(userPromptPayload)
                .build();

        Message message = client.messages().create(params);

        // Concatenate all returned text blocks into the raw JSON response.
        StringBuilder rawText = new StringBuilder();
        message.content().forEach(block -> block.text().ifPresent(t -> rawText.append(t.text())));

        // Defensive parsing: the system prompt forbids markdown, but strip code fences just in case.
        ClaudeEnrichment enrichment = objectMapper.readValue(stripCodeFences(rawText.toString()), ClaudeEnrichment.class);

        // Index Claude's results by lower-cased field name for O(1) reconciliation.
        Map<String, ClaudeField> byName = new HashMap<>();
        if (enrichment != null && enrichment.fields() != null) {
            for (ClaudeField cf : enrichment.fields()) {
                if (cf.fieldName() != null) {
                    byName.put(cf.fieldName().toLowerCase().trim(), cf);
                }
            }
        }

        for (FieldMetadata localMeta : localFields) {
            ClaudeField cf = byName.get(localMeta.getFieldName().toLowerCase().trim());
            if (cf != null) {
                localMeta.setDescription(cf.description());
                localMeta.setTags(cf.tags() != null ? cf.tags() : new ArrayList<>());
                localMeta.setPciData(cf.pciData());
                localMeta.setNpiData(cf.npiData());
                localMeta.setPhiData(cf.phiData());
            }
        }
    }

    /**
     * Fail-safe handler ensuring the system doesn't crash if Claude is unavailable.
     * Generates contextual placeholder descriptions and clear tags using local column signatures.
     */
    private void applyLocalFallback(List<FieldMetadata> fieldList) {
        for (FieldMetadata meta : fieldList) {
            String name = meta.getFieldName().toLowerCase().trim();
            if (name.startsWith("column_")) {
                meta.setDescription("Empty placeholder or unmapped spreadsheet column.");
                meta.setTags(List.of("empty"));
            } else if (name.contains("project")) {
                meta.setDescription("The designated enterprise initiative, client workstream, or program tracking tasks.");
                meta.setTags(List.of("project-tracking", "operations"));
            } else if (name.contains("task")) {
                meta.setDescription("The specific operational action item required to complete the underlying milestone.");
                meta.setTags(List.of("task-management", "workflow"));
            } else if (name.contains("assigned")) {
                meta.setDescription("The corporate identity or resource stakeholder accountable for executing this task.");
                meta.setTags(List.of("resource-allocation", "identity"));
                meta.setNpiData(true); // Flag safety risk locally for dashboard data integrity
            } else {
                meta.setDescription("Extracted dataset property capturing record inputs for: " + meta.getFieldName());
                meta.setTags(List.of("dataset-field", "system-extracted"));
            }
        }
    }

    /** Strip an enclosing ```json ... ``` (or bare ``` ... ```) fence if one is present. */
    private String stripCodeFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) {
                t = t.substring(firstNewline + 1); // drop the opening ```/```json line
            }
            int closing = t.lastIndexOf("```");
            if (closing >= 0) {
                t = t.substring(0, closing);
            }
        }
        return t.trim();
    }

    /** Typed view of a single field returned by Claude. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClaudeField(String fieldName, String description, List<String> tags,
                       boolean pciData, boolean npiData, boolean phiData) {
    }

    /** Typed view of Claude's response contract. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClaudeEnrichment(List<ClaudeField> fields) {
    }
}
