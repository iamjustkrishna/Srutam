// "2 a.m. Before the Exam": the exam is at nine and nobody remembers what Prof
// said would come. Meera dozed through that lecture too, but Srutam recorded it.
// One question, the answer with its source, and the lecture plays the moment back.
import { useId } from "react";
import { AbsoluteFill, interpolate, Sequence, useCurrentFrame } from "remotion";
import { clamp, easeIn, easeInOut, easeOut } from "../anim";
import { Kulhad, Zzz } from "../bedtime/Props";
import { KABIR, PROF, RIYA } from "../cast/cast";
import { Actor, blinkAt, reach, type Pt } from "../cast/Rig";
import { Camera, Depth, Grade, LightLeak } from "../stories/Camera";
import { ballistic, damped, flutter, follow, spin, springAt, springPt } from "../stories/physics";
import { about, Captions, pop, shake, Soundtrack } from "../stories/Story";
import { voice } from "../stories/voice";
import { AskScreen, Board, Books, Bubble, Burst, Clock, EndBand, Frame, GripHand, H, InsightsScreen, Lamp, LINE, LockNotifs, Phone, Sfx, Table, W, Window } from "../storyboards/Kit";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";
import { VISEMES } from "./visemes";

const { vis, talkTilt } = voice(VO, VISEMES);

/** Eyes darting in a panic: a new target every ~half second. */
const dart = (f: number, seed = 0): [number, number] => {
  const k = Math.floor((f + seed * 7) / 14);
  return [Math.sin(k * 12.9898 + seed) * 0.7, -0.15 + Math.cos(k * 7.233 + seed) * 0.3];
};

/** The hostel room at night (dawn 0..1 lightens the sky for the time-lapse). */
const Room: React.FC<{ dawn?: number; f: number }> = ({ dawn = 0, f }) => {
  const sky: [string, string] = dawn > 0 ? [mix("#0B1033", "#F9A8D4", dawn * 0.6), mix("#1E1B4B", "#FDBA74", dawn * 0.7)] : ["#0B1033", "#1E1B4B"];
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

const mix = (a: string, b: string, k: number) => {
  const p = (h: string) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const [x, y] = [p(a), p(b)];
  return `#${x.map((v, i) => Math.round(v + (y[i] - v) * k).toString(16).padStart(2, "0")).join("")}`;
};

/** Flashback look: a ripple that settles (k 0 -> 1) and a sepia wash. */
const Flashback: React.FC<{ k?: number; children: React.ReactNode }> = ({ k = 1, children }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const scale = 110 * (1 - k);
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
      {k < 1 && <rect width={W} height={H} fill="#FFF7ED" opacity={(1 - k) * 0.6} />}
    </g>
  );
};

/** A phone lying on a desk, seen at an angle, recording (the red dot breathes). */
const DeskPhone: React.FC<{ x: number; y: number; s?: number; f: number }> = ({ x, y, s = 1, f }) => {
  const dot = 0.5 + 0.5 * Math.sin(f / 6);
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <path d="M-70 -18 L70 -18 L84 18 L-84 18 Z" fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
      <path d="M-62 -13 L62 -13 L72 13 L-72 13 Z" fill="#1E2A5A" />
      <circle cx={-40} cy={0} r={7} fill="#EF4444" opacity={0.4 + 0.6 * dot} />
      <circle cx={-40} cy={0} r={18} fill="#EF4444" opacity={0.2 * dot} />
    </g>
  );
};

/* 1. 2:07 am. ECU on the desk clock, pull out to Meera: "Exam at nine. And I forgot what Prof said." */
const Night: React.FC = () => {
  const f = useCurrentFrame() + SCENES.night.from;
  const k = interpolate(f, [6, 80], [0, 1], { ...clamp, easing: easeInOut });
  const clockAt: Pt = [470, 1420];
  const zoom = 3.3 + (1 - 3.3) * k;
  const center: Pt = [540 + (clockAt[0] - 540) * k, 960 + (clockAt[1] - 960) * k];
  const lArm = springPt(f, [40, 800], [{ at: BEAT.handHead, to: [215, 345] }], 13, 110);
  const rArm = springPt(f, [580, 800], [{ at: BEAT.grab, to: reach("r", -12, 0.9) }], 12, 160);
  const flicker = 0.92 + 0.08 * Math.sin(f * 1.7) * Math.sin(f * 0.37);
  return (
    <Frame bg={["#0B1033", "#1E1B4B"]} night>
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
          expr="worried"
          viseme={vis("e-forgot", f)}
          tilt={talkTilt(["e-forgot"], f, f > BEAT.handHead ? -4 : 0, 1.6)}
          sway={follow((g) => talkTilt(["e-forgot"], g, 0, 1.6), f, 4, 0.3)}
          gaze={f < BEAT.grab ? dart(f, 1) : [0.9, 0.3]}
          blink={blinkAt(f, 1)}
          arms={{ l: { to: lArm, pose: "open", rot: f > BEAT.handHead - 6 ? 20 : 0 }, r: { to: rArm, pose: f > BEAT.grab ? "open" : "fist" } }}
        />
        <Table y={1500} color="#3F2A1D" top="#5B3A22" />
        <path d="M330 1534 L760 1534 L728 1494 L362 1494 Z" fill="#FFFDF8" stroke={LINE} strokeWidth={3} />
        <path d="M545 1494 L545 1534" stroke="#CBD5E1" strokeWidth={3} />
        <Books x={860} y={1500} n={7} />
        <g transform={`translate(${clockAt[0]} ${clockAt[1]})`}>
          <rect x={-150} y={-62} width={300} height={92} rx={18} fill="#0F172A" stroke={LINE} strokeWidth={5} />
          <text x={-22} y={8} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={62} fill="#EF4444">
            {f % 30 < 15 ? "2:07" : "2 07"}
          </text>
          <text x={96} y={6} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={24} fill="#EF4444">
            AM
          </text>
          <ellipse cx={0} cy={-80} rx={170} ry={40} fill="#EF4444" opacity={0.12} />
        </g>
      </Camera>
      <Grade warm="#FCD34D" cool="#1E1B4B" from="right" amount={0.2} />
    </Frame>
  );
};

/* 2. Panic: she tears through every notebook; pages fly, drag and flutter down. */
const Papers: React.FC = () => {
  const f = useCurrentFrame() + SCENES.papers.from;
  const flips = BEAT.flip;
  const hand = (side: "l" | "r") => {
    const desk: Pt = side === "r" ? [470, 880] : [130, 880];
    const up: Pt = side === "r" ? [600, 560] : [0, 560];
    const keys = flips
      .map((at, i) => ({ at, i }))
      .filter(({ i }) => (i % 2 === 0) === (side === "r"))
      .flatMap(({ at }) => [
        { at: at - 5, to: up },
        { at: at + 3, to: desk },
      ]);
    return springPt(f, desk, keys, 14, 260);
  };
  const push = interpolate(f, [150, 230], [1, 1.08], clamp);
  const sh: [number, number] = [shake(f, flips as unknown as number[], 5), 0];
  return (
    <Frame bg={["#1E1B4B", "#312E81"]} night>
      <Camera f={f} zoom={push} focus={[540, 1000]} shake={sh} drift={4} seed={5}>
        <Depth blur={5}>
          <Room f={f} />
        </Depth>
        <Actor
          x={-60}
          y={480}
          s={1.4}
          f={f}
          who="meera"
          shadow
          expr={f < 180 ? "surprised" : "worried"}
          viseme={f > 170 && f < 196 ? "D" : undefined}
          gaze={dart(f, 3)}
          tilt={Math.sin(f / 3) * 2}
          sway={Math.sin(f / 4) * 0.6}
          arms={{ l: { to: hand("l"), pose: "open" }, r: { to: hand("r"), pose: "open" } }}
        />
        <Table y={1600} color="#3F2A1D" top="#5B3A22" />
        {[0, 1, 2].map((i) => (
          <rect key={i} x={250 + i * 14} y={1560 - i * 26} width={560} height={30} rx={4} fill={["#1D4ED8", "#B91C1C", "#047857"][i]} stroke={LINE} strokeWidth={3} />
        ))}
        {flips.map((at, i) => {
          if (f < at) return null;
          const dir = i % 2 === 0 ? 1 : -1;
          const p = flutter(f, at, [540 + dir * 120, 1500], [dir * (420 + i * 70), -1700 - (i % 3) * 220]);
          const t = (f - at) / 30;
          const tumble = Math.cos(t * 5 + i);
          const rot = dir * (t * 140 + i * 20) + Math.sin(t * 6) * 18;
          return p[1] < 2200 ? (
            <g key={i} transform={`translate(${p[0]} ${p[1]}) rotate(${rot}) scale(${tumble} 1)`}>
              <rect x={-90} y={-115} width={180} height={230} fill="#FFFDF8" stroke={LINE} strokeWidth={4} />
              <path d="M-62 -70 h124 M-62 -30 h100 M-62 10 h124 M-62 50 h80" stroke="#94A3B8" strokeWidth={6} />
            </g>
          ) : null;
        })}
      </Camera>
      <g transform={about(540, 230, pop(f, 168, 10))} opacity={f >= 168 ? 1 : 0}>
        <Bubble x={140} y={110} w={800} lines={["Which chapters", "did he say?!"]} tail={[520, 620]} size={66} />
      </g>
      <Grade warm="#FCD34D" cool="#1E1B4B" from="right" amount={0.18} />
    </Frame>
  );
};

const BOARD = ["Exam:", "Ch. 4, Ch. 7", "+ metro case (20)"];

/* 3. Monday, sepia: Prof writes on the board; Kabir and Riya asleep, Meera dozing. Her phone records. */
const Lecture: React.FC = () => {
  const f = useCurrentFrame() + SCENES.lecture.from;
  const k = interpolate(f, [230, 252], [0, 1], { ...clamp, easing: easeOut });
  // Prof taps the board with a pointer: the hand springs on each tap, the stick follows.
  const P = { x: 620, y: 300, s: 0.66 };
  const hand = springPt(f, [60, 430], [{ at: 258, to: [40, 400] }, { at: 262, to: [60, 430] }, { at: 286, to: [40, 400] }, { at: 290, to: [60, 430] }], 10, 260);
  const hw: Pt = [P.x + hand[0] * P.s, P.y + hand[1] * P.s];
  const tip: Pt = [hw[0] - 150, hw[1] - 150 + damped(f, 262, 6, 4, 6) + damped(f, 290, 6, 4, 6)];
  const doze = (seed: number) => ((f + seed) % 70) / 70;
  const meeraNod = doze(0) < 0.85 ? doze(0) * 10 : 10 - (doze(0) - 0.85) * 66;
  return (
    <Frame bg={["#E9D8B4", "#C9AE82"]}>
      <Flashback k={k}>
        <Camera f={f} zoom={interpolate(f, [230, 308], [1.04, 1.1], clamp)} focus={[540, 900]} drift={3} seed={6}>
          <Depth blur={1.5}>
            <rect width={W} height={H} fill="#E2CFA8" />
            <Board x={40} y={150} w={1000} h={440} chalk lines={BOARD} />
            <path d={`M${hw[0] + 30} ${hw[1] + 30} L${tip[0]} ${tip[1]}`} stroke="#7C4A21" strokeWidth={9} strokeLinecap="round" />
            <Actor x={P.x} y={P.y} s={P.s} f={f} who={PROF} torso={620} expr="talk" viseme={f % 40 < 20 ? "C" : "B"} turn={-0.25} gaze={[-0.6, -0.4]} tilt={-3} arms={{ l: { to: hand, pose: "grip", rot: 20 } }} />
          </Depth>
          <Table y={1380} color="#7C5134" top="#A9744D" />
          <Actor x={-110} y={930} s={0.68} f={f} who={KABIR} shadow expr="sleepy" tilt={-12 + damped(f, 240, 3, 0.5, 0.2)} />
          <Actor x={270} y={960} s={0.64} f={f} who="meera" shadow expr="drowsy" tilt={meeraNod} sway={-meeraNod * 0.05} />
          <Actor x={600} y={930} s={0.68} f={f} who={RIYA} shadow expr="sleepy" tilt={10 + Math.sin(f / 25) * 2} />
          <Zzz x={150} y={1080} t={f} />
          <Zzz x={850} y={1080} t={f + 17} />
          <DeskPhone x={540} y={1420} s={1.1} f={f} />
        </Camera>
      </Flashback>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.18} />
    </Frame>
  );
};

/* 4. Insert: the phone on her desk, Srutam's ongoing recording notification ticking. */
const Insert: React.FC = () => {
  const f = useCurrentFrame() + SCENES.insert.from;
  const secs = 12 + Math.floor((f - 308) / 30);
  const motes = Array.from({ length: 14 }, (_, i) => {
    const x = 200 + ((i * 137) % 700) + Math.sin(f / 40 + i) * 30;
    const y = 300 + ((i * 251) % 1200) - ((f * (0.4 + (i % 3) * 0.2)) % 200);
    return <circle key={i} cx={x} cy={y} r={2 + (i % 3)} fill="#FFF7ED" opacity={0.5} />;
  });
  return (
    <Frame bg={["#7C5134", "#5B3A22"]}>
      <Flashback>
        <Camera f={f} zoom={interpolate(f, [308, 380], [1, 1.1], { ...clamp, easing: easeInOut })} focus={[540, 900]} drift={2} seed={8}>
          <rect width={W} height={H} fill="#8B5E3C" />
          {Array.from({ length: 9 }, (_, i) => (
            <path key={i} d={`M-40 ${140 + i * 210} C300 ${120 + i * 210} 700 ${170 + i * 210} 1120 ${150 + i * 210}`} stroke="#6B4528" strokeWidth={5} fill="none" opacity={0.35} />
          ))}
          <g transform="translate(820 1600) rotate(28)">
            <rect x={-12} y={-180} width={24} height={360} rx={10} fill="#1D4ED8" stroke={LINE} strokeWidth={4} />
            <path d="M-12 180 L0 214 L12 180 Z" fill="#F5D0A9" stroke={LINE} strokeWidth={3} />
          </g>
          <Phone x={210} y={330} w={620} rot={-5}>
            <LockNotifs w={620} date="Monday, 4 March" time="9:41" notifs={[{ title: "Recording", text: `40:${String(secs).padStart(2, "0")}`, recording: true, ongoing: true, actions: ["Save", "Pause"] }]} />
          </Phone>
          <path d="M-100 -100 L500 -100 L1300 1500 L700 1500 Z" fill="#FFF7ED" opacity={0.08} />
          {motes}
        </Camera>
      </Flashback>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.18} />
    </Frame>
  );
};

/* 5. MCU Prof: "Chapters 4 and 7... and the metro case study. Twenty marks." Palm beats on the stresses. */
const Prof: React.FC = () => {
  const f = useCurrentFrame() + SCENES.prof.from;
  const beats = BEAT.palm.map((at, i) => ({ at, to: (i % 2 === 0 ? [575, 455] : [620, 540]) as Pt }));
  const palm = springPt(f, [560, 760], beats, 11, 200);
  return (
    <Frame bg={["#E9D8B4", "#C9AE82"]}>
      <Flashback>
        <Camera f={f} zoom={interpolate(f, [380, 472], [1, 1.07], clamp)} focus={[540, 900]} drift={3} seed={10}>
          <Depth blur={5}>
            <rect width={W} height={H} fill="#E2CFA8" />
            <Board x={-80} y={120} w={1240} h={520} chalk lines={BOARD} />
          </Depth>
          <Actor
            x={30}
            y={420}
            s={1.4}
            f={f}
            who={PROF}
            shadow
            torso={200}
            expr="talk"
            viseme={vis("prof-chapters", f)}
            tilt={talkTilt(["prof-chapters"], f, 0, 1.8)}
            turn={0.12}
            gaze={[0.2, 0]}
            blink={blinkAt(f, 4)}
            arms={{ r: { to: palm, pose: "palm", rot: -20 } }}
          />
        </Camera>
      </Flashback>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.18} />
    </Frame>
  );
};

/* 6. The class, still asleep: Kabir's head slips off his hand. The phone keeps recording. */
const Class: React.FC = () => {
  const f = useCurrentFrame() + SCENES.class.from;
  const slip = f >= BEAT.headDrop;
  const kHand = springPt(f, [380, 560], [{ at: BEAT.headDrop, to: [470, 820] }], 9, 220);
  const kTilt = slip ? -26 + damped(f, BEAT.headDrop, 8, 2.2, 4) : -8;
  const kDrop = slip ? Math.min(60, (f - BEAT.headDrop) * 12) - damped(f, BEAT.headDrop + 5, 14, 2.4, 5) : 0;
  const push = interpolate(f, [512, 550], [1, 1.5], { ...clamp, easing: easeIn });
  const doze = ((f % 70) / 70) * 8;
  return (
    <Frame bg={["#E9D8B4", "#C9AE82"]}>
      <Flashback k={1}>
        <Camera f={f} zoom={push} focus={[560, 1380]} drift={3} seed={12}>
          <Depth blur={6}>
            <rect width={W} height={H} fill="#E2CFA8" />
            <Board x={40} y={100} w={1000} h={420} chalk lines={BOARD} />
          </Depth>
          <g transform={`translate(0 ${kDrop})`}>
            <Actor x={-180} y={560} s={1.0} f={f} who={KABIR} shadow expr="sleepy" tilt={kTilt} arms={{ r: { to: kHand, pose: "fist", rot: slip ? 0 : -80 } }} />
          </g>
          <Actor x={520} y={610} s={0.9} f={f} who="meera" shadow expr="drowsy" tilt={doze} turn={-0.1} />
          <Zzz x={140} y={720} t={f} />
          <Table y={1340} color="#7C5134" top="#A9744D" />
          <DeskPhone x={560} y={1385} s={1.6} f={f} />
        </Camera>
        <LightLeak k={interpolate(f, [BEAT.rippleBack, 552], [0, 1], clamp)} color="#FDE68A" />
      </Flashback>
    </Frame>
  );
};

const Q = "What did Prof say will be on the exam?";
const A = "In Monday's lecture: chapters 4 and 7, plus the metro case study, “twenty marks.”";

/* 7. 2:11 am, back in her room: she asks Srutam. The answer comes with its source. */
const Ask: React.FC = () => {
  const f = useCurrentFrame() + SCENES.ask.from;
  const qn = Math.round(interpolate(f, [BEAT.type + 4, BEAT.type + 60], [0, Q.length], clamp));
  const an = Math.round(interpolate(f, [BEAT.answer, BEAT.answer + 34], [0, A.length], clamp));
  const lift = springAt(f, 0, [{ at: 552, to: 1 }], 12, 120);
  const y = 260 + (1 - lift) * 500;
  const tapK = interpolate(f, [BEAT.tapRef, BEAT.tapRef + 12], [0, 1], clamp);
  const thumb = springPt(f, [880, 1700], [{ at: BEAT.tapRef - 8, to: [640, 1180] }, { at: BEAT.tapRef + 8, to: [900, 1560] }], 14, 220);
  return (
    <Frame bg={["#0B1033", "#050714"]} night>
      <Camera f={f} zoom={interpolate(f, [552, 690], [1, 1.05], clamp)} focus={[540, 900]} drift={3} seed={14}>
        <Depth blur={8}>
          <Room f={f} />
          <Lamp x={760} y={880} />
        </Depth>
        <Phone x={180} y={y} w={720} rot={-2 + (1 - lift) * 6} glow="#3B82F6">
          <AskScreen w={720} q={Q.slice(0, qn)} a={A.slice(0, an)} refs={f >= BEAT.refs ? ["Econ lecture · 41:05"] : []} pressed={f >= BEAT.tapRef && f < BEAT.tapRef + 8} />
        </Phone>
        {tapK > 0 && tapK < 1 && <circle cx={640} cy={1150} r={24 + tapK * 110} fill="none" stroke="#FFFFFF" strokeWidth={8} opacity={1 - tapK} />}
        <GripHand x={190} y={y + 1440} s={1.5} rot={-12} />
        <g transform={`translate(${thumb[0]} ${thumb[1]}) rotate(-35)`}>
          <path d="M-34 0 C-34 -60 34 -60 34 0 L40 260 L-40 260 Z" fill="#F2C3A2" stroke={LINE} strokeWidth={4} />
          <path d="M-18 -26 C-10 -40 10 -40 18 -26" stroke={LINE} strokeWidth={3} fill="#FBE3D2" />
        </g>
        <GripHand x={890} y={y + 1440} s={1.5} rot={12} />
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
    </Frame>
  );
};

/* 8. She listens: the lecture itself, "...and the metro case study. Twenty marks." Relief, then YES. */
const Listen: React.FC = () => {
  const f = useCurrentFrame() + SCENES.listen.from;
  const p0 = BEAT.play;
  const expr = f < BEAT.relief ? "blank" : f < BEAT.yes ? "smug" : "laugh";
  const fist = springPt(f, [130, 900], [{ at: BEAT.yes - 4, to: [70, 300] }], 8, 200);
  const stretch = f < BEAT.yes ? 1 : 1 + damped(f, BEAT.yes, 0.06, 2.5, 5) + 0.02;
  const phoneHand: Pt = [545, 430];
  const playing = f >= p0 && f < p0 + 92;
  const PH: Pt = [40 + 545 * 1.45 + 40, 380 + 430 * 1.45 - 120];
  return (
    <Frame bg={["#0B1033", "#1E1B4B"]} night>
      <Camera f={f} zoom={interpolate(f, [690, 790], [1, 1.08], clamp)} focus={[540, 900]} drift={3} seed={16}>
        <Depth blur={7}>
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
          squash={stretch}
          tilt={f > BEAT.yes ? -4 + Math.sin(f / 3) * 2 : 0}
          sway={f > BEAT.yes ? Math.sin(f / 3) * 0.6 : 0}
          gaze={f < BEAT.relief ? [0.6, 0.4] : [0, 0]}
          blink={blinkAt(f, 1)}
          arms={{ r: { to: phoneHand, pose: "phone", upright: true }, ...(f > BEAT.yes - 6 ? { l: { to: fist, pose: "fist" as const } } : {}) }}
        />
        {playing &&
          [0, 1, 2].map((i) => {
            const k = ((f - p0 + i * 10) % 30) / 30;
            return <path key={i} d={`M${PH[0] + 60 + k * 120} ${PH[1] - 80 - k * 60} q${40 + k * 40} ${80 + k * 50} 0 ${160 + k * 120}`} stroke="#93C5FD" strokeWidth={8} fill="none" opacity={(1 - k) * 0.8} strokeLinecap="round" />;
          })}
        <defs>
          <radialGradient id="screenGlow">
            <stop offset="0%" stopColor="#BFDBFE" stopOpacity={0.45} />
            <stop offset="100%" stopColor="#BFDBFE" stopOpacity={0} />
          </radialGradient>
        </defs>
        <ellipse cx={PH[0] - 120} cy={PH[1] - 60} rx={420} ry={460} fill="url(#screenGlow)" style={{ mixBlendMode: "screen" }} />
      </Camera>
      <g transform={about(250, 560, pop(f, BEAT.yes, 10))} opacity={f >= BEAT.yes ? 1 : 0}>
        <Burst x={250} y={560} r={150} color="#FBBF24" n={12} />
        <Sfx x={250} y={600} text="YES!" size={130} rot={-8} />
      </g>
      <Grade warm="#93C5FD" cool="#1E1B4B" from="right" amount={0.16} />
    </Frame>
  );
};

/* 9. Knock knock: the door bursts open, hallway light floods in. Riya: "You recorded it, right?!" */
const DoorShot: React.FC = () => {
  const f = useCurrentFrame() + SCENES.door.from;
  const raps = [BEAT.knock, BEAT.knock + 6, BEAT.knock + 14];
  const jig = raps.reduce((a, at) => a + (f >= at && f < at + 3 ? 4 : 0), 0);
  const open = BEAT.bang - 4;
  const raw = springAt(f, 0, [{ at: open, to: 96 }], 9, 260);
  const theta = raw > 96 ? 96 - (raw - 96) * 0.6 : raw;
  const th = (theta * Math.PI) / 180;
  const [hx, top, bot, dw] = [560, 520, 1480, 380];
  const freeX = hx + dw * Math.cos(th);
  const grow = 150 * Math.sin(th);
  const light = Math.sin(th);
  const riyaIn = springAt(f, 0, [{ at: open + 4, to: 1 }], 12, 140);
  const hop = f > open + 4 && f < open + 22 ? ballistic(f, open + 4, [0, 0], [0, -700], 4200)[1] : 0;
  const land = f >= open + 22 ? 1 - damped(f, open + 22, 0.07, 3, 7) : 1;
  const kabirIn = springAt(f, 0, [{ at: open + 14, to: 1 }], 12, 140);
  const meeraTurn = springAt(f, 0, [{ at: BEAT.knock + 4, to: 0.5 }], 12, 150);
  const zoom = interpolate(f, [790, 880], [1, 1.05], clamp);
  return (
    <Frame bg={["#1E1B4B", "#0B1033"]} night>
      <Camera f={f} zoom={zoom} shake={[shake(f, [BEAT.bang], 12), 0]} focus={[700, 1000]} drift={3} seed={18}>
        <rect width={W} height={H} fill="#2A2560" />
        <rect y={1480} width={W} height={H - 1480} fill="#1F1B47" />
        {/* the hallway behind the door */}
        <rect x={hx} y={top} width={dw} height={bot - top} fill="#FDE68A" />
        <rect x={hx} y={bot - 120} width={dw} height={120} fill="#F59E0B" opacity={0.4} />
        <path d={`M${hx} ${bot} L${hx + dw} ${bot} L${hx + dw + 360 * light} ${H} L${hx - 300 * light} ${H} Z`} fill="#FDE68A" opacity={0.35 * light} />
        <g opacity={kabirIn > 0.02 ? 1 : 0} transform={`translate(${(1 - kabirIn) * 220} 0)`}>
          <Actor x={600} y={560} s={0.62} f={f} who={KABIR} shadow torso={600} expr="laugh" gaze={[-0.8, 0.2]} blink={blinkAt(f, 5)} />
        </g>
        {/* the door panel, hinged on its left edge, swinging toward us */}
        <g transform={`translate(${jig} 0)`}>
          <path d={`M${hx} ${top} L${freeX} ${top - grow} L${freeX} ${bot + grow} L${hx} ${bot} Z`} fill={mix("#9A6B44", "#6B4528", light)} stroke={LINE} strokeWidth={8} />
          {theta < 40 && <circle cx={hx + (dw - 40) * Math.cos(th)} cy={(top + bot) / 2} r={14} fill="#FBBF24" stroke={LINE} strokeWidth={4} />}
        </g>
        <g transform={`translate(${-60 + (1 - riyaIn) * 260} ${hop})`}>
          <g transform={`translate(560 1500) scale(${1 / Math.sqrt(land)} ${land}) translate(-560 -1500)`} opacity={riyaIn > 0.02 ? 1 : 0}>
            <Actor x={340} y={600} s={0.62} f={f} who={RIYA} shadow torso={560} expr="surprised" viseme={vis("riya-record", f, "A")} tilt={talkTilt(["riya-record"], f, -3, 2)} gaze={[-0.8, 0.1]} blink={blinkAt(f, 3)} arms={{ l: { to: reach("l", 140, 0.9), pose: "point" } }} />
          </g>
        </g>
        <rect x={hx - 24} y={top - 24} width={dw + 48} height={24} fill="#4C3B8F" stroke={LINE} strokeWidth={6} />
        {/* Meera at her desk, foreground left, turns to the door */}
        <Actor x={-260} y={1000} s={1.05} f={f} who="meera" shadow expr={f < BEAT.bang ? "surprised" : "laugh"} turn={meeraTurn} gaze={[0.9, -0.3]} blink={blinkAt(f, 1)} arms={{ r: { to: springPt(f, [460, 880], [{ at: 832, to: [560, 440] }], 11, 160), pose: "phone", upright: true } }} />
        <Table y={1720} color="#3F2A1D" top="#5B3A22" />
        {f < BEAT.bang + 10 && (
          <g opacity={f >= BEAT.knock ? 1 : 0} transform={about(760, 380, pop(f, BEAT.knock, 8))}>
            <Sfx x={760} y={400} text="knock knock" size={96} rot={-4} />
          </g>
        )}
      </Camera>
      <Grade warm="#FDE68A" cool="#1E1B4B" from="right" amount={0.18} />
    </Frame>
  );
};

/* 10. "I'll get the chai!" Kabir dashes out and back; then a time-lapse, 2:15 to 3:40. */
const Lapse: React.FC = () => {
  const f = useCurrentFrame() + SCENES.lapse.from;
  const lapse = interpolate(f, [BEAT.back, 1004], [0, 1], clamp);
  const mins = Math.round(135 + lapse * 85);
  const clock = `${Math.floor(mins / 60)}:${String(mins % 60).padStart(2, "0")}`;
  const away = f >= BEAT.dash && f < BEAT.back;
  const kx = f < BEAT.dash ? 0 : f < BEAT.back ? interpolate(f, [BEAT.dash, BEAT.dash + 8], [0, 900], { ...clamp, easing: easeIn }) : interpolate(f, [BEAT.back, BEAT.back + 8], [-700, 0], { ...clamp, easing: easeOut });
  const step = Math.floor(f / 5);
  const jitter = (seed: number) => (f > BEAT.back + 10 ? Math.sin(step * 3.1 + seed) * 6 : 0);
  const poses = ["laugh", "talk", "smile", "surprised"] as const;
  const pose = (seed: number) => (f > BEAT.back + 10 ? poses[(step + seed) % poses.length] : "smile");
  const cups = f < BEAT.clink[0] ? 0 : Math.min(7, 3 + Math.floor(lapse * 5));
  const thumb = springPt(f, [130, 900], [{ at: 884, to: [40, 440] }, { at: BEAT.dash - 6, to: [130, 900] }], 10, 170);
  return (
    <Frame bg={["#1E1B4B", "#312E81"]} night>
      <Camera f={f} drift={3} seed={20}>
        <Depth blur={4}>
          <Room f={f} dawn={lapse * 0.6} />
          <Clock x={820} y={160} text={clock} size={70} color="#FBBF24" />
        </Depth>
        <Actor x={-60} y={640} s={0.78} f={f} who={RIYA} shadow expr={pose(1)} tilt={jitter(1)} gaze={[0.4, 0.4]} blink={blinkAt(f, 3)} />
        <g transform={`translate(${kx} 0)`} opacity={away ? 0 : 1}>
          <Actor
            x={640}
            y={640}
            s={0.78}
            f={f}
            who={KABIR}
            shadow
            expr={f < BEAT.dash ? "laugh" : pose(2)}
            viseme={vis("kabir-chai", f)}
            tilt={talkTilt(["kabir-chai"], f, jitter(2), 2)}
            gaze={[-0.5, 0.3]}
            blink={blinkAt(f, 5)}
            arms={f < BEAT.dash ? { l: { to: thumb, pose: "thumb", upright: true } } : {}}
          />
        </g>
        {away && (
          <g stroke="#FFFFFF" strokeWidth={10} strokeLinecap="round" opacity={interpolate(f, [BEAT.dash, BEAT.dash + 10], [0.8, 0], clamp)}>
            <path d="M700 900 h300 M720 1000 h340 M690 1100 h260" />
          </g>
        )}
        <Actor x={250} y={600} s={0.82} f={f} who="meera" shadow expr={f > BEAT.back + 10 ? (["laugh", "smug", "surprised"] as const)[step % 3] : "smug"} tilt={jitter(0)} gaze={[0.3, 0.6]} blink={blinkAt(f, 1)} />
        <Table y={1300} color="#3F2A1D" top="#5B3A22" />
        {Array.from({ length: cups }, (_, i) => (
          <Kulhad key={i} x={120 + i * 120} y={1290 - (i % 2) * 6} s={0.9} t={f + i * 7} />
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

/* 11. 8:45 am: Srutam's reminder knocks. "Econ exam. Starts in 15 minutes." */
const Remind: React.FC = () => {
  const f = useCurrentFrame() + SCENES.remind.from;
  const on = f >= BEAT.notify;
  const dx = shake(f, [BEAT.notify, BEAT.notify + 3], 9);
  return (
    <Frame bg={["#FEF3C7", "#FDE68A"]}>
      <Camera f={f} zoom={interpolate(f, [1010, 1044], [1, 1.06], clamp)} focus={[540, 900]} drift={3} seed={22}>
        <Depth blur={10}>
          <rect width={W} height={H} fill="#FDE7C7" />
          {[0, 1, 2].map((i) => (
            <rect key={i} x={80 + i * 340} y={180} width={260} height={560} fill="#FFFBEB" stroke="#E7C9A0" strokeWidth={10} />
          ))}
        </Depth>
        <g transform={`translate(${dx} 0)`}>
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

/* 12. The exam hall: bell, the paper floats down, Q3 is the metro case study. Pen spin, then write. */
const Hall: React.FC = () => {
  const f = useCurrentFrame() + SCENES.hall.from;
  // The question paper floats down (air drag, flutter) and lands on the desk.
  const start = BEAT.paperDown - 29;
  const fall = flutter(f, start, [0, -1000], [0, 0], [0, 1500], 3, 26, 1.1);
  const paper: Pt = f < start ? [0, -2000] : fall[1] >= 0 ? [0, 0] : fall;
  const pen = spin(f, BEAT.spin, 1500, 2.2);
  const caught = f >= BEAT.catch;
  const penRot = caught ? spin(BEAT.catch, BEAT.spin, 1500, 2.2) : pen;
  const write = caught ? Math.min(1, (f - BEAT.catch) / 14) : 0;
  const bell = f >= BEAT.bell && f < BEAT.bell + 40 ? Math.sin(f * 2.4) * 6 : 0;
  const hand: Pt = caught ? [520 + Math.sin(f * 1.3) * 10, 830 + Math.cos(f * 1.7) * 5] : [560, 800];
  const handW: Pt = [150 + hand[0] * 1.2, 260 + hand[1] * 1.2];
  return (
    <Frame bg={["#FEF9C3", "#FDE68A"]}>
      <Camera f={f} zoom={interpolate(f, [1044, 1110], [1, 1.08], { ...clamp, easing: easeInOut })} focus={[540, 1200]} drift={3} seed={24}>
        <Depth blur={6}>
          <rect width={W} height={H} fill="#FDF2DC" />
          {[0, 1, 2].map((i) => (
            <rect key={i} x={60 + i * 340} y={120} width={280} height={520} fill="#FFFBEB" stroke="#E7C9A0" strokeWidth={10} />
          ))}
          <g transform={`translate(900 120) rotate(${bell})`}>
            <circle cx={0} cy={60} r={56} fill="#DC2626" stroke={LINE} strokeWidth={6} />
            <circle cx={0} cy={60} r={14} fill="#1F2937" />
          </g>
          {[0, 1, 2, 3].map((i) => (
            <rect key={i} x={40 + i * 270} y={780} width={180} height={160} rx={80} fill="#D6C3A5" opacity={0.7} />
          ))}
        </Depth>
        <Actor
          x={150}
          y={260}
          s={1.2}
          f={f}
          who="meera"
          shadow
          torso={120}
          expr={f < BEAT.paperDown + 14 ? "blank" : "smug"}
          tilt={caught ? 3 : 0}
          gaze={[0.1, 0.9]}
          blink={blinkAt(f, 1)}
          arms={{ r: { to: hand, pose: "grip", rot: -30 } }}
        />
        <Table y={1240} color="#B08155" top="#C9976A" />
        <g transform={`translate(${paper[0]} ${paper[1]}) rotate(${-3 + paper[0] * 0.15} 540 1500)`}>
          <rect x={200} y={1290} width={680} height={540} fill="#FFFDF8" stroke={LINE} strokeWidth={5} />
          <text x={240} y={1380} fontFamily="Kalam" fontWeight={700} fontSize={44} fill="#1F2937">
            <tspan x={240}>Q1. Explain Chapter 4…</tspan>
            <tspan x={240} dy={74}>Q2. Chapter 7: public…</tspan>
            <tspan x={240} dy={74}>Q3. Metro case study</tspan>
            <tspan x={240} dy={74}>{"      (20 marks)"}</tspan>
          </text>
          {f > BEAT.paperDown + 16 && (
            <ellipse cx={500} cy={1560} rx={290 * Math.min(1, (f - BEAT.paperDown - 16) / 8)} ry={70} fill="none" stroke="#DC2626" strokeWidth={6} />
          )}
          {write > 0 && <path d={`M260 1760 q30 -20 60 0 t60 0 t60 0 t60 0 t60 0`} stroke="#1D4ED8" strokeWidth={5} fill="none" strokeDasharray={400} strokeDashoffset={400 * (1 - write)} />}
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
  const f = useCurrentFrame() + SCENES.end.from;
  const band = interpolate(f, [BEAT.band, BEAT.band + 18], [0, 1], { ...clamp, easing: easeOut });
  const bounce = (seed: number) => Math.abs(Math.sin((f + seed) / 5)) * 10;
  const raise = springPt(f, [470, 900], [{ at: 1116, to: [540, 420] }], 10, 150);
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <Camera f={f} drift={3} seed={26}>
        <Depth blur={5}>
          <rect width={W} height={H} fill="#FCE7C8" />
          {[0, 1, 2].map((i) => (
            <rect key={i} x={60 + i * 340} y={160} width={280} height={520} fill="#FFFBEB" stroke="#E7C9A0" strokeWidth={10} />
          ))}
        </Depth>
        <g transform={`translate(0 ${-bounce(0)})`}>
          <Actor x={-170} y={620} s={0.85} f={f} who={RIYA} shadow torso={300} expr="laugh" tilt={-3} />
        </g>
        <g transform={`translate(0 ${-bounce(7)})`}>
          <Actor x={650} y={620} s={0.85} f={f} who={KABIR} shadow torso={300} expr="laugh" tilt={3} />
        </g>
        <Actor x={230} y={560} s={1.0} f={f} who="meera" shadow torso={300} expr="laugh" viseme={vis("e-tagline", f)} tilt={talkTilt(["e-tagline"], f, 0, 1.4)} blink={blinkAt(f, 1)} arms={{ r: { to: raise, pose: "phone", upright: true } }} />
        {[0, 1, 2, 3].map((i) => {
          const at = 1112 + i * 4;
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
  ["2 Papers", SCENES.papers, Papers],
  ["3 Lecture (flashback)", SCENES.lecture, Lecture],
  ["4 Recording insert", SCENES.insert, Insert],
  ["5 Prof", SCENES.prof, Prof],
  ["6 Class", SCENES.class, Class],
  ["7 Ask Srutam", SCENES.ask, Ask],
  ["8 Listen", SCENES.listen, Listen],
  ["9 Knock knock", SCENES.door, DoorShot],
  ["10 Chai time-lapse", SCENES.lapse, Lapse],
  ["11 Reminder", SCENES.remind, Remind],
  ["12 Exam hall", SCENES.hall, Hall],
  ["13 End", SCENES.end, End],
] as const;

export const ExamVideo: React.FC = () => {
  const f = useCurrentFrame();
  // Old-film warmth through the flashback (shots 3-6).
  const sepia = interpolate(f, [SCENES.lecture.from, SCENES.lecture.from + 16, SCENES.class.from + SCENES.class.duration - 10, SCENES.ask.from], [0, 0.5, 0.5, 0], clamp);
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <AbsoluteFill style={{ filter: sepia > 0 ? `sepia(${sepia})` : undefined }}>
        {LIST.map(([name, s, Comp]) => (
          <Sequence key={name} name={name} from={s.from} durationInFrames={s.duration}>
            <Comp />
          </Sequence>
        ))}
      </AbsoluteFill>
      <Captions cues={CAPTIONS} />
      <Soundtrack music="exam/music.wav" musicVolume={0.5} lines={VO} sfx={SFX} />
    </AbsoluteFill>
  );
};

