// Shared pieces for the story-idea storyboards: the 9:16 panel frame, speech
// bubbles, sound lettering, set props and real Srutam screens (strings from
// ReminderAlarmReceiver.kt, GlobalCopilotScreen.kt, QuickRecordingTileService.kt
// and the Insights screen).
import { useId } from "react";
import { Img } from "remotion";
import { BellIcon, BulbIcon, LockIcon, MicIcon, WaveIcon } from "../components/Icons";
import { LOGO } from "../spirit/logoData";
import { C } from "../theme";

export const W = 1080;
export const H = 1920;
export const LINE = "#4A2E28";

/** One vertical panel: painted background, paper grain and a soft vignette. */
export const Frame: React.FC<{
  bg: [string, string];
  children: React.ReactNode;
  night?: boolean;
}> = ({ bg, children, night = false }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <svg viewBox={`0 0 ${W} ${H}`} width="100%" height="100%" style={{ display: "block" }}>
      <defs>
        <linearGradient id={`bg${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={bg[0]} />
          <stop offset="100%" stopColor={bg[1]} />
        </linearGradient>
        <radialGradient id={`vg${id}`} cx="50%" cy="45%" r="75%">
          <stop offset="60%" stopColor="#000" stopOpacity={0} />
          <stop offset="100%" stopColor="#000" stopOpacity={night ? 0.55 : 0.28} />
        </radialGradient>
        <filter id={`grain${id}`}>
          <feTurbulence type="fractalNoise" baseFrequency={0.85} numOctaves={2} seed={9} />
          <feColorMatrix type="saturate" values="0" />
        </filter>
      </defs>
      <rect width={W} height={H} fill={`url(#bg${id})`} />
      {children}
      <rect width={W} height={H} fill={`url(#vg${id})`} />
      <rect width={W} height={H} filter={`url(#grain${id})`} opacity={0.08} style={{ mixBlendMode: "overlay" }} />
    </svg>
  );
};

/** Hand-lettered speech bubble. `tail` is where the tail points. */
export const Bubble: React.FC<{
  x: number;
  y: number;
  w: number;
  lines: string[];
  tail: [number, number];
  size?: number;
  think?: boolean;
  fill?: string;
  color?: string;
}> = ({ x, y, w, lines, tail, size = 54, think = false, fill = "#FFFDF8", color = "#2B1E1B" }) => {
  const h = lines.length * size * 1.18 + size * 0.9;
  const cx = x + w / 2;
  const cy = y + h / 2;
  return (
    <g>
      {think ? (
        [0.35, 0.6].map((k, i) => (
          <circle
            key={i}
            cx={cx + (tail[0] - cx) * (0.55 + k * 0.5)}
            cy={cy + (tail[1] - cy) * (0.55 + k * 0.5)}
            r={22 - i * 9}
            fill={fill}
            stroke={LINE}
            strokeWidth={4}
          />
        ))
      ) : (
        <path
          d={`M${cx - 40} ${cy} L${tail[0]} ${tail[1]} L${cx + 40} ${cy} Z`}
          fill={fill}
          stroke={LINE}
          strokeWidth={4}
          strokeLinejoin="round"
        />
      )}
      <rect x={x} y={y} width={w} height={h} rx={think ? h / 2 : 44} fill={fill} stroke={LINE} strokeWidth={4} />
      {!think && <path d={`M${cx - 36} ${cy + 4} L${cx + 36} ${cy + 4}`} stroke={fill} strokeWidth={10} />}
      <text x={cx} y={y + size * 1.05} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={size} fill={color}>
        {lines.map((l, i) => (
          <tspan key={i} x={cx} dy={i ? size * 1.18 : 0}>
            {l}
          </tspan>
        ))}
      </text>
    </g>
  );
};

/** Big comic sound lettering ("knock knock!"). */
export const Sfx: React.FC<{
  x: number;
  y: number;
  text: string;
  size?: number;
  rot?: number;
  fill?: string;
}> = ({ x, y, text, size = 120, rot = -8, fill = "#FBBF24" }) => (
  <text
    x={x}
    y={y}
    textAnchor="middle"
    transform={`rotate(${rot} ${x} ${y})`}
    fontFamily="Kalam"
    fontWeight={700}
    fontSize={size}
    fill={fill}
    stroke="#2B1E1B"
    strokeWidth={size / 18}
    paintOrder="stroke"
  >
    {text}
  </text>
);

/** Motion/impact lines radiating from a point. */
export const Burst: React.FC<{ x: number; y: number; r: number; color?: string; n?: number }> = ({
  x,
  y,
  r,
  color = "#2B1E1B",
  n = 10,
}) => (
  <g stroke={color} strokeWidth={8} strokeLinecap="round">
    {Array.from({ length: n }, (_, i) => {
      const a = (i / n) * Math.PI * 2;
      return (
        <line
          key={i}
          x1={x + Math.cos(a) * r}
          y1={y + Math.sin(a) * r}
          x2={x + Math.cos(a) * (r + 46)}
          y2={y + Math.sin(a) * (r + 46)}
        />
      );
    })}
  </g>
);

/** On-screen caption strip (burned-in subtitle style). */
export const Caption: React.FC<{ text: string; y?: number }> = ({ text, y = 150 }) => (
  <foreignObject x={60} y={y} width={W - 120} height={260}>
    <div
      style={{
        display: "flex",
        justifyContent: "center",
      }}
    >
      <div
        style={{
          background: "rgba(15,23,42,0.78)",
          color: "#FFF",
          fontFamily: "Inter",
          fontWeight: 700,
          fontSize: 52,
          lineHeight: 1.25,
          padding: "18px 34px",
          borderRadius: 26,
          textAlign: "center",
        }}
      >
        {text}
      </div>
    </div>
  </foreignObject>
);

/** A phone with an HTML screen. `w` is the screen width. */
export const Phone: React.FC<{
  x: number;
  y: number;
  w: number;
  rot?: number;
  children: React.ReactNode;
  glow?: string;
}> = ({ x, y, w, rot = 0, children, glow }) => {
  const h = w * (2400 / 1080);
  const b = w * 0.045;
  return (
    <g transform={`translate(${x} ${y}) rotate(${rot})`}>
      {glow && (
        <g>
          <filter id={`glow${Math.round(w)}`} x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation={w * 0.12} />
          </filter>
          <ellipse cx={w / 2} cy={h / 2} rx={w * 0.75} ry={h * 0.6} fill={glow} opacity={0.55} filter={`url(#glow${Math.round(w)})`} />
        </g>
      )}
      <rect x={-b} y={-b} width={w + 2 * b} height={h + 2 * b} rx={w * 0.12} fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
      <foreignObject x={0} y={0} width={w} height={h}>
        <div
          style={{
            width: w,
            height: h,
            borderRadius: w * 0.09,
            overflow: "hidden",
            position: "relative",
            fontFamily: "Inter, sans-serif",
            background: "#F4F5F8",
          }}
        >
          {children}
        </div>
      </foreignObject>
    </g>
  );
};

/** A mitten hand gripping a phone from below. */
export const GripHand: React.FC<{ x: number; y: number; s?: number; rot?: number; skin?: string; sleeve?: string }> = ({
  x,
  y,
  s = 1,
  rot = 0,
  skin = "#F2C3A2",
  sleeve = "#EFE6D8",
}) => (
  <g transform={`translate(${x} ${y}) rotate(${rot}) scale(${s})`}>
    <path d="M-70 120 L-80 320 L80 320 L70 120 Z" fill={sleeve} stroke={LINE} strokeWidth={4} />
    <path d="M-70 40 C-90 -10 -40 -40 0 -36 C40 -40 90 -10 70 40 L70 130 L-70 130 Z" fill={skin} stroke={LINE} strokeWidth={4} />
    {[0, 1, 2].map((i) => (
      <path key={i} d={`M-40 ${-6 + i * 34} Q-62 ${4 + i * 34} -40 ${16 + i * 34}`} stroke={LINE} strokeWidth={3} fill="none" />
    ))}
  </g>
);

/* ---------- Screens (HTML) ---------- */

// Screens are drawn larger than life so they stay readable in a storyboard panel.
const k0 = (w: number) => w / 470;

/** Lock screen with notifications; the reminder uses the app's real strings. */
export const LockNotifs: React.FC<{
  w: number;
  time: string;
  date: string;
  notifs: {
    title: string;
    text: string;
    reminder?: boolean;
    recording?: boolean;
    app?: string;
    /** Pill buttons; the last is the blue primary (Start, or Save then Pause), as in notification_quick_record.xml. */
    actions?: string[];
    /** Ongoing notifications (the recorder) don't show a time. */
    ongoing?: boolean;
    pressed?: number;
  }[];
}> = ({ w, time, date, notifs }) => {
  const k = k0(w);
  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: "linear-gradient(180deg,#1E2A5A 0%,#0B0F26 100%)",
        color: "#F8FAFC",
        padding: `${80 * k}px ${22 * k}px 0`,
      }}
    >
      <div style={{ textAlign: "center", fontSize: 30 * k, opacity: 0.85 }}>{date}</div>
      <div style={{ textAlign: "center", fontSize: 120 * k, fontWeight: 300, lineHeight: 1.05 }}>{time}</div>
      {notifs.map((n, i) => (
        <div
          key={i}
          style={{
            marginTop: 26 * k,
            background: "rgba(248,250,252,0.96)",
            color: "#0F172A",
            borderRadius: 34 * k,
            padding: `${22 * k}px ${26 * k}px`,
            boxShadow: n.reminder ? `0 0 0 ${6 * k}px rgba(251,191,36,0.55)` : undefined,
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 10 * k, fontSize: 20 * k, color: "#64748B" }}>
            <Img src={LOGO} style={{ width: 30 * k, height: 30 * k, borderRadius: 999 }} />
            {n.app ?? "Srutam"}
            {n.ongoing ? "" : " · now"}
          </div>
          <div style={{ fontSize: 30 * k, fontWeight: 800, marginTop: 8 * k }}>{n.title}</div>
          <div style={{ fontSize: 24 * k, color: "#334155" }}>{n.text}</div>
          {n.actions && (
            <div style={{ display: "flex", gap: 14 * k, marginTop: 14 * k }}>
              {n.actions.map((a, j) => (
                <div
                  key={a}
                  style={{
                    padding: `${10 * k}px ${26 * k}px`,
                    borderRadius: 999,
                    background: j === n.actions!.length - 1 ? C.cobalt : "#64748B",
                    color: "#FFF",
                    fontWeight: 700,
                    fontSize: 22 * k,
                    transform: n.pressed === j ? "scale(0.9)" : undefined,
                    boxShadow: n.pressed === j ? `0 0 0 ${8 * k}px rgba(37,99,235,0.35)` : undefined,
                  }}
                >
                  {a}
                </div>
              ))}
            </div>
          )}
          {n.reminder && (
            <div style={{ display: "flex", gap: 34 * k, marginTop: 14 * k, fontSize: 24 * k, fontWeight: 700, color: C.cobalt }}>
              <span>Mark Done</span>
              <span>Snooze 10m</span>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

/** Quick Settings shade with Srutam's "Voice Record" tile. */
export const QuickSettings: React.FC<{ w: number; recording?: boolean }> = ({ w, recording = false }) => {
  const k = k0(w);
  const tile = (label: string, on: boolean, icon: React.ReactNode, key: number) => (
    <div
      key={key}
      style={{
        borderRadius: 40 * k,
        padding: `${26 * k}px ${22 * k}px`,
        background: on ? "#C7D7FE" : "#2A3046",
        color: on ? "#0F172A" : "#E2E8F0",
        display: "flex",
        alignItems: "center",
        gap: 14 * k,
        fontSize: 24 * k,
        fontWeight: 700,
        boxShadow: on ? `0 0 0 ${6 * k}px rgba(37,99,235,0.45)` : undefined,
      }}
    >
      {icon}
      {label}
    </div>
  );
  return (
    <div style={{ position: "absolute", inset: 0, background: "#141826", padding: `${90 * k}px ${28 * k}px`, color: "#E2E8F0" }}>
      <div style={{ fontSize: 64 * k, fontWeight: 300 }}>11:24</div>
      <div style={{ fontSize: 24 * k, opacity: 0.7, marginBottom: 30 * k }}>Sun, 3 March</div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 18 * k }}>
        {tile("Wi-Fi", true, <span>📶</span>, 0)}
        {tile("Bluetooth", false, <span>🎧</span>, 1)}
        {tile(recording ? "Recording..." : "Voice Record", recording, <Img src={LOGO} style={{ width: 40 * k, height: 40 * k }} />, 2)}
        {tile("Torch", false, <span>🔦</span>, 3)}
        {tile("Do not disturb", false, <span>🌙</span>, 4)}
        {tile("Rotate", false, <span>🔄</span>, 5)}
      </div>
    </div>
  );
};

/** Ask Srutam AI: a question, the answer and "Referenced Notes:" pills. */
export const AskScreen: React.FC<{ w: number; q: string; a: string; refs: string[]; pressed?: boolean }> = ({ w, q, a, refs, pressed = false }) => {
  const k = k0(w);
  return (
    <div style={{ position: "absolute", inset: 0, background: "#050714", color: "#F8FAFC", padding: `${80 * k}px ${28 * k}px` }}>
      <div style={{ fontFamily: "Playfair Display", fontSize: 48 * k }}>
        Srutam <span style={{ fontStyle: "italic", fontWeight: 700, color: "#93C5FD" }}>AI</span>
      </div>
      <div style={{ fontSize: 20 * k, color: "#94A3B8", marginTop: 4 * k }}>Ask anything across all your voice notes and ideas</div>
      <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 40 * k, visibility: q ? "visible" : "hidden" }}>
        <div style={{ background: C.cobalt, borderRadius: `${30 * k}px ${30 * k}px ${8 * k}px ${30 * k}px`, padding: `${18 * k}px ${24 * k}px`, fontSize: 27 * k, maxWidth: "82%", lineHeight: 1.35 }}>
          {q}
        </div>
      </div>
      <div style={{ marginTop: 28 * k, background: "#0C1225", border: "1.5px solid #1A1F3A", borderRadius: 30 * k, padding: `${22 * k}px ${24 * k}px`, visibility: a ? "visible" : "hidden" }}>
        <div style={{ fontSize: 27 * k, lineHeight: 1.45 }}>{a}</div>
        <div style={{ fontSize: 19 * k, color: "#94A3B8", marginTop: 18 * k, fontWeight: 700, visibility: refs.length ? "visible" : "hidden" }}>Referenced Notes:</div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 10 * k, marginTop: 10 * k }}>
          {refs.map((r) => (
            <div
              key={r}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8 * k,
                background: pressed ? "#1D4ED8" : "#172554",
                boxShadow: pressed ? `0 0 0 ${8 * k}px rgba(96,165,250,0.4)` : undefined,
                color: "#BFDBFE",
                borderRadius: 999,
                padding: `${8 * k}px ${16 * k}px`,
                fontSize: 20 * k,
                fontWeight: 600,
              }}
            >
              <WaveIcon size={20 * k} color="#93C5FD" /> {r}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

type Insight = { kind: "IDEA" | "NEXT STEP" | "DECISION" | "REMINDER" | "KEY POINT"; text: string; meta?: string };

const STYLE: Record<Insight["kind"], { color: string; bg: string }> = {
  IDEA: { color: C.amber, bg: C.amberContainer },
  "NEXT STEP": { color: C.cobalt, bg: C.cobaltContainer },
  DECISION: { color: C.teal, bg: C.emeraldContainer },
  REMINDER: { color: C.cobalt, bg: C.cobaltContainer },
  "KEY POINT": { color: "#7C3AED", bg: "#F3E8FF" },
};

/** Insights for one note: title card, then Idea / Next Step / Decision / Reminder cards. */
/** `appear` (0..1 per item, may overshoot) springs each card in; omit to show all. */
export const InsightsScreen: React.FC<{ w: number; note: string; meta: string; items: Insight[]; appear?: number[] }> = ({ w, note, meta, items, appear }) => {
  const k = k0(w);
  return (
    <div style={{ position: "absolute", inset: 0, background: "#F4F5F8", color: "#0F172A", padding: `${70 * k}px ${28 * k}px` }}>
      <div style={{ fontFamily: "Playfair Display", fontSize: 50 * k }}>
        Srutam <span style={{ fontStyle: "italic", fontWeight: 700, color: C.cobalt }}>Insights</span>
      </div>
      <div style={{ display: "flex", alignItems: "center", gap: 8 * k, color: C.teal, fontWeight: 600, fontSize: 20 * k, marginTop: 4 * k }}>
        <LockIcon size={20 * k} color={C.teal} /> Transcribed on this device
      </div>
      <div style={{ marginTop: 18 * k, background: "#FFF", border: "1.5px solid #E2E8F0", borderRadius: 26 * k, padding: `${18 * k}px ${22 * k}px` }}>
        <div style={{ fontSize: 26 * k, fontWeight: 800 }}>{note}</div>
        <div style={{ fontSize: 18 * k, color: "#64748B" }}>{meta}</div>
      </div>
      {items.map((it, i) => (
        <div
          key={i}
          style={{
            marginTop: 16 * k,
            background: "#FFF",
            border: "1.5px solid #E2E8F0",
            borderRadius: 26 * k,
            padding: `${18 * k}px ${22 * k}px`,
            opacity: appear ? Math.min(1, Math.max(0, appear[i] ?? 0) * 1.6) : 1,
            transform: appear ? `translateY(${(1 - (appear[i] ?? 0)) * 40 * k}px) scale(${0.9 + 0.1 * (appear[i] ?? 0)})` : undefined,
            boxShadow: appear && (appear[i] ?? 0) > 0.05 && (appear[i] ?? 0) < 0.98 ? "0 12px 30px rgba(37,99,235,0.25)" : undefined,
          }}
        >
          <div
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 6 * k,
              padding: `${6 * k}px ${14 * k}px`,
              borderRadius: 999,
              background: STYLE[it.kind].bg,
              color: STYLE[it.kind].color,
              fontWeight: 800,
              fontSize: 17 * k,
              letterSpacing: 1,
            }}
          >
            {it.kind === "IDEA" && <BulbIcon size={16 * k} color={C.amber} />}
            {it.kind === "REMINDER" && <BellIcon size={16 * k} />}
            {it.kind}
          </div>
          <div style={{ fontSize: 26 * k, fontWeight: 700, marginTop: 8 * k, lineHeight: 1.3 }}>{it.text}</div>
          {it.meta && <div style={{ fontSize: 18 * k, color: "#64748B", marginTop: 6 * k }}>{it.meta}</div>}
        </div>
      ))}
    </div>
  );
};

/** A note playing back: title, waveform and the quoted line. */
export const PlaybackScreen: React.FC<{ w: number; title: string; meta: string; quote: string; progress?: number }> = ({
  w,
  title,
  meta,
  quote,
  progress = 0.62,
}) => {
  const k = k0(w);
  const bars = Array.from({ length: 34 }, (_, i) => 0.25 + Math.abs(Math.sin(i * 1.7) * Math.cos(i * 0.6)) * 0.75);
  return (
    <div style={{ position: "absolute", inset: 0, background: "#F4F5F8", color: "#0F172A", padding: `${80 * k}px ${30 * k}px` }}>
      <div style={{ fontSize: 34 * k, fontWeight: 800 }}>{title}</div>
      <div style={{ fontSize: 20 * k, color: "#64748B" }}>{meta}</div>
      <div style={{ display: "flex", alignItems: "center", gap: 5 * k, height: 120 * k, marginTop: 40 * k }}>
        {bars.map((b, i) => (
          <div
            key={i}
            style={{
              flex: 1,
              height: `${b * 100}%`,
              borderRadius: 99,
              background: i / bars.length < progress ? C.cobalt : "#CBD5E1",
            }}
          />
        ))}
      </div>
      <div style={{ marginTop: 40 * k, background: "#FFF", borderRadius: 26 * k, padding: `${22 * k}px ${24 * k}px`, border: "1.5px solid #E2E8F0", fontSize: 28 * k, lineHeight: 1.45, color: "#334155" }}>
        <span style={{ background: "#FEF3C7" }}>{quote}</span>
      </div>
    </div>
  );
};

/** An ordinary app (chat, video, notes) with Srutam's floating dock on the edge. */
export const DockOverApp: React.FC<{ w: number; recording?: boolean; label?: string }> = ({ w, recording = false, label = "Team chat" }) => {
  const k = k0(w);
  return (
    <div style={{ position: "absolute", inset: 0, background: "#FFFFFF", padding: `${80 * k}px ${28 * k}px`, color: "#0F172A" }}>
      <div style={{ fontSize: 34 * k, fontWeight: 800 }}>{label}</div>
      {[0.8, 0.55, 0.7, 0.45, 0.65].map((wd, i) => (
        <div key={i} style={{ display: "flex", justifyContent: i % 2 ? "flex-end" : "flex-start", marginTop: 22 * k }}>
          <div style={{ width: `${wd * 100}%`, height: 54 * k, borderRadius: 22 * k, background: i % 2 ? "#DBEAFE" : "#F1F5F9" }} />
        </div>
      ))}
      <div
        style={{
          position: "absolute",
          left: 0,
          top: "40%",
          display: "flex",
          alignItems: "center",
          gap: 10 * k,
          background: "#0F172A",
          borderRadius: `0 ${40 * k}px ${40 * k}px 0`,
          padding: `${14 * k}px ${20 * k}px ${14 * k}px ${16 * k}px`,
          boxShadow: `0 0 0 ${6 * k}px ${recording ? "rgba(239,68,68,0.35)" : "rgba(103,232,249,0.35)"}`,
        }}
      >
        <div
          style={{
            width: 56 * k,
            height: 56 * k,
            borderRadius: 999,
            background: recording ? C.recordCircle : C.cobalt,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <MicIcon size={30 * k} />
        </div>
        {recording && <div style={{ color: "#FFF", fontWeight: 700, fontSize: 24 * k }}>0:42</div>}
      </div>
    </div>
  );
};

/** End card: Srutam wordmark and a tagline. */
export const EndCard: React.FC<{ tagline: string[]; sub?: string }> = ({ tagline, sub = "" }) => (
  <g>
    <text x={W / 2} y={820} textAnchor="middle" fontFamily="Playfair Display" fontSize={170} fill="#F8FAFC">
      Srutam
    </text>
    <text x={W / 2} y={960} textAnchor="middle" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={70} fill="#93C5FD">
      {tagline.map((l, i) => (
        <tspan key={i} x={W / 2} dy={i ? 86 : 0}>
          {l}
        </tspan>
      ))}
    </text>
    <text x={W / 2} y={1500} textAnchor="middle" fontFamily="Inter" fontWeight={600} fontSize={44} fill="#94A3B8">
      {sub}
    </text>
  </g>
);

/* ---------- Set props ---------- */

export const Window: React.FC<{ x: number; y: number; w: number; h: number; sky: [string, string]; moon?: boolean }> = ({
  x,
  y,
  w,
  h,
  sky,
  moon = false,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <g>
      <defs>
        <linearGradient id={`sky${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={sky[0]} />
          <stop offset="100%" stopColor={sky[1]} />
        </linearGradient>
      </defs>
      <rect x={x} y={y} width={w} height={h} fill={`url(#sky${id})`} stroke={LINE} strokeWidth={8} />
      {moon && <circle cx={x + w * 0.7} cy={y + h * 0.28} r={w * 0.09} fill="#FEF3C7" />}
      <path d={`M${x + w / 2} ${y} L${x + w / 2} ${y + h} M${x} ${y + h / 2} L${x + w} ${y + h / 2}`} stroke={LINE} strokeWidth={8} />
    </g>
  );
};

export const Table: React.FC<{ y: number; color?: string; top?: string }> = ({ y, color = "#8B5E3C", top = "#A9744D" }) => (
  <g>
    <rect x={-20} y={y} width={W + 40} height={H - y + 20} fill={color} />
    <rect x={-20} y={y} width={W + 40} height={40} fill={top} stroke={LINE} strokeWidth={5} />
  </g>
);

export const Diya: React.FC<{ x: number; y: number; s?: number }> = ({ x, y, s = 1 }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    <ellipse cx={0} cy={-46} rx={34} ry={60} fill="#FBBF24" opacity={0.25} />
    <path d="M0 -70 C14 -48 12 -30 0 -22 C-12 -30 -14 -48 0 -70 Z" fill="#FDE68A" stroke="#F59E0B" strokeWidth={3} />
    <path d="M-50 -10 C-40 20 40 20 50 -10 Z" fill="#C2410C" stroke={LINE} strokeWidth={4} />
  </g>
);

export const Kadhai: React.FC<{ x: number; y: number; s?: number; steam?: boolean }> = ({ x, y, s = 1, steam = true }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    {steam &&
      [0, 1, 2].map((i) => (
        <path
          key={i}
          d={`M${-60 + i * 60} -40 q-24 -50 0 -100 q24 -50 0 -100`}
          stroke="#FFFFFF"
          strokeWidth={10}
          fill="none"
          opacity={0.5}
          strokeLinecap="round"
        />
      ))}
    <path d="M-180 -20 C-160 80 160 80 180 -20 Z" fill="#3F3A3A" stroke={LINE} strokeWidth={5} />
    <ellipse cx={0} cy={-20} rx={180} ry={30} fill="#C9862F" stroke={LINE} strokeWidth={5} />
    <path d="M-180 -20 q-50 -10 -60 20 M180 -20 q50 -10 60 20" stroke="#3F3A3A" strokeWidth={14} fill="none" strokeLinecap="round" />
  </g>
);

export const Jar: React.FC<{ x: number; y: number; label: string; fill?: string; s?: number }> = ({ x, y, label, fill = "#C08A3E", s = 1 }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    <rect x={-70} y={-170} width={140} height={170} rx={30} fill={fill} stroke={LINE} strokeWidth={5} />
    <rect x={-58} y={-196} width={116} height={34} rx={10} fill="#B91C1C" stroke={LINE} strokeWidth={5} />
    <rect x={-50} y={-120} width={100} height={60} rx={8} fill="#FFFDF8" stroke={LINE} strokeWidth={3} />
    <text x={0} y={-80} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={34} fill="#2B1E1B">
      {label}
    </text>
  </g>
);

export const Door: React.FC<{ x: number; y: number; w: number; h: number; color?: string }> = ({ x, y, w, h, color = "#9A6B44" }) => (
  <g>
    <rect x={x} y={y} width={w} height={h} fill={color} stroke={LINE} strokeWidth={8} />
    {[0, 1].map((r) =>
      [0, 1].map((c) => (
        <rect
          key={`${r}${c}`}
          x={x + w * 0.1 + c * w * 0.42}
          y={y + h * 0.08 + r * h * 0.46}
          width={w * 0.38}
          height={h * 0.38}
          fill="none"
          stroke="#6B4528"
          strokeWidth={6}
        />
      )),
    )}
    <circle cx={x + w * 0.88} cy={y + h * 0.52} r={14} fill="#FBBF24" stroke={LINE} strokeWidth={4} />
  </g>
);

export const Clock: React.FC<{ x: number; y: number; text: string; color?: string; size?: number }> = ({
  x,
  y,
  text,
  color = "#EF4444",
  size = 90,
}) => (
  <g>
    <rect x={x - size * 2.1} y={y - size * 0.95} width={size * 4.2} height={size * 1.3} rx={22} fill="#0F172A" stroke={LINE} strokeWidth={5} />
    <text x={x} y={y} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={size} fill={color}>
      {text}
    </text>
  </g>
);

/** A slim end band across the bottom of a panel: wordmark and tagline. */
export const EndBand: React.FC<{ tagline: string; y?: number }> = ({ tagline, y = 1560 }) => (
  <g>
    <rect x={0} y={y} width={W} height={H - y} fill="#050714" />
    <text x={W / 2} y={y + 160} textAnchor="middle" fontFamily="Playfair Display" fontSize={120} fill="#F8FAFC">
      Srutam
    </text>
    <text x={W / 2} y={y + 260} textAnchor="middle" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={58} fill="#93C5FD">
      {tagline}
    </text>
  </g>
);

/** A plate of round laddoos. */
export const Laddoos: React.FC<{ x: number; y: number; s?: number }> = ({ x, y, s = 1 }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    <ellipse cx={0} cy={40} rx={230} ry={56} fill="#E5E7EB" stroke={LINE} strokeWidth={5} />
    {[
      [-120, 0], [-40, 6], [40, 4], [120, 0], [-80, -60], [0, -58], [80, -60], [-40, -116], [40, -114],
    ].map(([cx, cy], i) => (
      <g key={i}>
        <circle cx={cx} cy={cy} r={46} fill="#E8A33D" stroke={LINE} strokeWidth={4} />
        <circle cx={cx - 14} cy={cy - 16} r={10} fill="#FCD9A0" opacity={0.8} />
      </g>
    ))}
  </g>
);

/** A desk lamp with a warm cone of light. */
export const Lamp: React.FC<{ x: number; y: number }> = ({ x, y }) => (
  <g transform={`translate(${x} ${y})`}>
    <defs>
      <linearGradient id="lampCone" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#FDE68A" stopOpacity={0.75} />
        <stop offset="100%" stopColor="#FDE68A" stopOpacity={0.05} />
      </linearGradient>
    </defs>
    <path d="M60 60 L-300 1000 L440 1000 Z" fill="url(#lampCone)" />
    <path d="M0 0 L120 0 L160 80 L-40 80 Z" fill="#F59E0B" stroke={LINE} strokeWidth={5} />
    <ellipse cx={60} cy={82} rx={60} ry={12} fill="#FEF3C7" />
    <path d="M60 0 L20 -260 L60 -280" stroke="#334155" strokeWidth={16} fill="none" strokeLinecap="round" />
  </g>
);

/** A stack of books. */
export const Books: React.FC<{ x: number; y: number; n?: number }> = ({ x, y, n = 5 }) => {
  const colors = ["#B91C1C", "#1D4ED8", "#047857", "#B45309", "#6D28D9", "#0F766E"];
  return (
    <g transform={`translate(${x} ${y})`}>
      {Array.from({ length: n }, (_, i) => (
        <rect key={i} x={-150 + (i % 2) * 18} y={-i * 52 - 52} width={300 - (i % 3) * 20} height={50} rx={6} fill={colors[i % colors.length]} stroke={LINE} strokeWidth={4} />
      ))}
    </g>
  );
};

/** Blackboard or whiteboard with handwritten lines. */
export const Board: React.FC<{ x: number; y: number; w: number; h: number; lines: string[]; chalk?: boolean }> = ({ x, y, w, h, lines, chalk = false }) => (
  <g>
    <rect x={x} y={y} width={w} height={h} rx={12} fill={chalk ? "#1F3B2D" : "#F8FAFC"} stroke={chalk ? "#7C5A3A" : "#94A3B8"} strokeWidth={14} />
    <text x={x + 50} y={y + 110} fontFamily="Kalam" fontWeight={700} fontSize={70} fill={chalk ? "#F1F5F9" : "#1E3A8A"}>
      {lines.map((l, i) => (
        <tspan key={i} x={x + 50} dy={i ? 92 : 0}>
          {l}
        </tspan>
      ))}
    </text>
  </g>
);

/** Little sweat drops for a comic panic. */
export const Sweat: React.FC<{ x: number; y: number; s?: number }> = ({ x, y, s = 1 }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`} fill="#93C5FD" stroke={LINE} strokeWidth={3}>
    <path d="M0 0 C14 20 14 34 0 38 C-14 34 -14 20 0 0 Z" />
    <path d="M40 30 C50 44 50 54 40 57 C30 54 30 44 40 30 Z" />
  </g>
);

/**
 * The pulled-down notification shade with Srutam's ongoing recording notification
 * (QuickRecordNotification + notification_quick_record.xml): idle "Srutam / Ready to
 * record" with Start; recording "Recording" with a running timer, Save and Pause.
 */
export const NotificationShade: React.FC<{
  w: number;
  recording: boolean;
  pressed?: boolean;
  secs?: number;
  time?: string;
  date?: string;
  /** Another notification under Srutam's (app, title, text); null for none. */
  other?: { app: string; title: string; text: string } | null;
}> = ({ w, recording, pressed = false, secs = 0, time = "11:24", date = "Sun, 3 March", other = { app: "Messages · 2m", title: "Riya", text: "Send laddoo pics later 😋" } }) => {
  const k = k0(w);
  const pill = (label: string, primary: boolean, down: boolean) => (
    <div
      key={label}
      style={{
        height: 46 * k,
        padding: `0 ${(primary ? 24 : 19) * k}px`,
        borderRadius: 999,
        background: primary ? C.cobalt : "#64748B",
        color: "#FFF",
        fontWeight: 700,
        fontSize: 19 * k,
        display: "flex",
        alignItems: "center",
        transform: down ? "scale(0.9)" : undefined,
        boxShadow: down ? `0 0 0 ${9 * k}px rgba(96,165,250,0.45)` : undefined,
      }}
    >
      {label}
    </div>
  );
  const tile = (icon: string, on: boolean, i: number) => (
    <div
      key={i}
      style={{
        width: 62 * k,
        height: 62 * k,
        borderRadius: 999,
        background: on ? "#C7D7FE" : "#2A3046",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: 26 * k,
      }}
    >
      {icon}
    </div>
  );
  const card: React.CSSProperties = {
    background: "#232838",
    borderRadius: 30 * k,
    padding: `${18 * k}px ${22 * k}px`,
    marginTop: 14 * k,
    color: "#E2E8F0",
  };
  return (
    <div style={{ position: "absolute", inset: 0, background: "#11141D", padding: `${70 * k}px ${22 * k}px`, color: "#E2E8F0" }}>
      <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between" }}>
        <div style={{ fontSize: 58 * k, fontWeight: 300 }}>{time}</div>
        <div style={{ fontSize: 22 * k, opacity: 0.7 }}>{date}</div>
      </div>
      <div style={{ display: "flex", justifyContent: "space-between", margin: `${22 * k}px 0 ${14 * k}px` }}>
        {["📶", "🎧", "🔦", "🌙", "🔄"].map((ic, i) => tile(ic, i === 0, i))}
      </div>
      <div style={{ ...card, boxShadow: recording ? `0 0 0 ${4 * k}px rgba(239,68,68,0.5)` : undefined }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 * k, fontSize: 19 * k, color: "#94A3B8" }}>
          <Img src={LOGO} style={{ width: 28 * k, height: 28 * k, borderRadius: 999 }} />
          Srutam
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12 * k, marginTop: 10 * k }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 26 * k, fontWeight: 700, color: "#F8FAFC" }}>{recording ? "Recording" : "Srutam"}</div>
            <div style={{ fontSize: 21 * k, color: recording ? "#FCA5A5" : "#94A3B8", fontVariantNumeric: "tabular-nums" }}>
              {recording ? `0:${String(secs).padStart(2, "0")}` : "Ready to record"}
            </div>
          </div>
          {recording ? [pill("Save", false, false), pill("Pause", true, false)] : pill("Start", true, pressed)}
        </div>
      </div>
      {other && (
        <div style={card}>
        <div style={{ fontSize: 19 * k, color: "#94A3B8" }}>{other?.app}</div>
        <div style={{ fontSize: 24 * k, fontWeight: 700, color: "#F8FAFC", marginTop: 6 * k }}>{other?.title}</div>
        <div style={{ fontSize: 21 * k, color: "#CBD5E1" }}>{other?.text}</div>
      </div>
      )}
      <div style={{ display: "flex", justifyContent: "flex-end", gap: 24 * k, marginTop: 18 * k, fontSize: 20 * k, color: "#93C5FD" }}>
        <span>Manage</span>
        <span>Clear all</span>
      </div>
    </div>
  );
};

/** A group chat (a generic messenger, no real brand): messages spring in as `appear` goes 0 -> 1. */
export const ChatScreen: React.FC<{
  w: number;
  title: string;
  members: string;
  messages: { from: string; text: string; me?: boolean; color?: string }[];
  appear: number[];
  typing?: string;
}> = ({ w, title, members, messages, appear, typing }) => {
  const k = k0(w);
  return (
    <div style={{ position: "absolute", inset: 0, background: "#EEF1F6", color: "#0F172A", display: "flex", flexDirection: "column" }}>
      <div style={{ background: "#FFFFFF", padding: `${70 * k}px ${26 * k}px ${18 * k}px`, borderBottom: "1.5px solid #E2E8F0", display: "flex", alignItems: "center", gap: 14 * k }}>
        <div style={{ width: 54 * k, height: 54 * k, borderRadius: 99, background: "#C7D2FE", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 800, fontSize: 22 * k, color: "#3730A3" }}>
          {title.slice(0, 1)}
        </div>
        <div>
          <div style={{ fontWeight: 800, fontSize: 28 * k }}>{title}</div>
          <div style={{ fontSize: 18 * k, color: "#64748B" }}>{members}</div>
        </div>
      </div>
      <div style={{ flex: 1, padding: `${20 * k}px ${22 * k}px`, display: "flex", flexDirection: "column", justifyContent: "flex-end", gap: 14 * k }}>
        {messages.map((m, i) => {
          const a = Math.max(0, Math.min(1.2, appear[i] ?? 0));
          if (a <= 0.001) return null;
          return (
            <div key={i} style={{ display: "flex", justifyContent: m.me ? "flex-end" : "flex-start", opacity: Math.min(1, a * 1.5), transform: `translateY(${(1 - a) * 30 * k}px) scale(${0.85 + 0.15 * a})`, transformOrigin: m.me ? "right bottom" : "left bottom" }}>
              <div
                style={{
                  maxWidth: "80%",
                  background: m.me ? C.cobalt : "#FFFFFF",
                  color: m.me ? "#FFFFFF" : "#0F172A",
                  borderRadius: m.me ? `${26 * k}px ${26 * k}px ${8 * k}px ${26 * k}px` : `${26 * k}px ${26 * k}px ${26 * k}px ${8 * k}px`,
                  padding: `${12 * k}px ${20 * k}px`,
                  fontSize: 25 * k,
                  lineHeight: 1.3,
                  boxShadow: "0 1px 2px rgba(15,23,42,0.08)",
                }}
              >
                {!m.me && <div style={{ fontSize: 18 * k, fontWeight: 800, color: m.color ?? "#7C3AED", marginBottom: 2 * k }}>{m.from}</div>}
                {m.text}
              </div>
            </div>
          );
        })}
        {typing && <div style={{ fontSize: 19 * k, color: "#64748B" }}>{typing} is typing…</div>}
      </div>
      <div style={{ background: "#FFFFFF", margin: `0 ${16 * k}px ${40 * k}px`, borderRadius: 99, height: 64 * k, border: "1.5px solid #E2E8F0", display: "flex", alignItems: "center", padding: `0 ${22 * k}px`, color: "#94A3B8", fontSize: 22 * k }}>
        Message
      </div>
    </div>
  );
};

const KEYS = ["qwertyuiop", "asdfghjkl", "zxcvbnm"];

/** A phone keyboard; `down` is the key under the thumb right now. */
const Keyboard: React.FC<{ k: number; down?: string }> = ({ k, down }) => (
  <div style={{ background: "#0F1426", padding: `${14 * k}px ${8 * k}px ${44 * k}px`, display: "flex", flexDirection: "column", gap: 10 * k }}>
    {KEYS.map((row, r) => (
      <div key={r} style={{ display: "flex", justifyContent: "center", gap: 7 * k }}>
        {row.split("").map((c) => (
          <div
            key={c}
            style={{
              width: 38 * k,
              height: 52 * k,
              borderRadius: 8 * k,
              background: down === c ? "#475569" : "#1E2539",
              transform: down === c ? "translateY(-4px) scale(1.15)" : undefined,
              color: "#E2E8F0",
              fontSize: 22 * k,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {c}
          </div>
        ))}
      </div>
    ))}
    <div style={{ display: "flex", justifyContent: "center", gap: 7 * k }}>
      <div style={{ width: 300 * k, height: 52 * k, borderRadius: 8 * k, background: down === " " ? "#475569" : "#1E2539" }} />
    </div>
  </div>
);

/**
 * Ask Srutam AI as the app draws it (GlobalCopilotScreen): the conversation, a
 * "Thinking..." state, answers with "Referenced Notes:" pills, and the input bar
 * ("Ask, or tell me what to set up...") with the send button and the keyboard.
 */
export const AskFlow: React.FC<{
  w: number;
  sent?: string;
  thinking?: boolean;
  answer?: string;
  refs?: string[];
  pressedRef?: boolean;
  input?: string;
  caret?: boolean;
  keyboard?: number;
  keyDown?: string;
  sendDown?: boolean;
}> = ({ w, sent, thinking = false, answer, refs = [], pressedRef = false, input = "", caret = false, keyboard = 0, keyDown, sendDown = false }) => {
  const k = k0(w);
  const canSend = input.trim().length > 0;
  return (
    <div style={{ position: "absolute", inset: 0, background: "#050714", color: "#F8FAFC", display: "flex", flexDirection: "column" }}>
      <div style={{ padding: `${80 * k}px ${28 * k}px 0` }}>
        <div style={{ fontFamily: "Playfair Display", fontSize: 44 * k }}>
          Srutam <span style={{ fontStyle: "italic", fontWeight: 700, color: "#93C5FD" }}>AI</span>
        </div>
        <div style={{ fontSize: 19 * k, color: "#94A3B8", marginTop: 4 * k, lineHeight: 1.35 }}>I can search your voice notes and help you set reminders, add next steps, and save ideas.</div>
      </div>
      <div style={{ flex: 1, padding: `${20 * k}px ${28 * k}px`, display: "flex", flexDirection: "column", justifyContent: "flex-end", gap: 22 * k }}>
        {sent && (
          <div style={{ display: "flex", justifyContent: "flex-end" }}>
            <div style={{ background: C.cobalt, borderRadius: `${28 * k}px ${28 * k}px ${8 * k}px ${28 * k}px`, padding: `${16 * k}px ${22 * k}px`, fontSize: 25 * k, maxWidth: "82%", lineHeight: 1.35 }}>{sent}</div>
          </div>
        )}
        {thinking && <div style={{ fontSize: 22 * k, color: "#94A3B8" }}>✦ Thinking...</div>}
        {answer && (
          <div style={{ background: "#0C1225", border: "1.5px solid #1A1F3A", borderRadius: 28 * k, padding: `${20 * k}px ${22 * k}px` }}>
            <div style={{ fontSize: 18 * k, color: "#93C5FD", fontWeight: 700, marginBottom: 8 * k }}>✦ Srutam AI</div>
            <div style={{ fontSize: 25 * k, lineHeight: 1.45 }}>{answer}</div>
            {refs.length > 0 && (
              <>
                <div style={{ fontSize: 18 * k, color: "#94A3B8", marginTop: 16 * k, fontWeight: 700 }}>Referenced Notes:</div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 10 * k, marginTop: 10 * k }}>
                  {refs.map((r) => (
                    <div
                      key={r}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 8 * k,
                        background: pressedRef ? "#1D4ED8" : "#172554",
                        boxShadow: pressedRef ? `0 0 0 ${8 * k}px rgba(96,165,250,0.4)` : undefined,
                        color: "#BFDBFE",
                        borderRadius: 999,
                        padding: `${8 * k}px ${16 * k}px`,
                        fontSize: 20 * k,
                        fontWeight: 600,
                      }}
                    >
                      📄 {r}
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </div>
      <div style={{ display: "flex", alignItems: "flex-end", gap: 12 * k, margin: `0 ${18 * k}px ${keyboard > 0 ? 12 * k : 60 * k}px`, background: "#0C1225", border: "1.5px solid #1A1F3A", borderRadius: 26 * k, padding: `${14 * k}px ${14 * k}px ${14 * k}px ${22 * k}px` }}>
        <div style={{ flex: 1, fontSize: 23 * k, lineHeight: 1.35, color: input ? "#F8FAFC" : "#64748B", minHeight: 34 * k }}>
          {input || "Ask, or tell me what to set up..."}
          {caret && <span style={{ color: "#60A5FA" }}>|</span>}
        </div>
        <div
          style={{
            width: 52 * k,
            height: 52 * k,
            borderRadius: 999,
            background: canSend ? C.cobalt : "#1E2539",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            transform: sendDown ? "scale(0.88)" : undefined,
            boxShadow: sendDown ? `0 0 0 ${8 * k}px rgba(96,165,250,0.4)` : undefined,
            fontSize: 24 * k,
          }}
        >
          ➤
        </div>
      </div>
      {keyboard > 0 && (
        <div style={{ transform: `translateY(${(1 - keyboard) * 100}%)` }}>
          <Keyboard k={k} down={keyDown} />
        </div>
      )}
    </div>
  );
};

/**
 * Note Details as the app draws it (DetailScreen + AudioPlayerCard): the note title,
 * the round play button, the tap-to-seek waveform, the timecode and speed pills,
 * then the Summary / Transcript / Insights tabs with the transcript showing.
 */
export const NoteDetail: React.FC<{
  w: number;
  title: string;
  meta: string;
  progress: number;
  playing: boolean;
  time: string;
  total: string;
  tap?: number;
  transcript: string;
}> = ({ w, title, meta, progress, playing, time, total, tap, transcript }) => {
  const k = k0(w);
  const bars = Array.from({ length: 44 }, (_, i) => 0.22 + Math.abs(Math.sin(i * 1.7) * Math.cos(i * 0.55)) * 0.78);
  return (
    <div style={{ position: "absolute", inset: 0, background: "#050714", color: "#F8FAFC", padding: `${76 * k}px ${24 * k}px` }}>
      <div style={{ fontSize: 20 * k, color: "#94A3B8", fontWeight: 700 }}>‹ Note Details</div>
      <div style={{ fontSize: 34 * k, fontWeight: 800, marginTop: 18 * k }}>{title}</div>
      <div style={{ fontSize: 18 * k, color: "#94A3B8", marginTop: 4 * k }}>{meta}</div>
      <div style={{ marginTop: 24 * k, background: "#0C1225", border: "1px solid #1A1F3A", borderRadius: 28 * k, padding: `${22 * k}px ${20 * k}px` }}>
        <div style={{ display: "flex", alignItems: "center", gap: 16 * k }}>
          <div style={{ width: 70 * k, height: 70 * k, borderRadius: 999, background: C.cobalt, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 28 * k, flexShrink: 0 }}>
            {playing ? "❚❚" : "▶"}
          </div>
          <div style={{ flex: 1, position: "relative", display: "flex", alignItems: "center", gap: 3 * k, height: 64 * k }}>
            {bars.map((b, i) => (
              <div key={i} style={{ flex: 1, height: `${b * 100}%`, borderRadius: 99, background: i / bars.length < progress ? "#60A5FA" : "#334155" }} />
            ))}
            {tap !== undefined && tap > 0 && tap < 1 && (
              <div style={{ position: "absolute", left: `${progress * 100}%`, top: "50%", width: 24 * k + tap * 70 * k, height: 24 * k + tap * 70 * k, borderRadius: 999, border: `${3 * k}px solid rgba(255,255,255,${1 - tap})`, transform: "translate(-50%, -50%)" }} />
            )}
          </div>
          <div style={{ textAlign: "right" }}>
            <div style={{ fontSize: 20 * k, fontWeight: 800, color: playing ? "#60A5FA" : "#F8FAFC" }}>{time}</div>
            <div style={{ fontSize: 15 * k, color: "#64748B" }}>{total}</div>
          </div>
        </div>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 18 * k }}>
          <div style={{ fontSize: 17 * k, color: "#94A3B8" }}>Playback Speed</div>
          <div style={{ display: "flex", gap: 8 * k }}>
            {["1.0x", "1.25x", "1.5x", "2.0x"].map((s, i) => (
              <div key={s} style={{ fontSize: 15 * k, fontWeight: 700, borderRadius: 99, padding: `${5 * k}px ${11 * k}px`, background: i === 0 ? C.cobalt : "#1E2539", color: i === 0 ? "#FFF" : "#94A3B8" }}>
                {s}
              </div>
            ))}
          </div>
        </div>
      </div>
      <div style={{ display: "flex", gap: 10 * k, marginTop: 24 * k }}>
        {["✦ Summary", "📄 Transcript", "💡 Insights"].map((t, i) => (
          <div key={t} style={{ fontSize: 18 * k, fontWeight: 700, borderRadius: 99, padding: `${9 * k}px ${16 * k}px`, background: i === 1 ? "#172554" : "transparent", color: i === 1 ? "#BFDBFE" : "#94A3B8", border: "1px solid #1A1F3A" }}>
            {t}
          </div>
        ))}
      </div>
      <div style={{ marginTop: 18 * k, background: "#0C1225", border: "1px solid #1A1F3A", borderRadius: 24 * k, padding: `${18 * k}px ${20 * k}px` }}>
        <div style={{ fontSize: 18 * k, fontWeight: 800, color: "#94A3B8" }}>Full Transcript</div>
        <div style={{ fontSize: 21 * k, lineHeight: 1.5, color: "#CBD5E1", marginTop: 8 * k }}>{transcript}</div>
      </div>
    </div>
  );
};
