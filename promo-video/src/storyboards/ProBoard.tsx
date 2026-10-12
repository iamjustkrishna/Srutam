// A production storyboard sheet: two rows of five 9:16 panels, each with shot
// size, timing, the cut into the next panel, action, dialogue, sound and an
// animation note.
import { AbsoluteFill } from "remotion";
import { FONT } from "../theme";

export type ProBeat = {
  t: string;
  shot: string;
  /** How we leave this panel: CUT, WHIP, MATCH CUT, RIPPLE... */
  cut?: string;
  action: string;
  lines?: [string, string][];
  sfx?: string;
  anim?: string;
  Panel: React.FC;
};

export type ProIdea = {
  num: number;
  title: string;
  logline: string;
  runtime: string;
  cast: readonly string[];
  features: readonly string[];
  beats: readonly ProBeat[];
};

export const PRO_W = 2420;
export const PRO_H = 2420;
const PW = 440;
const PH = (PW * 1920) / 1080;

export const ProBoard: React.FC<{ idea: ProIdea }> = ({ idea }) => (
  <AbsoluteFill style={{ background: "#F3EDE4", color: "#2B1E1B", fontFamily: FONT.sans, padding: "40px 48px" }}>
    <div style={{ display: "flex", gap: 32, alignItems: "flex-start", height: 250 }}>
      <div
        style={{
          flex: "none",
          width: 110,
          height: 110,
          borderRadius: 999,
          background: "#2563EB",
          color: "#FFF",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontFamily: FONT.serif,
          fontSize: 64,
          fontWeight: 700,
        }}
      >
        {idea.num}
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ fontFamily: FONT.serif, fontSize: 72, lineHeight: 1.05 }}>
          {idea.title}{" "}
          <span style={{ fontSize: 30, fontFamily: FONT.sans, color: "#7A6A60", fontWeight: 600 }}>
            · {idea.runtime} · 9:16 · storyboard v2
          </span>
        </div>
        <div style={{ fontSize: 27, lineHeight: 1.4, marginTop: 10, maxWidth: 1420, color: "#4A3A33" }}>{idea.logline}</div>
      </div>
      <div style={{ flex: "none", width: 640, fontSize: 21 }}>
        <div style={{ fontWeight: 800, letterSpacing: 2, color: "#7A6A60", fontSize: 19 }}>CAST</div>
        <div style={{ fontWeight: 700, marginBottom: 10 }}>{idea.cast.join(" · ")}</div>
        <div style={{ fontWeight: 800, letterSpacing: 2, color: "#7A6A60", fontSize: 19 }}>SHOWS (REAL APP FEATURES)</div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 6 }}>
          {idea.features.map((f) => (
            <span key={f} style={{ background: "#DBEAFE", color: "#1E40AF", borderRadius: 999, padding: "4px 14px", fontWeight: 700, fontSize: 20 }}>
              {f}
            </span>
          ))}
        </div>
        <div style={{ marginTop: 10, fontSize: 18, color: "#7A6A60" }}>
          <span style={{ color: "#DC2626", fontWeight: 800 }}>Red</span> = camera ·{" "}
          <span style={{ color: "#F97316", fontWeight: 800 }}>orange</span> = character action
        </div>
      </div>
    </div>
    <div style={{ display: "grid", gridTemplateColumns: `repeat(5, ${PW}px)`, columnGap: 28, rowGap: 30, marginTop: 14 }}>
      {idea.beats.map(({ t, shot, cut, action, lines, sfx, anim, Panel }, i) => (
        <div key={i} style={{ width: PW }}>
          <div
            style={{
              width: PW,
              height: PH,
              borderRadius: 22,
              overflow: "hidden",
              border: "5px solid #2B1E1B",
              boxShadow: "0 10px 24px rgba(43,30,27,0.25)",
              background: "#000",
            }}
          >
            <Panel />
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", marginTop: 12, fontSize: 23, fontWeight: 800 }}>
            <span>
              {i + 1}. {shot} <span style={{ color: "#7A6A60", fontWeight: 600 }}>· {t}</span>
            </span>
            {cut && <span style={{ color: "#DC2626" }}>{cut} →</span>}
          </div>
          <div style={{ marginTop: 6, fontSize: 21, lineHeight: 1.32 }}>{action}</div>
          {lines?.map(([who, line], k) => (
            <div key={k} style={{ marginTop: 4, fontSize: 21, lineHeight: 1.3 }}>
              <b>{who}:</b> <i>{line}</i>
            </div>
          ))}
          {sfx && <div style={{ marginTop: 5, fontSize: 18, color: "#7A6A60", lineHeight: 1.3 }}>♪ {sfx}</div>}
          {anim && <div style={{ marginTop: 4, fontSize: 18, color: "#B45309", lineHeight: 1.3 }}>✎ {anim}</div>}
        </div>
      ))}
    </div>
  </AbsoluteFill>
);
