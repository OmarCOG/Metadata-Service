package com.bluebolt.fileparser.parser;

import com.bluebolt.fileparser.model.ParsedFile;
import com.bluebolt.fileparser.skill.DataNormalizationSkill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class CsvParserAgent implements FileParser {

    @Autowired
    private DataNormalizationSkill normalizationSkill;

    @Override
    public ParsedFile parse(InputStream inputStream, String fileName) {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            List<Map<String, Object>> records = new ArrayList<>();
            List<String> fieldNames = new ArrayList<>();

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV Empty");
            }

            String[] headers = headerLine.split(",");

            for (String h : headers) {
                fieldNames.add(h.trim());
            }

            String line;
            while ((line = reader.readLine()) != null) {

                String[] values = line.split(",");
                Map<String, Object> record = new HashMap<>();

                for (int i = 0; i < headers.length; i++) {
                    String value = i < values.length ? values[i] : null;
                    record.put(headers[i], value);
                }

                records.add(record);
            }

            ParsedFile parsedFile = new ParsedFile("csv", fieldNames, records);

            return normalizationSkill.normalize(parsedFile);

        } catch (Exception e) {
            throw new RuntimeException("CSV parsing failed");
        }
    }
}