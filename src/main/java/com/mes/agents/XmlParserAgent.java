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

        if (root.isObject()) {
            // The repeated child element is the record set. Pick the largest
            // array-valued child — this works even when the root also carries
            // attributes/metadata fields, e.g.
            // <InsuranceClaims total="120"><Claim>..</Claim><Claim>..</Claim></InsuranceClaims>
            // parses to { "total": "120", "Claim": [ {...}, {...} ] }.
            JsonNode bestArray = null;
            Iterator<Map.Entry<String, JsonNode>> it = root.fields();
            while (it.hasNext()) {
                JsonNode v = it.next().getValue();
                if (v.isArray() && (bestArray == null || v.size() > bestArray.size())) {
                    bestArray = v;
                }
            }
            if (bestArray != null) {
                bestArray.forEach(nodes::add);
                return nodes;
            }

            // No repeated element. A single wrapping object child -> one record.
            if (root.size() == 1) {
                JsonNode only = root.iterator().next();
                if (only.isObject()) {
                    nodes.add(only);
                    return nodes;
                }
            }
        }

        // Otherwise the object itself is a single flat record.
        nodes.add(root);
        return nodes;
    }

    /**
     * Flattens a record node into an ordered map, descending recursively through
     * nested element groups so leaf elements become columns (e.g. a
     * {@code <PatientInfo><ssn>..</ssn></PatientInfo>} group yields an {@code ssn}
     * column). Colliding leaf names are qualified with their parent element.
     */
    private Map<String, Object> flatten(JsonNode node) {
        Map<String, Object> record = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            flattenInto(null, node, record);
        }
        return record;
    }

    private void flattenInto(String parentKey, JsonNode node, Map<String, Object> out) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode val = entry.getValue();
            if (val != null && val.isObject()) {
                flattenInto(key, val, out);          // descend nested group
            } else {
                out.put(uniqueName(key, parentKey, out), toValue(val));
            }
        }
    }

    /** Keeps the clean leaf name; on collision qualifies with the parent, then a suffix. */
    private String uniqueName(String key, String parentKey, Map<String, Object> out) {
        if (!out.containsKey(key)) {
            return key;
        }
        String qualified = parentKey == null ? key : parentKey + "_" + key;
        if (!out.containsKey(qualified)) {
            return qualified;
        }
        int i = 2;
        while (out.containsKey(qualified + "_" + i)) {
            i++;
        }
        return qualified + "_" + i;
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
