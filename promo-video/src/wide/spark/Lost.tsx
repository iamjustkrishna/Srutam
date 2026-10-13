import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeIn, easeOut } from "../../anim";
import { Ash } from "../../components/Overlays";
import { Spark } from "../../components/Spark";
import { C, FONT } from "../../theme";

// Three panels side by side under the counter.
const W = 540;
const H = 590;
const GAP = 40;
const X0 = (1920 - (3 * W + 2 * GAP)) / 2;
const TOP = 280;
const SPARK = 140;
const SPARK_TOP = 200;

const PANELS = [
  { label: "In the shower", at: 0 },
  { label: "Driving", at: 16 },
  { label: "Mid-meeting", at: 32 },
] as const;

const Shower: React.FC<{ frame: number }> = ({ frame }) => (
  <>
    {Array.from({ length: 28 }, (_, i) => (
      <div
        key={i}
        style={{
          position: "absolute",
          left: (i % 4) * 140,
          top: Math.floor(i / 4) * 90,
          width: 132,
          height: 82,
          background: C.slate300,
          borderRadius: 6,
        }}
      />
    ))}
    {Array.from({ length: 8 }, (_, i) => (
      <div
        key={i}
        style={{
          position: "absolute",
          left: 50 + i * 62,
          top: ((frame * 22 + i * 97) % (H + 80)) - 60,
          width: 5,
          height: 60,
          borderRadius: 3,
          background: C.slate400,
        }}
      />
    ))}
  </>
);

const Driving: React.FC<{ frame: number }> = ({ frame }) => (
  <>
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: `linear-gradient(180deg, ${C.slate300}, ${C.slate400})`,
      }}
    />
    {Array.from({ length: 7 }, (_, i) => (
      <div
        key={i}
        style={{
          position: "absolute",
          left: W / 2 - 15,
          top: ((frame * 18 + i * 110) % 770) - 120,
          width: 30,
          height: 70,
          background: C.slate200,
          borderRadius: 6,
        }}
      />
    ))}
    <div
      style={{
        position: "absolute",
        left: W / 2 - 240,
        top: H - 190,
        width: 480,
        height: 480,
        borderRadius: 999,
        border: `46px solid ${C.slate700}`,
        boxSizing: "border-box",
      }}
    />
  </>
);

const Meeting: React.FC = () => (
  <>
    <div
      style={{
        position: "absolute",
        left: 0,
        right: 0,
        top: 440,
        height: 200,
        background: C.slate500,
      }}
    />
    {[28, 195, 362].map((x) => (
      <div
        key={x}
        style={{
          position: "absolute",
          left: x,
          top: 320,
          width: 150,
          height: 120,
          borderRadius: 10,
          background: C.slate700,
        }}
      />
    ))}
    <div
      style={{
        position: "absolute",
        right: 28,
        top: 112,
        padding: "16px 26px",
        borderRadius: 26,
        background: C.white,
        color: C.slate500,
        fontFamily: FONT.sans,
        fontSize: 34,
        fontWeight: 600,
      }}
    >
      ...and Q3 numbers
    </div>
  </>
);

/** 0:03 - Pattern interrupt: three quick deaths, a ticking loss counter. */
export const Lost: React.FC = () => {
  const frame = useCurrentFrame();
  const lost = 20 + PANELS.filter((p) => frame >= p.at + 30).length;

  return (
    <AbsoluteFill style={{ background: C.slate200, overflow: "hidden" }}>
      <div
        style={{
          position: "absolute",
          top: 56,
          left: 80,
          right: 80,
          textAlign: "center",
          fontFamily: FONT.sans,
        }}
      >
        <div style={{ fontSize: 46, fontWeight: 600, color: C.slate500 }}>
          Ideas lost this week
        </div>
        <div
          style={{
            fontSize: 130,
            fontWeight: 800,
            color: C.slate900,
            lineHeight: 1.05,
          }}
        >
          {lost}
        </div>
      </div>
      {PANELS.map((p, i) => {
        const t = frame - p.at;
        const life = interpolate(t, [8, 28], [1, 0], clamp);
        const fall = interpolate(t, [28, 40], [0, 120], {
          ...clamp,
          easing: easeIn,
        });
        return (
          <div
            key={p.label}
            style={{
              position: "absolute",
              left: X0 + i * (W + GAP),
              top: TOP,
              width: W,
              height: H,
              borderRadius: 40,
              overflow: "hidden",
              background: C.slate100,
              opacity: interpolate(t, [0, 6], [0, 1], clamp),
              translate: `0 ${interpolate(t, [0, 10], [200, 0], { ...clamp, easing: easeOut })}px`,
            }}
          >
            {i === 0 && <Shower frame={frame} />}
            {i === 1 && <Driving frame={frame} />}
            {i === 2 && <Meeting />}
            <div
              style={{
                position: "absolute",
                left: 28,
                top: 26,
                padding: "10px 24px",
                borderRadius: 999,
                background: C.slate900,
                color: C.white,
                fontFamily: FONT.sans,
                fontSize: 36,
                fontWeight: 700,
              }}
            >
              {p.label}
            </div>
            {t < 30 && (
              <Spark
                size={SPARK}
                life={life}
                seed={i}
                lookY={interpolate(t, [10, 28], [0, 1], clamp)}
                stretch={interpolate(t, [20, 28], [1, 0.8], clamp)}
                style={{
                  position: "absolute",
                  left: W / 2 - SPARK / 2,
                  top: SPARK_TOP + fall,
                  opacity: interpolate(t, [26, 30], [1, 0], clamp),
                }}
              />
            )}
            <Ash x={W / 2 - 5} y={SPARK_TOP + SPARK / 2} at={p.at + 28} seed={i} />
          </div>
        );
      })}
    </AbsoluteFill>
  );
};
