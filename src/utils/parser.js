// ─── Type Inference ──────────────────────────────────────────────────────────

function inferType(values) {
  const nonNull = values.filter(v => v !== null && v !== undefined && v !== "");
  if (nonNull.length === 0) return "null";

  const sample = nonNull.slice(0, 20);

  if (sample.every(v => typeof v === "boolean" || v === "true" || v === "false")) return "boolean";
  if (sample.every(v => typeof v === "object" && !Array.isArray(v))) return "object";
  if (sample.every(v => Array.isArray(v))) return "array";

  const asNums = sample.map(v => Number(v));
  if (sample.every((v, i) => !isNaN(asNums[i]) && String(v).trim() !== "")) {
    if (sample.every((v, i) => Number.isInteger(asNums[i]))) return "integer";
    return "float";
  }

  const dateRe = /^\d{4}-\d{2}-\d{2}(T|\s|$)/;
  if (sample.every(v => dateRe.test(String(v)))) return "date";

  return "string";
}

// ─── Tag Inference ───────────────────────────────────────────────────────────

function inferTags(fieldName, dataType) {
  const name = fieldName.toLowerCase();
  const tags = [];

  if (/(id|uuid|key|code|ref)/.test(name)) tags.push("identifier");
  if (/(date|time|at|_on|timestamp)/.test(name)) tags.push("temporal");
  if (/(amount|balance|price|cost|fee|total|rate|value)/.test(name)) tags.push("financial");
  if (/(name|first|last|full)/.test(name)) tags.push("personal");
  if (/(email|phone|address|zip|postal)/.test(name)) tags.push("contact");
  if (/(type|category|status|class|kind)/.test(name)) tags.push("categorical");
  if (/(score|rank|rating|count|num)/.test(name)) tags.push("metric");
  if (/(note|comment|description|remark)/.test(name)) tags.push("freetext");
  if (/(active|flag|is_|has_|enabled)/.test(name)) tags.push("flag");
  if (/(branch|region|state|country|city|location)/.test(name)) tags.push("geographic");

  if (dataType === "date") tags.push("date");
  if (dataType === "boolean") tags.push("boolean");

  return [...new Set(tags)];
}

// ─── PCI / NPI Detection ──────────────────────────────────────────────────────

function detectPCI(fieldName, values) {
  const name = fieldName.toLowerCase();
  if (/(card|pan|cvv|ccv|credit_num|debit_num|card_num|expir|account_num)/.test(name)) return true;
  return false;
}

function detectNPI(fieldName) {
  const name = fieldName.toLowerCase();
  return /(ssn|social_sec|dob|birth|passport|license|patient|npi|tax_id|ein)/.test(name);
}

// ─── Description Generator ───────────────────────────────────────────────────

function generateDescription(fieldName, dataType, tags) {
  const name = fieldName.replace(/_/g, " ").replace(/\b\w/g, c => c.toUpperCase());
  if (tags.includes("identifier")) return `Unique identifier for each ${name.replace(/(Id|Uuid|Key|Code|Ref)/gi, "").trim().toLowerCase() || "record"}.`;
  if (tags.includes("temporal"))  return `Timestamp or date value indicating when the ${name} event occurred.`;
  if (tags.includes("financial")) return `Numeric ${name.toLowerCase()} representing a monetary or calculated value.`;
  if (tags.includes("personal"))  return `Personal name or identifier associated with the record subject.`;
  if (tags.includes("contact"))   return `Contact information field: ${name.toLowerCase()}.`;
  if (tags.includes("flag"))      return `Boolean flag indicating the ${name.toLowerCase()} state of the record.`;
  if (tags.includes("categorical")) return `Categorical field classifying the record by ${name.toLowerCase()}.`;
  if (tags.includes("metric"))    return `Numeric metric representing the ${name.toLowerCase()} of the record.`;
  if (tags.includes("freetext"))  return `Optional free-text field for ${name.toLowerCase()} on the record.`;
  if (tags.includes("geographic")) return `Geographic identifier indicating the ${name.toLowerCase()} of the record.`;
  return `Field containing ${dataType} values for ${name.toLowerCase()}.`;
}

// ─── Core Field Profiler ──────────────────────────────────────────────────────

function profileField(fieldName, values) {
  const dataType   = inferType(values);
  const nonNull    = values.filter(v => v !== null && v !== undefined && v !== "");
  const nullCount  = values.length - nonNull.length;
  const unique     = new Set(nonNull.map(v => JSON.stringify(v)));
  const samples    = [...new Set(nonNull.slice(0, 20).map(v => String(v)))].slice(0, 5);
  const tags       = inferTags(fieldName, dataType);
  const description = generateDescription(fieldName, dataType, tags);

  return {
    field_name:   fieldName,
    data_type:    dataType,
    nullable:     nullCount > 0,
    null_count:   nullCount,
    unique_count: unique.size,
    sample_values: samples,
    tags,
    description,
    pci_data: detectPCI(fieldName, values),
    npi_data: detectNPI(fieldName),
  };
}

// ─── Format Parsers ───────────────────────────────────────────────────────────

function parseJSON(text) {
  const data = JSON.parse(text);
  const rows = Array.isArray(data) ? data : [data];
  if (rows.length === 0) throw new Error("JSON array is empty.");
  const keys = Object.keys(rows[0]);
  return { rows, keys };
}

function parseCSV(text) {
  const lines = text.trim().split(/\r?\n/);
  if (lines.length < 2) throw new Error("CSV must have a header and at least one data row.");

  const parseRow = (line) => {
    const cols = [];
    let cur = "", inQ = false;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      if (ch === '"') { inQ = !inQ; continue; }
      if (ch === "," && !inQ) { cols.push(cur.trim()); cur = ""; continue; }
      cur += ch;
    }
    cols.push(cur.trim());
    return cols;
  };

  const headers = parseRow(lines[0]);
  const rows = lines.slice(1).map(l => {
    const vals = parseRow(l);
    return Object.fromEntries(headers.map((h, i) => [h, vals[i] ?? null]));
  });

  return { rows, keys: headers };
}

function parseXML(text) {
  const parser = new DOMParser();
  const doc    = parser.parseFromString(text, "application/xml");
  const err    = doc.querySelector("parsererror");
  if (err) throw new Error("Invalid XML.");
  const root     = doc.documentElement;
  const children = Array.from(root.children);
  if (children.length === 0) throw new Error("XML has no child records.");
  const rows = children.map(el => {
    const obj = {};
    Array.from(el.children).forEach(child => { obj[child.tagName] = child.textContent || null; });
    return obj;
  });
  const keys = Object.keys(rows[0]);
  return { rows, keys };
}

// ─── Public API ───────────────────────────────────────────────────────────────

export async function parseFile(file) {
  const ext = file.name.split(".").pop().toLowerCase();
  const text = await file.text();

  let rows, keys, fileFormat;

  if (ext === "json") {
    ({ rows, keys } = parseJSON(text));
    fileFormat = "json";
  } else if (ext === "csv" || ext === "tsv") {
    ({ rows, keys } = parseCSV(text));
    fileFormat = "csv";
  } else if (ext === "xml") {
    ({ rows, keys } = parseXML(text));
    fileFormat = "xml";
  } else if (["xlsx", "xls"].includes(ext)) {
    throw new Error("Excel parsing requires the xlsx library. Upload a CSV export instead, or drag a JSON/CSV file.");
  } else if (ext === "parquet") {
    throw new Error("Parquet parsing is not supported in the browser demo. Upload a JSON or CSV file.");
  } else {
    throw new Error(`Unsupported format: .${ext}. Accepted: JSON, CSV, XML, Excel, Parquet.`);
  }

  const fields = keys.map(key => {
    const values = rows.map(r => {
      const v = r[key];
      return v === "" || v === "null" ? null : v;
    });
    return profileField(key, values);
  });

  return {
    source_file: file.name,
    file_format: fileFormat,
    record_count: rows.length,
    field_count: keys.length,
    extraction_timestamp: new Date().toISOString(),
    fields,
  };
}

// ─── Format Bytes ─────────────────────────────────────────────────────────────

export function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}
