// Hand-drawn props for "The 2 a.m. Idea", in the sketch style.
import { useId } from "react";
import { rand } from "../anim";

const LINE = "#4A2E28";

/** A doodled lightbulb: the idea. glow 0..1; pop 0..1 bursts it into smoke. */
export const Bulb: React.FC<{
  x: number;
  y: number;
  s: number;
  glow?: number;
  pop?: number;
}> = ({ x, y, s, glow = 1, pop = 0 }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  if (pop >= 1) return null;
  return (
    <g
      transform={`translate(${x} ${y}) scale(${s * (1 + pop * 0.4)})`}
      opacity={1 - pop}
    >
      <defs>
        <radialGradient id={`g${id}`}>
          <stop offset="0%" stopColor="#FFF4C2" stopOpacity={0.95} />
          <stop offset="35%" stopColor="#FBBF24" stopOpacity={0.55} />
          <stop offset="100%" stopColor="#FBBF24" stopOpacity={0} />
        </radialGradient>
      </defs>
      <circle r={200} fill={`url(#g${id})`} opacity={glow} />
      <path
        d="M-46 30 C-74 4 -70 -60 -20 -78 C30 -96 80 -60 70 -6 C66 18 50 30 42 46 L-36 46 C-40 40 -42 36 -46 30 Z"
        fill={glow > 0.4 ? "#FFF6D8" : "#D9D2C6"}
        stroke={LINE}
        strokeWidth={4}
        strokeLinejoin="round"
      />
      <path
        d="M-14 40 L-10 4 Q0 -10 10 4 L14 40"
        stroke={glow > 0.4 ? "#F59E0B" : "#8C8378"}
        strokeWidth={3.5}
        fill="none"
        strokeLinecap="round"
      />
      <path
        d="M-34 50 L34 50 L30 64 L-30 64 Z M-28 68 L28 68 L24 80 L-24 80 Z"
        fill="#B9A88F"
        stroke={LINE}
        strokeWidth={3}
        strokeLinejoin="round"
      />
      <path d="M-14 84 Q0 96 14 84" stroke={LINE} strokeWidth={3} fill="none" />
      <path
        d="M-40 -40 Q-36 -60 -16 -66"
        stroke="#FFFFFF"
        strokeWidth={6}
        fill="none"
        strokeLinecap="round"
        opacity={0.8}
      />
      {glow > 0.4 &&
        [0, 1, 2, 3, 4, 5].map((i) => {
          const a = -Math.PI / 2 + (i - 2.5) * 0.45;
          return (
            <line
              key={i}
              x1={Math.cos(a) * 100}
              y1={Math.sin(a) * 100 - 10}
              x2={Math.cos(a) * 130}
              y2={Math.sin(a) * 130 - 10}
              stroke="#F59E0B"
              strokeWidth={6}
              strokeLinecap="round"
              opacity={glow}
            />
          );
        })}
      {pop > 0 &&
        Array.from({ length: 8 }, (_, i) => {
          const a = (i / 8) * Math.PI * 2;
          return (
            <circle
              key={i}
              cx={Math.cos(a) * 90 * (1 + pop)}
              cy={Math.sin(a) * 90 * (1 + pop)}
              r={26 * (1 - pop * 0.5)}
              fill="#D9D2C6"
              opacity={0.8}
            />
          );
        })}
    </g>
  );
};

/** A sketchy thought cloud with handwritten lines. */
export const ThoughtBubble: React.FC<{
  x: number;
  y: number;
  w: number;
  lines: string[];
  size?: number;
  tail?: [number, number];
  opacity?: number;
}> = ({ x, y, w, lines, size = 46, tail, opacity = 1 }) => {
  const h = lines.length * size * 1.25 + size * 1.3;
  const bumps = Math.round(w / 70);
  const puffs = [
    ...Array.from({ length: bumps }, (_, i) => [
      x + (i + 0.5) * (w / bumps),
      y,
      w / bumps / 1.4,
    ]),
    ...Array.from({ length: bumps }, (_, i) => [
      x + (i + 0.5) * (w / bumps),
      y + h,
      w / bumps / 1.4,
    ]),
    [x, y + h / 2, h / 2.4],
    [x + w, y + h / 2, h / 2.4],
  ];
  return (
    <g opacity={opacity}>
      {puffs.map(([cx, cy, r], i) => (
        <circle
          key={`o${i}`}
          cx={cx}
          cy={cy}
          r={r}
          fill="#FFFBF4"
          stroke={LINE}
          strokeWidth={4}
        />
      ))}
      {puffs.map(([cx, cy, r], i) => (
        <circle key={`i${i}`} cx={cx} cy={cy} r={r - 3} fill="#FFFBF4" />
      ))}
      <rect x={x} y={y} width={w} height={h} fill="#FFFBF4" />
      {tail &&
        [0, 1, 2].map((i) => {
          const t = (i + 1) / 4;
          return (
            <circle
              key={i}
              cx={x + w * 0.3 + (tail[0] - x - w * 0.3) * t}
              cy={y + h + (tail[1] - y - h) * t}
              r={22 - i * 6}
              fill="#FFFBF4"
              stroke={LINE}
              strokeWidth={3.5}
            />
          );
        })}
      <text
        x={x + w / 2}
        y={y + size * 1.15}
        textAnchor="middle"
        fontFamily="Kalam"
        fontWeight={700}
        fontSize={size}
        fill="#3B2A2A"
      >
        {lines.map((l, i) => (
          <tspan key={i} x={x + w / 2} dy={i ? size * 1.25 : 0}>
            {l}
          </tspan>
        ))}
      </text>
    </g>
  );
};

/** The bedroom: headboard, window (night or morning), pillow and quilt. */
export const Bedroom: React.FC<{ morning: number }> = ({ morning }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const m = morning;
  return (
    <g>
      <defs>
        <linearGradient id={`wall${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={m > 0.5 ? "#F3DCC6" : "#2A2F55"} />
          <stop offset="100%" stopColor={m > 0.5 ? "#E8C9AE" : "#1B1F3E"} />
        </linearGradient>
        <linearGradient id={`sky${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={m > 0.5 ? "#9CCBF2" : "#0B1030"} />
          <stop offset="100%" stopColor={m > 0.5 ? "#FFE3B8" : "#2E2A6A"} />
        </linearGradient>
        <radialGradient id={`sun${id}`}>
          <stop offset="0%" stopColor="#FFF4D6" stopOpacity={0.9} />
          <stop offset="100%" stopColor="#FFF4D6" stopOpacity={0} />
        </radialGradient>
      </defs>
      <rect width={1080} height={1920} fill={`url(#wall${id})`} />
      {/* Window */}
      <rect
        x={560}
        y={90}
        width={420}
        height={500}
        rx={18}
        fill={`url(#sky${id})`}
        stroke={LINE}
        strokeWidth={6}
      />
      {m > 0.5 ? (
        <circle cx={860} cy={240} r={120} fill={`url(#sun${id})`} />
      ) : (
        <>
          <circle cx={870} cy={210} r={44} fill="#F8F1DC" />
          <circle cx={892} cy={198} r={40} fill="#0B1030" />
          {Array.from({ length: 24 }, (_, i) => (
            <circle
              key={i}
              cx={580 + rand(i + 4) * 380}
              cy={110 + rand(i * 3 + 4) * 300}
              r={1.5 + rand(i) * 2}
              fill="#FFFFFF"
              opacity={0.8}
            />
          ))}
        </>
      )}
      {Array.from({ length: 9 }, (_, i) => (
        <rect
          key={i}
          x={566 + i * 46}
          y={430 - rand(i + 9) * 120}
          width={40}
          height={170}
          fill={m > 0.5 ? "#C9B6D6" : "#141836"}
        />
      ))}
      {m < 0.5 &&
        Array.from({ length: 22 }, (_, i) => (
          <rect
            key={i}
            x={572 + rand(i + 31) * 400}
            y={340 + rand(i * 5 + 31) * 220}
            width={7}
            height={9}
            fill="#FDE68A"
            opacity={0.7}
          />
        ))}
      <path
        d="M770 90 L770 590 M560 340 L980 340"
        stroke={LINE}
        strokeWidth={6}
      />
      {/* Curtains */}
      <path
        d="M520 70 C560 250 540 450 580 620 L520 620 Z"
        fill={m > 0.5 ? "#E7B57A" : "#3A3466"}
        stroke={LINE}
        strokeWidth={4}
      />
      <path
        d="M1020 70 C980 250 1000 450 960 620 L1020 620 Z"
        fill={m > 0.5 ? "#E7B57A" : "#3A3466"}
        stroke={LINE}
        strokeWidth={4}
      />
      {/* Headboard */}
      <rect
        x={40}
        y={600}
        width={1000}
        height={260}
        rx={60}
        fill={m > 0.5 ? "#B07A55" : "#4A3A5E"}
        stroke={LINE}
        strokeWidth={5}
      />
      <rect
        x={80}
        y={640}
        width={920}
        height={180}
        rx={40}
        fill={m > 0.5 ? "#C48C64" : "#56456C"}
      />
      {/* Bed */}
      <rect
        x={0}
        y={820}
        width={1080}
        height={1100}
        fill={m > 0.5 ? "#F4EBDD" : "#3E4470"}
      />
      {/* Pillow */}
      <path
        d="M150 700 C140 640 220 620 540 626 C860 620 940 640 930 700 C946 800 940 880 930 930 C940 990 860 1000 540 994 C220 1000 140 990 150 930 C140 880 134 800 150 700 Z"
        fill={m > 0.5 ? "#FFFDF8" : "#8E93C4"}
        stroke={LINE}
        strokeWidth={4}
      />
      {/* Nightstand + phone glow */}
      <rect
        x={0}
        y={760}
        width={130}
        height={200}
        rx={12}
        fill={m > 0.5 ? "#B07A55" : "#3A2E4E"}
        stroke={LINE}
        strokeWidth={4}
      />
    </g>
  );
};

/** The quilt pulled up to the shoulders. */
export const Quilt: React.FC<{
  morning: number;
  y?: number;
  breathe?: number;
}> = ({ morning, y = 1060, breathe = 0 }) => (
  <g transform={`translate(0 ${breathe * 6})`}>
    <path
      d={`M-20 ${y} C200 ${y - 50} 880 ${y - 50} 1100 ${y} L1100 1940 L-20 1940 Z`}
      fill={morning > 0.5 ? "#E9A7A0" : "#5B5F9A"}
      stroke={LINE}
      strokeWidth={5}
    />
    <path
      d={`M-20 ${y + 40} C200 ${y - 10} 880 ${y - 10} 1100 ${y + 40}`}
      stroke={morning > 0.5 ? "#FBE7E2" : "#7C80BC"}
      strokeWidth={22}
      fill="none"
    />
    {Array.from({ length: 7 }, (_, i) => (
      <path
        key={i}
        d={`M${i * 170} ${y + 120} q60 30 120 0`}
        stroke={morning > 0.5 ? "#D98A82" : "#4A4E86"}
        strokeWidth={4}
        fill="none"
      />
    ))}
    {Array.from({ length: 5 }, (_, i) => (
      <path
        key={i}
        d={`M${80 + i * 210} ${y + 340} q50 -40 100 0 q50 40 100 0`}
        stroke={morning > 0.5 ? "#D98A82" : "#4A4E86"}
        strokeWidth={4}
        fill="none"
      />
    ))}
  </g>
);

/** Floating "z z z". */
export const Zzz: React.FC<{ x: number; y: number; t: number }> = ({
  x,
  y,
  t,
}) => (
  <g>
    {[0, 1, 2].map((i) => {
      const k = (t / 40 + i / 3) % 1;
      return (
        <text
          key={i}
          x={x + k * 60}
          y={y - k * 140}
          fontFamily="Kalam"
          fontWeight={700}
          fontSize={50 + i * 10}
          fill="#E9E4FF"
          opacity={Math.sin(k * Math.PI)}
        >
          z
        </text>
      );
    })}
  </g>
);

/** Rooftop café at golden hour: string lights, shelves, a chalkboard. */
export const Rooftop: React.FC<{ t: number }> = ({ t }) => (
  <g>
    <defs>
      <linearGradient id="rtSky" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#5B3FA8" />
        <stop offset="40%" stopColor="#E26AA8" />
        <stop offset="70%" stopColor="#FB923C" />
        <stop offset="100%" stopColor="#FDE68A" />
      </linearGradient>
      <radialGradient id="rtBulb">
        <stop offset="0%" stopColor="#FFF4C2" stopOpacity={0.9} />
        <stop offset="100%" stopColor="#FBBF24" stopOpacity={0} />
      </radialGradient>
    </defs>
    <rect width={1080} height={1920} fill="url(#rtSky)" />
    <circle cx={800} cy={860} r={90} fill="#FFF1C9" opacity={0.9} />
    {Array.from({ length: 12 }, (_, i) => (
      <rect
        key={i}
        x={i * 94 - 10}
        y={760 - rand(i + 70) * 200}
        width={86}
        height={500}
        fill="#7A3F86"
        opacity={0.85}
      />
    ))}
    {/* String lights */}
    {[0, 1].map((row) => {
      const y0 = 320 + row * 140;
      const pts = Array.from(
        { length: 13 },
        (_, i) => [i * 90, y0 + Math.sin((i / 12) * Math.PI) * 70] as const,
      );
      return (
        <g key={row}>
          <path
            d={pts
              .map(([px, py], i) => `${i ? "L" : "M"}${px} ${py}`)
              .join(" ")}
            stroke={LINE}
            strokeWidth={3}
            fill="none"
          />
          {pts.map(([px, py], i) => (
            <g key={i}>
              <circle
                cx={px}
                cy={py + 18}
                r={34}
                fill="url(#rtBulb)"
                opacity={0.6 + 0.4 * Math.sin(t / 6 + i + row)}
              />
              <circle
                cx={px}
                cy={py + 18}
                r={9}
                fill="#FFF4C2"
                stroke={LINE}
                strokeWidth={2}
              />
            </g>
          ))}
        </g>
      );
    })}
    {/* Floor + railing */}
    <rect y={1180} width={1080} height={740} fill="#B07A55" />
    {Array.from({ length: 9 }, (_, i) => (
      <line
        key={i}
        x1={0}
        y1={1200 + i * 80}
        x2={1080}
        y2={1200 + i * 80}
        stroke="#8E5E40"
        strokeWidth={4}
      />
    ))}
    <rect y={1040} width={1080} height={20} fill="#5A3A2E" />
    {Array.from({ length: 14 }, (_, i) => (
      <rect
        key={i}
        x={20 + i * 80}
        y={1040}
        width={14}
        height={150}
        fill="#5A3A2E"
      />
    ))}
    {/* Bookshelf */}
    <rect
      x={40}
      y={760}
      width={300}
      height={430}
      fill="#7A4E36"
      stroke={LINE}
      strokeWidth={5}
    />
    {[0, 1, 2].map((r) => (
      <g key={r}>
        <rect x={50} y={880 + r * 120} width={280} height={10} fill="#5A3A2E" />
        {Array.from({ length: 9 }, (_, i) => (
          <rect
            key={i}
            x={58 + i * 30}
            y={800 + r * 120 - rand(i + r * 9) * 20}
            width={24}
            height={80 + rand(i + r * 9) * 20}
            fill={
              [
                "#BE123C",
                "#2563EB",
                "#D97706",
                "#0D9488",
                "#7C3AED",
                "#F4EBDD",
              ][(i + r) % 6]
            }
            stroke={LINE}
            strokeWidth={2}
          />
        ))}
      </g>
    ))}
    {/* Sign + chalkboard */}
    <g transform="rotate(-3 700 640)">
      <rect
        x={520}
        y={580}
        width={420}
        height={120}
        rx={16}
        fill="#5A3A2E"
        stroke={LINE}
        strokeWidth={5}
      />
      <text
        x={730}
        y={660}
        textAnchor="middle"
        fontFamily="Playfair Display"
        fontStyle="italic"
        fontSize={60}
        fill="#FDE68A"
      >
        Chai &amp; Chapters
      </text>
    </g>
    <g transform="translate(760 1180) rotate(4)">
      <path
        d="M0 0 L220 0 L240 330 L-20 330 Z"
        fill="#2E3B2E"
        stroke="#7A4E36"
        strokeWidth={12}
      />
      <text
        x={110}
        y={90}
        textAnchor="middle"
        fontFamily="Kalam"
        fontWeight={700}
        fontSize={42}
        fill="#F8F4EC"
      >
        <tspan x={110}>Swap a</tspan>
        <tspan x={110} dy={52}>
          book,
        </tspan>
        <tspan x={110} dy={52}>
          get a
        </tspan>
        <tspan x={110} dy={52}>
          chai
        </tspan>
      </text>
    </g>
  </g>
);

/** A clay kulhad of chai with steam. */
export const Kulhad: React.FC<{
  x: number;
  y: number;
  s: number;
  t: number;
}> = ({ x, y, s, t }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    {[0, 1].map((i) => (
      <path
        key={i}
        d={`M${-12 + i * 24} -70 q${10 * Math.sin(t / 8 + i)} -30 0 -60`}
        stroke="#FFFFFF"
        strokeWidth={6}
        fill="none"
        opacity={0.6}
        strokeLinecap="round"
      />
    ))}
    <path
      d="M-50 -50 L50 -50 L38 50 Q0 62 -38 50 Z"
      fill="#B45E3C"
      stroke={LINE}
      strokeWidth={4}
    />
    <ellipse
      cx={0}
      cy={-50}
      rx={50}
      ry={12}
      fill="#8E4A2E"
      stroke={LINE}
      strokeWidth={4}
    />
    <ellipse cx={0} cy={-48} rx={42} ry={8} fill="#D9A06A" />
  </g>
);
