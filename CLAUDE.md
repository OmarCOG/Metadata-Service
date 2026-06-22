# Metadata Extraction Service (MES)

Service that accepts an uploaded file of any supported type, extracts
field-level metadata, and returns a normalized result for a downstream
Metadata Extraction Engine.

## Stack

- **Backend:** Java + Spring Boot
- **Frontend:** React
- **Excel parsing:** Apache POI (`poi-ooxml`)
- **XML parsing:** Jackson Dataformat XML (`jackson-dataformat-xml`)

## Package structure (`com.mes`)

| Package        | Responsibility |
|----------------|----------------|
| `models`       | Data structures, notably the normalized `ParsedFile`. |
| `skills`       | Reusable units of logic (format detection, normalization). |
| `agents`       | Format-specific parsers (Excel, XML, ...). |
| `orchestrator` | Routing/coordination between detection and the parser agents. |
| `controllers`  | HTTP entry points. |

## Core conventions

- **Every parser returns a normalized `ParsedFile` object.** Downstream
  services must work regardless of the original file format.
- **Every parser agent applies `DataNormalizationSkill` before returning.**
  No agent returns a `ParsedFile` that has not been normalized.
- **Supported formats:** `json`, `csv`, `xml`, `xlsx`.
- **All code follows standard Spring Boot conventions** — constructor
  injection, `@Component` / `@Service` / `@RestController` stereotypes,
  package-by-layer layout, and no field injection.

## Main endpoint

```
POST /api/upload     (multipart/form-data, form field: file)
→ returns the normalized ParsedFile as JSON
```

The controller delegates to `FileParserOrchestrator`, which detects the format
and routes to the correct parser agent.

## Metadata fields (downstream engine — not produced by the parsers)

> **Ownership:** The field-level metadata schema below is produced by the
> downstream **Metadata Extraction Engine**, which consumes the parsers'
> `ParsedFile` output. The parser agents in this module do **not** infer or emit
> these fields — they emit the normalized `ParsedFile` (`fileFormat`,
> `fieldNames`, raw `records`) and nothing more. This section documents the
> engine's target schema so parser output stays compatible with it.

Each field the engine describes carries the following metadata:

| Field           | Meaning |
|-----------------|---------|
| `field_name`    | Name of the field/column. |
| `data_type`     | Inferred data type. |
| `nullable`      | Whether the field may contain nulls. |
| `sample_values` | Representative example values. |
| `unique_count`  | Count of distinct values. |
| `null_count`    | Count of null/empty values. |
| `description`   | Human-readable description. |
| `tags`          | Classification tags. |
| `pci_data`      | Boolean — whether the field holds PCI (payment card) data. |
| `npi_data`      | Boolean — whether the field holds NPI (non-public personal) data. |

- In the engine's output, **`pci_data` and `npi_data` are boolean on every
  record** — never null/absent; default to `false` when not detected.

## Component reference

Detailed per-component documentation lives alongside the code conventions:

- `.claude/skills/format-detection.md` — `FileFormatDetectionSkill`
- `.claude/skills/normalization.md` — `DataNormalizationSkill`
- `.claude/skills/excel-parser.md` — `ExcelParserAgent`
- `.claude/skills/xml-parser.md` — `XmlParserAgent`
- `.claude/agents/orchestrator.md` — `FileParserOrchestrator`
