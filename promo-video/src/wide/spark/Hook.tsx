import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeOut } from "../../anim";
import { Host } from "../../components/Backdrops";
import { CountdownRing } from "../../components/Overlays";
import { Spark } from "../../components/Spark";
import { C, FONT } from "../../theme";
import { WideBusBackdrop } from "./Backdrops";

// The dying idea floats over the commuter's head on the right; the hook line
// takes the left.
const SPARK = { x: 1340, y: 330 };
const HOST = { x: 1340, y: 820, scale: 0.8 };

/** 0:00 - Open on the most loaded frame: a dying idea and a countdown. */
export const Hook: React.FC = () => {
  const frame = useCurrentFrame();
  const bob = Math.sin(frame / 9) * 10;
  const life = interpolate(frame, [0, 110], [1, 0.72], clamp);
  const seconds = frame < 35 ? "4" : frame < 70 ? "3" : "2";

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      <AbsoluteFill
        style={{ scale: `${interpolate(frame, [0, 110], [1, 1.08], clamp)}` }}
      >
        <WideBusBackdrop />
        <Host x={HOST.x} y={HOST.y} scale={HOST.scale} />
        <CountdownRing
          x={SPARK.x}
          y={SPARK.y + bob}
          r={150}
          progress={interpolate(frame, [0, 110], [1, 0.48], clamp)}
          label={seconds}
          life={life}
        />
        <Spark
          size={180}
          life={life}
          lookX={interpolate(frame, [0, 40, 60], [0, 0, -0.6], clamp)}
          lookY={0.3}
          stretch={1 + Math.sin(frame / 9) * 0.04}
          style={{
            position: "absolute",
            left: SPARK.x - 90,
            top: SPARK.y - 90 + bob,
          }}
        />
      </AbsoluteFill>

      {/* A light wash on the left keeps the hook line readable */}
      <div
        style={{
          position: "absolute",
          left: 0,
          top: 0,
          bottom: 0,
          width: 1240,
          background: `linear-gradient(90deg, ${C.slate200} 0%, ${C.slate200} 62%, transparent 100%)`,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: 130,
          top: 300,
          width: 860,
          fontFamily: FONT.sans,
          textAlign: "left",
        }}
      >
        <div
          style={{
            fontSize: 84,
            fontWeight: 700,
            color: C.slate900,
            opacity: interpolate(frame, [4, 14], [0, 1], clamp),
            translate: `0 ${interpolate(frame, [4, 16], [24, 0], { ...clamp, easing: easeOut })}px`,
          }}
        >
          This idea has
        </div>
        <div
          style={{
            marginTop: 14,
            fontSize: 150,
            fontWeight: 800,
            lineHeight: 1.08,
            color: C.slate900,
            transformOrigin: "left center",
            opacity: interpolate(frame, [44, 52], [0, 1], clamp),
            scale: `${interpolate(frame, [44, 56], [1.25, 1], { ...clamp, easing: easeOut })}`,
          }}
        >
          <span
            style={{
              color: C.white,
              background: C.slate900,
              padding: "0 22px",
              marginLeft: -22,
              borderRadius: 22,
            }}
          >
            <span style={{ color: C.gold }}>4 seconds</span>
          </span>
          <br />
          to live.
        </div>
      </div>
    </AbsoluteFill>
  );
};
