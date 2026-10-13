// Shared pieces for the native 16:9 cuts. Each landscape video is restaged for a
// 1920 x 1080 canvas (not reframed from the vertical one): wrap its scenes in
// <Landscape> so Frame, Camera, Grade and LightLeak size themselves to it.
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeOut } from "../anim";
import { CanvasCtx } from "../stories/canvas";
import type { CaptionCue } from "../stories/Story";
import { FONT } from "../theme";

export const LW = 1920;
export const LH = 1080;

export const Landscape: React.FC<{ children: React.ReactNode; bg?: string }> = ({ children, bg = "#000" }) => (
  <CanvasCtx.Provider value={{ w: LW, h: LH }}>
    <AbsoluteFill style={{ background: bg }}>{children}</AbsoluteFill>
  </CanvasCtx.Provider>
);

/**
 * Captions for the landscape cuts: bottom centre (or top for `top` cues), sized for
 * 1920 x 1080. `k` is render frames per timeline frame (2 at 60 fps); `lane` narrows
 * the caption to [left, right] insets when the centre of the frame is busy.
 */
export const LCaptions: React.FC<{ cues: CaptionCue[]; k?: number; lane?: (f: number) => [number, number] | undefined }> = ({
  cues,
  k: rate = 1,
  lane,
}) => {
  const f = useCurrentFrame() / rate;
  const c = cues.find((x) => f >= x.from - 2 && f <= x.to + 4);
  if (!c) return null;
  const k = interpolate(f, [c.from - 2, c.from + 5, c.to, c.to + 4], [0, 1, 1, 0], clamp);
  const [l, r] = lane?.(f) ?? [200, 200];
  return (
    <div
      style={{
        position: "absolute",
        left: l,
        right: r,
        top: c.top ? 40 : undefined,
        bottom: c.top ? undefined : 52,
        display: "flex",
        justifyContent: "center",
        opacity: k,
        translate: `0 ${interpolate(k, [0, 1], [14, 0], { ...clamp, easing: easeOut })}px`,
      }}
    >
      <div
        style={{
          padding: "14px 32px",
          borderRadius: 26,
          background: "rgba(26,20,51,0.84)",
          color: "#FFFFFF",
          fontFamily: FONT.sans,
          fontWeight: 800,
          fontSize: 44,
          textAlign: "center",
          lineHeight: 1.2,
          maxWidth: 1400,
        }}
      >
        {c.text}
      </div>
    </div>
  );
};

/** A dark chip at top centre (date stamps), faded over `keys` [in, full, full, out]. */
export const Chip: React.FC<{ f: number; text: string; keys: [number, number, number, number]; x?: number }> = ({ f, text, keys, x }) => (
  <div
    style={{
      position: "absolute",
      top: 40,
      left: x ?? 0,
      right: x === undefined ? 0 : undefined,
      display: "flex",
      justifyContent: "center",
      opacity: interpolate(f, keys, [0, 1, 1, 0], clamp),
    }}
  >
    <div style={{ padding: "12px 30px", borderRadius: 20, background: "rgba(15,23,42,0.78)", color: "#FFF", fontFamily: FONT.sans, fontWeight: 700, fontSize: 40 }}>
      {text}
    </div>
  </div>
);
