// ─── Backend API client ────────────────────────────────────────────────────
//
// Sends the uploaded file to the MES Spring Boot backend and maps the
// EnhancedMetadataResponse (camelCase, produced by MetadataEnhancementSkill)
// into the snake_case shape the Preview/Download pages expect.

const ENHANCE_ENDPOINT = "/api/upload/enhance";

/**
 * Uploads a file to the backend, runs parse + Claude enrichment, and returns
 * normalized metadata for the UI.
 *
 * @param {File} file the file selected/dropped by the user
 * @returns {Promise<object>} metadata in the UI's snake_case contract
 */
export async function analyzeFile(file) {
  const form = new FormData();
  form.append("file", file);

  let res;
  try {
    res = await fetch(ENHANCE_ENDPOINT, { method: "POST", body: form });
  } catch (networkErr) {
    throw new Error(
      "Could not reach the metadata service. Make sure the backend is running on port 8050."
    );
  }

  if (!res.ok) {
    // The backend returns a JSON error envelope: { status, error, message }.
    let detail = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      if (body && body.message) detail = body.message;
    } catch {
      /* response had no JSON body */
    }
    throw new Error(detail);
  }

  const data = await res.json();
  return mapResponse(data, file.name);
}

/** Maps the backend EnhancedMetadataResponse to the UI metadata contract. */
function mapResponse(data, fallbackFileName) {
  const fields = (data.fields || []).map((f) => ({
    field_name: f.fieldName,
    data_type: f.dataType,
    nullable: f.nullable,
    null_count: f.nullCount,
    unique_count: f.uniqueCount,
    sample_values: f.sampleValues || [],
    tags: f.tags || [],
    description: f.description || "",
    // Multi-label compliance flags — always boolean, never null.
    pii_data: Boolean(f.piiData),
    npi_data: Boolean(f.npiData),
    pci_data: Boolean(f.pciData),
  }));

  return {
    source_file: data.fileName || fallbackFileName,
    file_format: data.fileFormat,
    record_count: data.totalRecords ?? 0,
    field_count: data.totalFields ?? fields.length,
    // The backend does not stamp a timestamp; record when the UI received it.
    extraction_timestamp: new Date().toISOString(),
    fields,
  };
}

// ─── Catalogue API ───────────────────────────────────────────────────────────

const CATALOGUE_ENDPOINT = "/api/catalogue";

/**
 * Reverse of mapResponse: UI snake_case metadata → the backend's camelCase
 * EnhancedMetadataResponse shape, so a registered dataset round-trips cleanly.
 */
function toBackendMetadata(m) {
  const fields = (m.fields || []).map((f) => ({
    fieldName: f.field_name,
    dataType: f.data_type,
    nullable: Boolean(f.nullable),
    nullCount: f.null_count ?? 0,
    uniqueCount: f.unique_count ?? 0,
    sampleValues: f.sample_values || [],
    tags: f.tags || [],
    description: f.description || "",
    piiData: Boolean(f.pii_data),
    npiData: Boolean(f.npi_data),
    pciData: Boolean(f.pci_data),
  }));
  return {
    fileName: m.source_file,
    fileFormat: m.file_format,
    totalRecords: m.record_count ?? 0,
    totalFields: m.field_count ?? fields.length,
    piiFieldsCount: fields.filter((f) => f.piiData).length,
    npiFieldsCount: fields.filter((f) => f.npiData).length,
    pciFieldsCount: fields.filter((f) => f.pciData).length,
    fields,
  };
}

/**
 * Registers a dataset in the catalogue: uploads the original file plus a JSON
 * payload (title, description, owner, metadata) as multipart/form-data.
 *
 * @returns {Promise<{id:number, exchangeId:string, createdAt:string}>}
 */
export async function submitToCatalogue({
  file, datasetName, description, tags, dataSteward, piiData, pciData, dataRetentionYears, metadata,
}) {
  if (!file) {
    throw new Error("The original file is no longer available — please re-upload it.");
  }
  const payload = {
    title: datasetName,           // backend stores the dataset name as `title`
    description,
    tags: tags || [],
    dataSteward,
    piiData: Boolean(piiData),
    pciData: Boolean(pciData),
    dataRetentionYears,
    metadata: toBackendMetadata(metadata),
  };

  const form = new FormData();
  form.append("file", file);
  form.append(
    "payload",
    new Blob([JSON.stringify(payload)], { type: "application/json" })
  );

  let res;
  try {
    res = await fetch(CATALOGUE_ENDPOINT, { method: "POST", body: form });
  } catch {
    throw new Error(
      "Could not reach the metadata service. Make sure the backend is running on port 8050."
    );
  }
  if (!res.ok) {
    let detail = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      if (body && body.message) detail = body.message;
    } catch {
      /* no JSON body */
    }
    throw new Error(detail);
  }
  return res.json();
}

/**
 * Fetches the catalogue listing (lightweight summaries, newest first).
 * The backend returns a paged envelope; we request a large page and return the
 * content array (the page keeps client-side search/filter working unchanged).
 */
export async function fetchCatalogue() {
  let res;
  try {
    res = await fetch(`${CATALOGUE_ENDPOINT}?size=1000`);
  } catch {
    throw new Error(
      "Could not reach the metadata service. Make sure the backend is running on port 8050."
    );
  }
  if (!res.ok) throw new Error(`Failed to load the catalogue (${res.status}).`);
  const data = await res.json();
  return Array.isArray(data) ? data : (data.content ?? []);
}

/**
 * Fetches one catalogue entry and maps it to the UI metadata shape (snake_case)
 * so the field table and the JSON/CSV/PDF downloaders can be reused as-is.
 */
export async function fetchCatalogueDetail(id) {
  let res;
  try {
    res = await fetch(`${CATALOGUE_ENDPOINT}/${id}`);
  } catch {
    throw new Error(
      "Could not reach the metadata service. Make sure the backend is running on port 8050."
    );
  }
  if (!res.ok) {
    let detail = `Failed to load dataset (${res.status}).`;
    try {
      const body = await res.json();
      if (body && body.message) detail = body.message;
    } catch {
      /* no JSON body */
    }
    throw new Error(detail);
  }
  const data = await res.json();
  const fields = (data.fields || []).map((f) => ({
    field_name: f.fieldName,
    data_type: f.dataType,
    nullable: f.nullable,
    null_count: f.nullCount,
    unique_count: f.uniqueCount,
    sample_values: f.sampleValues || [],
    tags: f.tags || [],
    description: f.description || "",
    pii_data: Boolean(f.piiData),
    npi_data: Boolean(f.npiData),
    pci_data: Boolean(f.pciData),
  }));
  return {
    // catalogue-level attributes
    id: data.id,
    title: data.title,
    dataset_description: data.description,
    data_steward: data.dataSteward,
    pii_data: Boolean(data.piiData),
    pci_data: Boolean(data.pciData),
    data_retention_years: data.dataRetentionYears,
    dataset_tags: data.tags || [],
    created_at: data.createdAt,
    // UI metadata shape (reused by the table + downloaders)
    source_file: data.sourceFileName,
    file_format: data.fileFormat,
    record_count: data.totalRecords ?? 0,
    field_count: data.totalFields ?? fields.length,
    extraction_timestamp: data.createdAt,
    fields,
  };
}

/** Direct URL to download a dataset's original file. */
export function catalogueFileUrl(id) {
  return `${CATALOGUE_ENDPOINT}/${id}/file`;
}

/** Fetches the sensitive-data classification taxonomy for the Help Guide. */
export async function fetchTaxonomy() {
  let res;
  try {
    res = await fetch("/api/taxonomy");
  } catch {
    throw new Error("Could not reach the metadata service.");
  }
  if (!res.ok) throw new Error(`Failed to load the classification guide (${res.status}).`);
  return res.json(); // [{ key, label, definition, examples[], standard }]
}
