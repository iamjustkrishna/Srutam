import {
  AbsoluteFill,
  Img,
  interpolate,
  staticFile,
  useCurrentFrame,
} from "remotion";
import { clamp, easeInOut, easeOut, springy } from "../anim";
import { Spark } from "../components/Spark";
import { useWide } from "../landscape/frame";
import { C, FONT } from "../theme";

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
  const r = interpolate(frame, [0, 34], [520, 210], {
    ...clamp,
    easing: easeOut,
  });
  const sx = 540 + Math.cos(a) * r;
  const sy = 700 + Math.sin(a) * r * 0.8;
  // In a landscape cut the backdrop runs past the frame's edges (same gradient).
  const bleed = useWide() ? 900 : 0;

  return (
    <AbsoluteFill style={{ fontFamily: FONT.sans, alignItems: "center" }}>
      <div
        style={{
          position: "absolute",
          top: 0,
          bottom: 0,
          left: -bleed,
          right: -bleed,
          background: `radial-gradient(circle 1307px at ${540 + bleed}px 730px, ${C.cobaltContainer} 0%, ${C.slate50} 60%)`,
        }}
      />
      <Img
        src={staticFile("img/logo.png")}
        style={{
          position: "absolute",
          top: 470,
          width: 460,
          height: 460,
          scale: `${logo}`,
          opacity: logo,
        }}
      />
      <div
        style={{
          position: "absolute",
          top: 930,
          fontFamily: FONT.serif,
          fontSize: 170,
          color: C.slate900,
          opacity: word,
          translate: `0 ${(1 - word) * 30}px`,
        }}
      >
        Srutam
      </div>
      <div
        style={{
          position: "absolute",
          top: 1170,
          fontSize: 70,
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
          top: 1340,
          padding: "26px 52px",
          borderRadius: 999,
          background: C.slate900,
          color: C.white,
          fontSize: 46,
          fontWeight: 700,
          opacity: pill,
          scale: `${0.9 + 0.1 * pill}`,
        }}
      >
        Available on Android
      </div>
      <Spark
        size={110}
        life={1}
        lookX={-0.6}
        lookY={0.4}
        seed={5}
        style={{ position: "absolute", left: sx - 55, top: sy - 55 }}
      />
      {/* Fade into the opening frame's grey: a seamless loop on Reels/Shorts */}
      <div style={{ position: "absolute", top: 0, bottom: 0, left: -bleed, right: -bleed, background: C.slate300, opacity: loop }} />
    </AbsoluteFill>
  );
};
