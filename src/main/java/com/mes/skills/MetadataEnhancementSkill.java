package com.mes.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.models.EnhancedMetadataResponse;
import com.mes.models.FieldMetadata;
import com.mes.models.ParsedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * MetadataEnhancementSkill coordinates data cataloging and compliance discovery.
 * It calculates local profile statistics from parsed file contents to optimize token consumption,
 * dispatches schemas directly to the Anthropic Claude API for semantic enrichment, and aggregates data security risks.
 */
@Component
public class MetadataEnhancementSkill {

    private static final Logger log = LoggerFactory.getLogger(MetadataEnhancementSkill.class);

    // Endpoint URL configuration mapping for Anthropic Claude (e.g., [https://api.anthropic.com/v1/messages](https://api.anthropic.com/v1/messages))
    @Value("${llm.api.url}")
    private String apiUrl;

    // Secure authentication x-api-key token for the Anthropic platform
    @Value("${llm.api.key}")
    private String apiKey;

    // Specific model identifier (e.g., claude-3-5-sonnet-20240620)
    @Value("${llm.model}")
    private String modelName;

    // Core object handler utilized for serialization and deserialization tasks
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Entry pipeline orchestrating the complete metadata analysis and tracking loop.
     * * @param parsedFile The raw structural fields and string value records parsed from the upload agent.
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

        // Initialize variables to compute compliance risk roll-ups for top dashboard counters
        int pciCount = 0;
        int npiCount = 0;
        int phiCount = 0;

        // =========================================================================
        // PHASE 2 & 3: SEMANTIC ENRICHMENT & ERROR HANDLING BOUNDARY
        // =========================================================================
        try {
            log.info(">>>> Dispatching metadata schema manifest context to Claude API Endpoint: {}", apiUrl);
            enrichFieldsViaClaude(fieldList, originalFileName);
            log.info(">>>> Semantic AI metadata enrichment completed successfully.");
        } catch (Exception e) {
            log.error("Claude API Connection Failed. Initiating Smart Local Rules Engine Fallback Strategy.", e);

            // Fail-safe handler ensuring the system doesn't crash if external networks are unavailable.
            // Generates contextual placeholder descriptions and clear tags using local column signatures.
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

        // =========================================================================
        // PHASE 5: COMPLIANCE METRIC CARD ACCUMULATION
        // =========================================================================
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
     * Serializes structural data profiles, negotiates Anthropic-compliant transport parameters,
     * and maps Claude messages back to local Java data models.
     */
    private void enrichFieldsViaClaude(List<FieldMetadata> localFields, String fileName) throws Exception {

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

        // Convert context object into a clear JSON string block
        String userPromptPayload = objectMapper.writeValueAsString(datasetContext);

        // Assemble the parameters into Claude's top-level Messages API contract layout
        Map<String, Object> claudeRequestBody = new LinkedHashMap<>();
        claudeRequestBody.put("model", modelName);
        claudeRequestBody.put("max_tokens", 4000); // Required field parameter for Claude execution
        claudeRequestBody.put("system", systemInstructions); // Claude structures system rules as a root parameter
        claudeRequestBody.put("messages", List.of(
                Map.of("role", "user", "content", userPromptPayload)
        ));

        String requestBodyJson = objectMapper.writeValueAsString(claudeRequestBody);
        log.debug("Outgoing Request Payload to Claude: {}", requestBodyJson);

        // Configure strict headers matching Anthropic's authentication specifications
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01"); // Required version tracking string header

        // Dispatch direct HTTP call
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);
        String responseRawString = restTemplate.postForObject(apiUrl, entity, String.class);

        // =========================================================================
        // PHASE 4: TWO-TIERED JACKSON DESERIALIZATION WITH MARKDOWN SANITIZATION
        // =========================================================================
        JsonNode responseRootNode = objectMapper.readTree(responseRawString);

        // Unpack content node according to Claude's nested structural contract: content[0].text
        String innerJsonTextResponse = responseRootNode.path("content").get(0).path("text").asText();

        // Defensive Programming: Strip away unexpected markdown fences (e.g. ```json ... ```) if generated
        if (innerJsonTextResponse.contains("```json")) {
            innerJsonTextResponse = innerJsonTextResponse.substring(innerJsonTextResponse.indexOf("```json") + 7);
            if (innerJsonTextResponse.contains("```")) {
                innerJsonTextResponse = innerJsonTextResponse.substring(0, innerJsonTextResponse.indexOf("```"));
            }
        } else if (innerJsonTextResponse.contains("```")) {
            innerJsonTextResponse = innerJsonTextResponse.substring(innerJsonTextResponse.indexOf("```") + 3);
            if (innerJsonTextResponse.contains("```")) {
                innerJsonTextResponse = innerJsonTextResponse.substring(0, innerJsonTextResponse.indexOf("```"));
            }
        }
        innerJsonTextResponse = innerJsonTextResponse.trim();

        // Execute the second unwrapping pass to navigate the validated array tree structure
        JsonNode schemaPayload = objectMapper.readTree(innerJsonTextResponse);
        JsonNode fieldsArrayNode = schemaPayload.path("fields");

        // =========================================================================
        // PHASE 5: CASE-INSENSITIVE VALUE FIELD RECONCILIATION
        // =========================================================================
        for (FieldMetadata localMeta : localFields) {
            for (JsonNode node : fieldsArrayNode) {
                if (node.path("fieldName").asText().equalsIgnoreCase(localMeta.getFieldName())) {
                    localMeta.setDescription(node.path("description").asText());
                    localMeta.setPciData(node.path("pciData").asBoolean(false));
                    localMeta.setNpiData(node.path("npiData").asBoolean(false));
                    localMeta.setPhiData(node.path("phiData").asBoolean(false));

                    List<String> tagList = new ArrayList<>();
                    node.path("tags").forEach(t -> tagList.add(t.asText()));
                    localMeta.setTags(tagList);
                    break; // Match confirmed, break out of inner loop
                }
            }
        }
    }
}