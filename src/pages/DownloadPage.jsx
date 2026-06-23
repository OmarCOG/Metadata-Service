import { useState } from "react";

const FORMATS = [
  {
    key: "json", label: "JSON",
    color: "var(--accent)", bg: "var(--accent-dim)", border: "rgba(59,130,246,0.3)",
    desc: "Full metadata object — for API submission or programmatic use.",
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
        <path d="M8 13h.01M12 13h.01M16 13h.01"/>
      </svg>
    ),
  },
  {
    key: "csv", label: "CSV",
    color: "var(--green)", bg: "var(--green-dim)", border: "rgba(34,197,94,0.3)",
    desc: "Tabular view of all field metadata — open in Excel or Sheets.",
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
        <line x1="8" y1="13" x2="16" y2="13"/><line x1="8" y1="17" x2="16" y2="17"/>
      </svg>
    ),
  },
  {
    key: "pdf", label: "PDF",
    color: "var(--purple)", bg: "var(--purple-dim)", border: "rgba(167,139,250,0.3)",
    desc: "Formatted printable report with color-coded type pills.",
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
        <path d="M9 13h1a2 2 0 000-4H9v8"/>
      </svg>
    ),
  },
];

const inputStyle = {
  width: "100%",
  background: "#ffffff",
  color: "#1e293b",
  border: "1.5px solid #e2e8f0",
  borderRadius: 8,
  padding: "10px 14px",
  fontSize: 14,
  fontFamily: "Inter, sans-serif",
  outline: "none",
  transition: "border-color 0.15s, box-shadow 0.15s",
  boxSizing: "border-box",
};

const labelStyle = {
  display: "block",
  fontSize: 13,
  fontWeight: 500,
  color: "#374151",
  marginBottom: 6,
};

function InputField({ label, value, onChange, placeholder, type = "text", required }) {
  return (
    <div>
      <label style={labelStyle}>
        {label}{required && <span style={{ color: "#ef4444", marginLeft: 3 }}>*</span>}
      </label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        style={inputStyle}
        onFocus={e => { e.target.style.borderColor = "#2563eb"; e.target.style.boxShadow = "0 0 0 3px rgba(37,99,235,0.12)"; }}
        onBlur={e  => { e.target.style.borderColor = "#e2e8f0"; e.target.style.boxShadow = "none"; }}
      />
    </div>
  );
}

export default function DownloadPage({ metadata, onReset, onBack }) {
  // Register modal state
  const [showModal, setShowModal] = useState(false);
  const [regForm, setRegForm]     = useState({ accountName: "", email: "", role: "" });
  const [regError, setRegError]   = useState("");
  const [regLoading, setRegLoading] = useState(false);
  const [regDone, setRegDone]     = useState(false);
  const [regId, setRegId]         = useState("");

  // Exchange submit
  const [exLoading, setExLoading] = useState(false);
  const [exDone, setExDone]       = useState(false);
  const [exId, setExId]           = useState("");

  const handleRegisterSubmit = async () => {
    if (!regForm.accountName.trim() || !regForm.email.trim() || !regForm.role.trim()) {
      setRegError("All fields are required.");
      return;
    }
    setRegError("");
    setRegLoading(true);
    try {
      // TODO: await fetch("/api/register", { method: "POST", body: JSON.stringify(regForm) })
      await new Promise(r => setTimeout(r, 1200));
      setRegId("REG-" + Math.random().toString(36).substring(2, 10).toUpperCase());
      setRegDone(true);
    } catch {
      setRegError("Registration failed. Please try again.");
    } finally {
      setRegLoading(false);
    }
  };

  const handleExchangeSubmit = async () => {
    setExLoading(true);
    try {
      // TODO: await fetch("/api/exchange/submit", { method: "POST", body: JSON.stringify(metadata) })
      await new Promise(r => setTimeout(r, 1400));
      setExId("EXC-" + Math.random().toString(36).substring(2, 10).toUpperCase());
      setExDone(true);
    } finally {
      setExLoading(false);
    }
  };

  const closeModal = () => {
    if (regDone) return; // keep modal visible on success so user can read ID
    setShowModal(false);
    setRegError("");
  };

  return (
    <div>
      <h1 className="page-heading">Download &amp; Submit</h1>
      <p className="page-sub" style={{ marginBottom: 32 }}>
        Download metadata exports, register the dataset, or submit to Exchange.
      </p>

      {/* ── Summary bar ── */}
      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "16px 24px", display: "flex", alignItems: "center", flexWrap: "wrap", gap: 0, marginBottom: 32 }}>
        {[
          { label: "Source",  val: metadata.source_file },
          { label: "Fields",  val: metadata.field_count,              color: "var(--accent)" },
          { label: "Records", val: metadata.record_count.toLocaleString(), color: "var(--accent)" },
          { label: "Format",  val: metadata.file_format.toUpperCase(), color: "var(--green)" },
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

      {/* ── Download cards — UI only, backend wired later ── */}
      <h2 style={{ fontSize: 14, fontWeight: 600, color: "var(--text-2)", textTransform: "uppercase", letterSpacing: "0.8px", marginBottom: 14, fontFamily: "var(--font-mono)" }}>Download</h2>
      <div className="download-grid" style={{ marginBottom: 36 }}>
        {FORMATS.map(fmt => (
          <div className="download-card" key={fmt.key}>
            <div className="download-card-icon" style={{ background: fmt.bg, border: `1px solid ${fmt.border}` }}>
              <span style={{ color: fmt.color }}>{fmt.icon}</span>
            </div>
            <div>
              <div className="download-card-title">{fmt.label}</div>
              <div className="download-card-desc">{fmt.desc}</div>
            </div>
            <button className="btn btn-secondary">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
              Download {fmt.label}
            </button>
          </div>
        ))}
      </div>

      {/* ── Register button ── */}
      <h2 style={{ fontSize: 14, fontWeight: 600, color: "var(--text-2)", textTransform: "uppercase", letterSpacing: "0.8px", marginBottom: 14, fontFamily: "var(--font-mono)" }}>Register &amp; Submit</h2>

      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", alignItems: "stretch" }}>

        {/* Register card */}
        <div style={{ flex: 1, minWidth: 260, background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "24px", display: "flex", flexDirection: "column", gap: 14 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ width: 40, height: 40, background: "rgba(37,99,235,0.1)", border: "1px solid rgba(37,99,235,0.25)", borderRadius: 10, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#2563eb" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <div>
              <div style={{ fontWeight: 600, fontSize: 15, color: "var(--text-1)" }}>
                Register Dataset
                {regDone && <span style={{ marginLeft: 8, fontSize: 10, fontFamily: "var(--font-mono)", color: "var(--green)", background: "var(--green-dim)", border: "1px solid rgba(34,197,94,0.3)", padding: "2px 7px", borderRadius: 4 }}>DONE</span>}
              </div>
              <div style={{ fontSize: 12, color: "var(--text-2)", marginTop: 2 }}>Register this dataset in the catalog</div>
            </div>
          </div>
          <button
            className="btn btn-primary"
            onClick={() => { setShowModal(true); setRegDone(false); setRegError(""); }}
            style={{ alignSelf: "flex-start" }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
            Register
          </button>
          {regDone && (
            <div style={{ fontSize: 12, color: "var(--green)", fontFamily: "var(--font-mono)", background: "var(--green-dim)", border: "1px solid rgba(34,197,94,0.25)", borderRadius: 6, padding: "6px 10px" }}>
              ID: {regId}
            </div>
          )}
        </div>

        {/* Submit to Exchange card */}
        <div style={{ flex: 1, minWidth: 260, background: "var(--surface)", border: `1px solid ${exDone ? "rgba(34,197,94,0.4)" : "var(--border)"}`, borderRadius: "var(--radius-lg)", padding: "24px", display: "flex", flexDirection: "column", gap: 14 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ width: 40, height: 40, background: exDone ? "var(--green-dim)" : "rgba(34,197,94,0.08)", border: `1px solid ${exDone ? "rgba(34,197,94,0.4)" : "rgba(34,197,94,0.2)"}`, borderRadius: 10, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              {exDone
                ? <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--green)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                : <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--green)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
              }
            </div>
            <div>
              <div style={{ fontWeight: 600, fontSize: 15, color: "var(--text-1)" }}>Submit to Exchange</div>
              <div style={{ fontSize: 12, color: "var(--text-2)", marginTop: 2 }}>
                {exDone ? "Successfully submitted to Exchange" : "Register in Capital One's data catalog"}
              </div>
            </div>
          </div>
          {!exDone ? (
            <button className="btn btn-green" onClick={handleExchangeSubmit} disabled={exLoading} style={{ alignSelf: "flex-start" }}>
              {exLoading
                ? <><div className="spinner" style={{ width: 13, height: 13, borderWidth: 2 }} /> Submitting…</>
                : <><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Submit to Exchange</>
              }
            </button>
          ) : (
            <div style={{ fontSize: 12, color: "var(--green)", fontFamily: "var(--font-mono)", background: "var(--green-dim)", border: "1px solid rgba(34,197,94,0.25)", borderRadius: 6, padding: "6px 10px" }}>
              Confirmed · {exId}
            </div>
          )}
        </div>
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

      {/* ── Register Modal ── */}
      {showModal && (
        <div
          style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.55)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000, padding: 16 }}
          onClick={closeModal}
        >
          <div
            style={{ background: "#ffffff", borderRadius: 16, padding: 36, width: "100%", maxWidth: 460, position: "relative", boxShadow: "0 20px 60px rgba(0,0,0,0.3)" }}
            onClick={e => e.stopPropagation()}
          >
            {/* Close button */}
            {!regDone && (
              <button onClick={closeModal} style={{ position: "absolute", top: 16, right: 16, background: "none", border: "none", cursor: "pointer", color: "#94a3b8", padding: 4 }} aria-label="Close">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            )}

            {regDone ? (
              /* Success screen */
              <div style={{ textAlign: "center", padding: "8px 0" }}>
                <div style={{ width: 64, height: 64, background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 18px" }}>
                  <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
                  </svg>
                </div>
                <h3 style={{ fontSize: 20, fontWeight: 700, color: "#111827", marginBottom: 8 }}>Registered Successfully</h3>
                <p style={{ fontSize: 14, color: "#6b7280", marginBottom: 16 }}>
                  Your dataset has been registered in the catalog.
                </p>
                <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: 8, padding: "10px 16px", marginBottom: 24, display: "inline-block" }}>
                  <span style={{ fontSize: 13, color: "#374151" }}>Registration ID: </span>
                  <span style={{ fontFamily: "var(--font-mono)", fontWeight: 600, color: "#16a34a", fontSize: 14 }}>{regId}</span>
                </div>
                <br />
                <button className="btn btn-primary" onClick={() => { setShowModal(false); }}>Done</button>
              </div>
            ) : (
              /* Form */
              <>
                <h3 style={{ fontSize: 20, fontWeight: 700, color: "#111827", marginBottom: 6 }}>Register Dataset</h3>
                <p style={{ fontSize: 13, color: "#6b7280", marginBottom: 28 }}>
                  Provide your details to register this dataset in the Exchange catalog.
                </p>

                <div style={{ display: "flex", flexDirection: "column", gap: 18 }}>
                  <InputField
                    label="Account Name"
                    value={regForm.accountName}
                    onChange={e => setRegForm(f => ({ ...f, accountName: e.target.value }))}
                    placeholder="e.g. Capital One Data Team"
                    required
                  />
                  <InputField
                    label="Email ID"
                    type="email"
                    value={regForm.email}
                    onChange={e => setRegForm(f => ({ ...f, email: e.target.value }))}
                    placeholder="you@capitalone.com"
                    required
                  />
                  <div>
                    <label style={labelStyle}>
                      Role<span style={{ color: "#ef4444", marginLeft: 3 }}>*</span>
                    </label>
                    <select
                      value={regForm.role}
                      onChange={e => setRegForm(f => ({ ...f, role: e.target.value }))}
                      style={{ ...inputStyle }}
                      onFocus={e => { e.target.style.borderColor = "#2563eb"; e.target.style.boxShadow = "0 0 0 3px rgba(37,99,235,0.12)"; }}
                      onBlur={e  => { e.target.style.borderColor = "#e2e8f0"; e.target.style.boxShadow = "none"; }}
                    >
                      <option value="">Select a role…</option>
                      <option value="Data Engineer">Data Engineer</option>
                      <option value="Data Analyst">Data Analyst</option>
                      <option value="Data Scientist">Data Scientist</option>
                      <option value="Product Manager">Product Manager</option>
                      <option value="Platform Engineer">Platform Engineer</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                </div>

                {regError && (
                  <div style={{ marginTop: 16, display: "flex", alignItems: "center", gap: 8, fontSize: 13, color: "#dc2626", background: "#fef2f2", border: "1px solid #fecaca", borderRadius: 8, padding: "10px 14px" }}>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" style={{ flexShrink: 0 }}>
                      <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
                    </svg>
                    {regError}
                  </div>
                )}

                <div style={{ display: "flex", gap: 10, marginTop: 28 }}>
                  <button
                    className="btn btn-primary"
                    onClick={handleRegisterSubmit}
                    disabled={regLoading}
                    style={{ flex: 1 }}
                  >
                    {regLoading
                      ? <><div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} /> Registering…</>
                      : "Register"
                    }
                  </button>
                  <button className="btn btn-ghost" onClick={closeModal}>Cancel</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
