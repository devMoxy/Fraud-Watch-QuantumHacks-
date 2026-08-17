function riskColor(event) {
  if (!event.flagged) return "border-l-line";
  if (event.riskScore >= 70) return "border-l-alert";
  if (event.riskScore >= 45) return "border-l-caution";
  return "border-l-pulse";
}

function riskTextColor(event) {
  if (!event.flagged) return "text-ash";
  if (event.riskScore >= 70) return "text-alert";
  if (event.riskScore >= 45) return "text-caution";
  return "text-pulse";
}

function formatTime(ts) {
  try {
    return new Date(ts).toLocaleTimeString([], { hour12: false });
  } catch {
    return ts;
  }
}

export default function TickerFeed({ events }) {
  return (
    <div className="bg-panel border border-line rounded-lg flex flex-col h-full">
      <div className="px-5 py-3 border-b border-line flex items-center justify-between">
        <h2 className="font-display font-semibold text-sm tracking-wide">Transaction stream</h2>
        <span className="text-[11px] font-mono text-ash">newest first</span>
      </div>
      <div className="overflow-y-auto flex-1 divide-y divide-line/60">
        {events.length === 0 && (
          <div className="px-5 py-10 text-center text-ash text-sm font-body">
            No transactions yet. Upload a CSV or run the replay to populate the stream.
          </div>
        )}
        {events.map((e, i) => (
          <div
            key={`${e.id}-${i}`}
            className={`animate-row-in border-l-2 ${riskColor(e)} ${
              e.flagged ? "animate-pulse-glow" : ""
            } px-5 py-2.5 flex items-center gap-4 text-sm font-mono`}
          >
            <span className="text-ash w-[70px] shrink-0">{formatTime(e.timestamp)}</span>
            <span className="text-mist w-[90px] shrink-0 truncate">{e.accountId}</span>
            <span className="text-ash flex-1 truncate font-body">{e.merchant}</span>
            <span className="text-mist w-[80px] text-right tabular-nums">
              {Number(e.amount).toFixed(2)}
            </span>
            <span className={`w-[130px] text-right text-xs ${riskTextColor(e)}`}>
              {e.flagged ? `⚠ ${e.reason.replaceAll("_", " ")}` : "clean"}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
