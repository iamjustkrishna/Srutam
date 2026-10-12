import { Easing, interpolate } from "remotion";

export const clamp = {
  extrapolateLeft: "clamp",
  extrapolateRight: "clamp",
} as const;
export const easeOut = Easing.bezier(0.16, 1, 0.3, 1);
export const easeInOut = Easing.bezier(0.65, 0, 0.35, 1);
export const easeIn = Easing.bezier(0.55, 0, 1, 0.45);
export const springy = Easing.bezier(0.34, 1.56, 0.64, 1);

/** 0 -> 1 over [start, start + length] with an ease-out. */
export const enter = (
  frame: number,
  start: number,
  length = 15,
  easing = easeOut,
) => interpolate(frame, [start, start + length], [0, 1], { ...clamp, easing });

/** Deterministic pseudo-random in [0, 1) for a given seed. */
export const rand = (seed: number) => {
  const x = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
  return x - Math.floor(x);
};
