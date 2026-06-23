import { useState, useRef } from "react";

export default function PreviewPage({ metadata, onProceed, onBack }) {
  const [fields, setFields]         = useState(metadata.fields);
  const [editingIdx, setEditingIdx] = useState(null);
  const [search, setSearch]         = useState("");
  const textareaRef = useRef(null);

  const updateDescription = (idx, value) =>
    setFields(prev => prev.map((f, i) => i === idx ? { ...f, description: value } : f));

  const handleCellClick = (idx) => {
    setEditingIdx(idx);
    setTimeout(() => textareaRef.current?.focus(), 0);
  };

  const q = search.trim().toLowerCase();
  const visibleFields = q
    ? fields.filter(f =>
        f.field_name.toLowerCase().includes(q) ||
        f.data_type.toLowerCase().includes(q) ||
        f.description.toLowerCase().includes(q) ||
        (f.tags || []).some(t => t.toLowerCase().includes(q))
      )
    : fields;

  const pciCount = fields.filter(f => f.pci_data).length;
  const npiCount = fields.filter(f => f.npi_data).length;

  return (
    <div>
      {/* ── Top: title + stats ── */}
      <div className="preview-header">
        <div>
          <h1 className="page-heading">Review Metadata</h1>
          <p className="page-sub">
            Inspect all extracted fields.&nbsp;
            <span style={{ color: "#2563eb", fontWeight: 500 }}>Click any description cell to edit it inline.</span>
          </p>
        </div>
        <div className="preview-stats">
          <div className="stat-chip">
            <div className="stat-chip-value">{metadata.record_count.toLocaleString()}</div>
            <div className="stat-chip-label">Records</div>
          </div>
          <div className="stat-chip">
            <div className="stat-chip-value">{metadata.field_count}</div>
            <div className="stat-chip-label">Fields</div>
          </div>
          <div className="stat-chip">
            <div className="stat-chip-value" style={{ color: "var(--green)" }}>{metadata.file_format.toUpperCase()}</div>
            <div className="stat-chip-label">Format</div>
          </div>
          {pciCount > 0 && (
            <div className="stat-chip" style={{ borderColor: "rgba(239,68,68,0.4)" }}>
              <div className="stat-chip-value" style={{ color: "var(--red)" }}>{pciCount}</div>
              <div className="stat-chip-label">PCI</div>
            </div>
          )}
          {npiCount > 0 && (
            <div className="stat-chip" style={{ borderColor: "rgba(245,158,11,0.4)" }}>
              <div className="stat-chip-value" style={{ color: "var(--amber)" }}>{npiCount}</div>
              <div className="stat-chip-label">NPI</div>
            </div>
          )}
        </div>
      </div>

      {/* ── Search bar — sits between header and table ── */}
      <div style={{ display: "flex", alignItems: "center", gap: 12, margin: "0 0 16px 0" }}>
        <div style={{ position: "relative", flex: 1, maxWidth: 420 }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2" strokeLinecap="round"
            style={{ position: "absolute", left: 11, top: "50%", transform: "translateY(-50%)", pointerEvents: "none" }}>
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search fields by name, type, tag or description…"
            style={{
              width: "100%",
              background: "#ffffff",
              color: "#1e293b",
              border: "1.5px solid #cbd5e1",
              borderRadius: 8,
              padding: "8px 34px",
              fontSize: 13,
              fontFamily: "Inter, sans-serif",
              outline: "none",
              boxSizing: "border-box",
            }}
            onFocus={e => { e.target.style.borderColor = "#2563eb"; e.target.style.boxShadow = "0 0 0 3px rgba(37,99,235,0.12)"; }}
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

        {/* Source file info */}
        <span style={{ fontSize: 12, color: "var(--text-2)", whiteSpace: "nowrap", display: "flex", alignItems: "center", gap: 5 }}>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
          </svg>
          <span style={{ fontFamily: "var(--font-mono)", color: "var(--text-1)" }}>{metadata.source_file}</span>
        </span>

        {q && (
          <span style={{ fontSize: 12, color: "var(--text-2)", fontFamily: "var(--font-mono)", whiteSpace: "nowrap" }}>
            {visibleFields.length}/{fields.length} fields
          </span>
        )}
      </div>

      {/* ── Table ── */}
      <div className="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Field Name</th>
              <th>Type</th>
              <th>Nullable</th>
              <th>Null</th>
              <th>Unique</th>
              <th>Sample Values</th>
              <th>Tags</th>
              <th>Description</th>
              <th>Flags</th>
            </tr>
          </thead>
          <tbody>
            {visibleFields.length === 0 ? (
              <tr>
                <td colSpan={10} style={{ textAlign: "center", padding: "36px 0", color: "var(--text-3)", fontSize: 13 }}>
                  No fields match your search.
                </td>
              </tr>
            ) : visibleFields.map((field) => {
              const realIdx = fields.findIndex(f => f.field_name === field.field_name);
              const isEditing = editingIdx === realIdx;
              return (
                <tr key={field.field_name}>
                  <td style={{ color: "var(--text-3)", fontFamily: "var(--font-mono)", fontSize: 11 }}>
                    {String(realIdx + 1).padStart(2, "0")}
                  </td>
                  <td><span className="field-name">{field.field_name}</span></td>
                  <td><span className={`type-pill type-${field.data_type}`}>{field.data_type}</span></td>
                  <td>
                    <span className="nullable-dot">
                      <span className={`dot ${field.nullable ? "dot-red" : "dot-green"}`} />
                      {field.nullable ? "Yes" : "No"}
                    </span>
                  </td>
                  <td style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: field.null_count > 0 ? "var(--amber)" : "var(--text-2)" }}>
                    {field.null_count}
                  </td>
                  <td style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--text-2)" }}>
                    {field.unique_count}
                  </td>
                  <td>
                    <div className="sample-values">
                      {(field.sample_values || []).slice(0, 3).map((v, i) => (
                        <span key={i} className="sample-value">{v}</span>
                      ))}
                    </div>
                  </td>
                  <td>
                    <div className="tag-list">
                      {(field.tags || []).map(tag => <span key={tag} className="tag">{tag}</span>)}
                    </div>
                  </td>
                  {/* Editable description */}
                  <td
                    style={{ minWidth: 200, cursor: isEditing ? "default" : "pointer" }}
                    onClick={() => !isEditing && handleCellClick(realIdx)}
                    title="Click to edit"
                  >
                    {isEditing ? (
                      <textarea
                        ref={textareaRef}
                        value={field.description}
                        onChange={e => updateDescription(realIdx, e.target.value)}
                        onBlur={() => setEditingIdx(null)}
                        rows={3}
                        style={{
                          width: "100%",
                          background: "#ffffff",
                          color: "#1d4ed8",
                          border: "2px solid #2563eb",
                          borderRadius: 6,
                          padding: "6px 8px",
                          fontSize: 13,
                          fontFamily: "Inter, sans-serif",
                          lineHeight: 1.5,
                          resize: "vertical",
                          outline: "none",
                          boxShadow: "0 0 0 3px rgba(37,99,235,0.15)",
                        }}
                      />
                    ) : (
                      <div
                        style={{ display: "flex", alignItems: "flex-start", gap: 5, padding: "3px 2px", borderRadius: 4, border: "1px dashed transparent", transition: "border-color 0.15s" }}
                        onMouseEnter={e => e.currentTarget.style.borderColor = "#2563eb"}
                        onMouseLeave={e => e.currentTarget.style.borderColor = "transparent"}
                      >
                        <span style={{ fontSize: 13, color: "var(--text-1)", lineHeight: 1.5, flex: 1 }}>{field.description}</span>
                        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#2563eb" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                          style={{ flexShrink: 0, marginTop: 3, opacity: 0.4 }}>
                          <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                          <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                        </svg>
                      </div>
                    )}
                  </td>
                  <td>
                    <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                      {field.pci_data && <span className="pci-flag">PCI</span>}
                      {field.npi_data && <span className="npi-flag">NPI</span>}
                      {!field.pci_data && !field.npi_data && <span style={{ fontSize: 11, color: "var(--text-3)" }}>—</span>}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Footer */}
      <div className="btn-group" style={{ marginTop: 28 }}>
        <button className="btn btn-ghost" onClick={onBack}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/>
          </svg>
          Back
        </button>
        <button className="btn btn-primary btn-lg" onClick={() => onProceed({ ...metadata, fields })}>
          Proceed to Download
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/>
          </svg>
        </button>
      </div>
    </div>
  );
}
