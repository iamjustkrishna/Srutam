// Supporting cast for the story-idea storyboards, drawn to match MeeraSketch:
// same face shape, warm brown line art, glossy eyes and peach blush, in the
// same 600 x 800 local box so they can stand beside her at the same scale.
import { useId } from "react";

const LINE = "#4A2E28";
const LASH = "#24140F";

export type Hair = "nani" | "short" | "bob" | "ponytail" | "curly" | "bald";
export type Outfit = "saree" | "shirt" | "hoodie" | "kurta" | "blazer";
export type PExpr =
  | "smile"
  | "neutral"
  | "laugh"
  | "surprised"
  | "worried"
  | "angry"
  | "sideeye"
  | "sleepy"
  | "talk";

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
  /** Smile lines and softer cheeks (Nani). */
  aged?: boolean;
  /** Iris offset, -1..1 per axis. */
  gaze?: [number, number];
  flip?: boolean;
};

const FACE =
  "M182 300 C180 372 196 428 228 470 C252 500 278 518 300 520 C322 518 348 500 372 470 C404 428 420 372 418 300 C410 222 360 188 300 188 C240 188 190 222 182 300 Z";

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
  aged = false,
  gaze = [0, 0],
  flip = false,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const u = (k: string) => `${k}${id}`;
  const url = (k: string) => `url(#${k}${id})`;
  const gx = (flip ? -gaze[0] : gaze[0]) * 12;
  const gy = gaze[1] * 7;

  const eye = (cx: number, cy: number, left: boolean) => {
    const out = left ? -1 : 1;
    const w = 72;
    const xo = cx + (out * w) / 2;
    const xi = cx - (out * w) / 2;
    if (expr === "laugh" || expr === "sleepy") {
      const up = expr === "laugh" ? -18 : 10;
      return (
        <path
          d={`M${xi} ${cy + 4} Q${cx} ${cy + up} ${xo} ${cy + 2}`}
          stroke={LASH}
          strokeWidth={6}
          fill="none"
          strokeLinecap="round"
        />
      );
    }
    const open =
      expr === "surprised" ? 1.2 : expr === "sideeye" || expr === "angry" ? 0.62 : 1;
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
          <circle cx={ix - r * 0.38} cy={iy - r * 0.4} r={r * 0.3} fill="#FFF" />
          <circle cx={ix + r * 0.4} cy={iy + r * 0.38} r={r * 0.12} fill="#FFF" />
        </g>
        <path d={upper} stroke={LASH} strokeWidth={6.5} fill="none" strokeLinecap="round" />
        <path
          d={`M${xo - out * 4} ${cy + 2} C${xo - out * w * 0.2} ${cy + h * 0.45} ${cx} ${cy + h * 0.5} ${xi + out * w * 0.25} ${cy + h * 0.4}`}
          stroke={LINE}
          strokeWidth={1.6}
          fill="none"
          opacity={0.55}
        />
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
            ? [2, -20]
            : expr === "sideeye"
              ? left
                ? [-8, -10]
                : [-16, -22]
              : [-14, -14];
    const yi = 312 + inner;
    const yo = 312 + outer;
    const xi = cx - out * 28;
    const xo = cx + out * 32;
    const thick = hair === "short" || hair === "bald" || hair === "curly" ? 1.5 : 1;
    return (
      <path
        d={`M${xi} ${yi + 3} Q${cx} ${Math.min(yi, yo) - 10 * thick} ${xo} ${yo + 2} Q${cx} ${Math.min(yi, yo) - 3} ${xi} ${yi + 6 * thick} Z`}
        fill={hair === "nani" ? "#8A7A72" : LINE}
        opacity={0.85}
      />
    );
  };

  const mouth = (() => {
    switch (expr) {
      case "laugh":
      case "talk":
        return (
          <g transform="translate(300 452)">
            <path
              d={expr === "laugh" ? "M-30 -4 Q0 40 30 -4 Z" : "M-16 -2 Q0 22 16 -2 Z"}
              fill="#7A2E2E"
              stroke={LINE}
              strokeWidth={2.4}
              strokeLinejoin="round"
            />
            {expr === "laugh" && <path d="M-26 -2 L26 -2 L22 6 L-22 6 Z" fill="#FFFBF6" />}
          </g>
        );
      case "surprised":
        return (
          <g transform="translate(300 452)">
            <ellipse rx={12} ry={15} fill="#7A2E2E" stroke={LINE} strokeWidth={2} />
          </g>
        );
      case "worried":
        return (
          <path
            d="M286 458 Q300 448 314 458"
            stroke={LINE}
            strokeWidth={2.8}
            fill="none"
            strokeLinecap="round"
          />
        );
      case "angry":
        return (
          <g transform="translate(300 456)">
            <path d="M-32 -6 Q0 -16 32 -6 Q22 36 0 38 Q-22 36 -32 -6 Z" fill="#7A2E2E" stroke={LINE} strokeWidth={2.6} />
            <path d="M-26 -6 Q0 -13 26 -6 L22 4 Q0 0 -22 4 Z" fill="#FFFBF6" />
          </g>
        );
      case "sideeye":
      case "neutral":
      case "sleepy":
        return (
          <path
            d="M288 454 L312 454"
            stroke={LINE}
            strokeWidth={2.6}
            strokeLinecap="round"
          />
        );
      default:
        return (
          <path
            d="M278 448 Q300 470 322 448"
            stroke={LINE}
            strokeWidth={3}
            fill="none"
            strokeLinecap="round"
          />
        );
    }
  })();

  const backHair = (() => {
    switch (hair) {
      case "nani":
        return (
          <g>
            <circle cx={300} cy={182} r={0} />
            <path
              d="M170 340 C160 230 222 166 300 164 C378 166 440 230 430 340 C420 380 400 400 380 404 L220 404 C200 400 180 380 170 340 Z"
              fill={hairColor}
            />
            {/* low bun peeking out behind the neck */}
            <ellipse cx={300} cy={470} rx={92} ry={56} fill={hairColor} />
            <path d="M232 462 Q300 440 368 462" stroke="#FFFFFF" strokeWidth={4} fill="none" opacity={0.5} />
          </g>
        );
      case "bob":
        return (
          <path
            d="M150 350 C140 220 214 150 300 150 C386 150 460 220 450 350 C448 400 440 450 420 478 L180 478 C160 450 152 400 150 350 Z"
            fill={hairColor}
          />
        );
      case "ponytail":
        return (
          <g>
            <path
              d="M168 330 C160 226 224 160 300 160 C376 160 440 226 432 330 L432 380 L168 380 Z"
              fill={hairColor}
            />
            <path
              d="M410 190 C480 200 500 300 470 420 C460 470 430 520 420 560 C440 470 450 380 420 290 Z"
              fill={hairColor}
            />
          </g>
        );
      case "curly":
        return (
          <g fill={hairColor}>
            {Array.from({ length: 14 }, (_, i) => {
              const a = Math.PI * (1.02 + (i / 13) * 0.96);
              return (
                <circle
                  key={i}
                  cx={300 + Math.cos(a) * 150}
                  cy={300 + Math.sin(a) * 150}
                  r={46}
                />
              );
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
            <path
              d="M182 318 C176 232 232 176 300 176 C368 176 424 232 418 318 C404 270 370 228 304 214 L300 200 L296 214 C230 228 196 270 182 318 Z"
              fill={hairColor}
            />
            <path
              d="M300 184 L300 214"
              stroke="#E8DFD8"
              strokeWidth={3}
              opacity={0.8}
            />
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
          <path
            d="M180 300 C168 214 226 150 304 152 C382 154 434 214 420 300 C414 262 400 240 380 230 C352 246 300 250 250 236 C214 246 192 268 180 300 Z"
            fill={hairColor}
          />
        );
      case "bob":
        return (
          <path
            d="M176 330 C168 230 230 170 300 170 C370 170 432 230 424 330 C400 280 360 260 330 250 C300 262 240 270 210 280 C196 296 184 312 176 330 Z"
            fill={hairColor}
          />
        );
      case "ponytail":
        return (
          <path
            d="M180 320 C172 228 232 172 300 172 C368 172 428 228 420 320 C404 268 360 232 300 226 C252 232 204 262 180 320 Z"
            fill={hairColor}
          />
        );
      case "curly":
        return (
          <g fill={hairColor}>
            {Array.from({ length: 7 }, (_, i) => (
              <circle key={i} cx={210 + i * 30} cy={214 + Math.abs(3 - i) * 8} r={30} />
            ))}
          </g>
        );
      case "bald":
        return (
          <path
            d="M182 318 C178 290 182 262 194 244 M418 318 C422 290 418 262 406 244"
            stroke={hairColor}
            strokeWidth={22}
            fill="none"
            strokeLinecap="round"
          />
        );
    }
  })();

  const body = (() => {
    const shoulders =
      "M20 800 C30 730 90 672 188 642 C204 634 214 626 222 618 L378 618 C386 626 396 634 412 642 C510 672 570 730 580 800 Z";
    switch (outfit) {
      case "saree":
        return (
          <g>
            <path d={shoulders} fill={cloth} stroke={LINE} strokeWidth={2.4} />
            {/* blouse neckline */}
            <path d="M222 618 C250 660 350 660 378 618 Z" fill={url("skin")} stroke={LINE} strokeWidth={2} />
            {/* pallu across the left shoulder with a border */}
            <path
              d="M360 612 C420 640 470 700 520 800 L360 800 C340 740 330 680 340 620 Z"
              fill={accent}
              stroke={LINE}
              strokeWidth={2.2}
              opacity={0.95}
            />
            <path d="M372 618 C430 650 476 712 512 800" stroke="#C2410C" strokeWidth={14} fill="none" opacity={0.8} />
            <path d="M372 618 C430 650 476 712 512 800" stroke="#FBBF24" strokeWidth={4} fill="none" strokeDasharray="8 10" />
          </g>
        );
      case "shirt":
      case "blazer":
        return (
          <g>
            <path d={shoulders} fill={outfit === "blazer" ? "#334155" : cloth} stroke={LINE} strokeWidth={2.4} />
            {outfit === "blazer" && (
              <path d="M240 626 L300 760 L360 626 L330 618 L300 690 L270 618 Z" fill={cloth} stroke={LINE} strokeWidth={2} />
            )}
            <path d="M246 618 L300 680 L354 618 L332 610 L300 650 L268 610 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={2} />
            <circle cx={300} cy={710} r={4} fill={LINE} opacity={outfit === "shirt" ? 0.6 : 0} />
          </g>
        );
      case "hoodie":
        return (
          <g>
            <path d={shoulders} fill={cloth} stroke={LINE} strokeWidth={2.4} />
            <path
              d="M196 640 C220 700 380 700 404 640 C380 660 340 672 300 672 C260 672 220 660 196 640 Z"
              fill="#00000022"
              stroke={LINE}
              strokeWidth={2}
            />
            <path d="M282 676 L278 740 M318 676 L322 740" stroke={accent} strokeWidth={4} strokeLinecap="round" />
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
          <stop offset="0%" stopColor={skin} stopOpacity={0.85} />
          <stop offset="100%" stopColor={skin} />
        </linearGradient>
        <filter id={u("sketch")} x="-5%" y="-5%" width="110%" height="110%">
          <feTurbulence type="fractalNoise" baseFrequency={0.05} numOctaves={2} seed={7} result="n" />
          <feDisplacementMap in="SourceGraphic" in2="n" scale={2.4} xChannelSelector="R" yChannelSelector="G" />
        </filter>
      </defs>
      <g filter={url("sketch")}>
        {backHair}
        {/* Neck */}
        <path
          d="M266 476 C266 520 262 548 252 574 L348 574 C338 548 334 520 334 476 Z"
          fill={url("skin")}
          stroke={LINE}
          strokeWidth={2.2}
        />
        <g transform="translate(0 -44)">{body}</g>
        {/* Ears */}
        <ellipse cx={180} cy={356} rx={16} ry={26} fill={skin} stroke={LINE} strokeWidth={2} />
        <ellipse cx={420} cy={356} rx={16} ry={26} fill={skin} stroke={LINE} strokeWidth={2} />
        <path d={FACE} fill={url("skin")} stroke={LINE} strokeWidth={2.2} strokeLinejoin="round" />
        <ellipse cx={226} cy={404} rx={46} ry={24} fill={url("blush")} />
        <ellipse cx={374} cy={404} rx={46} ry={24} fill={url("blush")} />
        {aged && (
          <g stroke={LINE} strokeWidth={1.8} fill="none" opacity={0.45} strokeLinecap="round">
            <path d="M246 428 Q236 452 250 474 M354 428 Q364 452 350 474" />
            <path d="M196 352 l-12 -6 M196 364 l-14 2 M404 352 l12 -6 M404 364 l14 2" />
            <path d="M266 268 Q300 260 334 268" />
          </g>
        )}
        {brow(245, true)}
        {brow(355, false)}
        {eye(245, 362, true)}
        {eye(355, 362, false)}
        <path
          d="M302 392 Q309 406 299 412"
          stroke={LINE}
          strokeWidth={2}
          fill="none"
          strokeLinecap="round"
          opacity={0.75}
        />
        {mouth}
        {beard && (
          <path
            d="M220 440 C236 500 268 528 300 530 C332 528 364 500 380 440 C364 476 340 492 300 494 C260 492 236 476 220 440 Z"
            fill={hairColor}
            opacity={0.85}
          />
        )}
        {bindi && <circle cx={300} cy={300} r={7} fill="#B91C1C" />}
        {frontHair}
        {glasses && (
          <g stroke={LASH} strokeWidth={4} fill="#FFFFFF22">
            <rect x={196} y={326} width={98} height={74} rx={30} />
            <rect x={306} y={326} width={98} height={74} rx={30} />
            <path d="M294 352 Q300 346 306 352 M196 350 L180 344 M404 350 L420 344" fill="none" />
          </g>
        )}
      </g>
    </g>
  );
};
