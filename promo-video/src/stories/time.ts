// Story time for the 60 fps videos: timelines stay in 30 fps "story frames",
// renders run at 60 fps, and every animation reads a continuous time t (in story
// frames, steps of 0.5) so motion gets twice the in-betweens.
//
// A video can instead animate "on twos" like hand-drawn animation: wrap it in
// <Drawn step={2}> and every pose is held for 2 story frames (15 drawings a second).
import { createContext, useContext } from "react";
import { useCurrentFrame } from "remotion";

export const STORY_FPS = 30;
export const RENDER_FPS = 60;
/** Render frames per story frame. */
export const K = RENDER_FPS / STORY_FPS;

/** Story frames each drawing is held for (0 = smooth, every render frame). */
export const DrawnStep = createContext(0);

/** The current time in story frames (fractional at 60 fps; held on twos inside <Drawn>). */
export const useT = () => {
  const t = useCurrentFrame() / K;
  const step = useContext(DrawnStep);
  return step ? Math.floor(t / step) * step : t;
};
