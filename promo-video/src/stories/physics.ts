// Small physics helpers for character animation: spring-driven poses with
// overshoot, ballistic arcs under gravity, damped wobbles, follow-through and a
// handheld camera. Everything is a pure function of the frame (30 fps).
import { spring } from "remotion";
import type { Pt } from "../cast/Rig";

const FPS = 30;

export type Key<T> = { at: number; to: T };

/**
 * A value that springs from target to target: each key starts a spring from the
 * previous target to the new one (mass-spring overshoot, then settle).
 */
export const springAt = (f: number, from: number, keys: Key<number>[], damping = 12, stiffness = 150, mass = 1) => {
  let v = from;
  let prev = from;
  for (const k of keys) {
    // Stepped at 60 fps so half story frames (60 fps renders) get their own in-betweens.
    const p = spring({ frame: (f - k.at) * 2, fps: FPS * 2, config: { damping, stiffness, mass } });
    v += (k.to - prev) * p;
    prev = k.to;
  }
  return v;
};

/** springAt for a 2D point (IK targets). */
export const springPt = (f: number, from: Pt, keys: Key<Pt>[], damping = 12, stiffness = 150, mass = 1): Pt => [
  springAt(f, from[0], keys.map((k) => ({ at: k.at, to: k.to[0] })), damping, stiffness, mass),
  springAt(f, from[1], keys.map((k) => ({ at: k.at, to: k.to[1] })), damping, stiffness, mass),
];

/** Position of something thrown at frame `at` from p0 with velocity v (px/s) under gravity g (px/s^2). */
export const ballistic = (f: number, at: number, p0: Pt, v: Pt, g = 2600): Pt => {
  const t = Math.max(0, (f - at) / FPS);
  return [p0[0] + v[0] * t, p0[1] + v[1] * t + 0.5 * g * t * t];
};

/** A damped oscillation that starts at `at`: amp * e^(-decay t) * sin(2 pi freq t). */
export const damped = (f: number, at: number, amp: number, freq = 3, decay = 5) => {
  const t = (f - at) / FPS;
  return t < 0 ? 0 : amp * Math.exp(-decay * t) * Math.sin(2 * Math.PI * freq * t);
};

/** Follow-through: how far a value has moved over the last `lag` frames (for hair, earrings, cloth). */
export const follow = (fn: (f: number) => number, f: number, lag = 4, k = 0.25) => (fn(f) - fn(f - lag)) * k;

/** A gentle handheld camera: smooth drift in x, y and rotation. */
export const handheld = (f: number, amp = 6, seed = 1): { x: number; y: number; rot: number } => ({
  x: amp * (Math.sin(f / 37 + seed) * 0.6 + Math.sin(f / 23 + seed * 2) * 0.4),
  y: amp * (Math.sin(f / 41 + seed * 3) * 0.6 + Math.sin(f / 29 + seed) * 0.4),
  rot: 0.25 * Math.sin(f / 53 + seed),
});

/**
 * Something light (a sheet of paper) thrown at `at` from p0 with velocity v: air drag
 * pulls its velocity toward the terminal velocity vt at rate k (1/s), and it flutters.
 */
export const flutter = (f: number, at: number, p0: Pt, v: Pt, vt: Pt = [0, 340], k = 2.4, sway = 46, freq = 1.2): Pt => {
  const t = Math.max(0, (f - at) / FPS);
  const e = (1 - Math.exp(-k * t)) / k;
  return [p0[0] + vt[0] * t + (v[0] - vt[0]) * e + sway * Math.sin(2 * Math.PI * freq * t) * Math.min(1, t * 2), p0[1] + vt[1] * t + (v[1] - vt[1]) * e];
};

/** Angle (degrees) of something spun at `at` with angular velocity w0 (deg/s) that friction slows at rate `decay` (1/s). */
export const spin = (f: number, at: number, w0: number, decay = 1.5) => {
  const t = Math.max(0, (f - at) / FPS);
  return (w0 / decay) * (1 - Math.exp(-decay * t));
};
