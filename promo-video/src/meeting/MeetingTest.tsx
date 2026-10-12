// Animation test for "Who Said That?" (13.5 s): the rigged cast in motion.
// Rao winds up and slams the table (anticipation, squash & stretch, crash zoom,
// shake); Arjun and Neha point at each other (spring overshoot); Meera raises
// Srutam and plays Rao's own words back. Mouths follow Rhubarb Lip Sync.
import { AbsoluteFill, interpolate, Sequence, spring, useCurrentFrame } from "remotion";
import { clamp, easeIn, easeInOut, easeOut } from "../anim";
import { ARJUN, NEHA, RAO } from "../cast/cast";
import { visemeAt, type Viseme } from "../cast/Mouth";
import { Actor, blinkAt, saccade, type Pt } from "../cast/Rig";
import { AskScreen, Bubble, Burst, Frame, H, LINE, Phone, Sfx, Sweat, W } from "../storyboards/Kit";
import { about, Captions, pop, shake, Soundtrack, type CaptionCue, type Cue, type Line } from "../stories/Story";
import { VISEMES } from "./visemes";

export const TEST_TOTAL = 405;

const V = "voiceover/meeting";
const VO: (Line & { src: string })[] = [
  { file: "rao-who", at: 26, frames: 117, src: `${V}/rao/rao-who.wav` },
  { file: "arjun-her", at: 162, frames: 21, src: `${V}/arjun/arjun-her.wav` },
  { file: "neha-him", at: 180, frames: 22, src: `${V}/neha/neha-him.wav` },
  { file: "m-ask", at: 236, frames: 89, src: `${V}/meera/m-ask.mp3` },
  { file: "rao-recording", at: 334, frames: 75, src: `${V}/rao/rao-recording.wav` },
];
const SFX: Cue[] = [
  { file: "meeting/bam.wav", at: 18, volume: 1 },
  { file: "meeting/sting.wav", at: 19, volume: 0.6, dur: 70 },
  { file: "sfx/whoosh.wav", at: 145, volume: 0.5 },
  { file: "meeting/boing.wav", at: 160, volume: 0.5 },
  { file: "meeting/boing-hi.wav", at: 178, volume: 0.5 },
  { file: "meeting/scratch.wav", at: 222, volume: 0.45 },
  { file: "sfx/whoosh.wav", at: 250, volume: 0.3 },
  { file: "sfx/pop.wav", at: 282, volume: 0.5 },
  { file: "sfx/pop.wav", at: 324, volume: 0.45 },
  { file: "sfx/tap.wav", at: 330, volume: 0.6 },
];
const CAPTIONS: CaptionCue[] = [
  { from: 26, to: 143, text: "“The 14th? Who said yes to the 14th?!”", top: true },
  { from: 236, to: 325, text: "“Relax. Let's ask Srutam.”" },
  { from: 334, to: 404, text: "▶ Rao (recording): “Fine. The 14th, final.”" },
];

const vis = (file: string, f: number, rest: Viseme = "X") => {
  const v = visemeAt(VISEMES, file, VO.find((l) => l.file === file)!.at, f);
  return v === "X" ? rest : v;
};
const sp = (f: number, at: number, damping = 10, stiffness = 160) => spring({ frame: f - at, fps: 30, config: { damping, stiffness } });
const lerpPt = (a: Pt, b: Pt, k: number): Pt => [a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k];

const Wall: React.FC<{ tint?: string }> = ({ tint = "#DCE3EC" }) => (
  <g>
    <rect width={W} height={H} fill={tint} />
    <rect x={660} y={150} width={360} height={520} fill="#BFDBFE" stroke={LINE} strokeWidth={8} />
    {[0, 1, 2, 3].map((i) => (
      <rect key={i} x={690 + i * 82} y={420 - i * 40} width={60} height={250 + i * 40} fill="#94A3B8" />
    ))}
    <path d="M840 150 L840 670 M660 410 L1020 410" stroke={LINE} strokeWidth={6} />
  </g>
);

/* Shot A: Rao winds up, slams the table, crash zoom, roars. */
const ShotA: React.FC = () => {
  const f = useCurrentFrame();
  const impact = 18;
  const rest: Pt = [520, 1010];
  const up: Pt = [560, 300];
  const down: Pt = [520, 980];
  const hand =
    f < impact - 4
      ? lerpPt(rest, up, interpolate(f, [0, 12], [0, 1], { ...clamp, easing: easeOut }))
      : lerpPt(up, down, interpolate(f, [impact - 4, impact], [0, 1], { ...clamp, easing: easeIn }));
  const squash = f < impact ? interpolate(f, [0, 12], [1, 1.05], clamp) : 1 - 0.08 * (1 - sp(f, impact, 7, 220));
  const zoom = interpolate(f, [impact - 4, impact + 6], [1, 1.22], { ...clamp, easing: easeOut });
  const dx = shake(f, [impact, impact + 3, impact + 7], 22);
  const dy = shake(f, [impact + 1, impact + 5], 14);
  const after = f >= impact;
  const talking = vis("rao-who", f, "A") !== "A";
  const tilt = 3 + (talking ? Math.sin(f / 2.6) * 2.5 : 0);
  const anger = after ? 1 + 0.15 * Math.sin(f / 3) : 0;
  const cupJump = after ? Math.max(0, Math.sin(((f - impact) / 10) * Math.PI)) * 50 * (f < impact + 10 ? 1 : 0) : 0;
  return (
    <Frame bg={after ? ["#FEE2E2", "#FCA5A5"] : ["#CBD5E1", "#94A3B8"]}>
      {after &&
        Array.from({ length: 14 }, (_, i) => {
          const a = (i / 14) * Math.PI * 2 + f * 0.004;
          return <path key={i} d={`M540 860 L${540 + Math.cos(a) * 1600} ${860 + Math.sin(a) * 1600}`} stroke="#F87171" strokeWidth={70} opacity={0.3} />;
        })}
      {!after && <Wall tint="#CBD5E1" />}
      <g transform={`translate(${dx} ${dy}) ${about(540, 1100, zoom)}`}>
        <Actor
          x={20}
          y={420}
          s={1.4}
          who={RAO}
          f={f}
          expr="angry"
          viseme={after ? vis("rao-who", f, "A") : "A"}
          tilt={tilt}
          squash={squash}
          gaze={saccade(f, 3)}
          blink={!after && blinkAt(f, 2)}
          arms={{ r: { to: hand, pose: "fist", rot: f < impact - 4 ? -60 : 70 } }}
        />
        <rect x={-60} y={1690} width={W + 120} height={300} fill="#6B4F3A" stroke={LINE} strokeWidth={6} />
        <g transform={`translate(160 ${1610 - cupJump}) rotate(${cupJump / 4})`}>
          <path d="M0 0 L70 0 L62 80 L8 80 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={5} />
        </g>
        {after && (
          <g transform={about(830, 1690, pop(f, impact, 8))}>
            <Burst x={830} y={1690} r={90} color="#2B1E1B" n={10} />
            <Sfx x={820} y={1600} text="BAM!" size={130} rot={-8} />
          </g>
        )}
        {after && (
          <g transform={`translate(860 560) scale(${anger})`} stroke="#DC2626" strokeWidth={16} strokeLinecap="round" fill="none">
            <path d="M-40 -10 Q-10 -10 -10 -40 M40 -10 Q10 -10 10 -40 M-40 10 Q-10 10 -10 40 M40 10 Q10 10 10 40" />
          </g>
        )}
      </g>
    </Frame>
  );
};

/* Shot B: "Her!" "Him!" */
const ShotB: React.FC = () => {
  const f = useCurrentFrame() + 146;
  const a = sp(f, 158, 9, 170);
  const n = sp(f, 176, 9, 170);
  const arjunHand = lerpPt([470, 1010], [700, 600], a);
  const nehaHand = lerpPt([130, 1010], [-100, 600], n);
  const push = interpolate(f, [146, 228], [1, 1.06], { ...clamp, easing: easeInOut });
  const drop = (seed: number) => ((f + seed) % 24) * 5;
  return (
    <Frame bg={["#FFE4E6", "#FECDD3"]}>
      <g transform={about(540, 1000, push)}>
        <Actor x={-220} y={700} s={1.0} who={ARJUN} f={f} expr="worried" viseme={vis("arjun-her", f)} turn={0.1 + 0.25 * a} tilt={4 * a} gaze={[0.3 + 0.7 * a, 0]} blink={blinkAt(f, 4)} arms={a > 0.05 ? { r: { to: arjunHand, pose: a > 0.2 ? "point" : "fist" } } : {}} />
        <Actor x={480} y={700} s={1.0} who={NEHA} f={f} expr="angry" viseme={vis("neha-him", f, "A")} turn={-0.1 - 0.25 * n} tilt={-4 * n} gaze={[-0.3 - 0.7 * n, 0]} blink={blinkAt(f, 9)} arms={n > 0.05 ? { l: { to: nehaHand, pose: n > 0.2 ? "point" : "fist" } } : {}} />
        {f > 166 && (
          <g transform={`translate(0 ${drop(0)})`} opacity={1 - drop(0) / 120}>
            <Sweat x={150} y={760} s={1.3} />
          </g>
        )}
        {f > 184 && (
          <g transform={`translate(0 ${drop(9)})`} opacity={1 - drop(9) / 120}>
            <Sweat x={900} y={760} s={1.3} />
          </g>
        )}
      </g>
      <g transform={about(200, 330, pop(f, 162, 9))} opacity={f >= 162 ? 1 : 0}>
        <Bubble x={40} y={260} w={330} lines={["Her!"]} tail={[180, 640]} size={80} />
      </g>
      <g transform={about(860, 330, pop(f, 180, 9))} opacity={f >= 180 ? 1 : 0}>
        <Bubble x={700} y={260} w={330} lines={["Him!"]} tail={[880, 640]} size={80} />
      </g>
    </Frame>
  );
};

const Q = "Who decided on the 14th?";
const A = "In Tuesday's sync, Mr. Rao said: “Fine. The 14th, final.”";

/* Shot C: Meera raises Srutam; Rao hears himself. */
const ShotC: React.FC = () => {
  const f = useCurrentFrame() + 228;
  const raise = sp(f, 250, 9, 120);
  // She lifts the phone itself: its bottom sits in her hand, wherever the arm puts it.
  const hand = lerpPt([560, 1060], [713, 563], raise);
  const handScreen: Pt = [-250 + hand[0] * 1.15, 760 + hand[1] * 1.15];
  const PW = 380;
  const card = sp(f, 282, 11, 150);
  const qn = Math.round(interpolate(f, [284, 300], [0, Q.length], clamp));
  const an = Math.round(interpolate(f, [300, 322], [0, A.length], clamp));
  const playing = f >= 334 && f < 410;
  const shrink = 1 - 0.05 * interpolate(f, [340, 370], [0, 1], { ...clamp, easing: easeOut });
  const raoExpr = f < 334 ? "worried" : f < 350 ? "surprised" : "worried";
  return (
    <Frame bg={["#DCE3EC", "#C7D2E0"]}>
      <Wall />
      <g transform={`rotate(${(1 - raise) * 8} ${handScreen[0]} ${handScreen[1]})`}>
        <Phone x={handScreen[0] - PW / 2} y={handScreen[1] - PW * (2400 / 1080) + 40} w={PW} glow={playing ? "#FBBF24" : card > 0.5 ? "#3B82F6" : undefined}>
          <AskScreen w={PW} q={Q.slice(0, qn)} a={A.slice(0, an)} refs={f >= 324 ? ["Tuesday sync"] : []} pressed={f >= 330 && f < 338} />
        </Phone>
      </g>
      <Actor
        x={-250}
        y={760}
        s={1.15}
        who="meera"
        f={f}
        expr="smug"
        viseme={vis("m-ask", f)}
        turn={0.3}
        gaze={f < 282 ? [0.6, 0] : [0.8, -0.5]}
        blink={blinkAt(f, 1)}
        arms={{ r: { to: hand, pose: "grip", upright: true } }}
      />
      <Actor x={600} y={1260} s={0.62} who={RAO} f={f} expr={raoExpr} squash={shrink} gaze={[-0.8, -0.4]} blink={blinkAt(f, 6)} />
      {f > 345 && <Sweat x={980} y={1450 + ((f - 345) % 20) * 4} />}
      <rect width={W} height={H} fill="#000" opacity={interpolate(f, [396, 405], [0, 1], clamp)} />
    </Frame>
  );
};

export const MeetingTest: React.FC = () => {
  const f = useCurrentFrame();
  // Whip pan A -> B: A slides out left, B in from the right, with motion blur.
  const whip = interpolate(f, [146, 156], [0, 1], { ...clamp, easing: easeInOut });
  const blur = Math.sin(whip * Math.PI) * 26;
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <Sequence name="A Rao slams" durationInFrames={156}>
        <AbsoluteFill style={{ translate: `${-whip * W}px 0px`, filter: `blur(${blur}px)` }}>
          <ShotA />
        </AbsoluteFill>
      </Sequence>
      <Sequence name="B Her / Him" from={146} durationInFrames={82}>
        <AbsoluteFill style={{ translate: `${(1 - whip) * W}px 0px`, filter: `blur(${blur}px)` }}>
          <ShotB />
        </AbsoluteFill>
      </Sequence>
      <Sequence name="C Let's ask" from={228} durationInFrames={TEST_TOTAL - 228}>
        <ShotC />
      </Sequence>
      <Captions cues={CAPTIONS} />
      <Soundtrack music="meeting/test-music.wav" musicVolume={0.5} lines={VO} sfx={SFX} />
    </AbsoluteFill>
  );
};
