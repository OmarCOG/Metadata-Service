import { useEffect, useState } from "react";
import { fetchCatalogue, fetchCatalogueDetail, catalogueFileUrl } from "../utils/api";
import { downloadJSON, downloadCSV, downloadPDF } from "../utils/downloader";

const fmtDate = (iso) => {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleString(); } catch { return iso; }
};

// Read-only compliance badges (mirrors the Review page categories/colours).
const CAT_BADGES = [
  { key: "pii", label: "PII", field: "pii_data", color: "var(--accent)", dim: "var(--accent-dim)" },
  { key: "npi", label: "NPI", field: "npi_data", color: "var(--amber)",  dim: "var(--amber-dim)" },
  { key: "pci", label: "PCI", field: "pci_data", color: "var(--red)",    dim: "var(--red-dim)" },
];
const catBadgeStyle = (c) => ({
  fontFamily: "var(--font-mono)", fontSize: 10, fontWeight: 600, padding: "1px 6px",
  borderRadius: 4, color: c.color, background: c.dim, border: `1px solid ${c.color}`,
});

export default function CataloguePage({ onUploadNew }) {
  const [items, setItems]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState("");
  const [selectedId, setSelectedId] = useState(null);
  const [search, setSearch]   = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    fetchCatalogue()
      .then(data => { if (active) { setItems(data); setError(""); } })
      .catch(err => { if (active) setError(err.message || "Failed to load the catalogue."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  if (selectedId != null) {
    return <CatalogueDetail id={selectedId} onBack={() => setSelectedId(null)} />;
  }

  const q = search.trim().toLowerCase();
  const visibleItems = q
    ? items.filter(d =>
        (d.title || "").toLowerCase().includes(q) ||
        (d.description || "").toLowerCase().includes(q) ||
        (d.ownerName || "").toLowerCase().includes(q) ||
        (d.sourceFileName || "").toLowerCase().includes(q) ||
        (d.fileFormat || "").toLowerCase().includes(q)
      )
    : items;

  return (
    <div>
      <div className="preview-header">
        <div>
          <h1 className="page-heading">Dataset Catalogue</h1>
          <p className="page-sub">Browse datasets registered in the Exchange. Open one to view its metadata or download it.</p>
        </div>
        <button className="btn btn-primary" onClick={onUploadNew}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          Register New Dataset
        </button>
      </div>

      {/* Search bar */}
      {!loading && !error && items.length > 0 && (
        <div style={{ display: "flex", alignItems: "center", gap: 12, margin: "4px 0 20px 0" }}>
          <div style={{ position: "relative", flex: 1, maxWidth: 420 }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2" strokeLinecap="round"
              style={{ position: "absolute", left: 11, top: "50%", transform: "translateY(-50%)", pointerEvents: "none" }}>
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input
              type="text"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search datasets by title, description, owner, file or format…"
              style={{
                width: "100%", background: "#ffffff", color: "#1e293b",
                border: "1.5px solid #cbd5e1", borderRadius: 8, padding: "8px 34px",
                fontSize: 13, fontFamily: "Inter, sans-serif", outline: "none", boxSizing: "border-box",
              }}
              onFocus={e => { e.target.style.borderColor = "#103a63"; e.target.style.boxShadow = "0 0 0 3px rgba(16,58,99,0.14)"; }}
              onBlur={e  => { e.target.style.borderColor = "#cbd5e1"; e.target.style.boxShadow = "none"; }}
            />
            {search && (
              <button
                onClick={() => setSearch("")}
                style={{ position: "absolute", right: 9, top: "50%", transform: "translateY(-50%)", background: "none", border: "none", cursor: "pointer", color: "#94a3b8", padding: 2, display: "flex" }}
                aria-label="Clear search"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            )}
          </div>
          {q && (
            <span style={{ fontSize: 12, color: "var(--text-2)", fontFamily: "var(--font-mono)", whiteSpace: "nowrap" }}>
              {visibleItems.length}/{items.length} datasets
            </span>
          )}
        </div>
      )}

      {loading && (
        <div className="loading-wrapper">
          <div className="spinner" />
          <p className="loading-label">Loading catalogue…</p>
        </div>
      )}

      {!loading && error && (
        <div className="error-banner" role="alert">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" style={{ flexShrink: 0 }}>
            <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          {error}
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <div style={{ textAlign: "center", padding: "64px 0", color: "var(--text-3)" }}>
          <p style={{ fontSize: 15, marginBottom: 8 }}>No datasets registered yet.</p>
          <p style={{ fontSize: 13 }}>Upload a file and submit it to the Exchange to see it here.</p>
        </div>
      )}

      {!loading && !error && items.length > 0 && visibleItems.length === 0 && (
        <div style={{ textAlign: "center", padding: "48px 0", color: "var(--text-3)", fontSize: 13 }}>
          No datasets match your search.
        </div>
      )}

      {!loading && !error && visibleItems.length > 0 && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))", gap: 18, marginTop: 8 }}>
          {visibleItems.map(d => (
            <button
              key={d.id}
              onClick={() => setSelectedId(d.id)}
              style={{
                textAlign: "left", cursor: "pointer", background: "var(--surface)",
                border: "1px solid var(--border)", borderRadius: "var(--radius-lg)",
                padding: "20px 22px", display: "flex", flexDirection: "column", gap: 12,
                fontFamily: "Inter, sans-serif", transition: "border-color 0.15s, box-shadow 0.15s",
              }}
              onMouseEnter={e => { e.currentTarget.style.borderColor = "var(--gold)"; e.currentTarget.style.boxShadow = "var(--shadow)"; }}
              onMouseLeave={e => { e.currentTarget.style.borderColor = "var(--border)"; e.currentTarget.style.boxShadow = "none"; }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 10 }}>
                <span style={{ fontSize: 16, fontWeight: 700, color: "var(--text-1)", lineHeight: 1.3 }}>{d.title}</span>
                <span className={`type-pill type-${(d.fileFormat || "").toLowerCase()}`} style={{ flexShrink: 0 }}>{(d.fileFormat || "?").toUpperCase()}</span>
              </div>

              {d.description && (
                <p style={{ fontSize: 13, color: "var(--text-2)", lineHeight: 1.5, margin: 0,
                  display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
                  {d.description}
                </p>
              )}

              <div style={{ display: "flex", flexWrap: "wrap", gap: 8, fontSize: 12, fontFamily: "var(--font-mono)", color: "var(--text-2)" }}>
                <span>{(d.totalRecords ?? 0).toLocaleString()} records</span>
                <span style={{ color: "var(--text-3)" }}>·</span>
                <span>{d.totalFields ?? 0} fields</span>
                {d.piiData && <span className="npi-flag">PII</span>}
                {d.pciData && <span className="pci-flag">PCI</span>}
              </div>

              {d.tags && d.tags.length > 0 && (
                <div className="tag-list">
                  {d.tags.slice(0, 5).map(t => <span key={t} className="tag">{t}</span>)}
                </div>
              )}

              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 4, fontSize: 11, color: "var(--text-3)", fontFamily: "var(--font-mono)" }}>
                <span>{d.dataSteward || "—"}</span>
                <span>{fmtDate(d.createdAt)}</span>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Detail view ───────────────────────────────────────────────────────────

function CatalogueDetail({ id, onBack }) {
  const [data, setData]       = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    fetchCatalogueDetail(id)
      .then(d => { if (active) { setData(d); setError(""); } })
      .catch(err => { if (active) setError(err.message || "Failed to load dataset."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id]);

  const backBtn = (
    <button className="btn btn-ghost" onClick={onBack}>
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/>
      </svg>
      Back to Catalogue
    </button>
  );

  if (loading) {
    return <div><div style={{ marginBottom: 20 }}>{backBtn}</div><div className="loading-wrapper"><div className="spinner" /><p className="loading-label">Loading dataset…</p></div></div>;
  }
  if (error) {
    return <div><div style={{ marginBottom: 20 }}>{backBtn}</div><div className="error-banner" role="alert">{error}</div></div>;
  }

  const fields = data.fields || [];

  return (
    <div>
      <div style={{ marginBottom: 20 }}>{backBtn}</div>

      <div className="preview-header">
        <div>
          <h1 className="page-heading">{data.title}</h1>
          {data.dataset_description && <p className="page-sub" style={{ maxWidth: 720 }}>{data.dataset_description}</p>}
        </div>
        <div className="preview-stats">
          <div className="stat-chip"><div className="stat-chip-value">{(data.record_count ?? 0).toLocaleString()}</div><div className="stat-chip-label">Records</div></div>
          <div className="stat-chip"><div className="stat-chip-value">{data.field_count ?? fields.length}</div><div className="stat-chip-label">Fields</div></div>
          <div className="stat-chip"><div className="stat-chip-value" style={{ color: "var(--green)" }}>{(data.file_format || "?").toUpperCase()}</div><div className="stat-chip-label">Format</div></div>
        </div>
      </div>

      {/* Meta + downloads bar */}
      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "16px 24px", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 16, marginBottom: 28 }}>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 24, fontSize: 12, fontFamily: "var(--font-mono)", color: "var(--text-2)" }}>
          <span><span style={{ color: "var(--text-3)" }}>Source </span>{data.source_file}</span>
          <span><span style={{ color: "var(--text-3)" }}>Steward </span>{data.data_steward || "—"}</span>
          <span><span style={{ color: "var(--text-3)" }}>Retention </span>{data.data_retention_years ?? "—"} yrs</span>
          <span><span style={{ color: "var(--text-3)" }}>PII </span>{data.pii_data ? "Yes" : "No"}</span>
          <span><span style={{ color: "var(--text-3)" }}>PCI </span>{data.pci_data ? "Yes" : "No"}</span>
          <span><span style={{ color: "var(--text-3)" }}>Registered </span>{fmtDate(data.created_at)}</span>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button className="btn btn-ghost btn-sm" onClick={() => downloadJSON(data)}>JSON</button>
          <button className="btn btn-ghost btn-sm" onClick={() => downloadCSV(data)}>CSV</button>
          <button className="btn btn-ghost btn-sm" onClick={() => downloadPDF(data)}>PDF</button>
          <a className="btn btn-primary btn-sm" href={catalogueFileUrl(data.id)} download style={{ textDecoration: "none" }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            Original File
          </a>
        </div>
      </div>

      {/* Dataset tags (registration) */}
      {data.dataset_tags && data.dataset_tags.length > 0 && (
        <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: 6, marginBottom: 24 }}>
          <span style={{ fontSize: 11, fontFamily: "var(--font-mono)", color: "var(--text-3)", textTransform: "uppercase", letterSpacing: "0.6px", marginRight: 4 }}>Tags</span>
          {data.dataset_tags.map(t => <span key={t} className="tag">{t}</span>)}
        </div>
      )}

      {/* Field metadata table (read-only) */}
      <div className="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>#</th><th>Field Name</th><th>Type</th><th>Nullable</th><th>Null</th>
              <th>Unique</th><th>Sample Values</th><th>Tags</th><th>Description</th><th>Flags</th>
            </tr>
          </thead>
          <tbody>
            {fields.map((field, idx) => (
              <tr key={field.field_name + idx}>
                <td style={{ color: "var(--text-3)", fontFamily: "var(--font-mono)", fontSize: 11 }}>{String(idx + 1).padStart(2, "0")}</td>
                <td><span className="field-name">{field.field_name}</span></td>
                <td><span className={`type-pill type-${field.data_type}`}>{field.data_type}</span></td>
                <td>
                  <span className="nullable-dot">
                    <span className={`dot ${field.nullable ? "dot-red" : "dot-green"}`} />
                    {field.nullable ? "Yes" : "No"}
                  </span>
                </td>
                <td style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: field.null_count > 0 ? "var(--amber)" : "var(--text-2)" }}>{field.null_count}</td>
                <td style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--text-2)" }}>{field.unique_count}</td>
                <td>
                  <div className="sample-values">
                    {(field.sample_values || []).slice(0, 3).map((v, i) => <span key={i} className="sample-value">{v}</span>)}
                  </div>
                </td>
                <td>
                  <div className="tag-list">
                    {(field.tags || []).map(tag => <span key={tag} className="tag">{tag}</span>)}
                  </div>
                </td>
                <td style={{ minWidth: 200 }}>
                  <span style={{ fontSize: 13, color: "var(--text-1)", lineHeight: 1.5 }}>{field.description}</span>
                </td>
                <td>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
                    {CAT_BADGES.filter(c => field[c.field]).map(c => (
                      <span key={c.key} style={catBadgeStyle(c)}>{c.label}</span>
                    ))}
                    {!CAT_BADGES.some(c => field[c.field]) && <span style={{ fontSize: 11, color: "var(--text-3)" }}>—</span>}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
