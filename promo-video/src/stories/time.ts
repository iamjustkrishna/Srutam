// Story time for the 60 fps videos: timelines stay in 30 fps "story frames",
// renders run at 60 fps, and every animation reads a continuous time t (in story
// frames, steps of 0.5) so motion gets twice the in-betweens.
import { useCurrentFrame } from "remotion";

export const STORY_FPS = 30;
export const RENDER_FPS = 60;
/** Render frames per story frame. */
export const K = RENDER_FPS / STORY_FPS;

/** The current time in story frames (fractional at 60 fps). */
export const useT = () => useCurrentFrame() / K;
