// Landscape (1920 x 1080) sets for "The 2 a.m. Idea": a wider bedroom (wall,
// window, nightstand, headboard, pillow, bed) and a wider rooftop café. The art
// is the vertical video's (src/bedtime/Props.tsx), re-laid out across the width.
import { useId } from "react";
import { rand } from "../../anim";
import { LH, LW } from "../kit";

const LINE = "#4A2E28";

/**
 * The bedroom. `bx` is the bed's centre (Meera's head), `top` the top of the
 * headboard; `wx` the window's left edge and `nx` the nightstand's (either can
 * sit off-frame). Night (morning 0) or morning (1).
 */
export const LBedroom: React.FC<{
  morning: number;
  bx: number;
  top?: number;
  wx?: number;
  nx?: number;
}> = ({ morning, bx, top = 430, wx = bx + 520, nx = bx - 880 }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const m = morning > 0.5;
  const floorY = 880;
  // The window: the vertical one (560..980 x 90..590), scaled and clipped.
  const ws = 0.92;
  const wy = 70;
  return (
    <g>
      <defs>
        <linearGradient id={`wall${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={m ? "#F3DCC6" : "#2A2F55"} />
          <stop offset="100%" stopColor={m ? "#E8C9AE" : "#1B1F3E"} />
        </linearGradient>
        <linearGradient id={`sky${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={m ? "#9CCBF2" : "#0B1030"} />
          <stop offset="100%" stopColor={m ? "#FFE3B8" : "#2E2A6A"} />
        </linearGradient>
        <radialGradient id={`sun${id}`}>
          <stop offset="0%" stopColor="#FFF4D6" stopOpacity={0.9} />
          <stop offset="100%" stopColor="#FFF4D6" stopOpacity={0} />
        </radialGradient>
        <clipPath id={`win${id}`}>
          <rect x={560} y={90} width={420} height={500} rx={18} />
        </clipPath>
      </defs>
      <rect width={LW} height={LH} fill={`url(#wall${id})`} />
      {/* Floor, seen either side of the bed */}
      <rect y={floorY} width={LW} height={LH - floorY} fill={m ? "#C99A72" : "#2A2446"} />
      <rect y={floorY - 14} width={LW} height={16} fill={m ? "#E2BE98" : "#363058"} stroke={LINE} strokeWidth={3} />
      {Array.from({ length: 4 }, (_, i) => (
        <line key={i} x1={0} x2={LW} y1={floorY + 40 + i * 48} y2={floorY + 40 + i * 48} stroke={m ? "#B4845E" : "#221D3C"} strokeWidth={3} />
      ))}

      {/* Window */}
      <g transform={`translate(${wx} ${wy}) scale(${ws}) translate(-560 -90)`}>
        <rect x={560} y={90} width={420} height={500} rx={18} fill={`url(#sky${id})`} />
        <g clipPath={`url(#win${id})`}>
          {m ? (
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
            <rect key={i} x={566 + i * 46} y={430 - rand(i + 9) * 120} width={40} height={170} fill={m ? "#C9B6D6" : "#141836"} />
          ))}
          {!m &&
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
        </g>
        <rect x={560} y={90} width={420} height={500} rx={18} fill="none" stroke={LINE} strokeWidth={6} />
        <path d="M770 90 L770 590 M560 340 L980 340" stroke={LINE} strokeWidth={6} />
        <rect x={540} y={590} width={460} height={22} rx={6} fill={m ? "#E2BE98" : "#3A3466"} stroke={LINE} strokeWidth={4} />
        {/* Curtains */}
        <path d="M520 70 C560 250 540 450 580 620 L520 620 Z" fill={m ? "#E7B57A" : "#3A3466"} stroke={LINE} strokeWidth={4} />
        <path d="M1020 70 C980 250 1000 450 960 620 L1020 620 Z" fill={m ? "#E7B57A" : "#3A3466"} stroke={LINE} strokeWidth={4} />
        <rect x={500} y={58} width={540} height={14} rx={7} fill={m ? "#8A5A3C" : "#2A2240"} stroke={LINE} strokeWidth={3} />
      </g>

      {/* Nightstand */}
      <g transform={`translate(${nx} ${top + 150})`}>
        <rect x={20} y={240} width={16} height={floorY - top - 150 - 240} fill={m ? "#8A5A3C" : "#2A2240"} />
        <rect x={204} y={240} width={16} height={floorY - top - 150 - 240} fill={m ? "#8A5A3C" : "#2A2240"} />
        <rect x={0} y={0} width={240} height={250} rx={14} fill={m ? "#B07A55" : "#3A2E4E"} stroke={LINE} strokeWidth={4} />
        <rect x={-8} y={-6} width={256} height={22} rx={8} fill={m ? "#C48C64" : "#46385E"} stroke={LINE} strokeWidth={4} />
        <rect x={22} y={44} width={196} height={84} rx={10} fill="none" stroke={LINE} strokeWidth={3} />
        <rect x={22} y={146} width={196} height={84} rx={10} fill="none" stroke={LINE} strokeWidth={3} />
        <circle cx={120} cy={86} r={7} fill={LINE} />
        <circle cx={120} cy={188} r={7} fill={LINE} />
      </g>

      {/* Headboard */}
      <rect x={bx - 540} y={top} width={1080} height={280} rx={60} fill={m ? "#B07A55" : "#4A3A5E"} stroke={LINE} strokeWidth={5} />
      <rect x={bx - 500} y={top + 38} width={1000} height={200} rx={40} fill={m ? "#C48C64" : "#56456C"} />
      {/* Bed */}
      <rect x={bx - 600} y={top + 240} width={1200} height={LH} rx={20} fill={m ? "#F4EBDD" : "#3E4470"} stroke={LINE} strokeWidth={4} />
      {/* Pillow (the vertical one, 780 x 370, scaled) */}
      <path
        transform={`translate(${bx} ${top + 34}) scale(0.82 0.78) translate(-540 -626)`}
        d="M150 700 C140 640 220 620 540 626 C860 620 940 640 930 700 C946 800 940 880 930 930 C940 990 860 1000 540 994 C220 1000 140 990 150 930 C140 880 134 800 150 700 Z"
        fill={m ? "#FFFDF8" : "#8E93C4"}
        stroke={LINE}
        strokeWidth={5}
      />
    </g>
  );
};

/** The quilt across the bed, `y` its top edge at the sides (it bows up ~38 px). */
export const LQuilt: React.FC<{
  morning: number;
  bx: number;
  y: number;
  half?: number;
  breathe?: number;
}> = ({ morning, bx, y, half = 680, breathe = 0 }) => {
  const m = morning > 0.5;
  const x0 = bx - half;
  const x1 = bx + half;
  const fill = m ? "#E9A7A0" : "#5B5F9A";
  const hem = m ? "#FBE7E2" : "#7C80BC";
  const stitch = m ? "#D98A82" : "#4A4E86";
  const n = Math.round((x1 - x0) / 170);
  return (
    <g transform={`translate(0 ${breathe * 5})`}>
      <path
        d={`M${x0} ${y + 20} C${x0 + 260} ${y - 50} ${x1 - 260} ${y - 50} ${x1} ${y + 20} C${x1 + 24} ${y + 200} ${x1 + 30} ${y + 400} ${x1 + 40} ${LH + 40} L${x0 - 40} ${LH + 40} C${x0 - 30} ${y + 400} ${x0 - 24} ${y + 200} ${x0} ${y + 20} Z`}
        fill={fill}
        stroke={LINE}
        strokeWidth={5}
      />
      <path d={`M${x0 + 4} ${y + 60} C${x0 + 260} ${y - 10} ${x1 - 260} ${y - 10} ${x1 - 4} ${y + 60}`} stroke={hem} strokeWidth={22} fill="none" />
      {Array.from({ length: n }, (_, i) => (
        <path key={i} d={`M${x0 + 40 + i * 170} ${y + 130} q60 30 120 0`} stroke={stitch} strokeWidth={4} fill="none" />
      ))}
      {Array.from({ length: n - 1 }, (_, i) => (
        <path key={i} d={`M${x0 + 100 + i * 210} ${y + 290} q50 -40 100 0 q50 40 100 0`} stroke={stitch} strokeWidth={4} fill="none" />
      ))}
      {/* Fold shading where it drapes over the edges */}
      <path d={`M${x0 + 30} ${y + 120} C${x0 + 20} ${y + 260} ${x0 + 14} ${y + 380} ${x0 + 10} ${LH}`} stroke={stitch} strokeWidth={4} fill="none" />
      <path d={`M${x1 - 30} ${y + 120} C${x1 - 20} ${y + 260} ${x1 - 14} ${y + 380} ${x1 - 10} ${LH}`} stroke={stitch} strokeWidth={4} fill="none" />
    </g>
  );
};

/** A bedside digital clock (sits on the nightstand: x, y = bottom centre). */
export const AlarmClock: React.FC<{
  x: number;
  y: number;
  text: string;
  night?: boolean;
  buzz?: number;
  ringing?: boolean;
}> = ({ x, y, text, night = false, buzz = 0, ringing = false }) => (
  <g transform={`translate(${x + buzz} ${y}) rotate(${buzz * 0.8})`}>
    {night && <ellipse cx={0} cy={-62} rx={150} ry={90} fill="#FDE68A" opacity={0.12} />}
    <rect x={-80} y={-8} width={20} height={10} rx={3} fill={LINE} />
    <rect x={60} y={-8} width={20} height={10} rx={3} fill={LINE} />
    <rect x={-100} y={-118} width={200} height={112} rx={20} fill="#0F172A" stroke={LINE} strokeWidth={5} />
    <rect x={-84} y={-102} width={168} height={80} rx={10} fill="#1E1B30" />
    <text
      x={0}
      y={-43}
      textAnchor="middle"
      fontFamily="Inter"
      fontWeight={700}
      fontSize={54}
      fill="#FDE68A"
      style={night ? { filter: "drop-shadow(0 0 8px rgba(253,230,138,0.8))" } : undefined}
    >
      {text}
    </text>
    {ringing &&
      [0, 1, 2].map((i) => (
        <g key={i}>
          <path d={`M${118 + i * 18} -100 q16 40 0 80`} stroke={LINE} strokeWidth={6} fill="none" strokeLinecap="round" />
          <path d={`M${-118 - i * 18} -100 q-16 40 0 80`} stroke={LINE} strokeWidth={6} fill="none" strokeLinecap="round" />
        </g>
      ))}
  </g>
);

/** Rooftop café at golden hour across 1920 px: string lights, shelves, sign, chalkboard. */
export const LRooftop: React.FC<{ t: number }> = ({ t }) => {
  const floorY = 760;
  return (
    <g>
      <defs>
        <linearGradient id="lrtSky" x1="0" y1="0" x2="0" y2={floorY} gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#5B3FA8" />
          <stop offset="40%" stopColor="#E26AA8" />
          <stop offset="72%" stopColor="#FB923C" />
          <stop offset="100%" stopColor="#FDE68A" />
        </linearGradient>
        <radialGradient id="lrtBulb">
          <stop offset="0%" stopColor="#FFF4C2" stopOpacity={0.9} />
          <stop offset="100%" stopColor="#FBBF24" stopOpacity={0} />
        </radialGradient>
      </defs>
      <rect width={LW} height={LH} fill="url(#lrtSky)" />
      <circle cx={1230} cy={560} r={90} fill="#FFF1C9" opacity={0.9} />
      {Array.from({ length: 22 }, (_, i) => (
        <rect key={i} x={i * 90 - 10} y={470 - rand(i + 70) * 200} width={86} height={400} fill="#7A3F86" opacity={0.85} />
      ))}
      {/* String lights */}
      {[0, 1].map((row) => {
        const y0 = 190 + row * 120;
        const pts = Array.from({ length: 17 }, (_, i) => [i * 120, y0 + Math.sin((i / 16) * Math.PI) * 70] as const);
        return (
          <g key={row}>
            <path d={pts.map(([px, py], i) => `${i ? "L" : "M"}${px} ${py}`).join(" ")} stroke={LINE} strokeWidth={3} fill="none" />
            {pts.map(([px, py], i) => (
              <g key={i}>
                <circle cx={px} cy={py + 18} r={34} fill="url(#lrtBulb)" opacity={0.6 + 0.4 * Math.sin(t / 6 + i + row)} />
                <circle cx={px} cy={py + 18} r={9} fill="#FFF4C2" stroke={LINE} strokeWidth={2} />
              </g>
            ))}
          </g>
        );
      })}
      {/* Railing + floor */}
      <rect y={floorY} width={LW} height={LH - floorY} fill="#B07A55" />
      {Array.from({ length: 5 }, (_, i) => (
        <line key={i} x1={0} y1={floorY + 30 + i * 66} x2={LW} y2={floorY + 30 + i * 66} stroke="#8E5E40" strokeWidth={4} />
      ))}
      <rect y={620} width={LW} height={20} fill="#5A3A2E" />
      {Array.from({ length: 25 }, (_, i) => (
        <rect key={i} x={20 + i * 80} y={620} width={14} height={floorY - 620} fill="#5A3A2E" />
      ))}
      {/* Bookshelf */}
      <g transform="translate(70 330)">
        <rect x={0} y={0} width={330} height={440} fill="#7A4E36" stroke={LINE} strokeWidth={5} />
        {[0, 1, 2].map((r) => (
          <g key={r}>
            <rect x={10} y={120 + r * 120} width={310} height={10} fill="#5A3A2E" />
            {Array.from({ length: 10 }, (_, i) => (
              <rect
                key={i}
                x={18 + i * 30}
                y={40 + r * 120 - rand(i + r * 9) * 20}
                width={24}
                height={80 + rand(i + r * 9) * 20}
                fill={["#BE123C", "#2563EB", "#D97706", "#0D9488", "#7C3AED", "#F4EBDD"][(i + r) % 6]}
                stroke={LINE}
                strokeWidth={2}
              />
            ))}
          </g>
        ))}
      </g>
      {/* Sign */}
      <g transform="rotate(-3 1500 330)">
        <line x1={1330} y1={200} x2={1360} y2={272} stroke={LINE} strokeWidth={4} />
        <line x1={1670} y1={200} x2={1640} y2={272} stroke={LINE} strokeWidth={4} />
        <rect x={1270} y={270} width={460} height={124} rx={16} fill="#5A3A2E" stroke={LINE} strokeWidth={5} />
        <text x={1500} y={352} textAnchor="middle" fontFamily="Playfair Display" fontStyle="italic" fontSize={64} fill="#FDE68A">
          Chai &amp; Chapters
        </text>
      </g>
      {/* Chalkboard */}
      <g transform="translate(1500 560) rotate(4)">
        <path d="M0 0 L240 0 L262 360 L-22 360 Z" fill="#2E3B2E" stroke="#7A4E36" strokeWidth={12} />
        <text x={120} y={92} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={46} fill="#F8F4EC">
          <tspan x={120}>Swap a</tspan>
          <tspan x={120} dy={58}>
            book,
          </tspan>
          <tspan x={120} dy={58}>
            get a
          </tspan>
          <tspan x={120} dy={58}>
            chai
          </tspan>
        </text>
      </g>
    </g>
  );
};

/** A café table edge in the foreground (top at y). */
export const CafeTable: React.FC<{ x0: number; x1: number; y: number }> = ({ x0, x1, y }) => (
  <g>
    <rect x={x0} y={y} width={x1 - x0} height={LH - y + 20} fill="#8E5E40" stroke={LINE} strokeWidth={5} />
    <rect x={x0 - 14} y={y - 10} width={x1 - x0 + 28} height={30} rx={10} fill="#A86E48" stroke={LINE} strokeWidth={5} />
    <line x1={x0 + 30} y1={y + 60} x2={x1 - 30} y2={y + 60} stroke="#7A4E36" strokeWidth={4} />
  </g>
);
