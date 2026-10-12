// A light character rig for the sketch cast: two-bone IK arms with hand poses,
// head tilt, breathing and squash & stretch, wrapped around MeeraSketch or Person.
// All coordinates are in the characters' 600 x 800 local box.
import type { ReactNode } from "react";
import { MeeraSketch, type SketchExpr } from "../sketch/MeeraSketch";
import { Person, type PExpr } from "../storyboards/People";
import type { Viseme } from "./Mouth";

const LINE = "#4A2E28";

export type Pt = [number, number];
export type HandPose = "open" | "point" | "fist" | "grip" | "thumb" | "phone";

export type ArmPose = {
  /** Where the wrist goes (IK target), local coords. */
  to: Pt;
  pose: HandPose;
  /** Which way the elbow bends: 1 = outward/down (natural), -1 = inward. */
  bend?: 1 | -1;
  /** Extra wrist rotation in degrees. */
  rot?: number;
  /** Keep the hand pointing straight up (a phone held up to show its screen). */
  upright?: boolean;
  /** Draw behind the body (e.g. an arm reaching back). */
  behind?: boolean;
};

const SHOULDER = { l: [158, 640] as Pt, r: [442, 640] as Pt };

/** Two-bone IK: the elbow for a shoulder, a wrist target and bone lengths. */
export const solveElbow = (s: Pt, t: Pt, l1: number, l2: number, bend: number): { e: Pt; w: Pt } => {
  const dx = t[0] - s[0];
  const dy = t[1] - s[1];
  const d0 = Math.hypot(dx, dy) || 1;
  const d = Math.min(Math.max(d0, Math.abs(l1 - l2) + 1), l1 + l2 - 0.5);
  const ux = dx / d0;
  const uy = dy / d0;
  const a = (l1 * l1 - l2 * l2 + d * d) / (2 * d);
  const h = Math.sqrt(Math.max(0, l1 * l1 - a * a));
  const e: Pt = [s[0] + ux * a - uy * h * bend, s[1] + uy * a + ux * h * bend];
  return { e, w: [s[0] + ux * d, s[1] + uy * d] };
};

/** A hand pointing along +x from the wrist at (0, 0); `mirror` flips the thumb side. */
export const Hand: React.FC<{ pose: HandPose; skin: string; mirror?: boolean; screen?: ReactNode }> = ({ pose, skin, mirror = false, screen }) => {
  const st = { fill: skin, stroke: LINE, strokeWidth: 3.2, strokeLinejoin: "round" as const };
  const finger = (x: number, y: number, len: number, rot = 0, w = 13) => (
    <rect x={x} y={y - w / 2} width={len} height={w} rx={w / 2} transform={`rotate(${rot} ${x} ${y})`} {...st} />
  );
  const fist = (
    <g>
      <rect x={6} y={-24} width={52} height={48} rx={18} {...st} />
      <path d="M40 -22 L40 -6 M48 -16 L48 2 M40 6 L40 20" stroke={LINE} strokeWidth={2} opacity={0.6} />
    </g>
  );
  const body = (() => {
    switch (pose) {
      case "open":
        return (
          <g>
            {finger(46, -18, 34, -14)}
            {finger(50, -6, 38, -4)}
            {finger(50, 6, 36, 4)}
            {finger(46, 17, 30, 14, 12)}
            <ellipse cx={30} cy={0} rx={28} ry={25} {...st} />
            {finger(18, -20, 30, -58, 14)}
          </g>
        );
      case "point":
        return (
          <g>
            {fist}
            {finger(48, -14, 48, -2)}
            <path d="M14 -20 Q34 -30 44 -18" stroke={LINE} strokeWidth={3} fill={skin} />
          </g>
        );
      case "thumb":
        return (
          <g>
            {fist}
            {finger(16, -22, 34, -90, 15)}
          </g>
        );
      case "grip":
        return (
          <g>
            <ellipse cx={26} cy={0} rx={26} ry={24} {...st} />
            {[-14, -3, 8, 18].map((y, i) => (
              <path key={i} d={`M40 ${y} q22 -2 24 ${10 - i}`} stroke={LINE} strokeWidth={13} strokeLinecap="round" fill="none" />
            ))}
            {[-14, -3, 8, 18].map((y, i) => (
              <path key={`f${i}`} d={`M40 ${y} q22 -2 24 ${10 - i}`} stroke={skin} strokeWidth={8} strokeLinecap="round" fill="none" />
            ))}
          </g>
        );
      case "phone":
        return (
          <g>
            {/* phone upright along the forearm: its top points where the fingers do */}
            <g transform="translate(96 0) rotate(90)">
              <rect x={-44} y={-90} width={88} height={180} rx={14} fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
              {screen ?? <rect x={-38} y={-82} width={76} height={160} rx={9} fill="#1E2A5A" />}
              <path d="M-28 -70 L10 -76" stroke="#FFFFFF" strokeWidth={4} opacity={0.25} />
            </g>
            {fist}
          </g>
        );
      default:
        return fist;
    }
  })();
  return <g transform={mirror ? "scale(1 -1)" : undefined}>{body}</g>;
};

export const Arm: React.FC<{
  shoulder: Pt;
  arm: ArmPose;
  sleeve: string;
  skin: string;
  len?: [number, number];
  width?: number;
  mirror?: boolean;
  screen?: ReactNode;
}> = ({ shoulder, arm, sleeve, skin, len = [150, 140], width = 64, mirror = false, screen }) => {
  // Left arms solve with the opposite sign so bend = 1 always puts the elbow outward.
  const { e, w } = solveElbow(shoulder, arm.to, len[0], len[1], (arm.bend ?? 1) * (mirror ? -1 : 1));
  const ang = (Math.atan2(w[1] - e[1], w[0] - e[0]) * 180) / Math.PI;
  const path = `M${shoulder[0]} ${shoulder[1]} L${e[0]} ${e[1]} L${w[0]} ${w[1]}`;
  const cuffAt: Pt = [w[0] - Math.cos((ang * Math.PI) / 180) * 18, w[1] - Math.sin((ang * Math.PI) / 180) * 18];
  return (
    <g>
      <path d={path} stroke={LINE} strokeWidth={width + 6} strokeLinecap="round" strokeLinejoin="round" fill="none" />
      <path d={path} stroke={sleeve} strokeWidth={width} strokeLinecap="round" strokeLinejoin="round" fill="none" />
      {/* a fold at the elbow */}
      <circle cx={e[0]} cy={e[1]} r={width * 0.18} fill="none" stroke={LINE} strokeWidth={2} opacity={0.35} />
      <g transform={`translate(${cuffAt[0]} ${cuffAt[1]}) rotate(${ang})`}>
        <rect x={-6} y={-width / 2 - 2} width={14} height={width + 4} rx={6} fill={sleeve} stroke={LINE} strokeWidth={2.4} />
      </g>
      <g transform={`translate(${w[0]} ${w[1]}) rotate(${arm.upright ? -90 + (arm.rot ?? 0) : ang + (arm.rot ?? 0)}) scale(1.22)`}>
        <Hand pose={arm.pose} skin={skin} mirror={mirror} screen={screen} />
      </g>
    </g>
  );
};

type PersonLook = Omit<React.ComponentProps<typeof Person>, "x" | "y" | "s" | "expr" | "viseme" | "gaze" | "turn" | "blink">;

export type ActorProps = {
  x: number;
  y: number;
  s: number;
  /** "meera" or a Person look (hair, outfit, colours...). */
  who: "meera" | PersonLook;
  expr: SketchExpr | PExpr;
  viseme?: Viseme;
  gaze?: [number, number];
  turn?: number;
  /** Head-and-shoulders lean in degrees. */
  tilt?: number;
  blink?: boolean;
  arms?: { l?: ArmPose; r?: ArmPose };
  /** Frame, for breathing. */
  f?: number;
  /** Squash (<1) and stretch (>1) about the base. */
  squash?: number;
  sway?: number;
  screen?: ReactNode;
};

export const Actor: React.FC<ActorProps> = ({
  x,
  y,
  s,
  who,
  expr,
  viseme,
  gaze = [0, 0],
  turn = 0,
  tilt = 0,
  blink = false,
  arms = {},
  f = 0,
  squash = 1,
  sway = 0,
  screen,
}) => {
  const meera = who === "meera";
  const sleeve = meera ? "#EFE6D8" : who.outfit === "blazer" ? "#334155" : who.outfit === "saree" ? (who.skin ?? "#E8B48F") : (who.cloth ?? "#6B8CC7");
  const skin = meera ? "#F2C3A2" : (who.skin ?? "#E8B48F");
  const breath = 1 + 0.008 * Math.sin(f / 18);
  const sy = squash * breath;
  const sx = 1 / Math.sqrt(squash);
  const arm = (side: "l" | "r") => {
    const a = arms[side];
    return a ? <Arm key={side} shoulder={SHOULDER[side]} arm={a} sleeve={sleeve} skin={skin} mirror={side === "l"} screen={screen} /> : null;
  };
  const behind = (["l", "r"] as const).filter((k) => arms[k]?.behind);
  const front = (["l", "r"] as const).filter((k) => arms[k] && !arms[k]!.behind);
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <g transform={`translate(300 800) scale(${sx} ${sy}) rotate(${tilt}) translate(-300 -800)`}>
        {behind.map(arm)}
        {meera ? (
          <MeeraSketch x={0} y={0} s={1} expr={expr as SketchExpr} viseme={viseme} gaze={gaze} turn={turn} blink={blink} sway={sway} />
        ) : (
          <Person {...who} x={0} y={0} s={1} expr={expr as PExpr} viseme={viseme} gaze={gaze} turn={turn} blink={blink} />
        )}
        {front.map(arm)}
      </g>
    </g>
  );
};

/** Natural idle eye movement: small saccades every ~1.5 s, deterministic per seed. */
export const saccade = (f: number, seed = 0): [number, number] => {
  const k = Math.floor((f + seed * 13) / 46);
  const r = (n: number) => {
    const v = Math.sin((k + n) * 12.9898 + seed * 78.233) * 43758.5453;
    return v - Math.floor(v);
  };
  return [(r(1) - 0.5) * 0.9, (r(2) - 0.5) * 0.5];
};

/** A natural blink: closes for 3 frames roughly every 3 s. */
export const blinkAt = (f: number, seed = 0) => {
  const period = 84 + (seed % 5) * 9;
  return (f + seed * 17) % period < 3;
};
