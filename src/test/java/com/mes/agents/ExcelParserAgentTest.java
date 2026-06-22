package com.mes.agents;

import com.mes.models.ParsedFile;
import com.mes.skills.DataNormalizationSkill;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelParserAgentTest {

    private final ExcelParserAgent agent = new ExcelParserAgent(new DataNormalizationSkill());

    /**
     * Builds an in-memory xlsx workbook with a header row and three data rows
     * exercising string/numeric/boolean/date/null cells and one fully empty row.
     */
    private byte[] buildWorkbook() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("data");

            CreationHelper helper = wb.getCreationHelper();
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));

            // Header row.
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("name");
            header.createCell(1).setCellValue("age");
            header.createCell(2).setCellValue("active");
            header.createCell(3).setCellValue("joined");

            // Row 1: full record with a date.
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("Alice");
            r1.createCell(1).setCellValue(30);
            r1.createCell(2).setCellValue(true);
            Cell dateCell = r1.createCell(3);
            Date joined = Date.from(LocalDate.of(2021, 5, 17).atStartOfDay(ZoneId.systemDefault()).toInstant());
            dateCell.setCellValue(joined);
            dateCell.setCellStyle(dateStyle);

            // Row 2: a missing/null cell (age left out entirely).
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("Bob");
            // column 1 (age) intentionally not created -> null
            r2.createCell(2).setCellValue(false);

            // Row 3: fully empty row -> must be skipped.
            sheet.createRow(3);

            // Row 4: another full record.
            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("Carol");
            r4.createCell(1).setCellValue(42);
            r4.createCell(2).setCellValue(true);

            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void parsesHeaderAsFieldNames() throws Exception {
        ParsedFile result = agent.parse(new ByteArrayInputStream(buildWorkbook()));

        assertEquals("xlsx", result.getFileFormat());
        assertEquals(List.of("name", "age", "active", "joined"), result.getFieldNames());
    }

    @Test
    void skipsFullyEmptyRowsAndKeepsRealRecords() throws Exception {
        ParsedFile result = agent.parse(new ByteArrayInputStream(buildWorkbook()));

        // 3 real records (rows 1, 2, 4); row 3 is empty and dropped.
        assertEquals(3, result.getRecords().size());
    }

    @Test
    void mapsCellTypesToJavaTypes() throws Exception {
        ParsedFile result = agent.parse(new ByteArrayInputStream(buildWorkbook()));
        Map<String, Object> alice = result.getRecords().get(0);

        assertEquals("Alice", alice.get("name"));
        assertEquals(30.0, alice.get("age"));        // numeric -> double
        assertEquals(true, alice.get("active"));     // boolean
        assertTrue(alice.get("joined").toString().startsWith("2021-05-17")); // date -> ISO string
    }

    @Test
    void handlesNullCellsAndKeepsConsistentKeys() throws Exception {
        ParsedFile result = agent.parse(new ByteArrayInputStream(buildWorkbook()));
        Map<String, Object> bob = result.getRecords().get(1);

        // Every record carries all four keys even though Bob's age/joined are null.
        assertTrue(bob.containsKey("age"));
        assertNull(bob.get("age"));
        assertTrue(bob.containsKey("joined"));
        assertNull(bob.get("joined"));
        assertEquals(false, bob.get("active"));
        assertFalse(result.getFieldNames().isEmpty());
    }
}
