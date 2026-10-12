import { useId } from "react";
import { interpolate, interpolateColors, useCurrentFrame } from "remotion";
import { C } from "../theme";

type Props = {
  /** Diameter of the body in px. */
  size: number;
  /** 1 = alive (warm, glowing). 0 = dead (grey, no glow). */
  life?: number;
  /** Body color when fully alive. Defaults to the gold spark. */
  color?: string;
  /** >0.15 shows the waveform mouth (while it is being recorded). */
  talk?: number;
  /** Eye direction, -1..1 on each axis. */
  lookX?: number;
  lookY?: number;
  /** Squash and stretch: >1 stretches vertically. */
  stretch?: number;
  /** Frame offset so several sparks don't blink in sync. */
  seed?: number;
  /** Hide the face (for tiny background sparks). */
  faceless?: boolean;
  style?: React.CSSProperties;
};

const lighten = (hex: string) =>
  interpolateColors(0.55, [0, 1], [hex, "#FFFFFF"]);

/**
 * The hero: a thought. A firefly whose body carries the app's waveform mark.
 * No mouth - it emotes through glow, squash and stretch, and the waveform.
 */
export const Spark: React.FC<Props> = ({
  size,
  life = 1,
  color = C.gold,
  talk = 0,
  lookX = 0,
  lookY = 0,
  stretch = 1,
  seed = 0,
  faceless = false,
  style,
}) => {
  const frame = useCurrentFrame() + seed * 37;
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const body = interpolateColors(life, [0, 1], [C.slate400, color]);
  const core = interpolateColors(life, [0, 1], [C.slate300, lighten(color)]);
  const edge = interpolateColors(
    life,
    [0, 1],
    [C.slate500, color === C.gold ? C.goldDeep : color],
  );

  // Blink roughly every 2.6 s for 4 frames
  const cycle = frame % 78;
  const blink = cycle >= 74 ? 0.15 : 1;

  const bars = [0, 1, 2, 3, 4].map((i) => {
    const base = [0.35, 0.7, 1, 0.7, 0.35][i];
    const wobble = (Math.sin(frame * 0.55 + i * 1.3) + 1) / 2;
    return 6 + base * 26 * wobble;
  });
  // Mouth curve: smiles when alive, frowns as it dies
  const curve = interpolate(life, [0, 0.55, 1], [-14, 0, 16], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const glowR = 100 * (1.5 + 0.6 * life);

  return (
    <div
      style={{
        width: size,
        height: size,
        position: "relative",
        scale: `${1 / Math.sqrt(stretch)} ${stretch}`,
        ...style,
      }}
    >
      <svg
        viewBox="-250 -250 500 500"
        width={size * 2.5}
        height={size * 2.5}
        style={{
          position: "absolute",
          left: -size * 0.75,
          top: -size * 0.75,
          overflow: "visible",
        }}
      >
        <defs>
          <radialGradient id={`glow${id}`}>
            <stop offset="0%" stopColor={color} stopOpacity={0.55 * life} />
            <stop offset="45%" stopColor={color} stopOpacity={0.18 * life} />
            <stop offset="100%" stopColor={color} stopOpacity={0} />
          </radialGradient>
          <radialGradient id={`body${id}`} cx="38%" cy="32%" r="75%">
            <stop offset="0%" stopColor={core} />
            <stop offset="55%" stopColor={body} />
            <stop offset="100%" stopColor={edge} />
          </radialGradient>
        </defs>
        <circle r={glowR} fill={`url(#glow${id})`} />
        <circle r={100} fill={`url(#body${id})`} />
        {/* Rim light */}
        <circle
          r={96}
          fill="none"
          stroke="#FFFFFF"
          strokeOpacity={0.35 * life}
          strokeWidth={4}
        />
        {!faceless && (
          <g>
            {[-34, 34].map((x) => (
              <g
                key={x}
                transform={`translate(${x + lookX * 10} ${-14 + lookY * 8}) scale(1 ${blink})`}
              >
                <ellipse rx={13} ry={18} fill={C.slate900} />
                <circle cx={4} cy={-6} r={5} fill="#FFFFFF" />
              </g>
            ))}
            {/* Mouth: the app's waveform mark while it is being recorded, else a mood curve */}
            <g transform={`translate(${lookX * 6} ${34 + lookY * 4})`}>
              {talk > 0.15 ? (
                bars.map((h, i) => (
                  <rect
                    key={i}
                    x={(i - 2) * 14 - 3.5}
                    y={-h / 2}
                    width={7}
                    height={h}
                    rx={3.5}
                    fill={C.slate900}
                    fillOpacity={0.7}
                  />
                ))
              ) : (
                <path
                  d={`M -22 ${-curve / 2} Q 0 ${curve} 22 ${-curve / 2}`}
                  stroke={C.slate900}
                  strokeOpacity={0.8}
                  strokeWidth={7}
                  strokeLinecap="round"
                  fill="none"
                />
              )}
            </g>
          </g>
        )}
      </svg>
    </div>
  );
};
