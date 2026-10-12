// Supporting cast, drawn to match MeeraSketch: same face shape, warm brown line
// art, glossy layered eyes, soft face shading and peach blush, in the same
// 600 x 800 local box so they can stand beside her at the same scale.
import { useId } from "react";
import { Mouth, type Viseme } from "../cast/Mouth";

const LINE = "#4A2E28";
const LASH = "#24140F";

export type Hair = "nani" | "short" | "bob" | "ponytail" | "curly" | "bald" | "side";
export type Outfit = "saree" | "shirt" | "hoodie" | "kurta" | "blazer" | "tee";
export type PExpr =
  | "smile"
  | "neutral"
  | "laugh"
  | "surprised"
  | "worried"
  | "angry"
  | "sideeye"
  | "sleepy"
  | "talk"
  | "smug";

type Props = {
  x: number;
  y: number;
  s: number;
  hair: Hair;
  outfit: Outfit;
  expr: PExpr;
  hairColor?: string;
  skin?: string;
  cloth?: string;
  accent?: string;
  glasses?: boolean;
  bindi?: boolean;
  beard?: boolean;
  moustache?: boolean;
  /** Smile lines and softer cheeks (Nani, the professor). */
  aged?: boolean;
  /** Upper-lash flicks (defaults on for long-haired characters). */
  lashes?: boolean;
  /** Iris offset, -1..1 per axis. */
  gaze?: [number, number];
  /** Slight head turn, -1..1: features shift, the far ear hides. */
  turn?: number;
  /** Rhubarb mouth shape while speaking. */
  viseme?: Viseme;
  blink?: boolean;
  flip?: boolean;
};

const FACE =
  "M182 300 C180 372 196 428 228 470 C252 500 278 518 300 520 C322 518 348 500 372 470 C404 428 420 372 418 300 C410 222 360 188 300 188 C240 188 190 222 182 300 Z";

const shade = (hex: string, k: number) => {
  const n = parseInt(hex.slice(1), 16);
  const c = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => Math.max(0, Math.min(255, Math.round(v * k))));
  return `#${c.map((v) => v.toString(16).padStart(2, "0")).join("")}`;
};

export const Person: React.FC<Props> = ({
  x,
  y,
  s,
  hair,
  outfit,
  expr,
  hairColor = "#2B1E1B",
  skin = "#E8B48F",
  cloth = "#6B8CC7",
  accent = "#F4EBDD",
  glasses = false,
  bindi = false,
  beard = false,
  moustache = false,
  aged = false,
  lashes,
  gaze = [0, 0],
  turn = 0,
  viseme,
  blink = false,
  flip = false,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const u = (k: string) => `${k}${id}`;
  const url = (k: string) => `url(#${k}${id})`;
  const gx = (flip ? -gaze[0] : gaze[0]) * 12;
  const gy = gaze[1] * 7;
  const t = turn * 16;
  const hairHi = shade(hairColor, hair === "nani" ? 1.12 : 1.9);
  const longHair = lashes ?? ["nani", "bob", "ponytail", "side"].includes(hair);

  const eye = (cx: number, cy: number, left: boolean) => {
    const out = left ? -1 : 1;
    const w = 72 * (left ? 1 - turn * 0.1 : 1 + turn * 0.1);
    const xo = cx + (out * w) / 2;
    const xi = cx - (out * w) / 2;
    if (expr === "laugh" || expr === "sleepy" || blink) {
      const up = expr === "laugh" ? -18 : 10;
      return (
        <g>
          <path d={`M${xi} ${cy + 4} Q${cx} ${cy + up} ${xo} ${cy + 2}`} stroke={LASH} strokeWidth={6} fill="none" strokeLinecap="round" />
          {longHair && <path d={`M${xo} ${cy + 2} l${out * 12} -6`} stroke={LASH} strokeWidth={3.5} strokeLinecap="round" />}
        </g>
      );
    }
    const open = expr === "surprised" ? 1.2 : expr === "sideeye" || expr === "angry" || expr === "smug" ? 0.62 : 1;
    const h = 44 * open;
    const lidY = cy - h * 0.6;
    const upper = `M${xi} ${cy + 2} C${xi + out * w * 0.2} ${lidY - h * 0.1} ${xo - out * w * 0.2} ${lidY - h * 0.12} ${xo} ${cy - 4}`;
    const white = `${upper} C${xo - out * w * 0.2} ${cy + h * 0.5} ${xi + out * w * 0.2} ${cy + h * 0.5} ${xi} ${cy + 2} Z`;
    const side = expr === "sideeye" ? 16 * (flip ? -1 : 1) : 0;
    const ix = cx + gx + side;
    const iy = cy + 3 + gy;
    const r = expr === "surprised" ? 16 : 21;
    return (
      <g>
        <clipPath id={u(left ? "el" : "er")}>
          <path d={white} />
        </clipPath>
        <path d={white} fill="#FFFBF6" />
        <g clipPath={url(left ? "el" : "er")}>
          <circle cx={ix} cy={iy} r={r} fill={url("iris")} />
          <circle cx={ix} cy={iy} r={r * 0.45} fill="#120A08" />
          {/* lid shadow over the top of the eye */}
          <path d={`${upper} L${xo} ${cy - h} L${xi} ${cy - h} Z`} transform="translate(0 7)" fill="#7A5048" opacity={0.3} />
          <circle cx={ix - r * 0.38} cy={iy - r * 0.4} r={r * 0.3} fill="#FFF" />
          <circle cx={ix + r * 0.4} cy={iy + r * 0.38} r={r * 0.12} fill="#FFF" />
        </g>
        <path d={upper} stroke={LASH} strokeWidth={6.5} fill="none" strokeLinecap="round" />
        {longHair && (
          <path d={`M${xo - out * 6} ${cy - 6} q${out * 10} -4 ${out * 18} -14 M${xo - out * 16} ${lidY + 2} q${out * 8} -6 ${out * 14} -12`} stroke={LASH} strokeWidth={3.2} fill="none" strokeLinecap="round" />
        )}
        <path
          d={`M${xo - out * 4} ${cy + 2} C${xo - out * w * 0.2} ${cy + h * 0.45} ${cx} ${cy + h * 0.5} ${xi + out * w * 0.25} ${cy + h * 0.4}`}
          stroke={LINE}
          strokeWidth={1.6}
          fill="none"
          opacity={0.55}
        />
        <path d={`M${xi + out * w * 0.2} ${lidY - h * 0.22} Q${cx} ${lidY - h * 0.55} ${xo - out * w * 0.1} ${lidY - h * 0.14}`} stroke={LINE} strokeWidth={1.3} fill="none" opacity={0.4} />
      </g>
    );
  };

  const brow = (cx: number, left: boolean) => {
    const out = left ? -1 : 1;
    const [inner, outer]: [number, number] =
      expr === "surprised"
        ? [-30, -26]
        : expr === "worried"
          ? [-26, -6]
          : expr === "angry"
            ? [4, -22]
            : expr === "sideeye" || expr === "smug"
              ? left
                ? [-8, -10]
                : [-16, -24]
              : [-14, -14];
    const yi = 312 + inner;
    const yo = 312 + outer;
    const xi = cx - out * 28;
    const xo = cx + out * 32;
    const thick = hair === "short" || hair === "bald" || hair === "curly" ? 1.5 : 1;
    return (
      <path
        d={`M${xi} ${yi + 3} Q${cx} ${Math.min(yi, yo) - 10 * thick} ${xo} ${yo + 2} Q${cx} ${Math.min(yi, yo) - 3} ${xi} ${yi + 6 * thick} Z`}
        fill={hair === "nani" || aged ? "#8A7A72" : LINE}
        opacity={0.85}
      />
    );
  };

  const mouth = (() => {
    if (viseme && viseme !== "X") return <Mouth shape={viseme} x={300 + t * 1.2} y={454} s={expr === "angry" || expr === "surprised" ? 1.4 : 1} />;
    switch (expr) {
      case "laugh":
      case "talk":
        return (
          <g transform={`translate(${300 + t * 1.2} 452)`}>
            <path d={expr === "laugh" ? "M-30 -4 Q0 40 30 -4 Z" : "M-16 -2 Q0 22 16 -2 Z"} fill="#7A2E2E" stroke={LINE} strokeWidth={2.4} strokeLinejoin="round" />
            {expr === "laugh" && <path d="M-26 -2 L26 -2 L22 6 L-22 6 Z" fill="#FFFBF6" />}
            {expr === "laugh" && <ellipse cy={22} rx={11} ry={6} fill="#D86A6A" />}
          </g>
        );
      case "surprised":
        return (
          <g transform={`translate(${300 + t * 1.2} 452)`}>
            <ellipse rx={12} ry={15} fill="#7A2E2E" stroke={LINE} strokeWidth={2} />
            <ellipse cy={8} rx={8} ry={5} fill="#D86A6A" />
          </g>
        );
      case "worried":
        return <path d={`M${286 + t} 458 Q${300 + t} 448 ${314 + t} 458`} stroke={LINE} strokeWidth={2.8} fill="none" strokeLinecap="round" />;
      case "angry":
        return (
          <g transform={`translate(${300 + t * 1.2} 456)`}>
            <path d="M-32 -6 Q0 -16 32 -6 Q22 36 0 38 Q-22 36 -32 -6 Z" fill="#7A2E2E" stroke={LINE} strokeWidth={2.6} />
            <path d="M-26 -6 Q0 -13 26 -6 L22 4 Q0 0 -22 4 Z" fill="#FFFBF6" />
          </g>
        );
      case "smug":
        return <path d={`M${276 + t} 452 Q${300 + t} 466 ${328 + t} 444`} stroke={LINE} strokeWidth={3.2} fill="none" strokeLinecap="round" />;
      case "sideeye":
      case "neutral":
      case "sleepy":
        return <path d={`M${288 + t} 454 L${312 + t} 454`} stroke={LINE} strokeWidth={2.6} strokeLinecap="round" />;
      default:
        return (
          <g>
            <path d={`M${278 + t} 448 Q${300 + t} 470 ${322 + t} 448`} stroke={LINE} strokeWidth={3} fill="none" strokeLinecap="round" />
            <path d={`M${288 + t} 462 Q${300 + t} 468 ${312 + t} 462`} stroke="#E08E84" strokeWidth={4} fill="none" strokeLinecap="round" opacity={0.7} />
          </g>
        );
    }
  })();

  const backHair = (() => {
    switch (hair) {
      case "nani":
        return (
          <g>
            <path d="M170 340 C160 230 222 166 300 164 C378 166 440 230 430 340 C420 380 400 400 380 404 L220 404 C200 400 180 380 170 340 Z" fill={hairColor} />
            <ellipse cx={300} cy={470} rx={92} ry={56} fill={hairColor} />
            <path d="M232 462 Q300 440 368 462" stroke="#FFFFFF" strokeWidth={4} fill="none" opacity={0.5} />
          </g>
        );
      case "bob":
        return <path d="M150 350 C140 220 214 150 300 150 C386 150 460 220 450 350 C448 400 440 450 420 478 L180 478 C160 450 152 400 150 350 Z" fill={hairColor} />;
      case "side":
        return <path d="M156 360 C146 220 216 150 300 150 C384 150 452 220 444 360 C440 440 430 520 412 590 L370 590 C380 520 384 440 380 380 L220 380 C216 440 220 520 230 590 L188 590 C170 520 160 440 156 360 Z" fill={hairColor} />;
      case "ponytail":
        return (
          <g>
            <path d="M168 330 C160 226 224 160 300 160 C376 160 440 226 432 330 L432 380 L168 380 Z" fill={hairColor} />
            <path d="M410 190 C480 200 500 300 470 420 C460 470 430 520 420 560 C440 470 450 380 420 290 Z" fill={hairColor} />
            <path d="M440 240 C470 300 470 380 452 440" stroke={hairHi} strokeWidth={4} fill="none" opacity={0.7} strokeLinecap="round" />
          </g>
        );
      case "curly":
        return (
          <g fill={hairColor}>
            {Array.from({ length: 14 }, (_, i) => {
              const a = Math.PI * (1.02 + (i / 13) * 0.96);
              return <circle key={i} cx={300 + Math.cos(a) * 150} cy={300 + Math.sin(a) * 150} r={46} />;
            })}
            <circle cx={300} cy={250} r={120} />
          </g>
        );
      default:
        return null;
    }
  })();

  const frontHair = (() => {
    switch (hair) {
      case "nani":
        return (
          <g>
            <path d="M182 318 C176 232 232 176 300 176 C368 176 424 232 418 318 C404 270 370 228 304 214 L300 200 L296 214 C230 228 196 270 182 318 Z" fill={hairColor} />
            <path d="M300 184 L300 214" stroke="#E8DFD8" strokeWidth={3} opacity={0.8} />
            {[0, 1, 2, 3].map((i) => (
              <path
                key={i}
                d={`M${290 - i * 22} ${200 + i * 6} Q${240 - i * 14} ${226 + i * 10} ${206 - i * 6} ${286 + i * 8} M${310 + i * 22} ${200 + i * 6} Q${360 + i * 14} ${226 + i * 10} ${394 + i * 6} ${286 + i * 8}`}
                stroke="#FFFFFF"
                strokeWidth={2.5}
                fill="none"
                opacity={0.45}
              />
            ))}
          </g>
        );
      case "short":
        return (
          <g>
            <path d="M180 300 C168 214 226 150 304 152 C382 154 434 214 420 300 C414 262 400 240 380 230 C352 246 300 250 250 236 C214 246 192 268 180 300 Z" fill={hairColor} />
            <path d="M236 196 Q300 176 366 196 M220 220 Q262 204 300 210" stroke={hairHi} strokeWidth={4} fill="none" opacity={0.6} strokeLinecap="round" />
            <path d="M182 300 L178 340 M418 300 L422 340" stroke={hairColor} strokeWidth={14} strokeLinecap="round" />
          </g>
        );
      case "bob":
        return (
          <g>
            <path d="M176 330 C168 230 230 170 300 170 C370 170 432 230 424 330 C400 280 360 260 330 250 C300 262 240 270 210 280 C196 296 184 312 176 330 Z" fill={hairColor} />
            <path d="M226 210 Q300 182 376 214" stroke={hairHi} strokeWidth={4} fill="none" opacity={0.6} strokeLinecap="round" />
          </g>
        );
      case "side":
        return (
          <g>
            <path d="M178 330 C170 232 232 168 300 166 C380 166 432 236 422 320 C400 262 330 226 252 250 C222 266 196 296 178 330 Z" fill={hairColor} />
            <path d="M250 196 Q330 172 396 230 M226 236 Q280 216 330 222" stroke={hairHi} strokeWidth={4} fill="none" opacity={0.6} strokeLinecap="round" />
          </g>
        );
      case "ponytail":
        return (
          <g>
            <path d="M180 320 C172 228 232 172 300 172 C368 172 428 228 420 320 C404 268 360 232 300 226 C252 232 204 262 180 320 Z" fill={hairColor} />
            <path d="M236 200 Q300 182 364 200" stroke={hairHi} strokeWidth={4} fill="none" opacity={0.6} strokeLinecap="round" />
            <path d="M190 300 C182 340 186 380 196 410" stroke={hairColor} strokeWidth={6} fill="none" strokeLinecap="round" />
          </g>
        );
      case "curly":
        return (
          <g>
            <g fill={hairColor}>
              {Array.from({ length: 7 }, (_, i) => (
                <circle key={i} cx={210 + i * 30} cy={214 + Math.abs(3 - i) * 8} r={30} />
              ))}
            </g>
            {Array.from({ length: 6 }, (_, i) => (
              <path key={i} d={`M${200 + i * 36} ${206 + Math.abs(2.5 - i) * 6} q10 -12 20 0`} stroke={hairHi} strokeWidth={3} fill="none" opacity={0.6} />
            ))}
          </g>
        );
      case "bald":
        return <path d="M182 318 C178 290 182 262 194 244 M418 318 C422 290 418 262 406 244" stroke={hairColor} strokeWidth={22} fill="none" strokeLinecap="round" />;
    }
  })();

  const body = (() => {
    const shoulders =
      "M20 800 C30 730 90 672 188 642 C204 634 214 626 222 618 L378 618 C386 626 396 634 412 642 C510 672 570 730 580 800 Z";
    const folds = <path d="M150 720 q20 30 14 80 M450 720 q-20 30 -14 80" stroke={LINE} strokeWidth={2} fill="none" opacity={0.35} />;
    switch (outfit) {
      case "saree":
        return (
          <g>
            <path d={shoulders} fill={cloth} stroke={LINE} strokeWidth={2.4} />
            <path d="M222 618 C250 660 350 660 378 618 Z" fill={url("skin")} stroke={LINE} strokeWidth={2} />
            <path d="M360 612 C420 640 470 700 520 800 L360 800 C340 740 330 680 340 620 Z" fill={accent} stroke={LINE} strokeWidth={2.2} opacity={0.95} />
            <path d="M372 618 C430 650 476 712 512 800" stroke="#C2410C" strokeWidth={14} fill="none" opacity={0.8} />
            <path d="M372 618 C430 650 476 712 512 800" stroke="#FBBF24" strokeWidth={4} fill="none" strokeDasharray="8 10" />
          </g>
        );
      case "shirt":
      case "blazer":
        return (
          <g>
            <path d={shoulders} fill={outfit === "blazer" ? "#334155" : cloth} stroke={LINE} strokeWidth={2.4} />
            {outfit === "blazer" && <path d="M240 626 L300 760 L360 626 L330 618 L300 690 L270 618 Z" fill={cloth} stroke={LINE} strokeWidth={2} />}
            <path d="M246 618 L300 680 L354 618 L332 610 L300 650 L268 610 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={2} />
            {outfit === "blazer" && (
              <g fill="#B91C1C" stroke={LINE} strokeWidth={1.8} strokeLinejoin="round">
                <path d="M292 672 L308 672 L312 690 L300 698 L288 690 Z" />
                <path d="M290 696 L310 696 L306 742 L300 750 L294 742 Z" />
              </g>
            )}
            {outfit === "shirt" && <path d="M300 680 L300 800" stroke={LINE} strokeWidth={1.8} opacity={0.5} />}
            {outfit === "shirt" && [0, 1].map((i) => <circle key={i} cx={306} cy={712 + i * 50} r={4} fill="#FFF" stroke={LINE} strokeWidth={1.2} />)}
            {folds}
          </g>
        );
      case "hoodie":
        return (
          <g>
            <path d={shoulders} fill={cloth} stroke={LINE} strokeWidth={2.4} />
            <path d="M196 640 C220 700 380 700 404 640 C380 660 340 672 300 672 C260 672 220 660 196 640 Z" fill="#00000022" stroke={LINE} strokeWidth={2} />
            <path d="M282 676 L278 740 M318 676 L322 740" stroke={accent} strokeWidth={4} strokeLinecap="round" />
            {folds}
          </g>
        );
      case "tee":
        return (
          <g>
            <path d={shoulders} fill={cloth} stroke={LINE} strokeWidth={2.4} />
            <path d="M226 618 C250 650 350 650 374 618" stroke={shade(cloth, 0.75)} strokeWidth={8} fill="none" />
            {folds}
          </g>
        );
      case "kurta":
        return (
          <g>
            <path d={shoulders} fill={cloth} stroke={LINE} strokeWidth={2.4} />
            <path d="M300 640 L300 760" stroke={LINE} strokeWidth={2} />
            {[0, 1, 2].map((i) => (
              <circle key={i} cx={300} cy={660 + i * 30} r={4} fill={accent} stroke={LINE} strokeWidth={1} />
            ))}
            <path d="M222 618 C250 646 350 646 378 618" stroke={accent} strokeWidth={6} fill="none" />
            {folds}
          </g>
        );
    }
  })();

  return (
    <g transform={`translate(${x} ${y}) scale(${s}) ${flip ? "translate(600 0) scale(-1 1)" : ""}`}>
      <defs>
        <radialGradient id={u("iris")} cx="45%" cy="65%" r="65%">
          <stop offset="0%" stopColor="#6B4A3A" />
          <stop offset="60%" stopColor="#3A251C" />
          <stop offset="100%" stopColor="#1A0F0B" />
        </radialGradient>
        <radialGradient id={u("blush")}>
          <stop offset="0%" stopColor="#EE8E78" stopOpacity={0.6} />
          <stop offset="100%" stopColor="#EE8E78" stopOpacity={0} />
        </radialGradient>
        <linearGradient id={u("skin")} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={shade(skin, 1.06)} />
          <stop offset="100%" stopColor={skin} />
        </linearGradient>
        <filter id={u("soft")} x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation={7} />
        </filter>
        <filter id={u("sketch")} x="-5%" y="-5%" width="110%" height="110%">
          <feTurbulence type="fractalNoise" baseFrequency={0.05} numOctaves={2} seed={7} result="n" />
          <feDisplacementMap in="SourceGraphic" in2="n" scale={2.4} xChannelSelector="R" yChannelSelector="G" />
        </filter>
        <clipPath id={u("face")}>
          <path d={FACE} />
        </clipPath>
      </defs>
      <g filter={url("sketch")}>
        {backHair}
        <path d="M266 476 C266 520 262 548 252 574 L348 574 C338 548 334 520 334 476 Z" fill={url("skin")} stroke={LINE} strokeWidth={2.2} />
        <ellipse cx={300} cy={500} rx={44} ry={18} fill={shade(skin, 0.75)} opacity={0.55} filter={url("soft")} />
        <g transform="translate(0 -44)">{body}</g>
        {/* Ears: the far ear tucks away as the head turns */}
        <ellipse cx={180 + t * 0.6} cy={356} rx={16 - Math.max(0, turn) * 8} ry={26} fill={skin} stroke={LINE} strokeWidth={2} />
        <ellipse cx={420 + t * 0.6} cy={356} rx={16 + Math.min(0, turn) * 8} ry={26} fill={skin} stroke={LINE} strokeWidth={2} />
        <path d={FACE} fill={url("skin")} />
        <g clipPath={url("face")}>
          {/* soft shadow under the hairline and down the far cheek */}
          <path d="M170 240 L430 240 L430 296 C390 284 350 306 300 296 C250 308 210 286 170 302 Z" fill={shade(skin, 0.72)} opacity={0.45} filter={url("soft")} />
          <path d={`M${404 - t} 262 C${424 - t} 340 ${410 - t} 430 ${350 - t} 500 L440 500 L440 240 Z`} fill={shade(skin, 0.8)} opacity={0.45} filter={url("soft")} />
          <ellipse cx={226 + t} cy={404} rx={46} ry={24} fill={url("blush")} />
          <ellipse cx={374 + t} cy={404} rx={46} ry={24} fill={url("blush")} />
          {[0, 1, 2].map((i) => (
            <circle key={i} cx={230 + t + i * 9} cy={392 + (i % 2) * 6} r={1.6} fill="#FFFFFF" opacity={0.7} />
          ))}
        </g>
        <path d={FACE} fill="none" stroke={LINE} strokeWidth={2.2} strokeLinejoin="round" />
        {aged && (
          <g stroke={LINE} strokeWidth={1.8} fill="none" opacity={0.45} strokeLinecap="round">
            <path d={`M${246 + t} 428 Q${236 + t} 452 ${250 + t} 474 M${354 + t} 428 Q${364 + t} 452 ${350 + t} 474`} />
            <path d="M196 352 l-12 -6 M196 364 l-14 2 M404 352 l12 -6 M404 364 l14 2" />
            <path d="M266 268 Q300 260 334 268" />
          </g>
        )}
        {brow(245 + t, true)}
        {brow(355 + t, false)}
        {eye(245 + t, 362, true)}
        {eye(355 + t, 362, false)}
        <path d={`M${302 + t * 1.3} 392 Q${309 + t * 1.3} 406 ${299 + t * 1.3} 412`} stroke={LINE} strokeWidth={2} fill="none" strokeLinecap="round" opacity={0.75} />
        <ellipse cx={298 + t * 1.3} cy={415} rx={6} ry={2.2} fill={shade(skin, 0.7)} opacity={0.6} />
        {moustache && <path d={`M${264 + t} 432 Q${300 + t} 418 ${336 + t} 432 Q${300 + t} 440 ${264 + t} 432 Z`} fill={hairColor} />}
        {mouth}
        {beard && (
          <path d="M220 440 C236 500 268 528 300 530 C332 528 364 500 380 440 C364 476 340 492 300 494 C260 492 236 476 220 440 Z" fill={hairColor} opacity={0.85} />
        )}
        {bindi && <circle cx={300 + t} cy={300} r={7} fill="#B91C1C" />}
        {frontHair}
        {glasses && (
          <g stroke={LASH} strokeWidth={4} fill="#FFFFFF22" transform={`translate(${t} 0)`}>
            <rect x={196} y={326} width={98} height={74} rx={30} />
            <rect x={306} y={326} width={98} height={74} rx={30} />
            <path d="M294 352 Q300 346 306 352 M196 350 L180 344 M404 350 L420 344" fill="none" />
            <path d="M214 340 l20 -8" stroke="#FFFFFF" strokeWidth={4} opacity={0.7} />
          </g>
        )}
      </g>
    </g>
  );
};
