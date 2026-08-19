import { useRef, useState } from "react";

export default function ControlPanel({ onIngest, onReplay, onReset, replaying }) {
  const fileInputRef = useRef(null);
  const [fileName, setFileName] = useState("");
  const [resetting, setResetting] = useState(false);
  const [loadingDemo, setLoadingDemo] = useState(false);

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    setFileName(file ? file.name : "");
  };

  const getFile = () => fileInputRef.current?.files?.[0];

  const handleReset = async () => {
    if (!window.confirm("This will permanently delete all stored transactions and anomaly flags. Continue?")) {
      return;
    }
    setResetting(true);
    try {
      await onReset();
    } finally {
      setResetting(false);
    }
  };

  const handleLoadDemo = async () => {
    setLoadingDemo(true);
    try {
      const res = await fetch("/demo-transactions.csv");
      const blob = await res.blob();
      const file = new File([blob], "demo-transactions.csv", { type: "text/csv" });
      await onIngest(file);
      onReplay(file);
    } finally {
      setLoadingDemo(false);
    }
  };

  return (
    <div className="flex items-center gap-3 font-body">
      <label className="text-xs text-ash border border-line rounded-md px-3 py-2 cursor-pointer hover:border-pulse/60 hover:text-mist transition-colors">
        {fileName || "Choose CSV"}
        <input ref={fileInputRef} type="file" accept=".csv" className="hidden" onChange={handleFileChange} />
      </label>
      <button
        onClick={handleLoadDemo}
        disabled={loadingDemo || replaying}
        className="text-xs px-3 py-2 rounded-md border border-line text-mist hover:border-pulse/60 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
      >
        {loadingDemo ? "Loading…" : "Load demo data"}
      </button>
      <button
        onClick={() => getFile() && onIngest(getFile())}
        className="text-xs px-3 py-2 rounded-md border border-line text-mist hover:border-pulse/60 transition-colors"
      >
        Seed history
      </button>
      <button
        onClick={() => getFile() && onReplay(getFile())}
        disabled={replaying}
        className="text-xs px-3 py-2 rounded-md bg-pulse text-ink font-semibold hover:bg-pulse/90 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
      >
        {replaying ? "Replaying…" : "Replay transactions"}
      </button>
      <button
        onClick={handleReset}
        disabled={resetting || replaying}
        className="text-xs px-3 py-2 rounded-md border border-alert/50 text-alert hover:bg-alert/10 hover:border-alert transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
      >
        {resetting ? "Resetting…" : "Reset"}
      </button>
    </div>
  );
}