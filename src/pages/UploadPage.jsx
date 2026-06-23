import { useState, useRef, useCallback } from "react";
import { parseFile, formatBytes } from "../utils/parser";

const SUPPORTED = ["json", "csv", "tsv", "xml", "xlsx", "xls", "parquet"];

export default function UploadPage({ onMetadataReady }) {
  const [dragOver, setDragOver] = useState(false);
  const [file, setFile]         = useState(null);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState("");
  const inputRef                = useRef(null);
  const dragCounter             = useRef(0);

  const handleFile = useCallback((f) => {
    setError("");
    const ext = f.name.split(".").pop().toLowerCase();
    if (!SUPPORTED.includes(ext)) {
      setError(`Unsupported file type: .${ext}. Accepted: JSON, CSV, XML, Excel, Parquet.`);
      return;
    }
    setFile(f);
  }, []);

  // dragCounter fixes false dragLeave when cursor moves over child nodes
  const onDragEnter = (e) => { e.preventDefault(); dragCounter.current++; setDragOver(true); };
  const onDragLeave = (e) => { e.preventDefault(); dragCounter.current--; if (dragCounter.current === 0) setDragOver(false); };
  const onDragOver  = (e) => { e.preventDefault(); };
  const onDrop      = (e) => {
    e.preventDefault();
    dragCounter.current = 0;
    setDragOver(false);
    const f = e.dataTransfer.files[0];
    if (f) handleFile(f);
  };

  // Clicking anywhere on the zone triggers file picker (single select)
  const onZoneClick = () => inputRef.current?.click();

  const onInputChange = (e) => {
    const f = e.target.files[0];
    if (f) handleFile(f);
    e.target.value = "";
  };

  const handleAnalyze = async () => {
    if (!file) return;
    setLoading(true);
    setError("");
    try {
      // TODO: replace with backend call
      // const form = new FormData(); form.append("file", file);
      // const res = await fetch("/api/upload", { method: "POST", body: form });
      // const metadata = await res.json();
      const metadata = await parseFile(file);
      onMetadataReady(metadata, file.name);
    } catch (err) {
      setError(err.message || "Failed to parse file.");
    } finally {
      setLoading(false);
    }
  };

  const extColor = (ext) => ({ json:"accent", csv:"green", xml:"purple", xlsx:"amber", xls:"amber" }[ext] || "");

  return (
    <div>
      <div style={{ marginBottom: 32 }}>
        <h1 className="page-heading">Upload a Data File</h1>
        <p className="page-sub">Drag and drop your file, or click the zone to browse. The service will extract field-level metadata automatically.</p>
      </div>

      {/* Drop zone — drag-and-drop + click to select */}
      <div
        className={`upload-zone ${dragOver ? "drag-over" : ""}`}
        onDragEnter={onDragEnter}
        onDragLeave={onDragLeave}
        onDragOver={onDragOver}
        onDrop={onDrop}
        onClick={onZoneClick}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => e.key === "Enter" && onZoneClick()}
        aria-label="Click or drag a file here to upload"
        style={{ cursor: "pointer" }}
      >
        <input
          ref={inputRef}
          type="file"
          accept=".json,.csv,.tsv,.xml,.xlsx,.xls,.parquet"
          onChange={onInputChange}
          style={{ display: "none" }}
          tabIndex={-1}
        />

        <div className="upload-icon">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
        </div>

        {dragOver ? (
          <p className="upload-title" style={{ color: "var(--accent)" }}>Release to upload</p>
        ) : (
          <>
            <p className="upload-title">Drag &amp; drop your file here</p>
            <p className="upload-hint">or click to browse from your computer</p>
          </>
        )}

        <div className="format-badges" style={{ marginTop: 10 }}>
          {["JSON","CSV","XML","EXCEL","PARQUET"].map((fmt, i) => (
            <span key={fmt} className={`badge ${["accent","green","purple","amber",""][i]}`}>{fmt}</span>
          ))}
        </div>
      </div>

      {/* Selected file card */}
      {file && (
        <div className="file-card">
          <div className="file-card-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
          </div>
          <div className="file-card-info">
            <div className="file-card-name">{file.name}</div>
            <div className="file-card-meta">
              {formatBytes(file.size)} &nbsp;·&nbsp;
              <span className={`badge ${extColor(file.name.split(".").pop().toLowerCase())}`} style={{ marginLeft: 4, padding: "1px 7px" }}>
                {file.name.split(".").pop().toUpperCase()}
              </span>
            </div>
          </div>
          <button className="btn btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); setFile(null); setError(""); }} aria-label="Remove file">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            Remove
          </button>
        </div>
      )}

      {error && (
        <div className="error-banner" role="alert">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" style={{ flexShrink: 0 }}>
            <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          {error}
        </div>
      )}

      {loading ? (
        <div className="loading-wrapper">
          <div className="spinner" />
          <p className="loading-label">Extracting metadata…</p>
        </div>
      ) : (
        <div className="btn-group" style={{ marginTop: 24 }}>
          <button className="btn btn-primary btn-lg" disabled={!file} onClick={handleAnalyze}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            Analyze File
          </button>
          {!file && <span style={{ fontSize: 13, color: "var(--text-3)" }}>Select a file above to continue</span>}
        </div>
      )}
    </div>
  );
}
