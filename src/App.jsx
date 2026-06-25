import { useState } from "react";
import UploadPage from "./pages/UploadPage";
import PreviewPage from "./pages/PreviewPage";
import DownloadPage from "./pages/DownloadPage";
import CataloguePage from "./pages/CataloguePage";
import Header from "./components/Header";
import StepIndicator from "./components/StepIndicator";
import "./styles.css";

export default function App() {
  const [view, setView] = useState("wizard"); // "wizard" | "catalogue"
  const [currentStep, setCurrentStep] = useState(1); // 1: Upload, 2: Preview, 3: Download
  const [metadata, setMetadata] = useState(null);
  const [fileName, setFileName] = useState("");
  const [originalFile, setOriginalFile] = useState(null);

  const handleMetadataReady = (data, name, file) => {
    setMetadata(data);
    setFileName(name);
    setOriginalFile(file);
    setCurrentStep(2);
  };

  const handleProceedToDownload = (updatedMetadata) => {
    setMetadata(updatedMetadata);
    setCurrentStep(3);
  };

  const handleReset = () => {
    setMetadata(null);
    setFileName("");
    setOriginalFile(null);
    setCurrentStep(1);
    setView("wizard");
  };

  return (
    <div className="app-shell">
      <Header view={view} onNavigate={setView} />
      <main className="main-content">
        {view === "catalogue" ? (
          <CataloguePage onUploadNew={() => { handleReset(); }} />
        ) : (
          <>
            <StepIndicator currentStep={currentStep} />
            {currentStep === 1 && (
              <UploadPage onMetadataReady={handleMetadataReady} />
            )}
            {currentStep === 2 && (
              <PreviewPage
                metadata={metadata}
                fileName={fileName}
                onProceed={handleProceedToDownload}
                onBack={() => setCurrentStep(1)}
              />
            )}
            {currentStep === 3 && (
              <DownloadPage
                metadata={metadata}
                fileName={fileName}
                originalFile={originalFile}
                onReset={handleReset}
                onBack={() => setCurrentStep(2)}
                onViewCatalogue={() => setView("catalogue")}
              />
            )}
          </>
        )}
      </main>
    </div>
  );
}
