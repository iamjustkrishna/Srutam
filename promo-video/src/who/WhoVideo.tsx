// "Who Said That?": everyone agreed, nobody wrote it down, a week later nobody
// remembers. Srutam doesn't know who spoke; it finds the moment and plays it,
// and the room hears Rao say it himself.
import { AbsoluteFill, interpolate, Sequence, useCurrentFrame } from "remotion";
import { clamp, easeIn, easeInOut, easeOut } from "../anim";
import { ARJUN, NEHA, RAO } from "../cast/cast";
import { Actor, BROAD, blinkAt, Hand, reach, saccade, SLIM, type Pt } from "../cast/Rig";
import { VISEMES } from "../meeting/visemes";
import { Camera, Depth, Grade, LightLeak } from "../stories/Camera";
import { ballistic, damped, follow, springAt, springPt } from "../stories/physics";
import { about, Captions, pop, shake, Soundtrack } from "../stories/Story";
import { voice } from "../stories/voice";
import { AskScreen, Board, Bubble, Burst, DockOverApp, EndBand, Frame, H, InsightsScreen, LINE, Phone, PlaybackScreen, Sfx, Sweat, W } from "../storyboards/Kit";
import { MeetingTable, OfficeWall } from "../storyboards/meeting2";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";

const { vis, visAny, talkTilt } = voice(VO, VISEMES);

/* 1. Tuesday sync: "Launch on the 14th? ... Fine. The 14th, final. Arjun, pricing deck by Thursday." */
const Sync: React.FC = () => {
  const f = useCurrentFrame() + SCENES.sync.from;
  const rao = ["rao-launch", "rao-final", "rao-deck"];
  const raoArm = springPt(
    f,
    reach("l", 96, 0.97, BROAD),
    [
      { at: BEAT.boardPoint, to: reach("l", 197, 0.97, BROAD) },
      { at: BEAT.boardTap, to: reach("l", 203, 1.0, BROAD) },
      { at: BEAT.boardTap + 5, to: reach("l", 197, 0.96, BROAD) },
      { at: BEAT.pointArjun, to: reach("l", 152, 0.95, BROAD) },
      { at: BEAT.armsDown, to: reach("l", 96, 0.97, BROAD) },
    ],
    13,
    150,
  );
  const raoTurn = springAt(f, 0, [
    { at: BEAT.boardPoint, to: -0.28 },
    { at: 82, to: 0 },
    { at: BEAT.pointArjun, to: -0.2 },
    { at: BEAT.armsDown, to: 0 },
  ]);
  const nod = (at: number) => damped(f, at, 7, 2.2, 4);
  const meeraGlance: [number, number] = f > 196 && f < 236 ? [0.1, 1] : [0.5, -0.7];
  const zoom = interpolate(f, [0, 280], [1, 1.07], { ...clamp, easing: easeInOut });
  const recDot = 0.5 + 0.5 * Math.sin(f / 6);
  return (
    <Frame bg={["#DCE3EC", "#C7D2E0"]}>
      <Camera f={f} zoom={zoom} focus={[540, 1150]} drift={4}>
        <Depth blur={2.5}>
          <OfficeWall board={false} />
          <Board x={60} y={200} w={520} h={420} lines={["LAUNCH:", "14th?? 21st??", "pricing → ?"]} />
        </Depth>
        <Actor
          x={440}
          y={470}
          s={0.86}
          f={f}
          who={RAO}
          torso={560}
          shadow
          expr="smile"
          viseme={visAny(rao, f, "A")}
          turn={raoTurn}
          tilt={talkTilt(rao, f)}
          gaze={f < BEAT.pointArjun ? [-0.8, -0.2] : f < BEAT.armsDown ? [-0.6, 0.6] : saccade(f, 2)}
          blink={blinkAt(f, 3)}
          arms={{ l: { to: raoArm, pose: f > BEAT.boardPoint - 2 && f < BEAT.armsDown - 4 ? "point" : "open", rot: f < BEAT.boardPoint ? 70 : 0 } }}
        />
        <Actor x={-130} y={1170} s={0.74} f={f} who={ARJUN} shadow expr="smile" viseme={vis("arjun-yes", f)} tilt={nod(BEAT.nodA) + 2} turn={0.2} gaze={f > BEAT.pointArjun && f < BEAT.armsDown ? [0.9, -0.9] : [0.8, -0.6]} blink={blinkAt(f, 5)} />
        <Actor x={290} y={1230} s={0.7} f={f} who="meera" shadow expr="blank" turn={0.1} gaze={meeraGlance} blink={blinkAt(f, 1)} sway={Math.sin(f / 30) * 0.5} />
        <Actor x={690} y={1170} s={0.74} f={f} who={NEHA} shadow expr="smile" viseme={vis("neha-yes", f)} tilt={-nod(BEAT.nodN) - 2} turn={-0.15} gaze={[-0.1, -0.8]} blink={blinkAt(f, 8)} />
        <MeetingTable y={1610} />
        {/* Meera's phone on the table: Srutam's dock, quietly recording */}
        <g transform="translate(470 1660) rotate(-6)">
          <rect x={-70} y={-24} width={140} height={60} rx={14} fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
          <circle cx={-46} cy={6} r={9} fill="#EF4444" opacity={0.4 + 0.6 * recDot} />
          <circle cx={-46} cy={6} r={20} fill="#EF4444" opacity={0.18 * recDot} />
        </g>
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
      <g opacity={interpolate(f, [4, 12, 70, 80], [0, 1, 1, 0], clamp)}>
        <rect x={340} y={70} width={400} height={80} rx={20} fill="#0F172A" opacity={0.75} />
        <text x={540} y={124} textAnchor="middle" fill="#FFF" fontFamily="Inter" fontWeight={700} fontSize={40}>
          Tuesday, 11:02 am
        </text>
      </g>
    </Frame>
  );
};

/* 2. CU Rao, thumbs up: "Great. Someone note it down!" */
const Note: React.FC = () => {
  const f = useCurrentFrame() + SCENES.note.from;
  const arm = springPt(f, [600, 1040], [{ at: BEAT.thumb, to: [565, 470] }], 9, 170);
  const push = interpolate(f, [280, 350], [1, 1.05], clamp);
  return (
    <Frame bg={["#DCE3EC", "#C7D2E0"]}>
      <Camera f={f} zoom={push} focus={[540, 900]} drift={3} seed={4}>
        <Depth blur={6}>
          <OfficeWall board={false} />
        </Depth>
        <Actor
          x={10}
          y={520}
          s={1.45}
          f={f}
          who={RAO}
          shadow
          expr="laugh"
          viseme={vis("rao-note", f, "X")}
          tilt={talkTilt(["rao-note"], f, 2, 2.5)}
          gaze={[0.1, 0]}
          blink={blinkAt(f, 6)}
          arms={f >= BEAT.thumb - 2 ? { r: { to: arm, pose: "thumb", upright: true, rot: 90 } } : {}}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 3. The side-eye chain: Arjun looks at Neha, Neha looks at Meera, Meera looks at us. */
const Eyes: React.FC = () => {
  const f = useCurrentFrame() + SCENES.eyes.from;
  const band = (y: number, fill: string, at: number, child: React.ReactNode, k: number) => {
    const s = springAt(f, 0, [{ at, to: 1 }], 14, 180);
    return (
      <g key={k} transform={`translate(${(1 - s) * W} 0)`}>
        <clipPath id={`eyeband${k}`}>
          <rect x={0} y={y} width={W} height={640} />
        </clipPath>
        <rect x={0} y={y} width={W} height={640} fill={fill} />
        <g clipPath={`url(#eyeband${k})`}>{child}</g>
      </g>
    );
  };
  const snap = (at: number, from: [number, number], to: [number, number]): [number, number] => (f < at ? from : to);
  const meeraPush = interpolate(f, [BEAT.bandM + 8, 440], [1, 1.08], clamp);
  return (
    <Frame bg={["#0F172A", "#0F172A"]}>
      {band(0, "#E0E7FF", BEAT.bandA, <Actor x={130} y={-70} s={0.95} f={f} who={ARJUN} expr="sideeye" gaze={snap(BEAT.bandA + 10, [0, 0], [0.5, 1])} blink={blinkAt(f, 2)} />, 0)}
      {band(640, "#FEF3C7", BEAT.bandN, <Actor x={130} y={570} s={0.95} f={f} who={NEHA} expr="sideeye" gaze={snap(BEAT.bandN + 10, [0, 0], [-0.4, 1])} blink={blinkAt(f, 7)} />, 1)}
      {band(
        1280,
        "#FDE7C7",
        BEAT.bandM,
        <g transform={about(540, 1600, meeraPush)}>
          <Actor x={130} y={1210} s={0.95} f={f} who="meera" expr="smug" turn={0.15} gaze={snap(BEAT.bandM + 14, [0.6, 0], [0, 0])} sway={Math.sin(f / 20) * 0.4} />
        </g>,
        2,
      )}
      <path d={`M0 640 L${W} 640 M0 1280 L${W} 1280`} stroke="#0F172A" strokeWidth={14} />
    </Frame>
  );
};

/* 4. Insert: under the table, Meera's finger taps the Srutam dock. "Nobody wrote it down. Except Srutam." */
const Dock: React.FC = () => {
  const f = useCurrentFrame() + SCENES.dock.from;
  const wrist = springPt(f, [-260, 1340], [{ at: BEAT.tap - 14, to: [133, 1056] }], 15, 170);
  const press = f >= BEAT.tap && f < BEAT.tap + 5 ? 0.9 : 1;
  const ripple = interpolate(f, [BEAT.tap, BEAT.tap + 16], [0, 1], clamp);
  const push = interpolate(f, [440, 540], [1, 1.06], clamp);
  return (
    <Frame bg={["#3F2E22", "#1F1712"]} night>
      <Camera f={f} zoom={push} focus={[420, 1000]} drift={3} seed={7}>
        <rect x={0} y={0} width={W} height={250} fill="#6B4F3A" />
        <rect x={0} y={230} width={W} height={36} fill="#8B6B52" stroke={LINE} strokeWidth={5} />
        <ellipse cx={540} cy={1500} rx={620} ry={420} fill="#C4B5A0" opacity={0.25} />
        <Phone x={250} y={420} w={560} rot={-4} glow={f >= BEAT.tap ? "#EF4444" : undefined}>
          <DockOverApp w={560} recording={f >= BEAT.tap + 2} label="Team chat" />
        </Phone>
        {/* the holding hand */}
        <g transform="translate(560 1720) rotate(-92) scale(1.9)">
          <Hand pose="grip" skin="#F2C3A2" />
        </g>
        {/* the tapping hand: sleeve, wrist and index finger */}
        <g transform={`translate(${wrist[0]} ${wrist[1]}) rotate(-35) scale(${press} 1)`}>
          <path d="M-560 -46 L-6 -34 L-6 34 L-560 46 Z" fill="#EFE6D8" stroke={LINE} strokeWidth={5} />
          <g transform="scale(1.9)">
            <Hand pose="point" skin="#F2C3A2" />
          </g>
        </g>
        {ripple > 0 && ripple < 1 && <circle cx={268} cy={925} r={30 + ripple * 120} fill="none" stroke="#FFFFFF" strokeWidth={10} opacity={1 - ripple} />}
      </Camera>
    </Frame>
  );
};

/* 5. Decision. Next step. Reminder. Before lunch. */
const Cards: React.FC = () => {
  const f = useCurrentFrame() + SCENES.cards.from;
  const appear = [BEAT.cardDecision, BEAT.cardStep, BEAT.cardReminder].map((at) => springAt(f, 0, [{ at, to: 1 }], 11, 170));
  const sticker = springAt(f, 0, [{ at: BEAT.lunch, to: 1 }], 9, 180);
  const push = interpolate(f, [540, 690], [1, 1.07], { ...clamp, easing: easeInOut });
  return (
    <Frame bg={["#0F172A", "#1E293B"]} night>
      <Camera f={f} zoom={push} focus={[500, 900]} drift={3} seed={9}>
        <Phone x={150} y={170} w={700} glow="#3B82F6">
          <InsightsScreen
            w={700}
            note="Tuesday sync"
            meta="Tue, 11:02 am · 32:14"
            appear={appear}
            items={[
              { kind: "DECISION", text: "Launch moves to the 14th" },
              { kind: "NEXT STEP", text: "Arjun sends the pricing deck", meta: "By Thursday" },
              { kind: "REMINDER", text: "Pricing deck", meta: "Thursday, 10:00 am" },
            ]}
          />
        </Phone>
        <g transform={`translate(860 300) rotate(${8 - sticker * 4}) scale(${sticker})`} opacity={sticker > 0.02 ? 1 : 0}>
          <rect x={-150} y={-60} width={300} height={120} rx={24} fill="#FBBF24" stroke={LINE} strokeWidth={5} />
          <text x={0} y={-8} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={44} fill="#2B1E1B">
            12:58 pm
          </text>
          <text x={0} y={38} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={34} fill="#7C2D12">
            before lunch
          </text>
        </g>
      </Camera>
      <LightLeak k={interpolate(f, [672, 694], [0, 1], clamp)} />
    </Frame>
  );
};

/* 6. One week later: the calendar page tears off and falls. */
const Week: React.FC = () => {
  const f = useCurrentFrame() + SCENES.week.from;
  const torn = f >= BEAT.pageTear;
  const swing = torn ? Math.min(1, (f - BEAT.pageTear) / 6) : 0;
  const fall = ballistic(f, BEAT.pageTear + 6, [0, 0], [380, -500], 3000);
  const rot = torn ? -swing * 25 - Math.max(0, f - BEAT.pageTear - 6) * 6 : 0;
  const title = interpolate(f, [714, 728], [0, 1], { ...clamp, easing: easeOut });
  const Page: React.FC<{ day: string }> = ({ day }) => (
    <g>
      <rect x={-330} y={-380} width={660} height={760} rx={30} fill="#FFFDF8" stroke={LINE} strokeWidth={8} />
      <rect x={-330} y={-380} width={660} height={160} rx={30} fill="#DC2626" />
      <text x={0} y={-270} textAnchor="middle" fill="#FFF" fontFamily="Inter" fontWeight={800} fontSize={70}>
        TUESDAY
      </text>
      <text x={0} y={180} textAnchor="middle" fill="#111827" fontFamily="Inter" fontWeight={800} fontSize={360}>
        {day}
      </text>
    </g>
  );
  return (
    <Frame bg={["#111827", "#1F2937"]} night>
      <g transform="translate(540 860) rotate(-3)">
        <Page day="19" />
        <g transform={`translate(${fall[0]} ${fall[1]}) rotate(${rot} -330 -380)`}>
          <Page day="12" />
        </g>
      </g>
      <g opacity={title} transform={`translate(0 ${(1 - title) * 30})`}>
        <text x={540} y={1620} textAnchor="middle" fill="#FDE68A" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={100}>
          One week later
        </text>
      </g>
    </Frame>
  );
};

/* 7. CU Rao winds up and slams the table: "The 14th? Who said yes to the 14th?!" */
const Slam: React.FC = () => {
  const f = useCurrentFrame() + SCENES.slam.from;
  const t0 = BEAT.windup;
  const imp = BEAT.impact;
  // Local IK targets (Rao: x 20, y 420, s 1.4; the table top is at y 1690, local 907).
  const rest: Pt = [520, 900];
  const up: Pt = [600, 300];
  const down: Pt = [585, 905];
  const lift = interpolate(f, [t0, t0 + 12], [0, 1], { ...clamp, easing: easeOut });
  const strike = interpolate(f, [imp - 4, imp], [0, 1], { ...clamp, easing: easeIn });
  const hand: Pt =
    f < imp - 4
      ? [rest[0] + (up[0] - rest[0]) * lift, rest[1] + (up[1] - rest[1]) * lift]
      : [up[0] + (down[0] - up[0]) * strike, up[1] + (down[1] - up[1]) * strike - Math.abs(damped(f, imp, 30, 2.5, 7))];
  const squash = f < imp ? interpolate(f, [t0, t0 + 12], [1, 1.05], clamp) : 1 - damped(f, imp, 0.08, 2.5, 6) - 0.04 * Math.exp(-(f - imp) / 6);
  const zoom = interpolate(f, [imp - 4, imp + 6], [1, 1.22], { ...clamp, easing: easeOut });
  const sh: [number, number] = [shake(f, [imp, imp + 3, imp + 7], 24), shake(f, [imp + 1, imp + 5], 14)];
  const after = f >= imp;
  // The cup jumps off the table, lands on its side after 0.5 s and bounces once.
  const land = imp + 15;
  const cup = ballistic(Math.min(f, land), imp, [0, 0], [-260, -900], 3600);
  const cupX = cup[0];
  const cupY = Math.min(0, cup[1]) - Math.abs(damped(f, land, 34, 1.6, 7));
  const cupRot = after ? interpolate(f, [imp, land], [0, -100], clamp) + damped(f, land, 14, 2.5, 6) : 0;
  const tilt = talkTilt(["rao-who"], f, 3, 2.8);
  return (
    <Frame bg={after ? ["#FEE2E2", "#FCA5A5"] : ["#CBD5E1", "#94A3B8"]}>
      {after &&
        Array.from({ length: 14 }, (_, i) => {
          const a = (i / 14) * Math.PI * 2 + f * 0.004;
          return <path key={i} d={`M540 860 L${540 + Math.cos(a) * 1600} ${860 + Math.sin(a) * 1600}`} stroke="#F87171" strokeWidth={70} opacity={0.3} />;
        })}
      {!after && (
        <Depth blur={5}>
          <OfficeWall board={false} />
        </Depth>
      )}
      <Camera f={f} zoom={zoom} focus={[540, 1100]} shake={sh} drift={after ? 2 : 4} seed={11}>
        <Actor
          x={20}
          y={420}
          s={1.4}
          f={f}
          who={RAO}
          expr="angry"
          viseme={after ? vis("rao-who", f, "A") : "A"}
          tilt={tilt}
          squash={squash}
          torso={220}
          gaze={saccade(f, 3)}
          blink={!after && blinkAt(f, 2)}
          arms={{ r: { to: hand, pose: "fist", rot: f < imp - 4 ? -60 : 70 } }}
        />
        {/* the table: top surface catching the light, front edge, and it jolts on impact */}
        <g transform={`translate(0 ${after ? damped(f, imp, 10, 4, 9) : 0})`}>
          <path d={`M-60 1690 L${W + 60} 1690 L${W + 60} 2000 L-60 2000 Z`} fill="#5B4130" />
          <path d={`M-60 1690 L${W + 60} 1690 L${W + 60} 1730 L-60 1730 Z`} fill="#8B6B52" stroke={LINE} strokeWidth={5} />
          <ellipse cx={600} cy={1700} rx={360} ry={10} fill="#FFFFFF" opacity={0.12} />
        </g>
        <g transform={`translate(${200 + cupX} ${1610 + cupY}) rotate(${cupRot} 35 40)`}>
          <path d="M0 0 L70 0 L62 80 L8 80 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={5} />
          <path d="M66 18 q26 6 0 36" stroke={LINE} strokeWidth={5} fill="none" />
        </g>
        {after && (
          <g transform={about(840, 1690, pop(f, imp, 8))}>
            <Burst x={840} y={1690} r={90} color="#2B1E1B" n={10} />
            <Sfx x={880} y={1560} text="BAM!" size={130} rot={-8} />
          </g>
        )}
        {after && (
          <g transform={`translate(880 560) scale(${1 + 0.15 * Math.sin(f / 3)})`} stroke="#DC2626" strokeWidth={16} strokeLinecap="round" fill="none">
            <path d="M-40 -10 Q-10 -10 -10 -40 M40 -10 Q10 -10 10 -40 M-40 10 Q-10 10 -10 40 M40 10 Q10 10 10 40" />
          </g>
        )}
      </Camera>
    </Frame>
  );
};

/* 8. "Her!" "Him!": each points at the other's face; the arms cross. */
const Point: React.FC = () => {
  const f = useCurrentFrame() + SCENES.point.from;
  const a = springAt(f, 0, [{ at: BEAT.pointA, to: 1 }], 9, 170);
  const n = springAt(f, 0, [{ at: BEAT.pointN, to: 1 }], 9, 170);
  const arjunHand = springPt(f, reach("r", 92, 0.9, BROAD), [{ at: BEAT.pointA, to: reach("r", -40, 0.45, BROAD) }], 9, 170);
  const nehaHand = springPt(f, reach("l", 88, 0.9, SLIM), [{ at: BEAT.pointN, to: reach("l", 190, 0.45, SLIM) }], 9, 170);
  const push = interpolate(f, [896, 985], [1, 1.06], { ...clamp, easing: easeInOut });
  const flash = interpolate(f, [896, 930], [0.5, 0.18], clamp);
  const drop = (at: number, x: number, y: number) => {
    if (f < at) return null;
    const p = ballistic(f, at, [x, y], [x < 540 ? -70 : 70, -320], 2600);
    return p[1] < y + 520 ? (
      <g key={at} transform={`translate(${p[0] - x} ${p[1] - y})`}>
        <Sweat x={x} y={y} s={1.2} />
      </g>
    ) : null;
  };
  return (
    <Frame bg={["#FFE4E6", "#FECDD3"]}>
      <Camera f={f} zoom={push} focus={[540, 1000]} drift={3} seed={13}>
        <Depth blur={6}>
          <OfficeWall board={false} />
        </Depth>
        <rect width={W} height={H} fill="#F43F5E" opacity={flash} />
        <Actor x={-80} y={470} s={0.9} f={f} who={ARJUN} shadow torso={300} expr="worried" viseme={vis("arjun-her", f)} turn={0.12 + 0.15 * a} tilt={3 * a} gaze={[0.3 + 0.6 * a, -0.1]} blink={blinkAt(f, 4)} arms={a > 0.03 ? { r: { to: arjunHand, pose: a > 0.3 ? "point" : "fist", rot: 72 * a } } : {}} />
        <Actor x={560} y={470} s={0.9} f={f} who={NEHA} shadow torso={300} expr="angry" viseme={vis("neha-him", f, "A")} turn={-0.12 - 0.15 * n} tilt={-3 * n} gaze={[-0.3 - 0.6 * n, -0.1]} blink={blinkAt(f, 9)} arms={n > 0.03 ? { l: { to: nehaHand, pose: n > 0.3 ? "point" : "fist", rot: -55 * n } } : {}} />
        <MeetingTable y={1450} />
        {drop(BEAT.pointA + 6, 330, 730)}
        {drop(BEAT.pointA + 22, 345, 760)}
      </Camera>
      <g transform={about(220, 330, pop(f, BEAT.pointA + 4, 9))} opacity={f >= BEAT.pointA + 4 ? 1 : 0}>
        <Bubble x={60} y={250} w={320} lines={["Her!"]} tail={[200, 640]} size={80} />
      </g>
      <g transform={about(860, 330, pop(f, BEAT.pointN + 4, 9))} opacity={f >= BEAT.pointN + 4 ? 1 : 0}>
        <Bubble x={700} y={250} w={320} lines={["Him!"]} tail={[840, 640]} size={80} />
      </g>
    </Frame>
  );
};

const Q = "When did we agree on the 14th?";
const A = "Tuesday's sync, 0:42: “Fine. The 14th, final.”";

/* 9. Meera lifts Srutam: "Relax. Let's ask Srutam." The answer is a time, a quote and a source. */
const Ask: React.FC = () => {
  const f = useCurrentFrame() + SCENES.ask.from;
  const raise = springAt(f, 0, [{ at: BEAT.raise, to: 1 }], 11, 120);
  // Meera: x -170, y 860, s 1.2. The phone rides on her right hand.
  const M = { x: -170, y: 860, s: 1.2 };
  const hand: Pt = [606, 383 + (1 - raise) * 330];
  const hw: Pt = [M.x + hand[0] * M.s, M.y + hand[1] * M.s];
  const PW = 470;
  const qn = Math.round(interpolate(f, [BEAT.qStart, BEAT.qEnd], [0, Q.length], clamp));
  const an = Math.round(interpolate(f, [BEAT.aStart, BEAT.aEnd], [0, A.length], clamp));
  const push = interpolate(f, [BEAT.qStart, SCENES.ask.from + SCENES.ask.duration], [1, 1.1], { ...clamp, easing: easeInOut });
  const tapK = interpolate(f, [BEAT.tapRef, BEAT.tapRef + 10], [0, 1], clamp);
  return (
    <Frame bg={["#DCE3EC", "#C7D2E0"]}>
      <Camera f={f} zoom={push} focus={[700, 900]} drift={3} seed={17}>
        <Depth blur={7}>
          <OfficeWall board={false} />
          <Actor x={620} y={1040} s={0.62} f={f} who={RAO} expr="worried" gaze={[-0.8, -0.4]} blink={blinkAt(f, 6)} />
        </Depth>
        <g transform={`rotate(${(1 - raise) * 10 - 3} ${hw[0]} ${hw[1]})`}>
          <Phone x={hw[0] - 70} y={hw[1] - 1010} w={PW} glow={f >= BEAT.qStart ? "#3B82F6" : undefined}>
            <AskScreen w={PW} q={Q.slice(0, qn)} a={A.slice(0, an)} refs={f >= BEAT.refs ? ["Tuesday sync · 0:42"] : []} pressed={f >= BEAT.tapRef && f < BEAT.tapRef + 8} />
          </Phone>
        </g>
        {tapK > 0 && tapK < 1 && <circle cx={hw[0] + 90} cy={hw[1] - 1010 + 690} r={20 + tapK * 90} fill="none" stroke="#FFFFFF" strokeWidth={8} opacity={1 - tapK} />}
        <Actor
          x={M.x}
          y={M.y}
          s={M.s}
          f={f}
          who="meera"
          shadow
          expr="smug"
          viseme={vis("m-ask", f)}
          turn={0.22}
          tilt={talkTilt(["m-ask"], f, 0, 1.6)}
          sway={follow((g) => talkTilt(["m-ask"], g, 0, 1.6), f, 4, 0.3)}
          gaze={f < BEAT.qStart ? [0.7, -0.1] : [0.8, -0.6]}
          blink={blinkAt(f, 1)}
          arms={{ r: { to: hand, pose: "grip", upright: true } }}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

/* 9b. The source plays. The room hears Rao himself: "Fine. The 14th, final." Focus pulls from the phone to Rao. */
const Playback: React.FC = () => {
  const f = useCurrentFrame() + SCENES.play.from;
  const p0 = BEAT.play;
  const rack = interpolate(f, [p0 + 6, p0 + 22], [0, 1], { ...clamp, easing: easeInOut });
  const progress = interpolate(f, [p0, p0 + 75], [0.38, 0.47], clamp);
  const sink = springAt(f, 1, [{ at: p0 + 30, to: 0.93 }], 14, 90);
  const raoExpr = f < p0 + 8 ? "neutral" : f < p0 + 34 ? "surprised" : "worried";
  const look = springAt(f, 0, [{ at: p0 + 18, to: 1 }], 12, 150);
  const zoom = interpolate(f, [p0, p0 + 79], [1.02, 1.12], { ...clamp, easing: easeInOut });
  return (
    <Frame bg={["#DCE3EC", "#C7D2E0"]}>
      <Camera f={f} zoom={zoom} focus={[540, 900]} drift={3} seed={21}>
        <Depth blur={3 + 2 * (1 - rack)}>
          <OfficeWall board={false} />
        </Depth>
        <Depth blur={5 * (1 - rack)}>
          <Actor x={-90} y={700} s={0.78} f={f} who={ARJUN} shadow torso={300} expr={look > 0.5 ? "smug" : "worried"} turn={0.25 * look} gaze={[0.9 * look, -0.2]} blink={blinkAt(f, 4)} />
          <Actor x={560} y={700} s={0.78} f={f} who={NEHA} shadow torso={300} expr={look > 0.5 ? "smug" : "neutral"} turn={-0.25 * look} gaze={[-0.9 * look, -0.2]} blink={blinkAt(f, 9)} />
          <Actor
            x={240}
            y={560}
            s={0.95}
            f={f}
            who={RAO}
            shadow
            torso={320}
            expr={raoExpr}
            squash={sink}
            tilt={f > p0 + 34 ? -2 : 0}
            gaze={f < p0 + 34 ? [-0.2, 0.9] : [-0.6, 0.5]}
            blink={f > p0 + 34 && blinkAt(f, 2)}
          />
          {f > p0 + 36 && <Sweat x={655} y={840 + ((f - p0 - 36) % 24) * 5} s={1.1} />}
          <MeetingTable y={1450} />
        </Depth>
        {/* foreground: Srutam playing the moment it found */}
        <Depth blur={4 * rack}>
          <Phone x={70} y={1120} w={470} rot={-6} glow="#FBBF24">
            <PlaybackScreen w={470} title="Tuesday sync" meta="0:42 of 32:14 · Tue, 11:02 am" quote="“Fine. The 14th, final.”" progress={progress} />
          </Phone>
        </Depth>
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

/* 10. Everyone laughs; Rao rubs his head. "Every meeting, minuted." */
const End: React.FC = () => {
  const f = useCurrentFrame() + SCENES.end.from;
  const laugh = (seed: number) => Math.abs(Math.sin((f + seed) / 4)) * 10;
  const rub: Pt = [190 + Math.sin(f / 3) * 18, 330];
  const band = interpolate(f, [BEAT.band, BEAT.band + 18], [0, 1], { ...clamp, easing: easeOut });
  return (
    <Frame bg={["#DCE3EC", "#C7D2E0"]}>
      <Camera f={f} drift={3} seed={19}>
        <Depth blur={4}>
          <OfficeWall board={false} />
        </Depth>
        <g transform={`translate(0 ${-laugh(0)})`}>
          <Actor x={-170} y={600} s={0.82} f={f} who={ARJUN} shadow expr="laugh" tilt={-3} />
        </g>
        <g transform={`translate(0 ${-laugh(9)})`}>
          <Actor x={650} y={600} s={0.82} f={f} who={NEHA} shadow expr="laugh" tilt={3} />
        </g>
        <Actor x={180} y={760} s={0.98} f={f} who={RAO} shadow expr="laugh" tilt={-4} arms={{ l: { to: rub, pose: "palm", rot: 25 } }} />
      </Camera>
      <g transform={`translate(0 ${(1 - band) * 420})`}>
        <EndBand tagline="Every meeting, minuted." />
      </g>
    </Frame>
  );
};

const LIST = [
  ["1 Tuesday sync", SCENES.sync, Sync],
  ["2 Note it down", SCENES.note, Note],
  ["4 Dock", SCENES.dock, Dock],
  ["5 Cards", SCENES.cards, Cards],
  ["6 One week later", SCENES.week, Week],
  ["9 Let's ask", SCENES.ask, Ask],
  ["9b Playback", SCENES.play, Playback],
  ["10 End", SCENES.end, End],
] as const;

export const WhoVideo: React.FC = () => {
  const f = useCurrentFrame();
  // Whip into the side-eye chain, and from the slam into "Her!" / "Him!"
  const w1 = interpolate(f, [346, 354], [0, 1], { ...clamp, easing: easeInOut });
  const w2 = interpolate(f, [BEAT.whip, BEAT.whip + 8], [0, 1], { ...clamp, easing: easeInOut });
  const blur1 = Math.sin(w1 * Math.PI) * 24;
  const blur2 = Math.sin(w2 * Math.PI) * 26;
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      {LIST.map(([name, s, Comp]) => (
        <Sequence key={name} name={name} from={s.from} durationInFrames={s.duration}>
          <Comp />
        </Sequence>
      ))}
      <Sequence name="3 Side-eye" from={SCENES.eyes.from - 4} durationInFrames={SCENES.eyes.duration + 4}>
        <AbsoluteFill style={{ translate: `0px ${(1 - w1) * H}px`, filter: `blur(${blur1}px)` }}>
          <Eyes />
        </AbsoluteFill>
      </Sequence>
      <Sequence name="7 Slam" from={SCENES.slam.from} durationInFrames={SCENES.slam.duration}>
        <AbsoluteFill style={{ translate: `${-w2 * W}px 0px`, filter: `blur(${blur2}px)` }}>
          <Slam />
        </AbsoluteFill>
      </Sequence>
      <Sequence name="8 Her / Him" from={SCENES.point.from} durationInFrames={SCENES.point.duration}>
        <AbsoluteFill style={{ translate: `${(1 - w2) * W}px 0px`, filter: `blur(${blur2}px)` }}>
          <Point />
        </AbsoluteFill>
      </Sequence>
      <Captions cues={CAPTIONS} />
      <Soundtrack music="meeting/music.wav" musicVolume={0.5} lines={VO} sfx={SFX} />
    </AbsoluteFill>
  );
};
