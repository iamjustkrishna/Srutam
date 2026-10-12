// Rhubarb Lip Sync mouth shapes (the Hanna-Barbera / Preston Blair set) in the
// sketch style, centred on (0, 0) at a face's mouth position. Shapes:
//   A  M, B, P (lips pressed)       B  K, S, T, EE (clenched teeth, slightly open)
//   C  EH, AE (open)                D  AA (wide open)
//   E  AO, ER (slightly rounded)    F  UW, OW, W (puckered)
//   G  F, V (teeth on lower lip)    H  L (tongue up)       X  rest
export type Viseme = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "X";

const LINE = "#4A2E28";
const INSIDE = "#7A2E2E";
const TEETH = "#FFFBF6";
const TONGUE = "#D86A6A";
const LIP = "#E08E84";

export const Mouth: React.FC<{ shape: Viseme; x: number; y: number; s?: number }> = ({ shape, x, y, s = 1 }) => {
  const open = (rx: number, ry: number, teeth = true, tongue = true, tongueUp = false) => (
    <g>
      <ellipse rx={rx} ry={ry} fill={INSIDE} stroke={LINE} strokeWidth={2.4} />
      {teeth && (
        <path d={`M${-rx * 0.82} ${-ry * 0.55} Q0 ${-ry * 0.95} ${rx * 0.82} ${-ry * 0.55} L${rx * 0.7} ${-ry * 0.2} Q0 ${-ry * 0.45} ${-rx * 0.7} ${-ry * 0.2} Z`} fill={TEETH} />
      )}
      {tongue && !tongueUp && <ellipse cy={ry * 0.5} rx={rx * 0.6} ry={ry * 0.38} fill={TONGUE} />}
      {tongueUp && <ellipse cy={-ry * 0.2} rx={rx * 0.42} ry={ry * 0.45} fill={TONGUE} />}
      <path d={`M${-rx * 0.6} ${ry + 5} Q0 ${ry + 11} ${rx * 0.6} ${ry + 5}`} stroke={LIP} strokeWidth={4} fill="none" strokeLinecap="round" opacity={0.7} />
    </g>
  );
  const body = (() => {
    switch (shape) {
      case "A":
        return (
          <g>
            <path d="M-18 0 Q0 5 18 0" stroke={LINE} strokeWidth={3} fill="none" strokeLinecap="round" />
            <path d="M-12 6 Q0 12 12 6" stroke={LIP} strokeWidth={5} fill="none" strokeLinecap="round" opacity={0.8} />
          </g>
        );
      case "B":
        return (
          <g>
            <path d="M-22 -3 Q0 -7 22 -3 Q0 9 -22 -3 Z" fill={INSIDE} stroke={LINE} strokeWidth={2.4} strokeLinejoin="round" />
            <path d="M-18 -3 Q0 -6 18 -3 L16 2 Q0 0 -16 2 Z" fill={TEETH} />
            <path d="M-12 9 Q0 14 12 9" stroke={LIP} strokeWidth={4} fill="none" strokeLinecap="round" opacity={0.7} />
          </g>
        );
      case "C":
        return open(20, 12);
      case "D":
        return open(24, 20);
      case "E":
        return open(15, 13, false);
      case "F":
        return (
          <g>
            <ellipse rx={11} ry={10} fill={LIP} stroke={LINE} strokeWidth={2.4} />
            <ellipse rx={5} ry={5} fill={INSIDE} />
          </g>
        );
      case "G":
        return (
          <g>
            <path d="M-20 -2 Q0 -6 20 -2 Q0 8 -20 -2 Z" fill={INSIDE} stroke={LINE} strokeWidth={2.4} />
            <path d="M-15 -3 L15 -3 L13 4 L-13 4 Z" fill={TEETH} />
            <path d="M-16 5 Q0 13 16 5" stroke={LIP} strokeWidth={6} fill="none" strokeLinecap="round" />
          </g>
        );
      case "H":
        return open(18, 13, true, false, true);
      default:
        return null;
    }
  })();
  return <g transform={`translate(${x} ${y}) scale(${s})`}>{body}</g>;
};

/** The mouth shape at frame f for a voice line that starts at `at` (X when silent). */
export const visemeAt = (sync: Record<string, string>, file: string, at: number, f: number): Viseme => {
  const s = sync[file];
  const i = Math.round(f - at);
  return (s && i >= 0 && i < s.length ? s[i] : "X") as Viseme;
};
