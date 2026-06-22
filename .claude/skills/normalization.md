# DataNormalizationSkill

## Purpose

Validates and normalizes the `ParsedFile` output of **any** parser so that every
downstream consumer receives a consistent structure, regardless of the original
file format. Part of the `com.mes.skills` package.

## Guarantees

`normalize(ParsedFile)` enforces:

- **`fileFormat` is always set** — non-null and non-blank; otherwise the skill
  rejects the input.
- **`fieldNames` is never null or empty** — when a parser does not supply field
  names they are derived from the union of record keys; if neither is available
  the input is rejected.
- **`records` is a consistent `List<Map<String, Object>>`** — every record is
  rebuilt to contain exactly the same ordered key set; missing values are filled
  with `null`.

## Usage

- **This skill is called by every parser agent** (`ExcelParserAgent`,
  `XmlParserAgent`, and any future parser) before the `ParsedFile` is returned.
- Returns the normalized `ParsedFile`.

## Conventions

- Spring `@Component`; injected into each parser agent via the constructor.
