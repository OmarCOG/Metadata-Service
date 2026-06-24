import { useState } from "react";
import { downloadJSON, downloadCSV, downloadPDF } from "../utils/downloader";

const iStyle = {
  width: "100%",
  background: "#ffffff",
  color: "#1e293b",
  border: "1.5px solid #e2e8f0",
  borderRadius: 8,
  padding: "10px 14px",
  fontSize: 14,
  fontFamily: "Inter, sans-serif",
  outline: "none",
  boxSizing: "border-box",
  transition: "border-color 0.15s, box-shadow 0.15s",
};
const lStyle   = { display: "block", fontSize: 13, fontWeight: 500, color: "#374151", marginBottom: 6 };
const focusIn  = e => { e.target.style.borderColor = "#103a63"; e.target.style.boxShadow = "0 0 0 3px rgba(16,58,99,0.14)"; };
const focusOut = e => { e.target.style.borderColor = "#e2e8f0"; e.target.style.boxShadow = "none"; };

const FORMAT_OPTIONS = [
  {
    key: "json", label: "JSON",
    desc: "Full metadata object — for API submission or programmatic use.",
    color: "var(--accent)", bg: "var(--accent-dim)", border: "rgba(59,130,246,0.3)",
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
        <path d="M8 13h.01M12 13h.01M16 13h.01"/>
      </svg>
    ),
  },
  {
    key: "csv", label: "CSV",
    desc: "Tabular field metadata — open in Excel or Google Sheets.",
    color: "var(--green)", bg: "var(--green-dim)", border: "rgba(34,197,94,0.3)",
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
        <line x1="8" y1="13" x2="16" y2="13"/><line x1="8" y1="17" x2="16" y2="17"/>
      </svg>
    ),
  },
  {
    key: "pdf", label: "PDF",
    desc: "Formatted printable report with color-coded type pills.",
    color: "var(--purple)", bg: "var(--purple-dim)", border: "rgba(167,139,250,0.3)",
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
        <path d="M9 13h1a2 2 0 000-4H9v8"/>
      </svg>
    ),
  },
];

export default function DownloadPage({ metadata, onReset, onBack }) {
  const [showDropdown, setShowDropdown] = useState(false);

  // Exchange modal
  const [modalStep, setModalStep]   = useState(null);
  const [form, setForm]             = useState({ accountName: "", email: "", role: "" });
  const [formError, setFormError]   = useState("");
  const [exchangeId, setExchangeId] = useState("");

  const openModal  = () => { setModalStep("form"); setFormError(""); setForm({ accountName: "", email: "", role: "" }); };
  const closeModal = () => { if (modalStep !== "processing") setModalStep(null); };

  const handleFormatSelect = (key) => {
    // Exports are generated client-side from the metadata the backend already
    // returned for this upload — no extra round-trip needed.
    setShowDropdown(false);
    try {
      if (key === "json") downloadJSON(metadata);
      else if (key === "csv") downloadCSV(metadata);
      else if (key === "pdf") downloadPDF(metadata);
    } catch (err) {
      alert(`Download failed: ${err.message || err}`);
    }
  };

  const handleSubmit = async () => {
    if (!form.accountName.trim() || !form.email.trim() || !form.role.trim()) {
      setFormError("All fields are required."); return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      setFormError("Please enter a valid email address."); return;
    }
    setFormError("");
    setModalStep("processing");
    try {
      // TODO: await fetch("/api/exchange/submit", { method:"POST", body: JSON.stringify({...form, metadata}) })
      await new Promise(r => setTimeout(r, 2000));
      setExchangeId("EXC-" + Math.random().toString(36).substring(2, 10).toUpperCase());
      setModalStep("done");
    } catch {
      setFormError("Submission failed. Please try again.");
      setModalStep("form");
    }
  };

  return (
    <div onClick={() => setShowDropdown(false)}>
      <h1 className="page-heading">Download &amp; Submit</h1>
      <p className="page-sub" style={{ marginBottom: 32 }}>
        Download metadata exports or submit this dataset to the Exchange catalog.
      </p>

      {/* Summary bar */}
      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "16px 24px", display: "flex", alignItems: "center", flexWrap: "wrap", gap: 0, marginBottom: 40 }}>
        {[
          { label: "Source",  val: metadata.source_file },
          { label: "Fields",  val: metadata.field_count,                  color: "var(--accent)" },
          { label: "Records", val: metadata.record_count.toLocaleString(), color: "var(--accent)" },
          { label: "Format",  val: metadata.file_format.toUpperCase(),     color: "var(--green)"  },
        ].map((item, i) => (
          <div key={item.label} style={{ display: "flex", alignItems: "center" }}>
            {i > 0 && <div style={{ width: 1, height: 28, background: "var(--border)", margin: "0 24px" }} />}
            <div>
              <div style={{ fontSize: 10, fontFamily: "var(--font-mono)", color: "var(--text-2)", textTransform: "uppercase", letterSpacing: "0.8px" }}>{item.label}</div>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: 13, color: item.color || "var(--text-1)", marginTop: 2 }}>{item.val}</div>
            </div>
          </div>
        ))}
      </div>

      {/* ── Download section — single button with format dropdown ── */}
      <p style={{ fontSize: 13, fontWeight: 600, color: "var(--text-2)", textTransform: "uppercase", letterSpacing: "0.8px", marginBottom: 14, fontFamily: "var(--font-mono)" }}>Download</p>

      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "24px 28px", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 16, marginBottom: 40 }}>
        <div>
          <div style={{ fontWeight: 600, fontSize: 15, color: "var(--text-1)" }}>Export Metadata</div>
          <div style={{ fontSize: 13, color: "var(--text-2)", marginTop: 3 }}>
            Choose a format to download the extracted metadata
          </div>
        </div>

        {/* Download button with format dropdown */}
        <div style={{ position: "relative" }} onClick={e => e.stopPropagation()}>
          <button
            className="btn btn-primary btn-lg"
            onClick={() => setShowDropdown(v => !v)}
            style={{ display: "flex", alignItems: "center", gap: 10 }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            Download
            <svg
              width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
              style={{ transition: "transform 0.18s", transform: showDropdown ? "rotate(180deg)" : "rotate(0deg)" }}
            >
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>

          {/* Format picker dropdown */}
          {showDropdown && (
            <div style={{
              position: "absolute",
              top: "calc(100% + 8px)",
              right: 0,
              width: 280,
              background: "#ffffff",
              border: "1px solid #e2e8f0",
              borderRadius: 12,
              boxShadow: "0 12px 40px rgba(0,0,0,0.14)",
              zIndex: 300,
              overflow: "hidden",
              padding: "6px 0",
            }}>
              {/* Dropdown header */}
              <div style={{ padding: "10px 16px 8px", fontSize: 10, fontWeight: 600, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.9px", fontFamily: "var(--font-mono)", borderBottom: "1px solid #f1f5f9" }}>
                Select Format
              </div>

              {FORMAT_OPTIONS.map((fmt, i) => (
                <button
                  key={fmt.key}
                  onClick={() => handleFormatSelect(fmt.key)}
                  style={{
                    width: "100%",
                    background: "none",
                    border: "none",
                    cursor: "pointer",
                    padding: "12px 16px",
                    display: "flex",
                    alignItems: "center",
                    gap: 12,
                    textAlign: "left",
                    fontFamily: "Inter, sans-serif",
                    borderBottom: i < FORMAT_OPTIONS.length - 1 ? "1px solid #f8fafc" : "none",
                    transition: "background 0.1s",
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = "#f8faff"}
                  onMouseLeave={e => e.currentTarget.style.background = "none"}
                >
                  {/* Format icon badge */}
                  <div style={{
                    width: 36, height: 36, borderRadius: 8, flexShrink: 0,
                    background: fmt.bg, border: `1px solid ${fmt.border}`,
                    display: "flex", alignItems: "center", justifyContent: "center",
                    color: fmt.color,
                  }}>
                    {fmt.icon}
                  </div>

                  {/* Label + desc */}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14, fontWeight: 600, color: "#1e293b" }}>{fmt.label}</div>
                    <div style={{ fontSize: 11, color: "#94a3b8", marginTop: 1, lineHeight: 1.4 }}>{fmt.desc}</div>
                  </div>

                  {/* Arrow */}
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
                    <line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/>
                  </svg>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Submit to Exchange */}
      <p style={{ fontSize: 13, fontWeight: 600, color: "var(--text-2)", textTransform: "uppercase", letterSpacing: "0.8px", marginBottom: 14, fontFamily: "var(--font-mono)" }}>Submit to Exchange</p>
      <div style={{ background: "var(--surface)", border: `1px solid ${modalStep === "done" ? "rgba(34,197,94,0.4)" : "var(--border)"}`, borderRadius: "var(--radius-lg)", padding: "28px 32px", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 20 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <div style={{ width: 48, height: 48, borderRadius: 12, background: modalStep === "done" ? "var(--green-dim)" : "rgba(34,197,94,0.08)", border: `1px solid ${modalStep === "done" ? "rgba(34,197,94,0.4)" : "rgba(34,197,94,0.2)"}`, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
            {modalStep === "done"
              ? <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--green)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              : <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--green)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            }
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: 15, color: "var(--text-1)" }}>Submit to Exchange</div>
            <div style={{ fontSize: 13, color: "var(--text-2)", marginTop: 3 }}>
              {modalStep === "done"
                ? <span>Submitted successfully · <span style={{ fontFamily: "var(--font-mono)", color: "var(--green)", fontWeight: 600 }}>{exchangeId}</span></span>
                : "Register this dataset in Capital One's internal Exchange catalog"
              }
            </div>
          </div>
        </div>
        {modalStep !== "done" && (
          <button className="btn btn-green btn-lg" onClick={openModal}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
            Submit to Exchange
          </button>
        )}
      </div>

      <div className="divider" />

      {/* Footer */}
      <div className="btn-group">
        <button className="btn btn-ghost" onClick={onBack}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/>
          </svg>
          Back
        </button>
        <button className="btn btn-secondary" onClick={onReset}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/>
          </svg>
          Start Over
        </button>
      </div>

      {/* Exchange Modal */}
      {modalStep && (
        <div
          style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.55)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000, padding: 16 }}
          onClick={closeModal}
        >
          <div
            style={{ background: "#ffffff", borderRadius: 16, padding: "36px 36px 32px", width: "100%", maxWidth: 460, position: "relative", boxShadow: "0 24px 64px rgba(0,0,0,0.28)" }}
            onClick={e => e.stopPropagation()}
          >
            {/* Step 1 — Form */}
            {modalStep === "form" && (
              <>
                <button onClick={closeModal} style={{ position: "absolute", top: 16, right: 16, background: "none", border: "none", cursor: "pointer", color: "#94a3b8", padding: 4 }}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
                <div style={{ width: 52, height: 52, background: "#eef2f7", border: "1px solid #cdd6e3", borderRadius: 14, display: "flex", alignItems: "center", justifyContent: "center", marginBottom: 20 }}>
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#103a63" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
                  </svg>
                </div>
                <h3 style={{ fontSize: 20, fontWeight: 700, color: "#111827", marginBottom: 6 }}>Submit to Exchange</h3>
                <p style={{ fontSize: 13, color: "#6b7280", marginBottom: 28, lineHeight: 1.6 }}>
                  Enter your details to register this dataset in Capital One's Exchange catalog.
                </p>
                <div style={{ display: "flex", flexDirection: "column", gap: 18 }}>
                  <div>
                    <label style={lStyle}>Account Name <span style={{ color: "#ef4444" }}>*</span></label>
                    <input type="text" value={form.accountName} onChange={e => setForm(f => ({ ...f, accountName: e.target.value }))} placeholder="e.g. Capital One Data Team" style={iStyle} onFocus={focusIn} onBlur={focusOut} />
                  </div>
                  <div>
                    <label style={lStyle}>Email ID <span style={{ color: "#ef4444" }}>*</span></label>
                    <input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} placeholder="you@capitalone.com" style={iStyle} onFocus={focusIn} onBlur={focusOut} />
                  </div>
                  <div>
                    <label style={lStyle}>Role <span style={{ color: "#ef4444" }}>*</span></label>
                    <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))} style={{ ...iStyle, cursor: "pointer" }} onFocus={focusIn} onBlur={focusOut}>
                      <option value="">Select a role…</option>
                      <option>Data Engineer</option>
                      <option>Data Analyst</option>
                      <option>Data Scientist</option>
                      <option>Product Manager</option>
                      <option>Platform Engineer</option>
                      <option>Other</option>
                    </select>
                  </div>
                </div>
                {formError && (
                  <div style={{ marginTop: 16, display: "flex", alignItems: "center", gap: 8, fontSize: 13, color: "#dc2626", background: "#fef2f2", border: "1px solid #fecaca", borderRadius: 8, padding: "10px 14px" }}>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" style={{ flexShrink: 0 }}>
                      <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
                    </svg>
                    {formError}
                  </div>
                )}
                <div style={{ display: "flex", gap: 10, marginTop: 28 }}>
                  <button className="btn btn-primary" onClick={handleSubmit} style={{ flex: 1 }}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
                    </svg>
                    Submit
                  </button>
                  <button className="btn btn-ghost" onClick={closeModal}>Cancel</button>
                </div>
              </>
            )}

            {/* Step 2 — Processing */}
            {modalStep === "processing" && (
              <div style={{ textAlign: "center", padding: "16px 0 8px" }}>
                <div style={{ width: 64, height: 64, margin: "0 auto 24px", position: "relative" }}>
                  <svg width="64" height="64" viewBox="0 0 64 64" style={{ position: "absolute", inset: 0, animation: "spin 1.2s linear infinite" }}>
                    <circle cx="32" cy="32" r="28" fill="none" stroke="#e7ecf3" strokeWidth="5"/>
                    <circle cx="32" cy="32" r="28" fill="none" stroke="#103a63" strokeWidth="5" strokeLinecap="round" strokeDasharray="50 126"/>
                  </svg>
                  <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#103a63" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
                    </svg>
                  </div>
                </div>
                <h3 style={{ fontSize: 18, fontWeight: 700, color: "#111827", marginBottom: 8 }}>Submitting to Exchange…</h3>
                <p style={{ fontSize: 13, color: "#6b7280", lineHeight: 1.6 }}>
                  Registering <strong style={{ color: "#1e293b" }}>{metadata.source_file}</strong> in the Exchange catalog.<br />Please wait a moment.
                </p>
              </div>
            )}

            {/* Step 3 — Done */}
            {modalStep === "done" && (
              <div style={{ textAlign: "center", padding: "8px 0" }}>
                <div style={{ width: 72, height: 72, background: "#f0fdf4", border: "2px solid #bbf7d0", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 20px" }}>
                  <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
                  </svg>
                </div>
                <h3 style={{ fontSize: 20, fontWeight: 700, color: "#111827", marginBottom: 8 }}>Submitted Successfully!</h3>
                <p style={{ fontSize: 13, color: "#6b7280", marginBottom: 24, lineHeight: 1.6 }}>
                  <strong style={{ color: "#1e293b" }}>{metadata.source_file}</strong> has been registered<br />in the Exchange catalog.
                </p>
                <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: 10, padding: "14px 20px", marginBottom: 10, textAlign: "left" }}>
                  <div style={{ fontSize: 11, color: "#6b7280", textTransform: "uppercase", letterSpacing: "0.6px", marginBottom: 8, fontFamily: "var(--font-mono)" }}>Confirmation Details</div>
                  <div style={{ display: "flex", flexDirection: "column", gap: 7 }}>
                    {[
                      { label: "Exchange ID", val: exchangeId, mono: true, green: true },
                      { label: "Account",     val: form.accountName },
                      { label: "Email",       val: form.email },
                      { label: "Role",        val: form.role },
                      { label: "Timestamp",   val: new Date().toLocaleString(), mono: true, small: true },
                    ].map(row => (
                      <div key={row.label} style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}>
                        <span style={{ color: "#6b7280" }}>{row.label}</span>
                        <span style={{ fontFamily: row.mono ? "var(--font-mono)" : "inherit", fontWeight: row.green ? 600 : 500, color: row.green ? "#16a34a" : row.small ? "#6b7280" : "#1e293b", fontSize: row.small ? 12 : 13 }}>{row.val}</span>
                      </div>
                    ))}
                  </div>
                </div>
                <button className="btn btn-primary" onClick={() => setModalStep(null)} style={{ width: "100%", marginTop: 8, justifyContent: "center" }}>
                  Done
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
