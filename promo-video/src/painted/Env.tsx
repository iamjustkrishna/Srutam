// Painted (Shinkai-style) environments for 16:9 story panels, 1600 x 900.
import { useId } from "react";
import { rand } from "../anim";

export const PW = 1600;
export const PH = 900;

const useUid = () => useId().replace(/[^a-zA-Z0-9]/g, "");

export type SkyMood = "golden" | "dusk" | "night" | "morning" | "monsoon";

const SKIES: Record<SkyMood, string[]> = {
  golden: ["#4C3B9E", "#C06AB8", "#FF9E7A", "#FFD79A", "#FFF1C9"],
  dusk: ["#1E1B5A", "#5B3FA8", "#E26AA8", "#FB923C", "#FDE68A"],
  night: ["#03040F", "#0B1030", "#1E1B4B", "#3B2A7A", "#5B3F8E"],
  morning: ["#7DB6F0", "#A9CDF2", "#F3D7E6", "#FFE3C2", "#FFF4E0"],
  monsoon: ["#2B3A55", "#3E5272", "#5B6F8E", "#7F8FA8", "#A3B0C2"],
};

/** A layered sky with a sun (or moon) bloom. */
export const Sky: React.FC<{
  mood: SkyMood;
  sun?: [number, number];
  sunR?: number;
}> = ({ mood, sun, sunR = 70 }) => {
  const id = useUid();
  const c = SKIES[mood];
  const glow =
    mood === "night" ? "#C7D2FE" : mood === "monsoon" ? "#E2E8F0" : "#FFF3C4";
  return (
    <g>
      <defs>
        <linearGradient id={`s${id}`} x1="0" y1="0" x2="0" y2="1">
          {c.map((col, i) => (
            <stop
              key={i}
              offset={`${(i / (c.length - 1)) * 100}%`}
              stopColor={col}
            />
          ))}
        </linearGradient>
        <radialGradient id={`b${id}`}>
          <stop offset="0%" stopColor={glow} stopOpacity={0.95} />
          <stop offset="20%" stopColor={glow} stopOpacity={0.6} />
          <stop offset="100%" stopColor={glow} stopOpacity={0} />
        </radialGradient>
      </defs>
      <rect width={PW} height={PH} fill={`url(#s${id})`} />
      {sun && (
        <>
          <circle cx={sun[0]} cy={sun[1]} r={sunR * 7} fill={`url(#b${id})`} />
          <circle cx={sun[0]} cy={sun[1]} r={sunR} fill={glow} />
        </>
      )}
    </g>
  );
};

/** Soft painted cumulus: shadow mass, lit cap, blurred edges. */
export const SoftCloud: React.FC<{
  x: number;
  y: number;
  s: number;
  lit: string;
  shade: string;
  seed?: number;
}> = ({ x, y, s, lit, shade, seed = 1 }) => {
  const id = useUid();
  const puffs = Array.from({ length: 9 }, (_, i) => {
    const px = (i - 4) * 46 + (rand(i + seed) - 0.5) * 30;
    const py =
      -Math.sin((i / 8) * Math.PI) * 60 + (rand(i * 3 + seed) - 0.5) * 20;
    const r = 40 + Math.sin((i / 8) * Math.PI) * 45 + rand(i * 5 + seed) * 12;
    return [px, py, r];
  });
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <defs>
        <filter id={`c${id}`} x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation={3.5} />
        </filter>
      </defs>
      <g filter={`url(#c${id})`}>
        {puffs.map(([px, py, r], i) => (
          <circle key={`s${i}`} cx={px} cy={py + 10} r={r} fill={shade} />
        ))}
        <rect x={-210} y={0} width={420} height={50} rx={25} fill={shade} />
        {puffs.map(([px, py, r], i) => (
          <circle
            key={`l${i}`}
            cx={px - r * 0.15}
            cy={py - r * 0.2}
            r={r * 0.78}
            fill={lit}
          />
        ))}
      </g>
    </g>
  );
};

/** Distant city skyline layer, with optional lit windows. */
export const Skyline: React.FC<{
  y: number;
  color: string;
  seed?: number;
  windows?: boolean;
  h?: number;
}> = ({ y, color, seed = 1, windows = false, h = 160 }) => (
  <g>
    {Array.from({ length: 30 }, (_, i) => {
      const w = 40 + rand(i + seed) * 60;
      const x = i * 56 - 20;
      const bh = 40 + rand(i * 7 + seed) * h;
      return (
        <g key={i}>
          <rect x={x} y={y - bh} width={w} height={bh + 400} fill={color} />
          {windows &&
            Array.from({ length: 6 }, (_, k) => (
              <rect
                key={k}
                x={x + 8 + (k % 2) * 18}
                y={y - bh + 12 + Math.floor(k / 2) * 22}
                width={8}
                height={10}
                fill="#FDE68A"
                opacity={rand(i * 13 + k) > 0.45 ? 0.8 : 0}
              />
            ))}
        </g>
      );
    })}
  </g>
);

/** Floating bokeh dots. */
export const Bokeh: React.FC<{
  n?: number;
  colors?: string[];
  seed?: number;
  maxR?: number;
}> = ({ n = 18, colors = ["#FDBA74", "#F9A8D4"], seed = 3, maxR = 60 }) => (
  <g>
    {Array.from({ length: n }, (_, i) => (
      <circle
        key={i}
        cx={rand(i + seed) * PW}
        cy={rand(i * 3 + seed) * PH}
        r={10 + rand(i * 5 + seed) * maxR}
        fill={colors[i % colors.length]}
        opacity={0.08 + rand(i * 7) * 0.18}
      />
    ))}
  </g>
);

/** Diagonal light shafts. */
export const Rays: React.FC<{
  from: [number, number];
  color?: string;
  opacity?: number;
}> = ({ from, color = "#FFF1C9", opacity = 0.14 }) => {
  const id = useUid();
  return (
    <g>
      <defs>
        <radialGradient id={`r${id}`} cx={from[0] / PW} cy={from[1] / PH} r="1">
          <stop offset="0%" stopColor={color} stopOpacity={opacity} />
          <stop offset="70%" stopColor={color} stopOpacity={0} />
        </radialGradient>
      </defs>
      {[-0.9, -0.55, -0.2, 0.15, 0.5].map((a, i) => {
        const len = 2200;
        const w = 0.07 + (i % 2) * 0.04;
        const p = (ang: number) =>
          `${from[0] + Math.cos(Math.PI + ang) * len},${from[1] + Math.sin(Math.PI + ang) * len * 0.6}`;
        return (
          <polygon
            key={i}
            points={`${from[0]},${from[1]} ${p(a - w)} ${p(a + w)}`}
            fill={`url(#r${id})`}
          />
        );
      })}
    </g>
  );
};

/** Grain, vignette and a slight hand-painted wobble live on the panel itself. */
export const Grain: React.FC<{ vignette?: number }> = ({ vignette = 0.5 }) => {
  const id = useUid();
  return (
    <g pointerEvents="none">
      <defs>
        <filter id={`n${id}`}>
          <feTurbulence
            type="fractalNoise"
            baseFrequency={0.9}
            numOctaves={2}
            seed={7}
          />
          <feColorMatrix type="saturate" values="0" />
        </filter>
        <radialGradient id={`v${id}`} cx="50%" cy="50%" r="75%">
          <stop offset="60%" stopColor="#05030F" stopOpacity={0} />
          <stop offset="100%" stopColor="#05030F" stopOpacity={vignette} />
        </radialGradient>
      </defs>
      <rect
        width={PW}
        height={PH}
        filter={`url(#n${id})`}
        opacity={0.08}
        style={{ mixBlendMode: "overlay" }}
      />
      <rect width={PW} height={PH} fill={`url(#v${id})`} />
    </g>
  );
};

/** Wrap painted layers: edges get a faint brush wobble. */
export const Painted: React.FC<{
  children: React.ReactNode;
  wobble?: number;
}> = ({ children, wobble = 2.2 }) => {
  const id = useUid();
  return (
    <g>
      <defs>
        <filter id={`p${id}`} x="-5%" y="-5%" width="110%" height="110%">
          <feTurbulence
            type="fractalNoise"
            baseFrequency={0.035}
            numOctaves={2}
            seed={3}
            result="t"
          />
          <feDisplacementMap
            in="SourceGraphic"
            in2="t"
            scale={wobble}
            xChannelSelector="R"
            yChannelSelector="G"
          />
        </filter>
      </defs>
      <g filter={`url(#p${id})`}>{children}</g>
    </g>
  );
};

/** A Mumbai local, seen from inside: a long window band onto the sunset. */
export const TrainWindow: React.FC<{ mood?: SkyMood; tint?: number }> = ({
  mood = "golden",
  tint = 0,
}) => {
  const id = useUid();
  return (
    <g>
      <defs>
        <clipPath id={`w${id}`}>
          <rect x={0} y={130} width={PW} height={470} />
        </clipPath>
        <linearGradient id={`in${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#2E2252" />
          <stop offset="100%" stopColor="#140E2C" />
        </linearGradient>
      </defs>
      <rect width={PW} height={PH} fill={`url(#in${id})`} />
      <g clipPath={`url(#w${id})`}>
        <Sky mood={mood} sun={[1180, 520]} sunR={46} />
        <SoftCloud
          x={420}
          y={260}
          s={0.9}
          lit="#FFD3E6"
          shade="#A58BE0"
          seed={2}
        />
        <SoftCloud
          x={1000}
          y={330}
          s={0.6}
          lit="#FFE1C2"
          shade="#D592D8"
          seed={5}
        />
        <Skyline y={560} color="#7A3F86" seed={4} h={110} />
        {/* Sea link */}
        <g stroke="#5A2468" strokeWidth={2.5}>
          {Array.from({ length: 10 }, (_, i) => (
            <line key={i} x1={820} y1={420} x2={680 + i * 30} y2={560} />
          ))}
        </g>
        <path d="M812 562 L818 410 L824 410 L830 562 Z" fill="#5A2468" />
        <rect x={0} y={558} width={PW} height={8} fill="#5A2468" />
        {Array.from({ length: 30 }, (_, i) => (
          <rect
            key={i}
            x={rand(i + 20) * PW}
            y={170 + rand(i * 3 + 20) * 400}
            width={160 + rand(i) * 260}
            height={3 + rand(i * 5) * 5}
            rx={3}
            fill="#FFFFFF"
            opacity={0.12}
          />
        ))}
      </g>
      {/* Window frames and the rail */}
      {[0, 1, 2, 3].map((i) => (
        <rect
          key={i}
          x={i * 420 - 30}
          y={130}
          width={420}
          height={470}
          rx={36}
          fill="none"
          stroke="#140E2C"
          strokeWidth={26}
        />
      ))}
      <rect x={0} y={70} width={PW} height={14} rx={7} fill="#140E2C" />
      {[200, 520, 840, 1160, 1480].map((x, i) => (
        <g key={x} transform={`rotate(${i % 2 ? 4 : -5} ${x} 80)`}>
          <rect x={x - 6} y={80} width={12} height={70} rx={5} fill="#140E2C" />
          <ellipse
            cx={x}
            cy={172}
            rx={28}
            ry={32}
            fill="none"
            stroke="#140E2C"
            strokeWidth={11}
          />
        </g>
      ))}
      <Rays from={[1180, 520]} />
      {tint > 0 && (
        <rect width={PW} height={PH} fill="#1E1B4B" opacity={tint} />
      )}
    </g>
  );
};

/** Marine Drive at night: the Queen's Necklace curving round the bay. */
export const MarineDrive: React.FC<{ mood?: SkyMood }> = ({
  mood = "night",
}) => (
  <g>
    <Sky
      mood={mood}
      sun={mood === "night" ? [1320, 170] : [1250, 520]}
      sunR={mood === "night" ? 34 : 60}
    />
    {mood === "night" &&
      Array.from({ length: 140 }, (_, i) => (
        <circle
          key={i}
          cx={rand(i + 2) * PW}
          cy={rand(i * 3 + 2) * 480}
          r={0.8 + rand(i * 5) * 2.2}
          fill="#FFFFFF"
          opacity={0.3 + rand(i * 7) * 0.7}
        />
      ))}
    {mood !== "night" && (
      <SoftCloud
        x={420}
        y={250}
        s={1.1}
        lit="#FFD3E6"
        shade="#A58BE0"
        seed={8}
      />
    )}
    <Skyline
      y={560}
      color={mood === "night" ? "#0E1230" : "#5A2E78"}
      seed={9}
      windows={mood === "night"}
      h={140}
    />
    {/* The bay */}
    <path
      d="M0 600 Q800 520 1600 610 L1600 900 L0 900 Z"
      fill={mood === "night" ? "#070A1F" : "#3B1F5E"}
    />
    {/* Queen's Necklace: an arc of streetlights and their reflections */}
    {Array.from({ length: 46 }, (_, i) => {
      const t = i / 45;
      const x = t * PW;
      const y = 600 - Math.sin(t * Math.PI) * 70 + t * 10;
      return (
        <g key={i}>
          <circle cx={x} cy={y} r={14} fill="#FDE68A" opacity={0.22} />
          <circle cx={x} cy={y} r={4} fill="#FFF7D6" />
          <rect
            x={x - 2}
            y={y + 18}
            width={4}
            height={40 + rand(i) * 50}
            fill="#FDE68A"
            opacity={0.3}
          />
        </g>
      );
    })}
    {/* Sea wall */}
    <path
      d="M0 760 L1600 740 L1600 900 L0 900 Z"
      fill={mood === "night" ? "#151A33" : "#4A2C55"}
    />
    <path
      d="M0 760 L1600 740"
      stroke={mood === "night" ? "#2E3A66" : "#F5B7A0"}
      strokeWidth={6}
    />
  </g>
);

/** Monsoon street: rain, wet bokeh, reflections. */
export const Monsoon: React.FC = () => (
  <g>
    <Sky mood="monsoon" />
    <Skyline y={600} color="#3A4A66" seed={14} h={180} windows />
    <rect y={600} width={PW} height={300} fill="#2B364E" />
    <Bokeh
      n={22}
      colors={["#FCD34D", "#F87171", "#FDBA74"]}
      seed={30}
      maxR={40}
    />
    {Array.from({ length: 160 }, (_, i) => {
      const x = rand(i + 50) * (PW + 200) - 100;
      const y = rand(i * 3 + 50) * PH;
      return (
        <line
          key={i}
          x1={x}
          y1={y}
          x2={x - 14}
          y2={y + 46}
          stroke="#E2E8F0"
          strokeWidth={1.6}
          opacity={0.35}
        />
      );
    })}
  </g>
);

/** Design studio at dusk: desk lamp, sketches, warm window. */
export const Studio: React.FC = () => (
  <g>
    <rect width={PW} height={PH} fill="#2A1E3A" />
    <g>
      <rect x={80} y={80} width={620} height={480} rx={20} fill="#1A1228" />
      <svg x={96} y={96} width={588} height={448}>
        <Sky mood="dusk" sun={[420, 380]} sunR={30} />
        <Skyline y={420} color="#4A2C66" seed={21} h={160} windows />
      </svg>
    </g>
    <radialGradient id="lampGlow">
      <stop offset="0%" stopColor="#FFD79A" stopOpacity={0.55} />
      <stop offset="100%" stopColor="#FFD79A" stopOpacity={0} />
    </radialGradient>
    <circle cx={1300} cy={520} r={420} fill="url(#lampGlow)" />
    <path
      d="M1320 200 L1240 420 M1240 420 L1330 560"
      stroke="#0F0A1C"
      strokeWidth={14}
      strokeLinecap="round"
    />
    <path d="M1260 190 L1380 190 L1360 250 L1280 250 Z" fill="#0F0A1C" />
    <rect x={0} y={700} width={PW} height={200} fill="#3E2A4E" />
    {[
      [200, 690],
      [420, 700],
      [560, 684],
    ].map(([x, y], i) => (
      <rect
        key={i}
        x={x}
        y={y}
        width={180}
        height={120}
        rx={6}
        fill="#F8F4EC"
        transform={`rotate(${(i - 1) * 6} ${x} ${y})`}
      />
    ))}
  </g>
);

/** Bedroom at 2 a.m.: dark, a sliver of moon, phone glow. */
export const Bedroom: React.FC = () => (
  <g>
    <rect width={PW} height={PH} fill="#0B0C22" />
    <rect x={1000} y={90} width={460} height={420} rx={16} fill="#121638" />
    <svg x={1014} y={104} width={432} height={392}>
      <Sky mood="night" sun={[320, 110]} sunR={24} />
      {Array.from({ length: 40 }, (_, i) => (
        <circle
          key={i}
          cx={rand(i + 66) * 432}
          cy={rand(i * 2 + 66) * 280}
          r={1 + rand(i) * 1.8}
          fill="#FFFFFF"
          opacity={0.7}
        />
      ))}
      <Skyline y={392} color="#070A1F" seed={12} windows h={120} />
    </svg>
    <rect x={0} y={640} width={PW} height={260} fill="#151938" />
    <path d="M0 640 Q400 600 900 650 L900 900 L0 900 Z" fill="#232A5A" />
  </g>
);
