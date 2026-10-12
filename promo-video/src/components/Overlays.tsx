import { interpolate, useCurrentFrame } from "remotion";
import { clamp, easeOut } from "../anim";
import { C, FONT } from "../theme";
import { useWide } from "../landscape/frame";

/** Countdown ring around the spark: `progress` 1 -> 0, `label` in the middle. */
export const CountdownRing: React.FC<{
  x: number;
  y: number;
  r: number;
  progress: number;
  label: string;
  life?: number;
}> = ({ x, y, r, progress, label, life = 1 }) => {
  const circ = 2 * Math.PI * r;
  return (
    <svg
      width={r * 2 + 40}
      height={r * 2 + 40}
      style={{
        position: "absolute",
        left: x - r - 20,
        top: y - r - 20,
        overflow: "visible",
      }}
    >
      <circle
        cx={r + 20}
        cy={r + 20}
        r={r}
        stroke="rgba(255,255,255,0.25)"
        strokeWidth={10}
        fill="none"
      />
      <circle
        cx={r + 20}
        cy={r + 20}
        r={r}
        stroke={life > 0.5 ? C.gold : C.slate500}
        strokeWidth={10}
        fill="none"
        strokeLinecap="round"
        strokeDasharray={circ}
        strokeDashoffset={circ * (1 - progress)}
        transform={`rotate(-90 ${r + 20} ${r + 20})`}
      />
      <text
        x={r + 20 + r * 0.72}
        y={r + 20 - r * 0.72}
        textAnchor="middle"
        dominantBaseline="central"
        fontFamily="Inter"
        fontWeight={800}
        fontSize={r * 0.42}
        fill="#FFFFFF"
        stroke={C.slate900}
        strokeWidth={6}
        paintOrder="stroke"
      >
        {label}
      </text>
    </svg>
  );
};

/** Burned-in caption: short line, high contrast, inside the safe area. */
export const Caption: React.FC<{
  text: string;
  from: number;
  to: number;
  y?: number;
}> = ({ text, from, to, y = 1650 }) => {
  const frame = useCurrentFrame();
  const wide = useWide();
  if (wide || frame < from - 2 || frame > to + 8) return null;
  return (
    <div
      style={{
        position: "absolute",
        left: 80,
        right: 80,
        top: y,
        display: "flex",
        justifyContent: "center",
        opacity: interpolate(
          frame,
          [from - 2, from + 4, to, to + 8],
          [0, 1, 1, 0],
          clamp,
        ),
        translate: `0 ${interpolate(frame, [from - 2, from + 6], [16, 0], { ...clamp, easing: easeOut })}px`,
      }}
    >
      <div
        style={{
          padding: "18px 34px",
          borderRadius: 28,
          background: "rgba(15,23,42,0.82)",
          color: "#FFFFFF",
          fontFamily: FONT.sans,
          fontWeight: 700,
          fontSize: 48,
          textAlign: "center",
          lineHeight: 1.25,
        }}
      >
        {text}
      </div>
    </div>
  );
};

/** Falling ash for a spark that died at `at` (frames, local). */
export const Ash: React.FC<{
  x: number;
  y: number;
  at: number;
  seed?: number;
}> = ({ x, y, at, seed = 0 }) => {
  const frame = useCurrentFrame();
  const t = frame - at;
  if (t < 0 || t > 45) return null;
  return (
    <>
      {Array.from({ length: 18 }, (_, i) => {
        const a = (i / 18) * Math.PI * 2 + seed;
        const vx = Math.cos(a) * (2 + (i % 4));
        const px = x + vx * t;
        const py = y + Math.sin(a) * 2 * t + 0.25 * t * t;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: px,
              top: py,
              width: 10 - (i % 3) * 2,
              height: 10 - (i % 3) * 2,
              borderRadius: 3,
              background: C.slate500,
              opacity: 1 - t / 45,
            }}
          />
        );
      })}
    </>
  );
};
