import { AbsoluteFill } from "remotion";
import { FONT } from "../theme";
import { PANELS, PH, PW } from "./Panels";

const CAPTIONS = [
  "Meera on a Mumbai local, mind full of fireflies.",
  "By the next station, they start to fade.",
  "Shru wakes up in the floating dock.",
  "Tap, tap: the dock opens over any app.",
  "Shru leaps and catches the thought.",
  "She just talks. Shru's tails ripple with her voice.",
  "Saved, and transcribed on her phone.",
  "Next steps, ideas and decisions, sorted.",
  "Same Meera, every moment of her day.",
  "A reminder, right on time.",
  "Night: her ideas form a constellation (Mesh).",
  "Ask anything across your notes.",
  "Speak → Transcribe → Insights → Ask.",
  "Emotional close on Marine Drive.",
  "Logo, tagline and call to action.",
];

const DW = 640;
const DH = (DW * PH) / PW;

export const PaintedBoard: React.FC = () => (
  <AbsoluteFill
    style={{
      background: "#08070F",
      padding: "36px 40px",
      fontFamily: FONT.sans,
      color: "#F1F5F9",
    }}
  >
    <div
      style={{ display: "flex", alignItems: "baseline", gap: 24, height: 90 }}
    >
      <div style={{ fontFamily: FONT.serif, fontSize: 64 }}>
        Srutam ·{" "}
        <span style={{ fontStyle: "italic", color: "#93C5FD" }}>
          Catch it before it's gone
        </span>
      </div>
      <div style={{ fontSize: 26, color: "#A5A0C8" }}>
        30 s storyboard · painted anime style · Meera &amp; Shru
      </div>
    </div>
    <div
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(5, ${DW}px)`,
        columnGap: 24,
        rowGap: 26,
      }}
    >
      {PANELS.map((Panel, i) => (
        <div key={i}>
          <div style={{ fontSize: 28, fontWeight: 700, marginBottom: 8 }}>
            {i + 1}. {i * 2}–{i * 2 + 2}s
          </div>
          <div
            style={{
              width: DW,
              height: DH,
              overflow: "hidden",
              borderRadius: 6,
            }}
          >
            <svg
              viewBox={`0 0 ${PW} ${PH}`}
              width={DW}
              height={DH}
              style={{ display: "block" }}
            >
              <Panel />
            </svg>
          </div>
          <div style={{ fontSize: 23, color: "#CBD5E1", marginTop: 10 }}>
            {CAPTIONS[i]}
          </div>
        </div>
      ))}
    </div>
  </AbsoluteFill>
);
