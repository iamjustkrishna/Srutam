// Painted-style figures and the phone (with real app UI inside).
import { useId } from "react";
import { LOGO } from "../spirit/logoData";

const INK = "#4A2E35";

/** Meera from behind: bun, pencil, denim jacket, warm rim light. 600 x 700 box. */
export const MeeraBack: React.FC<{
  x: number;
  y: number;
  s: number;
  rim?: string;
}> = ({ x, y, s, rim = "#FFB27A" }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    <g transform="rotate(-24 330 120)">
      <rect
        x={250}
        y={112}
        width={170}
        height={15}
        rx={3}
        fill="#FACC15"
        stroke={INK}
        strokeWidth={3}
      />
      <rect
        x={410}
        y={112}
        width={20}
        height={15}
        rx={4}
        fill="#F472B6"
        stroke={INK}
        strokeWidth={3}
      />
    </g>
    <circle
      cx={300}
      cy={128}
      r={58}
      fill="#2A2050"
      stroke={INK}
      strokeWidth={3}
    />
    <path
      d="M256 112 Q300 76 344 112"
      stroke="#5C5AA8"
      strokeWidth={5}
      fill="none"
    />
    <path
      d="M300 470 L360 460 L372 560 L232 560 L242 460 Z"
      fill="#E7B48F"
      stroke={INK}
      strokeWidth={3}
    />
    <ellipse
      cx={300}
      cy={300}
      rx={140}
      ry={160}
      fill="#2A2050"
      stroke={INK}
      strokeWidth={3}
    />
    <path
      d="M190 330 C200 420 250 470 300 476 C350 470 400 420 410 330 C380 410 220 410 190 330 Z"
      fill="#1E1740"
    />
    <path
      d="M168 300 C170 200 230 150 300 142"
      stroke="#6E68C0"
      strokeWidth={6}
      fill="none"
      opacity={0.7}
      strokeLinecap="round"
    />
    <path
      d="M432 300 C430 210 380 160 320 146"
      stroke={rim}
      strokeWidth={6}
      fill="none"
      strokeLinecap="round"
    />
    <path
      d="M60 700 C70 590 150 520 250 500 L350 500 C450 520 530 590 540 700 Z"
      fill="#2F4A75"
      stroke={INK}
      strokeWidth={4}
    />
    <path
      d="M250 500 L300 560 L350 500"
      fill="#3E5F93"
      stroke={INK}
      strokeWidth={3}
    />
    <path
      d="M350 500 C450 520 530 590 540 700 L500 700 C490 610 430 545 350 520 Z"
      fill={rim}
      opacity={0.35}
    />
  </g>
);

/** A hand holding the phone: palm below, thumb over the left edge, fingertips on the right. */
export const Hand: React.FC<{ x: number; y: number; w: number }> = ({
  x,
  y,
  w,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const k = w / 400;
  return (
    <g transform={`translate(${x} ${y}) scale(${k})`}>
      <defs>
        <linearGradient id={`hs${id}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#F2C4A0" />
          <stop offset="100%" stopColor="#C98F72" />
        </linearGradient>
      </defs>
      <path
        d="M-60 520 C-80 420 -20 330 60 320 L360 320 C430 330 470 420 460 520 L460 760 L-60 760 Z"
        fill={`url(#hs${id})`}
        stroke={INK}
        strokeWidth={4}
      />
      {[0, 1, 2].map((i) => (
        <path
          key={i}
          d={`M398 ${120 + i * 78} C440 ${116 + i * 78} 450 ${170 + i * 78} 410 ${180 + i * 78}`}
          fill="#E6AE8A"
          stroke={INK}
          strokeWidth={4}
        />
      ))}
      <path
        d="M-30 330 C-60 260 -40 190 10 180 C40 176 56 200 52 240 L40 330 Z"
        fill="#EDBA95"
        stroke={INK}
        strokeWidth={4}
      />
      <path
        d="M-4 196 C14 192 30 200 34 216"
        stroke="#FBE1CC"
        strokeWidth={8}
        fill="none"
        strokeLinecap="round"
      />
    </g>
  );
};

/**
 * A phone whose screen is real HTML (the app's own UI components) via
 * foreignObject. `w` is the screen width in panel units.
 */
export const PhoneUI: React.FC<{
  x: number;
  y: number;
  w: number;
  rot?: number;
  dark?: boolean;
  children?: React.ReactNode;
  dock?: boolean;
}> = ({ x, y, w, rot = 0, dark = false, children, dock = false }) => {
  const h = w * (2400 / 1080);
  const b = w * 0.045;
  return (
    <g transform={`translate(${x} ${y}) rotate(${rot})`}>
      <rect
        x={-b}
        y={-b}
        width={w + 2 * b}
        height={h + 2 * b}
        rx={w * 0.12}
        fill="#0B0F1A"
        stroke="#3A4154"
        strokeWidth={3}
      />
      <foreignObject x={0} y={0} width={w} height={h}>
        <div
          style={{
            width: w,
            height: h,
            borderRadius: w * 0.09,
            overflow: "hidden",
            background: dark ? "#050714" : "#F4F5F8",
            position: "relative",
            fontFamily: "Inter, sans-serif",
          }}
        >
          {children}
        </div>
      </foreignObject>
      {dock && (
        <g>
          <circle
            cx={0}
            cy={h * 0.36}
            r={w * 0.28}
            fill="#67E8F9"
            opacity={0.28}
          />
          <path
            d={`M0 ${h * 0.33} L${w * 0.1} ${h * 0.33} Q${w * 0.15} ${h * 0.33} ${w * 0.15} ${h * 0.345} L${w * 0.15} ${h * 0.375} Q${w * 0.15} ${h * 0.39} ${w * 0.1} ${h * 0.39} L0 ${h * 0.39} Z`}
            fill="#0F172A"
            stroke="#E0F2FE"
            strokeWidth={2}
          />
          <image
            href={LOGO}
            x={w * 0.015}
            y={h * 0.336}
            width={w * 0.115}
            height={w * 0.115}
          />
        </g>
      )}
    </g>
  );
};

/** Handwritten panel text, white with a soft shadow. */
export const Script: React.FC<{
  x: number;
  y: number;
  lines: string[];
  size?: number;
  rot?: number;
  anchor?: "start" | "middle" | "end";
  color?: string;
}> = ({
  x,
  y,
  lines,
  size = 56,
  rot = -3,
  anchor = "start",
  color = "#FFFFFF",
}) => (
  <text
    x={x}
    y={y}
    textAnchor={anchor}
    transform={`rotate(${rot} ${x} ${y})`}
    fontFamily="Kalam"
    fontSize={size}
    fill={color}
    style={{ filter: "drop-shadow(0 3px 6px rgba(5,3,15,0.75))" }}
  >
    {lines.map((l, i) => (
      <tspan key={i} x={x} dy={i ? size * 1.15 : 0}>
        {l}
      </tspan>
    ))}
  </text>
);
