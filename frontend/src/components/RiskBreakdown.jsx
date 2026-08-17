import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";

const REASON_LABELS = {
  Z_SCORE_OUTLIER: "Amount outlier",
  LARGE_AMOUNT_DEVIATION: "Large deviation",
  HIGH_VELOCITY: "High velocity",
  NEW_LOCATION: "New location",
};

export default function RiskBreakdown({ flagsByReason }) {
  const data = Object.entries(flagsByReason || {}).map(([key, value]) => ({
    reason: REASON_LABELS[key] || key,
    count: value,
  }));

  return (
    <div className="bg-panel border border-line rounded-lg p-5 h-full flex flex-col">
      <h2 className="font-display font-semibold text-sm tracking-wide mb-4">Flags by signal</h2>
      {data.length === 0 ? (
        <div className="flex-1 flex items-center justify-center text-ash text-sm font-body">
          No flags yet
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={data} layout="vertical" margin={{ left: 10, right: 20 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#212B40" horizontal={false} />
            <XAxis type="number" stroke="#8A93AB" fontSize={11} tickLine={false} axisLine={{ stroke: "#212B40" }} />
            <YAxis
              type="category"
              dataKey="reason"
              stroke="#8A93AB"
              fontSize={11}
              width={110}
              tickLine={false}
              axisLine={{ stroke: "#212B40" }}
            />
            <Tooltip
              contentStyle={{ background: "#161E30", border: "1px solid #212B40", borderRadius: 8 }}
              labelStyle={{ color: "#EDF1F9" }}
              itemStyle={{ color: "#23D5D0" }}
            />
            <Bar dataKey="count" fill="#23D5D0" radius={[0, 4, 4, 0]} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
