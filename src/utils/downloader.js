// ─── JSON Download ────────────────────────────────────────────────────────────

export function downloadJSON(metadata) {
  const blob = new Blob([JSON.stringify(metadata, null, 2)], { type: "application/json" });
  triggerDownload(blob, `${stripExt(metadata.source_file)}_metadata.json`);
}

// ─── CSV Download ─────────────────────────────────────────────────────────────

export function downloadCSV(metadata) {
  const headers = [
    "field_name","data_type","nullable","null_count","unique_count",
    "sample_values","tags","description","pci_data","npi_data",
    "source_file","file_format","extraction_timestamp"
  ];

  const escape = v => {
    const s = String(v ?? "");
    return s.includes(",") || s.includes('"') || s.includes("\n")
      ? `"${s.replace(/"/g, '""')}"`
      : s;
  };

  const rows = metadata.fields.map(f => [
    f.field_name,
    f.data_type,
    f.nullable,
    f.null_count,
    f.unique_count,
    (f.sample_values || []).join("; "),
    (f.tags || []).join("; "),
    f.description,
    f.pci_data,
    f.npi_data,
    metadata.source_file,
    metadata.file_format,
    metadata.extraction_timestamp,
  ].map(escape).join(","));

  const csv = [headers.join(","), ...rows].join("\n");
  const blob = new Blob([csv], { type: "text/csv" });
  triggerDownload(blob, `${stripExt(metadata.source_file)}_metadata.csv`);
}

// ─── PDF Download (HTML→print) ────────────────────────────────────────────────

export function downloadPDF(metadata) {
  const rows = metadata.fields.map(f => `
    <tr>
      <td class="mono accent">${f.field_name}</td>
      <td><span class="pill pill-${f.data_type}">${f.data_type}</span></td>
      <td>${f.nullable ? "Yes" : "No"}</td>
      <td class="mono">${f.null_count}</td>
      <td class="mono">${f.unique_count}</td>
      <td class="small">${(f.sample_values || []).slice(0,3).join(", ")}</td>
      <td class="small">${(f.tags || []).join(", ")}</td>
      <td>${f.description}</td>
      <td>${f.pci_data ? "<span class='flag-red'>PCI</span>" : "—"}</td>
      <td>${f.npi_data ? "<span class='flag-amber'>NPI</span>" : "—"}</td>
    </tr>
  `).join("");

  const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<title>Metadata Report — ${metadata.source_file}</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500&family=Inter:wght@400;500;600&display=swap');
  body { font-family: 'Inter', sans-serif; font-size: 11px; color: #1a1f2e; margin: 0; padding: 32px 40px; }
  header { border-bottom: 2px solid #b08d57; padding-bottom: 16px; margin-bottom: 24px; display: flex; justify-content: space-between; align-items: flex-end; }
  h1 { font-size: 20px; font-weight: 700; margin: 0; color: #0f172a; }
  .meta-row { display: flex; gap: 24px; margin-bottom: 20px; }
  .meta-chip { background: #f4f6fa; border: 1px solid #dde4ee; border-radius: 6px; padding: 6px 12px; }
  .meta-chip label { display: block; font-size: 9px; text-transform: uppercase; letter-spacing: 0.8px; color: #64748b; }
  .meta-chip span { font-family: 'IBM Plex Mono', monospace; font-size: 13px; font-weight: 500; color: #103a63; }
  table { width: 100%; border-collapse: collapse; font-size: 10px; }
  thead { background: #f8faff; }
  th { padding: 8px 10px; text-align: left; font-size: 9px; text-transform: uppercase; letter-spacing: 0.6px; color: #64748b; border-bottom: 1px solid #e2e8f0; white-space: nowrap; }
  td { padding: 7px 10px; border-bottom: 1px solid #f1f5f9; vertical-align: top; }
  tr:nth-child(even) td { background: #f8faff; }
  .mono { font-family: 'IBM Plex Mono', monospace; }
  .accent { color: #103a63; }
  .small { font-size: 9px; color: #64748b; }
  .pill { font-family: 'IBM Plex Mono', monospace; font-size: 9px; padding: 1px 6px; border-radius: 3px; font-weight: 500; }
  .pill-string  { background: #dbeafe; color: #1d4ed8; }
  .pill-integer { background: #dcfce7; color: #15803d; }
  .pill-float   { background: #ede9fe; color: #7c3aed; }
  .pill-boolean { background: #fef9c3; color: #a16207; }
  .pill-date    { background: #ccfbf1; color: #0f766e; }
  .pill-null    { background: #fee2e2; color: #b91c1c; }
  .flag-red  { font-size: 9px; background: #fee2e2; color: #b91c1c; padding: 1px 5px; border-radius: 3px; font-weight: 600; }
  .flag-amber{ font-size: 9px; background: #fef3c7; color: #92400e; padding: 1px 5px; border-radius: 3px; font-weight: 600; }
  footer { margin-top: 32px; border-top: 1px solid #e2e8f0; padding-top: 12px; font-size: 9px; color: #94a3b8; display: flex; justify-content: space-between; }
  @media print { body { padding: 20px; } }
</style>
</head>
<body>
<header>
  <div>
    <h1>Metadata Report</h1>
    <div style="font-size:12px; color:#64748b; margin-top:4px;">${metadata.source_file} · Capital One Exchange Metadata Service</div>
  </div>
  <div style="font-size:10px; color:#94a3b8;">Generated ${new Date(metadata.extraction_timestamp).toLocaleString()}</div>
</header>

<div class="meta-row">
  <div class="meta-chip"><label>Source File</label><span>${metadata.source_file}</span></div>
  <div class="meta-chip"><label>Format</label><span>${metadata.file_format.toUpperCase()}</span></div>
  <div class="meta-chip"><label>Records</label><span>${metadata.record_count.toLocaleString()}</span></div>
  <div class="meta-chip"><label>Fields</label><span>${metadata.field_count}</span></div>
</div>

<table>
  <thead>
    <tr>
      <th>Field Name</th><th>Type</th><th>Nullable</th><th>Null Count</th>
      <th>Unique Count</th><th>Sample Values</th><th>Tags</th><th>Description</th>
      <th>PCI</th><th>NPI</th>
    </tr>
  </thead>
  <tbody>${rows}</tbody>
</table>

<footer>
  <span>Capital One · Exchange Metadata Service · Internal Use Only</span>
  <span>Extraction Timestamp: ${metadata.extraction_timestamp}</span>
</footer>
</body>
</html>`;

  const win = window.open("", "_blank");
  if (!win) { alert("Allow pop-ups to download the PDF report."); return; }
  win.document.write(html);
  win.document.close();
  win.onload = () => { win.focus(); win.print(); };
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function triggerDownload(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a   = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function stripExt(filename) {
  return filename.replace(/\.[^.]+$/, "");
}
