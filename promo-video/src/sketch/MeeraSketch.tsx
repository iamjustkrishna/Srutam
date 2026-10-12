// Meera in a warm, hand-drawn illustration style (expression sheet look):
// messy bun with a ribbon, wispy strands, big dark glossy eyes, peach
// blush, sketchy brown line art, cream knit sweater. Local box 600 x 800.
import { useId } from "react";
import { rand } from "../anim";
import { quad, ribbon, type Pt } from "../options/shapes";

export type SketchExpr =
  | "surprised"
  | "worried"
  | "smug"
  | "laugh"
  | "sleep"
  | "drowsy"
  | "blank";

const LINE = "#4A2E28";
const HAIR = "#2B1E1B";
const HAIR_HI = "#5A4038";
const SKIN = "#F2C3A2";
const BLUSH = "#EE8E78";
const SWEATER = "#EFE6D8";

type Props = {
  x: number;
  y: number;
  s: number;
  expr: SketchExpr;
  turn?: number;
  gaze?: [number, number];
  hand?: boolean;
  /** Close the eyes for a blink without changing the expression. */
  blink?: boolean;
  /** 0..1 how open the mouth is (lip-sync for "drowsy" whispering). */
  talk?: number;
  /** Hair sway, -1..1. */
  sway?: number;
};

export const MeeraSketch: React.FC<Props> = ({
  x,
  y,
  s,
  expr,
  turn = 0,
  gaze = [0, 0],
  hand = false,
  blink = false,
  talk = 0,
  sway = 0,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const u = (k: string) => `${k}${id}`;
  const url = (k: string) => `url(#${k}${id})`;
  const t = turn * 16; // feature shift for a slight head turn

  const eye = (cx: number, cy: number, left: boolean) => {
    const w = 82 * (left ? 1 - turn * 0.1 : 1 + turn * 0.1);
    const open =
      expr === "surprised"
        ? 1.18
        : expr === "smug"
          ? 0.72
          : expr === "drowsy"
            ? 0.6
            : 1;
    const h = 50 * open;
    const out = left ? -1 : 1; // direction of the outer corner
    const xo = cx + (out * w) / 2;
    const xi = cx - (out * w) / 2;
    if (expr === "sleep" || blink) {
      return (
        <g>
          <path
            d={`M${xi} ${cy + 2} Q${cx} ${cy + 14} ${xo} ${cy}`}
            stroke="#24140F"
            strokeWidth={6}
            fill="none"
            strokeLinecap="round"
          />
          <path
            d={`M${xo} ${cy} l${out * 12} 4 M${xo - out * 10} ${cy + 6} l${out * 8} 8`}
            stroke="#24140F"
            strokeWidth={3}
            strokeLinecap="round"
          />
        </g>
      );
    }
    if (expr === "laugh") {
      return (
        <g>
          <path
            d={`M${xi} ${cy + 6} Q${cx} ${cy - 22} ${xo} ${cy + 4}`}
            stroke={LINE}
            strokeWidth={6.5}
            fill="none"
            strokeLinecap="round"
          />
          <path
            d={`M${xo} ${cy + 4} l${out * 12} ${-8}`}
            stroke={LINE}
            strokeWidth={4}
            strokeLinecap="round"
          />
        </g>
      );
    }
    const lidY =
      expr === "smug" || expr === "drowsy" ? cy - h * 0.28 : cy - h * 0.62;
    const upper = `M${xi} ${cy + 2} C${xi + out * w * 0.18} ${lidY - h * 0.1} ${xo - out * w * 0.2} ${lidY - h * 0.14} ${xo} ${cy - 4}`;
    const white = `${upper} C${xo - out * w * 0.18} ${cy + h * 0.5} ${xi + out * w * 0.2} ${cy + h * 0.52} ${xi} ${cy + 2} Z`;
    const r =
      (expr === "surprised" || expr === "blank" ? 20 : 26) *
      (left ? 1 - turn * 0.06 : 1 + turn * 0.06);
    const ix = cx + gaze[0] * 14;
    const iy = cy + 3 + gaze[1] * 7;
    const k = left ? "l" : "r";
    return (
      <g>
        <clipPath id={u(`e${k}`)}>
          <path d={white} />
        </clipPath>
        <path d={white} fill="#FFFBF6" />
        <g clipPath={url(`e${k}`)}>
          <circle cx={ix} cy={iy} r={r} fill={url("iris")} />
          <circle cx={ix} cy={iy} r={r * 0.45} fill="#120A08" />
          <path
            d={`${upper} L${xo} ${cy - h} L${xi} ${cy - h} Z`}
            fill="#8E6A62"
            opacity={0.0}
          />
          <path
            d={`M${xi} ${cy} C${xi + out * w * 0.18} ${lidY - h * 0.06} ${xo - out * w * 0.2} ${lidY - h * 0.1} ${xo} ${cy - 6} L${xo} ${cy + 4} C${xo - out * w * 0.2} ${lidY + h * 0.35} ${xi + out * w * 0.18} ${lidY + h * 0.4} ${xi} ${cy + 10} Z`}
            fill="#7A5048"
            opacity={0.35}
          />
          <circle
            cx={ix - r * 0.38}
            cy={iy - r * 0.4}
            r={r * 0.3}
            fill="#FFFFFF"
          />
          <circle
            cx={ix + r * 0.42}
            cy={iy + r * 0.38}
            r={r * 0.12}
            fill="#FFFFFF"
          />
          {expr === "worried" && (
            <circle
              cx={ix + r * 0.1}
              cy={iy + r * 0.55}
              r={r * 0.18}
              fill="#FFFFFF"
              opacity={0.85}
            />
          )}
        </g>
        {/* Heavy upper lash line with an outer flick */}
        <path
          d={upper}
          stroke="#24140F"
          strokeWidth={7.5}
          fill="none"
          strokeLinecap="round"
        />
        <path
          d={`M${xo - out * 6} ${cy - 6} q${out * 10} -4 ${out * 18} -14`}
          stroke={LINE}
          strokeWidth={4}
          fill="none"
          strokeLinecap="round"
        />
        <path
          d={`M${xo - out * 16} ${lidY + 2} q${out * 8} -6 ${out * 14} -12`}
          stroke={LINE}
          strokeWidth={3}
          fill="none"
          strokeLinecap="round"
        />
        {/* Lower lid: light, with two tiny lashes */}
        <path
          d={`M${xo - out * 4} ${cy + 2} C${xo - out * w * 0.2} ${cy + h * 0.46} ${cx} ${cy + h * 0.5} ${xi + out * w * 0.25} ${cy + h * 0.4}`}
          stroke={LINE}
          strokeWidth={1.8}
          fill="none"
          opacity={0.6}
        />
        <path
          d={`M${xo - out * w * 0.2} ${cy + h * 0.38} l${out * 4} 6 M${xo - out * w * 0.34} ${cy + h * 0.44} l${out * 3} 6`}
          stroke={LINE}
          strokeWidth={1.6}
          opacity={0.6}
        />
        {/* Crease */}
        <path
          d={`M${xi + out * w * 0.2} ${lidY - h * 0.2} Q${cx} ${lidY - h * 0.55} ${xo - out * w * 0.1} ${lidY - h * 0.12}`}
          stroke={LINE}
          strokeWidth={1.4}
          fill="none"
          opacity={0.45}
        />
      </g>
    );
  };

  // Brows: [inner y offset, outer y offset]
  const brow = (cx: number, left: boolean) => {
    const out = left ? -1 : 1;
    const [inner, outer]: [number, number] =
      expr === "surprised"
        ? [-26, -22]
        : expr === "worried"
          ? [-24, -4]
          : expr === "smug"
            ? left
              ? [-6, -8]
              : [-14, -20]
            : expr === "blank"
              ? [-20, -20]
              : expr === "sleep" || expr === "drowsy"
                ? [-6, -6]
                : [-12, -12];
    const yi = 312 + inner;
    const yo = 312 + outer;
    const xi = cx - out * 30;
    const xo = cx + out * 32;
    return (
      <path
        d={`M${xi} ${yi + 3} Q${cx} ${Math.min(yi, yo) - 10} ${xo} ${yo + 2} Q${cx} ${Math.min(yi, yo) - 4} ${xi} ${yi + 7} Z`}
        fill={LINE}
        opacity={0.85}
      />
    );
  };

  const mx = 300 + t;
  const mouth =
    expr === "sleep" ? (
      <g transform={`translate(${mx} 452)`}>
        <path
          d="M-14 0 Q0 9 14 0"
          stroke={LINE}
          strokeWidth={2.6}
          fill="none"
          strokeLinecap="round"
        />
        <path
          d="M-10 4 Q0 10 10 4"
          stroke="#E08E84"
          strokeWidth={4}
          fill="none"
          strokeLinecap="round"
          opacity={0.7}
        />
      </g>
    ) : expr === "drowsy" ? (
      <g transform={`translate(${mx} 452)`}>
        <ellipse
          rx={7 + talk * 5}
          ry={2 + talk * 8}
          fill="#7A2E2E"
          stroke={LINE}
          strokeWidth={2}
        />
        <path
          d="M-13 -5 Q0 -9 13 -5"
          stroke="#B5605A"
          strokeWidth={2.5}
          fill="none"
          opacity={0.7}
        />
      </g>
    ) : expr === "blank" ? (
      <g transform={`translate(${mx} 454)`}>
        <path
          d="M-12 0 L12 0"
          stroke={LINE}
          strokeWidth={2.6}
          strokeLinecap="round"
        />
        <path
          d="M-8 4 Q0 8 8 4"
          stroke="#E08E84"
          strokeWidth={3}
          fill="none"
          strokeLinecap="round"
          opacity={0.6}
        />
      </g>
    ) : expr === "surprised" ? (
      <g transform={`translate(${mx} 450)`}>
        <ellipse rx={12} ry={15} fill="#7A2E2E" stroke={LINE} strokeWidth={2} />
        <ellipse cy={8} rx={8} ry={5} fill="#D86A6A" />
        <path
          d="M-15 -14 Q0 -20 15 -14"
          stroke="#B5605A"
          strokeWidth={2.5}
          fill="none"
          opacity={0.7}
        />
      </g>
    ) : expr === "worried" ? (
      <g transform={`translate(${mx} 454)`}>
        <path d="M-15 0 Q-7 -6 0 -3 Q7 -6 15 0 Q0 3 -15 0 Z" fill="#C9706A" />
        <path d="M-12 3 Q0 13 12 3 Q0 6 -12 3 Z" fill="#E08E84" />
        <path
          d="M-15 0 Q0 3 15 0"
          stroke={LINE}
          strokeWidth={2}
          fill="none"
          opacity={0.7}
        />
      </g>
    ) : expr === "smug" ? (
      <g transform={`translate(${mx} 452)`}>
        <path
          d="M-28 -2 Q-4 14 30 -10"
          stroke={LINE}
          strokeWidth={3.2}
          fill="none"
          strokeLinecap="round"
        />
        <path
          d="M-20 4 Q0 14 20 2"
          stroke="#E08E84"
          strokeWidth={5}
          fill="none"
          strokeLinecap="round"
          opacity={0.8}
        />
        <path
          d="M32 -12 q4 4 2 9"
          stroke={LINE}
          strokeWidth={2}
          fill="none"
          strokeLinecap="round"
          opacity={0.7}
        />
      </g>
    ) : (
      <g transform={`translate(${mx} 448)`}>
        <path
          d="M-30 -4 Q0 40 30 -4 Z"
          fill="#7A2E2E"
          stroke={LINE}
          strokeWidth={2.5}
          strokeLinejoin="round"
        />
        <path d="M-26 -2 L26 -2 L22 6 L-22 6 Z" fill="#FFFBF6" />
        <ellipse cy={20} rx={12} ry={6} fill="#D86A6A" />
      </g>
    );

  // Hair strands
  const bangs: Pt[][] = [
    quad([298, 196], [236, 214], [196, 332]),
    quad([296, 200], [252, 236], [226, 314]),
    quad([294, 204], [270, 244], [256, 290]),
    quad([302, 196], [364, 214], [404, 332]),
    quad([304, 200], [348, 236], [374, 314]),
    quad([306, 204], [330, 244], [344, 290]),
  ];
  const locks: Pt[][] = [
    Array.from({ length: 30 }, (_, i) => {
      const k = i / 29;
      return [180 - k * 24 + Math.sin(k * 7) * 10, 300 + k * 290] as Pt;
    }),
    Array.from({ length: 30 }, (_, i) => {
      const k = i / 29;
      return [420 + k * 24 + Math.sin(k * 6 + 1) * 10, 300 + k * 300] as Pt;
    }),
  ];
  // Flyaways lie along the silhouette (tangential), gently curved
  const flyaways = Array.from({ length: 16 }, (_, i) => {
    const a = -Math.PI * 0.92 + (i / 15) * Math.PI * 0.84;
    const r0 = 160 + rand(i * 7) * 14;
    const sx = 300 + Math.cos(a) * r0;
    const sy = 300 + Math.sin(a) * r0 * 1.1;
    const tang = a + Math.PI / 2 + (i % 2 ? 0.35 : -0.35);
    const len = 40 + rand(i * 3) * 50;
    const ex = sx + Math.cos(tang) * len * (i % 2 ? 1 : -1);
    const ey = sy + Math.sin(tang) * len * (i % 2 ? 1 : -1);
    const bend = 14 + rand(i * 9) * 14 + sway * 10 * (i % 2 ? 1 : -1);
    return `M${sx} ${sy} Q${(sx + ex) / 2 + Math.cos(a) * bend} ${(sy + ey) / 2 + Math.sin(a) * bend} ${ex} ${ey}`;
  });

  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <defs>
        <radialGradient id={u("iris")} cx="45%" cy="65%" r="65%">
          <stop offset="0%" stopColor="#6B4A3A" />
          <stop offset="60%" stopColor="#3A251C" />
          <stop offset="100%" stopColor="#1A0F0B" />
        </radialGradient>
        <radialGradient id={u("blush")}>
          <stop offset="0%" stopColor={BLUSH} stopOpacity={0.75} />
          <stop offset="100%" stopColor={BLUSH} stopOpacity={0} />
        </radialGradient>
        <linearGradient id={u("skin")} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#F7D2B4" />
          <stop offset="100%" stopColor={SKIN} />
        </linearGradient>
        <filter id={u("soft")} x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation={7} />
        </filter>
        <filter id={u("sketch")} x="-5%" y="-5%" width="110%" height="110%">
          <feTurbulence
            type="fractalNoise"
            baseFrequency={0.05}
            numOctaves={2}
            seed={5}
            result="n"
          />
          <feDisplacementMap
            in="SourceGraphic"
            in2="n"
            scale={2.6}
            xChannelSelector="R"
            yChannelSelector="G"
          />
        </filter>
        <clipPath id={u("face")}>
          <path d={FACE} />
        </clipPath>
      </defs>

      <g filter={url("sketch")}>
        {/* Back hair, tucked behind the face */}
        <path
          d="M160 360 C140 230 214 132 300 128 C388 124 462 220 440 360 C434 396 420 418 402 430 L198 430 C180 418 166 396 160 360 Z"
          fill={HAIR}
        />
        {/* Messy bun and ribbon */}
        <g>
          <circle cx={392} cy={120} r={58} fill={HAIR} />
          <circle cx={360} cy={98} r={36} fill={HAIR} />
          {[0, 1, 2, 3, 4, 5].map((i) => (
            <path
              key={i}
              d={`M${340 + i * 14} ${70 + (i % 2) * 10} q${20 - i * 4} -30 ${40 - i * 3} ${-6 + i * 4}`}
              stroke={HAIR}
              strokeWidth={4}
              fill="none"
              strokeLinecap="round"
            />
          ))}
          <path
            d="M350 92 Q392 70 432 104 M356 130 Q394 112 430 140"
            stroke={HAIR_HI}
            strokeWidth={4}
            fill="none"
            opacity={0.8}
            strokeLinecap="round"
          />
          <g transform="translate(420 168) rotate(12)">
            <path
              d="M-2 0 C-34 -30 -62 -8 -44 14 C-32 26 -14 12 -2 0 Z"
              fill="#F4EBDD"
              stroke={LINE}
              strokeWidth={2.2}
            />
            <path
              d="M2 0 C34 -30 62 -6 44 16 C32 28 14 12 2 0 Z"
              fill="#F4EBDD"
              stroke={LINE}
              strokeWidth={2.2}
            />
            <path
              d="M-26 -6 C-30 0 -30 6 -26 10 M26 -4 C30 2 30 8 26 12"
              stroke="#CDBFA9"
              strokeWidth={2}
              fill="none"
            />
            <path
              d="M-4 6 C-12 30 -10 52 -18 72 L-6 72 C-2 54 0 32 4 8 Z"
              fill="#F4EBDD"
              stroke={LINE}
              strokeWidth={2}
            />
            <path
              d="M4 6 C14 28 18 48 16 70 L28 66 C28 46 20 26 8 6 Z"
              fill="#E9DDCA"
              stroke={LINE}
              strokeWidth={2}
            />
            <ellipse
              cx={0}
              cy={3}
              rx={8}
              ry={10}
              fill="#E9DDCA"
              stroke={LINE}
              strokeWidth={2}
            />
          </g>
        </g>

        {/* Neck, collarbones, scoop-neck sweater */}
        <g transform="translate(0 -44)">
          <path
            d="M266 520 C266 548 262 572 252 598 C240 608 230 614 220 620 L380 620 C370 614 360 608 348 598 C338 572 334 548 334 520 Z"
            fill={url("skin")}
            stroke={LINE}
            strokeWidth={2.2}
          />
          <path
            d="M220 618 C250 650 276 666 300 668 C324 666 350 650 380 618 Z"
            fill={url("skin")}
          />
          <ellipse
            cx={300}
            cy={546}
            rx={46}
            ry={20}
            fill="#C9826A"
            opacity={0.6}
            filter={url("soft")}
          />
          <path
            d="M246 628 Q266 636 286 642 M314 642 Q334 636 354 628"
            stroke={LINE}
            strokeWidth={1.8}
            fill="none"
            opacity={0.5}
            strokeLinecap="round"
          />
          <path
            d="M20 800 C30 730 90 672 188 642 C204 634 214 626 222 618 C250 650 276 666 300 668 C324 666 350 650 378 618 C386 626 396 634 412 642 C510 672 570 730 580 800 Q540 792 500 802 Q440 794 380 804 Q300 794 220 804 Q140 794 60 802 Q40 800 20 800 Z"
            fill={SWEATER}
            stroke={LINE}
            strokeWidth={2.4}
          />
          <path
            d="M222 618 C250 650 276 666 300 668 C324 666 350 650 378 618 L392 634 C360 670 330 686 300 688 C270 686 240 670 208 634 Z"
            fill="#E4D8C6"
            stroke={LINE}
            strokeWidth={2}
          />
          {Array.from({ length: 24 }, (_, i) => {
            const k = i / 23;
            const px = 214 + k * 172;
            const py = 628 + Math.sin(k * Math.PI) * 48;
            return (
              <line
                key={i}
                x1={px}
                y1={py - 8}
                x2={px + (k - 0.5) * 4}
                y2={py + 12}
                stroke="#B9A88F"
                strokeWidth={1.5}
              />
            );
          })}
          {Array.from({ length: 28 }, (_, i) => (
            <path
              key={i}
              d={`M${50 + i * 19} ${720 + (i % 3) * 10} q3 30 0 60`}
              stroke="#CDBFA9"
              strokeWidth={1.4}
              fill="none"
              opacity={0.7}
            />
          ))}
          <path
            d="M188 642 C170 680 160 720 156 790 M412 642 C430 680 440 720 444 790"
            stroke="#CDBFA9"
            strokeWidth={2}
            fill="none"
          />
        </g>
        {/* Ears with small gold studs */}
        <path
          d="M182 318 C160 312 154 348 166 372 C174 388 188 392 194 384 Z"
          fill={SKIN}
          stroke={LINE}
          strokeWidth={2.2}
        />
        <path
          d="M418 318 C440 312 446 348 434 372 C426 388 412 392 406 384 Z"
          fill={SKIN}
          stroke={LINE}
          strokeWidth={2.2}
        />
        <ellipse cx={174} cy={352} rx={6} ry={9} fill={BLUSH} opacity={0.7} />
        <ellipse cx={426} cy={352} rx={6} ry={9} fill={BLUSH} opacity={0.7} />
        <circle
          cx={172}
          cy={386}
          r={4}
          fill="#F5B940"
          stroke="#7C2D12"
          strokeWidth={1}
        />
        <circle
          cx={428}
          cy={386}
          r={4}
          fill="#F5B940"
          stroke="#7C2D12"
          strokeWidth={1}
        />

        {/* Messy side volume, half over the ears */}
        <path
          d="M184 236 C150 262 140 318 156 360 C164 380 174 384 180 372 C174 340 176 300 190 270 Z"
          fill={HAIR}
        />
        <path
          d="M416 236 C450 262 460 318 444 360 C436 380 426 384 420 372 C426 340 424 300 410 270 Z"
          fill={HAIR}
        />
        <path
          d="M160 300 C150 330 152 354 162 372 M440 300 C450 330 448 354 438 372"
          stroke={HAIR_HI}
          strokeWidth={3}
          fill="none"
          opacity={0.6}
          strokeLinecap="round"
        />

        {/* Face */}
        <path d={FACE} fill={url("skin")} />
        <g clipPath={url("face")}>
          <path
            d="M404 260 C424 340 410 430 350 500 L440 500 L440 240 Z"
            fill="#D99A80"
            opacity={0.45}
            filter={url("soft")}
          />
          <path
            d="M170 240 L430 240 L430 300 C390 288 350 312 300 300 C250 314 210 290 170 306 Z"
            fill="#C9826A"
            opacity={0.45}
            filter={url("soft")}
          />
          <ellipse cx={226 + t} cy={404} rx={50} ry={28} fill={url("blush")} />
          <ellipse cx={374 + t} cy={404} rx={50} ry={28} fill={url("blush")} />
          <ellipse
            cx={300 + t * 1.2}
            cy={398}
            rx={74}
            ry={13}
            fill={url("blush")}
            opacity={0.55}
          />
          <ellipse
            cx={300 + t * 1.2}
            cy={410}
            rx={13}
            ry={9}
            fill={url("blush")}
          />
          {[0, 1, 2].map((i) => (
            <circle
              key={i}
              cx={232 + t + i * 9}
              cy={392 + (i % 2) * 6}
              r={1.6}
              fill="#FFFFFF"
              opacity={0.8}
            />
          ))}
        </g>
        <path
          d={FACE}
          fill="none"
          stroke={LINE}
          strokeWidth={2.2}
          strokeLinejoin="round"
        />

        {brow(243 + t, true)}
        {brow(357 + t, false)}
        {eye(243 + t, 362, true)}
        {eye(357 + t, 362, false)}
        {/* Nose */}
        <path
          d={`M${302 + t * 1.3} 392 Q${309 + t * 1.3} 406 ${299 + t * 1.3} 412`}
          stroke={LINE}
          strokeWidth={2}
          fill="none"
          strokeLinecap="round"
          opacity={0.75}
        />
        <ellipse
          cx={298 + t * 1.3}
          cy={415}
          rx={6}
          ry={2.2}
          fill="#C9826A"
          opacity={0.5}
        />
        {talk > 0.12 && !["drowsy", "sleep", "laugh"].includes(expr) ? (
          // Speaking: an open mouth sized by the voice's loudness (lip-sync)
          <g transform={`translate(${mx} 452)`}>
            <ellipse
              rx={10 + talk * 8}
              ry={3 + talk * 13}
              fill="#7A2E2E"
              stroke={LINE}
              strokeWidth={2.2}
            />
            <ellipse cy={2 + talk * 7} rx={6 + talk * 4} ry={2 + talk * 4} fill="#D86A6A" />
          </g>
        ) : expr === "laugh" && talk > 0.12 ? (
          <g transform={`translate(${mx} 448) scale(${0.8 + talk * 0.4}) translate(${-mx} -448)`}>
            {mouth}
          </g>
        ) : (
          mouth
        )}

        {/* Bangs (curtain), side locks, flyaways */}
        <path
          d="M178 312 C170 220 230 150 300 148 C370 146 430 220 422 312 C406 262 372 220 300 204 C228 220 194 262 178 312 Z"
          fill={HAIR}
        />
        {bangs.map((pts, i) => {
          const r = ribbon(
            pts,
            (k) =>
              16 *
                Math.sin(Math.PI * Math.min(1, k * 1.3 + 0.2)) *
                (1 - 0.65 * k) +
              1.5,
          );
          return <polygon key={i} points={r.outline} fill={HAIR} />;
        })}
        {locks.map((pts, i) => {
          const r = ribbon(pts, (k) => 5 * (1 - 0.7 * k) + 1);
          return <polygon key={i} points={r.outline} fill={HAIR} />;
        })}
        <g fill="none" strokeLinecap="round">
          {flyaways.map((d, i) => (
            <path
              key={i}
              d={d}
              stroke={HAIR}
              strokeWidth={1.6 + rand(i) * 1.2}
            />
          ))}
          {[
            "M240 190 Q270 170 300 168",
            "M318 170 Q350 176 372 196",
            "M214 250 Q222 222 238 204",
          ].map((d) => (
            <path
              key={d}
              d={d}
              stroke={HAIR_HI}
              strokeWidth={5}
              opacity={0.75}
            />
          ))}
          {["M270 160 C240 200 222 250 218 300"].map((d) => (
            <path key={d} d={d} stroke={HAIR} strokeWidth={2.2} />
          ))}
        </g>

        {/* Sweater-paw hand resting at her neck */}
        {hand && (
          <g transform="translate(0 -44)">
            <path
              d="M600 800 C566 720 506 650 432 604 L392 640 C454 688 500 744 520 800 Z"
              fill={SWEATER}
              stroke={LINE}
              strokeWidth={2.4}
            />
            <path
              d="M432 604 L392 640 L374 618 L414 584 Z"
              fill="#E4D8C6"
              stroke={LINE}
              strokeWidth={2}
            />
            {[0, 1, 2, 3, 4].map((i) => (
              <line
                key={i}
                x1={378 + i * 8}
                y1={620 - i * 7}
                x2={394 + i * 8}
                y2={638 - i * 7}
                stroke="#B9A88F"
                strokeWidth={1.5}
              />
            ))}
            {[0, 1, 2].map((i) => (
              <ellipse
                key={i}
                cx={384 + i * 14}
                cy={590 - i * 14}
                rx={10}
                ry={15}
                fill={SKIN}
                stroke={LINE}
                strokeWidth={2}
                transform={`rotate(-40 ${384 + i * 14} ${590 - i * 14})`}
              />
            ))}
          </g>
        )}
      </g>
    </g>
  );
};

const FACE =
  "M178 300 C176 372 192 428 226 470 C250 500 278 518 300 520 C322 518 350 500 374 470 C408 428 424 372 422 300 C414 222 362 186 300 186 C238 186 186 222 178 300 Z";
