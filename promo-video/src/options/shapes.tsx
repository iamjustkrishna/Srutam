// Shared drawing helpers for the character options.
import { useId } from "react";

export type Pt = [number, number];

/** A tapered ribbon along a polyline: tails, trunks, dragon bodies. */
export const ribbon = (pts: Pt[], width: (t: number) => number) => {
  const n = pts.length - 1;
  const left: string[] = [];
  const right: string[] = [];
  const spine: { x: number; y: number; nx: number; ny: number; t: number }[] =
    [];
  pts.forEach(([x, y], i) => {
    const [ax, ay] = pts[Math.max(0, i - 1)];
    const [bx, by] = pts[Math.min(n, i + 1)];
    const len = Math.hypot(bx - ax, by - ay) || 1;
    const nx = -(by - ay) / len;
    const ny = (bx - ax) / len;
    const t = i / n;
    const w = width(t);
    left.push(`${x + nx * w},${y + ny * w}`);
    right.push(`${x - nx * w},${y - ny * w}`);
    spine.push({ x, y, nx, ny, t });
  });
  return { outline: [...left, ...right.reverse()].join(" "), spine };
};

/** Points along a quadratic bezier. */
export const quad = (a: Pt, c: Pt, b: Pt, n = 40): Pt[] =>
  Array.from({ length: n + 1 }, (_, i) => {
    const t = i / n;
    return [
      (1 - t) ** 2 * a[0] + 2 * (1 - t) * t * c[0] + t ** 2 * b[0],
      (1 - t) ** 2 * a[1] + 2 * (1 - t) * t * c[1] + t ** 2 * b[1],
    ];
  });

/** Soft outer glow filter + aura gradient, shared by the light spirits. */
export const useGlow = (color: string) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const defs = (
    <>
      <filter id={`g${id}`} x="-50%" y="-50%" width="200%" height="200%">
        <feGaussianBlur stdDeviation={7} result="b" />
        <feMerge>
          <feMergeNode in="b" />
          <feMergeNode in="SourceGraphic" />
        </feMerge>
      </filter>
      <radialGradient id={`a${id}`}>
        <stop offset="0%" stopColor={color} stopOpacity={0.45} />
        <stop offset="100%" stopColor={color} stopOpacity={0} />
      </radialGradient>
    </>
  );
  return { id, defs, filter: `url(#g${id})`, aura: `url(#a${id})` };
};

/** Glossy cartoon eye: iris gradient, pupil, two highlights. */
export const Eye: React.FC<{
  cx: number;
  cy: number;
  rx: number;
  ry: number;
  iris: string;
  pupil?: string;
  slit?: boolean;
  lash?: string;
}> = ({ cx, cy, rx, ry, iris, pupil = "#0B1026", slit = false, lash }) => (
  <g>
    <ellipse
      cx={cx}
      cy={cy}
      rx={rx}
      ry={ry}
      fill={iris}
      stroke={pupil}
      strokeWidth={2.5}
    />
    <ellipse
      cx={cx - rx * 0.15}
      cy={cy + ry * 0.05}
      rx={slit ? rx * 0.18 : rx * 0.45}
      ry={ry * 0.7}
      fill={pupil}
    />
    <circle
      cx={cx - rx * 0.4}
      cy={cy - ry * 0.4}
      r={Math.max(2, rx * 0.32)}
      fill="#FFFFFF"
    />
    <circle
      cx={cx + rx * 0.35}
      cy={cy + ry * 0.4}
      r={Math.max(1.2, rx * 0.14)}
      fill="#FFFFFF"
    />
    {lash && (
      <path
        d={`M${cx - rx * 1.1} ${cy - ry * 0.6} Q${cx} ${cy - ry * 1.4} ${cx + rx * 1.15} ${cy - ry * 0.7}`}
        stroke={lash}
        strokeWidth={4}
        fill="none"
        strokeLinecap="round"
      />
    )}
  </g>
);

/** A thought: a golden firefly (same as the Shru board). */
export const Glowfly: React.FC<{ x: number; y: number; r: number }> = ({
  x,
  y,
  r,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <g transform={`translate(${x} ${y})`}>
      <defs>
        <radialGradient id={`f${id}`}>
          <stop offset="0%" stopColor="#FFF7D6" />
          <stop offset="35%" stopColor="#FBBF24" stopOpacity={0.95} />
          <stop offset="100%" stopColor="#FBBF24" stopOpacity={0} />
        </radialGradient>
      </defs>
      <circle r={r * 4} fill={`url(#f${id})`} />
      <ellipse
        cx={-r * 0.9}
        cy={-r * 0.6}
        rx={r * 0.9}
        ry={r * 0.45}
        fill="#FFFFFF"
        opacity={0.45}
        transform={`rotate(-30 ${-r * 0.9} ${-r * 0.6})`}
      />
      <ellipse
        cx={r * 0.9}
        cy={-r * 0.6}
        rx={r * 0.9}
        ry={r * 0.45}
        fill="#FFFFFF"
        opacity={0.45}
        transform={`rotate(30 ${r * 0.9} ${-r * 0.6})`}
      />
      <circle r={r * 0.6} fill="#FFF7D6" />
    </g>
  );
};
