// Set pieces for the 16:9 restaging of "Nani's Laddoos" (and the shared end
// treatment both landscape story cuts use): full-width versions of the Kit props
// that hard-code the 1080 x 1920 panel, plus the "scene card + lettering" end.
import { useId } from "react";
import { Jar, LINE } from "../../storyboards/Kit";
import { LH, LW } from "../kit";

/** A counter / table top across the whole 1920 width, from `y` to the bottom. */
export const LTable: React.FC<{ y: number; color?: string; top?: string; x0?: number; x1?: number }> = ({
  y,
  color = "#8B5E3C",
  top = "#A9744D",
  x0 = -40,
  x1 = LW + 40,
}) => (
  <g>
    <rect x={x0} y={y} width={x1 - x0} height={LH - y + 40} fill={color} />
    <rect x={x0} y={y} width={x1 - x0} height={40} fill={top} stroke={LINE} strokeWidth={5} />
  </g>
);

/** Nani's shelf of jars (gur, besan, ghee), on the wall from `x0` to the right edge. */
export const LShelf: React.FC<{ x0: number; y: number; xs: [number, number, number] }> = ({ x0, y, xs }) => (
  <g>
    <rect x={x0} y={y} width={LW - x0 + 40} height={26} fill="#8B5E3C" stroke={LINE} strokeWidth={5} />
    <path d={`M${x0 + 60} ${y + 26} l0 44 l44 -44 M${LW - 120} ${y + 26} l0 44 l44 -44`} stroke="#6B4528" strokeWidth={10} fill="none" />
    <Jar x={xs[0]} y={y} label="Gur" fill="#B7791F" />
    <Jar x={xs[1]} y={y} label="Besan" fill="#F6D58E" s={0.9} />
    <Jar x={xs[2]} y={y} label="Ghee" fill="#FDE68A" s={0.85} />
  </g>
);

/** Steel ladles and a spoon hanging from a rail. */
export const Ladles: React.FC<{ x: number; y: number; s?: number }> = ({ x, y, s = 1 }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    <rect x={-20} y={-8} width={340} height={16} rx={8} fill="#8B5E3C" stroke={LINE} strokeWidth={4} />
    {[
      [40, 210, 34],
      [130, 250, 0],
      [220, 190, 28],
    ].map(([cx, len, r], i) => (
      <g key={i}>
        <circle cx={cx} cy={8} r={9} fill="none" stroke="#6B7280" strokeWidth={5} />
        <rect x={cx - 7} y={16} width={14} height={len} rx={7} fill="#CBD5E1" stroke={LINE} strokeWidth={3} />
        {r ? (
          <path d={`M${cx - r} ${16 + len} a${r} ${r} 0 0 0 ${2 * r} 0 Z`} fill="#CBD5E1" stroke={LINE} strokeWidth={4} />
        ) : (
          <ellipse cx={cx} cy={26 + len} rx={22} ry={30} fill="#CBD5E1" stroke={LINE} strokeWidth={4} />
        )}
      </g>
    ))}
    <rect x={262} y={20} width={44} height={150} rx={10} fill="#F59E0B" stroke={LINE} strokeWidth={4} />
    <rect x={268} y={8} width={32} height={18} rx={4} fill="#B91C1C" stroke={LINE} strokeWidth={3} />
  </g>
);

/**
 * The 16:9 end: the closing scene shrinks into a rounded card on the left
 * (`k` 0 -> 1) over the brand navy, and `lettering` (drawn in the right column,
 * centred on END_X) fades up beside it. `scene` must paint its own background.
 */
const CARD = 0.6;
export const END_X = 1560;
export const EndSplit: React.FC<{ k: number; scene: React.ReactNode; lettering: React.ReactNode; letteringK?: number }> = ({
  k,
  scene,
  lettering,
  letteringK = k,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const s = 1 - (1 - CARD) * k; // card: 1152 x 648
  const x = 80 * k;
  const y = ((LH - LH * CARD) / 2) * k;
  const r = (36 * k) / s;
  return (
    <g>
      <rect width={LW} height={LH} fill="#050714" opacity={Math.min(1, k * 1.4)} />
      <defs>
        <clipPath id={`card${id}`}>
          <rect x={0} y={0} width={LW} height={LH} rx={r} />
        </clipPath>
      </defs>
      <g transform={`translate(${x} ${y}) scale(${s})`}>
        <g clipPath={`url(#card${id})`}>{scene}</g>
        {k > 0 && <rect x={0} y={0} width={LW} height={LH} rx={r} fill="none" stroke="#FDE7C7" strokeWidth={10 / s} opacity={k} />}
      </g>
      <g opacity={letteringK} transform={`translate(${(1 - letteringK) * 60} 0)`}>
        {lettering}
      </g>
    </g>
  );
};

/** Wordmark and an italic tagline (one line per entry) centred on END_X. */
export const Lettering: React.FC<{ tagline: string[]; y?: number }> = ({ tagline, y = 470 }) => (
  <g>
    <text x={END_X} y={y} textAnchor="middle" fontFamily="Playfair Display" fontSize={140} fill="#F8FAFC">
      Srutam
    </text>
    <text x={END_X} y={y + 120} textAnchor="middle" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={60} fill="#93C5FD">
      {tagline.map((l, i) => (
        <tspan key={i} x={END_X} dy={i ? 76 : 0}>
          {l}
        </tspan>
      ))}
    </text>
  </g>
);
