# FileFormatDetectionSkill

## Purpose

Determines the logical file format of an upload so the orchestrator can route it
to the correct parser agent. Part of the `com.mes.skills` package.

## Detection

- Detects format from the **file extension** first, then falls back to the
  **MIME / content type**.
- Extension example: `report.xlsx` → `xlsx`.
- MIME example: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  → `xlsx`.

## Supported formats

`json`, `csv`, `xml`, `xlsx`.

## Output

- **Returns a string format identifier** used by `FileParserOrchestrator` to
  select the parser agent.
- When the format cannot be determined, returns `unknown`; routing of unknown
  formats is the orchestrator's responsibility.

## Conventions

- Spring `@Component`; stateless.
