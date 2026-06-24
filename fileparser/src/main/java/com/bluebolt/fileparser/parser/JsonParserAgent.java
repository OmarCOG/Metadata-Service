package com.bluebolt.fileparser.parser;

import com.bluebolt.fileparser.model.ParsedFile;
import com.bluebolt.fileparser.skill.DataNormalizationSkill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Component
public class JsonParserAgent implements FileParser {

    @Autowired
    private DataNormalizationSkill normalizationSkill;

    @Override
    public ParsedFile parse(InputStream inputStream, String fileName) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(inputStream);

            List<Map<String, Object>> records = new ArrayList<>();
            List<String> fieldNames = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    records.add(mapper.convertValue(node, Map.class));
                }
            } else if (root.isObject()) {

                if (root.has("fields")) {
                    for (JsonNode node : root.get("fields")) {
                        records.add(mapper.convertValue(node, Map.class));
                    }
                } else {
                    records.add(mapper.convertValue(root, Map.class));
                }
            }

            if (!records.isEmpty()) {
                fieldNames.addAll(records.get(0).keySet());
            }

            ParsedFile parsedFile = new ParsedFile("json", fieldNames, records);

            return normalizationSkill.normalize(parsedFile);

        } catch (Exception e) {
            throw new RuntimeException("JSON parsing failed");
        }
    }
}