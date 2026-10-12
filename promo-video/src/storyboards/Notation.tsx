// Storyboard notation drawn over a 1080 x 1920 panel: shot size, camera moves
// (red) and character action (orange), the way board artists mark them up.
import { H, W } from "./Kit";

const CAM = "#DC2626";
const ACT = "#F97316";

export const NotationDefs: React.FC = () => (
  <defs>
    <marker id="camHead" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
      <path d="M0 0 L10 5 L0 10 Z" fill={CAM} />
    </marker>
    <marker id="actHead" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
      <path d="M0 0 L10 5 L0 10 Z" fill={ACT} />
    </marker>
  </defs>
);

const Label: React.FC<{ x: number; y: number; text: string; color?: string; anchor?: "start" | "middle" | "end" }> = ({ x, y, text, color = CAM, anchor = "start" }) => (
  <text x={x} y={y} fill={color} fontFamily="Kalam" fontWeight={700} fontSize={56} textAnchor={anchor} stroke="#FFFFFF" strokeWidth={10} paintOrder="stroke">
    {text}
  </text>
);

/** Shot size chip, top-left (WS, MS, CU, ECU, OTS, INSERT...). */
export const Shot: React.FC<{ text: string }> = ({ text }) => (
  <g>
    <rect x={30} y={30} width={40 + text.length * 34} height={84} rx={18} fill="#111827" opacity={0.85} />
    <text x={50} y={92} fill="#FFFFFF" fontFamily="Inter" fontWeight={800} fontSize={54}>
      {text}
    </text>
  </g>
);

/** Push-in: the end frame as a dashed box, arrows from the corners. Pull-out reverses them. */
export const Push: React.FC<{ inset?: number; cy?: number; out?: boolean; label?: string }> = ({ inset = 170, cy = H / 2, out = false, label }) => {
  const w = W - inset * 2;
  const h = (w * H) / W;
  const x0 = inset;
  const y0 = cy - h / 2;
  const corners: [number, number, number, number][] = [
    [40, y0 - 120, x0, y0],
    [W - 40, y0 - 120, x0 + w, y0],
    [40, y0 + h + 120, x0, y0 + h],
    [W - 40, y0 + h + 120, x0 + w, y0 + h],
  ];
  return (
    <g>
      <rect x={x0} y={y0} width={w} height={h} fill="none" stroke={CAM} strokeWidth={8} strokeDasharray="30 20" />
      {corners.map(([ax, ay, bx, by], i) => (
        <line key={i} x1={out ? bx : ax} y1={out ? by : ay} x2={out ? ax : bx} y2={out ? ay : by} stroke={CAM} strokeWidth={10} markerEnd="url(#camHead)" />
      ))}
      <Label x={x0 + 20} y={y0 + h - 30} text={label ?? (out ? "PULL OUT" : "PUSH IN")} />
    </g>
  );
};

/** A camera pan / whip arrow across the frame. */
export const Pan: React.FC<{ y?: number; from?: number; to?: number; label?: string; whip?: boolean }> = ({ y = 1700, from = 120, to = W - 120, label = "PAN", whip = false }) => (
  <g>
    {whip &&
      [-40, 0, 40].map((d) => <line key={d} x1={from} y1={y + d} x2={to - 80} y2={y + d} stroke={CAM} strokeWidth={4} opacity={0.6} />)}
    <line x1={from} y1={y} x2={to} y2={y} stroke={CAM} strokeWidth={12} markerEnd="url(#camHead)" />
    <Label x={(from + to) / 2} y={y - 40} text={whip ? "WHIP PAN" : label} anchor="middle" />
  </g>
);

/** Camera shake: zigzags at the frame edges. */
export const Shake: React.FC<{ label?: string }> = ({ label = "SHAKE" }) => (
  <g stroke={CAM} strokeWidth={8} fill="none">
    <path d="M30 400 l30 -30 l30 30 l30 -30 l30 30" />
    <path d={`M${W - 180} 400 l30 -30 l30 30 l30 -30 l30 30`} />
    <path d="M30 1500 l30 -30 l30 30 l30 -30 l30 30" />
    <path d={`M${W - 180} 1500 l30 -30 l30 30 l30 -30 l30 30`} />
    <Label x={W - 40} y={340} text={label} anchor="end" />
  </g>
);

/** Character action: an orange arrow along a path, with an optional note. */
export const Act: React.FC<{ d: string; label?: string; lx?: number; ly?: number }> = ({ d, label, lx = 0, ly = 0 }) => (
  <g>
    <path d={d} stroke={ACT} strokeWidth={10} fill="none" strokeLinecap="round" markerEnd="url(#actHead)" />
    {label && <Label x={lx} y={ly} text={label} color={ACT} />}
  </g>
);
