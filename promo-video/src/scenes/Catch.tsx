import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut, rand, springy } from "../anim";
import {
  FloatingDock,
  OnDeviceBadge,
  SavedNotification,
  type DockState,
} from "../components/AppUI";
import { BusBackdrop } from "../components/Backdrops";
import { CountdownRing } from "../components/Overlays";
import { Phone, Ripple, Thumb } from "../components/Phone";
import { Spark } from "../components/Spark";
import { C, FONT } from "../theme";
import { CATCH } from "../timeline";

// Phone geometry (px). Screen is inset by the 3% bezel.
const PHONE = { x: 170, y: 470, w: 740 };
const BEZEL = PHONE.w * 0.03;
const SCREEN = {
  x: PHONE.x + BEZEL,
  y: PHONE.y + BEZEL,
  w: PHONE.w - 2 * BEZEL,
};
const DP = SCREEN.w / 411;
const DOCK_TOP = 620; // inside the screen
const MIC = { x: SCREEN.x + 27 * DP, y: SCREEN.y + DOCK_TOP + 25 * DP };
const STOP = { x: SCREEN.x + 127 * DP, y: MIC.y };
const SPARK_HOME = { x: 640, y: 700 };
const REST = { x: 900, y: 1640 };

const TRANSCRIPT = [
  "What if the launch",
  "moves to Friday,",
  "and beta users",
  "invite one friend each?",
  "I'll draft the email",
  "by Thursday.",
];

/** A generic, unbranded social feed - "any app". */
const AnyApp: React.FC<{ scroll: number }> = ({ scroll }) => (
  <div style={{ position: "absolute", inset: 0, background: "#FFFFFF" }}>
    <div style={{ position: "absolute", top: 70 - scroll, left: 0, right: 0 }}>
      {Array.from({ length: 6 }, (_, i) => (
        <div key={i} style={{ padding: "26px 30px" }}>
          <div style={{ display: "flex", gap: 16, alignItems: "center" }}>
            <div
              style={{
                width: 54,
                height: 54,
                borderRadius: 99,
                background: C.slate300,
              }}
            />
            <div
              style={{
                width: 160 + (i % 3) * 40,
                height: 18,
                borderRadius: 9,
                background: C.slate200,
              }}
            />
          </div>
          <div
            style={{
              marginTop: 18,
              height: 360,
              borderRadius: 26,
              background: [C.slate200, C.slate300, "#E5E7EB"][i % 3],
            }}
          />
        </div>
      ))}
    </div>
  </div>
);

/** 0:06.7 - The catch: dock, tap, tap, the spark dives in, words, Saved. */
export const Catch: React.FC = () => {
  const frame = useCurrentFrame();
  const { dockGlow, tap1, tap2, dive, words, stop, saved } = CATCH;
  const diveEnd = dive + 30;

  // Dock state
  const dockState: DockState =
    frame < tap1
      ? "collapsed"
      : frame < tap2
        ? "idle"
        : frame < stop
          ? "recording"
          : "collapsed";
  const dockOpen =
    frame < tap2
      ? interpolate(frame, [tap1, tap1 + 5], [0.3, 1], {
          ...clamp,
          easing: easeOut,
        })
      : 1;
  const recSeconds = Math.max(0, (frame - tap2) / 30);

  // Thumb path: rest -> dock -> rest -> stop -> rest
  const thumbX = interpolate(
    frame,
    [32, tap1, tap2 + 6, 120, stop - 14, stop, stop + 14],
    [REST.x, MIC.x, MIC.x, REST.x, REST.x, STOP.x, REST.x],
    { ...clamp, easing: easeInOut },
  );
  const thumbY = interpolate(
    frame,
    [32, tap1, tap2 + 6, 120, stop - 14, stop, stop + 14],
    [REST.y, MIC.y, MIC.y, REST.y, REST.y, STOP.y, REST.y],
    { ...clamp, easing: easeInOut },
  );
  const press = [tap1, tap2, stop].some((t) => frame >= t && frame < t + 4)
    ? 1
    : 0;

  // Spark: flickering near death, then dives into the mic and re-ignites
  const flicker = 0.35 + 0.3 * rand(Math.floor(frame / 2));
  const d = interpolate(frame, [dive, diveEnd], [0, 1], {
    ...clamp,
    easing: easeInOut,
  });
  const ctrl = { x: 260, y: 260 };
  const sx =
    (1 - d) ** 2 * SPARK_HOME.x + 2 * (1 - d) * d * ctrl.x + d ** 2 * MIC.x;
  const sy =
    (1 - d) ** 2 * SPARK_HOME.y + 2 * (1 - d) * d * ctrl.y + d ** 2 * MIC.y;
  const sparkSize = interpolate(d, [0, 1], [150, 40]);
  const sparkLife =
    frame < dive
      ? flicker
      : interpolate(frame, [dive, dive + 10], [flicker, 1], clamp);
  const flash = interpolate(
    frame,
    [diveEnd - 2, diveEnd + 4, diveEnd + 22],
    [0, 1, 0],
    clamp,
  );

  // Transcript: words ride out of the dock while recording
  const shown = Math.floor(
    interpolate(frame, [words, words + 90], [0, TRANSCRIPT.length], clamp),
  );
  const panelIn = interpolate(
    frame,
    [words - 6, words + 6, stop + 30, stop + 42],
    [0, 1, 1, 0],
    clamp,
  );

  // Notification + exit wipe
  const notifY = interpolate(frame, [stop + 6, stop + 18], [-260, 40], {
    ...clamp,
    easing: springy,
  });
  const wipe = interpolate(frame, [248, 270], [0, 1], {
    ...clamp,
    easing: easeInOut,
  });
  const popOut = interpolate(frame, [236, 252], [0, 1], {
    ...clamp,
    easing: springy,
  });

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      <AbsoluteFill
        style={{
          scale: `${interpolate(frame, [0, 60], [1.12, 1.05], { ...clamp, easing: easeOut })}`,
        }}
      >
        <BusBackdrop dim={0.25} speed={0.7} />
      </AbsoluteFill>

      {/* Camera: punch in on the dock for "tap, tap", ride the dive, pull back */}
      <AbsoluteFill
        style={{
          transformOrigin: `${MIC.x}px ${MIC.y}px`,
          scale: `${interpolate(frame, [28, 42, 60, 84], [1, 1.45, 1.45, 1], { ...clamp, easing: easeInOut })}`,
        }}
      >
        <Phone x={PHONE.x} y={PHONE.y} width={PHONE.w}>
          <AnyApp scroll={Math.min(frame, tap1) * 4} />
          <div style={{ position: "absolute", left: 0, top: DOCK_TOP }}>
            <FloatingDock
              state={dockState}
              dp={DP}
              seconds={recSeconds}
              open={dockOpen}
              glow={
                frame < tap1
                  ? interpolate(
                      frame,
                      [dockGlow, dockGlow + 8],
                      [0, 0.6 + 0.4 * Math.sin(frame / 3)],
                      clamp,
                    )
                  : 0
              }
            />
          </div>
          <div
            style={{ position: "absolute", left: 21, right: 21, top: notifY }}
          >
            <SavedNotification
              width={SCREEN.w - 42}
              finishing={frame < saved + 16}
              seconds={Math.round((stop - tap2) / 30)}
            />
          </div>
        </Phone>

        {/* Gold flash where the spark is caught */}
        <div
          style={{
            position: "absolute",
            left: MIC.x - 200,
            top: MIC.y - 200,
            width: 400,
            height: 400,
            borderRadius: 999,
            background: `radial-gradient(circle, ${C.goldLight} 0%, ${C.gold}88 30%, transparent 70%)`,
            opacity: flash,
            scale: `${0.4 + flash}`,
          }}
        />

        {frame < tap2 + 4 && (
          <CountdownRing
            x={SPARK_HOME.x}
            y={SPARK_HOME.y}
            r={110}
            progress={interpolate(frame, [0, tap2], [0.25, 0.04], clamp)}
            label="1"
            life={0}
          />
        )}
        {frame < diveEnd && (
          <Spark
            size={sparkSize}
            life={sparkLife}
            lookX={
              frame < dive
                ? interpolate(frame, [dockGlow, dockGlow + 10], [0, -1], clamp)
                : -1
            }
            lookY={
              frame < dive
                ? interpolate(frame, [dockGlow, dockGlow + 10], [0, 1], clamp)
                : 1
            }
            stretch={
              frame < dive ? 1 : interpolate(d, [0, 0.5, 1], [1, 1.5, 1])
            }
            style={{
              position: "absolute",
              left: sx - sparkSize / 2,
              top: sy - sparkSize / 2,
            }}
          />
        )}

        <Ripple x={MIC.x} y={MIC.y} t={(frame - tap1) / 12} />
        <Ripple x={MIC.x} y={MIC.y} t={(frame - tap2) / 14} color={C.crimson} />
        <Ripple x={STOP.x} y={STOP.y} t={(frame - stop) / 12} />
        <Thumb x={thumbX} y={thumbY} press={press} />
      </AbsoluteFill>

      {/* Live transcript (the spark's words), transcribed on the phone */}
      <div
        style={{
          position: "absolute",
          left: 60,
          right: 60,
          top: 100,
          padding: "30px 36px",
          borderRadius: 36,
          background: "rgba(15,23,42,0.88)",
          opacity: panelIn,
          translate: `0 ${interpolate(panelIn, [0, 1], [30, 0])}px`,
          fontFamily: FONT.sans,
        }}
      >
        <div
          style={{
            fontSize: 46,
            fontWeight: 700,
            lineHeight: 1.3,
            color: C.white,
            minHeight: 180,
          }}
        >
          {TRANSCRIPT.map((w, i) => (
            <span
              key={i}
              style={{
                color: i === shown - 1 ? C.gold : C.white,
                opacity: i < shown ? 1 : 0,
              }}
            >
              {w}{" "}
            </span>
          ))}
        </div>
        <div style={{ marginTop: 16 }}>
          <OnDeviceBadge size={32} color={C.emerald} />
        </div>
      </div>

      <div
        style={{
          position: "absolute",
          left: 540,
          top: 600,
          width: 10,
          height: 10,
          borderRadius: 999,
          background: "#F4F5F8",
          scale: `${wipe * 500}`,
        }}
      />
      {/* The caught spark pops out of "Saved", happy, and the world brightens */}
      {frame >= 236 && (
        <Spark
          size={180}
          life={1}
          lookY={-0.3}
          stretch={interpolate(frame, [236, 244, 252], [0.7, 1.25, 1], clamp)}
          style={{
            position: "absolute",
            left: 540 - 90,
            top: interpolate(popOut, [0, 1], [640, 330]),
            opacity: popOut,
          }}
        />
      )}
    </AbsoluteFill>
  );
};
