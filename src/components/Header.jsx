export default function Header({ view = "wizard", onNavigate }) {
  const navLink = (key, label) => {
    const active = view === key;
    return (
      <button
        onClick={() => onNavigate?.(key)}
        style={{
          background: "none",
          border: "none",
          cursor: "pointer",
          fontFamily: "Inter, sans-serif",
          fontSize: 13,
          fontWeight: 600,
          padding: "6px 4px",
          color: active ? "#ffffff" : "rgba(231,238,248,0.62)",
          borderBottom: `2px solid ${active ? "var(--gold)" : "transparent"}`,
          transition: "color 0.15s",
        }}
      >
        {label}
      </button>
    );
  };

  return (
    <header className="header">
      <div className="header-logo">
        <div className="header-logo-mark">MX</div>
        <span className="header-product">Exchange Metadata Service</span>
      </div>
      <div className="header-divider" />
      <span className="header-sub">Capital One · Internal Tooling</span>
      <nav style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 20 }}>
        {navLink("wizard", "Upload")}
        {navLink("catalogue", "Catalogue")}
      </nav>
    </header>
  );
}
