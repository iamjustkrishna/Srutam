// Landscape versions of the Spark backdrops: the same art, laid out for 1920 x 1080.
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { rand } from "../../anim";
import { C } from "../../theme";
import { LH, LW } from "../kit";

/** The grey commute, wide: a long window band with passing city and a pole. */
export const WideBusBackdrop: React.FC<{
  speed?: number;
  dim?: number;
  bandTop?: number;
  bandH?: number;
  poleX?: number;
}> = ({ speed = 1, dim = 0, bandTop = 130, bandH = 700, poleX = 1790 }) => {
  const frame = useCurrentFrame();
  const span = 2800;
  const buildings = Array.from({ length: 18 }, (_, i) => {
    const w = 120 + rand(i) * 220;
    const h = 180 + rand(i + 50) * 380;
    const layer = i % 2;
    const v = (layer ? 9 : 5) * speed;
    const x0 = i * 155 + rand(i + 7) * 60;
    const x = ((((x0 - frame * v) % span) + span) % span) - 440;
    return { w, h, x, layer };
  });

  return (
    <AbsoluteFill
      style={{
        background: `linear-gradient(180deg, ${C.slate300} 0%, ${C.slate400} 100%)`,
      }}
    >
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: bandTop,
          height: bandH,
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
        {Array.from({ length: Math.ceil((LW + 80) / 380) }, (_, i) => (
          <div
            key={i}
            style={{
              position: "absolute",
              left: i * 380 - 40,
              top: 0,
              width: 360,
              height: bandH,
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
          top: bandTop - 60,
          height: 18,
          borderRadius: 9,
          background: C.slate500,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: poleX,
          top: bandTop - 60,
          width: 22,
          height: LH,
          borderRadius: 11,
          background: C.slate500,
        }}
      />
      <AbsoluteFill style={{ background: C.slate900, opacity: dim }} />
      <AbsoluteFill
        style={{
          background:
            "radial-gradient(ellipse at 50% 45%, transparent 50%, rgba(15,23,42,0.35) 100%)",
        }}
      />
    </AbsoluteFill>
  );
};

/** Cosmic Void night sky, wide, with twinkling stardust. */
export const WideVoidSky: React.FC<{ reveal?: number }> = ({ reveal = 1 }) => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(ellipse at 50% 42%, #12183A 0%, ${C.voidBg} 70%)`,
        clipPath: `circle(${reveal * 150}% at 50% 45%)`,
      }}
    >
      {Array.from({ length: 96 }, (_, i) => {
        const tw = (Math.sin(frame / 8 + i * 2.1) + 1) / 2;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: rand(i) * LW,
              top: rand(i + 300) * LH,
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
          background: `radial-gradient(circle at 18% 28%, ${C.glowPurple}33 0%, transparent 32%), radial-gradient(circle at 86% 72%, ${C.nebulaPink}22 0%, transparent 30%)`,
        }}
      />
    </AbsoluteFill>
  );
};
