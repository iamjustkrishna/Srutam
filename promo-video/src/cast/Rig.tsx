// Character rig (v3) for the sketch cast: two-bone IK arms with tapered sleeves
// and anatomical hands, real proportions (arm lengths and widths from the head
// and shoulder size), head tilt, breathing, squash & stretch and a soft drop
// shadow, wrapped around MeeraSketch or Person. Coordinates are in the
// characters' 600 x 800 local box (head ~330 tall, shoulders ~520 wide).
import { useId, type ReactNode } from "react";
import { MeeraSketch, type SketchExpr } from "../sketch/MeeraSketch";
import { Person, type PExpr } from "../storyboards/People";
import type { Viseme } from "./Mouth";

const LINE = "#4A2E28";

export type Pt = [number, number];
export type HandPose = "open" | "point" | "fist" | "grip" | "thumb" | "phone" | "palm";

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

/** Shoulder joints sit on the sloped edge of the shoulders, not inside the chest. */
const SHOULDER = { l: [140, 652] as Pt, r: [460, 652] as Pt };

const shade = (hex: string, k: number) => {
  const n = parseInt(hex.slice(1), 16);
  const c = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => Math.max(0, Math.min(255, Math.round(v * k))));
  return `#${c.map((v) => v.toString(16).padStart(2, "0")).join("")}`;
};

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

/** A tapered limb segment as a polygon plus round joints (for seamless unions). */
const segment = (a: Pt, b: Pt, wa: number, wb: number) => {
  const dx = b[0] - a[0];
  const dy = b[1] - a[1];
  const l = Math.hypot(dx, dy) || 1;
  const nx = -dy / l;
  const ny = dx / l;
  return `M${a[0] + (nx * wa) / 2} ${a[1] + (ny * wa) / 2} L${b[0] + (nx * wb) / 2} ${b[1] + (ny * wb) / 2} L${b[0] - (nx * wb) / 2} ${b[1] - (ny * wb) / 2} L${a[0] - (nx * wa) / 2} ${a[1] - (ny * wa) / 2} Z`;
};

/** A hand pointing along +x from the wrist at (0, 0); `mirror` flips the thumb side. */
export const Hand: React.FC<{ pose: HandPose; skin: string; mirror?: boolean; screen?: ReactNode }> = ({ pose, skin, mirror = false, screen }) => {
  const dark = shade(skin, 0.78);
  const nail = shade(skin, 1.12);
  const st = { fill: skin, stroke: LINE, strokeWidth: 3, strokeLinejoin: "round" as const };
  /** A finger as two tapered phalanges with a nail; `curl` bends the tip down (degrees). */
  const finger = (x: number, y: number, len: number, rot = 0, curl = 0, w = 12) => {
    const l1 = len * 0.55;
    const l2 = len * 0.45;
    return (
      <g transform={`translate(${x} ${y}) rotate(${rot})`}>
        <rect x={-2} y={-w / 2} width={l1 + 4} height={w} rx={w / 2} {...st} />
        <g transform={`translate(${l1} 0) rotate(${curl})`}>
          <rect x={-3} y={-w * 0.46} width={l2 + 3} height={w * 0.92} rx={w * 0.46} {...st} />
          <ellipse cx={l2 - 5} cy={-w * 0.12} rx={4.2} ry={w * 0.24} fill={nail} stroke={dark} strokeWidth={1} />
        </g>
        <path d={`M${l1 - 1} ${-w / 2 + 2} l0 ${w - 4}`} stroke={dark} strokeWidth={1.4} />
      </g>
    );
  };
  const palm = (
    <g>
      <path d="M-2 -21 L40 -27 Q53 -27 53 -16 L53 17 Q53 28 40 27 L-2 21 Z" {...st} />
      <path d="M14 -6 Q28 6 42 -2" stroke={dark} strokeWidth={1.6} fill="none" opacity={0.7} />
    </g>
  );
  const knuckles = (
    <g>
      <path d="M-2 -21 L40 -27 Q58 -27 58 -12 L58 13 Q58 28 40 27 L-2 21 Z" {...st} />
      {[-17, -5, 7, 18].map((y, i) => (
        <path key={i} d={`M58 ${y - 4} q5 4 0 9`} stroke={dark} strokeWidth={1.6} fill="none" />
      ))}
      <path d="M30 -24 L30 24" stroke={dark} strokeWidth={1.2} opacity={0.5} />
    </g>
  );
  const thumbAcross = <path d="M8 -24 C24 -36 46 -30 50 -16 C44 -12 30 -14 18 -10 Z" {...st} />;
  const body = (() => {
    switch (pose) {
      case "open":
      case "palm":
        return (
          <g>
            {finger(48, -19, 34, -10, pose === "palm" ? 8 : 4)}
            {finger(52, -6, 40, -2, pose === "palm" ? 8 : 4)}
            {finger(52, 7, 38, 5, pose === "palm" ? 8 : 4)}
            {finger(48, 19, 29, 13, pose === "palm" ? 8 : 4, 10.5)}
            {palm}
            {finger(12, -22, 30, -55, 12, 14)}
          </g>
        );
      case "point":
        return (
          <g>
            {knuckles}
            {finger(54, -16, 44, -3, 0)}
            {thumbAcross}
          </g>
        );
      case "thumb":
        return (
          <g>
            {knuckles}
            {finger(16, -24, 34, -88, -6, 14)}
          </g>
        );
      case "grip":
        return (
          <g>
            {palm}
            {[-18, -6, 6, 17].map((y, i) => (
              <g key={i}>
                <path d={`M50 ${y} q24 -4 24 ${12 - i * 2}`} stroke={LINE} strokeWidth={14} strokeLinecap="round" fill="none" />
                <path d={`M50 ${y} q24 -4 24 ${12 - i * 2}`} stroke={skin} strokeWidth={9} strokeLinecap="round" fill="none" />
              </g>
            ))}
            {finger(12, -22, 30, -40, 10, 14)}
          </g>
        );
      case "phone":
        return (
          <g>
            {/* phone upright along the forearm: its top points where the fingers do */}
            <g transform="translate(100 0) rotate(90)">
              <rect x={-44} y={-92} width={88} height={184} rx={14} fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
              {screen ?? <rect x={-38} y={-84} width={76} height={164} rx={9} fill="#1E2A5A" />}
              <path d="M-28 -72 L10 -78" stroke="#FFFFFF" strokeWidth={4} opacity={0.25} />
            </g>
            {knuckles}
            {thumbAcross}
          </g>
        );
      default:
        return (
          <g>
            {knuckles}
            {thumbAcross}
          </g>
        );
    }
  })();
  return <g transform={mirror ? "scale(1 -1)" : undefined}>{body}</g>;
};

export type Build = { upper: number; lower: number; shoulderW: number; wristW: number; hand: number };
export const SLIM: Build = { upper: 196, lower: 182, shoulderW: 62, wristW: 40, hand: 1.32 };
export const BROAD: Build = { upper: 204, lower: 188, shoulderW: 74, wristW: 48, hand: 1.45 };

export const Arm: React.FC<{
  shoulder: Pt;
  arm: ArmPose;
  sleeve: string;
  skin: string;
  build?: Build;
  mirror?: boolean;
  screen?: ReactNode;
}> = ({ shoulder, arm, sleeve, skin, build = SLIM, mirror = false, screen }) => {
  // Left arms solve with the opposite sign so bend = 1 always puts the elbow outward.
  const { e, w } = solveElbow(shoulder, arm.to, build.upper, build.lower, (arm.bend ?? 1) * (mirror ? -1 : 1));
  const ang = (Math.atan2(w[1] - e[1], w[0] - e[0]) * 180) / Math.PI;
  const rad = (ang * Math.PI) / 180;
  const cuff: Pt = [w[0] - Math.cos(rad) * 16, w[1] - Math.sin(rad) * 16];
  const we = (build.shoulderW + build.wristW) / 2 + 4;
  const pieces = (grow: number) => (
    <>
      <path d={segment(shoulder, e, build.shoulderW + grow, we + grow)} />
      <path d={segment(e, cuff, we + grow, build.wristW + 6 + grow)} />
      <circle cx={shoulder[0]} cy={shoulder[1]} r={(build.shoulderW + grow) / 2} />
      <circle cx={e[0]} cy={e[1]} r={(we + grow) / 2} />
    </>
  );
  return (
    <g>
      <g fill={LINE}>{pieces(6)}</g>
      <g fill={sleeve}>{pieces(0)}</g>
      {/* sleeve folds at the elbow and the shoulder */}
      <path d={`M${e[0] - 10} ${e[1] - 6} q8 6 16 2 M${e[0] - 6} ${e[1] + 6} q8 4 14 0`} stroke={LINE} strokeWidth={2} fill="none" opacity={0.4} />
      <path d={segment(shoulder, e, build.shoulderW * 0.34, we * 0.3)} fill="#FFFFFF" opacity={0.12} />
      {/* wrist and cuff */}
      <path d={segment(cuff, w, build.wristW - 6, build.wristW - 8)} fill={skin} stroke={LINE} strokeWidth={2.4} />
      <g transform={`translate(${cuff[0]} ${cuff[1]}) rotate(${ang})`}>
        <rect x={-7} y={-(build.wristW + 10) / 2} width={14} height={build.wristW + 10} rx={6} fill={shade(sleeve, 0.92)} stroke={LINE} strokeWidth={2.4} />
      </g>
      <g transform={`translate(${w[0]} ${w[1]}) rotate(${arm.upright ? -90 + (arm.rot ?? 0) : ang + (arm.rot ?? 0)}) scale(${build.hand})`}>
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
  /** Closed (true) or partly closed (0..1, see blinkK). */
  blink?: boolean | number;
  arms?: { l?: ArmPose; r?: ArmPose };
  /** Frame, for breathing. */
  f?: number;
  /** Squash (<1) and stretch (>1) about the base. */
  squash?: number;
  /** Hair swing (-1..1), e.g. from head velocity. */
  sway?: number;
  /** Soft drop shadow onto the background. */
  shadow?: boolean;
  /** Extend the body below the bust (local units) for standing wide shots. */
  torso?: number;
  /** With `torso`: trousers and shoes below the waist; `walk` is the stride phase (radians). */
  legs?: { pants: string; shoes?: string; walk?: number };
  screen?: ReactNode;
};

const isBroad = (who: PersonLook) => who.hair === "short" || who.hair === "bald" || !!who.beard || !!who.moustache;

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
  shadow = false,
  torso = 0,
  legs,
  screen,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const meera = who === "meera";
  const sleeve = meera ? "#EFE6D8" : who.outfit === "blazer" ? "#334155" : who.outfit === "saree" ? (who.skin ?? "#E8B48F") : (who.cloth ?? "#6B8CC7");
  const skin = meera ? "#F2C3A2" : (who.skin ?? "#E8B48F");
  const build = meera ? SLIM : isBroad(who) ? BROAD : SLIM;
  const coat = meera ? "#EFE6D8" : who.outfit === "blazer" ? "#334155" : (who.cloth ?? "#6B8CC7");
  // Where the bust ends, for the torso extension.
  const top = meera ? 792 : 758;
  const bw = meera ? 1 : (who.bw ?? (isBroad(who) ? 1.08 : 1));
  const out = { l: !!arms.l && !arms.l.behind, r: !!arms.r && !arms.r.behind };
  const breath = 1 + 0.008 * Math.sin(f / 18);
  const sy = squash * breath;
  const sx = 1 / Math.sqrt(squash);
  const arm = (side: "l" | "r") => {
    const a = arms[side];
    return a ? <Arm key={side} shoulder={SHOULDER[side]} arm={a} sleeve={sleeve} skin={skin} build={build} mirror={side === "l"} screen={screen} /> : null;
  };
  const behind = (["l", "r"] as const).filter((k) => arms[k]?.behind);
  const front = (["l", "r"] as const).filter((k) => arms[k] && !arms[k]!.behind);
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      {shadow && (
        <defs>
          <filter id={`sh${id}`} x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx={16} dy={12} stdDeviation={16} floodColor="#1F1712" floodOpacity={0.22} />
          </filter>
        </defs>
      )}
      <g filter={shadow ? `url(#sh${id})` : undefined}>
        <g transform={`translate(300 800) scale(${sx} ${sy}) rotate(${tilt}) translate(-300 -800)`}>
          {torso > 0 && meera && (
            <path
              d={`M20 ${top} C18 ${top + torso * 0.5} 46 ${800 + torso * 0.85} 70 ${800 + torso} L530 ${800 + torso} C554 ${800 + torso * 0.85} 582 ${top + torso * 0.5} 580 ${top} Z`}
              fill={coat}
              stroke={LINE}
              strokeWidth={2.4}
            />
          )}
          {behind.map(arm)}
          {meera ? (
            <MeeraSketch x={0} y={0} s={1} expr={expr as SketchExpr} viseme={viseme} gaze={gaze} turn={turn} blink={typeof blink === "number" ? blink > 0.5 : blink} sway={sway} />
          ) : (
            <Person {...who} x={0} y={0} s={1} expr={expr as PExpr} viseme={viseme} gaze={gaze} turn={turn} blink={blink} armsOut={out} bw={bw} sway={sway} />
          )}
          {torso > 0 && meera && <rect x={24} y={top - 8} width={552} height={20} fill={coat} />}
          {torso > 0 && !meera && <LowerBody who={who} torso={torso} bw={bw} out={out} coat={coat} sleeve={sleeve} skin={skin} legs={legs} />}
          {front.map(arm)}
        </g>
      </g>
    </g>
  );
};

/**
 * The body below a Person's bust for standing shots: shirt to the waist, the resting
 * forearms and hands, then (with `legs`) trousers and shoes. Drawn over the bust's
 * bottom edge so the figure reads as one piece.
 */
const LowerBody: React.FC<{
  who: PersonLook;
  torso: number;
  bw: number;
  out: { l: boolean; r: boolean };
  coat: string;
  sleeve: string;
  skin: string;
  legs?: { pants: string; shoes?: string; walk?: number };
}> = ({ who, torso, bw, out, coat, sleeve, skin, legs }) => {
  const X = (dx: number) => 300 + dx * bw;
  const floor = 800 + torso;
  const waist = legs ? 800 + torso * 0.36 : floor;
  const hem = legs ? waist + (who.outfit === "kurta" ? torso * 0.22 : who.outfit === "hoodie" ? 40 : 0) : floor;
  const edge = (side: -1 | 1) => (out[side < 0 ? "l" : "r"] ? 162 : 218);
  const L = edge(-1);
  const R = edge(1);
  const body = `M${X(-L)} 757 C${X(-L - 4)} ${757 + (hem - 757) * 0.4} ${X(-150)} ${hem - (hem - 757) * 0.3} ${X(-150)} ${hem} L${X(150)} ${hem} C${X(150)} ${hem - (hem - 757) * 0.3} ${X(R + 4)} ${757 + (hem - 757) * 0.4} ${X(R)} 757 Z`;
  const w = legs?.walk ?? 0;
  const leg = (side: -1 | 1) => {
    const swing = Math.sin(w + (side < 0 ? 0 : Math.PI)) * 12;
    const hipX = X(side * 64);
    return (
      <g key={side} transform={`rotate(${swing} ${hipX} ${waist + 20})`}>
        <path
          d={`M${X(side * 140)} ${waist} C${X(side * 136)} ${waist + (floor - waist) * 0.4} ${X(side * 116)} ${floor - 120} ${X(side * 112)} ${floor - 30} L${X(side * 30)} ${floor - 30} C${X(side * 28)} ${floor - 160} ${X(side * 10)} ${waist + 140} ${X(0)} ${waist + 90} Z`}
          fill={legs!.pants}
          stroke={LINE}
          strokeWidth={2.4}
          strokeLinejoin="round"
        />
        <path d={`M${X(side * 70)} ${waist + 120} Q${X(side * 76)} ${floor - 220} ${X(side * 70)} ${floor - 60}`} stroke={LINE} strokeWidth={1.6} fill="none" opacity={0.3} />
        <path
          d={`M${X(side * 118)} ${floor - 34} C${X(side * 122)} ${floor + 6} ${X(side * 40)} ${floor + 14} ${X(side * 6)} ${floor + 4} L${X(side * 8)} ${floor - 34} Z`}
          transform={`translate(${side * 14} 0)`}
          fill={legs!.shoes ?? "#1F2937"}
          stroke={LINE}
          strokeWidth={2.4}
          strokeLinejoin="round"
        />
      </g>
    );
  };
  const forearm = (side: -1 | 1) => {
    if (out[side < 0 ? "l" : "r"]) return null;
    const x0 = X(side * 196);
    const handY = legs ? waist + 40 : Math.min(floor - 40, 757 + 260);
    const swing = legs ? Math.sin(w + (side < 0 ? Math.PI : 0)) * 6 : 0;
    return (
      <g key={side} transform={`rotate(${swing} ${x0} 740)`}>
        <path d={`M${x0 - 30} 740 L${x0 + side * 8 - 26} ${handY - 30} L${x0 + side * 8 + 26} ${handY - 30} L${x0 + 30} 740 Z`} fill={sleeve} stroke={LINE} strokeWidth={2.2} strokeLinejoin="round" />
        <ellipse cx={x0 + side * 8} cy={handY} rx={26} ry={34} fill={skin} stroke={LINE} strokeWidth={2.2} />
        <path d={`M${x0 + side * 8 - side * 16} ${handY - 8} q${-side * 4} 14 ${side * 2} 26`} stroke={LINE} strokeWidth={1.4} fill="none" opacity={0.5} />
      </g>
    );
  };
  return (
    <g>
      {legs && [leg(-1), leg(1)]}
      <path d={body} fill={coat} />
      <path d={`M${X(-L)} 757 C${X(-L - 4)} ${757 + (hem - 757) * 0.4} ${X(-150)} ${hem - (hem - 757) * 0.3} ${X(-150)} ${hem} L${X(150)} ${hem} C${X(150)} ${hem - (hem - 757) * 0.3} ${X(R + 4)} ${757 + (hem - 757) * 0.4} ${X(R)} 757`} fill="none" stroke={LINE} strokeWidth={2.4} />
      <rect x={X(-150)} y={757} width={300 * bw} height={Math.max(0, hem - 757)} fill="#000" opacity={0.05} />
      {who.outfit === "blazer" && (
        <g>
          <path d={`M300 757 L300 ${hem}`} stroke={LINE} strokeWidth={2.2} opacity={0.6} />
          <circle cx={308} cy={790} r={6} fill="#1E293B" stroke={LINE} strokeWidth={1.4} />
        </g>
      )}
      {(who.outfit === "shirt" || who.outfit === "blazer") && legs && <rect x={X(-150)} y={waist - 16} width={300 * bw} height={22} fill="#3F2A1D" stroke={LINE} strokeWidth={1.6} />}
      {who.outfit === "shirt" && <path d={`M300 757 L300 ${waist - 16}`} stroke={LINE} strokeWidth={1.8} opacity={0.5} />}
      {who.outfit === "hoodie" && <path d={`M${X(-110)} ${hem - 150} L${X(110)} ${hem - 150} L${X(90)} ${hem - 40} L${X(-90)} ${hem - 40} Z`} fill="none" stroke={LINE} strokeWidth={1.8} opacity={0.5} />}
      {forearm(-1)}
      {forearm(1)}
    </g>
  );
};

/** A smooth blink (0 open .. 1 closed) about every 3 s, 6 story frames long. */
export const blinkK = (f: number, seed = 0) => {
  const period = 84 + (seed % 5) * 9;
  const p = (((f + seed * 17) % period) + period) % period;
  return p < 6 ? Math.sin((p / 6) * Math.PI) : 0;
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

/** An arm target at an angle (degrees, 0 = right, 90 = down) and a fraction of full reach. */
export const reach = (side: "l" | "r", deg: number, k = 0.92, build: Build = SLIM): Pt => {
  const s = SHOULDER[side];
  const L = (build.upper + build.lower) * k;
  return [s[0] + Math.cos((deg * Math.PI) / 180) * L, s[1] + Math.sin((deg * Math.PI) / 180) * L];
};
