# Metadata Extraction Service (MES)

Upload a data file (xlsx / xml / csv / json / parquet); the service parses it into
a normalized `ParsedFile`, enriches it with Claude (per-field descriptions, tags,
PCI/NPI/PHI flags), and lets you register the result in a browsable, searchable
**dataset catalogue**. Java 17 / Spring Boot 3.2.5 backend + React (Vite) frontend.

## Structure

Single repo; the Spring Boot backend and the React frontend currently share the
root (`src/main/java` is the backend, `src/*.jsx` + `src/pages` etc. are the
frontend).

```
src/main/java/com/mes/...   Spring Boot backend
src/main/resources/         application.properties (+ aws profile)
src/{App.jsx,pages,components,utils,styles.css}   React (Vite) frontend
pom.xml, package.json, vite.config.js, index.html
```

## Running locally

Start the **backend first** (port **8050**); the frontend dev server proxies
`/api` to it.

- **Backend:** open the project in IntelliJ and run `MesPocApplication`. Set
  `ANTHROPIC_API_KEY` in the run config's env vars to enable Claude enrichment
  (without it, a local rules-based fallback is used). Catalogue data is stored in
  a file-based H2 DB at `./data/mes-catalogue` (inspect at
  http://localhost:8050/h2-console). For AWS, activate the `aws` profile for
  MySQL/RDS (`application-aws.properties`).
- **Frontend:**
  ```bash
  npm install      # first time
  npm run dev      # http://localhost:3000  (proxies /api -> :8050)
  ```

## API

```
POST   /api/upload              multipart (file) -> normalized ParsedFile JSON
POST   /api/upload/enhance      multipart (file) -> EnhancedMetadataResponse (parse + Claude)

POST   /api/catalogue           multipart (file + JSON payload) -> register a dataset
GET    /api/catalogue           list datasets (paged). Query params:
                                   q       free-text (title/description/owner/source file)
                                   tag     dataset has this tag
                                   format  file format (csv/xlsx/...)
                                   page, size, sort   pagination + sorting (default: createdAt desc)
                                 -> { content[], page, size, totalElements, totalPages }
GET    /api/catalogue/{id}      full metadata for one dataset
GET    /api/catalogue/{id}/file download the original uploaded file
PUT    /api/catalogue/{id}      update title/description (and optionally fields)
DELETE /api/catalogue/{id}      remove a dataset and its stored file
```

Errors use a JSON envelope `{ status, error, message }` — 400 (invalid), 404
(unknown id), 415 (unsupported format), 422 (parse failure).

## Tests

Backend tests live under `src/test/java/com/mes`:
- `agents/` — parser tests (Excel, XML, Parquet).
- `catalogue/CatalogueServiceTest` — register / detail / update / delete (Mockito,
  no DB needed).
