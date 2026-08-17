function riskBadgeClass(score) {
  if (score >= 70) return "bg-alert/15 text-alert";
  if (score >= 45) return "bg-caution/15 text-caution";
  return "bg-pulse/15 text-pulse";
}

export default function FlaggedTable({ flags }) {
  return (
    <div className="bg-panel border border-line rounded-lg flex flex-col">
      <div className="px-5 py-3 border-b border-line">
        <h2 className="font-display font-semibold text-sm tracking-wide">Flagged transactions</h2>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm font-mono">
          <thead>
            <tr className="text-left text-[11px] uppercase tracking-wider text-ash font-body border-b border-line">
              <th className="px-5 py-2 font-medium">Account</th>
              <th className="px-5 py-2 font-medium">Amount</th>
              <th className="px-5 py-2 font-medium">Merchant</th>
              <th className="px-5 py-2 font-medium">Location</th>
              <th className="px-5 py-2 font-medium">Risk</th>
              <th className="px-5 py-2 font-medium font-body">Detail</th>
            </tr>
          </thead>
          <tbody>
            {flags.length === 0 && (
              <tr>
                <td colSpan={6} className="px-5 py-8 text-center text-ash font-body">
                  Nothing flagged yet.
                </td>
              </tr>
            )}
            {flags.map((f, i) => (
              <tr key={`${f.id}-${i}`} className="border-b border-line/50 last:border-b-0">
                <td className="px-5 py-2.5 text-mist">{f.accountId}</td>
                <td className="px-5 py-2.5 text-mist tabular-nums">{Number(f.amount).toFixed(2)}</td>
                <td className="px-5 py-2.5 text-ash font-body">{f.merchant}</td>
                <td className="px-5 py-2.5 text-ash font-body">{f.location}</td>
                <td className="px-5 py-2.5">
                  <span className={`px-2 py-0.5 rounded text-xs ${riskBadgeClass(f.riskScore)}`}>
                    {Math.round(f.riskScore)}
                  </span>
                </td>
                <td className="px-5 py-2.5 text-ash font-body text-xs max-w-[320px]">{f.detail}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
