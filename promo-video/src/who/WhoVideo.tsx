// "Who Said That?" v2: cold open on the outburst, then Tuesday. Arjun suggests the
// 21st and Rao overrules him himself; nobody writes it down except Srutam. A week
// later Srutam finds the moment and plays it, and Rao hears his own voice.
// Rendered at 60 fps: every shot reads continuous story time from useT().
import { AbsoluteFill, interpolate, Sequence } from "remotion";
import { clamp, easeIn, easeInOut, easeOut } from "../anim";
import { ARJUN, NEHA, RAO } from "../cast/cast";
import { Actor, blinkK, BROAD, Hand, reach, saccade, type Pt } from "../cast/Rig";
import { VISEMES } from "../meeting/visemes";
import { Camera, Depth, Grade, LightLeak } from "../stories/Camera";
import { ballistic, damped, follow, springAt, springPt } from "../stories/physics";
import { Captions, Soundtrack } from "../stories/Story";
import { K, useT } from "../stories/time";
import { voice } from "../stories/voice";
import { AskScreen, Board, DockOverApp, EndBand, Frame, H, InsightsScreen, LINE, Phone, PlaybackScreen, Sweat, W } from "../storyboards/Kit";
import { MeetingTable, OfficeWall } from "../storyboards/meeting2";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";

const { vis, visAny, talkTilt } = voice(VO, VISEMES);
const RAO_LINES = ["rao-who", "rao-launch", "rao-final", "rao-deck", "rao-note", "rao-sheepish"];
const BOARD = ["LAUNCH:", "14th?? 21st??", "pricing → ?"];
const OFFICE: [string, string] = ["#DCE3EC", "#C7D2E0"];
const span = (sc: { from: number; duration: number }): [number, number] => [sc.from, sc.from + sc.duration];

/** The meeting room behind the actors, softly out of focus. */
const Room: React.FC<{ blur?: number; board?: boolean }> = ({ blur = 3, board = true }) => (
  <Depth blur={blur}>
    <OfficeWall board={false} />
    {board && <Board x={60} y={200} w={520} h={420} lines={BOARD} />}
  </Depth>
);

/* 1. Cold open, a week later: Rao slams the table. "The 14th? Who said yes to the 14th?!" */
const Open: React.FC<{ at?: number }> = ({ at }) => {
  const live = useT() + SCENES.open.from;
  const f = at ?? live;
  const imp = BEAT.impact;
  const lift = interpolate(f, [BEAT.windup, imp - 5], [0, 1], { ...clamp, easing: easeOut });
  const strike = interpolate(f, [imp - 5, imp], [0, 1], { ...clamp, easing: easeIn });
  const rest: Pt = [520, 900];
  const up: Pt = [600, 320];
  const down: Pt = [585, 905];
  const hand: Pt =
    f < imp - 5
      ? [rest[0] + (up[0] - rest[0]) * lift, rest[1] + (up[1] - rest[1]) * lift]
      : [up[0] + (down[0] - up[0]) * strike, up[1] + (down[1] - up[1]) * strike - Math.abs(damped(f, imp, 24, 2.2, 7))];
  const after = f >= imp;
  const squash = after ? 1 - damped(f, imp, 0.05, 2.5, 6) - 0.03 * Math.exp(-(f - imp) / 6) : 1;
  const sh: [number, number] = [after ? damped(f, imp, 9, 7, 9) : 0, after ? damped(f, imp + 1, 5, 6, 9) : 0];
  const land = imp + 14;
  const cup = ballistic(Math.min(f, land), imp, [0, 0], [-180, -620], 3600);
  const cupRot = after ? interpolate(f, [imp, land], [0, -18], clamp) + damped(f, land, 8, 3, 7) : 0;
  const zoom = interpolate(f, span(SCENES.open), [1.04, 1.12], { ...clamp, easing: easeInOut });
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={zoom} focus={[540, 1000]} shake={sh} drift={3} seed={11}>
        <Room blur={6} board={false} />
        <Actor
          x={20}
          y={420}
          s={1.4}
          f={f}
          who={RAO}
          shadow
          torso={220}
          expr="angry"
          viseme={vis("rao-who", f, "B")}
          tilt={talkTilt(["rao-who"], f, 2, 2.4)}
          squash={squash}
          gaze={saccade(f, 3)}
          blink={f < imp ? blinkK(f, 2) : 0}
          arms={{ r: { to: hand, pose: "fist", rot: f < imp - 5 ? -60 : 70 } }}
        />
        <g transform={`translate(0 ${after ? damped(f, imp, 6, 5, 9) : 0})`}>
          <path d={`M-60 1690 L${W + 60} 1690 L${W + 60} 2000 L-60 2000 Z`} fill="#5B4130" />
          <path d={`M-60 1690 L${W + 60} 1690 L${W + 60} 1730 L-60 1730 Z`} fill="#8B6B52" stroke={LINE} strokeWidth={5} />
          <ellipse cx={600} cy={1700} rx={360} ry={10} fill="#FFFFFF" opacity={0.12} />
        </g>
        <g transform={`translate(${200 + cup[0]} ${1610 + Math.min(0, cup[1])}) rotate(${cupRot} 35 80)`}>
          <path d="M0 0 L70 0 L62 80 L8 80 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={5} />
          <path d="M66 18 q26 6 0 36" stroke={LINE} strokeWidth={5} fill="none" />
        </g>
      </Camera>
      {/* a warm flush of anger, not a cartoon burst */}
      <rect width={W} height={H} fill="#DC2626" opacity={after ? 0.1 * Math.exp(-(f - imp) / 20) + 0.04 : 0} style={{ mixBlendMode: "multiply" }} />
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 2. The frame freezes and drains of colour: "One week earlier". */
const Freeze: React.FC = () => {
  const f = useT() + SCENES.freeze.from;
  const k = interpolate(f, [SCENES.freeze.from, SCENES.freeze.from + 12], [0, 1], { ...clamp, easing: easeOut });
  const title = interpolate(f, [SCENES.freeze.from + 8, SCENES.freeze.from + 20], [0, 1], { ...clamp, easing: easeOut });
  return (
    <AbsoluteFill>
      <AbsoluteFill style={{ filter: `grayscale(${k}) brightness(${1 - 0.35 * k})`, scale: `${1 + 0.03 * k}` }}>
        <Open at={BEAT.freeze} />
      </AbsoluteFill>
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", opacity: title, translate: `0 ${(1 - title) * 24}px` }}>
        <div style={{ fontFamily: "Playfair Display", fontStyle: "italic", fontWeight: 700, fontSize: 104, color: "#FFF7ED", textShadow: "0 4px 30px rgba(0,0,0,0.5)" }}>One week earlier</div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

/** Tuesday's room, wide: Rao at the board, the team at the table, Meera's phone recording. */
const SyncWide: React.FC<{ f: number; arm: Pt; pointing: boolean; raoTurn: number; arjunNod?: number; gazeA?: [number, number] }> = ({ f, arm, pointing, raoTurn, arjunNod = 0, gazeA = [0.8, -0.6] }) => {
  const recDot = 0.5 + 0.5 * Math.sin(f / 6);
  return (
    <g>
      <Room blur={2.5} />
      <Actor
        x={440}
        y={470}
        s={0.86}
        f={f}
        who={RAO}
        shadow
        torso={560}
        expr="smile"
        viseme={visAny(RAO_LINES, f)}
        turn={raoTurn}
        tilt={talkTilt(RAO_LINES, f)}
        gaze={pointing ? [-0.8, -0.2] : saccade(f, 2)}
        blink={blinkK(f, 3)}
        arms={{ l: { to: arm, pose: pointing ? "point" : "open", rot: pointing ? 0 : 70 } }}
      />
      <Actor x={-130} y={1170} s={0.74} f={f} who={ARJUN} shadow expr="smile" tilt={arjunNod + 2} turn={0.2} gaze={gazeA} blink={blinkK(f, 5)} />
      <Actor x={290} y={1230} s={0.7} f={f} who="meera" shadow expr="blank" turn={0.1} gaze={[0.5, -0.7]} blink={blinkK(f, 1)} sway={Math.sin(f / 30) * 0.5} />
      <Actor x={690} y={1170} s={0.74} f={f} who={NEHA} shadow expr="smile" tilt={-2} turn={-0.15} gaze={[-0.1, -0.8]} blink={blinkK(f, 8)} />
      <MeetingTable y={1610} />
      <g transform="translate(470 1660) rotate(-6)">
        <rect x={-70} y={-24} width={140} height={60} rx={14} fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
        <circle cx={-46} cy={6} r={9} fill="#EF4444" opacity={0.4 + 0.6 * recDot} />
        <circle cx={-46} cy={6} r={20} fill="#EF4444" opacity={0.18 * recDot} />
      </g>
    </g>
  );
};

/* 3. Tuesday, 11:02: "So... do we launch on the 14th?" Rao taps the board. */
const BoardShot: React.FC = () => {
  const f = useT() + SCENES.board.from;
  const arm = springPt(
    f,
    reach("l", 96, 0.97, BROAD),
    [
      { at: 150, to: reach("l", 197, 0.97, BROAD) },
      { at: BEAT.boardTap, to: reach("l", 203, 1.0, BROAD) },
      { at: BEAT.boardTap + 5, to: reach("l", 197, 0.96, BROAD) },
    ],
    14,
    140,
  );
  const turn = springAt(f, 0, [{ at: 150, to: -0.28 }, { at: 200, to: -0.05 }], 14, 120);
  const zoom = interpolate(f, span(SCENES.board), [1, 1.05], { ...clamp, easing: easeInOut });
  const chip = interpolate(f, [150, 160, 206, 216], [0, 1, 1, 0], clamp);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={zoom} focus={[540, 1150]} drift={4}>
        <SyncWide f={f} arm={arm} pointing={f > 152} raoTurn={turn} />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
      <g opacity={chip}>
        <rect x={340} y={70} width={400} height={80} rx={20} fill="#0F172A" opacity={0.75} />
        <text x={540} y={124} textAnchor="middle" fill="#FFF" fontFamily="Inter" fontWeight={700} fontSize={40}>
          Tuesday, 11:02 am
        </text>
      </g>
    </Frame>
  );
};

/* 4. Arjun, carefully: "Sir, the 21st would be safer..." */
const ArjunShot: React.FC = () => {
  const f = useT() + SCENES.arjun.from;
  const hand = springPt(f, reach("r", 95, 0.85), [{ at: BEAT.arjunHand, to: reach("r", -35, 0.62) }], 14, 120);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.arjun), [1, 1.05], clamp)} focus={[500, 1000]} drift={3} seed={4}>
        <Depth blur={8}>
          <OfficeWall board={false} />
          <Board x={60} y={200} w={520} h={420} lines={BOARD} />
          <Actor x={-40} y={300} s={0.9} f={f} who={RAO} torso={400} expr="neutral" gaze={[0.8, 0.3]} />
        </Depth>
        <Actor
          x={180}
          y={560}
          s={1.25}
          f={f}
          who={ARJUN}
          shadow
          torso={120}
          expr="worried"
          viseme={vis("arjun-21", f)}
          tilt={talkTilt(["arjun-21"], f, -2, 1.6)}
          turn={-0.25}
          gaze={[-0.7, -0.5]}
          blink={blinkK(f, 5)}
          arms={f > BEAT.arjunHand - 2 ? { r: { to: hand, pose: "palm", rot: -30 } } : {}}
        />
        <MeetingTable y={1640} />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 5. Rao, decisive: "No, no. The 14th. Final." A head shake, then a palm chop. */
const FinalShot: React.FC = () => {
  const f = useT() + SCENES.final.from;
  const shakeHead = damped(f, BEAT.shake, 0.28, 2.4, 2.6);
  const hand = springPt(
    f,
    [560, 900],
    [
      { at: BEAT.chop - 14, to: [570, 520] },
      { at: BEAT.chop, to: [600, 780] },
    ],
    16,
    260,
  );
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.final), [1.02, 1.08], clamp)} focus={[540, 900]} drift={3} seed={6}>
        <Room blur={7} />
        <Actor
          x={40}
          y={420}
          s={1.35}
          f={f}
          who={RAO}
          shadow
          torso={200}
          expr="neutral"
          viseme={vis("rao-final", f)}
          turn={shakeHead}
          tilt={talkTilt(["rao-final"], f, 0, 1.4)}
          gaze={[0.1, 0.1]}
          blink={blinkK(f, 6)}
          arms={f > BEAT.chop - 18 ? { r: { to: hand, pose: "palm", rot: -75 } } : {}}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 6. "Arjun, pricing deck by Thursday." Arjun nods fast. */
const DeckShot: React.FC = () => {
  const f = useT() + SCENES.deck.from;
  const arm = springPt(f, reach("l", 96, 0.97, BROAD), [{ at: BEAT.pointArjun, to: reach("l", 152, 0.95, BROAD) }], 13, 150);
  const turn = springAt(f, 0, [{ at: BEAT.pointArjun, to: -0.2 }], 13, 140);
  const nod = damped(f, BEAT.nodA, 7, 2.4, 4);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.deck), [1.05, 1.08], clamp)} focus={[540, 1150]} drift={4} seed={2}>
        <SyncWide f={f} arm={arm} pointing={f > BEAT.pointArjun + 2} raoTurn={turn} arjunNod={nod} gazeA={[0.9, -0.9]} />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 7. CU Rao, thumbs up: "Someone note it down!" */
const NoteShot: React.FC = () => {
  const f = useT() + SCENES.note.from;
  const arm = springPt(f, [600, 1040], [{ at: BEAT.thumb - 4, to: [565, 470] }], 10, 170);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.note), [1, 1.05], clamp)} focus={[540, 900]} drift={3} seed={4}>
        <Room blur={7} board={false} />
        <Actor
          x={10}
          y={520}
          s={1.45}
          f={f}
          who={RAO}
          shadow
          torso={120}
          expr="smile"
          viseme={vis("rao-note", f)}
          tilt={talkTilt(["rao-note"], f, 2, 2.2)}
          gaze={[0.1, 0]}
          blink={blinkK(f, 6)}
          arms={f >= BEAT.thumb - 6 ? { r: { to: arm, pose: "thumb", upright: true, rot: 90 } } : {}}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 8. The side-eye chain: Arjun looks at Neha, Neha looks at Meera, Meera looks at us. */
const Eyes: React.FC = () => {
  const f = useT() + SCENES.eyes.from;
  const band = (y: number, fill: string, at: number, child: React.ReactNode, k: number) => {
    const s = springAt(f, 0, [{ at, to: 1 }], 16, 170);
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
  const glance = (at: number, from: [number, number], to: [number, number]): [number, number] => {
    const k = springAt(f, 0, [{ at, to: 1 }], 18, 260);
    return [from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k];
  };
  return (
    <Frame bg={["#0F172A", "#0F172A"]}>
      {band(0, "#E0E7FF", BEAT.bandA, <Actor x={130} y={-70} s={0.95} f={f} who={ARJUN} expr="sideeye" gaze={glance(BEAT.bandA + 10, [0, 0], [0.5, 1])} blink={blinkK(f, 2)} />, 0)}
      {band(640, "#FEF3C7", BEAT.bandN, <Actor x={130} y={570} s={0.95} f={f} who={NEHA} expr="sideeye" gaze={glance(BEAT.bandN + 10, [0, 0], [-0.4, 1])} blink={blinkK(f, 7)} />, 1)}
      {band(
        1280,
        "#FDE7C7",
        BEAT.bandM,
        <g transform={`translate(540 1600) scale(${interpolate(f, [BEAT.bandM + 8, BEAT.bandM + 20], [1, 1.06], clamp)}) translate(-540 -1600)`}>
          <Actor x={130} y={1210} s={0.95} f={f} who="meera" expr="smug" turn={0.15} gaze={glance(BEAT.bandM + 14, [0.6, 0], [0, 0])} sway={Math.sin(f / 20) * 0.4} />
        </g>,
        2,
      )}
      <path d={`M0 640 L${W} 640 M0 1280 L${W} 1280`} stroke="#0F172A" strokeWidth={14} />
    </Frame>
  );
};

/* 9. Under the table, Meera taps the Srutam dock. "Nobody wrote it down. Except Srutam." */
const Dock: React.FC = () => {
  const f = useT() + SCENES.dock.from;
  const wrist = springPt(f, [-260, 1340], [{ at: BEAT.tap - 16, to: [133, 1056] }], 16, 150);
  const press = f >= BEAT.tap && f < BEAT.tap + 5 ? 0.92 : 1;
  const ripple = interpolate(f, [BEAT.tap, BEAT.tap + 18], [0, 1], clamp);
  return (
    <Frame bg={["#3F2E22", "#1F1712"]} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.dock), [1, 1.06], clamp)} focus={[420, 1000]} drift={3} seed={7}>
        <rect x={0} y={0} width={W} height={250} fill="#6B4F3A" />
        <rect x={0} y={230} width={W} height={36} fill="#8B6B52" stroke={LINE} strokeWidth={5} />
        <ellipse cx={540} cy={1500} rx={620} ry={420} fill="#C4B5A0" opacity={0.25} />
        <Phone x={250} y={420} w={560} rot={-4} glow={f >= BEAT.tap ? "#EF4444" : undefined}>
          <DockOverApp w={560} recording={f >= BEAT.tap + 2} label="Team chat" />
        </Phone>
        <g transform="translate(560 1720) rotate(-92) scale(1.9)">
          <Hand pose="grip" skin="#F2C3A2" />
        </g>
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

/* 10. Decision. Next step. Reminder. Before lunch. */
const Cards: React.FC = () => {
  const f = useT() + SCENES.cards.from;
  const appear = [BEAT.cardDecision, BEAT.cardStep, BEAT.cardReminder].map((at) => springAt(f, 0, [{ at, to: 1 }], 13, 160));
  const sticker = springAt(f, 0, [{ at: BEAT.lunch, to: 1 }], 12, 170);
  return (
    <Frame bg={["#0F172A", "#1E293B"]} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.cards), [1, 1.07], { ...clamp, easing: easeInOut })} focus={[500, 900]} drift={3} seed={9}>
        <Phone x={150} y={170} w={700} glow="#3B82F6">
          <InsightsScreen
            w={700}
            note="Tuesday sync"
            meta="Tue, 11:02 am · 32:14"
            appear={appear}
            items={[
              { kind: "DECISION", text: "Launch on the 14th" },
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
      <LightLeak k={interpolate(f, [SCENES.week.from - 22, SCENES.week.from + 4], [0, 1], clamp)} />
    </Frame>
  );
};

/* 11. One week later: the calendar page tears off and falls. */
const Week: React.FC = () => {
  const f = useT() + SCENES.week.from;
  const torn = f >= BEAT.pageTear;
  const swing = torn ? Math.min(1, (f - BEAT.pageTear) / 6) : 0;
  const fall = ballistic(f, BEAT.pageTear + 6, [0, 0], [380, -500], 3000);
  const rot = torn ? -swing * 25 - Math.max(0, f - BEAT.pageTear - 6) * 6 : 0;
  const title = interpolate(f, [BEAT.pageTear + 10, BEAT.pageTear + 24], [0, 1], { ...clamp, easing: easeOut });
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

/* 12. Back to the outburst: Rao glares; "Neha did!" "Arjun did!" */
const Blame: React.FC = () => {
  const f = useT() + SCENES.blame.from;
  const a = springAt(f, 0, [{ at: BEAT.pointA, to: 1 }], 11, 160);
  const n = springAt(f, 0, [{ at: BEAT.pointN, to: 1 }], 11, 160);
  const arjunHand = springPt(f, reach("r", 92, 0.9, BROAD), [{ at: BEAT.pointA, to: reach("r", -40, 0.45, BROAD) }], 11, 160);
  const nehaHand = springPt(f, reach("l", 88, 0.9), [{ at: BEAT.pointN, to: reach("l", 190, 0.45) }], 11, 160);
  const glare = springAt(f, -0.3, [{ at: BEAT.pointA + 4, to: -0.15 }, { at: BEAT.pointN + 4, to: 0.15 }], 14, 150);
  const drop = (at: number, x: number, y: number) => {
    if (f < at) return null;
    const p = ballistic(f, at, [x, y], [-60, -280], 2600);
    return p[1] < y + 500 ? (
      <g key={at} transform={`translate(${p[0] - x} ${p[1] - y})`}>
        <Sweat x={x} y={y} s={1.1} />
      </g>
    ) : null;
  };
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.blame), [1, 1.06], { ...clamp, easing: easeInOut })} focus={[540, 1000]} drift={3} seed={13}>
        <Room blur={5} board={false} />
        <Depth blur={2}>
          <Actor x={240} y={260} s={0.8} f={f} who={RAO} shadow torso={560} expr="angry" turn={glare} gaze={[glare * 3, 0.4]} blink={blinkK(f, 2)} />
        </Depth>
        <Actor
          x={-80}
          y={560}
          s={0.9}
          f={f}
          who={ARJUN}
          shadow
          torso={240}
          expr="worried"
          viseme={vis("arjun-her", f)}
          turn={0.12 + 0.15 * a}
          tilt={3 * a}
          gaze={[0.3 + 0.6 * a, -0.1]}
          blink={blinkK(f, 4)}
          arms={a > 0.03 ? { r: { to: arjunHand, pose: a > 0.3 ? "point" : "fist", rot: 72 * a } } : {}}
        />
        <Actor
          x={560}
          y={560}
          s={0.9}
          f={f}
          who={NEHA}
          shadow
          torso={240}
          expr="angry"
          viseme={vis("neha-him", f, "A")}
          turn={-0.12 - 0.15 * n}
          tilt={-3 * n}
          gaze={[-0.3 - 0.6 * n, -0.1]}
          blink={blinkK(f, 9)}
          arms={n > 0.03 ? { l: { to: nehaHand, pose: n > 0.3 ? "point" : "fist", rot: -55 * n } } : {}}
        />
        <MeetingTable y={1450} />
        {drop(BEAT.pointA + 8, 330, 820)}
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

const Q = "When did we agree on the 14th?";
const A = "Tuesday's sync, 0:42: “No, no. The 14th. Final.”";

/* 13. Meera lifts Srutam: "Relax. Let's ask Srutam." The answer is a time, a quote and a source. */
const Ask: React.FC = () => {
  const f = useT() + SCENES.ask.from;
  const raise = springAt(f, 0, [{ at: BEAT.raise, to: 1 }], 13, 110);
  const M = { x: -170, y: 860, s: 1.2 };
  const hand: Pt = [606, 383 + (1 - raise) * 330];
  const hw: Pt = [M.x + hand[0] * M.s, M.y + hand[1] * M.s];
  const qn = Math.round(interpolate(f, [BEAT.qStart, BEAT.qEnd], [0, Q.length], clamp));
  const an = Math.round(interpolate(f, [BEAT.aStart, BEAT.aEnd], [0, A.length], clamp));
  const tapK = interpolate(f, [BEAT.tapRef, BEAT.tapRef + 12], [0, 1], clamp);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, [BEAT.qStart, SCENES.play.from], [1, 1.1], { ...clamp, easing: easeInOut })} focus={[700, 900]} drift={3} seed={17}>
        <Depth blur={7}>
          <OfficeWall board={false} />
          <Actor x={620} y={1040} s={0.62} f={f} who={RAO} torso={300} expr="angry" gaze={[-0.8, -0.4]} />
        </Depth>
        <g transform={`rotate(${(1 - raise) * 10 - 3} ${hw[0]} ${hw[1]})`}>
          <Phone x={hw[0] - 70} y={hw[1] - 1010} w={470} glow={f >= BEAT.qStart ? "#3B82F6" : undefined}>
            <AskScreen w={470} q={Q.slice(0, qn)} a={A.slice(0, an)} refs={f >= BEAT.refs ? ["Tuesday sync · 0:42"] : []} pressed={f >= BEAT.tapRef && f < BEAT.tapRef + 8} />
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
          tilt={talkTilt(["m-ask"], f, 0, 1.4)}
          sway={follow((g) => talkTilt(["m-ask"], g, 0, 1.4), f, 4, 0.3)}
          gaze={f < BEAT.qStart ? [0.7, -0.1] : [0.8, -0.6]}
          blink={blinkK(f, 1)}
          arms={{ r: { to: hand, pose: "grip", upright: true } }}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

/* 14. The source plays and the room hears Rao himself. Focus pulls from the phone to his face. */
const Play: React.FC = () => {
  const f = useT() + SCENES.play.from;
  const p0 = BEAT.play;
  const rack = interpolate(f, [p0 + 6, p0 + 24], [0, 1], { ...clamp, easing: easeInOut });
  const progress = interpolate(f, [p0, p0 + 60], [0.4, 0.46], clamp);
  const sink = springAt(f, 1, [{ at: BEAT.realise, to: 0.95 }], 16, 80);
  const raoExpr = f < p0 + 10 ? "angry" : f < BEAT.realise ? "surprised" : "worried";
  const look = springAt(f, 0, [{ at: p0 + 20, to: 1 }], 14, 140);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, [p0, p0 + 74], [1.02, 1.12], { ...clamp, easing: easeInOut })} focus={[540, 900]} drift={3} seed={21}>
        <Depth blur={3 + 2 * (1 - rack)}>
          <OfficeWall board={false} />
        </Depth>
        <Depth blur={5 * (1 - rack)}>
          <Actor x={-90} y={700} s={0.78} f={f} who={ARJUN} shadow torso={300} expr={look > 0.5 ? "smug" : "worried"} turn={0.25 * look} gaze={[0.9 * look, -0.2]} blink={blinkK(f, 4)} />
          <Actor x={560} y={700} s={0.78} f={f} who={NEHA} shadow torso={300} expr={look > 0.5 ? "smug" : "neutral"} turn={-0.25 * look} gaze={[-0.9 * look, -0.2]} blink={blinkK(f, 9)} />
          <Actor x={240} y={560} s={0.95} f={f} who={RAO} shadow torso={320} expr={raoExpr} squash={sink} tilt={f > BEAT.realise ? -2 : 0} gaze={f < BEAT.realise ? [-0.2, 0.9] : [-0.6, 0.5]} blink={f > BEAT.realise ? blinkK(f, 2) : 0} />
          {f > BEAT.realise + 6 && <Sweat x={655} y={840 + ((f - BEAT.realise - 6) % 24) * 5} s={1.1} />}
          <MeetingTable y={1450} />
        </Depth>
        <Depth blur={4 * rack}>
          <Phone x={70} y={1120} w={470} rot={-6} glow="#FBBF24">
            <PlaybackScreen w={470} title="Tuesday sync" meta="0:42 of 32:14 · Tue, 11:02 am" quote="“No, no. The 14th. Final.”" progress={progress} />
          </Phone>
        </Depth>
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

/* 15. Rao owns it: "Okay... so the 14th is final, then." The room exhales and laughs. */
const Payoff: React.FC = () => {
  const f = useT() + SCENES.payoff.from;
  const rub = springPt(f, [130, 900], [{ at: BEAT.rub, to: [150, 300] }], 13, 120);
  const rubbing: Pt = [rub[0] + (f > BEAT.rub + 10 ? Math.sin(f / 3) * 12 : 0), rub[1]];
  const laugh = f >= BEAT.laugh;
  const bob = (seed: number) => (laugh ? Math.abs(Math.sin((f + seed) / 5)) * 8 * Math.min(1, (f - BEAT.laugh) / 10) : 0);
  const zoom = interpolate(f, [SCENES.payoff.from, BEAT.laugh - 4, SCENES.end.from], [1.12, 1.12, 1.0], { ...clamp, easing: easeInOut });
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={zoom} focus={[540, 900]} drift={3} seed={23}>
        <Room blur={5} board={false} />
        <g transform={`translate(0 ${-bob(0)})`}>
          <Actor x={-170} y={640} s={0.82} f={f} who={ARJUN} shadow torso={300} expr={laugh ? "laugh" : "smug"} tilt={laugh ? -3 : 0} gaze={[0.8, -0.2]} blink={blinkK(f, 4)} />
        </g>
        <g transform={`translate(0 ${-bob(9)})`}>
          <Actor x={650} y={640} s={0.82} f={f} who={NEHA} shadow torso={300} expr={laugh ? "laugh" : "smug"} tilt={laugh ? 3 : 0} gaze={[-0.8, -0.2]} blink={blinkK(f, 9)} />
        </g>
        <Actor
          x={200}
          y={540}
          s={1.0}
          f={f}
          who={RAO}
          shadow
          torso={320}
          expr={laugh ? "laugh" : "sheepish"}
          viseme={vis("rao-sheepish", f)}
          tilt={talkTilt(["rao-sheepish"], f, -4, 1.4)}
          gaze={laugh ? [0, 0] : [-0.5, 0.3]}
          blink={blinkK(f, 6)}
          arms={{ l: { to: rubbing, pose: "palm", rot: 25 } }}
        />
        <MeetingTable y={1500} />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

/* 16. Everyone laughs. "Every meeting, minuted." */
const End: React.FC = () => {
  const f = useT() + SCENES.end.from;
  const bob = (seed: number) => Math.abs(Math.sin((f + seed) / 5)) * 8;
  const band = interpolate(f, [BEAT.band, BEAT.band + 20], [0, 1], { ...clamp, easing: easeOut });
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} drift={3} seed={19}>
        <Room blur={5} board={false} />
        <g transform={`translate(0 ${-bob(0)})`}>
          <Actor x={-170} y={600} s={0.82} f={f} who={ARJUN} shadow torso={300} expr="laugh" tilt={-3} />
        </g>
        <g transform={`translate(0 ${-bob(9)})`}>
          <Actor x={650} y={600} s={0.82} f={f} who={NEHA} shadow torso={300} expr="laugh" tilt={3} />
        </g>
        <g transform={`translate(0 ${-bob(4) * 0.6})`}>
          <Actor x={200} y={700} s={0.98} f={f} who={RAO} shadow torso={300} expr="laugh" tilt={-4} />
        </g>
      </Camera>
      <g transform={`translate(0 ${(1 - band) * 420})`}>
        <EndBand tagline="Every meeting, minuted." />
      </g>
    </Frame>
  );
};

const LIST = [
  ["1 Cold open", SCENES.open, Open],
  ["2 One week earlier", SCENES.freeze, Freeze],
  ["3 Board", SCENES.board, BoardShot],
  ["4 Arjun: the 21st", SCENES.arjun, ArjunShot],
  ["5 Rao: the 14th, final", SCENES.final, FinalShot],
  ["6 Pricing deck", SCENES.deck, DeckShot],
  ["7 Note it down", SCENES.note, NoteShot],
  ["8 Side-eye", SCENES.eyes, Eyes],
  ["9 Dock", SCENES.dock, Dock],
  ["10 Cards", SCENES.cards, Cards],
  ["11 One week later", SCENES.week, Week],
  ["12 Blame", SCENES.blame, Blame],
  ["13 Let's ask", SCENES.ask, Ask],
  ["14 Playback", SCENES.play, Play],
  ["15 Payoff", SCENES.payoff, Payoff],
  ["16 End", SCENES.end, End],
] as const;

export const WhoVideo: React.FC = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    {LIST.map(([name, s, Comp]) => (
      <Sequence key={name} name={name} from={s.from * K} durationInFrames={s.duration * K}>
        <Comp />
      </Sequence>
    ))}
    <Captions cues={CAPTIONS} k={K} />
    <Soundtrack music="meeting/music2.wav" musicVolume={0.42} lines={VO} sfx={SFX} k={K} sfxGain={0.6} duck={0.35} />
  </AbsoluteFill>
);
