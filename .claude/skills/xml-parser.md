# XmlParserAgent

## Purpose

Parses XML documents into a normalized `ParsedFile` using Jackson Dataformat
XML (`XmlMapper`). Part of the `com.mes.agents` package.

## Contract

- **Input:** `InputStream` of an `.xml` file.
- **Output:** `ParsedFile`.

## Rules

- **Handle single and repeated elements** — a document with repeated child
  elements produces one record per element; a document with a single record
  produces a single-element record list.
  - `<root><item>..</item><item>..</item></root>` → many records.
  - `<root><item>..</item></root>` → one record.
  - `<record><a>1</a><b>2</b></record>` → one flat record with fields `a`, `b`.
- **Flatten nested nodes** — the top-level fields of each record become map
  entries; nested objects/arrays are stored as their text representation so the
  record stays a flat `Map<String, Object>`.
- **Track all field names** — the union of keys across every record is collected
  (in encounter order) and reported as the field names. Empty elements
  (`<note></note>`) normalize to `null`.

## Normalization

**Must apply `DataNormalizationSkill` before returning.** The agent builds the
`ParsedFile` (with `fileFormat = "xml"`), then passes it through
`DataNormalizationSkill.normalize(...)` so every record carries the full,
consistent key set.

## Conventions

- Spring `@Component`; `DataNormalizationSkill` injected via the constructor.
