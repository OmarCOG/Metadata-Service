import { useState, useRef, useEffect } from "react";
import { fetchTaxonomy } from "../utils/api";

// Inline styles for the tag editor — kept here to match the file's existing
// inline-style approach and reuse the .tag visual language from styles.css.
const tagInputStyle = {
  fontFamily: "var(--font-mono)",
  fontSize: 10,
  fontWeight: 500,
  padding: "2px 6px",
  borderRadius: 4,
  background: "#ffffff",
  border: "1px solid #103a63",
  color: "#0a2a4a",
  outline: "none",
  boxShadow: "0 0 0 2px rgba(16,58,99,0.14)",
  boxSizing: "content-box",
};
const tagRemoveBtnStyle = {
  background: "none", border: "none", cursor: "pointer",
  color: "var(--text-3)", fontSize: 13, lineHeight: 1, padding: "0 1px",
  display: "flex", alignItems: "center",
};
const tagAddBtnStyle = {
  fontFamily: "var(--font-mono)", fontSize: 11, fontWeight: 600,
  width: 18, height: 18, borderRadius: 4, cursor: "pointer",
  background: "var(--surface-2)", border: "1px dashed var(--border-mid)",
  color: "#103a63", display: "inline-flex", alignItems: "center", justifyContent: "center",
  lineHeight: 1, padding: 0,
};

// Sensitive-data categories (multi-label). Colours reuse existing theme vars.
const FLAG_CATEGORIES = [
  { key: "pii", label: "PII", field: "pii_data", color: "var(--accent)", dim: "var(--accent-dim)" },
  { key: "npi", label: "NPI", field: "npi_data", color: "var(--amber)",  dim: "var(--amber-dim)" },
  { key: "pci", label: "PCI", field: "pci_data", color: "var(--red)",    dim: "var(--red-dim)" },
];

// One-click toggle chip styled like its category badge — lit when on, muted when off.
// A user corrects what the model flagged; the stats/banner derive from these values.
function FlagChip({ label, on, color, dim, onToggle }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      title={`Toggle ${label}`}
      aria-pressed={on}
      style={{
        fontFamily: "var(--font-mono)", fontSize: 10, fontWeight: 600,
        padding: "2px 7px", borderRadius: 4, cursor: "pointer", transition: "all 0.12s",
        border: `1px solid ${on ? color : "var(--border-mid)"}`,
        background: on ? dim : "transparent",
        color: on ? color : "var(--text-3)",
        opacity: on ? 1 : 0.65,
      }}
    >
      {label}
    </button>
  );
}

// Inline classification guide shown top-right of the Review header (from /api/taxonomy).
function ClassificationGuide() {
  const [cats, setCats] = useState([]);
  useEffect(() => { fetchTaxonomy().then(setCats).catch(() => {}); }, []);
  if (!cats.length) return null;
  const meta = (key) => FLAG_CATEGORIES.find(c => c.key === key) || { color: "var(--text-2)", dim: "var(--surface-2)" };
  return (
    <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "14px 16px", width: 380, maxWidth: 380, flexShrink: 0, boxShadow: "var(--shadow-sm)" }}>
      <div style={{ fontSize: 11, fontFamily: "var(--font-mono)", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.7px", color: "var(--text-2)", marginBottom: 10 }}>
        Classification Guide
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {cats.map(c => (
          <div key={c.key} style={{ display: "flex", gap: 8, alignItems: "flex-start" }}>
            <span title={`Examples: ${(c.examples || []).join(", ")} · ${c.standard}`}
              style={{ fontFamily: "var(--font-mono)", fontSize: 10, fontWeight: 600, padding: "1px 6px", borderRadius: 4, flexShrink: 0, marginTop: 1, color: meta(c.key).color, background: meta(c.key).dim, border: `1px solid ${meta(c.key).color}` }}>
              {c.label}
            </span>
            <span style={{ fontSize: 11.5, color: "var(--text-2)", lineHeight: 1.4 }}>{c.definition}</span>
          </div>
        ))}
      </div>
      <div style={{ fontSize: 10.5, color: "var(--text-3)", marginTop: 10, lineHeight: 1.4 }}>
        A column can fall under several categories at once.
      </div>
    </div>
  );
}

const PAGE_SIZE = 8;

export default function PreviewPage({ metadata, onProceed, onBack }) {
  const [fields, setFields]         = useState(metadata.fields);
  const [editingIdx, setEditingIdx] = useState(null);
  const [search, setSearch]         = useState("");
  const [page, setPage]             = useState(0);
  const textareaRef = useRef(null);

  // Tag editing state: which tag is open for inline edit, its draft text, and
  // which field's "add tag" input is open plus its draft.
  const [editingTag, setEditingTag] = useState(null); // { fieldIdx, tagIdx }
  const [tagDraft, setTagDraft]     = useState("");
  const [addingForIdx, setAddingForIdx] = useState(null);
  const [newTag, setNewTag]         = useState("");

  const updateDescription = (idx, value) =>
    setFields(prev => prev.map((f, i) => i === idx ? { ...f, description: value } : f));

  // Toggle a per-field compliance flag (pci_data / npi_data). Stat chips and the
  // sensitive-data banner recompute from `fields`, so they update automatically.
  const setFlag = (idx, key, value) =>
    setFields(prev => prev.map((f, i) => i === idx ? { ...f, [key]: value } : f));

  const handleCellClick = (idx) => {
    setEditingIdx(idx);
    setTimeout(() => textareaRef.current?.focus(), 0);
  };

  // ── Tag editing ──
  const removeTag = (fieldIdx, tagIdx) =>
    setFields(prev => prev.map((f, i) =>
      i === fieldIdx ? { ...f, tags: (f.tags || []).filter((_, t) => t !== tagIdx) } : f));

  const startEditTag = (fieldIdx, tagIdx, current) => {
    setEditingTag({ fieldIdx, tagIdx });
    setTagDraft(current);
  };

  const cancelEditTag = () => { setEditingTag(null); setTagDraft(""); };

  const commitEditTag = () => {
    if (!editingTag) return;
    const { fieldIdx, tagIdx } = editingTag;
    const value = tagDraft.trim();
    setFields(prev => prev.map((f, i) => {
      if (i !== fieldIdx) return f;
      const tags = [...(f.tags || [])];
      if (!value) tags.splice(tagIdx, 1);                 // emptied → remove
      else if (!tags.includes(value) || tags[tagIdx] === value) tags[tagIdx] = value; // modify (skip if dupes another)
      return { ...f, tags };
    }));
    cancelEditTag();
  };

  const startAddTag = (fieldIdx) => { setAddingForIdx(fieldIdx); setNewTag(""); };
  const cancelAddTag = () => { setAddingForIdx(null); setNewTag(""); };

  const commitAddTag = (fieldIdx) => {
    const value = newTag.trim();
    if (value) {
      setFields(prev => prev.map((f, i) =>
        i === fieldIdx
          ? { ...f, tags: (f.tags || []).includes(value) ? f.tags : [...(f.tags || []), value] }
          : f));
    }
    cancelAddTag();
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

  const counts = Object.fromEntries(
    FLAG_CATEGORIES.map(c => [c.key, fields.filter(f => f[c.field]).length])
  );
  const detected = FLAG_CATEGORIES.filter(c => counts[c.key] > 0);

  // Pagination — show PAGE_SIZE rows at a time.
  const totalPages = Math.max(1, Math.ceil(visibleFields.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const pageStart = safePage * PAGE_SIZE;
  const pageFields = visibleFields.slice(pageStart, pageStart + PAGE_SIZE);

  return (
    <div>
      {/* ── Top: title + inline classification guide ── */}
      <div className="preview-header">
        <div style={{ flex: 1, minWidth: 0 }}>
          <h1 className="page-heading">Review Metadata</h1>
          <p className="page-sub">
            Inspect all extracted fields.&nbsp;
            <span style={{ color: "#103a63", fontWeight: 500 }}>Click a description to edit it; click a tag to rename, &times; to remove, or + to add. Toggle the flag chips to correct a column's classification.</span>
          </p>
        </div>
        <ClassificationGuide />
      </div>

      {/* ── Stats row (per-category counts) ── */}
      <div className="preview-stats" style={{ margin: "0 0 16px 0" }}>
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
        {detected.map(c => (
          <div key={c.key} className="stat-chip" style={{ borderColor: c.color }}>
            <div className="stat-chip-value" style={{ color: c.color }}>{counts[c.key]}</div>
            <div className="stat-chip-label">{c.label}</div>
          </div>
        ))}
      </div>

      {/* ── Sensitive-data banner — lists every detected category ── */}
      {detected.length > 0 && (
        <div role="alert" style={{ display: "flex", alignItems: "flex-start", gap: 10, background: "var(--amber-dim)", border: "1px solid rgba(183,121,31,0.4)", borderRadius: "var(--radius)", padding: "12px 16px", margin: "0 0 16px 0", fontSize: 13, color: "var(--amber)" }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, marginTop: 1 }}>
            <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
            <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          <span>
            <strong>Sensitive data detected.</strong>{" "}
            {detected.map((c, i) => (
              <span key={c.key}>{i > 0 ? " · " : ""}{counts[c.key]} <strong>{c.label}</strong></span>
            ))}
            . Review handling before submitting to the Exchange.
          </span>
        </div>
      )}

      {/* ── Search bar — sits between header and table ── */}
      <div style={{ display: "flex", alignItems: "center", gap: 12, margin: "0 0 16px 0", flexWrap: "wrap" }}>
        <div style={{ position: "relative", flex: 1, maxWidth: 420 }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2" strokeLinecap="round"
            style={{ position: "absolute", left: 11, top: "50%", transform: "translateY(-50%)", pointerEvents: "none" }}>
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            type="text"
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0); }}
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
          {/* Proportional widths (sum 100%) — with table-layout: fixed these keep
              the table at exactly 100% of its wrapper so it never forces a page scroll. */}
          <colgroup>
            <col style={{ width: "5%" }} />
            <col style={{ width: "14%" }} />
            <col style={{ width: "8%" }} />
            <col style={{ width: "7%" }} />
            <col style={{ width: "5%" }} />
            <col style={{ width: "6%" }} />
            <col style={{ width: "15%" }} />
            <col style={{ width: "10%" }} />
            <col style={{ width: "20%" }} />
            <col style={{ width: "10%" }} />
          </colgroup>
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
            ) : pageFields.map((field) => {
              const realIdx = fields.findIndex(f => f.field_name === field.field_name);
              const isEditing = editingIdx === realIdx;
              return (
                <tr key={field.field_name}>
                  <td style={{ color: "var(--text-3)", fontFamily: "var(--font-mono)", fontSize: 11, whiteSpace: "nowrap" }}>
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
                  <td style={{ minWidth: 150 }}>
                    <div className="tag-list" style={{ alignItems: "center" }}>
                      {(field.tags || []).map((tag, tIdx) => {
                        const isEditingThis = editingTag &&
                          editingTag.fieldIdx === realIdx && editingTag.tagIdx === tIdx;
                        return isEditingThis ? (
                          <input
                            key={tIdx}
                            autoFocus
                            value={tagDraft}
                            onChange={e => setTagDraft(e.target.value)}
                            onBlur={commitEditTag}
                            onKeyDown={e => {
                              if (e.key === "Enter") commitEditTag();
                              else if (e.key === "Escape") cancelEditTag();
                            }}
                            style={{ ...tagInputStyle, width: Math.max(40, tagDraft.length * 6 + 8) }}
                          />
                        ) : (
                          <span
                            key={tIdx}
                            className="tag"
                            style={{ display: "inline-flex", alignItems: "center", gap: 4, paddingRight: 4 }}
                          >
                            <span
                              style={{ cursor: "pointer" }}
                              onClick={() => startEditTag(realIdx, tIdx, tag)}
                              title="Click to edit tag"
                            >
                              {tag}
                            </span>
                            <button
                              onClick={() => removeTag(realIdx, tIdx)}
                              title="Remove tag"
                              style={tagRemoveBtnStyle}
                              aria-label={`Remove tag ${tag}`}
                            >
                              ×
                            </button>
                          </span>
                        );
                      })}
                      {addingForIdx === realIdx ? (
                        <input
                          autoFocus
                          value={newTag}
                          onChange={e => setNewTag(e.target.value)}
                          onBlur={() => commitAddTag(realIdx)}
                          onKeyDown={e => {
                            if (e.key === "Enter") commitAddTag(realIdx);
                            else if (e.key === "Escape") cancelAddTag();
                          }}
                          placeholder="tag…"
                          style={{ ...tagInputStyle, width: Math.max(48, newTag.length * 6 + 24) }}
                        />
                      ) : (
                        <button
                          onClick={() => startAddTag(realIdx)}
                          title="Add tag"
                          style={tagAddBtnStyle}
                          aria-label="Add tag"
                        >
                          +
                        </button>
                      )}
                    </div>
                  </td>
                  {/* Editable description */}
                  <td
                    style={{ maxWidth: "100%", cursor: isEditing ? "default" : "pointer" }}
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
                          color: "#0a2a4a",
                          border: "2px solid #103a63",
                          borderRadius: 6,
                          padding: "6px 8px",
                          fontSize: 13,
                          fontFamily: "Inter, sans-serif",
                          lineHeight: 1.5,
                          resize: "vertical",
                          outline: "none",
                          boxShadow: "0 0 0 3px rgba(16,58,99,0.16)",
                        }}
                      />
                    ) : (
                      <div
                        style={{ display: "flex", alignItems: "flex-start", gap: 5, padding: "3px 2px", borderRadius: 4, border: "1px dashed transparent", transition: "border-color 0.15s" }}
                        onMouseEnter={e => e.currentTarget.style.borderColor = "#103a63"}
                        onMouseLeave={e => e.currentTarget.style.borderColor = "transparent"}
                      >
                        <span style={{ fontSize: 13, color: "var(--text-1)", lineHeight: 1.5, flex: 1 }}>{field.description}</span>
                        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#103a63" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                          style={{ flexShrink: 0, marginTop: 3, opacity: 0.4 }}>
                          <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                          <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                        </svg>
                      </div>
                    )}
                  </td>
                  <td>
                    <div style={{ display: "flex", flexWrap: "wrap", gap: 4, maxWidth: 120 }}>
                      {FLAG_CATEGORIES.map(c => (
                        <FlagChip
                          key={c.key}
                          label={c.label}
                          color={c.color}
                          dim={c.dim}
                          on={!!field[c.field]}
                          onToggle={() => setFlag(realIdx, c.field, !field[c.field])}
                        />
                      ))}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* ── Pagination (8 rows per page) ── */}
      {visibleFields.length > PAGE_SIZE && (
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginTop: 14, fontSize: 13, color: "var(--text-2)" }}>
          <span style={{ fontFamily: "var(--font-mono)", fontSize: 12 }}>
            Showing {pageStart + 1}–{Math.min(pageStart + PAGE_SIZE, visibleFields.length)} of {visibleFields.length}
          </span>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={safePage === 0}
              style={{ opacity: safePage === 0 ? 0.5 : 1 }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
              Prev
            </button>
            <span style={{ fontFamily: "var(--font-mono)", fontSize: 12 }}>Page {safePage + 1} / {totalPages}</span>
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={safePage >= totalPages - 1}
              style={{ opacity: safePage >= totalPages - 1 ? 0.5 : 1 }}
            >
              Next
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
            </button>
          </div>
        </div>
      )}

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
