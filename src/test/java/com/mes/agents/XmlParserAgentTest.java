package com.mes.agents;

import com.mes.models.ParsedFile;
import com.mes.skills.DataNormalizationSkill;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlParserAgentTest {

    private final XmlParserAgent agent = new XmlParserAgent(new DataNormalizationSkill());

    private ParsedFile parse(String xml) {
        return agent.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesRepeatedElementsAsMultipleRecords() {
        String xml = """
                <users>
                    <user><name>Alice</name><age>30</age></user>
                    <user><name>Bob</name><age>25</age></user>
                    <user><name>Carol</name><age>42</age></user>
                </users>
                """;

        ParsedFile result = parse(xml);

        assertEquals("xml", result.getFileFormat());
        assertEquals(3, result.getRecords().size());
        assertTrue(result.getFieldNames().contains("name"));
        assertTrue(result.getFieldNames().contains("age"));
        assertEquals("Alice", result.getRecords().get(0).get("name"));
        assertEquals("Carol", result.getRecords().get(2).get("name"));
    }

    @Test
    void parsesSingleRepeatedElementAsOneRecord() {
        String xml = """
                <users>
                    <user><name>Solo</name><age>99</age></user>
                </users>
                """;

        ParsedFile result = parse(xml);

        assertEquals(1, result.getRecords().size());
        assertEquals("Solo", result.getRecords().get(0).get("name"));
    }

    @Test
    void parsesFlatSingleRecord() {
        String xml = "<record><name>Dave</name><age>50</age><active>true</active></record>";

        ParsedFile result = parse(xml);

        assertEquals(1, result.getRecords().size());
        Map<String, Object> rec = result.getRecords().get(0);
        assertEquals("Dave", rec.get("name"));
        assertEquals("50", rec.get("age"));
        assertEquals("true", rec.get("active"));
    }

    @Test
    void tracksUnionOfFieldNamesAndFillsMissingWithNull() {
        // Second record has an extra "email" field; first record lacks it.
        String xml = """
                <people>
                    <person><name>Alice</name><age>30</age></person>
                    <person><name>Bob</name><age>25</age><email>bob@example.com</email></person>
                </people>
                """;

        ParsedFile result = parse(xml);

        assertTrue(result.getFieldNames().contains("email"));
        // Every record is normalized to carry the full key set.
        assertTrue(result.getRecords().get(0).containsKey("email"));
        assertNull(result.getRecords().get(0).get("email"));
        assertEquals("bob@example.com", result.getRecords().get(1).get("email"));
    }

    @Test
    void handlesEmptyElementsAsNull() {
        String xml = """
                <items>
                    <item><label>full</label><note>hello</note></item>
                    <item><label>empty</label><note></note></item>
                </items>
                """;

        ParsedFile result = parse(xml);

        assertEquals(2, result.getRecords().size());
        assertEquals("hello", result.getRecords().get(0).get("note"));
        // An empty element <note></note> should normalize to null.
        assertNull(result.getRecords().get(1).get("note"));
    }
}
