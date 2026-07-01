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

    /** Reasoning effort (LOW|MEDIUM|HIGH). MEDIUM by default for nuanced multi-label classification. */
    private final String effortConfig;

    /** Output token budget per request. Raised for wide datasets so JSON isn't truncated. */
    private final long maxTokens;

    /** Columns per Claude request — keeps wide (50+ column) datasets within the token budget. */
    private static final int BATCH_SIZE = 40;

    /** Core object handler used to deserialize Claude's JSON response. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Generates format-preserving synthetic samples so real values never reach Claude. */
    private final SampleSanitizer sampleSanitizer;

    public MetadataEnhancementSkill(@Value("${llm.api.key:}") String apiKey,
                                    @Value("${llm.model}") String modelName,
                                    @Value("${llm.effort:MEDIUM}") String effortConfig,
                                    @Value("${llm.maxTokens:16000}") long maxTokens,
                                    SampleSanitizer sampleSanitizer) {
        this.modelName = modelName;
        this.effortConfig = effortConfig;
        this.maxTokens = maxTokens;
        this.sampleSanitizer = sampleSanitizer;
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
        int piiCount = 0;
        int npiCount = 0;
        int pciCount = 0;
        for (FieldMetadata meta : fieldList) {
            if (meta.isPiiData()) piiCount++;
            if (meta.isNpiData()) npiCount++;
            if (meta.isPciData()) pciCount++;
        }

        // Bind finalized metric sums onto the response root to render top UI dashboard metrics
        response.setPiiFieldsCount(piiCount);
        response.setNpiFieldsCount(npiCount);
        response.setPciFieldsCount(pciCount);
        response.setFields(fieldList);

        return response;
    }

    /**
     * Local datatype heuristic evaluating string records against patterns to determine types.
     */
    private String inferDataType(List<String> samples, String fieldName) {
        String lowerName = fieldName.toLowerCase();
        if (lowerName.contains("date") || lowerName.contains("dob")) return "date";
        if (samples.isEmpty()) return "string";

        boolean allBoolean = samples.stream()
                .allMatch(s -> s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"));
        if (allBoolean) return "boolean";

        boolean allNumeric = samples.stream().allMatch(s -> s.matches("-?\\d+(\\.\\d+)?"));
        if (allNumeric) {
            // Identifiers (account/routing/card/zip/SSN/IDs) look numeric but are
            // categorical — never treat them as quantities. A leading zero is a
            // strong identifier signal too (e.g. zip "01234", account "0099...").
            boolean anyLeadingZero = samples.stream().anyMatch(s -> s.matches("0\\d+"));
            if (anyLeadingZero || isIdentifierName(lowerName)) return "string";
            return "float";
        }

        if (lowerName.contains("amount") || lowerName.contains("balance")
                || lowerName.contains("price") || lowerName.contains("progress")
                || lowerName.contains("days")) {
            return "float";
        }
        return "string";
    }

    /** Column names that denote categorical identifiers rather than numeric quantities. */
    private boolean isIdentifierName(String lowerName) {
        return lowerName.contains("account") || lowerName.contains("acct")
                || lowerName.contains("routing") || lowerName.contains("iban")
                || lowerName.contains("swift") || lowerName.contains("ssn")
                || lowerName.contains("card") || lowerName.contains("pan")
                || lowerName.contains("zip") || lowerName.contains("postal")
                || lowerName.contains("number") || lowerName.contains("code")
                || lowerName.contains("phone") || lowerName.contains("identifier")
                || lowerName.equals("id") || lowerName.endsWith("_id");
    }

    /**
     * Serializes structural data profiles, calls the Anthropic Messages API via the official SDK,
     * and reconciles Claude's per-field enrichment back onto the local Java data models.
     */
    private void enrichFieldsViaClaude(List<FieldMetadata> localFields, String fileName) throws Exception {

        if (client == null) {
            // No API key: mark every field with the degraded 'error' state (keeps the
            // workflow alive) while the taxonomy still flags likely-sensitive columns.
            applyLocalFallback(localFields);
            return;
        }

        String systemInstructions = buildSystemPrompt();
        OutputConfig.Effort effort = resolveEffort(effortConfig);

        // Batch wide datasets (50+ columns) so the JSON output never exceeds the token budget.
        // A failure in one batch only degrades that batch — earlier successful batches are kept.
        for (int start = 0; start < localFields.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, localFields.size());
            List<FieldMetadata> batch = localFields.subList(start, end);
            try {
                enrichBatchViaClaude(batch, fileName, systemInstructions, effort);
            } catch (Exception e) {
                log.error("Claude enrichment failed for column batch [{}..{}); marking 'error' for manual editing.",
                        start, end, e);
                applyLocalFallback(batch);
            }
        }
    }

    /** Classifies one batch of columns via Claude and reconciles the result onto the local models. */
    private void enrichBatchViaClaude(List<FieldMetadata> batch, String fileName,
                                      String systemInstructions, OutputConfig.Effort effort) throws Exception {
        // Assemble the prompt context (field name, type, sample values) for this batch.
        Map<String, Object> datasetContext = new LinkedHashMap<>();
        datasetContext.put("targetFileName", fileName);
        List<Map<String, Object>> columnsSummary = new ArrayList<>();
        for (FieldMetadata f : batch) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("fieldName", f.getFieldName());
            col.put("dataType", f.getDataType());
            // Synthetic, format-preserving samples only — never the real banking values.
            col.put("sampleValues", sampleSanitizer.synthesize(f.getSampleValues()));
            columnsSummary.add(col);
        }
        datasetContext.put("columns", columnsSummary);
        String userPromptPayload = objectMapper.writeValueAsString(datasetContext);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(modelName)
                .maxTokens(maxTokens)
                .outputConfig(OutputConfig.builder().effort(effort).build())
                .system(systemInstructions)
                .addUserMessage(userPromptPayload)
                .build();

        Message message = client.messages().create(params);

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

        for (FieldMetadata localMeta : batch) {
            ClaudeField cf = byName.get(localMeta.getFieldName().toLowerCase().trim());
            if (cf != null) {
                localMeta.setDescription(cf.description());
                localMeta.setTags(cf.tags() != null ? cf.tags() : new ArrayList<>());
                localMeta.setPiiData(cf.piiData());
                localMeta.setNpiData(cf.npiData());
                localMeta.setPciData(cf.pciData());
            } else {
                // Claude omitted this field — never leave it silently blank/unflagged.
                markUnenriched(localMeta);
            }
        }
    }

    /** Builds the system prompt, injecting the category rubric from {@link SensitiveDataTaxonomy}. */
    private String buildSystemPrompt() {
        return """
            You are an advanced enterprise Data Catalog and Governance intelligence engine.
            Analyze the columns of an uploaded file collectively to determine its overall business domain.

            CRITICAL OUTPUT DIRECTIONS:
            Return ONLY a valid, raw, unescaped JSON object matching the requested structural schema contract.
            Do NOT wrap the JSON payload in markdown code blocks, do not include backticks, and do not append any introductory or concluding commentary text.

            DIRECTIONS:
            1. For each field, provide a high-quality human description detailing its business purpose. Do NOT use placeholder sentences.
            2. If a column is empty or named like 'column_X' with zero samples, classify it as: description: "Empty placeholder or unmapped spreadsheet column.", tags: ["empty"].
            3. Generate 2 to 4 precise lowercase business tags relevant to the deduced domain.

            COMPLIANCE CLASSIFICATION (multi-label — a field may be true for SEVERAL of these at once; they overlap, e.g. a card number is pci AND pii AND npi):
            """ + SensitiveDataTaxonomy.promptRubric() + """
            Response Structural JSON Contract:
            {
              "fields": [
                {
                  "fieldName": "exact_matching_column_name",
                  "description": "Functional business description.",
                  "tags": ["tag1", "tag2"],
                  "piiData": false,
                  "npiData": false,
                  "pciData": false
                }
              ]
            }
            """;
    }

    /** Maps the configured effort string to the SDK enum (defaults to MEDIUM). */
    private OutputConfig.Effort resolveEffort(String cfg) {
        String v = cfg == null ? "" : cfg.trim().toUpperCase();
        return switch (v) {
            case "LOW" -> OutputConfig.Effort.LOW;
            case "HIGH" -> OutputConfig.Effort.HIGH;
            default -> OutputConfig.Effort.MEDIUM;
        };
    }

    /**
     * Degraded path when Claude is unavailable for a set of fields (no API key or an
     * API/network error). Applies the honest {@link #markUnenriched} marker to each.
     */
    private void applyLocalFallback(List<FieldMetadata> fieldList) {
        for (FieldMetadata meta : fieldList) {
            markUnenriched(meta);
        }
    }

    /**
     * Honest degraded state for a field Claude didn't classify (failed call or an
     * omitted field): a clear {@code "error"} tag and "No response from Claude"
     * description so the user knows to review/edit it — while the local name-pattern
     * taxonomy still flags likely-sensitive columns so compliance counts aren't
     * silently zeroed for banking data. No business descriptions are fabricated.
     */
    private void markUnenriched(FieldMetadata meta) {
        meta.setDescription("No response from Claude");
        meta.setTags(new ArrayList<>(List.of("error")));

        Map<String, Boolean> hits = SensitiveDataTaxonomy.detect(meta.getFieldName());
        meta.setPiiData(Boolean.TRUE.equals(hits.get("pii")));
        meta.setNpiData(Boolean.TRUE.equals(hits.get("npi")));
        meta.setPciData(Boolean.TRUE.equals(hits.get("pci")));
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
                       boolean piiData, boolean npiData, boolean pciData) {
    }

    /** Typed view of Claude's response contract. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClaudeEnrichment(List<ClaudeField> fields) {
    }
}
