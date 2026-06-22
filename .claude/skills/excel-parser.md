# ExcelParserAgent

## Purpose

Parses Microsoft Excel (`.xlsx`) spreadsheets into a normalized `ParsedFile`
using Apache POI. Part of the `com.mes.agents` package.

## Contract

- **Input:** `InputStream` of an `.xlsx` file.
- **Output:** `ParsedFile`.

## Rules

- **First sheet only** — additional sheets are ignored.
- **Row 0 is the header row** — its cells become the field names. Blank header
  cells are given a generated name (e.g. `column_<index>`).
- **Handle null / empty cells** — missing cells are preserved as `null` values;
  fully empty rows are skipped.
- **Handle dates** — date-formatted numeric cells are detected
  (`DateUtil.isCellDateFormatted`) and emitted as ISO-8601 strings rather than
  raw serial numbers.
- **Handle booleans** — boolean cells are emitted as Java `Boolean`.
- String and numeric cells are mapped to their natural Java types
  (`String`, `Double`).

## Normalization

**Must apply `DataNormalizationSkill` before returning.** The agent builds the
`ParsedFile` (with `fileFormat = "xlsx"`), then passes it through
`DataNormalizationSkill.normalize(...)` so every record carries a consistent
key set and the field names are guaranteed non-empty.

## Conventions

- Spring `@Component`; `DataNormalizationSkill` injected via the constructor.
