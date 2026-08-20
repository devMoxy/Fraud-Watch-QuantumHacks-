import { useEffect, useRef, useState, useCallback } from "react";
import {
  fetchRecentTransactions,
  fetchFlaggedTransactions,
  fetchStats,
  uploadCsv,
  resetDashboard,
  connectTransactionStream,
} from "./api";
import StatStrip from "./components/StatStrip";
import TickerFeed from "./components/TickerFeed";
import RiskBreakdown from "./components/RiskBreakdown";
import FlaggedTable from "./components/FlaggedTable";
import ControlPanel from "./components/ControlPanel";

const MAX_TICKER_ROWS = 60;

export default function App() {
  const [events, setEvents] = useState([]);
  const [flags, setFlags] = useState([]);
  const [stats, setStats] = useState(null);
  const [connected, setConnected] = useState(false);
  const [replaying, setReplaying] = useState(false);
  const socketRef = useRef(null);

  const refreshAll = useCallback(async () => {
    try {
      const [recent, flagged, statsData] = await Promise.all([
        fetchRecentTransactions(),
        fetchFlaggedTransactions(),
        fetchStats(),
      ]);
      setEvents(recent);
      setFlags(flagged);
      setStats(statsData);
    } catch (e) {
      console.error(e);
    }
  }, []);

  useEffect(() => {
    refreshAll();

    // StrictMode mounts/cleans up/remounts this effect in dev, and socket.close() doesn't
    // tear the connection down synchronously. socketRef doubles as a guard here: only the
    // socket currently held in the ref is treated as live, so a stale socket from a prior
    // effect run can't apply duplicate updates even if the server briefly still has it open.
    const socket = connectTransactionStream((event) => {
      if (socketRef.current !== socket) return;
      setEvents((prev) => [event, ...prev].slice(0, MAX_TICKER_ROWS));
      if (event.flagged) {
        setFlags((prev) => [event, ...prev].slice(0, 100));
      }
      setStats((prev) => {
        if (!prev) return prev;
        const total = prev.totalTransactions + 1;
        const totalFlags = prev.totalFlags + (event.flagged ? 1 : 0);
        return {
          ...prev,
          totalTransactions: total,
          totalFlags,
          flagRatePercent: Math.round((totalFlags / total) * 10000) / 100,
          flagsByReason: event.flagged
            ? {
                ...prev.flagsByReason,
                [event.reason]: (prev.flagsByReason?.[event.reason] || 0) + 1,
              }
            : prev.flagsByReason,
        };
      });
    });
    socket.onopen = () => { if (socketRef.current === socket) setConnected(true); };
    socket.onclose = () => { if (socketRef.current === socket) setConnected(false); };
    socket.onerror = () => { if (socketRef.current === socket) setConnected(false); };
    socketRef.current = socket;

    return () => {
      if (socketRef.current === socket) {
        socketRef.current = null;
      }
      socket.close();
    };
  }, [refreshAll]);

  const handleIngest = async (file) => {
    await uploadCsv(file, { replay: false });
    await refreshAll();
  };

  const handleReplay = async (file) => {
    setReplaying(true);
    await uploadCsv(file, { replay: true, delayMs: 350 });
    const apiBase = import.meta.env.VITE_API_BASE || "http://localhost:8080";
    const poll = setInterval(async () => {
      const res = await fetch(`${apiBase}/api/transactions/replay/status`);
      const data = await res.json();
      if (!data.inProgress) {
        clearInterval(poll);
        setReplaying(false);
        refreshAll();
      }
    }, 1000);
  };

  const handleReset = async () => {
    await resetDashboard();
    setEvents([]);
    await refreshAll();
  };

  return (
    <div className="min-h-screen bg-ink text-mist font-body">
      <header className="border-b border-line px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="h-8 w-8 rounded bg-pulse/10 border border-pulse/40 flex items-center justify-center">
            <span className="text-pulse font-mono text-sm font-semibold">FW</span>
          </div>
          <div>
            <h1 className="font-display font-semibold text-lg tracking-tight leading-none">
              FraudWatch
            </h1>
            <p className="text-[11px] text-ash font-mono tracking-wide">
              real-time transaction anomaly monitor
            </p>
          </div>
        </div>
        <ControlPanel onIngest={handleIngest} onReplay={handleReplay} onReset={handleReset} replaying={replaying} />
      </header>

      <main className="max-w-[1400px] mx-auto px-6 py-6 flex flex-col gap-6">
        <StatStrip stats={stats} connected={connected} />

        <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_1fr] gap-6" style={{ minHeight: 420 }}>
          <TickerFeed events={events} />
          <RiskBreakdown flagsByReason={stats?.flagsByReason} />
        </div>

        <FlaggedTable flags={flags} />
      </main>

      <footer className="px-6 py-6 text-center text-[11px] text-ash font-mono">
        QuantumHacks 2026 — FraudWatch
      </footer>
    </div>
  );
}
