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
    // Engine contract: pci_data / npi_data are always boolean, never null.
    pci_data: Boolean(f.pciData),
    npi_data: Boolean(f.npiData),
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
    pciData: Boolean(f.pci_data),
    npiData: Boolean(f.npi_data),
    phiData: Boolean(f.phi_data),
  }));
  return {
    fileName: m.source_file,
    fileFormat: m.file_format,
    totalRecords: m.record_count ?? 0,
    totalFields: m.field_count ?? fields.length,
    pciFieldsCount: fields.filter((f) => f.pciData).length,
    npiFieldsCount: fields.filter((f) => f.npiData).length,
    phiFieldsCount: fields.filter((f) => f.phiData).length,
    fields,
  };
}

/**
 * Registers a dataset in the catalogue: uploads the original file plus a JSON
 * payload (title, description, owner, metadata) as multipart/form-data.
 *
 * @returns {Promise<{id:number, exchangeId:string, createdAt:string}>}
 */
export async function submitToCatalogue({ file, title, description, owner, metadata }) {
  if (!file) {
    throw new Error("The original file is no longer available — please re-upload it.");
  }
  const payload = {
    title,
    description,
    ownerName: owner?.accountName ?? owner?.name ?? "",
    ownerEmail: owner?.email ?? "",
    ownerRole: owner?.role ?? "",
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

/** Fetches the catalogue listing (lightweight summaries, newest first). */
export async function fetchCatalogue() {
  let res;
  try {
    res = await fetch(CATALOGUE_ENDPOINT);
  } catch {
    throw new Error(
      "Could not reach the metadata service. Make sure the backend is running on port 8050."
    );
  }
  if (!res.ok) throw new Error(`Failed to load the catalogue (${res.status}).`);
  return res.json(); // array of camelCase CatalogueSummary
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
    pci_data: Boolean(f.pciData),
    npi_data: Boolean(f.npiData),
  }));
  return {
    // catalogue-level attributes
    id: data.id,
    title: data.title,
    dataset_description: data.description,
    owner_name: data.ownerName,
    owner_email: data.ownerEmail,
    owner_role: data.ownerRole,
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
