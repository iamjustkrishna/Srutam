import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeOut } from "../anim";
import { BusBackdrop, Host } from "../components/Backdrops";
import { CountdownRing } from "../components/Overlays";
import { Spark } from "../components/Spark";
import { C, FONT } from "../theme";

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
        <BusBackdrop />
        <Host y={1500} />
        <CountdownRing
          x={540}
          y={900 + bob}
          r={170}
          progress={interpolate(frame, [0, 110], [1, 0.48], clamp)}
          label={seconds}
          life={life}
        />
        <Spark
          size={190}
          life={life}
          lookX={interpolate(frame, [0, 40, 60], [0, 0, 0.6], clamp)}
          lookY={0.3}
          stretch={1 + Math.sin(frame / 9) * 0.04}
          style={{ position: "absolute", left: 540 - 95, top: 900 - 95 + bob }}
        />
      </AbsoluteFill>

      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 0,
          height: 760,
          background: `linear-gradient(180deg, ${C.slate200} 55%, transparent 100%)`,
        }}
      />
      {/* On-screen hook: layered with the VO and the visual */}
      <div
        style={{
          position: "absolute",
          left: 80,
          right: 80,
          top: 230,
          fontFamily: FONT.sans,
          textAlign: "center",
        }}
      >
        <div
          style={{
            fontSize: 72,
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
            fontSize: 112,
            fontWeight: 800,
            lineHeight: 1.05,
            color: C.slate900,
            opacity: interpolate(frame, [44, 52], [0, 1], clamp),
            scale: `${interpolate(frame, [44, 56], [1.25, 1], { ...clamp, easing: easeOut })}`,
          }}
        >
          <span
            style={{
              color: C.white,
              background: C.slate900,
              padding: "0 18px",
              borderRadius: 18,
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
