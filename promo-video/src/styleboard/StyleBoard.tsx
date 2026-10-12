import { AbsoluteFill } from "remotion";
import { rand } from "../anim";
import { C, FONT } from "../theme";
import { AnimeHost, EyeCloseUp } from "./AnimeHost";
import { AnimeSpark, type Expression } from "./AnimeSpark";
import { Burst, Cloud, OnionArrow, Sparkle, SpeedLines } from "./FX";

const PW = 480;
const PH = 853;

const Stars: React.FC<{ n: number; maxY: number; seed?: number }> = ({
  n,
  maxY,
  seed = 0,
}) => (
  <g>
    {Array.from({ length: n }, (_, i) => (
      <circle
        key={i}
        cx={rand(i + seed) * PW}
        cy={rand(i * 3 + seed) * maxY}
        r={0.8 + rand(i * 5 + seed) * 2.2}
        fill={i % 6 ? "#FFFFFF" : C.gold}
        opacity={0.4 + rand(i * 7) * 0.6}
      />
    ))}
  </g>
);

const Label: React.FC<{
  x: number;
  y: number;
  text: string;
  size?: number;
  color?: string;
  anchor?: "start" | "middle" | "end";
}> = ({ x, y, text, size = 30, color = "#FFFFFF", anchor = "middle" }) => (
  <text
    x={x}
    y={y}
    textAnchor={anchor}
    fontFamily="Inter"
    fontWeight={800}
    fontSize={size}
    fill={color}
    stroke="#0F172A"
    strokeWidth={6}
    paintOrder="stroke"
  >
    {text}
  </text>
);

const Hook = () => (
  <>
    <defs>
      <linearGradient id="hookBg" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#475569" />
        <stop offset="100%" stopColor="#1E293B" />
      </linearGradient>
    </defs>
    <rect width={PW} height={PH} fill="url(#hookBg)" />
    <SpeedLines cx={240} cy={440} inner={175} />
    <circle
      cx={240}
      cy={440}
      r={178}
      fill="none"
      stroke="rgba(255,255,255,0.25)"
      strokeWidth={10}
    />
    <circle
      cx={240}
      cy={440}
      r={178}
      fill="none"
      stroke={C.gold}
      strokeWidth={10}
      strokeLinecap="round"
      strokeDasharray={`${2 * Math.PI * 178 * 0.75} 9999`}
      transform="rotate(-90 240 440)"
    />
    <AnimeSpark x={240} y={450} r={118} expression="worried" lookX={0.4} />
    <Label x={372} y={300} text="4" size={78} color={C.gold} />
    <g transform="translate(40 170) rotate(-12)">
      <text
        fontFamily="Inter"
        fontStyle="italic"
        fontWeight={900}
        fontSize={58}
        fill="#FFFFFF"
        stroke="#0F172A"
        strokeWidth={8}
        paintOrder="stroke"
      >
        BA-DUMP
      </text>
    </g>
    <Label x={240} y={790} text="4 seconds to live." size={38} />
  </>
);

const Lost = () => (
  <>
    <defs>
      <linearGradient id="lostBg" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#64748B" />
        <stop offset="100%" stopColor="#1E293B" />
      </linearGradient>
    </defs>
    <rect width={PW} height={PH} fill="url(#lostBg)" />
    {Array.from({ length: 40 }, (_, i) => {
      const x = rand(i) * 560 - 40;
      const y = rand(i * 9) * PH;
      return (
        <line
          key={i}
          x1={x}
          y1={y}
          x2={x - 18}
          y2={y + 70}
          stroke="#CBD5E1"
          strokeWidth={2}
          opacity={0.35}
        />
      );
    })}
    <AnimeSpark
      x={240}
      y={400}
      r={110}
      expression="crying"
      life={0.25}
      glow={false}
    />
    {/* Gloom lines */}
    {Array.from({ length: 9 }, (_, i) => (
      <line
        key={i}
        x1={170 + i * 17}
        y1={300}
        x2={170 + i * 17}
        y2={346 + (i % 3) * 10}
        stroke="#4C1D95"
        strokeWidth={5}
        opacity={0.55}
      />
    ))}
    {Array.from({ length: 22 }, (_, i) => (
      <rect
        key={i}
        x={170 + rand(i) * 140}
        y={560 + rand(i * 4) * 170}
        width={8}
        height={8}
        rx={2}
        fill="#94A3B8"
        opacity={0.9 - rand(i * 4) * 0.7}
        transform={`rotate(${rand(i) * 90} ${170 + rand(i) * 140} ${560 + rand(i * 4) * 170})`}
      />
    ))}
    <Label x={240} y={790} text="Most never make it." size={36} />
  </>
);

const Impact = () => (
  <>
    <rect width={PW} height={PH} fill="#000000" />
    <SpeedLines
      cx={210}
      cy={470}
      inner={210}
      color="#FFFFFF"
      count={70}
      seed={9}
    />
    <Burst cx={210} cy={470} r={210} fill={C.crimson} stroke="#FFFFFF" />
    {/* The dock, white on black (inverted impact frame) */}
    <rect
      x={110}
      y={430}
      width={210}
      height={80}
      rx={40}
      fill="#FFFFFF"
      stroke="#000"
      strokeWidth={6}
    />
    <circle cx={152} cy={470} r={28} fill="#000" />
    <circle
      cx={215}
      cy={470}
      r={22}
      fill="none"
      stroke="#000"
      strokeWidth={6}
    />
    <path
      d="M265 456 L291 484 M291 456 L265 484"
      stroke="#000"
      strokeWidth={6}
      strokeLinecap="round"
    />
    {/* Thumb with smear lines */}
    <g transform="translate(150 470) rotate(-35)">
      {[0, 1, 2, 3].map((i) => (
        <line
          key={i}
          x1={-40 + i * 26}
          y1={170}
          x2={-40 + i * 26}
          y2={420}
          stroke="#FFFFFF"
          strokeWidth={6}
          opacity={0.6 - i * 0.1}
        />
      ))}
      <rect
        x={-50}
        y={-10}
        width={100}
        height={260}
        rx={50}
        fill="#FFFFFF"
        stroke="#000"
        strokeWidth={8}
      />
      <rect x={-32} y={6} width={64} height={78} rx={32} fill="#000" />
    </g>
    <g transform="translate(250 250) rotate(-10)">
      <text
        textAnchor="middle"
        fontFamily="Inter"
        fontStyle="italic"
        fontWeight={900}
        fontSize={130}
        fill="#FFFFFF"
        stroke={C.crimson}
        strokeWidth={12}
        paintOrder="stroke"
      >
        TAP!
      </text>
    </g>
  </>
);

const Joy = () => (
  <>
    <defs>
      <radialGradient id="joyBg" cx="55%" cy="40%" r="80%">
        <stop offset="0%" stopColor="#FFFBEB" />
        <stop offset="45%" stopColor="#FDE68A" />
        <stop offset="100%" stopColor="#F59E0B" />
      </radialGradient>
    </defs>
    <rect width={PW} height={PH} fill="url(#joyBg)" />
    {Array.from({ length: 14 }, (_, i) => (
      <circle
        key={i}
        cx={rand(i + 4) * PW}
        cy={rand(i * 6 + 1) * PH}
        r={14 + rand(i * 2) * 40}
        fill="#FFFFFF"
        opacity={0.18 + rand(i) * 0.2}
      />
    ))}
    {/* Onion skin: the bounce in, on 2s */}
    <g opacity={0.18}>
      <AnimeSpark x={90} y={700} r={40} expression="happy" glow={false} />
    </g>
    <g opacity={0.3}>
      <AnimeSpark x={130} y={560} r={52} expression="happy" glow={false} />
    </g>
    <OnionArrow d="M70 760 Q 110 440 200 420" />
    <AnimeSpark x={270} y={380} r={112} expression="happy" />
    <Sparkle x={110} y={250} s={30} />
    <Sparkle x={420} y={230} s={22} />
    <Sparkle x={410} y={520} s={34} />
    <Sparkle x={150} y={410} s={16} />
    <Label x={240} y={790} text="Caught!" size={48} />
  </>
);

const Sunset = () => (
  <>
    <defs>
      <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#312E81" />
        <stop offset="30%" stopColor="#7C3AED" />
        <stop offset="55%" stopColor="#F472B6" />
        <stop offset="72%" stopColor="#FDBA74" />
        <stop offset="80%" stopColor="#FEF3C7" />
      </linearGradient>
      <radialGradient id="sun">
        <stop offset="0%" stopColor="#FFFBEB" />
        <stop offset="30%" stopColor="#FDE68A" stopOpacity={0.9} />
        <stop offset="100%" stopColor="#FDBA74" stopOpacity={0} />
      </radialGradient>
    </defs>
    <rect width={PW} height={PH} fill="url(#sky)" />
    <circle cx={370} cy={600} r={240} fill="url(#sun)" />
    {[0, 1, 2, 3].map((i) => (
      <polygon
        key={i}
        points={`370,600 ${-60 + i * 110},0 ${-10 + i * 110},0`}
        fill="#FFFFFF"
        opacity={0.08}
      />
    ))}
    <Cloud x={70} y={170} s={0.9} lit="#FBCFE8" shade="#A78BFA" />
    <Cloud x={320} y={330} s={0.6} lit="#FED7AA" shade="#C084FC" />
    {/* City */}
    {Array.from({ length: 12 }, (_, i) => (
      <rect
        key={i}
        x={i * 42 - 10}
        y={610 - rand(i + 20) * 120}
        width={40}
        height={300}
        fill={i % 2 ? "#4C1D95" : "#5B21B6"}
      />
    ))}
    {/* Bus window frame + rail */}
    <path
      d={`M0 0 L${PW} 0 L${PW} ${PH} L0 ${PH} Z M30 90 L450 90 L450 640 L30 640 Z`}
      fill="#1E1B4B"
      fillRule="evenodd"
    />
    <rect x={0} y={60} width={PW} height={14} rx={7} fill="#312E81" />
    <AnimeHost x={54} y={372} s={0.62} lighting="sunset" />
    <circle
      cx={128}
      cy={232}
      r={64}
      fill="none"
      stroke={C.gold}
      strokeWidth={6}
      strokeDasharray="300 9999"
      transform="rotate(-90 128 232)"
    />
    <AnimeSpark x={128} y={236} r={44} expression="worried" />
  </>
);

const Eye = () => (
  <>
    <EyeCloseUp w={PW} h={PH} />
    <Sparkle x={330} y={300} s={22} color="#FDE68A" />
    <Sparkle x={140} y={660} s={14} color="#FDE68A" />
  </>
);

const EXPRESSIONS: Expression[] = [
  "happy",
  "shocked",
  "crying",
  "determined",
  "love",
  "sleepy",
];
const ModelSheet = () => (
  <>
    <rect width={PW} height={PH} fill="#FFFFFF" />
    {Array.from({ length: 30 }, (_, i) => (
      <line
        key={i}
        x1={0}
        y1={i * 30}
        x2={PW}
        y2={i * 30}
        stroke="#E2E8F0"
        strokeWidth={1}
      />
    ))}
    <text
      x={30}
      y={108}
      fontFamily="Inter"
      fontWeight={800}
      fontSize={30}
      fill="#0F172A"
    >
      SPARK · expressions
    </text>
    {EXPRESSIONS.map((e, i) => {
      const x = i % 2 ? 350 : 130;
      const y = 240 + Math.floor(i / 2) * 225;
      return (
        <g key={e}>
          <AnimeSpark
            x={x}
            y={y}
            r={60}
            expression={e}
            glow={false}
            life={e === "sleepy" ? 0.35 : 1}
          />
          <text
            x={x}
            y={y + 105}
            textAnchor="middle"
            fontFamily="Inter"
            fontWeight={700}
            fontSize={24}
            fill="#475569"
          >
            {e}
          </text>
        </g>
      );
    })}
  </>
);

const Night = () => {
  const nodes = [
    [80, 120],
    [180, 80],
    [300, 150],
    [410, 90],
    [130, 250],
    [260, 280],
    [380, 260],
  ];
  const links = [
    [0, 1],
    [1, 2],
    [2, 3],
    [0, 4],
    [4, 5],
    [5, 2],
    [5, 6],
    [6, 3],
  ];
  const cols = [
    C.glowBlue,
    C.gold,
    C.emerald,
    C.nebulaPink,
    C.glowPurple,
    C.gold,
    C.glowBlue,
  ];
  return (
    <>
      <defs>
        <linearGradient id="night" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#050714" />
          <stop offset="100%" stopColor="#1E1B4B" />
        </linearGradient>
      </defs>
      <rect width={PW} height={PH} fill="url(#night)" />
      <Stars n={70} maxY={500} />
      {links.map(([a, b], i) => (
        <line
          key={i}
          x1={nodes[a][0]}
          y1={nodes[a][1]}
          x2={nodes[b][0]}
          y2={nodes[b][1]}
          stroke={C.glowPurple}
          strokeWidth={3}
          opacity={0.9}
        />
      ))}
      {nodes.map(([x, y], i) => (
        <g key={i}>
          <circle cx={x} cy={y} r={20} fill={cols[i]} opacity={0.3} />
          <circle cx={x} cy={y} r={9} fill={cols[i]} />
        </g>
      ))}
      {Array.from({ length: 12 }, (_, i) => (
        <rect
          key={i}
          x={i * 42 - 10}
          y={560 - rand(i + 50) * 140}
          width={40}
          height={400}
          fill="#0C1225"
        />
      ))}
      {Array.from({ length: 30 }, (_, i) => (
        <rect
          key={i}
          x={rand(i + 7) * 470}
          y={470 + rand(i * 2 + 7) * 160}
          width={6}
          height={8}
          fill="#FDE68A"
          opacity={0.7}
        />
      ))}
      <AnimeHost x={54} y={372} s={0.62} lighting="phone" mouth="open" />
      <AnimeSpark x={395} y={420} r={42} expression="love" />
    </>
  );
};

const PANELS: { title: string; shot: string; note: string; Art: React.FC }[] = [
  {
    shot: "01 · ECU",
    title: "Hook",
    note: "Radial speed lines, sweat drop, heartbeat SFX. Countdown ring drains.",
    Art: Hook,
  },
  {
    shot: "02 · MCU",
    title: "Fading",
    note: "Desaturate to grey, tear streams, gloom lines, ash falls on 2s.",
    Art: Lost,
  },
  {
    shot: "03 · INSERT",
    title: "Tap",
    note: "2-frame inverted impact flash, smear on the thumb, burst + screen shake.",
    Art: Impact,
  },
  {
    shot: "04 · MS",
    title: "Caught",
    note: "Bounce in on 2s (onion skin), squash on landing, kira-kira sparkle pops.",
    Art: Joy,
  },
  {
    shot: "05 · WS",
    title: "The Host",
    note: "Golden-hour bus. Painted sky, cel-shaded clouds, warm rim light on the face.",
    Art: Sunset,
  },
  {
    shot: "06 · ECU",
    title: "Realization",
    note: "The spark reflected in the iris. Slow push-in, highlights twinkle.",
    Art: Eye,
  },
  {
    shot: "MODEL",
    title: "Spark sheet",
    note: "Ahoge flame curl, hard cel shadow, brown line art, glossy eyes.",
    Art: ModelSheet,
  },
  {
    shot: "08 · MCU",
    title: "Every thought connects",
    note: "Night, phone-glow underlight, the constellation of saved ideas.",
    Art: Night,
  },
];

const SWATCHES = [
  ["Spark gold", "#FBBF24"],
  ["Capture crimson", "#EF4444"],
  ["Next Steps", "#2563EB"],
  ["Ideas", "#D97706"],
  ["Decisions", "#0D9488"],
  ["Golden hour", "#F472B6"],
  ["Cosmic Void", "#050714"],
];

export const StyleBoard: React.FC = () => (
  <AbsoluteFill
    style={{
      background: "#F7F5F0",
      padding: 80,
      fontFamily: FONT.sans,
      color: C.slate900,
    }}
  >
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "flex-end",
        height: 230,
      }}
    >
      <div>
        <div
          style={{
            fontSize: 30,
            fontWeight: 700,
            letterSpacing: 6,
            color: C.slate500,
          }}
        >
          SRUTAM · THE SPARK · STYLE EXPLORATION
        </div>
        <div style={{ fontFamily: FONT.serif, fontSize: 110, lineHeight: 1.1 }}>
          Anime{" "}
          <span
            style={{ fontStyle: "italic", fontWeight: 700, color: C.cobalt }}
          >
            storyboard
          </span>
        </div>
        <div style={{ fontSize: 30, color: C.slate700, marginTop: 6 }}>
          9:16 panels · every frame drawn in code (SVG) and animatable in
          Remotion
        </div>
      </div>
      <div style={{ display: "flex", gap: 18 }}>
        {SWATCHES.map(([name, hex]) => (
          <div key={name} style={{ width: 110, textAlign: "center" }}>
            <div
              style={{
                height: 80,
                borderRadius: 16,
                background: hex,
                border: "3px solid #0F172A",
              }}
            />
            <div
              style={{
                fontSize: 18,
                fontWeight: 600,
                marginTop: 8,
                color: C.slate700,
              }}
            >
              {name}
            </div>
          </div>
        ))}
      </div>
    </div>

    <div
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(4, ${PW}px)`,
        gap: "50px 40px",
        marginTop: 50,
      }}
    >
      {PANELS.map(({ title, shot, note, Art }) => (
        <div key={title}>
          <div
            style={{
              position: "relative",
              width: PW,
              height: PH,
              border: "5px solid #0F172A",
              borderRadius: 8,
              overflow: "hidden",
              background: "#FFF",
            }}
          >
            <svg
              viewBox={`0 0 ${PW} ${PH}`}
              width={PW}
              height={PH}
              style={{ display: "block" }}
            >
              <Art />
            </svg>
            <div
              style={{
                position: "absolute",
                left: 14,
                top: 14,
                padding: "6px 14px",
                borderRadius: 8,
                background: "#0F172A",
                color: "#FFF",
                fontSize: 20,
                fontWeight: 800,
                letterSpacing: 1,
              }}
            >
              {shot}
            </div>
          </div>
          <div style={{ fontSize: 32, fontWeight: 800, marginTop: 16 }}>
            {title}
          </div>
          <div
            style={{
              fontSize: 23,
              color: C.slate700,
              lineHeight: 1.35,
              marginTop: 6,
            }}
          >
            {note}
          </div>
        </div>
      ))}
    </div>

    <div
      style={{
        display: "flex",
        gap: 40,
        marginTop: 50,
        fontSize: 24,
        lineHeight: 1.4,
        color: C.slate700,
      }}
    >
      {[
        [
          "Timing",
          "Characters animate on 2s/3s (12/8 fps) for the anime feel; camera moves, FX and UI stay on 1s (smooth).",
        ],
        [
          "Anime language",
          "Speed lines, impact frames, smears, held poses with shake, sweat drops, gloom lines, kira-kira sparkles.",
        ],
        [
          "What changes",
          "Spark gets anime eyes, an ahoge and cel shadow; the Host becomes a real character; backgrounds become painted skies.",
        ],
      ].map(([h, t]) => (
        <div
          key={h}
          style={{ flex: 1, borderTop: "4px solid #0F172A", paddingTop: 14 }}
        >
          <div style={{ fontWeight: 800, color: C.slate900, fontSize: 26 }}>
            {h}
          </div>
          {t}
        </div>
      ))}
    </div>
  </AbsoluteFill>
);
