import { useId } from "react";

export type MeeraExpr = "awe" | "smile" | "focus";
type Light = "sunset" | "night";

const DEFAULT_INK = "#2A1A22";
const HAIR = "#2A2050";
const HAIR_DARK = "#140F2A";

/**
 * Meera: a young designer, 3/4 view facing left, painted-anime rendering.
 * Local box is 800 x 900. Key light: warm sunset from the right (near side),
 * cool fill on the far side; "night" swaps in a phone glow from below.
 */
export const Meera: React.FC<{
  x: number;
  y: number;
  s: number;
  expr?: MeeraExpr;
  light?: Light;
  look?: number;
  /** Line-art color; the painted style uses a softer, warmer ink. */
  ink?: string;
}> = ({
  x,
  y,
  s,
  expr = "awe",
  light = "sunset",
  look = -1,
  ink = DEFAULT_INK,
}) => {
  const LINE = ink;
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const skin = "#F3C9A8";
  const skinShade = light === "sunset" ? "#C98F7A" : "#8E8AB8";
  const rim = light === "sunset" ? "#FFB27A" : "#8FD3FF";
  const face =
    "M336 296 C326 340 320 380 328 414 C334 446 356 484 390 510 Q401 518 413 512 C458 490 496 456 508 414 C516 376 514 330 506 288 Z";
  const lx = look * 5;

  const eye = (cx: number, cy: number, w: number, h: number, near: boolean) => {
    const top = `M${cx - w / 2} ${cy - 2} Q${cx - w * 0.1} ${cy - h * 0.75} ${cx + w / 2} ${cy - h * 0.15}`;
    const white = `${top} Q${cx + w * 0.1} ${cy + h * 0.55} ${cx - w / 2} ${cy - 2} Z`;
    const irx = near ? w * 0.26 : w * 0.22;
    const iry = h * 0.62;
    const icx = cx + lx + (near ? -3 : -2);
    const icy = cy + 2;
    if (expr === "smile") {
      return (
        <path
          d={`M${cx - w / 2} ${cy + 2} Q${cx} ${cy - h * 0.55} ${cx + w / 2} ${cy}`}
          stroke={LINE}
          strokeWidth={near ? 6 : 5}
          fill="none"
          strokeLinecap="round"
        />
      );
    }
    return (
      <g>
        <clipPath id={`eye${id}${near ? "n" : "f"}`}>
          <path d={white} />
        </clipPath>
        <path d={white} fill="#FBF6F2" />
        <g clipPath={`url(#eye${id}${near ? "n" : "f"})`}>
          <ellipse
            cx={icx}
            cy={icy}
            rx={irx}
            ry={iry}
            fill={`url(#iris${id})`}
          />
          <ellipse
            cx={icx}
            cy={icy}
            rx={irx}
            ry={iry}
            fill="none"
            stroke="#1B0C05"
            strokeWidth={2.5}
          />
          <ellipse
            cx={icx}
            cy={icy - 1}
            rx={irx * 0.45}
            ry={iry * 0.5}
            fill="#120703"
          />
          {/* lid shadow */}
          <path
            d={`${top} L${cx + w / 2} ${cy - h} L${cx - w / 2} ${cy - h} Z`}
            fill="#6B4E5E"
            opacity={0.0}
          />
          <path
            d={`M${cx - w / 2} ${cy - 4} Q${cx} ${cy - h * 0.8} ${cx + w / 2} ${cy - h * 0.2} L${cx + w / 2} ${cy - h * 0.05} Q${cx} ${cy - h * 0.45} ${cx - w / 2} ${cy + 4} Z`}
            fill="#8A6F7E"
            opacity={0.45}
          />
          <circle
            cx={icx - irx * 0.4}
            cy={icy - iry * 0.45}
            r={near ? 8 : 5}
            fill="#FFFFFF"
          />
          <circle
            cx={icx + irx * 0.45}
            cy={icy + iry * 0.35}
            r={near ? 3 : 2}
            fill="#FFFFFF"
          />
          {/* the firefly, reflected */}
          <circle
            cx={icx - irx * 0.1}
            cy={icy + iry * 0.25}
            r={near ? 3.5 : 2.5}
            fill="#FFD36B"
          />
        </g>
        {/* Upper lash: heavy, tapered, with an outer flick */}
        <path
          d={`M${cx - w / 2 - 3} ${cy} Q${cx - w * 0.1} ${cy - h * 0.86} ${cx + w / 2 + 2} ${cy - h * 0.2} L${cx + w / 2 + (near ? 12 : 6)} ${cy - h * 0.42} L${cx + w / 2 + 2} ${cy - h * 0.02} Q${cx - w * 0.1} ${cy - h * 0.62} ${cx - w / 2 + 2} ${cy + 3} Z`}
          fill={LINE}
        />
        {near && (
          <path
            d={`M${cx + w * 0.1} ${cy + h * 0.42} Q${cx + w * 0.35} ${cy + h * 0.32} ${cx + w * 0.48} ${cy + h * 0.1}`}
            stroke={LINE}
            strokeWidth={2.5}
            fill="none"
            strokeLinecap="round"
          />
        )}
      </g>
    );
  };

  const mouth =
    expr === "awe" ? (
      <path
        d="M394 470 Q406 465 420 468 Q413 482 401 480 Q395 477 394 470 Z"
        fill="#7A2E3A"
        stroke={LINE}
        strokeWidth={2.5}
        strokeLinejoin="round"
      />
    ) : expr === "smile" ? (
      <path
        d="M388 466 Q405 480 424 463"
        stroke={LINE}
        strokeWidth={4}
        fill="none"
        strokeLinecap="round"
      />
    ) : (
      <path
        d="M394 472 Q407 469 420 471"
        stroke={LINE}
        strokeWidth={3.5}
        fill="none"
        strokeLinecap="round"
      />
    );

  const browLift = expr === "awe" ? -6 : expr === "focus" ? 4 : 0;

  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <defs>
        <linearGradient id={`iris${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#1E0E06" />
          <stop offset="55%" stopColor="#7A3E14" />
          <stop offset="100%" stopColor="#F0B062" />
        </linearGradient>
        <clipPath id={`face${id}`}>
          <path d={face} />
        </clipPath>
        <radialGradient id={`blush${id}`}>
          <stop offset="0%" stopColor="#F2777E" stopOpacity={0.55} />
          <stop offset="100%" stopColor="#F2777E" stopOpacity={0} />
        </radialGradient>
        <linearGradient id={`hairRim${id}`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="60%" stopColor={rim} stopOpacity={0} />
          <stop offset="100%" stopColor={rim} stopOpacity={0.9} />
        </linearGradient>
        <linearGradient id={`phone${id}`} x1="0" y1="1" x2="0" y2="0">
          <stop offset="0%" stopColor="#7DD3FC" stopOpacity={0.55} />
          <stop offset="60%" stopColor="#7DD3FC" stopOpacity={0} />
        </linearGradient>
        <pattern
          id={`print${id}`}
          width={26}
          height={26}
          patternUnits="userSpaceOnUse"
        >
          <rect width={26} height={26} fill="#D99A2B" />
          <circle cx={13} cy={13} r={4} fill="#9A3412" opacity={0.55} />
          <circle cx={0} cy={0} r={2.5} fill="#FDE68A" opacity={0.6} />
          <circle cx={26} cy={26} r={2.5} fill="#FDE68A" opacity={0.6} />
        </pattern>
      </defs>

      {/* Bun + pencil (behind the head) */}
      <g transform="translate(8 -28) rotate(-28 560 210)">
        <rect
          x={470}
          y={198}
          width={190}
          height={16}
          rx={3}
          fill="#FACC15"
          stroke={LINE}
          strokeWidth={3}
        />
        <rect
          x={650}
          y={198}
          width={22}
          height={16}
          rx={4}
          fill="#F472B6"
          stroke={LINE}
          strokeWidth={3}
        />
        <path
          d="M470 198 L446 206 L470 214 Z"
          fill="#F5D0A9"
          stroke={LINE}
          strokeWidth={3}
          strokeLinejoin="round"
        />
      </g>
      <circle
        cx={556}
        cy={200}
        r={64}
        fill={HAIR}
        stroke={LINE}
        strokeWidth={4}
      />
      <path
        d="M508 182 Q556 142 602 186 M520 222 Q564 186 608 218"
        stroke="#3A3466"
        strokeWidth={6}
        fill="none"
        strokeLinecap="round"
      />

      {/* Back hair mass */}
      <path
        d="M310 326 C290 214 360 150 444 148 C530 146 594 204 584 300 C578 344 562 372 540 386 L516 370 L512 330 Z"
        fill={HAIR}
        stroke={LINE}
        strokeWidth={3}
      />
      <path
        d="M540 386 C556 420 548 470 530 500 L506 470 Z"
        fill={HAIR_DARK}
        stroke={LINE}
        strokeWidth={3}
        strokeLinejoin="round"
      />

      {/* Neck */}
      <path
        d="M408 498 L466 484 L476 650 Q440 670 398 650 Z"
        fill={skin}
        stroke={LINE}
        strokeWidth={3}
      />
      <path
        d="M408 498 L466 484 L468 522 Q440 538 410 530 Z"
        fill={skinShade}
      />

      {/* Body: kurta + denim jacket */}
      <path
        d="M214 900 C226 790 254 706 322 668 C346 654 368 634 382 616 L498 608 C520 630 560 650 604 672 C664 708 684 792 692 900 Z"
        fill="#2F4A75"
        stroke={LINE}
        strokeWidth={5}
      />
      <path
        d="M372 620 L500 610 C500 668 470 728 440 768 C410 728 384 668 372 620 Z"
        fill={`url(#print${id})`}
        stroke={LINE}
        strokeWidth={4}
      />
      <path
        d="M408 620 Q440 656 470 616"
        fill="none"
        stroke="#7C2D12"
        strokeWidth={5}
      />
      {/* Jacket collar folds */}
      <path
        d="M372 620 L330 668 L360 728 L400 668 Z"
        fill="#3E5F93"
        stroke={LINE}
        strokeWidth={4}
        strokeLinejoin="round"
      />
      <path
        d="M500 610 L560 658 L520 738 L478 668 Z"
        fill="#3E5F93"
        stroke={LINE}
        strokeWidth={4}
        strokeLinejoin="round"
      />
      <path
        d="M560 658 C612 690 652 770 668 900 L600 900 C590 800 560 730 520 738 Z"
        fill="#24395E"
      />
      <path
        d="M250 820 L262 900 M630 820 L618 900"
        stroke="#5B7DB5"
        strokeWidth={3}
        strokeDasharray="8 8"
      />

      {/* Ear + earbud + jhumka */}
      <path
        d="M506 372 C530 360 544 384 536 408 C530 426 516 436 506 432"
        fill={skin}
        stroke={LINE}
        strokeWidth={4}
      />
      <ellipse
        cx={522}
        cy={398}
        rx={7}
        ry={9}
        fill="#F8FAFC"
        stroke={LINE}
        strokeWidth={2.5}
      />
      <g transform="translate(520 432)">
        <line x1={0} y1={0} x2={0} y2={12} stroke="#B45309" strokeWidth={3} />
        <path
          d="M-14 28 Q0 4 14 28 Z"
          fill="#F59E0B"
          stroke="#7C2D12"
          strokeWidth={2.5}
        />
        {[-10, -5, 0, 5, 10].map((bx) => (
          <circle
            key={bx}
            cx={bx}
            cy={32}
            r={2.6}
            fill="#FDE68A"
            stroke="#7C2D12"
            strokeWidth={1}
          />
        ))}
      </g>

      {/* Face */}
      <path
        d={face}
        fill={skin}
        stroke={LINE}
        strokeWidth={3}
        strokeLinejoin="round"
      />
      <g clipPath={`url(#face${id})`}>
        {/* Far-side cool shadow (cel) */}
        <path
          d="M300 280 L352 280 C344 330 338 380 350 420 C360 452 374 482 398 520 L300 540 Z"
          fill={skinShade}
          opacity={0.65}
        />
        {/* Hair shadow on forehead */}
        <path
          d="M300 280 L520 280 L520 330 C480 316 460 344 430 330 C400 348 370 322 340 340 Z"
          fill={skinShade}
        />
        {light === "night" && (
          <rect
            x={300}
            y={280}
            width={240}
            height={260}
            fill={`url(#phone${id})`}
          />
        )}
        {/* Warm rim on the near cheek */}
        <path
          d="M498 300 C512 360 508 430 470 490 L520 520 L540 280 Z"
          fill={rim}
          opacity={0.55}
        />
        <ellipse cx={472} cy={430} rx={30} ry={15} fill={`url(#blush${id})`} />
        <ellipse cx={352} cy={428} rx={15} ry={11} fill={`url(#blush${id})`} />
      </g>

      {/* Brows */}
      <path
        d={`M344 ${344 + browLift} Q368 ${332 + browLift} 396 ${338 + browLift}`}
        stroke={LINE}
        strokeWidth={4.5}
        fill="none"
        strokeLinecap="round"
      />
      <path
        d={`M420 ${334 + browLift} Q456 ${322 + browLift} 496 ${336 + browLift}`}
        stroke={LINE}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
      />
      {eye(370, 384, 54, 44, false)}
      {eye(456, 382, 82, 56, true)}
      {/* Nose */}
      <path
        d="M406 410 L394 440 Q398 446 406 443"
        stroke={LINE}
        strokeWidth={3}
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx={410} cy={432} r={2.5} fill="#FFF1E6" />
      {mouth}

      {/* Front hair: side-swept bangs, far-side lock, loose near strand */}
      <path
        d="M326 328 C310 236 378 174 452 178 C516 182 552 232 530 300 C520 270 500 258 486 262 C496 290 494 318 482 340 C474 300 452 282 432 280 C440 304 436 330 424 346 C418 312 398 296 372 300 C382 320 374 344 358 362 C356 340 346 332 330 330 Z"
        fill={HAIR}
        stroke={LINE}
        strokeWidth={4}
        strokeLinejoin="round"
      />
      <path
        d="M332 320 C310 380 316 450 338 500 C330 440 336 392 352 350 Z"
        fill={HAIR}
        stroke={LINE}
        strokeWidth={4}
        strokeLinejoin="round"
      />
      <path
        d="M512 300 C540 360 506 420 532 480 C548 520 520 560 536 600"
        stroke={HAIR}
        strokeWidth={9}
        fill="none"
        strokeLinecap="round"
      />
      <path
        d="M512 300 C540 360 506 420 532 480 C548 520 520 560 536 600"
        stroke={rim}
        strokeWidth={2.5}
        fill="none"
        strokeLinecap="round"
        opacity={0.8}
      />
      {/* Strand separation and highlights so the bangs read as hair, not a helmet */}
      <g fill="none" strokeLinecap="round">
        {[
          "M380 196 C368 236 366 270 372 300",
          "M420 186 C412 230 418 262 432 280",
          "M462 188 C466 226 478 248 486 262",
          "M500 200 C512 232 520 250 530 298",
          "M352 226 C336 262 332 296 340 330",
        ].map((d) => (
          <path
            key={d}
            d={d}
            stroke={HAIR_DARK}
            strokeWidth={3}
            opacity={0.8}
          />
        ))}
        {[
          "M392 204 C384 236 384 260 390 284",
          "M446 194 C446 222 454 244 464 258",
          "M486 206 C496 228 504 246 512 270",
        ].map((d) => (
          <path
            key={d}
            d={d}
            stroke="#6E68C0"
            strokeWidth={2.5}
            opacity={0.7}
          />
        ))}
      </g>
      {/* Hair shine: cool angel ring + warm rim */}
      <path
        d="M372 226 Q440 200 512 228 L506 240 Q440 214 378 238 Z"
        fill="#5C5AA8"
        opacity={0.75}
      />
      <path
        d="M310 326 C290 214 360 150 444 148 C530 146 592 204 580 296"
        fill="none"
        stroke={`url(#hairRim${id})`}
        strokeWidth={8}
      />
      <path
        d="M548 166 C590 180 612 220 606 260"
        stroke={rim}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
        opacity={0.9}
      />
      <path
        d="M360 236 C380 216 420 204 450 206"
        stroke={HAIR_DARK}
        strokeWidth={4}
        fill="none"
        opacity={0.6}
      />
    </g>
  );
};
