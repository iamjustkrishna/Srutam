import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeIn, easeOut } from "../anim";
import { Ash } from "../components/Overlays";
import { Spark } from "../components/Spark";
import { C, FONT } from "../theme";

const PANEL_H = 440;
const PANELS = [
  { label: "In the shower", at: 0 },
  { label: "Driving", at: 16 },
  { label: "Mid-meeting", at: 32 },
] as const;

const Shower: React.FC<{ frame: number }> = ({ frame }) => (
  <>
    {Array.from({ length: 48 }, (_, i) => (
      <div
        key={i}
        style={{
          position: "absolute",
          left: (i % 8) * 140,
          top: Math.floor(i / 8) * 90,
          width: 132,
          height: 82,
          background: C.slate300,
          borderRadius: 6,
        }}
      />
    ))}
    {Array.from({ length: 14 }, (_, i) => (
      <div
        key={i}
        style={{
          position: "absolute",
          left: 120 + i * 62,
          top: ((frame * 22 + i * 97) % 520) - 60,
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
        left: 0,
        right: 0,
        top: 0,
        bottom: 0,
        background: `linear-gradient(180deg, ${C.slate300}, ${C.slate400})`,
      }}
    />
    {Array.from({ length: 6 }, (_, i) => (
      <div
        key={i}
        style={{
          position: "absolute",
          left: 525,
          top: ((frame * 18 + i * 110) % 660) - 120,
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
        left: 290,
        top: 300,
        width: 500,
        height: 500,
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
        top: 330,
        height: 200,
        background: C.slate500,
      }}
    />
    {[140, 460, 780].map((x) => (
      <div
        key={x}
        style={{
          position: "absolute",
          left: x,
          top: 210,
          width: 170,
          height: 120,
          borderRadius: 10,
          background: C.slate700,
        }}
      />
    ))}
    <div
      style={{
        position: "absolute",
        left: 600,
        top: 60,
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
          top: 90,
          left: 80,
          right: 80,
          textAlign: "center",
          fontFamily: FONT.sans,
        }}
      >
        <div style={{ fontSize: 44, fontWeight: 600, color: C.slate500 }}>
          Ideas lost this week
        </div>
        <div
          style={{
            fontSize: 120,
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
              left: 40,
              right: 40,
              top: 360 + i * (PANEL_H + 30),
              height: PANEL_H,
              borderRadius: 40,
              overflow: "hidden",
              background: C.slate100,
              opacity: interpolate(t, [0, 6], [0, 1], clamp),
              translate: `${interpolate(t, [0, 10], [i % 2 ? 300 : -300, 0], { ...clamp, easing: easeOut })}px 0`,
            }}
          >
            {i === 0 && <Shower frame={frame} />}
            {i === 1 && <Driving frame={frame} />}
            {i === 2 && <Meeting />}
            <div
              style={{
                position: "absolute",
                left: 32,
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
                size={130}
                life={life}
                seed={i}
                lookY={interpolate(t, [10, 28], [0, 1], clamp)}
                stretch={interpolate(t, [20, 28], [1, 0.8], clamp)}
                style={{
                  position: "absolute",
                  left: 475,
                  top: 150 + fall,
                  opacity: interpolate(t, [26, 30], [1, 0], clamp),
                }}
              />
            )}
            <Ash x={500} y={215} at={p.at + 28} seed={i} />
          </div>
        );
      })}
    </AbsoluteFill>
  );
};
