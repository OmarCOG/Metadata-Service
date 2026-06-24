package com.mes.agents;

import com.mes.models.ParsedFile;
import com.mes.skills.DataNormalizationSkill;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses {@code .parquet} files into a normalized {@link ParsedFile}.
 *
 * <p>Parquet is a binary columnar format, so (unlike the text-based agents) it
 * needs a reader library. This agent uses the embedded DuckDB engine via JDBC
 * and its {@code read_parquet} table function, which reads an arbitrary Parquet
 * file with no predefined schema and without the Apache Hadoop / {@code winutils}
 * dependency chain.</p>
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>The Parquet column names become the field names (in file order).</li>
 *   <li>Each row becomes one record; SQL {@code NULL}s are preserved as
 *       {@code null}.</li>
 *   <li>Values keep their natural JDBC Java types (numbers, booleans, strings,
 *       timestamps, ...).</li>
 * </ul>
 */
@Component
public class ParquetParserAgent {

    private static final String FORMAT = "parquet";

    static {
        // Ensure the DuckDB JDBC driver is registered (ServiceLoader normally
        // handles this, but force-load defensively for fat-jar/classloader setups).
        try {
            Class.forName("org.duckdb.DuckDBDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final DataNormalizationSkill normalizationSkill;

    public ParquetParserAgent(DataNormalizationSkill normalizationSkill) {
        this.normalizationSkill = normalizationSkill;
    }

    public ParsedFile parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        // DuckDB reads from a path, so spool the upload to a temp file first.
        Path temp = null;
        try {
            temp = Files.createTempFile("mes-upload-", ".parquet");
            Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);

            List<String> fieldNames = new ArrayList<>();
            List<Map<String, Object>> records = new ArrayList<>();

            // Forward slashes + escaped single quotes keep the path literal safe;
            // the path is server-generated, so it cannot be attacker-controlled.
            String safePath = temp.toString().replace('\\', '/').replace("'", "''");
            String sql = "SELECT * FROM read_parquet('" + safePath + "')";

            try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    fieldNames.add(meta.getColumnName(i));
                }

                while (rs.next()) {
                    Map<String, Object> record = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        record.put(fieldNames.get(i - 1), rs.wasNull() ? null : value);
                    }
                    records.add(record);
                }
            }

            ParsedFile parsedFile = new ParsedFile(FORMAT, fieldNames, records);
            return normalizationSkill.normalize(parsedFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read parquet upload", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to parse parquet document: " + e.getMessage(), e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // Temp file cleanup is best-effort.
                }
            }
        }
    }
}
