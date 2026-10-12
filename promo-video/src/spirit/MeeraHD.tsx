import { useId } from "react";
import type { MeeraExpr } from "./Meera";

type Light = "sunset" | "night";

/**
 * Meera, bold semi-realistic anime heroine (3/4 view, facing left).
 * Upturned eyes with a kajal wing and lash clusters, arched brows, full
 * glossy lips, a gold nose stud and jhumkas, long hair flowing in the wind.
 * Drawn in a 1000 x 1125 space and scaled into <Meera>'s 800 x 900 box.
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
  ink = "#3A2226",
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const u = (k: string) => `${k}${id}`;
  const url = (k: string) => `url(#${k}${id})`;
  const rim = light === "sunset" ? "#FFB27A" : "#9AD8FF";
  const shade = light === "sunset" ? "#A8664A" : "#6C6AA8";
  const LASH = "#1A0C12";

  const face =
    "M430 290 C392 320 374 370 376 410 C377 430 370 440 372 455 C375 470 384 478 382 492 C378 510 368 520 372 540 C378 575 392 600 406 625 C420 640 448 668 482 678 C492 681 502 680 512 676 C548 660 582 634 604 600 C614 584 620 562 622 536 L650 420 C664 330 624 262 552 250 C500 246 455 262 430 290 Z";

  // Long hair flowing back in the wind
  const FLOW =
    "M380 340 C360 230 450 160 566 168 C680 176 740 250 740 330 C790 380 850 440 920 470 C960 486 990 480 1010 470 C990 520 940 540 890 530 C920 580 950 640 1000 680 C940 700 880 670 840 620 C850 700 880 780 930 840 C860 840 800 790 770 720 C760 800 770 880 800 960 C730 920 690 840 680 760 C670 700 650 640 640 600 L600 330 Z";

  const eye = (cx: number, cy: number, w: number, h: number, near: boolean) => {
    const dir = near ? 1 : -1;
    const xi = cx - (dir * w) / 2;
    const xo = cx + (dir * w) / 2;
    const yo = cy - h * 0.3; // upturned outer corner
    const upper = `M${xi} ${cy} C${xi + dir * w * 0.18} ${cy - h * 0.8} ${xo - dir * w * 0.28} ${cy - h * 0.86} ${xo} ${yo}`;
    const shape = `${upper} C${xo - dir * w * 0.16} ${cy + h * 0.3} ${xi + dir * w * 0.3} ${cy + h * 0.5} ${xi} ${cy} Z`;
    const k = near ? "n" : "f";
    // Kajal wing + heavy upper lash as one filled shape
    const lash = `${upper} L${xo + dir * w * 0.24} ${yo - h * 0.42} L${xo + dir * w * 0.02} ${yo + h * 0.08} C${xo - dir * w * 0.3} ${cy - h * 0.62} ${xi + dir * w * 0.18} ${cy - h * 0.6} ${xi + dir * 3} ${cy + 3} Z`;
    const clusters = [0.62, 0.78, 0.92].map((t, i) => {
      const bx = xi + dir * w * t;
      const by = cy - h * (0.78 - t * 0.38);
      return (
        <path
          key={i}
          d={`M${bx - dir * 4} ${by} Q${bx + dir * 6} ${by - h * 0.32} ${bx + dir * (14 + i * 4)} ${by - h * (0.36 + i * 0.06)} Q${bx + dir * 4} ${by - h * 0.12} ${bx + dir * 4} ${by + 2} Z`}
          fill={LASH}
        />
      );
    });

    if (expr === "smile") {
      const arc = `M${xi} ${cy + 4} C${xi + dir * w * 0.25} ${cy - h * 0.5} ${xo - dir * w * 0.25} ${cy - h * 0.62} ${xo} ${yo + 4}`;
      return (
        <g>
          <path
            d={arc}
            stroke={LASH}
            strokeWidth={near ? 7 : 5.5}
            fill="none"
            strokeLinecap="round"
          />
          <path
            d={`M${xo} ${yo + 4} L${xo + dir * w * 0.2} ${yo - h * 0.3}`}
            stroke={LASH}
            strokeWidth={near ? 6 : 4.5}
            strokeLinecap="round"
          />
          {clusters}
        </g>
      );
    }
    const rx = near ? w * 0.27 : w * 0.24;
    const ry = near ? w * 0.29 : w * 0.28;
    const icx = cx + look * w * 0.1 + (near ? -w * 0.02 : w * 0.04);
    const icy = cy - h * 0.02 + (expr === "focus" ? h * 0.06 : 0);
    return (
      <g>
        <clipPath id={u(`eye${k}`)}>
          <path d={shape} />
        </clipPath>
        <path d={shape} fill="#FFF8F4" />
        <g clipPath={url(`eye${k}`)}>
          <ellipse cx={icx} cy={icy} rx={rx} ry={ry} fill={url("iris")} />
          {Array.from({ length: 18 }, (_, i) => {
            const a = (i / 18) * Math.PI * 2;
            return (
              <line
                key={i}
                x1={icx + Math.cos(a) * rx * 0.42}
                y1={icy + Math.sin(a) * ry * 0.42}
                x2={icx + Math.cos(a) * rx * 0.9}
                y2={icy + Math.sin(a) * ry * 0.9}
                stroke="#FCD34D"
                strokeWidth={1.2}
                opacity={0.5}
              />
            );
          })}
          <ellipse
            cx={icx}
            cy={icy}
            rx={rx * 0.62}
            ry={ry * 0.62}
            fill="none"
            stroke="#FCD34D"
            strokeWidth={1.5}
            opacity={0.45}
          />
          <ellipse
            cx={icx}
            cy={icy}
            rx={rx}
            ry={ry}
            fill="none"
            stroke="#150702"
            strokeWidth={near ? 3.5 : 2.8}
          />
          <ellipse
            cx={icx}
            cy={icy}
            rx={rx * 0.4}
            ry={ry * 0.44}
            fill="#0B0402"
          />
          <path
            d={`M${xi} ${cy - 4} C${xi + dir * w * 0.18} ${cy - h * 0.84} ${xo - dir * w * 0.28} ${cy - h * 0.9} ${xo} ${yo - 4} L${xo} ${yo + h * 0.25} C${xo - dir * w * 0.28} ${cy - h * 0.3} ${xi + dir * w * 0.18} ${cy - h * 0.3} ${xi} ${cy + 8} Z`}
            fill="#5A3440"
            opacity={0.42}
          />
          <circle
            cx={icx - rx * 0.36}
            cy={icy - ry * 0.36}
            r={near ? 7.5 : 5}
            fill="#FFFFFF"
          />
          <circle
            cx={icx + rx * 0.42}
            cy={icy + ry * 0.4}
            r={near ? 3 : 2}
            fill="#FFFFFF"
          />
          <path
            d={`M${icx - rx * 0.75} ${icy + ry * 0.42} Q${icx} ${icy + ry * 0.82} ${icx + rx * 0.65} ${icy + ry * 0.5}`}
            stroke="#FFD58A"
            strokeWidth={2.5}
            fill="none"
            opacity={0.8}
          />
        </g>
        <path d={lash} fill={LASH} />
        {clusters}
        {/* Crease */}
        <path
          d={`M${xi + dir * w * 0.14} ${cy - h * 0.66} C${xi + dir * w * 0.3} ${cy - h * 1.2} ${xo - dir * w * 0.3} ${cy - h * 1.22} ${xo - dir * w * 0.02} ${yo - h * 0.5}`}
          stroke={ink}
          strokeWidth={2}
          fill="none"
          opacity={0.55}
        />
        {/* Lower kajal line */}
        <path
          d={`M${xo + dir * 2} ${yo + 2} C${xo - dir * w * 0.16} ${cy + h * 0.32} ${xi + dir * w * 0.3} ${cy + h * 0.52} ${xi + dir * 4} ${cy + 2}`}
          stroke={LASH}
          strokeWidth={near ? 2.6 : 2}
          fill="none"
          opacity={0.85}
        />
      </g>
    );
  };

  const browY = expr === "awe" ? -8 : expr === "focus" ? 4 : -3;

  return (
    <g transform={`translate(${x} ${y}) scale(${s * 0.8})`}>
      <defs>
        <linearGradient id={u("skin")} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#F4C6A0" />
          <stop offset="100%" stopColor="#D69A70" />
        </linearGradient>
        <radialGradient id={u("iris")} cx="50%" cy="68%" r="62%">
          <stop offset="0%" stopColor="#F6B35A" />
          <stop offset="45%" stopColor="#B05A1C" />
          <stop offset="100%" stopColor="#2A1206" />
        </radialGradient>
        <radialGradient id={u("blush")}>
          <stop offset="0%" stopColor="#D9626A" stopOpacity={0.5} />
          <stop offset="100%" stopColor="#D9626A" stopOpacity={0} />
        </radialGradient>
        <radialGradient id={u("glow")}>
          <stop offset="0%" stopColor="#FFF4E6" stopOpacity={0.35} />
          <stop offset="100%" stopColor="#FFF4E6" stopOpacity={0} />
        </radialGradient>
        <linearGradient id={u("hair")} x1="0" y1="0" x2="0.5" y2="1">
          <stop offset="0%" stopColor="#3C3470" />
          <stop offset="40%" stopColor="#221A40" />
          <stop offset="100%" stopColor="#0F0B1E" />
        </linearGradient>
        <linearGradient id={u("lip")} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#8E2440" />
          <stop offset="100%" stopColor="#C4455E" />
        </linearGradient>
        <linearGradient id={u("under")} x1="0" y1="1" x2="0" y2="0">
          <stop offset="0%" stopColor="#7DD3FC" stopOpacity={0.45} />
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

      {/* Long hair flowing behind in the wind: one wavy mass with three lobes */}
      <path d={FLOW} fill={url("hair")} />
      <g fill="none" strokeLinecap="round">
        {[
          "M700 260 C780 330 860 420 960 470",
          "M690 330 C760 420 830 520 950 640",
          "M670 420 C720 540 790 660 880 800",
          "M650 520 C680 640 720 760 770 900",
        ].map((d) => (
          <path key={d} d={d} stroke="#6E66C4" strokeWidth={4} opacity={0.5} />
        ))}
        {[
          "M720 300 C800 360 880 430 990 474",
          "M700 380 C760 470 830 560 960 660",
        ].map((d) => (
          <path key={d} d={d} stroke="#0A0716" strokeWidth={3} opacity={0.6} />
        ))}
        {/* Wisps escaping the silhouette */}
        {[
          "M1010 470 C1040 460 1060 440 1080 420",
          "M1000 680 C1030 700 1050 730 1060 760",
          "M930 840 C950 870 960 900 960 930",
        ].map((d) => (
          <path key={d} d={d} stroke="#2C2352" strokeWidth={3} />
        ))}
      </g>
      <path
        d="M566 168 C680 176 740 250 740 330 C790 380 850 440 920 470 C960 486 990 480 1010 470"
        stroke={rim}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
        opacity={0.85}
      />

      {/* Neck, collarbones, gold chain */}
      <path
        d="M474 666 C476 720 472 760 466 800 L612 800 C606 760 600 716 606 600 Z"
        fill={url("skin")}
        stroke={ink}
        strokeWidth={2.2}
      />
      <path
        d="M478 674 C524 692 576 670 604 606 L604 710 C576 746 524 752 478 730 Z"
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
      <path
        d="M474 760 Q540 812 606 760"
        stroke="#F5C451"
        strokeWidth={2.5}
        fill="none"
      />
      <circle
        cx={540}
        cy={789}
        r={6}
        fill="#F59E0B"
        stroke="#7C2D12"
        strokeWidth={1.5}
      />

      {/* Body: kurta + denim jacket */}
      <g transform="translate(-8 -84)">
        <path
          d="M210 1125 C222 1020 262 950 336 914 C388 890 430 874 470 852 L630 852 C672 874 720 890 774 914 C848 950 884 1020 894 1125 Z"
          fill="#2F4A75"
          stroke={ink}
          strokeWidth={3.5}
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
          opacity={0.6}
        />
      </g>

      {/* Face */}
      <path d={face} fill={url("skin")} />
      <g clipPath={url("face")}>
        <path
          d="M574 420 C612 470 620 560 586 650 C560 688 532 702 514 704 L700 706 L700 400 Z"
          fill={shade}
          opacity={0.7}
          filter={url("soft")}
        />
        <path
          d="M360 300 L700 300 L700 420 C640 400 600 440 560 420 C520 450 470 420 430 440 C410 430 390 440 360 430 Z"
          fill={shade}
          opacity={0.55}
          filter={url("soft")}
        />
        <ellipse
          cx={520}
          cy={456}
          rx={66}
          ry={22}
          fill={shade}
          opacity={0.3}
          filter={url("soft")}
        />
        <ellipse
          cx={446}
          cy={634}
          rx={22}
          ry={7}
          fill={shade}
          opacity={0.4}
          filter={url("softer")}
        />
        <path
          d="M444 470 C440 500 430 530 428 548 L446 556 C452 530 456 500 458 474 Z"
          fill={shade}
          opacity={0.5}
          filter={url("softer")}
        />
        <ellipse cx={560} cy={560} rx={50} ry={28} fill={url("blush")} />
        <ellipse cx={392} cy={556} rx={22} ry={18} fill={url("blush")} />
        {/* Dewy glow: cheekbone, nose bridge, chin, lip line */}
        <ellipse cx={548} cy={526} rx={30} ry={12} fill={url("glow")} />
        <ellipse cx={440} cy={505} rx={8} ry={26} fill={url("glow")} />
        <ellipse cx={492} cy={664} rx={18} ry={7} fill={url("glow")} />
        <path
          d="M604 600 C584 640 554 664 516 674"
          stroke={rim}
          strokeWidth={9}
          fill="none"
          opacity={0.8}
          filter={url("softer")}
        />
        {light === "night" && (
          <rect
            x={340}
            y={300}
            width={360}
            height={420}
            fill={url("under")}
            opacity={0.4}
          />
        )}
      </g>
      <path
        d={face}
        fill="none"
        stroke={ink}
        strokeWidth={2.4}
        strokeLinejoin="round"
      />

      {/* Ear */}
      <path
        d="M622 452 C642 440 664 456 662 486 C660 520 646 548 626 552 C622 530 624 480 622 452 Z"
        fill="#D69A70"
        stroke={ink}
        strokeWidth={2.2}
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
        opacity={0.85}
      />
      {/* Bold jhumka */}
      <g transform="translate(634 556)">
        <circle
          cx={0}
          cy={4}
          r={6}
          fill="#F59E0B"
          stroke="#7C2D12"
          strokeWidth={1.5}
        />
        <line x1={0} y1={10} x2={0} y2={22} stroke="#B45309" strokeWidth={3} />
        <path
          d="M-24 50 Q0 10 24 50 Z"
          fill="#F59E0B"
          stroke="#7C2D12"
          strokeWidth={2.5}
        />
        <path
          d="M-16 40 Q0 24 16 40"
          stroke="#FDE68A"
          strokeWidth={2}
          fill="none"
        />
        {[-18, -9, 0, 9, 18].map((bx) => (
          <circle
            key={bx}
            cx={bx}
            cy={55}
            r={3.4}
            fill="#FDE68A"
            stroke="#7C2D12"
            strokeWidth={1}
          />
        ))}
      </g>

      {/* Brows: bold, arched */}
      <path
        d={`M484 ${434 + browY} C510 ${404 + browY} 552 ${398 + browY} 584 ${414 + browY} C560 ${410 + browY} 520 ${414 + browY} 488 ${442 + browY} Z`}
        fill={LASH}
      />
      <path
        d={`M392 ${436 + browY} C406 ${414 + browY} 428 ${408 + browY} 448 ${414 + browY} C430 ${418 + browY} 412 ${426 + browY} 396 ${444 + browY} Z`}
        fill={LASH}
      />

      {eye(528, 484, 104, 54, true)}
      {eye(420, 488, 66, 46, false)}

      {/* Nose + gold stud */}
      <path
        d="M408 550 C414 558 424 561 434 555"
        stroke={ink}
        strokeWidth={2.4}
        fill="none"
        strokeLinecap="round"
      />
      <ellipse cx={420} cy={538} rx={4} ry={3} fill="#FFFFFF" opacity={0.6} />
      <circle
        cx={412}
        cy={551}
        r={4.2}
        fill="#FBBF24"
        stroke="#7C2D12"
        strokeWidth={1}
      />
      <circle cx={411} cy={549.5} r={1.4} fill="#FFFFFF" />

      {/* Lips: full, glossy berry */}
      <g transform="translate(0 -8)">
        {expr !== "smile" ? (
          <g>
            {expr === "awe" && (
              <path
                d="M414 607 Q446 616 478 605 Q446 626 414 607 Z"
                fill="#4A1220"
              />
            )}
            <path
              d="M408 604 Q423 592 436 597 Q446 590 458 596 Q471 596 482 603 Q446 610 408 604 Z"
              fill={url("lip")}
            />
            <path
              d={
                expr === "awe"
                  ? "M414 612 Q446 644 478 610 Q446 624 414 612 Z"
                  : "M412 606 Q446 638 480 604 Q446 614 412 606 Z"
              }
              fill="#C4455E"
            />
            <ellipse
              cx={448}
              cy={expr === "awe" ? 627 : 621}
              rx={11}
              ry={3}
              fill="#FFFFFF"
              opacity={0.6}
            />
            <path
              d="M408 604 Q446 610 482 603"
              stroke="#5A1A26"
              strokeWidth={2}
              fill="none"
              opacity={0.7}
            />
          </g>
        ) : (
          <g>
            <path
              d="M404 598 Q446 630 488 592 Q446 614 404 598 Z"
              fill="#4A1220"
            />
            <path
              d="M404 598 Q446 608 488 592"
              stroke="#8E2440"
              strokeWidth={4}
              fill="none"
              strokeLinecap="round"
            />
            <path
              d="M422 619 Q446 636 470 617"
              stroke="#C4455E"
              strokeWidth={6}
              fill="none"
              strokeLinecap="round"
            />
            <ellipse
              cx={446}
              cy={626}
              rx={9}
              ry={2.5}
              fill="#FFFFFF"
              opacity={0.55}
            />
          </g>
        )}
      </g>

      {/* Front hair: deep side part, sweeping bangs, face-framing locks */}
      <path
        d="M372 430 C346 300 430 214 534 220 C640 226 700 300 690 400 C684 470 664 530 650 600 C640 540 630 480 610 440 C596 400 572 360 536 346 C552 374 548 400 530 418 C508 372 468 346 428 350 C446 372 444 398 426 414 C414 394 392 390 372 430 Z"
        fill={url("hair")}
        stroke="#0F0B1E"
        strokeWidth={2.2}
        strokeLinejoin="round"
      />
      <path
        d="M382 420 C366 474 376 532 370 584 C366 624 372 656 392 684 C398 690 404 686 400 676 C388 650 390 618 392 584 C394 530 388 474 394 432 Z"
        fill={url("hair")}
        stroke="#0F0B1E"
        strokeWidth={1.8}
      />
      <path
        d="M650 470 C676 540 652 610 676 680 C690 720 670 760 682 800"
        stroke="#221A40"
        strokeWidth={7}
        fill="none"
        strokeLinecap="round"
      />
      <g fill="none" strokeLinecap="round">
        {[
          "M430 262 C412 310 404 350 410 400",
          "M486 240 C474 290 482 330 500 380",
          "M548 236 C560 280 574 310 590 380",
          "M610 254 C634 290 650 330 660 400",
        ].map((d) => (
          <path
            key={d}
            d={d}
            stroke="#0A0716"
            strokeWidth={2.4}
            opacity={0.75}
          />
        ))}
        {/* Glossy highlights */}
        {[
          "M440 262 C480 246 520 242 560 246",
          "M576 250 C610 258 640 276 664 302",
          "M404 302 C414 288 426 280 440 274",
        ].map((d) => (
          <path key={d} d={d} stroke="#9C95EA" strokeWidth={8} opacity={0.6} />
        ))}
      </g>
      <path
        d="M410 300 C450 250 520 232 590 240 C630 246 660 262 684 286 C650 270 610 262 572 262 C520 262 466 276 420 312 Z"
        fill="#9C95EA"
        opacity={0.35}
        filter={url("softer")}
      />
      <path
        d="M690 400 C700 300 640 232 560 222"
        stroke={rim}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
        opacity={0.9}
      />
    </g>
  );
};
