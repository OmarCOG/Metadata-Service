package com.mes.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.models.ParsedFile;
import com.mes.skills.DataNormalizationSkill;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses JSON documents into a normalized {@link ParsedFile} using Jackson.
 *
 * <p>Recognized shapes:</p>
 * <ul>
 *   <li>{@code [ {...}, {...} ]} &rarr; an array of objects, one record each.</li>
 *   <li>{@code { "fields": [ {...}, {...} ] }} &rarr; the records live under a
 *       top-level {@code fields} array.</li>
 *   <li>{@code { ... }} &rarr; a single object becomes one record.</li>
 * </ul>
 *
 * <p>Field names are the union of the keys across every record (in encounter
 * order); {@link DataNormalizationSkill} fills any gaps so all records share the
 * same ordered key set.</p>
 */
@Component
public class JsonParserAgent {

    private static final String FORMAT = "json";

    private static final TypeReference<LinkedHashMap<String, Object>> RECORD_TYPE =
            new TypeReference<>() {
            };

    private final DataNormalizationSkill normalizationSkill;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonParserAgent(DataNormalizationSkill normalizationSkill) {
        this.normalizationSkill = normalizationSkill;
    }

    public ParsedFile parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        try {
            JsonNode root = objectMapper.readTree(inputStream);
            if (root == null || root.isMissingNode() || root.isNull()) {
                throw new IllegalArgumentException("JSON document is empty");
            }

            List<JsonNode> recordNodes = resolveRecordNodes(root);

            List<Map<String, Object>> records = new ArrayList<>();
            for (JsonNode node : recordNodes) {
                if (node != null && node.isObject()) {
                    records.add(objectMapper.convertValue(node, RECORD_TYPE));
                }
            }

            ParsedFile parsedFile = new ParsedFile(FORMAT, new ArrayList<>(), records);
            return normalizationSkill.normalize(parsedFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read json document", e);
        }
    }

    /** Resolves the list of nodes that each represent a single record. */
    private List<JsonNode> resolveRecordNodes(JsonNode root) {
        List<JsonNode> nodes = new ArrayList<>();

        if (root.isArray()) {
            root.forEach(nodes::add);
            return nodes;
        }

        if (root.isObject()) {
            JsonNode fields = root.get("fields");
            if (fields != null && fields.isArray()) {
                fields.forEach(nodes::add);
                return nodes;
            }
            // A single object is one record.
            nodes.add(root);
        }
        return nodes;
    }
}
