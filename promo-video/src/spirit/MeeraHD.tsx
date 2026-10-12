import { useId } from "react";
import type { MeeraExpr } from "./Meera";

type Light = "sunset" | "night";

/**
 * Meera, semi-realistic anime rendering (3/4 view, facing left).
 * Built on a real head construction (brow ridge, eye socket, cheekbone,
 * jaw to chin) with soft gradient shading instead of flat cel blocks.
 * Drawn in a 1000 x 1125 space and scaled into the same 800 x 900 box as
 * <Meera>, so it is a drop-in replacement.
 */
export const MeeraHD: React.FC<{
  x: number;
  y: number;
  s: number;
  expr?: MeeraExpr;
  light?: Light;
  look?: number;
  ink?: string;
}> = ({
  x,
  y,
  s,
  expr = "awe",
  light = "sunset",
  look = -1,
  ink = "#5A3A3A",
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const u = (k: string) => `${k}${id}`;
  const url = (k: string) => `url(#${k}${id})`;
  const rim = light === "sunset" ? "#FFB27A" : "#8FD3FF";
  const shade = light === "sunset" ? "#C98673" : "#7F7FB8";
  const HAIR = "#211B3A";

  const face =
    "M430 290 C392 320 374 370 376 410 C377 430 370 440 372 455 C375 470 384 478 382 492 C378 510 368 520 372 540 C378 575 392 600 406 625 C422 652 448 684 484 698 C498 703 512 702 526 696 C556 676 586 646 604 606 C614 584 620 562 622 536 L650 420 C664 330 624 262 552 250 C500 246 455 262 430 290 Z";

  const eye = (cx: number, cy: number, w: number, h: number, near: boolean) => {
    const dir = near ? 1 : -1;
    const xi = cx - (dir * w) / 2;
    const xo = cx + (dir * w) / 2;
    const upper = `M${xi} ${cy} C${xi + dir * w * 0.2} ${cy - h * 0.78} ${xo - dir * w * 0.25} ${cy - h * 0.74} ${xo} ${cy - h * 0.14}`;
    const shape = `${upper} C${xo - dir * w * 0.2} ${cy + h * 0.45} ${xi + dir * w * 0.3} ${cy + h * 0.52} ${xi} ${cy} Z`;
    const k = near ? "n" : "f";
    if (expr === "smile") {
      return (
        <g>
          <path
            d={`M${xi} ${cy + 2} C${xi + dir * w * 0.25} ${cy - h * 0.55} ${xo - dir * w * 0.25} ${cy - h * 0.55} ${xo} ${cy}`}
            stroke={ink}
            strokeWidth={near ? 5.5 : 4.5}
            fill="none"
            strokeLinecap="round"
          />
          <path
            d={`M${xi + dir * w * 0.15} ${cy + h * 0.32} Q${cx} ${cy + h * 0.12} ${xo - dir * w * 0.1} ${cy + h * 0.25}`}
            stroke={ink}
            strokeWidth={2}
            fill="none"
            opacity={0.5}
          />
        </g>
      );
    }
    const rx = near ? w * 0.25 : w * 0.21;
    const ry = near ? w * 0.27 : w * 0.26;
    const icx = cx + look * w * 0.1 + (near ? -w * 0.02 : w * 0.04);
    const icy = cy + h * 0.06 + (expr === "focus" ? h * 0.05 : 0);
    return (
      <g>
        <clipPath id={u(`eye${k}`)}>
          <path d={shape} />
        </clipPath>
        <path d={shape} fill="#FBF7F4" />
        <g clipPath={url(`eye${k}`)}>
          <ellipse cx={icx} cy={icy} rx={rx} ry={ry} fill={url("iris")} />
          {Array.from({ length: 14 }, (_, i) => {
            const a = (i / 14) * Math.PI * 2;
            return (
              <line
                key={i}
                x1={icx + Math.cos(a) * rx * 0.45}
                y1={icy + Math.sin(a) * ry * 0.45}
                x2={icx + Math.cos(a) * rx * 0.92}
                y2={icy + Math.sin(a) * ry * 0.92}
                stroke="#E9A86A"
                strokeWidth={1.2}
                opacity={0.45}
              />
            );
          })}
          <ellipse
            cx={icx}
            cy={icy}
            rx={rx}
            ry={ry}
            fill="none"
            stroke="#1A0B05"
            strokeWidth={near ? 3 : 2.4}
          />
          <ellipse
            cx={icx}
            cy={icy}
            rx={rx * 0.42}
            ry={ry * 0.44}
            fill="#0E0503"
          />
          {/* Lid shadow on the eye */}
          <path
            d={`${upper} L${xo} ${cy - h * 1.2} L${xi} ${cy - h * 1.2} Z`}
            fill="#6B4A52"
            opacity={0.0}
          />
          <path
            d={`M${xi} ${cy - 2} C${xi + dir * w * 0.2} ${cy - h * 0.8} ${xo - dir * w * 0.25} ${cy - h * 0.76} ${xo} ${cy - h * 0.16} L${xo} ${cy - h * 0.02} C${xo - dir * w * 0.25} ${cy - h * 0.42} ${xi + dir * w * 0.2} ${cy - h * 0.45} ${xi} ${cy + 6} Z`}
            fill="#7A5560"
            opacity={0.38}
          />
          <circle
            cx={icx - rx * 0.38}
            cy={icy - ry * 0.42}
            r={near ? 6 : 4.2}
            fill="#FFFFFF"
          />
          <circle
            cx={icx + rx * 0.42}
            cy={icy + ry * 0.38}
            r={near ? 2.6 : 1.8}
            fill="#FFFFFF"
            opacity={0.9}
          />
          <path
            d={`M${icx - rx * 0.7} ${icy + ry * 0.45} Q${icx} ${icy + ry * 0.75} ${icx + rx * 0.6} ${icy + ry * 0.5}`}
            stroke="#F6C27E"
            strokeWidth={2}
            fill="none"
            opacity={0.7}
          />
        </g>
        {/* Upper lid line, thicker toward the outer corner, with a lash flick */}
        <path
          d={upper}
          stroke={ink}
          strokeWidth={near ? 5.5 : 4.2}
          fill="none"
          strokeLinecap="round"
        />
        <path
          d={`M${xo - dir * w * 0.12} ${cy - h * 0.36} L${xo + dir * w * 0.12} ${cy - h * 0.48} L${xo + dir * 2} ${cy - h * 0.1} Z`}
          fill={ink}
        />
        {/* Double-eyelid crease */}
        <path
          d={`M${xi + dir * w * 0.12} ${cy - h * 0.62} C${xi + dir * w * 0.3} ${cy - h * 1.12} ${xo - dir * w * 0.25} ${cy - h * 1.08} ${xo - dir * w * 0.02} ${cy - h * 0.55}`}
          stroke={ink}
          strokeWidth={1.8}
          fill="none"
          opacity={0.55}
        />
        {/* Lower lid: soft, outer two-thirds */}
        <path
          d={`M${xo} ${cy - h * 0.12} C${xo - dir * w * 0.2} ${cy + h * 0.45} ${cx} ${cy + h * 0.5} ${xi + dir * w * 0.25} ${cy + h * 0.4}`}
          stroke={ink}
          strokeWidth={1.6}
          fill="none"
          opacity={0.5}
        />
      </g>
    );
  };

  const browY = expr === "awe" ? -8 : expr === "focus" ? 5 : -2;

  return (
    <g transform={`translate(${x} ${y}) scale(${s * 0.8})`}>
      <defs>
        <linearGradient id={u("skin")} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#F8D9C2" />
          <stop offset="100%" stopColor="#EDB898" />
        </linearGradient>
        <radialGradient id={u("iris")} cx="50%" cy="65%" r="60%">
          <stop offset="0%" stopColor="#C77B3A" />
          <stop offset="55%" stopColor="#6B3412" />
          <stop offset="100%" stopColor="#241005" />
        </radialGradient>
        <radialGradient id={u("blush")}>
          <stop offset="0%" stopColor="#EF7C7C" stopOpacity={0.45} />
          <stop offset="100%" stopColor="#EF7C7C" stopOpacity={0} />
        </radialGradient>
        <linearGradient id={u("hair")} x1="0" y1="0" x2="0.4" y2="1">
          <stop offset="0%" stopColor="#3A3466" />
          <stop offset="45%" stopColor={HAIR} />
          <stop offset="100%" stopColor="#120E22" />
        </linearGradient>
        <linearGradient id={u("lip")} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#B65A5E" />
          <stop offset="100%" stopColor="#D98A84" />
        </linearGradient>
        <linearGradient id={u("under")} x1="0" y1="1" x2="0" y2="0">
          <stop offset="0%" stopColor="#7DD3FC" stopOpacity={0.5} />
          <stop offset="60%" stopColor="#7DD3FC" stopOpacity={0} />
        </linearGradient>
        <filter id={u("soft")} x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation={9} />
        </filter>
        <filter id={u("softer")} x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation={4} />
        </filter>
        <clipPath id={u("face")}>
          <path d={face} />
        </clipPath>
        <pattern
          id={u("print")}
          width={30}
          height={30}
          patternUnits="userSpaceOnUse"
        >
          <rect width={30} height={30} fill="#D99A2B" />
          <circle cx={15} cy={15} r={4.5} fill="#9A3412" opacity={0.55} />
          <circle cx={0} cy={0} r={3} fill="#FDE68A" opacity={0.6} />
          <circle cx={30} cy={30} r={3} fill="#FDE68A" opacity={0.6} />
        </pattern>
      </defs>

      {/* Bun and pencil, behind everything */}
      <g transform="rotate(-26 700 220)">
        <rect
          x={590}
          y={208}
          width={230}
          height={18}
          rx={4}
          fill="#FACC15"
          stroke={ink}
          strokeWidth={3}
        />
        <rect
          x={810}
          y={208}
          width={26}
          height={18}
          rx={5}
          fill="#F472B6"
          stroke={ink}
          strokeWidth={3}
        />
        <path
          d="M590 208 L562 217 L590 226 Z"
          fill="#F5D0A9"
          stroke={ink}
          strokeWidth={3}
          strokeLinejoin="round"
        />
      </g>
      <circle cx={694} cy={236} r={76} fill={url("hair")} />
      <path
        d="M634 214 Q694 168 752 220 M646 262 Q700 222 758 256"
        stroke="#4B4590"
        strokeWidth={6}
        fill="none"
        opacity={0.8}
        strokeLinecap="round"
      />
      <path
        d="M760 200 C780 240 774 280 750 300"
        stroke={rim}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
        opacity={0.85}
      />

      {/* Back hair */}
      <path
        d="M380 340 C360 232 452 168 562 176 C664 184 712 262 698 364 C692 430 676 500 646 522 C630 532 614 524 606 500 L600 330 Z"
        fill={url("hair")}
      />

      {/* Neck with the jaw's soft shadow */}
      <path
        d="M476 686 C476 730 472 760 466 800 L612 800 C606 760 600 716 606 602 Z"
        fill={url("skin")}
        stroke={ink}
        strokeWidth={2.5}
      />
      <path
        d="M480 694 C526 712 576 688 604 612 L604 716 C576 752 526 760 480 740 Z"
        fill={shade}
        opacity={0.55}
        filter={url("soft")}
      />
      <path
        d="M602 640 C598 700 600 750 608 790"
        stroke={rim}
        strokeWidth={5}
        fill="none"
        opacity={0.7}
      />

      {/* Body: kurta + denim jacket */}
      <g transform="translate(-8 -84)">
        <path
          d="M210 1125 C222 1020 262 950 336 914 C388 890 430 874 470 852 L630 852 C672 874 720 890 774 914 C848 950 884 1020 894 1125 Z"
          fill="#2F4A75"
          stroke={ink}
          strokeWidth={4}
        />
        <path
          d="M462 856 L636 856 C626 910 590 958 550 994 C510 958 478 910 462 856 Z"
          fill={url("print")}
          stroke={ink}
          strokeWidth={3}
        />
        <path
          d="M462 856 L414 906 L446 982 L490 906 Z"
          fill="#3E5F93"
          stroke={ink}
          strokeWidth={3}
          strokeLinejoin="round"
        />
        <path
          d="M636 856 L700 900 L656 990 L612 908 Z"
          fill="#3E5F93"
          stroke={ink}
          strokeWidth={3}
          strokeLinejoin="round"
        />
        <path
          d="M700 900 C780 930 846 1010 868 1125 L796 1125 C786 1040 740 980 656 990 Z"
          fill="#24395E"
        />
        <path
          d="M700 900 C780 930 846 1010 868 1125"
          stroke={rim}
          strokeWidth={4}
          fill="none"
          opacity={0.55}
        />
      </g>
      {/* Face */}
      <path d={face} fill={url("skin")} />
      <g clipPath={url("face")}>
        {/* Soft form shading: near-side plane, under the cheekbone, under the hair */}
        <path
          d="M574 420 C612 470 620 560 586 650 C560 688 532 702 514 704 L700 706 L700 400 Z"
          fill={shade}
          opacity={0.7}
          filter={url("soft")}
        />
        <path
          d="M560 560 C590 590 600 630 580 660"
          stroke={shade}
          strokeWidth={26}
          fill="none"
          opacity={0.25}
          filter={url("soft")}
        />
        <path
          d="M360 300 L700 300 L700 420 C640 400 600 440 560 420 C520 450 470 420 430 440 C410 430 390 440 360 430 Z"
          fill={shade}
          opacity={0.55}
          filter={url("soft")}
        />
        {/* Eye sockets, under-lip and cheekbone modeling */}
        <ellipse
          cx={520}
          cy={458}
          rx={64}
          ry={22}
          fill={shade}
          opacity={0.28}
          filter={url("soft")}
        />
        <ellipse
          cx={414}
          cy={462}
          rx={34}
          ry={16}
          fill={shade}
          opacity={0.25}
          filter={url("soft")}
        />
        <ellipse
          cx={446}
          cy={648}
          rx={24}
          ry={8}
          fill={shade}
          opacity={0.4}
          filter={url("softer")}
        />
        <path
          d="M470 520 L476 470"
          stroke="#FFFFFF"
          strokeWidth={6}
          opacity={0.18}
          filter={url("softer")}
        />
        {/* Nose side plane */}
        <path
          d="M444 470 C440 500 430 530 428 548 L446 556 C452 530 456 500 458 474 Z"
          fill={shade}
          opacity={0.55}
          filter={url("softer")}
        />
        {/* Blush */}
        <ellipse cx={568} cy={562} rx={46} ry={26} fill={url("blush")} />
        <ellipse cx={392} cy={556} rx={22} ry={18} fill={url("blush")} />
        {/* Rim light along the near jaw */}
        <path
          d="M604 606 C584 648 556 678 522 694"
          stroke={rim}
          strokeWidth={8}
          fill="none"
          opacity={0.75}
          filter={url("softer")}
        />
        {light === "night" && (
          <rect
            x={340}
            y={300}
            width={360}
            height={420}
            fill={url("under")}
            opacity={0.3}
          />
        )}
        {/* Forehead / cheek / chin highlights */}
        <ellipse
          cx={470}
          cy={600}
          rx={14}
          ry={8}
          fill="#FFFFFF"
          opacity={0.18}
          filter={url("softer")}
        />
        <ellipse
          cx={490}
          cy={688}
          rx={18}
          ry={7}
          fill="#FFFFFF"
          opacity={0.25}
          filter={url("softer")}
        />
      </g>
      <path
        d={face}
        fill="none"
        stroke={ink}
        strokeWidth={2.6}
        strokeLinejoin="round"
      />

      {/* Ear */}
      <path
        d="M622 452 C642 440 664 456 662 486 C660 520 646 548 626 552 C622 530 624 480 622 452 Z"
        fill="#EDB898"
        stroke={ink}
        strokeWidth={2.5}
      />
      <path
        d="M634 470 C648 472 652 492 644 512"
        stroke={ink}
        strokeWidth={2}
        fill="none"
        opacity={0.6}
      />
      <path
        d="M658 470 C664 500 654 530 638 548"
        stroke={rim}
        strokeWidth={4}
        fill="none"
        opacity={0.8}
      />
      {/* Jhumka */}
      <g transform="translate(634 556)">
        <line x1={0} y1={0} x2={0} y2={14} stroke="#B45309" strokeWidth={3} />
        <path
          d="M-16 32 Q0 6 16 32 Z"
          fill="#F59E0B"
          stroke="#7C2D12"
          strokeWidth={2.5}
        />
        {[-12, -6, 0, 6, 12].map((bx) => (
          <circle
            key={bx}
            cx={bx}
            cy={36}
            r={2.8}
            fill="#FDE68A"
            stroke="#7C2D12"
            strokeWidth={1}
          />
        ))}
      </g>

      {/* Brows: tapered */}
      <path
        d={`M488 ${428 + browY} C520 ${412 + browY} 556 ${412 + browY} 580 ${426 + browY} C556 ${420 + browY} 520 ${422 + browY} 490 ${434 + browY} Z`}
        fill={ink}
      />
      <path
        d={`M394 ${430 + browY} C410 ${418 + browY} 428 ${416 + browY} 446 ${420 + browY} C430 ${424 + browY} 412 ${426 + browY} 396 ${436 + browY} Z`}
        fill={ink}
      />

      {eye(526, 482, 94, 47, true)}
      {eye(420, 484, 62, 42, false)}

      {/* Nose */}
      <path
        d="M404 548 C410 558 422 562 434 556"
        stroke={ink}
        strokeWidth={2.6}
        fill="none"
        strokeLinecap="round"
      />
      <ellipse cx={418} cy={538} rx={4} ry={3} fill="#FFFFFF" opacity={0.55} />

      {/* Mouth */}
      {expr === "awe" && (
        <g>
          <path
            d="M416 606 Q446 616 474 605 Q446 630 416 606 Z"
            fill="#6E2A33"
          />
          <path
            d="M412 605 Q428 596 443 600 Q458 596 478 603 Q446 610 412 605 Z"
            fill={url("lip")}
          />
          <path
            d="M422 616 Q446 640 468 614 Q446 628 422 616 Z"
            fill="#D98A84"
          />
          <ellipse
            cx={446}
            cy={624}
            rx={8}
            ry={2.5}
            fill="#FFFFFF"
            opacity={0.4}
          />
        </g>
      )}
      {expr === "smile" && (
        <g>
          <path
            d="M410 598 Q446 630 484 594 Q446 616 410 598 Z"
            fill="#6E2A33"
          />
          <path
            d="M410 598 Q446 610 484 594"
            stroke="#B65A5E"
            strokeWidth={3}
            fill="none"
            strokeLinecap="round"
          />
          <path
            d="M424 618 Q446 632 468 616"
            stroke="#D98A84"
            strokeWidth={4}
            fill="none"
            strokeLinecap="round"
          />
        </g>
      )}
      {expr === "focus" && (
        <g>
          <path
            d="M416 606 Q446 600 476 604"
            stroke="#8E4A4E"
            strokeWidth={3}
            fill="none"
            strokeLinecap="round"
          />
          <path
            d="M424 612 Q446 624 468 611"
            stroke="#D98A84"
            strokeWidth={5}
            fill="none"
            strokeLinecap="round"
            opacity={0.8}
          />
        </g>
      )}

      {/* Front hair: bangs in clumps, far lock, loose near strand */}
      <path
        d="M372 404 C356 300 430 232 522 232 C608 232 668 290 664 382 L648 344 C654 384 642 404 624 424 C630 384 618 354 602 340 C592 384 572 414 548 432 C558 394 550 364 532 348 C520 388 498 412 472 424 C482 388 476 360 462 348 C448 376 422 396 396 406 C402 384 400 364 394 354 C386 374 380 390 372 404 Z"
        fill={url("hair")}
        stroke="#120E22"
        strokeWidth={2.5}
        strokeLinejoin="round"
      />
      <path
        d="M380 384 C362 440 372 500 364 556 C360 590 366 620 380 646 C380 610 386 580 388 548 C392 500 386 444 394 404 Z"
        fill={url("hair")}
        stroke="#120E22"
        strokeWidth={2.5}
      />
      <path
        d="M614 400 C640 470 614 540 638 610 C654 660 634 700 646 740"
        stroke={HAIR}
        strokeWidth={6}
        fill="none"
        strokeLinecap="round"
      />
      <path
        d="M614 400 C640 470 614 540 638 610 C654 660 634 700 646 740"
        stroke={rim}
        strokeWidth={2.5}
        fill="none"
        strokeLinecap="round"
        opacity={0.85}
      />
      {/* Strand lines and shine */}
      <g fill="none" strokeLinecap="round">
        {[
          "M420 270 C408 320 404 360 412 398",
          "M470 250 C462 300 466 340 476 412",
          "M520 246 C522 300 530 330 540 420",
          "M572 252 C584 296 596 320 608 404",
          "M624 270 C640 300 652 330 664 370",
        ].map((d) => (
          <path
            key={d}
            d={d}
            stroke="#0D0A1A"
            strokeWidth={2.4}
            opacity={0.75}
          />
        ))}
        {[
          "M440 276 C472 262 506 258 540 260",
          "M560 262 C590 266 620 278 646 296",
          "M410 300 C420 290 430 284 444 280",
        ].map((d) => (
          <path key={d} d={d} stroke="#8A84D8" strokeWidth={7} opacity={0.55} />
        ))}
      </g>
      <g fill="none" stroke={HAIR} strokeWidth={3} strokeLinecap="round">
        <path d="M470 236 C440 214 420 216 404 230" />
        <path d="M600 230 C626 206 650 206 668 222" />
        <path d="M530 230 C536 206 552 196 570 196" />
      </g>
      <path
        d="M664 382 C662 300 616 244 554 236"
        stroke={rim}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
        opacity={0.85}
      />
    </g>
  );
};
