import { useState } from "react";
import UploadPage from "./pages/UploadPage";
import PreviewPage from "./pages/PreviewPage";
import DownloadPage from "./pages/DownloadPage";
import Header from "./components/Header";
import StepIndicator from "./components/StepIndicator";
import "./styles.css";

export default function App() {
  const [currentStep, setCurrentStep] = useState(1); // 1: Upload, 2: Preview, 3: Download
  const [metadata, setMetadata] = useState(null);
  const [fileName, setFileName] = useState("");

  const handleMetadataReady = (data, name) => {
    setMetadata(data);
    setFileName(name);
    setCurrentStep(2);
  };

  const handleProceedToDownload = (updatedMetadata) => {
    setMetadata(updatedMetadata);
    setCurrentStep(3);
  };

  const handleReset = () => {
    setMetadata(null);
    setFileName("");
    setCurrentStep(1);
  };

  return (
    <div className="app-shell">
      <Header />
      <main className="main-content">
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
            onReset={handleReset} onBack={() => setCurrentStep(2)}
          />
        )}
      </main>
    </div>
  );
}
