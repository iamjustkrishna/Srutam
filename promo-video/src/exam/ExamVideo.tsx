// "2 a.m. Before the Exam" v2: the group chat asks Meera what's coming; she slept
// through that lecture too. Srutam didn't. One question, the answer with its source,
// and the lecture plays the line back. She shares it, her friends arrive with chai,
// and the reminder knocks before the exam. Rendered at 60 fps (see stories/time.ts).
import { useId } from "react";
import { AbsoluteFill, interpolate, Sequence } from "remotion";
import { clamp, easeInOut, easeOut } from "../anim";
import { Kulhad, Zzz } from "../bedtime/Props";
import { KABIR, PROF, RIYA } from "../cast/cast";
import { Actor, blinkK, reach, type Pt } from "../cast/Rig";
import { Camera, Depth, Grade, LightLeak } from "../stories/Camera";
import { damped, flutter, follow, spin, springAt, springPt } from "../stories/physics";
import { Captions, Soundtrack } from "../stories/Story";
import { K, useT } from "../stories/time";
import { voice } from "../stories/voice";
import { AskScreen, Board, Books, ChatScreen, EndBand, Frame, GripHand, H, InsightsScreen, Lamp, LINE, LockNotifs, Phone, Table, W, Window } from "../storyboards/Kit";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";
import { VISEMES } from "./visemes";

const { vis, talkTilt } = voice(VO, VISEMES);
const span = (sc: { from: number; duration: number }): [number, number] => [sc.from, sc.from + sc.duration];
const NIGHT: [string, string] = ["#0B1033", "#1E1B4B"];

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

/** The hostel room at night (dawn 0..1 warms the sky for the time-lapse). */
const Room: React.FC<{ dawn?: number; f: number }> = ({ dawn = 0, f }) => {
  const sky: [string, string] = [mix("#0B1033", "#F9A8D4", dawn * 0.6), mix("#1E1B4B", "#FDBA74", dawn * 0.7)];
  return (
    <g>
      <rect width={W} height={H} fill="#2A2560" />
      <rect y={1480} width={W} height={H - 1480} fill="#1F1B47" />
      <Window x={90} y={200} w={400} h={500} sky={sky} moon={dawn < 0.5} />
      {[
        [140, 260],
        [250, 330],
        [180, 430],
        [430, 600],
        [300, 250],
      ].map(([x, y], i) => (
        <circle key={i} cx={x} cy={y} r={3 + (i % 2)} fill="#FEF3C7" opacity={(0.5 + 0.5 * Math.sin(f / 9 + i * 2)) * (1 - dawn)} />
      ))}
      {[0, 1, 2, 3, 4].map((i) => (
        <rect
          key={i}
          x={620 + (i % 3) * 130}
          y={230 + Math.floor(i / 3) * 170}
          width={96}
          height={128}
          rx={8}
          fill={["#F472B6", "#60A5FA", "#FBBF24", "#34D399", "#A78BFA"][i]}
          opacity={0.45}
          transform={`rotate(${i * 5 - 8} ${668 + (i % 3) * 130} ${294})`}
        />
      ))}
      <rect x={600} y={640} width={420} height={18} fill="#4C3B8F" />
    </g>
  );
};

/** Flashback look: a ripple that settles (k 0 -> 1) and a warm sepia wash. */
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
      <rect width={W} height={H} fill="#B45309" opacity={0.16} style={{ mixBlendMode: "multiply" }} />
      {k < 1 && <rect width={W} height={H} fill="#FFF7ED" opacity={(1 - k) * 0.5} />}
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

/** Two hands holding a phone up in front of us. */
const HeldPhone: React.FC<{ f: number; y: number; w?: number; glow?: string; children: React.ReactNode }> = ({ f, y, w = 680, glow, children }) => {
  const x = 540 - w / 2;
  const h = w * (2400 / 1080);
  const breathe = Math.sin(f / 22) * 4;
  return (
    <g transform={`translate(0 ${breathe})`}>
      <Phone x={x} y={y} w={w} rot={-1.5} glow={glow}>
        {children}
      </Phone>
      <GripHand x={x + 20} y={y + h - 120} s={1.5} rot={-12} />
      <GripHand x={x + w - 20} y={y + h - 120} s={1.5} rot={12} />
    </g>
  );
};

const CHAT = [
  { from: "Riya", text: "What's coming in Econ tomorrow??", color: "#DB2777" },
  { from: "Kabir", text: "no idea yaar", color: "#16A34A" },
  { from: "Riya", text: "Meera, you were in class na?", color: "#DB2777" },
  { from: "Me", text: "Ch 4 + Ch 7 + the metro case study (20 marks). Straight from the lecture recording.", me: true },
  { from: "Riya", text: "WHAT. you legend", color: "#DB2777" },
  { from: "Kabir", text: "coming over. bringing chai", color: "#16A34A" },
];

/* 1. 2:07 am. ECU on the desk clock, pull out: "Exam at nine. And I forgot what Prof said." */
const Night: React.FC = () => {
  const f = useT() + SCENES.night.from;
  const k = interpolate(f, [6, 80], [0, 1], { ...clamp, easing: easeInOut });
  const clockAt: Pt = [470, 1420];
  const zoom = 3.3 + (1 - 3.3) * k;
  const center: Pt = [540 + (clockAt[0] - 540) * k, 960 + (clockAt[1] - 960) * k];
  const lArm = springPt(f, [40, 800], [{ at: BEAT.handHead, to: [215, 345] }], 14, 100);
  const rArm = springPt(f, [580, 800], [{ at: BEAT.buzz + 4, to: [640, 830] }], 14, 140);
  const flicker = 0.94 + 0.06 * Math.sin(f * 0.9) * Math.sin(f * 0.37);
  const lit = interpolate(f, [BEAT.buzz, BEAT.buzz + 4], [0, 1], clamp);
  const buzz = f > BEAT.buzz && f < BEAT.buzz + 10 ? Math.sin(f * 9) * 3 : 0;
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={zoom} focus={clockAt} center={center} drift={3} seed={2}>
        <Depth blur={2}>
          <Room f={f} />
        </Depth>
        <g opacity={flicker}>
          <Lamp x={760} y={880} />
        </g>
        <Actor
          x={150}
          y={650}
          s={1.05}
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
        <Table y={1500} color="#3F2A1D" top="#5B3A22" />
        <path d="M250 1534 L640 1534 L612 1494 L282 1494 Z" fill="#FFFDF8" stroke={LINE} strokeWidth={3} />
        <Books x={880} y={1500} n={7} />
        <g transform={`translate(${720 + buzz} 1506)`}>
          <DeskPhone x={0} y={0} s={0.9} f={f} rec={false} lit={lit} />
        </g>
        <g transform={`translate(${clockAt[0]} ${clockAt[1]})`}>
          <rect x={-150} y={-62} width={300} height={92} rx={18} fill="#0F172A" stroke={LINE} strokeWidth={5} />
          <text x={-22} y={8} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={62} fill="#EF4444">
            {f % 30 < 15 ? "2:07" : "2 07"}
          </text>
          <text x={96} y={6} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={24} fill="#EF4444">
            AM
          </text>
        </g>
      </Camera>
      <Grade warm="#FCD34D" cool="#1E1B4B" from="right" amount={0.2} />
    </Frame>
  );
};

/* 2. The group chat: "What's coming in Econ tomorrow??" ... "Meera, you were in class na?" */
const Chat: React.FC = () => {
  const f = useT() + SCENES.chat.from;
  const appear = BEAT.msg.map((at) => springAt(f, 0, [{ at, to: 1 }], 14, 170));
  const lift = springAt(f, 0, [{ at: 132, to: 1 }], 14, 110);
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.chat), [1, 1.06], clamp)} focus={[540, 1000]} drift={3} seed={3}>
        <Depth blur={9}>
          <Room f={f} />
          <Lamp x={760} y={880} />
        </Depth>
        <HeldPhone f={f} y={200 + (1 - lift) * 500}>
          <ChatScreen w={680} title="Econ gang" members="Riya, Kabir, you" messages={CHAT.slice(0, 3)} appear={appear} typing={f > 176 && f < BEAT.msg[2] ? "Riya" : undefined} />
        </HeldPhone>
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
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
      <Camera f={f} zoom={interpolate(f, span(SCENES.guilt), [1.04, 1.12], { ...clamp, easing: easeInOut })} focus={[540, 900]} drift={3} seed={5}>
        <Depth blur={8}>
          <Room f={f} />
        </Depth>
        <Actor
          x={40}
          y={420}
          s={1.45}
          f={f}
          who="meera"
          shadow
          expr={dawn ? "smug" : "worried"}
          tilt={-3 + up * 4}
          gaze={dawn ? [0.1, -0.1] : [-0.6, -0.7]}
          blink={blinkK(f, 2)}
          squash={1 + up * 0.02}
          arms={{ r: { to: [520, 560], pose: "phone", upright: true } }}
        />
        <ellipse cx={760} cy={1100} rx={380} ry={420} fill="#BFDBFE" opacity={0.14} style={{ mixBlendMode: "screen" }} />
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="right" amount={0.16} />
    </Frame>
  );
};

const BOARD = ["Exam:", "Ch. 4, Ch. 7", "+ metro case (20)"];

/* 4. Monday, sepia, half-heard: Prof talks at the board, the class dozes, Meera too. Her phone records. */
const Lecture: React.FC = () => {
  const f = useT() + SCENES.lecture.from;
  const k = interpolate(f, [BEAT.ripple, BEAT.ripple + 22], [0, 1], { ...clamp, easing: easeOut });
  const P = { x: 620, y: 300, s: 0.66 };
  const hand = springPt(f, [60, 430], [{ at: 288, to: [40, 400] }, { at: 292, to: [60, 430] }], 12, 240);
  const hw: Pt = [P.x + hand[0] * P.s, P.y + hand[1] * P.s];
  const tip: Pt = [hw[0] - 150, hw[1] - 150 + damped(f, 292, 6, 4, 6)];
  const doze = (seed: number) => {
    const p = ((f + seed) % 80) / 80;
    return p < 0.85 ? p * 9 : 9 - (p - 0.85) * 60;
  };
  return (
    <Frame bg={["#E9D8B4", "#C9AE82"]}>
      <Flashback k={k}>
        <Camera f={f} zoom={interpolate(f, span(SCENES.lecture), [1.04, 1.1], clamp)} focus={[540, 900]} drift={3} seed={6}>
          <Depth blur={1.5}>
            <rect width={W} height={H} fill="#E2CFA8" />
            <Board x={40} y={150} w={1000} h={440} chalk lines={BOARD} />
            <path d={`M${hw[0] + 30} ${hw[1] + 30} L${tip[0]} ${tip[1]}`} stroke="#7C4A21" strokeWidth={9} strokeLinecap="round" />
            <Actor x={P.x} y={P.y} s={P.s} f={f} who={PROF} torso={620} expr="talk" viseme={vis("prof-lecture", f, "B")} turn={-0.25} gaze={[-0.6, -0.4]} tilt={talkTilt(["prof-lecture"], f, -3, 1.2)} blink={blinkK(f, 4)} arms={{ l: { to: hand, pose: "grip", rot: 20 } }} />
          </Depth>
          <Table y={1380} color="#7C5134" top="#A9744D" />
          <Actor x={-110} y={930} s={0.68} f={f} who={KABIR} shadow expr="sleepy" tilt={-12 + Math.sin(f / 30) * 2} />
          <Actor x={270} y={960} s={0.64} f={f} who="meera" shadow expr="drowsy" tilt={doze(0)} sway={-doze(0) * 0.05} />
          <Actor x={600} y={930} s={0.68} f={f} who={RIYA} shadow expr="sleepy" tilt={10 + Math.sin(f / 25) * 2} sway={Math.sin(f / 25) * 0.3} />
          <Zzz x={150} y={1080} t={f} />
          <Zzz x={850} y={1080} t={f + 17} />
          <DeskPhone x={540} y={1420} s={1.1} f={f} />
        </Camera>
      </Flashback>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.18} />
    </Frame>
  );
};

/* 5. Insert: on her desk, Srutam's ongoing recording notification, at 41:05. */
const Insert: React.FC = () => {
  const f = useT() + SCENES.insert.from;
  const secs = 3 + Math.floor((f - SCENES.insert.from) / 30);
  return (
    <Frame bg={["#7C5134", "#5B3A22"]}>
      <Flashback>
        <Camera f={f} zoom={interpolate(f, span(SCENES.insert), [1, 1.1], { ...clamp, easing: easeInOut })} focus={[540, 900]} drift={2} seed={8}>
          <rect width={W} height={H} fill="#8B5E3C" />
          {Array.from({ length: 9 }, (_, i) => (
            <path key={i} d={`M-40 ${140 + i * 210} C300 ${120 + i * 210} 700 ${170 + i * 210} 1120 ${150 + i * 210}`} stroke="#6B4528" strokeWidth={5} fill="none" opacity={0.35} />
          ))}
          <Phone x={210} y={330} w={620} rot={-5}>
            <LockNotifs w={620} date="Monday, 4 March" time="9:41" notifs={[{ title: "Recording", text: `41:${String(secs).padStart(2, "0")}`, recording: true, ongoing: true, actions: ["Save", "Pause"] }]} />
          </Phone>
          <path d="M-100 -100 L500 -100 L1300 1500 L700 1500 Z" fill="#FFF7ED" opacity={0.08} />
          {Array.from({ length: 14 }, (_, i) => (
            <circle key={i} cx={200 + ((i * 137) % 700) + Math.sin(f / 40 + i) * 30} cy={300 + ((i * 251) % 1200) - ((f * (0.4 + (i % 3) * 0.2)) % 200)} r={2 + (i % 3)} fill="#FFF7ED" opacity={0.5} />
          ))}
        </Camera>
        <LightLeak k={interpolate(f, [SCENES.ask.from - 14, SCENES.ask.from + 4], [0, 1], clamp)} color="#FDE68A" />
      </Flashback>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.18} />
    </Frame>
  );
};

const Q = "What did Prof say will be on the exam?";
const A = "In Monday's Econ lecture (41:05): chapters 4 and 7, plus the metro case study, “twenty marks.” Exam: Thursday, 9:00 am.";

/* 6. 2:11 am, she asks Srutam. The answer comes with its source; she taps it. */
const Ask: React.FC = () => {
  const f = useT() + SCENES.ask.from;
  const qn = Math.round(interpolate(f, [BEAT.type + 2, BEAT.type + 62], [0, Q.length], clamp));
  const an = Math.round(interpolate(f, [BEAT.answer, BEAT.answer + 30], [0, A.length], clamp));
  const tapK = interpolate(f, [BEAT.tapRef, BEAT.tapRef + 12], [0, 1], clamp);
  const thumb = springPt(f, [900, 1900], [{ at: BEAT.tapRef - 10, to: [600, 1290] }, { at: BEAT.tapRef + 8, to: [900, 1900] }], 15, 200);
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.ask), [1, 1.05], clamp)} focus={[540, 900]} drift={3} seed={14}>
        <Depth blur={9}>
          <Room f={f} />
          <Lamp x={760} y={880} />
        </Depth>
        <HeldPhone f={f} y={180} w={700} glow="#3B82F6">
          <AskScreen w={700} q={Q.slice(0, qn)} a={A.slice(0, an)} refs={f >= BEAT.refs ? ["Econ lecture · 41:05"] : []} pressed={f >= BEAT.tapRef && f < BEAT.tapRef + 8} />
        </HeldPhone>
        {tapK > 0 && tapK < 1 && <circle cx={600} cy={1280} r={24 + tapK * 110} fill="none" stroke="#FFFFFF" strokeWidth={8} opacity={1 - tapK} />}
        <g transform={`translate(${thumb[0]} ${thumb[1]}) rotate(-35)`}>
          <path d="M-34 0 C-34 -60 34 -60 34 0 L40 260 L-40 260 Z" fill="#F2C3A2" stroke={LINE} strokeWidth={4} />
          <path d="M-18 -26 C-10 -40 10 -40 18 -26" stroke={LINE} strokeWidth={3} fill="#FBE3D2" />
        </g>
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
    </Frame>
  );
};

/* 7. The lecture plays: "In Thursday's exam: chapters 4 and 7, and the metro case study. Twenty marks." */
const Listen: React.FC = () => {
  const f = useT() + SCENES.listen.from;
  const p0 = BEAT.play;
  const expr = f < BEAT.relief ? "blank" : f < BEAT.yes ? "smug" : "laugh";
  const fist = springPt(f, [130, 900], [{ at: BEAT.yes - 6, to: [110, 620] }], 13, 150);
  const lean = springAt(f, 0, [{ at: p0 + 4, to: 1 }], 16, 90);
  const playing = f >= p0 && f < p0 + 186;
  const PH: Pt = [40 + 545 * 1.45 + 40, 380 + 430 * 1.45 - 120];
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.listen), [1, 1.1], { ...clamp, easing: easeInOut })} focus={[600, 900]} drift={3} seed={16}>
        <Depth blur={8}>
          <Room f={f} />
          <Lamp x={760} y={880} />
        </Depth>
        <Actor
          x={40}
          y={380}
          s={1.45}
          f={f}
          who="meera"
          shadow
          expr={expr}
          tilt={lean * 4 + (f > BEAT.yes ? Math.sin(f / 6) * 1.5 : 0)}
          gaze={f < BEAT.relief ? [0.7, 0.3] : [0, 0]}
          blink={blinkK(f, 1)}
          arms={{ r: { to: [545, 430], pose: "phone", upright: true }, ...(f > BEAT.yes - 8 ? { l: { to: fist, pose: "fist" as const } } : {}) }}
        />
        {playing &&
          [0, 1, 2].map((i) => {
            const k = ((f - p0 + i * 12) % 36) / 36;
            return <path key={i} d={`M${PH[0] + 60 + k * 110} ${PH[1] - 80 - k * 50} q${36 + k * 36} ${80 + k * 40} 0 ${160 + k * 100}`} stroke="#93C5FD" strokeWidth={7} fill="none" opacity={(1 - k) * 0.5} strokeLinecap="round" />;
          })}
        <defs>
          <radialGradient id="screenGlow">
            <stop offset="0%" stopColor="#BFDBFE" stopOpacity={0.4} />
            <stop offset="100%" stopColor="#BFDBFE" stopOpacity={0} />
          </radialGradient>
        </defs>
        <ellipse cx={PH[0] - 120} cy={PH[1] - 60} rx={420} ry={460} fill="url(#screenGlow)" style={{ mixBlendMode: "screen" }} />
      </Camera>
      <Grade warm="#93C5FD" cool="#1E1B4B" from="right" amount={0.16} />
    </Frame>
  );
};

/* 8. She shares it with the group; the replies come in. */
const Share: React.FC = () => {
  const f = useT() + SCENES.share.from;
  const appear = [1, 1, 1, ...[BEAT.mine, ...BEAT.replies].map((at) => springAt(f, 0, [{ at, to: 1 }], 14, 170))];
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.share), [1, 1.05], clamp)} focus={[540, 1000]} drift={3} seed={18}>
        <Depth blur={9}>
          <Room f={f} />
          <Lamp x={760} y={880} />
        </Depth>
        <HeldPhone f={f} y={200}>
          <ChatScreen w={680} title="Econ gang" members="Riya, Kabir, you" messages={CHAT} appear={appear} typing={f > BEAT.replies[0] + 4 && f < BEAT.replies[1] ? "Kabir" : undefined} />
        </HeldPhone>
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
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

/* 9. Knock knock, and the door swings in: Kabir with chai, "Chai's here!"; Riya: "Come on, let's start!" */
const DoorShot: React.FC = () => {
  const f = useT() + SCENES.door.from;
  const raps = [BEAT.knock, BEAT.knock + 6, BEAT.knock + 14];
  const jig = raps.reduce((a, at) => a + (f >= at && f < at + 3 ? 3 : 0), 0);
  const raw = springAt(f, 0, [{ at: BEAT.open, to: 92 }], 13, 120);
  const theta = Math.min(raw, 96);
  const th = (theta * Math.PI) / 180;
  const [hx, top, bot, dw] = [560, 520, 1480, 380];
  const freeX = hx + dw * Math.cos(th);
  const grow = 150 * Math.sin(th);
  const light = Math.sin(th);
  // Kabir walks in from the hallway (legs swing with distance), then stops.
  const kIn = springAt(f, 0, [{ at: BEAT.open + 4, to: 1 }], 20, 60);
  const kx = 660 - 300 * kIn;
  const kWalk = (660 - kx) / 38;
  const rIn = springAt(f, 0, [{ at: BEAT.riyaIn, to: 1 }], 20, 60);
  const rx = 700 - 180 * rIn;
  const rWalk = (700 - rx) / 38;
  const S = 0.52;
  const floor = 1480;
  const yOf = (torso: number) => floor - (800 + torso) * S;
  const meeraTurn = springAt(f, 0, [{ at: BEAT.knock + 4, to: 0.5 }], 14, 120);
  return (
    <Frame bg={["#1E1B4B", "#0B1033"]} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.door), [1, 1.05], clamp)} focus={[700, 1000]} drift={3} seed={18}>
        <rect width={W} height={H} fill="#2A2560" />
        <rect y={1480} width={W} height={H - 1480} fill="#1F1B47" />
        {/* the lit hallway behind the door */}
        <rect x={hx} y={top} width={dw} height={bot - top} fill="#FDE68A" />
        <rect x={hx} y={bot - 120} width={dw} height={120} fill="#F59E0B" opacity={0.4} />
        <path d={`M${hx} ${bot} L${hx + dw} ${bot} L${hx + dw + 360 * light} ${H} L${hx - 300 * light} ${H} Z`} fill="#FDE68A" opacity={0.3 * light} />
        {/* the door panel, hinged on its left edge, swinging toward us */}
        <g transform={`translate(${jig} 0)`}>
          <path d={`M${hx} ${top} L${freeX} ${top - grow} L${freeX} ${bot + grow} L${hx} ${bot} Z`} fill={mix("#9A6B44", "#6B4528", light)} stroke={LINE} strokeWidth={8} />
          {theta < 40 && <circle cx={hx + (dw - 40) * Math.cos(th)} cy={(top + bot) / 2} r={14} fill="#FBBF24" stroke={LINE} strokeWidth={4} />}
        </g>
        <rect x={hx - 24} y={top - 24} width={dw + 48} height={24} fill="#4C3B8F" stroke={LINE} strokeWidth={6} />
        <g opacity={rIn > 0.01 ? 1 : 0}>
          <Actor x={rx} y={yOf(1000)} s={S} f={f} who={RIYA} shadow torso={1000} legs={{ pants: "#1E3A8A", shoes: "#F8FAFC", walk: rWalk }} expr="laugh" viseme={vis("riya-study", f)} tilt={talkTilt(["riya-study"], f, -2, 1.6)} gaze={[-0.8, 0.2]} blink={blinkK(f, 3)} arms={f > 856 ? { r: { to: reach("r", -60, 0.9), pose: "fist" } } : {}} />
        </g>
        <g opacity={kIn > 0.01 ? 1 : 0}>
          <Actor x={kx} y={yOf(1000)} s={S} f={f} who={KABIR} shadow torso={1000} legs={{ pants: "#334155", shoes: "#1F2937", walk: kWalk }} expr="laugh" viseme={vis("kabir-chai", f)} tilt={talkTilt(["kabir-chai"], f, 0, 1.6)} gaze={[-0.8, 0.3]} blink={blinkK(f, 5)} arms={{ l: { to: [160, 880], pose: "grip" }, r: { to: [440, 880], pose: "grip" } }} />
          <Tray x={kx + 300 * S} y={yOf(1000) + 880 * S - 6} f={f} s={S * 1.6} />
        </g>
        {/* Meera at her desk, foreground left, turns to the door */}
        <Actor x={-260} y={1000} s={1.05} f={f} who="meera" shadow expr={f < BEAT.open ? "surprised" : "laugh"} turn={meeraTurn} gaze={[0.9, -0.3]} blink={blinkK(f, 1)} />
        <Table y={1720} color="#3F2A1D" top="#5B3A22" />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E1B4B" from="right" amount={0.18} />
    </Frame>
  );
};

/* 10. Time-lapse, 2:31 to 4:10: chai, Key Points, the sky turning. Smooth, not jittery. */
const Lapse: React.FC = () => {
  const f = useT() + SCENES.lapse.from;
  const lapse = interpolate(f, span(SCENES.lapse), [0, 1], { ...clamp, easing: easeInOut });
  const mins = Math.round(151 + lapse * 99);
  const clock = `${Math.floor(mins / 60)}:${String(mins % 60).padStart(2, "0")}`;
  const cups = [BEAT.clink[0], BEAT.clink[1], 990].map((at) => springAt(f, 0, [{ at, to: 1 }], 12, 170));
  const sway = (seed: number, amp = 3) => Math.sin(f / 9 + seed) * amp;
  const laughing = (seed: number) => Math.sin(f / 14 + seed) > 0.55;
  return (
    <Frame bg={NIGHT} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.lapse), [1, 1.06], clamp)} focus={[540, 1000]} drift={3} seed={20}>
        <Depth blur={4}>
          <Room f={f} dawn={lapse * 0.65} />
          <g transform="translate(820 160)">
            <rect x={-150} y={-66} width={300} height={92} rx={18} fill="#0F172A" stroke={LINE} strokeWidth={5} />
            <text x={0} y={6} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={66} fill="#FBBF24">
              {clock}
            </text>
          </g>
        </Depth>
        <Actor x={-60} y={640} s={0.78} f={f} who={RIYA} shadow torso={120} expr={laughing(1) ? "laugh" : "smile"} tilt={sway(1)} sway={sway(1, 0.4)} gaze={[0.4, 0.5]} blink={blinkK(f, 3)} />
        <Actor x={640} y={640} s={0.78} f={f} who={KABIR} shadow torso={120} expr={laughing(3) ? "laugh" : "smile"} tilt={sway(3)} gaze={[-0.5, 0.4]} blink={blinkK(f, 5)} />
        <Actor x={250} y={600} s={0.82} f={f} who="meera" shadow expr={laughing(5) ? "laugh" : "smug"} tilt={sway(5, 2)} gaze={[0.2, 0.6]} blink={blinkK(f, 1)} />
        <Table y={1300} color="#3F2A1D" top="#5B3A22" />
        {cups.map((c, i) => (
          <g key={i} transform={`translate(${130 + i * 150} 1290) scale(${c}) translate(${-(130 + i * 150)} -1290)`} opacity={c > 0.02 ? 1 : 0}>
            <Kulhad x={130 + i * 150} y={1290} s={0.9} t={f + i * 7} />
          </g>
        ))}
        <Books x={930} y={1300} n={4} />
        <Phone x={600} y={1180} w={420} rot={8} glow="#FBBF24">
          <InsightsScreen
            w={420}
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

/* 11. 8:45 am: the reminder knocks. "Econ exam. Starts in 15 minutes." */
const Remind: React.FC = () => {
  const f = useT() + SCENES.remind.from;
  const on = f >= BEAT.notify;
  const nudge = on ? damped(f, BEAT.notify, 6, 4, 6) : 0;
  return (
    <Frame bg={["#FEF3C7", "#FDE68A"]}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.remind), [1, 1.05], clamp)} focus={[540, 900]} drift={3} seed={22}>
        <Depth blur={10}>
          <rect width={W} height={H} fill="#FDE7C7" />
          {[0, 1, 2].map((i) => (
            <rect key={i} x={80 + i * 340} y={180} width={260} height={560} fill="#FFFBEB" stroke="#E7C9A0" strokeWidth={10} />
          ))}
        </Depth>
        <g transform={`translate(${nudge} 0)`}>
          <Phone x={250} y={300} w={580} rot={-3} glow={on ? "#FBBF24" : undefined}>
            <LockNotifs w={580} date="Thursday, 7 March" time="8:45" notifs={on ? [{ title: "Econ exam", text: "Starts in 15 minutes.", reminder: true }] : []} />
          </Phone>
        </g>
        <GripHand x={300} y={1520} s={1.4} rot={-14} />
        <GripHand x={790} y={1520} s={1.4} rot={14} />
      </Camera>
    </Frame>
  );
};

/* 12. The exam hall: the paper floats down, Q3 is the metro case study. A glance at the friends, a pen spin. */
const Hall: React.FC = () => {
  const f = useT() + SCENES.hall.from;
  const start = BEAT.paperDown - 29;
  const fall = flutter(f, start, [0, -1000], [0, 0], [0, 1500], 3, 26, 1.1);
  const paper: Pt = f < start ? [0, -2000] : fall[1] >= 0 ? [0, 0] : fall;
  const caught = f >= BEAT.catch;
  const penRot = caught ? spin(BEAT.catch, BEAT.spin, 1500, 2.2) : spin(f, BEAT.spin, 1500, 2.2);
  const write = caught ? Math.min(1, (f - BEAT.catch) / 16) : 0;
  const hand: Pt = caught ? [520 + Math.sin(f * 1.1) * 8, 830 + Math.cos(f * 1.5) * 4] : [560, 800];
  const handW: Pt = [150 + hand[0] * 1.2, 260 + hand[1] * 1.2];
  const glance = springAt(f, 0, [{ at: BEAT.nod - 6, to: 1 }, { at: BEAT.spin - 2, to: 0 }], 14, 150);
  return (
    <Frame bg={["#FEF9C3", "#FDE68A"]}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.hall), [1, 1.08], { ...clamp, easing: easeInOut })} focus={[540, 1200]} drift={3} seed={24}>
        <Depth blur={6}>
          <rect width={W} height={H} fill="#FDF2DC" />
          {[0, 1, 2].map((i) => (
            <rect key={i} x={60 + i * 340} y={120} width={280} height={520} fill="#FFFBEB" stroke="#E7C9A0" strokeWidth={10} />
          ))}
          <Actor x={-120} y={520} s={0.55} f={f} who={RIYA} torso={200} expr={f > BEAT.nod ? "smile" : "neutral"} gaze={[0.6, 0.3]} />
          <Actor x={780} y={520} s={0.55} f={f} who={KABIR} torso={200} expr={f > BEAT.nod ? "laugh" : "neutral"} gaze={[-0.6, 0.3]} arms={f > BEAT.nod ? { l: { to: [60, 440], pose: "thumb", upright: true } } : {}} />
        </Depth>
        <Actor
          x={150}
          y={260}
          s={1.2}
          f={f}
          who="meera"
          shadow
          torso={120}
          expr={f < BEAT.circle ? "blank" : "smug"}
          tilt={caught ? 3 : 0}
          turn={glance * 0.2}
          gaze={glance > 0.5 ? [0.9, -0.1] : [0.1, 0.9]}
          blink={blinkK(f, 1)}
          arms={{ r: { to: hand, pose: "grip", rot: -30 } }}
        />
        <Table y={1240} color="#B08155" top="#C9976A" />
        <g transform={`translate(${paper[0]} ${paper[1]}) rotate(${-3 + paper[0] * 0.15} 540 1500)`}>
          <rect x={200} y={1290} width={680} height={540} fill="#FFFDF8" stroke={LINE} strokeWidth={5} />
          <text x={240} y={1380} fontFamily="Kalam" fontWeight={700} fontSize={44} fill="#1F2937">
            <tspan x={240}>Q1. Explain Chapter 4…</tspan>
            <tspan x={240} dy={74}>
              Q2. Chapter 7: public…
            </tspan>
            <tspan x={240} dy={74}>
              Q3. Metro case study
            </tspan>
            <tspan x={240} dy={74}>
              {"      (20 marks)"}
            </tspan>
          </text>
          {f > BEAT.circle && <ellipse cx={500} cy={1560} rx={290 * Math.min(1, (f - BEAT.circle) / 8)} ry={70} fill="none" stroke="#DC2626" strokeWidth={6} />}
          {write > 0 && <path d="M260 1760 q30 -20 60 0 t60 0 t60 0 t60 0 t60 0" stroke="#1D4ED8" strokeWidth={5} fill="none" strokeDasharray={400} strokeDashoffset={400 * (1 - write)} />}
        </g>
        <g transform={`translate(${handW[0]} ${handW[1]}) rotate(${penRot - 30})`}>
          <rect x={-8} y={-110} width={16} height={190} rx={6} fill="#1D4ED8" stroke={LINE} strokeWidth={3} />
          <path d="M-8 80 L0 104 L8 80 Z" fill="#E5E7EB" stroke={LINE} strokeWidth={2} />
        </g>
      </Camera>
    </Frame>
  );
};

/* 13. Out of the hall: the three laugh, the question papers go up. "Listen once. Ask anytime." */
const End: React.FC = () => {
  const f = useT() + SCENES.end.from;
  const band = interpolate(f, [BEAT.band, BEAT.band + 20], [0, 1], { ...clamp, easing: easeOut });
  const bob = (seed: number) => Math.abs(Math.sin((f + seed) / 6)) * 8;
  const raise = springPt(f, [470, 900], [{ at: 1134, to: [540, 420] }], 12, 140);
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <Camera f={f} drift={3} seed={26}>
        <Depth blur={5}>
          <rect width={W} height={H} fill="#FCE7C8" />
          {[0, 1, 2].map((i) => (
            <rect key={i} x={60 + i * 340} y={160} width={280} height={520} fill="#FFFBEB" stroke="#E7C9A0" strokeWidth={10} />
          ))}
        </Depth>
        <g transform={`translate(0 ${-bob(0)})`}>
          <Actor x={-170} y={620} s={0.85} f={f} who={RIYA} shadow torso={300} expr="laugh" tilt={-3} />
        </g>
        <g transform={`translate(0 ${-bob(7)})`}>
          <Actor x={650} y={620} s={0.85} f={f} who={KABIR} shadow torso={300} expr="laugh" tilt={3} />
        </g>
        <Actor x={230} y={560} s={1.0} f={f} who="meera" shadow torso={300} expr="laugh" viseme={vis("e-tagline", f)} tilt={talkTilt(["e-tagline"], f, 0, 1.2)} blink={blinkK(f, 1)} arms={{ r: { to: raise, pose: "phone", upright: true } }} />
        {[0, 1, 2, 3].map((i) => {
          const at = 1132 + i * 4;
          if (f < at) return null;
          const dir = i % 2 === 0 ? 1 : -1;
          const p = flutter(f, at, [540 + dir * 330, 1300], [dir * (1100 + i * 120), -2000 - i * 150], [dir * 120, 300], 2.0, 40, 1.1);
          const t = (f - at) / 30;
          return (
            <g key={i} transform={`translate(${p[0]} ${p[1]}) rotate(${dir * t * 160 + i * 30}) scale(${Math.cos(t * 4 + i)} 1)`}>
              <rect x={-70} y={-90} width={140} height={180} fill="#FFFDF8" stroke={LINE} strokeWidth={4} />
              <path d="M-48 -50 h96 M-48 -18 h76 M-48 14 h96" stroke="#94A3B8" strokeWidth={5} />
            </g>
          );
        })}
      </Camera>
      <g transform={`translate(0 ${(1 - band) * 420})`}>
        <EndBand tagline="Listen once. Ask anytime." />
      </g>
    </Frame>
  );
};

const LIST = [
  ["1 2:07 am", SCENES.night, Night],
  ["2 Group chat", SCENES.chat, Chat],
  ["3 She was asleep too", SCENES.guilt, Guilt],
  ["4 Lecture (flashback)", SCENES.lecture, Lecture],
  ["5 Recording insert", SCENES.insert, Insert],
  ["6 Ask Srutam", SCENES.ask, Ask],
  ["7 The lecture plays", SCENES.listen, Listen],
  ["8 Share", SCENES.share, Share],
  ["9 Chai's here", SCENES.door, DoorShot],
  ["10 Time-lapse", SCENES.lapse, Lapse],
  ["11 Reminder", SCENES.remind, Remind],
  ["12 Exam hall", SCENES.hall, Hall],
  ["13 End", SCENES.end, End],
] as const;

export const ExamVideo: React.FC = () => {
  const f = useT();
  // Old-film warmth through the flashback (shots 4-5).
  const sepia = interpolate(f, [SCENES.lecture.from, SCENES.lecture.from + 16, SCENES.ask.from - 10, SCENES.ask.from], [0, 0.5, 0.5, 0], clamp);
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <AbsoluteFill style={{ filter: sepia > 0 ? `sepia(${sepia})` : undefined }}>
        {LIST.map(([name, s, Comp]) => (
          <Sequence key={name} name={name} from={s.from * K} durationInFrames={s.duration * K}>
            <Comp />
          </Sequence>
        ))}
      </AbsoluteFill>
      <Captions cues={CAPTIONS} k={K} />
      <Soundtrack music="exam/music2.wav" musicVolume={0.42} lines={VO} sfx={SFX} k={K} sfxGain={0.6} duck={0.35} />
    </AbsoluteFill>
  );
};


