import {
  AbsoluteFill,
  Img,
  interpolate,
  staticFile,
  useCurrentFrame,
} from "remotion";
import { clamp, easeInOut, easeOut, springy } from "../../anim";
import { Spark } from "../../components/Spark";
import { C, FONT } from "../../theme";

// A horizontal lockup (logo + wordmark) with the line and the pill under it.
const LOGO = { size: 340, cx: 630, cy: 380 };

/** 0:26.3 - Brand resolve, then fade to the opening's grey so it loops. */
export const End: React.FC = () => {
  const frame = useCurrentFrame();
  const logo = interpolate(frame, [4, 22], [0, 1], {
    ...clamp,
    easing: springy,
  });
  const word = interpolate(frame, [14, 28], [0, 1], {
    ...clamp,
    easing: easeOut,
  });
  const tag = interpolate(frame, [30, 42], [0, 1], {
    ...clamp,
    easing: easeOut,
  });
  const pill = interpolate(frame, [50, 62], [0, 1], {
    ...clamp,
    easing: easeOut,
  });
  const loop = interpolate(frame, [94, 110], [0, 1], {
    ...clamp,
    easing: easeInOut,
  });

  // The spark circles in and settles on the logo's shoulder
  const a = interpolate(frame, [0, 34], [Math.PI, Math.PI * 2.75], {
    ...clamp,
    easing: easeOut,
  });
  const r = interpolate(frame, [0, 34], [560, 165], {
    ...clamp,
    easing: easeOut,
  });
  const sx = LOGO.cx + Math.cos(a) * r;
  const sy = LOGO.cy + Math.sin(a) * r * 0.8;

  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(ellipse 70% 80% at 50% 40%, ${C.cobaltContainer} 0%, ${C.slate50} 70%)`,
        fontFamily: FONT.sans,
      }}
    >
      <Img
        src={staticFile("img/logo.png")}
        style={{
          position: "absolute",
          left: LOGO.cx - LOGO.size / 2,
          top: LOGO.cy - LOGO.size / 2,
          width: LOGO.size,
          height: LOGO.size,
          scale: `${logo}`,
          opacity: logo,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: LOGO.cx + LOGO.size / 2 + 30,
          top: LOGO.cy,
          translate: `${(1 - word) * 30}px -54%`,
          fontFamily: FONT.serif,
          fontSize: 200,
          lineHeight: 1.1,
          color: C.slate900,
          opacity: word,
        }}
      >
        Srutam
      </div>
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 630,
          fontSize: 76,
          fontWeight: 800,
          color: C.slate900,
          opacity: tag,
          textAlign: "center",
        }}
      >
        Catch it <span style={{ color: C.cobalt }}>before it's gone.</span>
      </div>
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: 790,
          display: "flex",
          justifyContent: "center",
        }}
      >
        <div
          style={{
            padding: "26px 56px",
            borderRadius: 999,
            background: C.slate900,
            color: C.white,
            fontSize: 48,
            fontWeight: 700,
            opacity: pill,
            scale: `${0.9 + 0.1 * pill}`,
          }}
        >
          Available on Android
        </div>
      </div>
      <Spark
        size={100}
        life={1}
        lookX={-0.6}
        lookY={0.4}
        seed={5}
        style={{ position: "absolute", left: sx - 50, top: sy - 50 }}
      />
      {/* Fade into the opening frame's grey: a seamless loop */}
      <AbsoluteFill style={{ background: C.slate300, opacity: loop }} />
    </AbsoluteFill>
  );
};
