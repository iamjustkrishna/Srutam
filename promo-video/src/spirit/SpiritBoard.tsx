import { AbsoluteFill } from "remotion";
import { FONT } from "../theme";
import type { MeeraExpr } from "./Meera";
import { MeeraHD as Meera } from "./MeeraHD";
import {
  FrameCaught,
  FrameEye,
  FrameFade,
  FrameLeap,
  FrameNight,
  FrameWake,
  H,
  KeyVisual,
  W,
} from "./Scenes";
import { Firefly, Shru } from "./Shru";

const INK = "#1A1433";
const PAPER = "#F6F1EA";

const FRAMES: { Art: React.FC; shot: string; title: string; note: string }[] = [
  {
    Art: FrameEye,
    shot: "01 · ECU",
    title: "A thought arrives",
    note: "Meera's eye. The firefly glints in her iris. Slow push-in.",
  },
  {
    Art: FrameFade,
    shot: "02 · CU",
    title: "It starts to fade",
    note: "Gold drains to grey, embers fall on 2s. The ring runs out.",
  },
  {
    Art: FrameWake,
    shot: "03 · INSERT",
    title: "Shru wakes",
    note: "In the floating dock, two ears and gold eyes open. A beat of silence.",
  },
  {
    Art: FrameLeap,
    shot: "04 · ACTION",
    title: "The leap",
    note: "Tap, tap: Shru bursts out. Speed lines, smears, tails crack like a whip.",
  },
  {
    Art: FrameCaught,
    shot: "05 · MS",
    title: "Caught",
    note: "The firefly burns bright again; its words unspool along Shru's tail.",
  },
  {
    Art: FrameNight,
    shot: "06 · WS",
    title: "Every thought, kept",
    note: "Night. The ideas form a constellation; Shru sleeps on the phone.",
  },
];

const Label: React.FC<{ x: number; y: number; big: string; small: string }> = ({
  x,
  y,
  big,
  small,
}) => (
  <g>
    <text
      x={x}
      y={y}
      fontFamily="Inter"
      fontWeight={800}
      fontSize={44}
      letterSpacing={6}
      fill="#FFFFFF"
    >
      {big}
    </text>
    <text
      x={x}
      y={y + 40}
      fontFamily="Playfair Display"
      fontStyle="italic"
      fontSize={30}
      fill="#C4B5FD"
    >
      {small}
    </text>
  </g>
);

const Note: React.FC<{ x: number; y: number; lines: string[] }> = ({
  x,
  y,
  lines,
}) => (
  <text x={x} y={y} fontFamily="Inter" fontSize={24} fill="#DDD6FE">
    {lines.map((l, i) => (
      <tspan key={l} x={x} dy={i ? 34 : 0}>
        {l}
      </tspan>
    ))}
  </text>
);

const CharacterSheet: React.FC = () => (
  <svg
    viewBox="0 0 1400 1600"
    width={1400}
    height={1600}
    style={{ display: "block", borderRadius: 28 }}
  >
    <defs>
      <linearGradient id="csBg" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#1E1640" />
        <stop offset="100%" stopColor="#0E0A24" />
      </linearGradient>
      <linearGradient id="csWarm" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="#3B2A6A" />
        <stop offset="100%" stopColor="#7A3D6E" />
      </linearGradient>
    </defs>
    <rect width={1400} height={1600} fill="url(#csBg)" />
    {Array.from({ length: 26 }, (_, i) => (
      <line
        key={i}
        x1={0}
        y1={i * 64}
        x2={1400}
        y2={i * 64}
        stroke="#FFFFFF"
        strokeOpacity={0.03}
      />
    ))}

    {/* SHRU */}
    <Label
      x={50}
      y={80}
      big="SHRU"
      small="the listening spirit · lives in Srutam's floating dock"
    />
    <Shru x={10} y={170} s={0.95} pose="sit" />
    <Shru x={600} y={140} s={0.8} pose="leap" eyes="fierce" phase={2} />
    <Shru x={760} y={420} s={0.62} pose="curl" />
    <text
      fontFamily="Inter"
      fontWeight={700}
      fontSize={20}
      fill="#93C5FD"
      letterSpacing={3}
    >
      <tspan x={230} y={600}>
        IDLE
      </tspan>
      <tspan x={860} y={410}>
        LEAP
      </tspan>
      <tspan x={1210} y={620}>
        ASLEEP IN THE DOCK
      </tspan>
    </text>
    <Note
      x={60}
      y={680}
      lines={[
        "Its tails are sound waves; they ripple when anyone speaks.",
        "Cobalt body = Srutam. Gold markings = the spark it protects.",
        "Glows brighter the moment it catches a thought.",
      ]}
    />

    {/* MEERA */}
    <rect
      x={30}
      y={810}
      width={1340}
      height={560}
      rx={28}
      fill="url(#csWarm)"
      opacity={0.55}
    />
    <Label
      x={60}
      y={880}
      big="MEERA"
      small="26 · product designer · Mumbai · always mid-idea"
    />
    <Meera x={30} y={900} s={0.52} expr="awe" />
    {(["focus", "smile"] as MeeraExpr[]).map((e, i) => (
      <g key={e}>
        <Meera
          x={520 + i * 420}
          y={940}
          s={0.46}
          expr={e}
          light={e === "smile" ? "night" : "sunset"}
        />
        <text
          x={700 + i * 420}
          y={1356}
          textAnchor="middle"
          fontFamily="Inter"
          fontWeight={700}
          fontSize={20}
          fill="#E9D5FF"
          letterSpacing={3}
        >
          {e === "focus"
            ? "FOCUSED (SUNSET KEY LIGHT)"
            : "AT PEACE (PHONE GLOW)"}
        </text>
      </g>
    ))}
    <text
      x={250}
      y={1356}
      textAnchor="middle"
      fontFamily="Inter"
      fontWeight={700}
      fontSize={20}
      fill="#E9D5FF"
      letterSpacing={3}
    >
      AWE
    </text>

    {/* THE THOUGHTS */}
    <Label
      x={50}
      y={1440}
      big="THE THOUGHTS"
      small="fireflies: bright, fading, ash"
    />
    <Firefly x={820} y={1470} r={24} life={1} />
    <Firefly x={1000} y={1470} r={24} life={0.45} />
    <Firefly x={1180} y={1470} r={24} life={0.1} />
    {[
      ["#3B82F6", "Shru"],
      ["#FBBF24", "Spark"],
      ["#FB923C", "Golden hour"],
      ["#E26AA8", "Dusk"],
      ["#1A1433", "Night"],
    ].map(([c, n], i) => (
      <g key={n}>
        <rect
          x={60 + i * 150}
          y={1500}
          width={120}
          height={50}
          rx={12}
          fill={c}
          stroke="#FFFFFF"
          strokeOpacity={0.4}
          strokeWidth={2}
        />
        <text
          x={120 + i * 150}
          y={1580}
          textAnchor="middle"
          fontFamily="Inter"
          fontSize={18}
          fill="#DDD6FE"
        >
          {n}
        </text>
      </g>
    ))}
  </svg>
);

export const SpiritBoard: React.FC = () => (
  <AbsoluteFill
    style={{
      background: PAPER,
      padding: 60,
      fontFamily: FONT.sans,
      color: INK,
    }}
  >
    <div
      style={{
        height: 170,
        display: "flex",
        justifyContent: "space-between",
        alignItems: "flex-end",
      }}
    >
      <div>
        <div
          style={{
            fontSize: 26,
            fontWeight: 700,
            letterSpacing: 8,
            color: "#7C6FA8",
          }}
        >
          SRUTAM · CHARACTER &amp; STORY BOARD
        </div>
        <div style={{ fontFamily: FONT.serif, fontSize: 96, lineHeight: 1.1 }}>
          Shru,{" "}
          <span style={{ fontStyle: "italic", color: "#2563EB" }}>
            the listening spirit
          </span>
        </div>
      </div>
      <div
        style={{
          fontSize: 24,
          color: "#4B4470",
          maxWidth: 760,
          textAlign: "right",
          lineHeight: 1.4,
        }}
      >
        Every idea is a firefly that fades in seconds. Shru lives in Srutam's
        floating dock, and when you tap, it leaps out and catches the thought
        before it's gone.
      </div>
    </div>
    <div style={{ display: "flex", gap: 40, marginTop: 30 }}>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        width={900}
        height={1600}
        style={{
          display: "block",
          borderRadius: 28,
          boxShadow: "0 30px 60px rgba(26,20,51,0.35)",
        }}
      >
        <KeyVisual />
      </svg>
      <CharacterSheet />
    </div>
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(6, 370px)",
        gap: 28,
        marginTop: 50,
      }}
    >
      {FRAMES.map(({ Art, shot, title, note }) => (
        <div key={shot}>
          <div
            style={{
              position: "relative",
              borderRadius: 18,
              overflow: "hidden",
              border: `4px solid ${INK}`,
            }}
          >
            <svg
              viewBox={`0 0 ${W} ${H}`}
              width={362}
              height={644}
              style={{ display: "block" }}
            >
              <Art />
            </svg>
            <div
              style={{
                position: "absolute",
                left: 12,
                top: 12,
                padding: "5px 12px",
                borderRadius: 8,
                background: INK,
                color: "#FFF",
                fontSize: 17,
                fontWeight: 800,
                letterSpacing: 1,
              }}
            >
              {shot}
            </div>
          </div>
          <div style={{ fontSize: 28, fontWeight: 800, marginTop: 12 }}>
            {title}
          </div>
          <div
            style={{
              fontSize: 20,
              color: "#4B4470",
              lineHeight: 1.35,
              marginTop: 4,
            }}
          >
            {note}
          </div>
        </div>
      ))}
    </div>
  </AbsoluteFill>
);
