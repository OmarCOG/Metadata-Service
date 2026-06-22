package com.mes.agents;

import com.mes.models.ParsedFile;
import com.mes.skills.DataNormalizationSkill;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses {@code .xlsx} spreadsheets into a normalized {@link ParsedFile} using
 * Apache POI.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>Only the first sheet is read.</li>
 *   <li>Row 0 is treated as the header row, producing the field names.</li>
 *   <li>Each remaining row becomes one record.</li>
 *   <li>Null / empty cells are preserved as {@code null} values; fully empty
 *       rows are skipped.</li>
 *   <li>Date-formatted numeric cells are emitted as ISO-8601 strings.</li>
 *   <li>String, numeric and boolean cell types are mapped to their natural Java
 *       types ({@link String}, {@link Double}, {@link Boolean}).</li>
 * </ul>
 */
@Component
public class ExcelParserAgent {

    private static final String FORMAT = "xlsx";

    private final DataNormalizationSkill normalizationSkill;

    public ExcelParserAgent(DataNormalizationSkill normalizationSkill) {
        this.normalizationSkill = normalizationSkill;
    }

    /**
     * Parses the supplied xlsx input stream.
     *
     * @param inputStream stream of an xlsx workbook (caller retains ownership;
     *                    this method closes the workbook but not the stream
     *                    beyond what POI requires)
     * @return a normalized {@link ParsedFile}
     */
    public ParsedFile parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Workbook contains no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            List<String> fieldNames = readHeader(sheet);
            List<Map<String, Object>> records = readRecords(sheet, fieldNames);

            ParsedFile parsedFile = new ParsedFile(FORMAT, fieldNames, records);
            return normalizationSkill.normalize(parsedFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read xlsx workbook", e);
        }
    }

    /** Reads row 0 as the list of field names, generating names for blank headers. */
    private List<String> readHeader(Sheet sheet) {
        List<String> fieldNames = new ArrayList<>();
        Row header = sheet.getRow(sheet.getFirstRowNum());
        if (header == null) {
            return fieldNames; // empty header -> normalization will reject if no records either
        }

        int lastCol = header.getLastCellNum(); // 1-based count of the last cell + 1
        for (int c = 0; c < lastCol; c++) {
            Cell cell = header.getCell(c);
            Object value = getCellValue(cell);
            String name = value != null ? value.toString().trim() : "";
            if (name.isEmpty()) {
                name = "column_" + c;
            }
            fieldNames.add(name);
        }
        return fieldNames;
    }

    /** Reads every row after the header into a record keyed by field name. */
    private List<Map<String, Object>> readRecords(Sheet sheet, List<String> fieldNames) {
        List<Map<String, Object>> records = new ArrayList<>();
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();

        for (int r = firstRow + 1; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue; // entirely missing row
            }

            Map<String, Object> record = new LinkedHashMap<>();
            boolean hasValue = false;
            for (int c = 0; c < fieldNames.size(); c++) {
                Cell cell = row.getCell(c);
                Object value = getCellValue(cell);
                if (value != null) {
                    hasValue = true;
                }
                record.put(fieldNames.get(c), value);
            }

            if (hasValue) {
                records.add(record);
            }
        }
        return records;
    }

    /**
     * Converts a POI {@link Cell} to a plain Java value, handling null/blank
     * cells, dates, and the string/numeric/boolean cell types.
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        switch (type) {
            case STRING:
                String s = cell.getStringCellValue();
                return (s == null || s.isEmpty()) ? null : s;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return cell.getNumericCellValue();
            case BLANK:
            case _NONE:
            case ERROR:
            default:
                return null;
        }
    }
}
