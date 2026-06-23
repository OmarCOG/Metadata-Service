const STEPS = [
  { id: 1, label: "Upload File" },
  { id: 2, label: "Review Metadata" },
  { id: 3, label: "Download & Submit" },
];

export default function StepIndicator({ currentStep }) {
  return (
    <div className="step-indicator">
      {STEPS.map((step, i) => {
        const isDone   = step.id < currentStep;
        const isActive = step.id === currentStep;
        return (
          <div key={step.id} style={{ display: "flex", alignItems: "center", gap: 0, flex: i < STEPS.length - 1 ? 1 : "none" }}>
            <div className={`step-item ${isDone ? "done" : ""} ${isActive ? "active" : ""}`}>
              <div className="step-circle">
                {isDone ? (
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <path d="M2 6l3 3 5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                ) : (
                  step.id
                )}
              </div>
              <span>{step.label}</span>
            </div>
            {i < STEPS.length - 1 && (
              <div className={`step-connector ${isDone ? "done" : ""}`} />
            )}
          </div>
        );
      })}
    </div>
  );
}
