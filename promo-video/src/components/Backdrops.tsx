import { AbsoluteFill, interpolateColors, useCurrentFrame } from "remotion";
import { rand } from "../anim";
import { C } from "../theme";

/** The grey commute the idea is born into. Flat, muted, low contrast. */
export const BusBackdrop: React.FC<{ speed?: number; dim?: number }> = ({
  speed = 1,
  dim = 0,
}) => {
  const frame = useCurrentFrame();
  const buildings = Array.from({ length: 14 }, (_, i) => {
    const w = 120 + rand(i) * 220;
    const h = 180 + rand(i + 50) * 380;
    const layer = i % 2;
    const v = (layer ? 9 : 5) * speed;
    const span = 2400;
    const x = ((((i * 190 - frame * v) % span) + span) % span) - 400;
    return { w, h, x, layer };
  });

  return (
    <AbsoluteFill
      style={{
        background: `linear-gradient(180deg, ${C.slate300} 0%, ${C.slate400} 100%)`,
      }}
    >
      {/* Window band with passing city */}
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 260,
          height: 760,
          overflow: "hidden",
          background: `linear-gradient(180deg, #DCE3EC 0%, ${C.slate200} 100%)`,
        }}
      >
        {buildings.map((b, i) => (
          <div
            key={i}
            style={{
              position: "absolute",
              left: b.x,
              bottom: 0,
              width: b.w,
              height: b.h,
              borderRadius: 18,
              background: b.layer ? C.slate400 : C.slate300,
              opacity: b.layer ? 0.75 : 0.9,
              filter: `blur(${b.layer ? 6 : 2}px)`,
            }}
          />
        ))}
        {/* Window frames */}
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            style={{
              position: "absolute",
              left: i * 380 - 40,
              top: 0,
              width: 360,
              height: 760,
              border: `22px solid ${C.slate500}`,
              borderRadius: 48,
              boxSizing: "border-box",
            }}
          />
        ))}
      </div>
      {/* Handrail and pole */}
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 200,
          height: 18,
          borderRadius: 9,
          background: C.slate500,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: 140,
          top: 200,
          width: 22,
          height: 1800,
          borderRadius: 11,
          background: C.slate500,
        }}
      />
      <AbsoluteFill style={{ background: C.slate900, opacity: dim }} />
      {/* Vignette */}
      <AbsoluteFill
        style={{
          background:
            "radial-gradient(ellipse at 50% 45%, transparent 50%, rgba(15,23,42,0.35) 100%)",
        }}
      />
    </AbsoluteFill>
  );
};

/** Back of the Host's head and shoulders, faceless so any viewer fits. */
export const Host: React.FC<{ x?: number; y?: number; scale?: number }> = ({
  x = 540,
  y = 1420,
  scale = 1,
}) => {
  const frame = useCurrentFrame();
  const sway = Math.sin(frame / 18) * 6;
  return (
    <svg
      viewBox="-540 -500 1080 1000"
      width={1080 * scale}
      height={1000 * scale}
      style={{
        position: "absolute",
        left: x - 540 * scale,
        top: y - 500 * scale,
        overflow: "visible",
      }}
    >
      <g transform={`translate(${sway} 0)`}>
        <path
          d="M -470 520 C -450 220 -260 140 -120 120 L 120 120 C 260 140 450 220 470 520 Z"
          fill={C.slate700}
        />
        <rect
          x={-70}
          y={40}
          width={140}
          height={120}
          rx={40}
          fill={C.slate700}
        />
        <ellipse cx={0} cy={-90} rx={175} ry={195} fill={C.slate700} />
        {/* Hair rim light */}
        <path
          d="M -150 -170 C -90 -280 90 -280 150 -170"
          stroke={C.slate500}
          strokeWidth={14}
          fill="none"
          strokeLinecap="round"
        />
      </g>
    </svg>
  );
};

/** Cosmic Void night sky with twinkling stardust. */
export const VoidSky: React.FC<{ reveal?: number }> = ({ reveal = 1 }) => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(ellipse at 50% 40%, #12183A 0%, ${C.voidBg} 70%)`,
        clipPath: `circle(${reveal * 150}% at 50% 45%)`,
      }}
    >
      {Array.from({ length: 70 }, (_, i) => {
        const tw = (Math.sin(frame / 8 + i * 2.1) + 1) / 2;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: rand(i) * 1080,
              top: rand(i + 300) * 1920,
              width: 3 + rand(i + 99) * 5,
              height: 3 + rand(i + 99) * 5,
              borderRadius: 99,
              background: i % 5 === 0 ? C.gold : "#FFFFFF",
              opacity: 0.25 + 0.6 * tw,
            }}
          />
        );
      })}
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at 20% 30%, ${C.glowPurple}33 0%, transparent 40%), radial-gradient(circle at 85% 70%, ${C.nebulaPink}22 0%, transparent 35%)`,
        }}
      />
    </AbsoluteFill>
  );
};

export const mixBg = (t: number) =>
  interpolateColors(t, [0, 1], [C.slate300, "#F4F5F8"]);
