// Hands holding a phone up close, seen from the front: palms cupping the lower half,
// fingertips peeking from behind the side edges, and two thumbs over the screen
// that can reach for keys while typing (thumb tips are given in phone coordinates).
import { LINE, Phone } from "../storyboards/Kit";
import type { Pt } from "../cast/Rig";

const SKIN = "#F2C3A2";
const SKIN_SHADE = "#DFA585";
const SLEEVE = "#EFE6D8";

/** A capsule from a to b (a fingertip peeking from behind the phone). */
const Digit: React.FC<{ a: Pt; b: Pt; r: number }> = ({ a, b, r }) => (
  <g>
    <path d={`M${a[0]} ${a[1]} L${b[0]} ${b[1]}`} stroke={LINE} strokeWidth={r * 2 + 5} strokeLinecap="round" />
    <path d={`M${a[0]} ${a[1]} L${b[0]} ${b[1]}`} stroke={SKIN} strokeWidth={r * 2} strokeLinecap="round" />
  </g>
);

/**
 * A thumb from its base joint to its tip: two tapered segments with the knuckle bowed
 * outward (as a real thumb arcs over a screen), a nail at the tip.
 */
const Thumb: React.FC<{ base: Pt; tip: Pt; s: number; side: -1 | 1 }> = ({ base, tip, s, side }) => {
  const dx = tip[0] - base[0];
  const dy = tip[1] - base[1];
  const len = Math.hypot(dx, dy) || 1;
  const nx = -dy / len;
  const ny = dx / len;
  const bow = side * 0.16 * len;
  const knuckle: Pt = [base[0] + dx * 0.5 + nx * bow, base[1] + dy * 0.5 + ny * bow];
  const seg = (a: Pt, b: Pt, ra: number, rb: number) => {
    const ux = (b[0] - a[0]) / (Math.hypot(b[0] - a[0], b[1] - a[1]) || 1);
    const uy = (b[1] - a[1]) / (Math.hypot(b[0] - a[0], b[1] - a[1]) || 1);
    const [px, py] = [-uy, ux];
    return `M${a[0] + px * ra} ${a[1] + py * ra} L${b[0] + px * rb} ${b[1] + py * rb} A${rb} ${rb} 0 0 0 ${b[0] - px * rb} ${b[1] - py * rb} L${a[0] - px * ra} ${a[1] - py * ra} A${ra} ${ra} 0 0 0 ${a[0] + px * ra} ${a[1] + py * ra} Z`;
  };
  const ang = Math.atan2(tip[1] - knuckle[1], tip[0] - knuckle[0]);
  const nail: Pt = [tip[0] - Math.cos(ang) * 10 * s, tip[1] - Math.sin(ang) * 10 * s];
  return (
    <g stroke={LINE} strokeWidth={4} strokeLinejoin="round">
      <path d={seg(base, knuckle, 34 * s, 27 * s)} fill={SKIN} />
      <path d={seg(knuckle, tip, 27 * s, 23 * s)} fill={SKIN} />
      <path d={`M${knuckle[0] - 10 * s} ${knuckle[1]} q${10 * s} ${-6 * s} ${20 * s} 0`} fill="none" strokeWidth={2} opacity={0.4} />
      <ellipse cx={nail[0]} cy={nail[1]} rx={13 * s} ry={10 * s} transform={`rotate(${(ang * 180) / Math.PI} ${nail[0]} ${nail[1]})`} fill="#FBE3D2" strokeWidth={1.6} />
      <path d={`M${base[0]} ${base[1]} L${knuckle[0]} ${knuckle[1]}`} stroke={SKIN_SHADE} strokeWidth={10 * s} opacity={0.25} fill="none" />
    </g>
  );
};

/** The right hand coming in from below to tap with the index finger (tip in screen coordinates). */
const PointingHand: React.FC<{ tip: Pt; s: number }> = ({ tip, s }) => {
  const [x, y] = tip;
  return (
    <g transform={`translate(${x} ${y}) rotate(-24) scale(${s})`}>
      <path d="M40 300 L-40 300 L-60 900 L120 900 Z" fill={SLEEVE} stroke={LINE} strokeWidth={4} />
      <path d="M-26 0 C-26 -16 26 -16 26 0 L30 150 C70 150 96 170 100 220 C104 290 60 330 0 330 C-60 330 -80 280 -78 220 L-74 120 C-74 100 -40 100 -34 130 Z" fill={SKIN} stroke={LINE} strokeWidth={4} strokeLinejoin="round" />
      <ellipse cx={0} cy={6} rx={15} ry={11} fill="#FBE3D2" stroke={LINE} strokeWidth={1.6} />
      <path d="M30 190 q30 6 50 -6 M24 240 q34 8 56 -4" stroke={LINE} strokeWidth={2.5} fill="none" opacity={0.5} />
    </g>
  );
};

export const HandsOnPhone: React.FC<{
  x: number;
  y: number;
  w: number;
  rot?: number;
  glow?: string;
  /** Thumb tips in phone coordinates (0..w, 0..h); null tucks the thumb on the bezel. */
  thumbL?: Pt | null;
  thumbR?: Pt | null;
  /** Tap with the right index finger here instead (screen coordinates); the left hand holds. */
  point?: Pt | null;
  children: React.ReactNode;
}> = ({ x, y, w, rot = 0, glow, thumbL = null, thumbR = null, point = null, children }) => {
  const h = w * (2400 / 1080);
  const s = w / 680;
  // Thumb joints sit at the lower corners; resting thumbs lie along the bezel.
  const baseL: Pt = [x + 18 * s, y + h * 0.985];
  const baseR: Pt = [x + w - 18 * s, y + h * 0.985];
  const reachMax = w * 0.62;
  const clampTip = (base: Pt, t: Pt): Pt => {
    const d = Math.hypot(t[0] - base[0], t[1] - base[1]);
    return d <= reachMax ? t : [base[0] + ((t[0] - base[0]) * reachMax) / d, base[1] + ((t[1] - base[1]) * reachMax) / d];
  };
  const tipL: Pt = clampTip(baseL, thumbL ? [x + thumbL[0], y + thumbL[1]] : [x + 30 * s, y + h * 0.8]);
  const tipR: Pt = clampTip(baseR, thumbR ? [x + thumbR[0], y + thumbR[1]] : [x + w - 30 * s, y + h * 0.8]);
  const tips = (side: -1 | 1) =>
    [0, 1, 2].map((i) => {
      const ex = side < 0 ? x - 6 * s : x + w + 6 * s;
      const ty = y + h * (0.6 + i * 0.075);
      return <Digit key={i} a={[ex - side * 30 * s, ty + 8 * s]} b={[ex + side * 4 * s, ty]} r={17 * s} />;
    });
  return (
    <g transform={`rotate(${rot} ${x + w / 2} ${y + h / 2})`}>
      {/* fingers behind the phone, tips showing at the edges */}
      {tips(-1)}
      {!point && tips(1)}
      <Phone x={x} y={y} w={w} glow={glow}>
        {children}
      </Phone>
      {/* palms and wrists below, sleeves */}
      {(point ? ([-1] as const) : ([-1, 1] as const)).map((side) => {
        const cx = side < 0 ? x + 40 * s : x + w - 40 * s;
        return (
          <g key={side}>
            <path d={`M${cx - 120 * s} ${y + h + 260 * s} L${cx - 90 * s} ${y + h * 0.98} L${cx + 90 * s} ${y + h * 0.98} L${cx + 120 * s} ${y + h + 260 * s} Z`} fill={SLEEVE} stroke={LINE} strokeWidth={4} />
            <path
              d={`M${cx - 100 * s} ${y + h * 0.99} C${cx - 110 * s} ${y + h * 0.88} ${cx - side * 30 * s} ${y + h * 0.84} ${cx + side * 20 * s} ${y + h * 0.86} C${cx + 90 * s} ${y + h * 0.88} ${cx + 100 * s} ${y + h * 0.95} ${cx + 96 * s} ${y + h * 1.0} Z`}
              fill={SKIN}
              stroke={LINE}
              strokeWidth={4}
            />
          </g>
        );
      })}
      <Thumb base={baseL} tip={tipL} s={s} side={-1} />
      {!point && <Thumb base={baseR} tip={tipR} s={s} side={1} />}
      {point && <PointingHand tip={[x + point[0], y + point[1]]} s={s * 1.05} />}
    </g>
  );
};

/** Where a key sits on the AskFlow keyboard, in phone coordinates (w = phone width). */
export const keyPos = (c: string, w: number): Pt => {
  const k = w / 470;
  const h = w * (2400 / 1080);
  const rows = ["qwertyuiop", "asdfghjkl", "zxcvbnm"];
  const keyW = 45 * k;
  const top = h - (44 + 4 * 62) * k;
  if (c === " ") return [w / 2, top + 3 * 62 * k + 26 * k];
  for (let r = 0; r < 3; r++) {
    const i = rows[r].indexOf(c.toLowerCase());
    if (i >= 0) {
      const rowW = rows[r].length * keyW;
      return [(w - rowW) / 2 + i * keyW + keyW / 2, top + r * 62 * k + 26 * k];
    }
  }
  return [w / 2, top + 62 * k];
};
