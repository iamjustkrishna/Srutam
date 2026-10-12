import { useId } from "react";
import { Sparkle, SweatDrop } from "./FX";

export type Expression =
  | "worried"
  | "crying"
  | "happy"
  | "shocked"
  | "determined"
  | "love"
  | "sleepy"
  | "neutral";

type Props = {
  x: number;
  y: number;
  /** Body radius in px. */
  r: number;
  expression?: Expression;
  /** 0 = fully desaturated (dying), 1 = full color. */
  life?: number;
  color?: string;
  shade?: string;
  glow?: boolean;
  lookX?: number;
};

const LINE = "#5B2A0E";

/**
 * Spark, redrawn in an anime mascot style: hard cel shadow, brown line art,
 * big glossy eyes, blush hatching and an "ahoge" flame curl.
 */
export const AnimeSpark: React.FC<Props> = ({
  x,
  y,
  r,
  expression = "neutral",
  life = 1,
  color = "#FBBF24",
  shade = "#F59E0B",
  glow = true,
  lookX = 0,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const s = r / 100;

  const eye = (ex: number) => {
    const cx = ex + lookX * 6;
    switch (expression) {
      case "happy":
        return (
          <path
            d={`M${cx - 18} 2 Q${cx} -22 ${cx + 18} 2`}
            stroke={LINE}
            strokeWidth={8}
            fill="none"
            strokeLinecap="round"
          />
        );
      case "crying":
        return (
          <path
            d={`M${cx - 18} -10 Q${cx} 8 ${cx + 18} -10`}
            stroke={LINE}
            strokeWidth={8}
            fill="none"
            strokeLinecap="round"
          />
        );
      case "sleepy":
        return (
          <g>
            <path
              d={`M${cx - 18} -2 Q${cx} 8 ${cx + 18} -2`}
              stroke={LINE}
              strokeWidth={7}
              fill="none"
              strokeLinecap="round"
            />
            <path
              d={`M${cx - 14} 2 Q${cx} 12 ${cx + 14} 2 L${cx + 14} 6 Q${cx} 16 ${cx - 14} 6 Z`}
              fill="#3B1E08"
              opacity={0.8}
            />
          </g>
        );
      case "love":
        return (
          <path
            transform={`translate(${cx} -6) scale(1.1)`}
            d="M0 14 C -26 -4 -16 -24 0 -10 C 16 -24 26 -4 0 14 Z"
            fill="#F43F5E"
            stroke={LINE}
            strokeWidth={3}
          />
        );
      case "shocked":
        return (
          <g>
            <ellipse
              cx={cx}
              cy={-6}
              rx={20}
              ry={24}
              fill="#FFFFFF"
              stroke={LINE}
              strokeWidth={4}
            />
            <circle cx={cx} cy={-4} r={4.5} fill={LINE} />
          </g>
        );
      default: {
        const iris = `iris${id}`;
        return (
          <g>
            <ellipse
              cx={cx}
              cy={-6}
              rx={21}
              ry={28}
              fill="#FFFFFF"
              stroke={LINE}
              strokeWidth={3}
            />
            <ellipse
              cx={cx + 1}
              cy={-2}
              rx={16}
              ry={23}
              fill={`url(#${iris})`}
            />
            <ellipse cx={cx + 1} cy={-2} rx={7} ry={11} fill="#1C0A02" />
            <circle cx={cx - 6} cy={-14} r={7} fill="#FFFFFF" />
            <circle cx={cx + 7} cy={8} r={3.5} fill="#FFFFFF" />
            {expression === "determined" && (
              <path
                d={`M${cx - 4} -2 C ${cx - 9} -10 ${cx} -14 ${cx + 2} -22 C ${cx + 8} -12 ${cx + 6} -6 ${cx + 4} -2 Z`}
                fill="#F97316"
                opacity={0.9}
              />
            )}
            {/* Upper lash with an outer flick */}
            <path
              d={`M${cx - 24} -22 Q${cx} -42 ${cx + 24} -24 L${cx + 30} -30 L${cx + 22} -18 Q${cx} -34 ${cx - 22} -16 Z`}
              fill={LINE}
              transform={
                ex > 0 ? `translate(${2 * cx} 0) scale(-1 1)` : undefined
              }
            />
          </g>
        );
      }
    }
  };

  const mouth = () => {
    switch (expression) {
      case "happy":
        return (
          <path
            d="M-16 34 Q0 58 16 34 Z"
            fill="#9F1239"
            stroke={LINE}
            strokeWidth={3}
          />
        );
      case "crying":
        return (
          <path
            d="M-18 46 Q0 18 18 46 Q0 40 -18 46 Z"
            fill="#9F1239"
            stroke={LINE}
            strokeWidth={3}
          />
        );
      case "shocked":
        return (
          <ellipse
            cx={0}
            cy={42}
            rx={11}
            ry={15}
            fill="#9F1239"
            stroke={LINE}
            strokeWidth={3}
          />
        );
      case "worried":
        return (
          <path
            d="M-12 42 Q-6 36 0 42 Q6 48 12 42"
            stroke={LINE}
            strokeWidth={4}
            fill="none"
            strokeLinecap="round"
          />
        );
      case "determined":
        return (
          <path
            d="M-14 38 L14 38 L10 46 L-10 46 Z"
            fill="#FFFFFF"
            stroke={LINE}
            strokeWidth={3}
          />
        );
      case "sleepy":
        return <ellipse cx={0} cy={42} rx={5} ry={6} fill={LINE} />;
      default:
        return (
          <path
            d="M-10 38 Q0 46 10 38"
            stroke={LINE}
            strokeWidth={4}
            fill="none"
            strokeLinecap="round"
          />
        );
    }
  };

  const brows =
    expression === "worried" || expression === "crying" ? (
      <g stroke={LINE} strokeWidth={5} strokeLinecap="round">
        <path d="M-52 -46 L-22 -54" />
        <path d="M52 -46 L22 -54" />
      </g>
    ) : expression === "determined" ? (
      <g stroke={LINE} strokeWidth={6} strokeLinecap="round">
        <path d="M-54 -58 L-20 -44" />
        <path d="M54 -58 L20 -44" />
      </g>
    ) : null;

  return (
    <g
      transform={`translate(${x} ${y}) scale(${s})`}
      style={{ filter: life < 1 ? `grayscale(${1 - life})` : undefined }}
    >
      <defs>
        <radialGradient id={`glow${id}`}>
          <stop offset="0%" stopColor={color} stopOpacity={0.6} />
          <stop offset="100%" stopColor={color} stopOpacity={0} />
        </radialGradient>
        <linearGradient id={`iris${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#3B1E08" />
          <stop offset="60%" stopColor="#B45309" />
          <stop offset="100%" stopColor="#FCD34D" />
        </linearGradient>
        <mask id={`cel${id}`}>
          <circle r={100} fill="#FFFFFF" />
          <circle cx={-22} cy={-24} r={104} fill="#000000" />
        </mask>
      </defs>
      {glow && <circle r={190} fill={`url(#glow${id})`} />}
      {/* Ahoge: the flame curl on top */}
      <path
        d="M-4 -96 C -26 -136 8 -170 34 -158 C 12 -146 22 -122 10 -96 Z"
        fill={shade}
        stroke={LINE}
        strokeWidth={5}
        strokeLinejoin="round"
      />
      <circle r={100} fill={color} stroke={LINE} strokeWidth={6} />
      <circle r={100} fill={shade} mask={`url(#cel${id})`} />
      <path
        d="M-62 -58 Q-40 -82 -8 -86"
        stroke="#FFFFFF"
        strokeWidth={10}
        strokeLinecap="round"
        fill="none"
        opacity={0.85}
      />
      {brows}
      {eye(-38)}
      {eye(38)}
      {/* Blush with hatching */}
      {expression !== "shocked" && (
        <g opacity={0.75}>
          {[-62, 62].map((bx) => (
            <g key={bx}>
              <ellipse
                cx={bx}
                cy={24}
                rx={17}
                ry={8}
                fill="#FB7185"
                opacity={0.55}
              />
              {[-8, 0, 8].map((hx) => (
                <path
                  key={hx}
                  d={`M${bx + hx - 3} 30 L${bx + hx + 3} 18`}
                  stroke="#E11D48"
                  strokeWidth={2.5}
                  strokeLinecap="round"
                />
              ))}
            </g>
          ))}
        </g>
      )}
      {mouth()}
      {expression === "crying" && (
        <g>
          {[-38, 38].map((tx) => (
            <path
              key={tx}
              d={`M${tx} 0 C ${tx - 6} 40 ${tx + 10} 80 ${tx - 4} 140`}
              stroke="#38BDF8"
              strokeWidth={14}
              fill="none"
              strokeLinecap="round"
              opacity={0.9}
            />
          ))}
        </g>
      )}
      {expression === "worried" && <SweatDrop x={92} y={-60} s={1.1} />}
      {expression === "shocked" && (
        <g stroke={LINE} strokeWidth={6} strokeLinecap="round">
          <path d="M-70 -120 L-84 -150" />
          <path d="M0 -175 L0 -205" />
          <path d="M70 -120 L84 -150" />
        </g>
      )}
      {expression === "love" && (
        <>
          <Sparkle x={-120} y={-80} s={18} color="#FDA4AF" />
          <Sparkle x={124} y={-40} s={14} color="#FDA4AF" />
        </>
      )}
      {expression === "sleepy" && (
        <text
          x={96}
          y={-90}
          fontFamily="Inter"
          fontWeight={800}
          fontSize={40}
          fill="#64748B"
        >
          z z
        </text>
      )}
    </g>
  );
};
