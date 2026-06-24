package com.mes.agents;

import com.mes.models.ParsedFile;
import com.mes.skills.DataNormalizationSkill;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies {@link ParquetParserAgent} by writing a small Parquet file with
 * DuckDB ({@code COPY ... TO ... (FORMAT PARQUET)}) and parsing it back.
 */
class ParquetParserAgentTest {

    private final ParquetParserAgent agent = new ParquetParserAgent(new DataNormalizationSkill());

    @Test
    void parsesColumnsRowsAndNulls() throws Exception {
        Path tmp = Files.createTempFile("mes-test-", ".parquet");
        Files.deleteIfExists(tmp); // DuckDB's COPY writes the file itself
        String path = tmp.toString().replace('\\', '/');

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "COPY (SELECT * FROM (VALUES "
                            + "(1, 'Alice', true), "
                            + "(2, 'Bob', false), "
                            + "(3, NULL, true)) AS t(id, name, active)) "
                            + "TO '" + path + "' (FORMAT PARQUET)");
        }

        try (InputStream in = Files.newInputStream(tmp)) {
            ParsedFile parsed = agent.parse(in);

            assertEquals("parquet", parsed.getFileFormat());
            assertEquals(List.of("id", "name", "active"), parsed.getFieldNames());
            assertEquals(3, parsed.getRecords().size());

            Map<String, Object> first = parsed.getRecords().get(0);
            assertEquals("Alice", first.get("name"));
            assertEquals(true, first.get("active"));

            // The NULL name on row 3 must be preserved as null.
            assertNull(parsed.getRecords().get(2).get("name"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
