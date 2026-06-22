# FileParserOrchestrator

## Purpose

Coordinates the parsing pipeline: detects the uploaded file's format and routes
it to the appropriate parser agent, returning a normalized `ParsedFile`. Part of
the `com.mes.orchestrator` package.

## Flow

1. **Receives a `MultipartFile`** from `ParserController`.
2. **Invokes `FileFormatDetectionSkill`** with the file name and content type to
   obtain a format identifier.
3. **Routes to the correct parser agent:**
   - `xlsx` → `ExcelParserAgent`
   - `xml` → `XmlParserAgent`
   - (additional formats — `json`, `csv` — route to their agents as they are added)
4. **Returns the normalized `ParsedFile`** produced by the agent (each agent has
   already applied `DataNormalizationSkill`).

## Error handling

- **Empty or null file** → `IllegalArgumentException` (surfaced by the controller
  as `400 Bad Request`).
- **Unrecognized / unsupported format** → `UnsupportedOperationException`
  (surfaced as `415 Unsupported Media Type`).
- **Unreadable file stream / parse failure** → surfaced as `422 Unprocessable
  Entity`.

## Conventions

- Spring `@Service`; `FileFormatDetectionSkill` and all parser agents are
  injected via the constructor.
