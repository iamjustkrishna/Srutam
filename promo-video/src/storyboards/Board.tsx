// A storyboard sheet for one story idea: title, logline, who it's for, the
// Srutam features it shows, then six 9:16 panels with timing, VO and sound.
import { AbsoluteFill } from "remotion";
import { FONT } from "../theme";

export type Beat = { t: string; title: string; vo: string; sfx: string; Panel: React.FC };
export type Idea = {
  num: number;
  title: string;
  logline: string;
  audience: string;
  features: readonly string[];
  length: string;
  beats: readonly Beat[];
};

export const SHEET_W = 2760;
export const SHEET_H = 1300;
const PW = 420;
const PH = (PW * 1920) / 1080;

export const IdeaBoard: React.FC<{ idea: Idea }> = ({ idea }) => (
  <AbsoluteFill style={{ background: "#F3EDE4", color: "#2B1E1B", fontFamily: FONT.sans, padding: "44px 48px" }}>
    <div style={{ display: "flex", gap: 40, alignItems: "flex-start", height: 260 }}>
      <div
        style={{
          flex: "none",
          width: 120,
          height: 120,
          borderRadius: 999,
          background: "#2563EB",
          color: "#FFF",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontFamily: FONT.serif,
          fontSize: 70,
          fontWeight: 700,
        }}
      >
        {idea.num}
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ fontFamily: FONT.serif, fontSize: 76, lineHeight: 1.05 }}>
          {idea.title}{" "}
          <span style={{ fontSize: 34, fontFamily: FONT.sans, color: "#7A6A60", fontWeight: 600 }}>
            · {idea.length} · 9:16
          </span>
        </div>
        <div style={{ fontSize: 30, lineHeight: 1.4, marginTop: 12, maxWidth: 1560, color: "#4A3A33" }}>{idea.logline}</div>
      </div>
      <div style={{ flex: "none", width: 720, fontSize: 24 }}>
        <div style={{ fontWeight: 800, letterSpacing: 2, color: "#7A6A60", fontSize: 21 }}>FOR</div>
        <div style={{ fontWeight: 700, marginBottom: 14 }}>{idea.audience}</div>
        <div style={{ fontWeight: 800, letterSpacing: 2, color: "#7A6A60", fontSize: 21 }}>SHOWS (REAL APP FEATURES)</div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 8 }}>
          {idea.features.map((f) => (
            <span key={f} style={{ background: "#DBEAFE", color: "#1E40AF", borderRadius: 999, padding: "4px 14px", fontWeight: 700, fontSize: 21 }}>
              {f}
            </span>
          ))}
        </div>
      </div>
    </div>
    <div style={{ display: "flex", gap: 24, marginTop: 20 }}>
      {idea.beats.map(({ t, title, vo, sfx, Panel }, i) => (
        <div key={i} style={{ width: PW }}>
          <div
            style={{
              width: PW,
              height: PH,
              borderRadius: 26,
              overflow: "hidden",
              border: "5px solid #2B1E1B",
              boxShadow: "0 10px 24px rgba(43,30,27,0.25)",
              background: "#000",
            }}
          >
            <Panel />
          </div>
          <div style={{ marginTop: 16, fontSize: 25, fontWeight: 800 }}>
            {i + 1}. {title} <span style={{ color: "#7A6A60", fontWeight: 600 }}>· {t}</span>
          </div>
          <div style={{ marginTop: 6, fontSize: 23, lineHeight: 1.35, fontStyle: "italic" }}>{vo}</div>
          <div style={{ marginTop: 6, fontSize: 20, lineHeight: 1.3, color: "#7A6A60" }}>♪ {sfx}</div>
        </div>
      ))}
    </div>
  </AbsoluteFill>
);
