// "2 a.m. Before the Exam" restaged for 16:9 with English dialogue. Same story, cast and
// beats as src/exam/ExamVideo.tsx, laid out for a 1920 x 1080 frame: Meera's face beside
// her phone in the chat shots, the whole classroom in the flashback, the lecture
// replaying as a sepia memory beside her while it plays, the door and the room in one
// wide shot, the trio and the Insights card side by side, and the band under the laugh.
import { useId } from "react";
import { AbsoluteFill, interpolate, Sequence } from "remotion";
import { clamp, easeInOut, easeOut } from "../../anim";
import { Kulhad, Zzz } from "../../bedtime/Props";
import { KABIR, PROF, RIYA } from "../../cast/cast";
import { Actor, blinkK, Hand, reach, type Pt } from "../../cast/Rig";
import { Camera, Depth, Grade, LightLeak } from "../../stories/Camera";
import { HandsOnPhone } from "../../stories/Hands";
import { damped, flutter, follow, spin, springAt, springPt } from "../../stories/physics";
import { Soundtrack } from "../../stories/Story";
import { K, useT } from "../../stories/time";
import { thumbs, typed } from "../../stories/typing";
import { voice } from "../../stories/voice";
import { AskFlow, Board, Books, ChatScreen, Frame, GripHand, InsightsScreen, Lamp, LINE, LockNotifs, NoteDetail, NotificationShade, Phone, Window } from "../../storyboards/Kit";
import { Landscape, LCaptions, LH, LW } from "../kit";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";
import { VISEMES } from "./visemes";

const { vis, talkTilt, emph } = voice(VO, VISEMES);
const span = (sc: { from: number; duration: number }): [number, number] => [sc.from, sc.from + sc.duration];
const NIGHT: [string, string] = ["#0B1033", "#1E1B4B"];
const SEPIA: [string, string] = ["#E9D8B4", "#C9AE82"];

const mix = (a: string, b: string, k: number) => {
  const p = (h: string) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const [x, y] = [p(a), p(b)];
  return `#${x.map((v, i) => Math.round(v + (y[i] - v) * k).toString(16).padStart(2, "0")).join("")}`;
};

/** Eyes moving between targets, eased (no snapping), a new one every ~0.4 s. */
const dart = (f: number, seed = 0): [number, number] => {
  const at = (k: number): [number, number] => [Math.sin(k * 12.9898 + seed) * 0.6, -0.15 + Math.cos(k * 7.233 + seed) * 0.25];
  const step = (f + seed * 7) / 12;
  const k = Math.floor(step);
  const u = Math.min(1, (step - k) * 3);
  const e = u * u * (3 - 2 * u);
  const [a, b] = [at(k), at(k + 1)];
  return [a[0] + (b[0] - a[0]) * e, a[1] + (b[1] - a[1]) * e];
};

/** A desk or table across the whole frame from `y` down. */
const DeskL: React.FC<{ y: number; color?: string; top?: string; x0?: number; x1?: number }> = ({ y, color = "#3F2A1D", top = "#5B3A22", x0 = -40, x1 = LW + 40 }) => (
  <g>
    <rect x={x0} y={y} width={x1 - x0} height={LH - y + 60} fill={color} />
    <rect x={x0} y={y} width={x1 - x0} height={36} fill={top} stroke={LINE} strokeWidth={5} />
  </g>
);

/** The hostel room at night across the full width (dawn 0..1 warms the sky for the time-lapse). */
const RoomL: React.FC<{ dawn?: number; f: number }> = ({ dawn = 0, f }) => {
  const sky: [string, string] = [mix("#0B1033", "#F9A8D4", dawn * 0.6), mix("#1E1B4B", "#FDBA74", dawn * 0.7)];
  return (
    <g>
      <rect width={LW} height={LH} fill="#2A2560" />
      <rect y={900} width={LW} height={LH - 900} fill="#1F1B47" />
      <Window x={110} y={110} w={420} h={470} sky={sky} moon={dawn < 0.5} />
      {[
        [160, 170],
        [270, 240],
        [200, 330],
        [460, 500],
        [330, 160],
        [470, 300],
      ].map(([x, y], i) => (
        <circle key={i} cx={x} cy={y} r={3 + (i % 2)} fill="#FEF3C7" opacity={(0.5 + 0.5 * Math.sin(f / 9 + i * 2)) * (1 - dawn)} />
      ))}
      {/* fairy lights strung across the wall */}
      <path d="M600 90 Q900 190 1200 100 Q1450 40 1780 120" stroke="#1E1B4B" strokeWidth={3} fill="none" />
      {Array.from({ length: 16 }, (_, i) => {
        const u = i / 15;
        const x = 600 + u * 1180;
        const y = u < 0.5 ? 90 + Math.sin(u * 2 * Math.PI) * 70 + u * 20 : 100 + Math.sin((u - 0.5) * 2 * Math.PI) * -50 + (u - 0.5) * 40;
        return <circle key={i} cx={x} cy={y + 8} r={7} fill={["#FDE68A", "#F9A8D4", "#A5F3FC"][i % 3]} opacity={0.55 + 0.35 * Math.sin(f / 11 + i)} />;
      })}
      {/* study notes pinned on the wall */}
      {[0, 1, 2, 3, 4, 5].map((i) => (
        <rect
          key={i}
          x={1240 + (i % 3) * 150}
          y={200 + Math.floor(i / 3) * 180}
          width={110}
          height={140}
          rx={8}
          fill={["#F472B6", "#60A5FA", "#FBBF24", "#34D399", "#A78BFA", "#FB923C"][i]}
          opacity={0.45}
          transform={`rotate(${i * 5 - 10} ${1295 + (i % 3) * 150} ${270 + Math.floor(i / 3) * 180})`}
        />
      ))}
      <rect x={1200} y={600} width={520} height={18} fill="#4C3B8F" />
    </g>
  );
};

/** A desk clock (red LED). */
const Clock: React.FC<{ x: number; y: number; text: string; color?: string; s?: number }> = ({ x, y, text, color = "#EF4444", s = 1 }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    <rect x={-150} y={-62} width={300} height={92} rx={18} fill="#0F172A" stroke={LINE} strokeWidth={5} />
    <text x={-22} y={8} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={62} fill={color}>
      {text}
    </text>
    <text x={96} y={6} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={24} fill={color}>
      AM
    </text>
  </g>
);

/** Flashback look over the whole frame: a ripple that settles (k 0 -> 1) and a warm wash. */
const Flashback: React.FC<{ k?: number; children: React.ReactNode }> = ({ k = 1, children }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const scale = 90 * (1 - k);
  return (
    <g>
      <defs>
        <filter id={`rip${id}`} x="-5%" y="-5%" width="110%" height="110%">
          <feTurbulence type="turbulence" baseFrequency="0.004 0.02" numOctaves={2} seed={4} />
          <feDisplacementMap in="SourceGraphic" scale={scale} xChannelSelector="R" yChannelSelector="G" />
        </filter>
      </defs>
      <g filter={scale > 0.5 ? `url(#rip${id})` : undefined}>{children}</g>
      <rect width={LW} height={LH} fill="#B45309" opacity={0.16} style={{ mixBlendMode: "multiply" }} />
      {k < 1 && <rect width={LW} height={LH} fill="#FFF7ED" opacity={(1 - k) * 0.5} />}
    </g>
  );
};

/** A phone lying on a desk, seen at an angle; recording, or lighting up for a message. */
const DeskPhone: React.FC<{ x: number; y: number; s?: number; f: number; rec?: boolean; lit?: number }> = ({ x, y, s = 1, f, rec = true, lit = 0 }) => {
  const dot = 0.5 + 0.5 * Math.sin(f / 6);
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      {lit > 0 && <ellipse cx={0} cy={-10} rx={160} ry={60} fill="#93C5FD" opacity={0.25 * lit} />}
      <path d="M-70 -18 L70 -18 L84 18 L-84 18 Z" fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
      <path d="M-62 -13 L62 -13 L72 13 L-72 13 Z" fill={lit > 0 ? mix("#1E2A5A", "#DBEAFE", lit) : "#1E2A5A"} />
      {rec && <circle cx={-40} cy={0} r={7} fill="#EF4444" opacity={0.4 + 0.6 * dot} />}
      {rec && <circle cx={-40} cy={0} r={18} fill="#EF4444" opacity={0.2 * dot} />}
    </g>
  );
};

/** Two hands holding a phone up in front of us, at x. */
const HeldPhone: React.FC<{ f: number; x: number; y: number; w: number; glow?: string; children: React.ReactNode }> = ({ f, x, y, w, glow, children }) => {
  const h = w * (2400 / 1080);
  const breathe = Math.sin(f / 22) * 3;
  const s = w / 680;
  return (
    <g transform={`translate(0 ${breathe})`}>
      <Phone x={x} y={y} w={w} rot={-1.5} glow={glow}>
        {children}
      </Phone>
      <GripHand x={x + 20 * s} y={y + h - 120 * s} s={1.5 * s} rot={-12} />
      <GripHand x={x + w - 20 * s} y={y + h - 120 * s} s={1.5 * s} rot={12} />
    </g>
  );
};

/** A big chat phone on the right, held from below the frame; the screen's content is fitted
    to the part that's in frame, so the newest messages and the input bar stay visible. */
const ChatPhone: React.FC<{ f: number; children: React.ReactNode }> = ({ f, children }) => {
  const [x, y, w] = [1150, 40, 600];
  const breathe = Math.sin(f / 22) * 3;
  return (
    <g transform={`translate(0 ${breathe})`}>
      <Phone x={x} y={y} w={w} rot={-1.5}>
        <div style={{ position: "absolute", left: 0, top: 0, width: w, height: LH - y - 44 }}>{children}</div>
      </Phone>
      <GripHand x={x + 40} y={1010} s={1.1} rot={-14} />
      <GripHand x={x + w - 20} y={1000} s={1.1} rot={10} />
    </g>
  );
};

/** Blue light from a phone screen on a face (screen blend). */
const ScreenGlow: React.FC<{ cx: number; cy: number; rx: number; ry: number; k?: number; color?: string }> = ({ cx, cy, rx, ry, k = 1, color = "#BFDBFE" }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <g>
      <defs>
        <radialGradient id={`sg${id}`}>
          <stop offset="0%" stopColor={color} stopOpacity={0.42 * k} />
          <stop offset="100%" stopColor={color} stopOpacity={0} />
        </radialGradient>
      </defs>
      <ellipse cx={cx} cy={cy} rx={rx} ry={ry} fill={`url(#sg${id})`} style={{ mixBlendMode: "screen" }} />
    </g>
  );
};

const CHAT = [
  { from: "Riya", text: "What's coming in Econ tomorrow??", color: "#DB2777" },
  { from: "Kabir", text: "no clue, honestly", color: "#16A34A" },
  { from: "Riya", text: "Meera, you were in class, right?", color: "#DB2777" },
  { from: "Me", text: "Ch 4 + Ch 7 + the metro case study (20 marks). Straight from the lecture recording.", me: true },
  { from: "Riya", text: "WHAT. you legend", color: "#DB2777" },
  { from: "Kabir", text: "coming over. bringing chai", color: "#16A34A" },
];

/* 1. 2:07 am. ECU on the desk clock, pull out to the room: "Exam at nine. And I forgot what Prof said." */
const Night: React.FC = () => {
  const f = useT() + SCENES.night.from;
  const k = interpolate(f, [6, 80], [0, 1], { ...clamp, easing: easeInOut });
  const clockAt: Pt = [400, 880];
  const zoom = 3.2 + (1 - 3.2) * k;
  const center: Pt = [LW / 2 + (clockAt[0] - LW / 2) * k, LH / 2 + (clockAt[1] - LH / 2) * k];
  const M = { x: 560, y: 110, s: 1 };
  const lArm = springPt(f, [40, 800], [{ at: BEAT.handHead, to: [215, 345] }], 14, 100);
  const rArm = springPt(f, [580, 820], [{ at: BEAT.buzz + 4, to: [660, 780] }], 14, 140);
  const flicker = 0.94 + 0.06 * Math.sin(f * 0.9) * Math.sin(f * 0.37);
  const lit = interpolate(f, [BEAT.buzz, BEAT.buzz + 4], [0, 1], clamp);
  const buzz = f > BEAT.buzz && f < BEAT.buzz + 10 ? Math.sin(f * 9) * 3 : 0;
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={zoom} focus={clockAt} center={center} drift={3} seed={2}>
        <Depth blur={2}>
          <RoomL f={f} />
        </Depth>
        <g opacity={flicker}>
          <Lamp x={1180} y={330} />
        </g>
        <Actor
          x={M.x}
          y={M.y}
          s={M.s}
          f={f}
          who="meera"
          shadow
          expr={f < BEAT.buzz + 6 ? "worried" : "surprised"}
          viseme={vis("e-forgot", f)}
          tilt={talkTilt(["e-forgot"], f, f > BEAT.handHead ? -4 : 0, 1.4)}
          sway={follow((g) => talkTilt(["e-forgot"], g, 0, 1.4), f, 4, 0.3)}
          gaze={f < BEAT.buzz ? dart(f, 1) : [0.8, 0.8]}
          blink={blinkK(f, 1)}
          arms={{ l: { to: lArm, pose: "open", rot: f > BEAT.handHead - 6 ? 20 : 0 }, r: { to: rArm, pose: "open" } }}
        />
        <DeskL y={850} />
        <path d="M640 884 L1040 884 L1012 852 L668 852 Z" fill="#FFFDF8" stroke={LINE} strokeWidth={3} />
        <path d="M700 866 h120 M860 866 h110" stroke="#94A3B8" strokeWidth={3} />
        <Books x={1520} y={862} n={6} />
        <g transform={`translate(${1250 + buzz} 900)`}>
          <DeskPhone x={0} y={0} s={0.9} f={f} rec={false} lit={lit} />
        </g>
        <Clock x={clockAt[0]} y={clockAt[1]} text={f % 30 < 15 ? "2:07" : "2 07"} />
      </Camera>
      <Grade warm="#FCD34D" cool="#1E1B4B" from="right" amount={0.2} />
    </Frame>
  );
};

/** Meera in close-up on the left of the frame, lit by the phone she's reading (below frame). */
const ReadingMeera: React.FC<{ f: number; expr: Parameters<typeof Actor>[0]["expr"]; gaze?: [number, number]; tilt?: number; k?: number }> = ({ f, expr, gaze = [0.8, 0.7], tilt = 3, k = 1 }) => (
  <g>
    <Actor x={150} y={170} s={1.15} f={f} who="meera" shadow expr={expr} tilt={tilt} gaze={gaze} blink={blinkK(f, 1)} sway={Math.sin(f / 30) * 0.4} />
    <ScreenGlow cx={820} cy={760} rx={560} ry={440} k={k} />
  </g>
);

/* 2. The group chat: "What's coming in Econ tomorrow??" ... "Meera, you were in class, right?" */
const Chat: React.FC = () => {
  const f = useT() + SCENES.chat.from;
  const appear = BEAT.msg.map((at) => springAt(f, 0, [{ at, to: 1 }], 14, 170));
  const lift = springAt(f, 0, [{ at: 132, to: 1 }], 14, 110);
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.chat), [1, 1.04], clamp)} focus={[960, 540]} drift={3} seed={3}>
        <Depth blur={9}>
          <RoomL f={f} />
          <Lamp x={1180} y={330} />
        </Depth>
        <ReadingMeera f={f} expr={f < BEAT.msg[2] + 4 ? "worried" : "surprised"} gaze={f < BEAT.msg[2] + 4 ? [0.8, 0.7] : [0.9, 0.5]} />
        <g transform={`translate(0 ${(1 - lift) * 400})`}>
          <ChatPhone f={f}>
            <ChatScreen w={600} title="Econ gang" members="Riya, Kabir, you" messages={CHAT.slice(0, 3)} appear={appear} typing={f > 176 && f < BEAT.msg[2] ? "Riya" : undefined} />
          </ChatPhone>
        </g>
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="right" amount={0.14} />
    </Frame>
  );
};

/* 3. She was asleep in that lecture too. Then it dawns on her: Srutam wasn't. */
const Guilt: React.FC = () => {
  const f = useT() + SCENES.guilt.from;
  const dawn = f >= BEAT.realise;
  const up = springAt(f, 0, [{ at: BEAT.realise, to: 1 }], 12, 160);
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.guilt), [1.02, 1.1], { ...clamp, easing: easeInOut })} focus={[920, 520]} drift={3} seed={5}>
        <Depth blur={8}>
          <RoomL f={f} />
        </Depth>
        <Actor
          x={560}
          y={120}
          s={1.2}
          f={f}
          who="meera"
          shadow
          expr={dawn ? "smug" : "worried"}
          tilt={-3 + up * 4}
          gaze={dawn ? [0.1, -0.1] : [-0.6, -0.7]}
          blink={blinkK(f, 2)}
          squash={1 + up * 0.02}
          screen={<rect x={-38} y={-84} width={76} height={164} rx={9} fill="#0C1225" />}
          arms={{ r: { to: [540, 600], pose: "phone", upright: true } }}
        />
        <ScreenGlow cx={1200} cy={760} rx={420} ry={440} k={0.8} />
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="right" amount={0.16} />
    </Frame>
  );
};

const BOARD = ["Exam: Thursday", "Ch. 4, Ch. 7", "+ metro case (20)"];

/** The classroom wall: chalkboard on the left half, a door and a notice board on the right. */
const ClassWall: React.FC<{ lines?: string[] }> = ({ lines = BOARD }) => (
  <g>
    <rect width={LW} height={LH} fill="#E2CFA8" />
    <Board x={100} y={70} w={980} h={430} chalk lines={lines} />
    <rect x={1640} y={150} width={220} height={560} fill="#B08155" stroke={LINE} strokeWidth={6} />
    <circle cx={1670} cy={440} r={10} fill="#FBBF24" stroke={LINE} strokeWidth={3} />
    <rect x={1150} y={110} width={300} height={190} fill="#C9976A" stroke={LINE} strokeWidth={6} />
    {[0, 1, 2].map((i) => (
      <rect key={i} x={1175 + i * 92} y={135 + (i % 2) * 20} width={72} height={90} fill={["#FEF9C3", "#DBEAFE", "#FCE7F3"][i]} transform={`rotate(${i * 4 - 4} ${1210 + i * 92} 180)`} />
    ))}
    <rect y={760} width={LW} height={LH - 760} fill="#CDB68C" />
  </g>
);

/* 4. Monday, 9:00, sepia: before the lecture starts, Meera, yawning, pulls down the shade
   and taps Start on Srutam's notification. Prof is at the board behind her phone. */
const Start: React.FC = () => {
  const f = useT() + SCENES.start.from;
  const k = interpolate(f, [BEAT.ripple, BEAT.ripple + 22], [0, 1], { ...clamp, easing: easeOut });
  const PX = 1120;
  const PY = 40;
  const PW = 580;
  const kk = PW / 470;
  const startBtn: Pt = [PW - (22 + 60) * kk, (70 + 28 + 10 + 34 + 23) * kk + 18 * kk];
  const point: Pt | null = f >= BEAT.startTap - 12 && f < BEAT.startTap + 10 ? [startBtn[0], startBtn[1] + Math.max(0, BEAT.startTap - f) * 12] : null;
  const rec = f >= BEAT.startTap + 2;
  const tapK = interpolate(f, [BEAT.startTap, BEAT.startTap + 12], [0, 1], clamp);
  return (
    <Frame bg={SEPIA}>
      <Flashback k={k}>
        <Camera f={f} zoom={interpolate(f, span(SCENES.start), [1, 1.04], clamp)} focus={[960, 540]} drift={2} seed={5}>
          <Depth blur={8}>
            <ClassWall />
            <Actor x={640} y={90} s={0.6} f={f} who={PROF} torso={620} expr="talk" viseme={vis("prof-lecture", f)} turn={0.2} gaze={[0.3, 0.2]} />
            <DeskL y={880} color="#7C5134" top="#A9744D" />
          </Depth>
          <HandsOnPhone x={PX} y={PY} w={PW} point={point}>
            <NotificationShade w={PW} recording={rec} pressed={f >= BEAT.startTap - 2 && f < BEAT.startTap + 4} secs={rec ? Math.floor((f - BEAT.startTap) / 30) + 1 : 0} time="9:00" date="Mon, 4 March" other={{ app: "Calendar · now", title: "Econ lecture", text: "9:00 – 10:00 · Room 204" }} />
          </HandsOnPhone>
          {tapK > 0 && tapK < 1 && <circle cx={PX + startBtn[0]} cy={PY + startBtn[1]} r={14 + tapK * 60} fill="none" stroke="#FFFFFF" strokeWidth={6} opacity={1 - tapK} />}
        </Camera>
      </Flashback>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.18} />
    </Frame>
  );
};

/* 5. Monday, sepia, half-heard: Prof talks at the board, the class dozes, Meera too. Her phone records. */
const Lecture: React.FC = () => {
  const f = useT() + SCENES.lecture.from;
  const P = { x: 1080, y: 70, s: 0.6 };
  const hand = springPt(
    f,
    reach("l", 150, 0.8),
    [
      { at: BEAT.chalk - 10, to: reach("l", 205, 0.98) },
      { at: BEAT.chalk, to: reach("l", 210, 1.0) },
      { at: BEAT.chalk + 5, to: reach("l", 205, 0.97) },
      { at: BEAT.chalk + 40, to: reach("l", 150, 0.8) },
    ],
    12,
    200,
  );
  const hw: Pt = [P.x + hand[0] * P.s, P.y + hand[1] * P.s];
  const chalking = f > BEAT.chalk - 12 && f < BEAT.chalk + 40;
  const tip: Pt = [hw[0] - 70, hw[1] - 40 + damped(f, BEAT.chalk, 4, 4, 6)];
  const doze = (seed: number) => {
    const p = ((f + seed) % 80) / 80;
    return p < 0.85 ? p * 9 : 9 - (p - 0.85) * 60;
  };
  const turn = chalking ? -0.45 : -0.1;
  return (
    <Frame bg={SEPIA}>
      <Flashback>
        <Camera f={f} zoom={interpolate(f, span(SCENES.lecture), [1.03, 1.08], clamp)} focus={[960, 560]} drift={3} seed={6}>
          <Depth blur={1.5}>
            <ClassWall />
            {chalking && <path d={`M${hw[0]} ${hw[1]} L${tip[0]} ${tip[1]}`} stroke="#F8FAFC" strokeWidth={9} strokeLinecap="round" />}
            <Actor
              x={P.x}
              y={P.y}
              s={P.s}
              f={f}
              who={PROF}
              torso={620}
              expr="talk"
              viseme={vis("prof-lecture", f, (["C", "B", "E", "B", "A", "C"] as const)[Math.floor(f / 4) % 6])}
              turn={turn}
              gaze={chalking ? [-0.9, -0.4] : [0.2, 0.3]}
              tilt={talkTilt(["prof-lecture"], f, -3, 1.2)}
              blink={blinkK(f, 4)}
              arms={{ l: { to: hand, pose: chalking ? "grip" : "open", rot: 20 } }}
            />
          </Depth>
          <DeskL y={830} color="#7C5134" top="#A9744D" />
          <Actor x={-30} y={340} s={0.62} f={f} who={KABIR} shadow expr="sleepy" tilt={-12 + Math.sin(f / 30) * 2} />
          <Actor x={520} y={360} s={0.6} f={f} who="meera" shadow expr="drowsy" tilt={doze(0)} sway={-doze(0) * 0.05} />
          <Actor x={1480} y={340} s={0.62} f={f} who={RIYA} shadow expr="sleepy" tilt={10 + Math.sin(f / 25) * 2} sway={Math.sin(f / 25) * 0.3} />
          <Zzz x={180} y={450} t={f} />
          <Zzz x={1640} y={450} t={f + 17} />
          <DeskPhone x={880} y={880} s={1.1} f={f} />
          <path d="M1060 900 L1300 900 L1280 862 L1080 862 Z" fill="#FFFDF8" stroke={LINE} strokeWidth={3} />
          <path d="M1090 880 q20 -14 40 0 t40 0 t40 0" stroke="#94A3B8" strokeWidth={3} fill="none" />
        </Camera>
      </Flashback>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.18} />
    </Frame>
  );
};

/* 5b. Insert: on her desk, Srutam's ongoing recording notification, at 41:0x. */
const Insert: React.FC = () => {
  const f = useT() + SCENES.insert.from;
  const secs = 3 + Math.floor((f - SCENES.insert.from) / 30);
  return (
    <Frame bg={["#7C5134", "#5B3A22"]}>
      <Flashback>
        <Camera f={f} zoom={interpolate(f, span(SCENES.insert), [1, 1.08], { ...clamp, easing: easeInOut })} focus={[960, 420]} drift={2} seed={8}>
          <rect width={LW} height={LH} fill="#8B5E3C" />
          {Array.from({ length: 7 }, (_, i) => (
            <path key={i} d={`M-40 ${80 + i * 170} C600 ${60 + i * 170} 1300 ${110 + i * 170} 1960 ${90 + i * 170}`} stroke="#6B4528" strokeWidth={5} fill="none" opacity={0.35} />
          ))}
          {/* her notebook: notes that trail off into a doodle */}
          <g transform="rotate(-6 380 560)">
            <rect x={120} y={260} width={520} height={640} rx={8} fill="#FFFDF8" stroke={LINE} strokeWidth={5} />
            {Array.from({ length: 9 }, (_, i) => (
              <path key={i} d={`M150 ${330 + i * 62} H610`} stroke="#BFDBFE" strokeWidth={3} />
            ))}
            <text x={160} y={320} fontFamily="Kalam" fontWeight={700} fontSize={40} fill="#1E3A8A">
              <tspan x={160}>Econ, 4 March</tspan>
              <tspan x={160} dy={62}>
                Ch. 4: metro fares
              </tspan>
              <tspan x={160} dy={62}>
                {"demand curve, w"}
              </tspan>
            </text>
            <path d="M380 450 q30 40 10 80 q-20 40 20 80 q40 30 10 70" stroke="#1E3A8A" strokeWidth={4} fill="none" />
            <text x={420} y={720} fontFamily="Kalam" fontWeight={700} fontSize={46} fill="#64748B">
              z z z
            </text>
          </g>
          <g transform="translate(1660 640) rotate(28)">
            <rect x={-10} y={-180} width={20} height={300} rx={8} fill="#1D4ED8" stroke={LINE} strokeWidth={3} />
          </g>
          <g transform="rotate(4 1640 300)">
            <rect x={1440} y={120} width={420} height={330} rx={10} fill="#B91C1C" stroke={LINE} strokeWidth={5} />
            <text x={1650} y={300} textAnchor="middle" fontFamily="Playfair Display" fontWeight={700} fontSize={54} fill="#FEF3C7">
              ECONOMICS
            </text>
          </g>
          <Phone x={730} y={70} w={480} rot={-4}>
            <LockNotifs w={480} date="Monday, 4 March" time="9:41" notifs={[{ title: "Recording", text: `41:${String(secs).padStart(2, "0")}`, recording: true, ongoing: true, actions: ["Save", "Pause"] }]} />
          </Phone>
          <path d="M300 -100 L900 -100 L1700 1200 L1100 1200 Z" fill="#FFF7ED" opacity={0.08} />
          {Array.from({ length: 18 }, (_, i) => (
            <circle key={i} cx={300 + ((i * 197) % 1400) + Math.sin(f / 40 + i) * 30} cy={120 + ((i * 251) % 900) - ((f * (0.4 + (i % 3) * 0.2)) % 160)} r={2 + (i % 3)} fill="#FFF7ED" opacity={0.5} />
          ))}
        </Camera>
        <LightLeak k={interpolate(f, [SCENES.type.from - 14, SCENES.type.from + 4], [0, 1], clamp)} color="#FDE68A" />
      </Flashback>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.18} />
    </Frame>
  );
};

const Q = "What did Prof say will be on the exam?";
const A = "From Monday's Econ lecture: chapters 4 and 7, plus the metro case study (20 marks). Exam: Thursday, 9 am.";
const TRANSCRIPT = "...So, in Thursday's exam: chapters four and seven, and the metro case study. Twenty marks. So read the metro case carefully...";

/** Behind her phone at 2:11 am: the desk, the lamp, the clock (left half of the frame). */
const DeskAt211: React.FC<{ f: number }> = ({ f }) => (
  <g>
    <Depth blur={9}>
      <RoomL f={f} />
      <Lamp x={700} y={300} />
    </Depth>
    <DeskL y={800} />
    <Books x={830} y={812} n={5} />
    <Clock x={350} y={760} text={f % 30 < 15 ? "2:11" : "2 11"} />
  </g>
);

/** Srutam's answer, large, on the left of the frame (the phone on the right is small to read). */
const AnswerCallout: React.FC<{ f: number; words: number; k: number }> = ({ f, words, k }) => {
  const all = A.split(" ");
  return (
    <g opacity={k} transform={`translate(${(1 - k) * -40} 0)`}>
      <foreignObject x={110} y={150} width={900} height={520}>
        <div style={{ background: "rgba(255,255,255,0.96)", borderRadius: 34, padding: "34px 42px", boxShadow: "0 18px 50px rgba(0,0,0,0.35)", fontFamily: "Inter, sans-serif", color: "#0F172A" }}>
          <div style={{ fontFamily: "Playfair Display", fontSize: 38, fontWeight: 700, color: "#1E3A8A" }}>
            Srutam <span style={{ fontStyle: "italic", color: "#2563EB" }}>AI</span>
          </div>
          <div style={{ fontSize: 42, fontWeight: 600, lineHeight: 1.32, marginTop: 14, minHeight: 222 }}>
            {all.slice(0, words).join(" ")}
            {words < all.length && <span style={{ opacity: Math.floor(f / 6) % 2 ? 0.2 : 1 }}>▍</span>}
          </div>
          <div style={{ display: "flex", gap: 14, marginTop: 18, opacity: words >= all.length ? 1 : 0 }}>
            <div style={{ padding: "8px 22px", borderRadius: 999, background: "#DBEAFE", color: "#1D4ED8", fontWeight: 700, fontSize: 30 }}>Econ lecture · Mon, 9:00 am</div>
          </div>
        </div>
      </foreignObject>
    </g>
  );
};

/* 6. 2:11 am: she types the question (a jump cut skips most of it), sends it; Srutam answers
   from the lecture and cites it; she taps the note with her index finger. */
const TypeShot: React.FC = () => {
  const f = useT() + SCENES.type.from;
  const PX = 1120;
  const PY = 50;
  const PW = 440;
  const ph = PW * (2400 / 1080);
  const cut = BEAT.typeStart + 22;
  const t = typed(Q, f < cut ? f : f + 220, BEAT.typeStart, 9);
  const sent = f >= BEAT.send;
  const kb = sent ? interpolate(f, [BEAT.send, BEAT.send + 8], [1, 0], clamp) : 1;
  const words = A.split(" ");
  const shown = Math.round(interpolate(f, [BEAT.answer, BEAT.answerEnd], [0, words.length], clamp));
  const th = thumbs(sent ? undefined : t.key, t.press, PW);
  const thumbR: Pt | null = f >= BEAT.send - 4 && f < BEAT.send + 4 ? [PW - 46, ph - 214] : th.r;
  const point: Pt | null = f >= BEAT.tapRef - 10 && f < BEAT.tapRef + 10 ? [PW * 0.32, ph * 0.68 + (f < BEAT.tapRef ? (BEAT.tapRef - f) * 9 : 0)] : null;
  const tapK = interpolate(f, [BEAT.tapRef, BEAT.tapRef + 12], [0, 1], clamp);
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.type), [1, 1.02], clamp)} focus={[1340, 520]} drift={2} seed={14}>
        <DeskAt211 f={f} />
        <HandsOnPhone x={PX} y={PY} w={PW} glow={sent ? "#3B82F6" : undefined} thumbL={th.l} thumbR={thumbR} point={point}>
          <AskFlow
            w={PW}
            sent={sent ? Q : undefined}
            thinking={sent && f < BEAT.answer}
            answer={shown > 0 ? words.slice(0, shown).join(" ") : undefined}
            refs={f >= BEAT.answerEnd ? ["Econ lecture"] : []}
            pressedRef={f >= BEAT.tapRef && f < BEAT.tapRef + 8}
            input={sent ? "" : t.text}
            caret={!sent && Math.floor(f / 8) % 2 === 0}
            keyboard={kb}
            keyDown={t.key}
            sendDown={f >= BEAT.send - 2 && f < BEAT.send + 3}
          />
        </HandsOnPhone>
        {tapK > 0 && tapK < 1 && <circle cx={PX + PW * 0.32} cy={PY + ph * 0.68} r={16 + tapK * 70} fill="none" stroke="#FFFFFF" strokeWidth={7} opacity={1 - tapK} />}
        <AnswerCallout f={f} words={shown} k={springAt(f, 0, [{ at: BEAT.answer, to: 1 }], 16, 150)} />
      </Camera>
      {f >= cut && f < cut + 4 && <rect width={LW} height={LH} fill="#FFFFFF" opacity={0.12} />}
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
    </Frame>
  );
};

/* 6b. The lecture opens; she taps the waveform near the part, then play. */
const DetailShot: React.FC = () => {
  const f = useT() + SCENES.detail.from;
  const PX = 1120;
  const PY = 50;
  const PW = 440;
  const k = PW / 470;
  const slide = interpolate(f, [SCENES.detail.from, SCENES.detail.from + 8], [1, 0], { ...clamp, easing: easeOut });
  const seeked = f >= BEAT.seek;
  const waveX = (24 + 20 + 70 + 16) * k;
  const waveW = PW - waveX - 120 * k;
  const waveY = (76 + 20 + 18 + 34 + 4 + 18 + 24 + 22 + 35) * k;
  const seekX = waveX + waveW * 0.79;
  const playX = (24 + 20 + 35) * k;
  const point: Pt | null = f < BEAT.seek - 10 ? null : f < BEAT.playBtn - 5 ? [seekX, waveY + 8] : f < BEAT.playBtn + 8 ? [playX, waveY + 8] : null;
  const tapB = interpolate(f, [BEAT.playBtn, BEAT.playBtn + 10], [0, 1], clamp);
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} drift={2} seed={15} focus={[1340, 520]}>
        <DeskAt211 f={f} />
        <HandsOnPhone x={PX} y={PY} w={PW} glow="#3B82F6" point={point}>
          <div style={{ position: "absolute", inset: 0, transform: `translateX(${slide * 100}%)` }}>
            <NoteDetail
              w={PW}
              title="Econ lecture"
              meta="Mon, 9:00 am · 52:10"
              progress={seeked ? 0.79 : 0}
              playing={f >= BEAT.playBtn}
              time={seeked ? "41:05" : "0:00"}
              total="52:10"
              tap={interpolate(f, [BEAT.seek, BEAT.seek + 10], [0, 1], clamp)}
              transcript={TRANSCRIPT}
            />
          </div>
        </HandsOnPhone>
        {tapB > 0 && tapB < 1 && <circle cx={PX + playX} cy={PY + waveY} r={16 + tapB * 64} fill="none" stroke="#FFFFFF" strokeWidth={7} opacity={1 - tapB} />}
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
    </Frame>
  );
};

/** Chalk lines that write themselves in from `at` (k 0..1 over `len` frames). */
const ChalkLine: React.FC<{ f: number; x: number; y: number; text: string; at: number; len?: number; size?: number; hot?: boolean }> = ({ f, x, y, text, at, len = 14, size = 50, hot = false }) => {
  const k = interpolate(f, [at, at + len], [0, 1], clamp);
  if (k <= 0) return null;
  const chars = Math.round(text.length * k);
  return (
    <text x={x} y={y} fontFamily="Kalam" fontWeight={700} fontSize={size} fill={hot ? "#FDE68A" : "#F1F5F9"}>
      {text.slice(0, chars)}
    </text>
  );
};

/** The lecture, replayed as a sepia memory: Prof at the board, the words chalked as he says them. */
const Memory: React.FC<{ f: number; x: number; y: number; w: number; h: number; k: number }> = ({ f, x, y, w, h, k }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const p0 = BEAT.play;
  const speaking = f >= p0 && f < p0 + 192;
  const P = { x: x + w - 280, y: y + 70, s: 0.45 };
  const pointAt = (at: number) => f >= at - 6 && f < at + 30;
  const pointing = pointAt(BEAT.chapters) || pointAt(BEAT.metro) || pointAt(BEAT.marks);
  const hand = springPt(f, reach("l", 150, 0.8), [
    { at: BEAT.chapters - 6, to: reach("l", 200, 0.82) },
    { at: BEAT.chapters + 30, to: reach("l", 150, 0.8) },
    { at: BEAT.metro - 6, to: reach("l", 212, 0.82) },
    { at: BEAT.metro + 30, to: reach("l", 150, 0.8) },
    { at: BEAT.marks - 6, to: reach("l", 222, 0.82) },
    { at: BEAT.marks + 30, to: reach("l", 150, 0.8) },
  ], 13, 170);
  const bx = x + 34;
  const by = y + 60;
  return (
    <g opacity={k} transform={`translate(${(1 - k) * 60} 0)`}>
      <defs>
        <clipPath id={`mem${id}`}>
          <rect x={x} y={y} width={w} height={h} rx={28} />
        </clipPath>
      </defs>
      <rect x={x - 10} y={y - 10} width={w + 20} height={h + 20} rx={36} fill="#FFF7ED" opacity={0.9} />
      <g clipPath={`url(#mem${id})`}>
        <rect x={x} y={y} width={w} height={h} fill="#E2CFA8" />
        <rect x={bx} y={by} width={w - 300} height={h - 190} rx={12} fill="#1F3B2D" stroke="#7C5A3A" strokeWidth={12} />
        <ChalkLine f={f} x={bx + 26} y={by + 92} text="Exam: Thursday" at={p0 + 4} />
        <ChalkLine f={f} x={bx + 26} y={by + 182} text="Ch. 4, Ch. 7" at={BEAT.chapters} hot={f < BEAT.metro} />
        <ChalkLine f={f} x={bx + 26} y={by + 272} text="+ metro case study" at={BEAT.metro} hot={f >= BEAT.metro && f < BEAT.marks} />
        <ChalkLine f={f} x={bx + 26} y={by + 362} text="(20 marks)" at={BEAT.marks} hot={f >= BEAT.marks} />
        <Actor
          x={P.x}
          y={P.y}
          s={P.s}
          f={f}
          who={PROF}
          torso={620}
          expr="talk"
          viseme={speaking ? vis("prof-recording", f) : "X"}
          turn={pointing ? -0.4 : -0.1}
          gaze={pointing ? [-0.9, -0.2] : [0.1, 0.3]}
          tilt={talkTilt(["prof-recording"], f, -2, 1.2)}
          blink={blinkK(f, 4)}
          arms={{ l: { to: hand, pose: pointing ? "point" : "open", rot: pointing ? 0 : 20 } }}
        />
        <rect x={x} y={y + h - 90} width={w} height={90} fill="#A9744D" />
        <rect x={x} y={y + h - 90} width={w} height={16} fill="#C9976A" />
        <rect x={x} y={y} width={w} height={h} fill="#B45309" opacity={0.18} style={{ mixBlendMode: "multiply" }} />
      </g>
      <g transform={`translate(${x + 26} ${y + 24})`}>
        <rect width={300} height={52} rx={26} fill="#78350F" opacity={0.85} />
        <text x={150} y={36} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={26} fill="#FEF3C7">
          MONDAY · ECON
        </text>
      </g>
    </g>
  );
};

/* 7. The lecture plays: "In Thursday's exam: chapters 4 and 7, and the metro case study. Twenty
   marks." Meera listens on the left; the lecture replays beside her as a memory. */
const Listen: React.FC = () => {
  const f = useT() + SCENES.listen.from;
  const p0 = BEAT.play;
  const expr = f < BEAT.relief ? "blank" : f < BEAT.yes ? "smug" : "laugh";
  const M = { x: 40, y: 150, s: 1.12 };
  const fist = springPt(f, [130, 900], [{ at: BEAT.yes - 6, to: [110, 600] }], 13, 150);
  const lean = springAt(f, 0, [{ at: p0 + 4, to: 1 }], 16, 90);
  const playing = f >= p0 && f < p0 + 192;
  const PH: Pt = [M.x + 545 * M.s + 20, M.y + 430 * M.s - 120];
  const mem = springAt(f, 0, [{ at: p0 + 2, to: 1 }], 16, 120);
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.listen), [1, 1.04], { ...clamp, easing: easeInOut })} focus={[960, 560]} drift={3} seed={16}>
        <Depth blur={9}>
          <RoomL f={f} />
          <Lamp x={700} y={300} />
        </Depth>
        <Actor
          x={M.x}
          y={M.y}
          s={M.s}
          f={f}
          who="meera"
          shadow
          screen={<rect x={-38} y={-84} width={76} height={164} rx={9} fill="#0C1225" />}
          expr={expr}
          tilt={lean * 4 + (f > BEAT.yes ? Math.sin(f / 6) * 1.5 : 0)}
          gaze={f < BEAT.relief ? [0.8, -0.2] : [0.3, 0]}
          blink={blinkK(f, 1)}
          arms={{ r: { to: [545, 430], pose: "phone", upright: true }, ...(f > BEAT.yes - 8 ? { l: { to: fist, pose: "fist" as const } } : {}) }}
        />
        {playing &&
          [0, 1, 2].map((i) => {
            const k = ((f - p0 + i * 12) % 36) / 36;
            return <path key={i} d={`M${PH[0] + 60 + k * 90} ${PH[1] - 80 - k * 40} q${36 + k * 30} ${80 + k * 40} 0 ${160 + k * 80}`} stroke="#93C5FD" strokeWidth={7} fill="none" opacity={(1 - k) * 0.5} strokeLinecap="round" />;
          })}
        <ScreenGlow cx={PH[0] - 120} cy={PH[1] - 40} rx={380} ry={420} />
        <Memory f={f} x={1000} y={230} w={860} h={680} k={mem} />
      </Camera>
      <Grade warm="#93C5FD" cool="#1E1B4B" from="right" amount={0.12} />
    </Frame>
  );
};

/* 8. She shares it with the group; the replies come in. */
const Share: React.FC = () => {
  const f = useT() + SCENES.share.from;
  const appear = [1, 1, 1, ...[BEAT.mine, ...BEAT.replies].map((at) => springAt(f, 0, [{ at, to: 1 }], 14, 170))];
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.share), [1, 1.04], clamp)} focus={[960, 540]} drift={3} seed={18}>
        <Depth blur={9}>
          <RoomL f={f} />
          <Lamp x={1180} y={330} />
        </Depth>
        <ReadingMeera f={f} expr={f < BEAT.replies[0] ? "smug" : "laugh"} gaze={[0.8, 0.6]} tilt={f < BEAT.replies[0] ? 2 : 4 + Math.sin(f / 6)} />
        <ChatPhone f={f}>
          <ChatScreen w={600} title="Econ gang" members="Riya, Kabir, you" messages={CHAT} appear={appear} typing={f > BEAT.replies[0] + 4 && f < BEAT.replies[1] ? "Kabir" : undefined} />
        </ChatPhone>
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="right" amount={0.14} />
    </Frame>
  );
};

/** A tray of chai in kulhads, carried. */
const Tray: React.FC<{ x: number; y: number; f: number; s?: number }> = ({ x, y, f, s = 1 }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    <ellipse cx={0} cy={0} rx={150} ry={26} fill="#9CA3AF" stroke={LINE} strokeWidth={4} />
    {[-80, 0, 80].map((dx, i) => (
      <Kulhad key={i} x={dx} y={-14} s={0.55} t={f + i * 9} />
    ))}
  </g>
);

/* 9. Knock knock, and the door swings in: Kabir with chai, "Chai's here!"; Riya: "Come on, let's start!"
   One wide shot: Meera at her desk on the left, the door in the middle, her bed on the right. */
const DoorShot: React.FC = () => {
  const f = useT() + SCENES.door.from;
  const raps = [BEAT.knock, BEAT.knock + 6, BEAT.knock + 14];
  const jig = raps.reduce((a, at) => a + (f >= at && f < at + 3 ? 3 : 0), 0);
  const raw = springAt(f, 0, [{ at: BEAT.open, to: 92 }], 13, 120);
  const theta = Math.min(raw, 96);
  const th = (theta * Math.PI) / 180;
  const [hx, top, bot, dw] = [1000, 150, 990, 360];
  const freeX = hx + dw * Math.cos(th);
  const grow = 110 * Math.sin(th);
  const light = Math.sin(th);
  const S = 0.47;
  const floor = 1000;
  const yOf = (torso: number) => floor - (800 + torso) * S;
  const door0 = hx + dw / 2 - 300 * S;
  // Kabir walks in from the hallway toward Meera (legs swing with distance), then stops.
  const kIn = springAt(f, 0, [{ at: BEAT.open + 4, to: 1 }], 20, 60);
  const kx = door0 - 300 * kIn;
  const kWalk = (door0 - kx) / 38;
  const rIn = springAt(f, 0, [{ at: BEAT.riyaIn, to: 1 }], 20, 60);
  const rx = door0 + 10 + 140 * rIn;
  const rWalk = (rx - door0) / 38;
  const meeraTurn = springAt(f, 0, [{ at: BEAT.knock + 4, to: 0.5 }], 14, 120);
  const rArm = reach("r", -60, 0.9);
  return (
    <Frame bg={["#1E1B4B", "#0B1033"]} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.door), [1, 1.04], clamp)} focus={[960, 560]} drift={3} seed={18}>
        <rect width={LW} height={LH} fill="#2A2560" />
        <rect y={floor} width={LW} height={LH - floor} fill="#1F1B47" />
        {/* the wall: pinned notes over the desk, a shelf and a poster by the bed */}
        {[0, 1, 2, 3].map((i) => (
          <rect key={i} x={120 + i * 150} y={150 + (i % 2) * 40} width={110} height={140} rx={8} fill={["#F472B6", "#60A5FA", "#FBBF24", "#34D399"][i]} opacity={0.4} transform={`rotate(${i * 5 - 8} ${175 + i * 150} 220)`} />
        ))}
        <rect x={1520} y={170} width={300} height={380} fill="#4C3B8F" stroke={LINE} strokeWidth={6} />
        <circle cx={1670} cy={330} r={90} fill="#FBBF24" opacity={0.5} />
        {/* the lit hallway behind the door */}
        <rect x={hx} y={top} width={dw} height={bot - top} fill="#FDE68A" />
        <rect x={hx} y={bot - 110} width={dw} height={110} fill="#F59E0B" opacity={0.4} />
        <path d={`M${hx} ${bot} L${hx + dw} ${bot} L${hx + dw + 300 * light} ${LH} L${hx - 240 * light} ${LH} Z`} fill="#FDE68A" opacity={0.3 * light} />
        {/* the door panel, hinged on its left edge, swinging toward us */}
        <g transform={`translate(${jig} 0)`}>
          <path d={`M${hx} ${top} L${freeX} ${top - grow} L${freeX} ${bot + grow} L${hx} ${bot} Z`} fill={mix("#9A6B44", "#6B4528", light)} stroke={LINE} strokeWidth={8} />
          {theta < 40 && <circle cx={hx + (dw - 40) * Math.cos(th)} cy={(top + bot) / 2} r={13} fill="#FBBF24" stroke={LINE} strokeWidth={4} />}
        </g>
        <rect x={hx - 22} y={top - 22} width={dw + 44} height={22} fill="#4C3B8F" stroke={LINE} strokeWidth={6} />
        {/* her bed, right */}
        <rect x={1460} y={760} width={520} height={260} rx={20} fill="#6D28D9" stroke={LINE} strokeWidth={6} />
        <rect x={1460} y={720} width={520} height={80} rx={30} fill="#A78BFA" stroke={LINE} strokeWidth={5} />
        <rect x={1700} y={660} width={200} height={90} rx={36} fill="#F5F3FF" stroke={LINE} strokeWidth={5} />
        <g opacity={rIn > 0.01 ? 1 : 0}>
          <Actor
            x={rx}
            y={yOf(1000)}
            s={S}
            f={f}
            who={RIYA}
            shadow
            torso={1000}
            legs={{ pants: "#1E3A8A", shoes: "#F8FAFC", walk: rWalk }}
            expr="laugh"
            viseme={vis("riya-study", f)}
            tilt={talkTilt(["riya-study"], f, -2, 1.6)}
            gaze={[-0.8, 0.2]}
            blink={blinkK(f, 3)}
            turn={-0.4}
            arms={f > 954 ? { r: { to: [rArm[0], rArm[1] - emph(["riya-study"], f) * 40], pose: "fist" } } : {}}
          />
        </g>
        <g opacity={kIn > 0.01 ? 1 : 0}>
          <Actor
            x={kx}
            y={yOf(1000)}
            s={S}
            f={f}
            who={KABIR}
            shadow
            torso={1000}
            legs={{ pants: "#334155", shoes: "#1F2937", walk: kWalk }}
            expr="laugh"
            viseme={vis("kabir-chai", f)}
            tilt={talkTilt(["kabir-chai"], f, 0, 1.6)}
            turn={-0.4}
            gaze={[-0.8, 0.3]}
            blink={blinkK(f, 5)}
            arms={{ l: { to: [160, 880], pose: "grip" }, r: { to: [440, 880], pose: "grip" } }}
          />
          <Tray x={kx + 300 * S} y={yOf(1000) + 880 * S - 6} f={f} s={S * 1.6} />
        </g>
        {/* Meera at her desk, left, turns to the door */}
        <Actor x={60} y={330} s={0.88} f={f} who="meera" shadow expr={f < BEAT.open ? "surprised" : "laugh"} turn={meeraTurn} gaze={[0.9, -0.1]} blink={blinkK(f, 1)} />
        <DeskL y={860} x0={-40} x1={820} />
        <Books x={640} y={872} n={3} />
        <g transform="translate(250 880)">
          <DeskPhone x={0} y={0} s={0.8} f={f} rec={false} />
        </g>
      </Camera>
      <Grade warm="#FDE68A" cool="#1E1B4B" from="right" amount={0.18} />
    </Frame>
  );
};

/* 10. Time-lapse, 2:31 to 4:10: chai, Key Points, the sky turning. The trio on the left, the
   Insights card on the propped-up phone on the right. */
const Lapse: React.FC = () => {
  const f = useT() + SCENES.lapse.from;
  const lapse = interpolate(f, span(SCENES.lapse), [0, 1], { ...clamp, easing: easeInOut });
  const mins = Math.round(151 + lapse * 99);
  const clock = `${Math.floor(mins / 60)}:${String(mins % 60).padStart(2, "0")}`;
  const cups = [BEAT.clink[0], BEAT.clink[1], BEAT.cup].map((at) => springAt(f, 0, [{ at, to: 1 }], 12, 170));
  const sway = (seed: number, amp = 3) => Math.sin(f / 9 + seed) * amp;
  const laughing = (seed: number) => Math.sin(f / 14 + seed) > 0.55;
  const tableY = 820;
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.lapse), [1, 1.04], clamp)} focus={[960, 560]} drift={3} seed={20}>
        <Depth blur={4}>
          <RoomL f={f} dawn={lapse * 0.65} />
          <g transform="translate(860 110)">
            <rect x={-150} y={-66} width={300} height={92} rx={18} fill="#0F172A" stroke={LINE} strokeWidth={5} />
            <text x={0} y={6} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={66} fill="#FBBF24">
              {clock}
            </text>
          </g>
        </Depth>
        <Actor x={-30} y={300} s={0.72} f={f} who={RIYA} shadow torso={120} expr={laughing(1) ? "laugh" : "smile"} tilt={sway(1)} sway={sway(1, 0.4)} gaze={[0.5, 0.5]} blink={blinkK(f, 3)} />
        <Actor x={890} y={300} s={0.72} f={f} who={KABIR} shadow torso={120} expr={laughing(3) ? "laugh" : "smile"} tilt={sway(3)} gaze={[-0.5, 0.4]} blink={blinkK(f, 5)} />
        <Actor x={430} y={250} s={0.78} f={f} who="meera" shadow expr={laughing(5) ? "laugh" : "smug"} tilt={sway(5, 2)} gaze={[0.2, 0.6]} blink={blinkK(f, 1)} />
        <DeskL y={tableY} />
        {cups.map((c, i) => {
          const cx = 260 + i * 330;
          return (
            <g key={i} transform={`translate(${cx} ${tableY + 6}) scale(${c}) translate(${-cx} ${-(tableY + 6)})`} opacity={c > 0.02 ? 1 : 0}>
              <Kulhad x={cx} y={tableY + 6} s={0.9} t={f + i * 7} />
            </g>
          );
        })}
        <path d="M480 880 L800 880 L780 846 L500 846 Z" fill="#FFFDF8" stroke={LINE} strokeWidth={3} />
        <Books x={1240} y={832} n={3} />
        {/* the phone, propped against the books */}
        <Phone x={1430} y={60} w={440} rot={3} glow="#FBBF24">
          <InsightsScreen
            w={440}
            note="Econ lecture"
            meta="Mon, 9:00 am · 52:10"
            items={[
              { kind: "KEY POINT", text: "Ch. 4: why metro fares rose" },
              { kind: "KEY POINT", text: "Ch. 7: public goods" },
              { kind: "REMINDER", text: "Econ exam", meta: "Thursday, 9:00 am" },
            ]}
          />
        </Phone>
      </Camera>
      <Grade warm="#FDE68A" cool="#1E1B4B" from="right" amount={0.16} />
    </Frame>
  );
};

/** The corridor outside the exam hall, morning (blurred behind). */
const Corridor: React.FC = () => (
  <g>
    <rect width={LW} height={LH} fill="#FDE7C7" />
    {[0, 1, 2, 3, 4].map((i) => (
      <rect key={i} x={80 + i * 380} y={140} width={260} height={520} fill="#FFFBEB" stroke="#E7C9A0" strokeWidth={10} />
    ))}
    <rect y={780} width={LW} height={LH - 780} fill="#F3D2A8" />
  </g>
);

/* 11. 8:45 am: the reminder knocks. "Econ exam. Starts in 15 minutes." Meera, ready, beside it. */
const Remind: React.FC = () => {
  const f = useT() + SCENES.remind.from;
  const on = f >= BEAT.notify;
  const nudge = on ? damped(f, BEAT.notify, 6, 4, 6) : 0;
  return (
    <Frame bg={["#FEF3C7", "#FDE68A"]}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.remind), [1, 1.04], clamp)} focus={[960, 540]} drift={3} seed={22}>
        <Depth blur={10}>
          <Corridor />
        </Depth>
        <Actor x={130} y={170} s={1.1} f={f} who="meera" shadow expr={on ? "smug" : "smile"} gaze={[0.8, 0.5]} tilt={2} blink={blinkK(f, 1)} />
        <g transform={`translate(${nudge} 0)`}>
          <HeldPhone f={f} x={1140} y={60} w={480} glow={on ? "#FBBF24" : undefined}>
            <LockNotifs w={480} date="Thursday, 7 March" time="8:45" notifs={on ? [{ title: "Econ exam", text: "Starts in 15 minutes.", reminder: true }] : []} />
          </HeldPhone>
        </g>
      </Camera>
    </Frame>
  );
};

/* 12. The exam hall: the paper floats down, Q3 is the metro case study. A glance at the friends, a pen spin. */
const Hall: React.FC = () => {
  const f = useT() + SCENES.hall.from;
  const start = BEAT.paperDown - 29;
  const fall = flutter(f, start, [0, -900], [0, 0], [0, 1300], 3, 26, 1.1);
  const paper: Pt = f < start ? [0, -2000] : fall[1] >= 0 ? [0, 0] : fall;
  const caught = f >= BEAT.catch;
  const penRot = caught ? spin(BEAT.catch, BEAT.spin, 1500, 2.2) : spin(f, BEAT.spin, 1500, 2.2);
  const write = caught ? Math.min(1, (f - BEAT.catch) / 16) : 0;
  const M = { x: 40, y: 120, s: 1.02 };
  // The writing hand, in frame coordinates: hovering while the pen spins, then writing along Q3.
  // While writing, the hand sits under the answer line with the pen tip up-left of it.
  const handW: Pt = caught ? [931 + write * 260 + Math.sin(f * 1.1) * 6, 1046 + Math.cos(f * 1.5) * 3] : [690, 880 + Math.sin(f / 9) * 4];
  const glance = springAt(f, 0, [{ at: BEAT.nod - 6, to: 1 }, { at: BEAT.spin - 2, to: 0 }], 14, 150);
  const tableY = 760;
  return (
    <Frame bg={["#FEF9C3", "#FDE68A"]}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.hall), [1, 1.05], { ...clamp, easing: easeInOut })} focus={[960, 640]} drift={3} seed={24}>
        <Depth blur={6}>
          <rect width={LW} height={LH} fill="#FDF2DC" />
          {[0, 1, 2, 3].map((i) => (
            <rect key={i} x={760 + i * 300} y={90} width={220} height={420} fill="#FFFBEB" stroke="#E7C9A0" strokeWidth={10} />
          ))}
          <Actor x={980} y={300} s={0.46} f={f} who={RIYA} torso={200} expr={f > BEAT.nod ? "smile" : "neutral"} gaze={[-0.6, 0.3]} turn={-0.2} />
          <Actor x={1400} y={300} s={0.46} f={f} who={KABIR} torso={200} expr={f > BEAT.nod ? "laugh" : "neutral"} gaze={[-0.7, 0.3]} turn={-0.3} arms={f > BEAT.nod ? { l: { to: [60, 440], pose: "thumb", upright: true } } : {}} />
          <rect x={900} y={640} width={900} height={30} fill="#B08155" />
        </Depth>
        <Actor
          x={M.x}
          y={M.y}
          s={M.s}
          f={f}
          who="meera"
          shadow
          torso={120}
          expr={f < BEAT.circle ? "blank" : "smug"}
          tilt={caught ? 3 : 0}
          turn={glance * 0.25}
          gaze={glance > 0.5 ? [0.9, -0.2] : [0.6, 0.9]}
          blink={blinkK(f, 1)}
        />
        <DeskL y={tableY} color="#B08155" top="#C9976A" />
        <g transform={`translate(${paper[0]} ${paper[1]}) rotate(${-3 + paper[0] * 0.15} 1140 950)`}>
          <rect x={820} y={730} width={680} height={400} fill="#FFFDF8" stroke={LINE} strokeWidth={5} />
          <text x={860} y={796} fontFamily="Kalam" fontWeight={700} fontSize={40} fill="#1F2937">
            <tspan x={860}>Q1. Explain Chapter 4…</tspan>
            <tspan x={860} dy={56}>
              Q2. Chapter 7: public…
            </tspan>
            <tspan x={860} dy={56}>
              Q3. Metro case study (20 marks)
            </tspan>
          </text>
          {f > BEAT.circle && <ellipse cx={1150} cy={896} rx={320 * Math.min(1, (f - BEAT.circle) / 8)} ry={40} fill="none" stroke="#DC2626" strokeWidth={6} />}
          {write > 0 && <path d="M900 990 q30 -20 60 0 t60 0 t60 0 t60 0 t60 0" stroke="#1D4ED8" strokeWidth={5} fill="none" strokeDasharray={400} strokeDashoffset={400 * (1 - write)} />}
        </g>
        <g transform={`translate(${handW[0]} ${handW[1]}) rotate(${caught ? -58 : -33})`}>
          <path d="M-560 -54 L-14 -36 L-14 36 L-560 54 Z" fill="#EFE6D8" stroke={LINE} strokeWidth={5} />
          <g transform="scale(1.15)">
            <Hand pose="grip" skin="#F2C3A2" />
          </g>
        </g>
        <g transform={`translate(${handW[0]} ${handW[1]}) rotate(${caught ? 151 + Math.sin(f * 1.1) * 4 : penRot - 30})`}>
          <rect x={-8} y={-150} width={16} height={190} rx={6} fill="#1D4ED8" stroke={LINE} strokeWidth={3} />
          <path d="M-8 40 L0 64 L8 40 Z" fill="#E5E7EB" stroke={LINE} strokeWidth={2} />
        </g>
      </Camera>
    </Frame>
  );
};

/** The closing band across the bottom: wordmark and tagline. */
const EndBandL: React.FC<{ tagline: string }> = ({ tagline }) => (
  <g>
    <rect x={0} y={810} width={LW} height={LH - 810} fill="#050714" />
    <text x={LW / 2} y={930} textAnchor="middle" fontFamily="Playfair Display" fontSize={104} fill="#F8FAFC">
      Srutam
    </text>
    <text x={LW / 2} y={1018} textAnchor="middle" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={50} fill="#93C5FD">
      {tagline}
    </text>
  </g>
);

/* 13. Out of the hall: the three laugh, the question papers go up. "Listen once. Ask anytime." */
const End: React.FC = () => {
  const f = useT() + SCENES.end.from;
  const band = interpolate(f, [BEAT.band, BEAT.band + 20], [0, 1], { ...clamp, easing: easeOut });
  const bob = (seed: number) => Math.abs(Math.sin((f + seed) / 6)) * 8;
  const raise = springPt(f, [470, 900], [{ at: SCENES.end.from + 2, to: [540, 420] }], 12, 140);
  const t0 = SCENES.end.from + 4;
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <Camera f={f} drift={3} seed={26}>
        <g transform={`translate(0 ${-band * 150})`}>
          <Depth blur={5}>
            <Corridor />
          </Depth>
          <g transform={`translate(0 ${-bob(0)})`}>
            <Actor x={200} y={300} s={0.72} f={f} who={RIYA} shadow torso={300} expr="laugh" tilt={-3} turn={0.3} />
          </g>
          <g transform={`translate(0 ${-bob(7)})`}>
            <Actor x={1250} y={300} s={0.72} f={f} who={KABIR} shadow torso={300} expr="laugh" tilt={3} turn={-0.3} />
          </g>
          <Actor x={700} y={220} s={0.82} f={f} who="meera" shadow torso={300} expr="laugh" viseme={vis("e-tagline", f)} tilt={talkTilt(["e-tagline"], f, 0, 1.2)} blink={blinkK(f, 1)} arms={{ r: { to: raise, pose: "phone", upright: true } }} />
        </g>
        {[0, 1, 2, 3].map((i) => {
          const at = t0 + i * 4;
          if (f < at) return null;
          const dir = i % 2 === 0 ? 1 : -1;
          const p = flutter(f, at, [960 + dir * 520, 1000], [dir * (900 + i * 120), -1700 - i * 150], [dir * 120, 300], 2.0, 40, 1.1);
          const t = (f - at) / 30;
          return (
            <g key={i} transform={`translate(${p[0]} ${p[1]}) rotate(${dir * t * 160 + i * 30}) scale(${Math.cos(t * 4 + i)} 1)`}>
              <rect x={-70} y={-90} width={140} height={180} fill="#FFFDF8" stroke={LINE} strokeWidth={4} />
              <path d="M-48 -50 h96 M-48 -18 h76 M-48 14 h96" stroke="#94A3B8" strokeWidth={5} />
            </g>
          );
        })}
      </Camera>
      <g transform={`translate(0 ${(1 - band) * 300})`}>
        <EndBandL tagline="Listen once. Ask anytime." />
      </g>
    </Frame>
  );
};

const LIST = [
  ["1 2:07 am", SCENES.night, Night],
  ["2 Group chat", SCENES.chat, Chat],
  ["3 She was asleep too", SCENES.guilt, Guilt],
  ["4 Monday: Start", SCENES.start, Start],
  ["5 Lecture (flashback)", SCENES.lecture, Lecture],
  ["5b Recording insert", SCENES.insert, Insert],
  ["6 Type and ask", SCENES.type, TypeShot],
  ["6b Open the lecture", SCENES.detail, DetailShot],
  ["7 The lecture plays", SCENES.listen, Listen],
  ["8 Share", SCENES.share, Share],
  ["9 Chai's here", SCENES.door, DoorShot],
  ["10 Time-lapse", SCENES.lapse, Lapse],
  ["11 Reminder", SCENES.remind, Remind],
  ["12 Exam hall", SCENES.hall, Hall],
  ["13 End", SCENES.end, End],
] as const;

/** Keep captions off the phone in the shots where it sits on the right. */
const captionLane = (f: number): [number, number] | undefined => {
  if (f >= SCENES.start.from && f < SCENES.lecture.from) return [80, 900];
  if (f >= SCENES.type.from && f < SCENES.listen.from) return [80, 900];
  return undefined;
};

export const ExamLandscape: React.FC = () => {
  const f = useT();
  // Old-film warmth through the flashback (shots 4-5).
  const sepia = interpolate(f, [SCENES.start.from, SCENES.start.from + 16, SCENES.type.from - 10, SCENES.type.from], [0, 0.5, 0.5, 0], clamp);
  return (
    <Landscape>
      <AbsoluteFill style={{ filter: sepia > 0 ? `sepia(${sepia})` : undefined }}>
        {LIST.map(([name, s, Comp]) => (
          <Sequence key={name} name={name} from={s.from * K} durationInFrames={s.duration * K}>
            <Comp />
          </Sequence>
        ))}
      </AbsoluteFill>
      <LCaptions cues={CAPTIONS} k={K} lane={captionLane} />
      <Soundtrack music="exam/music-wide.wav" musicVolume={0.42} lines={VO} sfx={SFX} k={K} sfxGain={0.6} duck={0.35} />
    </Landscape>
  );
};
