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
