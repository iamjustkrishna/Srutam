// Anime visual-language effects as SVG groups (all coordinates in px).
import { rand } from "../anim";

/** Radial speed lines converging on (cx, cy), leaving a clear center. */
export const SpeedLines: React.FC<{
  cx: number;
  cy: number;
  inner?: number;
  color?: string;
  count?: number;
  seed?: number;
}> = ({ cx, cy, inner = 150, color = "#FFFFFF", count = 110, seed = 1 }) => (
  <g>
    {Array.from({ length: count }, (_, i) => {
      const a = (i / count) * Math.PI * 2 + rand(i + seed) * 0.05;
      const r0 = inner + rand(i * 3 + seed) * 120;
      const w = 0.004 + rand(i * 7 + seed) * 0.012;
      const R = 1200;
      const p = (ang: number, r: number) =>
        `${cx + Math.cos(ang) * r},${cy + Math.sin(ang) * r}`;
      return (
        <polygon
          key={i}
          points={`${p(a, r0)} ${p(a - w, R)} ${p(a + w, R)}`}
          fill={color}
          opacity={0.35 + rand(i + 99) * 0.5}
        />
      );
    })}
  </g>
);

/** Four-point "kira-kira" sparkle. */
export const Sparkle: React.FC<{
  x: number;
  y: number;
  s: number;
  color?: string;
  opacity?: number;
}> = ({ x, y, s, color = "#FFFFFF", opacity = 1 }) => (
  <path
    transform={`translate(${x} ${y})`}
    d={`M0 ${-s} Q0 0 ${s} 0 Q0 0 0 ${s} Q0 0 ${-s} 0 Q0 0 0 ${-s} Z`}
    fill={color}
    opacity={opacity}
  />
);

/** Classic anime sweat drop. */
export const SweatDrop: React.FC<{ x: number; y: number; s: number }> = ({
  x,
  y,
  s,
}) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    <path
      d="M0 -30 C 10 -12 18 0 18 10 A 18 18 0 0 1 -18 10 C -18 0 -10 -12 0 -30 Z"
      fill="#BAE6FD"
      stroke="#0369A1"
      strokeWidth={3}
    />
    <ellipse cx={-6} cy={8} rx={4} ry={7} fill="#FFFFFF" />
  </g>
);

/** Jagged impact burst. */
export const Burst: React.FC<{
  cx: number;
  cy: number;
  r: number;
  fill: string;
  stroke?: string;
  spikes?: number;
  seed?: number;
}> = ({ cx, cy, r, fill, stroke, spikes = 18, seed = 2 }) => {
  const pts = Array.from({ length: spikes * 2 }, (_, i) => {
    const a = (i / (spikes * 2)) * Math.PI * 2;
    const rr =
      i % 2
        ? r * (0.55 + rand(i + seed) * 0.12)
        : r * (0.92 + rand(i * 5 + seed) * 0.25);
    return `${cx + Math.cos(a) * rr},${cy + Math.sin(a) * rr}`;
  }).join(" ");
  return (
    <polygon
      points={pts}
      fill={fill}
      stroke={stroke}
      strokeWidth={stroke ? 8 : 0}
      strokeLinejoin="round"
    />
  );
};

/** Cel-shaded cumulus: a shadow mass with a lit cap. */
export const Cloud: React.FC<{
  x: number;
  y: number;
  s: number;
  lit: string;
  shade: string;
}> = ({ x, y, s, lit, shade }) => {
  const puffs = [
    [0, 0, 60],
    [60, -25, 70],
    [130, -5, 55],
    [180, 15, 40],
    [-55, 15, 42],
  ];
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      {puffs.map(([px, py, r], i) => (
        <circle key={`s${i}`} cx={px} cy={py} r={r} fill={shade} />
      ))}
      <rect x={-97} y={15} width={318} height={40} fill={shade} />
      {puffs.map(([px, py, r], i) => (
        <circle
          key={`l${i}`}
          cx={px - r * 0.12}
          cy={py - r * 0.22}
          r={r * 0.82}
          fill={lit}
        />
      ))}
    </g>
  );
};

/** Ghosted onion-skin copies along a path, to show motion in a still. */
export const OnionArrow: React.FC<{ d: string; color?: string }> = ({
  d,
  color = "#0F172A",
}) => (
  <g>
    <defs>
      <marker
        id="arrowhead"
        viewBox="0 0 10 10"
        refX="6"
        refY="5"
        markerWidth="6"
        markerHeight="6"
        orient="auto-start-reverse"
      >
        <path d="M0 0 L10 5 L0 10 z" fill={color} />
      </marker>
    </defs>
    <path
      d={d}
      fill="none"
      stroke={color}
      strokeWidth={4}
      strokeDasharray="10 9"
      markerEnd="url(#arrowhead)"
    />
  </g>
);
