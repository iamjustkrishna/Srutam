// Supporting cast, drawn to match MeeraSketch: same face shape, warm brown line
// art, glossy layered eyes, soft face shading and peach blush, in the same
// 600 x 800 local box so they can stand beside her at the same scale.
import { useId } from "react";
import { Mouth, type Viseme } from "../cast/Mouth";

const LINE = "#4A2E28";
const LASH = "#24140F";

export type Hair = "nani" | "short" | "bob" | "ponytail" | "curly" | "bald" | "side";
export type Outfit = "saree" | "shirt" | "hoodie" | "kurta" | "blazer" | "tee";
export type Face = "oval" | "round" | "square" | "long" | "heart";
export type Nose = "button" | "straight" | "broad";
export type PExpr =
  | "sheepish"
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
  /** Closed (true), open (false) or partly closed (0..1, for smooth blinks). */
  blink?: boolean | number;
  flip?: boolean;
  /** Head shape. */
  face?: Face;
  nose?: Nose;
  /** Eye size: adults smaller than the young ones. */
  eyeScale?: number;
  iris?: string;
  /** Shoulder width: 1 slim, ~1.08 broad. */
  bw?: number;
  /** Hide the resting upper arm on a side (the rig draws a posed arm there). */
  armsOut?: { l?: boolean; r?: boolean };
  earrings?: boolean;
  headphones?: boolean;
  stubble?: boolean;
  /** A sleeveless Nehru jacket over the outfit, in this colour. */
  vest?: string;
  pocketSquare?: boolean;
  /** Hair swing (-1..1). */
  sway?: number;
};

const FACES: Record<Face, string> = {
  oval: "M182 300 C180 372 196 428 228 470 C252 500 278 518 300 520 C322 518 348 500 372 470 C404 428 420 372 418 300 C410 222 360 188 300 188 C240 188 190 222 182 300 Z",
  round: "M180 304 C178 384 200 440 236 478 C258 500 280 512 300 512 C320 512 342 500 364 478 C400 440 422 384 420 304 C412 224 362 190 300 190 C238 190 188 224 180 304 Z",
  square: "M182 300 C180 382 188 446 218 484 C242 512 274 522 300 522 C326 522 358 512 382 484 C412 446 420 382 418 300 C410 222 360 188 300 188 C240 188 190 222 182 300 Z",
  long: "M190 300 C188 380 204 446 234 488 C256 518 280 532 300 532 C320 532 344 518 366 488 C396 446 412 380 410 300 C404 220 358 184 300 184 C242 184 196 220 190 300 Z",
  heart: "M180 296 C182 370 204 432 240 476 C264 504 284 520 300 522 C316 520 336 504 360 476 C396 432 418 370 420 296 C412 220 362 186 300 186 C238 186 188 220 180 296 Z",
};

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
  face = "oval",
  nose = "button",
  eyeScale = 1,
  iris = "#6B4A3A",
  bw = 1,
  armsOut = {},
  earrings = false,
  headphones = false,
  stubble = false,
  vest,
  pocketSquare = false,
  sway = 0,
}) => {
  const FACE = FACES[face];
  const shut = blink === true ? 1 : typeof blink === "number" ? blink : 0;
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const u = (k: string) => `${k}${id}`;
  const url = (k: string) => `url(#${k}${id})`;
  const gx = (flip ? -gaze[0] : gaze[0]) * 12;
  const gy = gaze[1] * 7;
  const t = turn * 30;
  // In a three-quarter view the far eye travels less than the near one (the face wraps away).
  const eyeL = 245 + t * (turn < 0 ? 0.72 : 1.05);
  const eyeR = 355 + t * (turn > 0 ? 0.72 : 1.05);
  const hairHi = shade(hairColor, hair === "nani" ? 1.12 : 1.9);
  const longHair = lashes ?? ["nani", "bob", "ponytail", "side"].includes(hair);

  const eye = (cx: number, cy: number, left: boolean) => {
    const out = left ? -1 : 1;
    const w = 72 * eyeScale * (left ? 1 + Math.max(0, turn) * 0.1 - Math.max(0, -turn) * 0.32 : 1 + Math.max(0, -turn) * 0.1 - Math.max(0, turn) * 0.32);
    const xo = cx + (out * w) / 2;
    const xi = cx - (out * w) / 2;
    if (expr === "laugh" || expr === "sleepy" || shut > 0.8) {
      const up = expr === "laugh" ? -18 : 10;
      return (
        <g>
          <path d={`M${xi} ${cy + 4} Q${cx} ${cy + up} ${xo} ${cy + 2}`} stroke={LASH} strokeWidth={6} fill="none" strokeLinecap="round" />
          {longHair && <path d={`M${xo} ${cy + 2} l${out * 12} -6`} stroke={LASH} strokeWidth={3.5} strokeLinecap="round" />}
        </g>
      );
    }
    const open = (expr === "surprised" ? 1.2 : expr === "sideeye" || expr === "angry" || expr === "smug" ? 0.62 : expr === "sheepish" ? 0.8 : 1) * (1 - shut * 0.85);
    const h = 44 * eyeScale * open;
    const lidY = cy - h * 0.6;
    const upper = `M${xi} ${cy + 2} C${xi + out * w * 0.2} ${lidY - h * 0.1} ${xo - out * w * 0.2} ${lidY - h * 0.12} ${xo} ${cy - 4}`;
    const white = `${upper} C${xo - out * w * 0.2} ${cy + h * 0.5} ${xi + out * w * 0.2} ${cy + h * 0.5} ${xi} ${cy + 2} Z`;
    const side = expr === "sideeye" ? 16 * (flip ? -1 : 1) : 0;
    const ix = cx + gx + side;
    const iy = cy + 3 + gy;
    const r = (expr === "surprised" ? 16 : 21) * eyeScale;
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
        <path d={upper} stroke={LASH} strokeWidth={5.5 + eyeScale} fill="none" strokeLinecap="round" />
        {/* eyelid crease */}
        <path d={`M${xi + out * w * 0.12} ${lidY - 10 * eyeScale} Q${cx} ${lidY - 20 * eyeScale - h * 0.1} ${xo - out * w * 0.08} ${cy - 10 - h * 0.25}`} stroke={LINE} strokeWidth={1.8} fill="none" opacity={0.5} strokeLinecap="round" />
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
    const sheep: [number, number] = [-22, -8];
    const yi = 312 + (expr === "sheepish" ? sheep[0] : inner) + (1 - eyeScale) * 30;
    const yo = 312 + (expr === "sheepish" ? sheep[1] : outer) + (1 - eyeScale) * 30;
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

  const lip = shade(skin, 0.78);
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
      case "sheepish":
        return (
          <g>
            <path d={`M${280 + t} 452 Q${292 + t} 446 ${300 + t} 452 Q${310 + t} 458 ${322 + t} 448`} stroke={LINE} strokeWidth={3} fill="none" strokeLinecap="round" />
            <path d={`M${290 + t} 462 Q${300 + t} 466 ${310 + t} 462`} stroke={lip} strokeWidth={4} fill="none" strokeLinecap="round" opacity={0.6} />
          </g>
        );
      case "sideeye":
      case "neutral":
      case "sleepy":
        return (
          <g>
            <path d={`M${284 + t} 455 Q${292 + t} 451 ${300 + t} 453 Q${308 + t} 451 ${316 + t} 455`} stroke={LINE} strokeWidth={2.6} fill="none" strokeLinecap="round" />
            <path d={`M${291 + t} 463 Q${300 + t} 467 ${309 + t} 463`} stroke={lip} strokeWidth={4} fill="none" strokeLinecap="round" opacity={0.55} />
          </g>
        );
      default:
        return (
          <g>
            <path d={`M${278 + t} 448 Q${300 + t} 470 ${322 + t} 448`} stroke={LINE} strokeWidth={3} fill="none" strokeLinecap="round" />
            <path d={`M${288 + t} 462 Q${300 + t} 468 ${312 + t} 462`} stroke={lip} strokeWidth={4} fill="none" strokeLinecap="round" opacity={0.7} />
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
            <g transform={`rotate(${sway * 9} 412 200)`}>
              <path d="M410 190 C484 196 506 300 476 420 C466 472 434 524 420 566 C442 474 452 382 420 290 Z" fill={hairColor} />
              <path d="M440 240 C470 300 470 380 452 440 M424 260 C446 320 444 400 432 470" stroke={hairHi} strokeWidth={3.5} fill="none" opacity={0.6} strokeLinecap="round" />
            </g>
            <ellipse cx={414} cy={204} rx={20} ry={26} fill={accent === "#FDF2F8" ? "#F472B6" : accent} stroke={LINE} strokeWidth={2} transform="rotate(30 414 204)" />
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
            {Array.from({ length: 9 }, (_, i) => {
              const a = Math.PI * (1.08 + (i / 8) * 0.84);
              return <path key={`s${i}`} d={`M${300 + Math.cos(a) * 150 - 14} ${300 + Math.sin(a) * 150} q14 -18 28 0`} stroke={hairHi} strokeWidth={3} fill="none" opacity={0.45} />;
            })}
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
            <path d="M196 236 C230 170 330 150 396 196 C360 176 300 178 262 204 C240 214 214 226 196 236 Z" fill={hairColor} />
            <path d="M236 196 Q300 176 366 196 M220 220 Q262 204 300 210 M300 172 Q336 168 372 184" stroke={hairHi} strokeWidth={3.5} fill="none" opacity={0.6} strokeLinecap="round" />
            <path d="M186 296 L182 348 M414 296 L418 348" stroke={hairColor} strokeWidth={14} strokeLinecap="round" />
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
            <g transform={`rotate(${sway * 4} 420 320)`}>
              <path d="M404 260 C432 330 440 420 432 520 C428 580 440 630 456 668 C420 640 402 590 404 520 C406 430 404 350 386 290 Z" fill={hairColor} stroke={LINE} strokeWidth={1.2} strokeOpacity={0.4} />
              <path d="M416 330 C424 410 420 500 430 600" stroke={hairHi} strokeWidth={3} fill="none" opacity={0.5} strokeLinecap="round" />
            </g>
          </g>
        );
      case "ponytail":
        return (
          <g>
            <path d="M180 320 C172 228 232 172 300 172 C368 172 428 228 420 320 C404 268 360 232 300 226 C252 232 204 262 180 320 Z" fill={hairColor} />
            <path d="M236 200 Q300 182 364 200" stroke={hairHi} strokeWidth={4} fill="none" opacity={0.6} strokeLinecap="round" />
            <path d="M190 300 C182 340 186 380 196 410" stroke={hairColor} strokeWidth={6} fill="none" strokeLinecap="round" />
            <path d="M206 262 C226 232 262 220 286 222 C276 246 262 268 240 282 C250 262 254 252 252 246 C236 256 222 268 206 262 Z" fill={hairColor} />
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
        return (
          <g>
            <path d="M182 318 C178 290 182 262 194 244 M418 318 C422 290 418 262 406 244" stroke={hairColor} strokeWidth={22} fill="none" strokeLinecap="round" />
            <path d="M184 318 C180 296 182 272 192 254 M416 318 C420 296 418 272 408 254" stroke={hairHi} strokeWidth={4} fill="none" opacity={0.4} strokeLinecap="round" />
            <ellipse cx={262} cy={222} rx={44} ry={16} fill="#FFFFFF" opacity={0.22} transform="rotate(-18 262 222)" />
          </g>
        );
    }
  })();

  // ---------- body: neck, trapezius, deltoids and resting upper arms (no more dome) ----------
  const X = (dx: number) => 300 + dx * bw;
  const leftOut = !!armsOut[flip ? "r" : "l"];
  const rightOut = !!armsOut[flip ? "l" : "r"];
  const half = (side: -1 | 1, out: boolean) => {
    const x = (dx: number) => 300 + side * dx * bw;
    return out
      ? `M300 572 L${x(46)} 572 L${x(94)} 584 C${x(124)} 594 ${x(148)} 604 ${x(160)} 618 C${x(178)} 632 ${x(180)} 660 ${x(166)} 690 C${x(160)} 716 ${x(160)} 740 ${x(162)} 760 L300 760 Z`
      : `M300 572 L${x(46)} 572 L${x(94)} 584 C${x(124)} 594 ${x(148)} 604 ${x(160)} 618 C${x(184)} 628 ${x(200)} 652 ${x(204)} 690 C${x(208)} 716 ${x(214)} 740 ${x(218)} 760 L300 760 Z`;
  };
  /** A resting upper arm's inner edge (the line between arm and chest). */
  const armLine = (side: -1 | 1) => {
    const x = (dx: number) => 300 + side * dx * bw;
    return `M${x(158)} 664 C${x(154)} 694 ${x(156)} 728 ${x(162)} 760`;
  };
  const clothBody = (fill: string, sleeve = fill) => (
    <g>
      <path d={half(-1, leftOut)} fill={fill} />
      <path d={half(1, rightOut)} fill={fill} />
      {/* sleeves a touch darker, so the resting arms read as arms */}
      {!leftOut && <path d={`${half(-1, false)}`} fill={sleeve} clipPath={url("armL")} />}
      {!rightOut && <path d={`${half(1, false)}`} fill={sleeve} clipPath={url("armR")} />}
      {/* light from the upper left: the far half and the lower chest fall into shade */}
      <path d={half(1, rightOut)} fill="#000" opacity={0.07} />
      <path d={`${half(-1, leftOut)} ${half(1, rightOut)}`} fill={url("chest")} />
      <path d={`${half(-1, leftOut)} ${half(1, rightOut)}`} fill="none" stroke={LINE} strokeWidth={2.4} strokeLinejoin="round" />
      {!leftOut && <path d={armLine(-1)} stroke={LINE} strokeWidth={2} fill="none" opacity={0.7} />}
      {!rightOut && <path d={armLine(1)} stroke={LINE} strokeWidth={2} fill="none" opacity={0.7} />}
      {/* trapezius / collarbone hint and a shoulder seam */}
      <path d={`M${X(-60)} 600 Q${X(-100)} 612 ${X(-136)} 612 M${X(60)} 600 Q${X(100)} 612 ${X(136)} 612`} stroke={LINE} strokeWidth={1.4} fill="none" opacity={0.25} />
    </g>
  );
  const folds = <path d={`M${X(-110)} 690 q8 30 4 70 M${X(110)} 690 q-8 30 -4 70`} stroke={LINE} strokeWidth={1.8} fill="none" opacity={0.3} />;
  const body = (() => {
    switch (outfit) {
      case "saree":
        return (
          <g>
            {clothBody(cloth, shade(cloth, 0.92))}
            <path d="M246 572 C262 612 338 612 354 572 Z" fill={url("skin")} stroke={LINE} strokeWidth={2} />
            <path d={`M356 572 C${X(150)} 600 ${X(196)} 660 ${X(214)} 760 L356 760 C338 700 334 630 352 574 Z`} fill={accent} stroke={LINE} strokeWidth={2.2} opacity={0.95} />
            <path d={`M368 576 C${X(150)} 606 ${X(190)} 664 ${X(206)} 760`} stroke="#C2410C" strokeWidth={14} fill="none" opacity={0.8} />
            <path d={`M368 576 C${X(150)} 606 ${X(190)} 664 ${X(206)} 760`} stroke="#FBBF24" strokeWidth={4} fill="none" strokeDasharray="8 10" />
          </g>
        );
      case "shirt":
      case "blazer": {
        const coat = outfit === "blazer" ? "#334155" : cloth;
        return (
          <g>
            {clothBody(coat, shade(coat, 0.9))}
            {outfit === "blazer" ? (
              <g>
                {/* shirt and tie in the V, notched lapels */}
                <path d="M254 572 L300 712 L346 572 Z" fill={cloth} stroke={LINE} strokeWidth={2} />
                <g fill="#B91C1C" stroke={LINE} strokeWidth={1.8} strokeLinejoin="round">
                  <path d="M292 616 L308 616 L312 632 L300 640 L288 632 Z" />
                  <path d="M290 638 L310 638 L306 694 L300 704 L294 694 Z" />
                </g>
                <path d="M246 574 L268 572 L300 612 L276 590 Z M354 574 L332 572 L300 612 L324 590 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={2} strokeLinejoin="round" />
                <path d="M248 572 L226 594 L240 610 L226 618 L300 716 L262 600 Z" fill={shade(coat, 1.15)} stroke={LINE} strokeWidth={2} strokeLinejoin="round" />
                <path d="M352 572 L374 594 L360 610 L374 618 L300 716 L338 600 Z" fill={shade(coat, 1.15)} stroke={LINE} strokeWidth={2} strokeLinejoin="round" />
                {pocketSquare && <path d="M352 662 L384 662 L376 646 L366 656 L360 644 Z" fill="#F8FAFC" stroke={LINE} strokeWidth={1.4} />}
                <path d="M346 668 L392 668" stroke={LINE} strokeWidth={1.6} opacity={0.5} />
                <circle cx={300} cy={738} r={6} fill="#1E293B" stroke={LINE} strokeWidth={1.4} />
              </g>
            ) : (
              <g>
                {/* collar points, placket, buttons and a chest pocket */}
                <path d="M300 760 L300 604" stroke={LINE} strokeWidth={1.8} opacity={0.5} />
                {[0, 1, 2].map((i) => (
                  <circle key={i} cx={306} cy={626 + i * 46} r={4} fill="#FFFFFF" stroke={LINE} strokeWidth={1.2} />
                ))}
                <path d="M248 568 L300 604 L290 620 L238 590 Z M352 568 L300 604 L310 620 L362 590 Z" fill={shade(cloth, 1.12)} stroke={LINE} strokeWidth={2} strokeLinejoin="round" />
                <path d="M226 650 L262 650 L262 690 Q244 698 226 690 Z" fill="none" stroke={LINE} strokeWidth={1.6} opacity={0.55} />
              </g>
            )}
            {folds}
          </g>
        );
      }
      case "hoodie":
        return (
          <g>
            {clothBody(cloth, shade(cloth, 0.9))}
            {/* the hood bunched behind the neck, raglan seams, drawstrings */}
            <path d="M226 574 C236 548 364 548 374 574 C372 606 346 626 300 628 C254 626 228 606 226 574 Z" fill={shade(cloth, 0.82)} stroke={LINE} strokeWidth={2.2} />
            <path d="M246 580 C262 604 338 604 354 580" stroke={LINE} strokeWidth={1.6} fill="none" opacity={0.5} />
            <path d={`M244 600 L${X(-150)} 700 M356 600 L${X(150)} 700`} stroke={LINE} strokeWidth={1.6} opacity={0.35} />
            <path d="M284 622 C282 650 280 676 278 700 M316 622 C318 650 320 676 322 700" stroke={accent} strokeWidth={4} strokeLinecap="round" fill="none" />
            <circle cx={278} cy={704} r={5} fill={accent} stroke={LINE} strokeWidth={1.2} />
            <circle cx={322} cy={704} r={5} fill={accent} stroke={LINE} strokeWidth={1.2} />
            {folds}
          </g>
        );
      case "tee":
        return (
          <g>
            {clothBody(cloth, shade(cloth, 0.92))}
            <path d="M248 572 C262 604 338 604 352 572" stroke={shade(cloth, 0.75)} strokeWidth={8} fill="none" />
            {folds}
          </g>
        );
      case "kurta":
        return (
          <g>
            {clothBody(cloth, shade(cloth, 0.92))}
            {/* band collar, placket with buttons, embroidered edge */}
            <path d="M252 566 C262 586 338 586 348 566 L350 580 C338 600 262 600 250 580 Z" fill={shade(cloth, 0.9)} stroke={LINE} strokeWidth={2} />
            <path d="M292 592 L292 690 L308 690 L308 592" fill={shade(cloth, 0.95)} stroke={LINE} strokeWidth={1.6} />
            {[0, 1, 2].map((i) => (
              <circle key={i} cx={300} cy={612 + i * 28} r={4} fill={accent} stroke={LINE} strokeWidth={1} />
            ))}
            <path d="M292 690 L308 690" stroke={accent} strokeWidth={4} />
            {vest && (
              <g>
                <path d={`M252 574 L${X(-118)} 610 C${X(-140)} 650 ${X(-140)} 700 ${X(-136)} 760 L286 760 L286 640 Z`} fill={vest} stroke={LINE} strokeWidth={2.2} strokeLinejoin="round" />
                <path d={`M348 574 L${X(118)} 610 C${X(140)} 650 ${X(140)} 700 ${X(136)} 760 L314 760 L314 640 Z`} fill={vest} stroke={LINE} strokeWidth={2.2} strokeLinejoin="round" />
                {[0, 1].map((i) => (
                  <circle key={i} cx={322} cy={668 + i * 40} r={5} fill="#F5E6C8" stroke={LINE} strokeWidth={1.2} />
                ))}
                <path d="M346 652 L386 652" stroke={LINE} strokeWidth={1.6} opacity={0.5} />
                <path d="M358 652 L358 618 L364 612 L370 618 L370 652" fill="#1D4ED8" stroke={LINE} strokeWidth={1.4} />
              </g>
            )}
            {folds}
          </g>
        );
    }
  })();

  return (
    <g transform={`translate(${x} ${y}) scale(${s}) ${flip ? "translate(600 0) scale(-1 1)" : ""}`}>
      <defs>
        <radialGradient id={u("iris")} cx="45%" cy="65%" r="65%">
          <stop offset="0%" stopColor={iris} />
          <stop offset="60%" stopColor="#3A251C" />
          <stop offset="100%" stopColor="#1A0F0B" />
        </radialGradient>
        <linearGradient id={u("chest")} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#000" stopOpacity={0} />
          <stop offset="55%" stopColor="#000" stopOpacity={0.02} />
          <stop offset="100%" stopColor="#000" stopOpacity={0.12} />
        </linearGradient>
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
        <clipPath id={u("armL")}>
          <path d={`M${X(-150)} 600 L${X(-260)} 600 L${X(-260)} 770 L${X(-162)} 770 C${X(-156)} 728 ${X(-154)} 694 ${X(-158)} 664 Z`} />
        </clipPath>
        <clipPath id={u("armR")}>
          <path d={`M${X(150)} 600 L${X(260)} 600 L${X(260)} 770 L${X(162)} 770 C${X(156)} 728 ${X(154)} 694 ${X(158)} 664 Z`} />
        </clipPath>
      </defs>
      <g filter={url("sketch")}>
        {backHair}
        <path d={`M${300 - 34 * bw} 476 C${300 - 34 * bw} 520 ${300 - 38 * bw} 548 ${300 - 50 * bw} 576 L${300 + 50 * bw} 576 C${300 + 38 * bw} 548 ${300 + 34 * bw} 520 ${300 + 34 * bw} 476 Z`} fill={url("skin")} stroke={LINE} strokeWidth={2.2} />
        <ellipse cx={300} cy={504} rx={44 * bw} ry={20} fill={shade(skin, 0.72)} opacity={0.6} filter={url("soft")} />
        {bw > 1.02 && <path d="M300 528 q-5 10 0 18" stroke={LINE} strokeWidth={1.6} fill="none" opacity={0.3} />}
        {headphones && <path d="M232 560 C226 600 374 600 368 560" stroke="#1F2937" strokeWidth={14} fill="none" strokeLinecap="round" />}
        {body}
        {headphones && (
          <g>
            <ellipse cx={240} cy={594} rx={30} ry={38} fill="#1F2937" stroke={LINE} strokeWidth={2.4} transform="rotate(-24 240 594)" />
            <ellipse cx={360} cy={594} rx={30} ry={38} fill="#1F2937" stroke={LINE} strokeWidth={2.4} transform="rotate(24 360 594)" />
            <ellipse cx={244} cy={590} rx={14} ry={20} fill="#374151" transform="rotate(-24 244 590)" />
            <ellipse cx={356} cy={590} rx={14} ry={20} fill="#374151" transform="rotate(24 356 590)" />
          </g>
        )}
        {/* Ears: the far ear tucks away as the head turns */}
        <ellipse cx={180 + t * 0.35} cy={356} rx={Math.max(4, 16 + Math.max(0, turn) * 6 - Math.max(0, -turn) * 14)} ry={26} fill={skin} stroke={LINE} strokeWidth={2} />
        <ellipse cx={420 + t * 0.35} cy={356} rx={Math.max(4, 16 + Math.max(0, -turn) * 6 - Math.max(0, turn) * 14)} ry={26} fill={skin} stroke={LINE} strokeWidth={2} />
        <path d={`M${184 + t * 0.35} 340 q-8 10 -2 26 q4 6 -2 10 M${416 + t * 0.35} 340 q8 10 2 26 q-4 6 2 10`} stroke={LINE} strokeWidth={1.6} fill="none" opacity={0.5} />
        {earrings && (
          <g fill="#F59E0B" stroke={LINE} strokeWidth={1.4}>
            {[178 + t * 0.35, 422 + t * 0.35].map((ex, i) => (
              <g key={i} transform={`translate(${ex} 384) rotate(${sway * -12})`}>
                <circle cx={0} cy={0} r={4} />
                <path d="M-9 8 Q0 2 9 8 L7 20 L-7 20 Z" />
                <circle cx={0} cy={24} r={3} fill="#FDE68A" />
              </g>
            ))}
          </g>
        )}
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
        {brow(eyeL, true)}
        {brow(eyeR, false)}
        {eye(eyeL, 362, true)}
        {eye(eyeR, 362, false)}
        {(() => {
          const n = t * 1.3;
          if (nose === "button")
            return (
              <g>
                <path d={`M${302 + n} 392 Q${309 + n} 406 ${299 + n} 412`} stroke={LINE} strokeWidth={2} fill="none" strokeLinecap="round" opacity={0.75} />
                <ellipse cx={298 + n} cy={415} rx={6} ry={2.2} fill={shade(skin, 0.7)} opacity={0.6} />
              </g>
            );
          const wide = nose === "broad" ? 1.3 : 1;
          return (
            <g strokeLinecap="round" fill="none">
              {/* bridge shadow, tip highlight, nostril wings */}
              <path d={`M${292 + n} 356 Q${288 + n} 384 ${290 + n} 402`} stroke={shade(skin, 0.7)} strokeWidth={5} opacity={0.35} />
              <ellipse cx={302 + n} cy={404} rx={7 * wide} ry={5} fill="#FFFFFF" opacity={0.25} />
              <path d={`M${300 - 16 * wide + n} 410 Q${300 - 12 * wide + n} 420 ${300 - 4 + n} 416 Q${300 + n} 420 ${304 + n} 416 Q${300 + 12 * wide + n} 420 ${300 + 16 * wide + n} 410`} stroke={LINE} strokeWidth={2} opacity={0.7} />
              <ellipse cx={300 - 8 * wide + n} cy={414} rx={3.5} ry={2} fill={shade(skin, 0.55)} stroke="none" opacity={0.7} />
              <ellipse cx={300 + 8 * wide + n} cy={414} rx={3.5} ry={2} fill={shade(skin, 0.55)} stroke="none" opacity={0.7} />
            </g>
          );
        })()}
        {stubble && (
          <path d="M224 440 C238 496 270 520 300 522 C330 520 362 496 376 440 C360 470 340 488 300 490 C260 488 240 470 224 440 Z" fill={hairColor} opacity={0.18} />
        )}
        {expr === "sheepish" && (
          <g>
            <ellipse cx={226 + t} cy={404} rx={44} ry={20} fill="#E05A4A" opacity={0.28} filter={url("soft")} />
            <ellipse cx={374 + t} cy={404} rx={44} ry={20} fill="#E05A4A" opacity={0.28} filter={url("soft")} />
          </g>
        )}
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
