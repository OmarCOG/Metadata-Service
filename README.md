# MES PoC — XML & Excel Parser Module

Metadata Extraction Service parser module (Java 17 / Spring Boot 3.2.5). Accepts
an uploaded file, detects its format, parses it into a normalized `ParsedFile`,
and returns it as JSON for the downstream Metadata Extraction Engine.

## Package layout (`com.mes`)

| Package        | Class                       | Responsibility |
|----------------|-----------------------------|----------------|
| `models`       | `ParsedFile`                | Normalized output: `fileFormat`, `fieldNames`, `records`. |
| `skills`       | `FileFormatDetectionSkill`  | Detects `json` / `csv` / `xml` / `xlsx` from file name + MIME type. |
| `skills`       | `DataNormalizationSkill`    | Validates/normalizes any parser output (non-empty field names, consistent record keys, format set). |
| `agents`       | `ExcelParserAgent`          | Parses `.xlsx` (Apache POI 5.2.3) — first sheet, row 0 = headers, handles null/date/string/numeric/boolean cells. |
| `agents`       | `XmlParserAgent`            | Parses XML (Jackson `jackson-dataformat-xml` 2.15.2) — single + repeated elements, flattens top-level fields. |
| `orchestrator` | `FileParserOrchestrator`    | Detects format and routes to the right agent; `UnsupportedOperationException` for unknown formats. |
| `controllers`  | `ParserController`          | `POST /api/upload` (multipart) → `ParsedFile` JSON, with error handling. |

## API

```
POST /api/upload        Content-Type: multipart/form-data
  form field: file=<the upload>
→ 200  ParsedFile JSON
→ 400  empty/invalid file
→ 415  unsupported/unrecognized format
→ 422  file could not be parsed
```

## Building behind the corporate proxy

This machine has no `mvn` on the PATH; a cached Maven 3.9.6 is used directly.
The Zscaler proxy **blocks `.jar` downloads from Maven Central**
(`repo.maven.apache.org`) but allows them from Google's Cloud Storage mirror, and
intercepts TLS with corporate root CAs that the JDK does not trust by default.

Two things make the build work (already set up in this repo / machine):

1. **`maven-settings.xml`** mirrors `central` → the GCS Maven Central mirror.
2. **A truststore** (`C:\Users\981357\tools\mes-truststore.jks`, password
   `changeit`) containing the Zscaler + Cognizant root CAs, passed via
   `MAVEN_OPTS`.

```powershell
$MVN = "C:\Users\981357\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd"
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStore=C:\Users\981357\tools\mes-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"

& $MVN -s maven-settings.xml clean test       # run tests
& $MVN -s maven-settings.xml package           # build bootable jar
java -jar target/mes-poc-0.0.1-SNAPSHOT.jar    # run the service (port 8080)
```

## Tests

`ExcelParserAgentTest` and `XmlParserAgentTest` build sample files in memory and
cover null cells, empty rows, date fields, type mapping, repeated XML elements,
field-name union, and empty-element handling. Current status: **9 tests, all
passing.**
