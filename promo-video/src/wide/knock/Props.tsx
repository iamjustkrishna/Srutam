// Set pieces for the 16:9 restaging of "Knock Knock": full-width versions of the
// vertical's street, sofa and front door (src/storyboards/knock.tsx, Kit.tsx draw
// them for the 1080 x 1920 panel), a popcorn bowl and an SVG date chip.
import { Door, LINE } from "../../storyboards/Kit";
import { FONT } from "../../theme";
import { LH, LW } from "../kit";

/** Dusk street across the width; `pan` slides the buildings (parallax), `auto` is the rickshaw's x. */
export const LStreet: React.FC<{ pan: number; auto: number }> = ({ pan, auto }) => (
  <g>
    <g transform={`translate(${pan} 0)`}>
      {Array.from({ length: 11 }, (_, i) => (
        <rect key={i} x={i * 230 - 330} y={300 + (i % 2) * 120 + (i % 3) * 30} width={210} height={900} fill={i % 2 ? "#3B2F5C" : "#4C3B6E"} />
      ))}
      {Array.from({ length: 11 * 3 }, (_, j) => {
        const i = j % 11;
        const r = Math.floor(j / 11);
        return (
          <g key={j}>
            <rect x={i * 230 - 330 + 40} y={380 + (i % 2) * 120 + (i % 3) * 30 + r * 150} width={40} height={60} fill="#FCD34D" opacity={(i + r) % 4 === 0 ? 0.25 : 0.7} />
            <rect x={i * 230 - 330 + 130} y={380 + (i % 2) * 120 + (i % 3) * 30 + r * 150} width={40} height={60} fill="#FCD34D" opacity={(i + r) % 3 === 0 ? 0.25 : 0.7} />
          </g>
        );
      })}
    </g>
    <rect x={-20} y={940} width={LW + 40} height={LH - 900} fill="#2E2448" />
    {/* auto-rickshaw */}
    <g transform={`translate(${auto} 760)`}>
      <path d="M0 120 C0 20 60 -20 160 -20 L260 -20 L280 120 Z" fill="#FACC15" stroke={LINE} strokeWidth={6} />
      <path d="M0 120 L300 120 L300 170 L0 170 Z" fill="#15803D" stroke={LINE} strokeWidth={6} />
      <circle cx={60} cy={180} r={36} fill="#1F2937" />
      <circle cx={250} cy={180} r={36} fill="#1F2937" />
    </g>
  </g>
);

/** The sofa: a back from `y` and the seat below it, between x0 and x1. */
export const LSofa: React.FC<{ y: number; x0?: number; x1?: number }> = ({ y, x0 = -40, x1 = LW + 40 }) => (
  <g>
    <rect x={x0} y={y} width={x1 - x0} height={420} rx={60} fill="#7C3AED" stroke={LINE} strokeWidth={6} opacity={0.85} />
    <rect x={x0} y={y + 160} width={x1 - x0} height={LH} fill="#6D28D9" stroke={LINE} strokeWidth={6} />
  </g>
);

/** The front door in its frame, standing on the floor, centred on `cx`; `dx` shakes the door. */
export const FrontDoor: React.FC<{ cx: number; y: number; h: number; dx?: number }> = ({ cx, y, h, dx = 0 }) => {
  const w = h * 0.46;
  const x = cx - w / 2;
  return (
    <g>
      <rect x={-20} y={y + h} width={LW + 40} height={LH - y - h + 20} fill="#1A0F0A" />
      <rect x={x - 40} y={y - 40} width={w + 80} height={h + 40} fill="#4A2E1C" stroke={LINE} strokeWidth={8} />
      <g transform={`translate(${dx} 0)`}>
        <Door x={x} y={y} w={w} h={h} />
      </g>
      <rect x={cx - 230} y={y + h + 8} width={460} height={34} rx={10} fill="#7A2E1F" stroke={LINE} strokeWidth={5} />
    </g>
  );
};

/** A wall sconce with a warm pool of light. */
export const Sconce: React.FC<{ x: number; y: number }> = ({ x, y }) => (
  <g>
    <defs>
      <radialGradient id="sconceGlow">
        <stop offset="0%" stopColor="#FDBA74" stopOpacity={0.45} />
        <stop offset="100%" stopColor="#FDBA74" stopOpacity={0} />
      </radialGradient>
    </defs>
    <ellipse cx={x} cy={y + 40} rx={260} ry={300} fill="url(#sconceGlow)" />
    <rect x={x - 10} y={y + 30} width={20} height={60} fill="#3F2A1D" stroke={LINE} strokeWidth={4} />
    <path d={`M${x - 46} ${y + 34} L${x + 46} ${y + 34} L${x + 30} ${y - 30} L${x - 30} ${y - 30} Z`} fill="#FCD9A8" stroke={LINE} strokeWidth={5} />
  </g>
);

/** A bowl of popcorn. */
export const Popcorn: React.FC<{ x: number; y: number; rot?: number; s?: number }> = ({ x, y, rot = 0, s = 1 }) => (
  <g transform={`translate(${x} ${y}) rotate(${rot}) scale(${s})`}>
    <path d="M-120 0 C-110 90 110 90 120 0 Z" fill="#DC2626" stroke={LINE} strokeWidth={5} />
    {Array.from({ length: 9 }, (_, i) => (
      <circle key={i} cx={-90 + i * 22} cy={-10 - (i % 3) * 16} r={20} fill="#FEF3C7" stroke={LINE} strokeWidth={2} />
    ))}
  </g>
);

/** A dark date chip (burned-in caption style) centred on (cx, y). */
export const DateChip: React.FC<{ cx: number; y: number; text: string; opacity?: number }> = ({ cx, y, text, opacity = 1 }) => (
  <foreignObject x={cx - 400} y={y} width={800} height={120} opacity={opacity}>
    <div style={{ display: "flex", justifyContent: "center" }}>
      <div
        style={{
          background: "rgba(15,23,42,0.8)",
          color: "#FFF",
          fontFamily: FONT.sans,
          fontWeight: 700,
          fontSize: 42,
          lineHeight: 1.2,
          padding: "12px 32px",
          borderRadius: 22,
        }}
      >
        {text}
      </div>
    </div>
  </foreignObject>
);
