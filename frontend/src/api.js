const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";
const WS_BASE = API_BASE.replace(/^http/, "ws");

export async function fetchRecentTransactions() {
  const res = await fetch(`${API_BASE}/api/dashboard/recent`);
  if (!res.ok) throw new Error("Failed to load recent transactions");
  return res.json();
}

export async function fetchFlaggedTransactions() {
  const res = await fetch(`${API_BASE}/api/dashboard/flags`);
  if (!res.ok) throw new Error("Failed to load flagged transactions");
  return res.json();
}

export async function fetchStats() {
  const res = await fetch(`${API_BASE}/api/dashboard/stats`);
  if (!res.ok) throw new Error("Failed to load stats");
  return res.json();
}

export async function resetDashboard() {
  const res = await fetch(`${API_BASE}/api/dashboard/reset`, { method: "DELETE" });
  if (!res.ok) throw new Error("Reset failed");
  return res.json();
}

export async function uploadCsv(file, { replay = false, delayMs = 400 } = {}) {
  const formData = new FormData();
  formData.append("file", file);
  const endpoint = replay
    ? `${API_BASE}/api/transactions/replay?delayMs=${delayMs}`
    : `${API_BASE}/api/transactions/ingest`;
  const res = await fetch(endpoint, { method: "POST", body: formData });
  if (!res.ok) throw new Error("Upload failed");
  return res.json();
}

export function connectTransactionStream(onMessage) {
  const socket = new WebSocket(`${WS_BASE}/ws/transactions`);
  socket.onmessage = (evt) => {
    try {
      const data = JSON.parse(evt.data);
      onMessage(data);
    } catch (e) {
      console.error("Bad WS payload", e);
    }
  };
  return socket;
}
