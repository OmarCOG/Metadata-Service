package com.mes.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.mes.models.ParsedFile;
import com.mes.skills.DataNormalizationSkill;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Parses XML documents into a normalized {@link ParsedFile} using Jackson's
 * {@link XmlMapper}.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>Handles a single record as well as repeated elements (treated as an
 *       array of records).</li>
 *   <li>Flattens the top-level fields of each record into a
 *       {@code Map<String, Object>}; nested elements are stored as their text
 *       representation.</li>
 *   <li>Tracks the union of all field names across every record (in encounter
 *       order).</li>
 * </ul>
 *
 * <p>Recognized shapes (after XmlMapper reads the document):</p>
 * <ul>
 *   <li>{@code <root><item>..</item><item>..</item></root>} &rarr; many records.</li>
 *   <li>{@code <root><item>..</item></root>} &rarr; one record.</li>
 *   <li>{@code <record><a>1</a><b>2</b></record>} &rarr; one record with fields a, b.</li>
 * </ul>
 */
@Component
public class XmlParserAgent {

    private static final String FORMAT = "xml";

    private final DataNormalizationSkill normalizationSkill;
    private final XmlMapper xmlMapper = new XmlMapper();

    public XmlParserAgent(DataNormalizationSkill normalizationSkill) {
        this.normalizationSkill = normalizationSkill;
    }

    public ParsedFile parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        try {
            JsonNode root = xmlMapper.readTree(inputStream);
            if (root == null || root.isMissingNode() || root.isNull()) {
                throw new IllegalArgumentException("XML document is empty");
            }

            List<JsonNode> recordNodes = resolveRecordNodes(root);

            List<Map<String, Object>> records = new ArrayList<>();
            LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
            for (JsonNode node : recordNodes) {
                Map<String, Object> record = flatten(node);
                fieldNames.addAll(record.keySet());
                records.add(record);
            }

            ParsedFile parsedFile = new ParsedFile(FORMAT, new ArrayList<>(fieldNames), records);
            return normalizationSkill.normalize(parsedFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read xml document", e);
        }
    }

    /**
     * Determines which nodes represent records, supporting single records and
     * repeated (array) elements.
     */
    private List<JsonNode> resolveRecordNodes(JsonNode root) {
        List<JsonNode> nodes = new ArrayList<>();

        if (root.isArray()) {
            root.forEach(nodes::add);
            return nodes;
        }

        // root is an object. A document with a single wrapping element whose
        // child is repeated parses to { "child": [ {...}, {...} ] }; a single
        // occurrence parses to { "child": {...} }.
        if (root.isObject() && root.size() == 1) {
            JsonNode only = root.iterator().next();
            if (only.isArray()) {
                only.forEach(nodes::add);
                return nodes;
            }
            if (only.isObject()) {
                nodes.add(only);
                return nodes;
            }
            // Single scalar child (e.g. <root><value>1</value></root>): the root
            // itself is the single record carrying that one field.
            nodes.add(root);
            return nodes;
        }

        // Object with multiple top-level fields -> a single flat record.
        nodes.add(root);
        return nodes;
    }

    /**
     * Flattens the top-level fields of a record node into an ordered map.
     * Scalar values keep their natural type where possible; nested objects and
     * arrays are stored as their text representation.
     */
    private Map<String, Object> flatten(JsonNode node) {
        Map<String, Object> record = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return record;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            record.put(entry.getKey(), toValue(entry.getValue()));
        }
        return record;
    }

    /** Converts a JSON/XML value node to a plain Java value. */
    private Object toValue(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isNumber()) {
            return value.numberValue();
        }
        if (value.isValueNode()) {
            String text = value.asText();
            return (text == null || text.isEmpty()) ? null : text;
        }
        // Nested object or array: keep a flattened textual representation.
        return value.toString();
    }
}
