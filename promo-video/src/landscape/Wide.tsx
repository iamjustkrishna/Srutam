// Landscape (16:9) cuts of the vertical story videos. The vertical video is drawn in
// vectors, so each shot is re-rendered through a window into it at full resolution:
// a reframe (optionally drifting to a second framing), two panels side by side
// (say the phone and the face reacting to it), or a landscape-native card. Windows
// are [centre x, centre y, width] in the vertical frame's 1080 x 1920 coordinates;
// the height follows the panel's shape.
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut } from "../anim";
import type { CaptionCue } from "../stories/Story";
import { FONT } from "../theme";
import { FrameCtx } from "./frame";

export const WIDE_W = 1920;
export const WIDE_H = 1080;
const VW = 1080;
const VH = 1920;
const GAP = 8;

export type Win = [cx: number, cy: number, w: number];
export type Box = [left: number, top: number, width: number, height: number];

type Common = {
  /** Start, in timeline frames (30 fps); the shot runs until the next one. */
  at: number;
  /** Ease from the previous shot's last framing over this many frames instead of cutting. */
  blend?: number;
  /** Colour around the panels and past the vertical frame's edges (flat backgrounds only). */
  bg?: string;
  /** Caption lane [left, right] in px, when the bottom centre is busy. */
  cap?: [number, number];
  /** Landscape-native layer on top (titles, HUDs), given the story frame. */
  over?: React.FC<{ f: number }>;
};
export type Shot = Common &
  (
    | { crop: Win; to?: Win }
    | { split: [Win, Win]; to?: [Win, Win]; ratio?: number }
    | { panels: { box: Box; win: Win; to?: Win; open?: boolean }[] }
    | { card: React.FC<{ f: number; V: React.FC }> }
  );

const lerpWin = (a: Win, b: Win, t: number): Win => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];

/** A window into the vertical video, filling a box in the landscape frame. */
export const Panel: React.FC<{
  V: React.FC;
  win: Win;
  left: number;
  top: number;
  width: number;
  height: number;
  bg?: string;
  /** Let the window reach past the picture's edges (over a matching `bg`). */
  open?: boolean;
}> = ({ V, win, left, top, width, height, bg, open }) => {
  const [cx, cy, w] = win;
  const s = width / w;
  const h = height / s;
  let x = cx - w / 2;
  let y = cy - h / 2;
  if (!open) {
    // Keep the window inside the picture so no edge ever shows.
    x = w >= VW ? (VW - w) / 2 : Math.min(Math.max(x, 0), VW - w);
    y = h >= VH ? (VH - h) / 2 : Math.min(Math.max(y, 0), VH - h);
  }
  return (
    <div style={{ position: "absolute", left, top, width, height, overflow: "hidden", background: bg ?? "#000" }}>
      <div
        style={{
          position: "absolute",
          left: 0,
          top: 0,
          width: VW,
          height: VH,
          transformOrigin: "0 0",
          transform: `scale(${s}) translate(${-x}px, ${-y}px)`,
        }}
      >
        <FrameCtx.Provider value={{ wide: true }}>
          <V />
        </FrameCtx.Provider>
      </div>
    </div>
  );
};

const FULL: Box = [0, 0, WIDE_W, WIDE_H];
const splitBoxes = (ratio: number): Box[] => {
  const lw = Math.round(WIDE_W * ratio - GAP / 2);
  return [
    [0, 0, lw, WIDE_H],
    [lw + GAP, 0, WIDE_W - lw - GAP, WIDE_H],
  ];
};

/** The panels of a shot at story frame f (before any blend). */
type Placed = { box: Box; win: Win; open?: boolean };
const layout = (shot: Shot, f: number, end: number): Placed[] | null => {
  if ("card" in shot) return null;
  const t = interpolate(f, [shot.at, end], [0, 1], { ...clamp, easing: easeInOut });
  const at = (w: Win, to?: Win) => (to ? lerpWin(w, to, t) : w);
  if ("crop" in shot) return [{ box: FULL, win: at(shot.crop, shot.to), open: !!shot.bg }];
  if ("split" in shot) {
    const boxes = splitBoxes(shot.ratio ?? 0.5);
    return shot.split.map((w, i) => ({ box: boxes[i], win: at(w, shot.to?.[i]), open: !!shot.bg }));
  }
  return shot.panels.map((p) => ({ box: p.box, win: at(p.win, p.to), open: p.open }));
};

export const WideCaptions: React.FC<{ cues: CaptionCue[]; f: number; lane?: [number, number] }> = ({
  cues,
  f,
  lane = [160, 160],
}) => {
  const c = cues.find((x) => f >= x.from - 2 && f <= x.to + 4);
  if (!c) return null;
  const k = interpolate(f, [c.from - 2, c.from + 5, c.to, c.to + 4], [0, 1, 1, 0], clamp);
  return (
    <div
      style={{
        position: "absolute",
        left: lane[0],
        right: lane[1],
        top: c.top ? 44 : undefined,
        bottom: c.top ? undefined : 58,
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
          background: "rgba(26,20,51,0.82)",
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

/**
 * A landscape cut: `shots` in timeline frames; `k` render frames per timeline frame
 * (2 for the 60 fps videos). The vertical video's own captions are swapped for
 * landscape ones.
 */
export const Wide: React.FC<{ V: React.FC; shots: Shot[]; cues: CaptionCue[]; k?: number; total: number }> = ({
  V,
  shots,
  cues,
  k = 1,
  total,
}) => {
  const f = useCurrentFrame() / k;
  let i = 0;
  while (i + 1 < shots.length && shots[i + 1].at <= f) i++;
  const shot = shots[i];
  const end = shots[i + 1]?.at ?? total;
  let ps = layout(shot, f, end);
  const prev = shots[i - 1];
  if (ps && shot.blend && prev && f < shot.at + shot.blend) {
    const from = layout(prev, shot.at, shot.at);
    if (from && from.length === ps.length) {
      const t = interpolate(f, [shot.at, shot.at + shot.blend], [0, 1], { ...clamp, easing: easeInOut });
      ps = ps.map((p, j) => ({
        ...p,
        box: p.box.map((v, n) => from[j].box[n] + (v - from[j].box[n]) * t) as Box,
        win: lerpWin(from[j].win, p.win, t),
      }));
    }
  }
  return (
    <AbsoluteFill style={{ background: shot.bg ?? "#0D0A1A" }}>
      {"card" in shot ? (
        <shot.card f={f - shot.at} V={V} />
      ) : (
        ps!.map(({ box: [left, top, width, height], win, open }, j) => (
          <Panel key={`p${j}`} V={V} win={win} left={left} top={top} width={width} height={height} bg={shot.bg} open={open} />
        ))
      )}
      {shot.over && <shot.over f={f} />}
      <WideCaptions cues={cues} f={f} lane={shot.cap} />
    </AbsoluteFill>
  );
};
