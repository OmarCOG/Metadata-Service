import { useState, useRef } from "react";

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

export default function PreviewPage({ metadata, onProceed, onBack }) {
  const [fields, setFields]         = useState(metadata.fields);
  const [editingIdx, setEditingIdx] = useState(null);
  const [search, setSearch]         = useState("");
  const textareaRef = useRef(null);

  // Tag editing state: which tag is open for inline edit, its draft text, and
  // which field's "add tag" input is open plus its draft.
  const [editingTag, setEditingTag] = useState(null); // { fieldIdx, tagIdx }
  const [tagDraft, setTagDraft]     = useState("");
  const [addingForIdx, setAddingForIdx] = useState(null);
  const [newTag, setNewTag]         = useState("");

  const updateDescription = (idx, value) =>
    setFields(prev => prev.map((f, i) => i === idx ? { ...f, description: value } : f));

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
            <span style={{ color: "#103a63", fontWeight: 500 }}>Click a description to edit it; click a tag to rename, &times; to remove, or + to add.</span>
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
