function Stat({ label, value, accent }) {
  return (
    <div className="flex-1 min-w-[140px] border-r border-line last:border-r-0 px-5 py-4">
      <div className="text-[11px] uppercase tracking-[0.18em] text-ash font-body">{label}</div>
      <div
        className={`mt-1 font-mono text-2xl tabular-nums ${accent || "text-mist"}`}
      >
        {value}
      </div>
    </div>
  );
}

export default function StatStrip({ stats, connected }) {
  const total = stats?.totalTransactions ?? 0;
  const flags = stats?.totalFlags ?? 0;
  const rate = stats?.flagRatePercent ?? 0;

  return (
    <div className="flex flex-wrap bg-panel border border-line rounded-lg overflow-hidden">
      <Stat label="Transactions seen" value={total.toLocaleString()} />
      <Stat label="Flagged" value={flags.toLocaleString()} accent={flags > 0 ? "text-alert" : "text-mist"} />
      <Stat label="Flag rate" value={`${rate}%`} accent="text-caution" />
      <div className="flex-1 min-w-[140px] px-5 py-4 flex flex-col justify-center">
        <div className="text-[11px] uppercase tracking-[0.18em] text-ash font-body">Feed status</div>
        <div className="mt-1 flex items-center gap-2 font-mono text-sm">
          <span
            className={`h-2 w-2 rounded-full ${connected ? "bg-pulse animate-live-dot" : "bg-ash"}`}
          />
          <span className={connected ? "text-pulse" : "text-ash"}>
            {connected ? "LIVE" : "OFFLINE"}
          </span>
        </div>
      </div>
    </div>
  );
}
