/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#0A0E17",
        panel: "#121826",
        "panel-raised": "#161E30",
        line: "#212B40",
        pulse: "#23D5D0",
        alert: "#FF5A5F",
        caution: "#F5B93D",
        ash: "#8A93AB",
        mist: "#EDF1F9",
      },
      fontFamily: {
        display: ["Space Grotesk", "sans-serif"],
        mono: ["IBM Plex Mono", "monospace"],
        body: ["Inter", "sans-serif"],
      },
      keyframes: {
        "row-in": {
          "0%": { opacity: 0, transform: "translateY(-6px)" },
          "100%": { opacity: 1, transform: "translateY(0)" },
        },
        "pulse-glow": {
          "0%, 100%": { boxShadow: "0 0 0 0 rgba(255, 90, 95, 0.0)" },
          "50%": { boxShadow: "0 0 0 3px rgba(255, 90, 95, 0.15)" },
        },
        "live-dot": {
          "0%, 100%": { opacity: 1 },
          "50%": { opacity: 0.35 },
        },
      },
      animation: {
        "row-in": "row-in 240ms ease-out",
        "pulse-glow": "pulse-glow 1.8s ease-in-out infinite",
        "live-dot": "live-dot 1.6s ease-in-out infinite",
      },
    },
  },
  plugins: [],
};
