// "Who Said That?" restaged for 16:9 with English dialogue. Same story, cast and beats
// as src/who/WhoVideo.tsx, laid out for a 1920 x 1080 room: Rao at the whiteboard and
// the team across the table in the wide shots, Arjun and Rao facing each other across
// the frame in the singles, the side-eye as three columns, Meera's phone in the
// foreground with the team waiting behind it, and the band under the laugh.
import { AbsoluteFill, interpolate, Sequence } from "remotion";
import { clamp, easeIn, easeInOut, easeOut } from "../../anim";
import { ARJUN, NEHA, RAO } from "../../cast/cast";
import { Actor, blinkK, BROAD, Hand, reach, type Pt } from "../../cast/Rig";
import { Camera, Depth, Grade, LightLeak } from "../../stories/Camera";
import { HandsOnPhone } from "../../stories/Hands";
import { ballistic, damped, follow, springAt, springPt } from "../../stories/physics";
import { Soundtrack } from "../../stories/Story";
import { K, useT } from "../../stories/time";
import { thumbs, typed } from "../../stories/typing";
import { voice } from "../../stories/voice";
import { AskFlow, Board, DockOverApp, Frame, InsightsScreen, LINE, NoteDetail, Phone, Sweat } from "../../storyboards/Kit";
import { Landscape, LCaptions, LH, LW } from "../kit";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";
import { VISEMES } from "./visemes";

const { vis, visAny, talkTilt, talking, emph } = voice(VO, VISEMES);
const RAO_LINES = ["rao-who", "rao-launch", "rao-final", "rao-deck", "rao-note", "rao-sheepish"];
const BOARD = ["LAUNCH:", "14th?? 21st??", "pricing → ?"];
const OFFICE: [string, string] = ["#DCE3EC", "#C7D2E0"];
const span = (sc: { from: number; duration: number }): [number, number] => [sc.from, sc.from + sc.duration];
const toward = (f: number, at: number, from: number, to: number, len = 10) => from + (to - from) * interpolate(f, [at, at + len], [0, 1], { ...clamp, easing: easeInOut });

/** The meeting room across the full width: window and plant on the right, the board on the left. */
const OfficeWallL: React.FC<{ board?: boolean }> = ({ board = true }) => (
  <g>
    <rect width={LW} height={LH} fill="#DCE3EC" />
    <rect x={1330} y={70} width={430} height={540} fill="#BFDBFE" stroke={LINE} strokeWidth={8} />
    {[0, 1, 2, 3, 4].map((i) => (
      <rect key={i} x={1358 + i * 80} y={390 - i * 38} width={58} height={220 + i * 38} fill="#94A3B8" />
    ))}
    <path d="M1545 70 L1545 610 M1330 330 L1760 330" stroke={LINE} strokeWidth={6} />
    {board && <Board x={120} y={100} w={620} h={420} lines={BOARD} />}
    <g transform="translate(1830 820)">
      <path d="M-44 0 L44 0 L33 130 L-33 130 Z" fill="#B45309" stroke={LINE} strokeWidth={5} />
      {[-54, -22, 10, 42].map((x, i) => (
        <ellipse key={i} cx={x} cy={-44 - (i % 2) * 44} rx={36} ry={76} fill="#15803D" stroke={LINE} strokeWidth={4} transform={`rotate(${x / 2} ${x} -20)`} />
      ))}
    </g>
  </g>
);

/** The meeting table across the frame, with a laptop and a mug on it. */
const TableL: React.FC<{ y: number; props?: boolean }> = ({ y, props = true }) => (
  <g>
    <path d={`M-40 ${y} L${LW + 40} ${y} L${LW + 40} ${LH + 40} L-40 ${LH + 40} Z`} fill="#6B4F3A" />
    <rect x={-40} y={y} width={LW + 80} height={34} fill="#8B6B52" stroke={LINE} strokeWidth={5} />
    {props && (
      <>
        <g transform={`translate(250 ${y - 28})`}>
          <path d="M0 28 L130 28 L112 -46 L18 -46 Z" fill="#CBD5E1" stroke={LINE} strokeWidth={4} />
        </g>
        <g transform={`translate(1680 ${y - 4})`}>
          <path d="M0 0 L56 0 L50 -66 L6 -66 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={4} />
          <path d="M54 -52 q28 10 0 32" stroke={LINE} strokeWidth={5} fill="none" />
        </g>
      </>
    )}
  </g>
);

/** Meera's phone on the table, recording (a red dot that breathes). */
const Recorder: React.FC<{ f: number; x: number; y: number }> = ({ f, x, y }) => {
  const dot = 0.5 + 0.5 * Math.sin(f / 6);
  return (
    <g transform={`translate(${x} ${y}) rotate(-6)`}>
      <rect x={-64} y={-22} width={128} height={56} rx={13} fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
      <circle cx={-42} cy={6} r={8} fill="#EF4444" opacity={0.4 + 0.6 * dot} />
      <circle cx={-42} cy={6} r={18} fill="#EF4444" opacity={0.18 * dot} />
    </g>
  );
};

/* 1. Cold open, a week later: Rao slams the table and glares at the team. */
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
  const cup = ballistic(Math.min(f, land), imp, [0, 0], [-260, -620], 3600);
  const cupRot = after ? interpolate(f, [imp, land], [0, -18], clamp) + damped(f, land, 8, 3, 7) : 0;
  const turn = after ? 0.3 * Math.sin((f - imp) / 22) : 0.1;
  const tableY = 930;
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.open), [1.02, 1.08], { ...clamp, easing: easeInOut })} focus={[900, 520]} shake={sh} drift={3} seed={11}>
        <Depth blur={6}>
          <OfficeWallL board={false} />
        </Depth>
        <Actor
          x={600}
          y={tableY - 905}
          s={1}
          f={f}
          who={RAO}
          shadow
          torso={220}
          expr="angry"
          viseme={vis("rao-who", f, "B")}
          tilt={talkTilt(["rao-who"], f, 2, 3)}
          turn={turn}
          squash={squash}
          gaze={[turn * 2.4, 0.15]}
          blink={f < imp ? blinkK(f, 2) : 0}
          arms={{ r: { to: hand, pose: "fist", rot: f < imp - 5 ? -60 : 70 } }}
        />
        <g transform={`translate(0 ${after ? damped(f, imp, 6, 5, 9) : 0})`}>
          <TableL y={tableY} props={false} />
          <ellipse cx={1180} cy={tableY + 10} rx={420} ry={10} fill="#FFFFFF" opacity={0.12} />
        </g>
        <g transform={`translate(${560 + cup[0]} ${tableY - 80 + Math.min(0, cup[1])}) rotate(${cupRot} 35 80)`}>
          <path d="M0 0 L70 0 L62 80 L8 80 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={5} />
          <path d="M66 18 q26 6 0 36" stroke={LINE} strokeWidth={5} fill="none" />
        </g>
      </Camera>
      <rect width={LW} height={LH} fill="#DC2626" opacity={after ? 0.1 * Math.exp(-(f - imp) / 20) + 0.04 : 0} style={{ mixBlendMode: "multiply" }} />
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 2. The frame freezes and drains of colour: "One week earlier". */
const Freeze: React.FC = () => {
  const f = useT() + SCENES.freeze.from;
  const k = interpolate(f, [SCENES.freeze.from, SCENES.freeze.from + 12], [0, 1], { ...clamp, easing: easeOut });
  const title = interpolate(f, [SCENES.freeze.from + 6, SCENES.freeze.from + 18], [0, 1], { ...clamp, easing: easeOut });
  return (
    <AbsoluteFill>
      <AbsoluteFill style={{ filter: `grayscale(${k}) brightness(${1 - 0.35 * k})`, scale: `${1 + 0.03 * k}` }}>
        <Open at={BEAT.freeze} />
      </AbsoluteFill>
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", opacity: title, translate: `0 ${(1 - title) * 24}px` }}>
        <div style={{ fontFamily: "Playfair Display", fontStyle: "italic", fontWeight: 700, fontSize: 120, color: "#FFF7ED", textShadow: "0 4px 30px rgba(0,0,0,0.5)" }}>One week earlier</div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

/* 3. Tuesday, 11:00, before anyone speaks: under the table Meera taps Srutam's dock. */
const DockStart: React.FC = () => {
  const f = useT() + SCENES.dock.from;
  const PX = 760;
  const PY = 150;
  const PW = 400;
  const PH = PW * (2400 / 1080);
  const dock: Pt = [PX + 0.032 * PW, PY + 0.406 * PH];
  const s = PW / 560;
  const wrist = springPt(f, [dock[0] - 560, dock[1] + 520], [{ at: BEAT.tap - 16, to: [dock[0] - 96, dock[1] + 94] }, { at: BEAT.tap + 10, to: [dock[0] - 300, dock[1] + 230] }], 16, 140);
  const press = f >= BEAT.tap && f < BEAT.tap + 5 ? 0.92 : 1;
  const ripple = interpolate(f, [BEAT.tap, BEAT.tap + 18], [0, 1], clamp);
  const chip = interpolate(f, [SCENES.dock.from + 4, SCENES.dock.from + 12, SCENES.dock.from + 52, SCENES.dock.from + 60], [0, 1, 1, 0], clamp);
  return (
    <Frame bg={["#3F2E22", "#1F1712"]} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.dock), [1, 1.05], clamp)} focus={[dock[0], dock[1]]} drift={3} seed={7}>
        <rect x={0} y={0} width={LW} height={130} fill="#6B4F3A" />
        <rect x={0} y={112} width={LW} height={34} fill="#8B6B52" stroke={LINE} strokeWidth={5} />
        <ellipse cx={960} cy={760} rx={760} ry={360} fill="#C4B5A0" opacity={0.22} />
        <Phone x={PX} y={PY} w={PW} rot={-4} glow={f >= BEAT.tap ? "#EF4444" : undefined}>
          <DockOverApp w={PW} recording={f >= BEAT.tap + 2} label="Team chat" />
        </Phone>
        <g transform={`translate(${PX + 310 * s} ${PY + 1300 * s}) rotate(-92) scale(${1.9 * s})`}>
          <Hand pose="grip" skin="#F2C3A2" />
        </g>
        <g transform={`translate(${wrist[0]} ${wrist[1]}) rotate(-35) scale(${s * press} ${s})`}>
          <path d="M-760 -46 L-6 -34 L-6 34 L-760 46 Z" fill="#EFE6D8" stroke={LINE} strokeWidth={5} />
          <g transform="scale(1.9)">
            <Hand pose="point" skin="#F2C3A2" />
          </g>
        </g>
        {ripple > 0 && ripple < 1 && <circle cx={dock[0]} cy={dock[1]} r={22 + ripple * 90} fill="none" stroke="#FFFFFF" strokeWidth={8} opacity={1 - ripple} />}
      </Camera>
      <g opacity={chip}>
        <rect x={760} y={22} width={400} height={78} rx={20} fill="#0F172A" opacity={0.78} />
        <text x={960} y={75} textAnchor="middle" fill="#FFF" fontFamily="Inter" fontWeight={700} fontSize={40}>
          Tuesday, 11:00 am
        </text>
      </g>
    </Frame>
  );
};

/** Tuesday's room, wide: Rao standing at the board on the left; the team at the table on the right. */
const SyncWide: React.FC<{
  f: number;
  arms: Parameters<typeof Actor>[0]["arms"];
  raoTurn: number;
  raoGaze: [number, number];
  arjunNod?: number;
  arjunLook?: number;
  nehaLook?: number;
  meeraLook?: number;
}> = ({ f, arms, raoTurn, raoGaze, arjunNod = 0, arjunLook = -0.45, nehaLook = -0.55, meeraLook = -0.45 }) => (
  <g>
    <Depth blur={2}>
      <OfficeWallL />
    </Depth>
    <Actor
      x={640}
      y={150}
      s={0.6}
      f={f}
      who={RAO}
      shadow
      torso={600}
      expr="smile"
      viseme={visAny(RAO_LINES, f)}
      turn={raoTurn}
      tilt={talkTilt(RAO_LINES, f)}
      gaze={raoGaze}
      blink={blinkK(f, 3)}
      arms={arms}
    />
    <Actor x={880} y={430} s={0.62} f={f} who={ARJUN} shadow expr="smile" tilt={arjunNod + 2} turn={arjunLook} gaze={[arjunLook * 1.6, -0.5]} blink={blinkK(f, 5)} />
    <Actor x={1190} y={450} s={0.6} f={f} who="meera" shadow expr="blank" turn={meeraLook} gaze={[meeraLook * 1.4, -0.5]} blink={blinkK(f, 1)} sway={Math.sin(f / 30) * 0.5} />
    <Actor x={1490} y={430} s={0.62} f={f} who={NEHA} shadow expr="smile" tilt={-2} turn={nehaLook} gaze={[nehaLook * 1.6, -0.5]} blink={blinkK(f, 8)} />
    <TableL y={900} />
    <Recorder f={f} x={1150} y={935} />
  </g>
);

/* 4. "So... do we launch on the 14th?" Rao taps the board; the team watches him. */
const BoardShot: React.FC = () => {
  const f = useT() + SCENES.board.from;
  const at = SCENES.board.from + 6;
  const arm = springPt(
    f,
    reach("l", 96, 0.97, BROAD),
    [
      { at, to: reach("l", 197, 0.97, BROAD) },
      { at: BEAT.boardTap, to: reach("l", 203, 1.0, BROAD) },
      { at: BEAT.boardTap + 5, to: reach("l", 197, 0.96, BROAD) },
    ],
    14,
    140,
  );
  const turn = springAt(f, 0.1, [{ at, to: -0.4 }, { at: BEAT.boardTap + 30, to: 0.35 }], 14, 120);
  const pointing = f > at + 2;
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.board), [1, 1.04], { ...clamp, easing: easeInOut })} focus={[800, 560]} drift={4}>
        <SyncWide f={f} arms={{ l: { to: arm, pose: pointing ? "point" : "open", rot: pointing ? 0 : 70 } }} raoTurn={turn} raoGaze={pointing && turn < 0 ? [-0.8, -0.2] : [turn * 2, 0.3]} />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 5. Arjun, carefully, toward Rao: "Sir, the 21st would be safer..." Rao, out of focus, listens. */
const ArjunShot: React.FC = () => {
  const f = useT() + SCENES.arjun.from;
  const e = emph(["arjun-21"], f);
  const hand = springPt(f, reach("r", 95, 0.85), [{ at: BEAT.arjunHand, to: reach("r", -35, 0.62) }], 14, 120);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.arjun), [1, 1.04], clamp)} focus={[1200, 560]} drift={3} seed={4}>
        <Depth blur={8}>
          <OfficeWallL />
          <Actor x={500} y={150} s={0.62} f={f} who={RAO} torso={600} expr="neutral" turn={0.45} gaze={[0.9, 0.3]} />
          <TableL y={930} />
        </Depth>
        <Actor
          x={1020}
          y={120}
          s={0.98}
          f={f}
          who={ARJUN}
          shadow
          torso={160}
          expr="worried"
          viseme={vis("arjun-21", f)}
          tilt={talkTilt(["arjun-21"], f, -2, 1.6)}
          turn={-0.55}
          gaze={[-0.9, -0.3]}
          blink={blinkK(f, 5)}
          arms={f > BEAT.arjunHand - 2 ? { r: { to: [hand[0], hand[1] - e * 24], pose: "palm", rot: -30 } } : {}}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 6. Rao, to Arjun: "No, no. The 14th. Final." A head shake, then a palm chop on "final". */
const FinalShot: React.FC = () => {
  const f = useT() + SCENES.final.from;
  const shakeHead = 0.42 + damped(f, BEAT.shake, 0.22, 2.4, 2.6);
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
      <Camera f={f} zoom={interpolate(f, span(SCENES.final), [1.0, 1.05], clamp)} focus={[700, 520]} drift={3} seed={6}>
        <Depth blur={7}>
          <OfficeWallL />
          <Actor x={1340} y={330} s={0.55} f={f} who={ARJUN} expr="worried" turn={-0.5} gaze={[-0.8, -0.2]} />
          <TableL y={930} />
        </Depth>
        <Actor
          x={330}
          y={110}
          s={1}
          f={f}
          who={RAO}
          shadow
          torso={200}
          expr="neutral"
          viseme={vis("rao-final", f)}
          turn={shakeHead}
          tilt={talkTilt(["rao-final"], f, 0, 1.6)}
          gaze={[0.9, 0.2]}
          blink={blinkK(f, 6)}
          arms={f > BEAT.chop - 18 ? { r: { to: hand, pose: "palm", rot: -75 } } : {}}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 7. "Arjun. Pricing deck, by Thursday." He points across the table; Arjun nods fast. */
const DeckShot: React.FC = () => {
  const f = useT() + SCENES.deck.from;
  const point = springPt(f, reach("r", 84, 0.97, BROAD), [{ at: BEAT.pointArjun, to: reach("r", 24, 0.95, BROAD) }], 13, 150);
  const turn = springAt(f, 0.2, [{ at: BEAT.pointArjun, to: 0.35 }], 13, 140);
  const nod = damped(f, BEAT.nodA, 7, 2.4, 4);
  const nehaLook = toward(f, BEAT.nodA - 4, -0.55, -0.75, 8);
  const pointing = f > BEAT.pointArjun + 2;
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.deck), [1.03, 1.06], clamp)} focus={[1000, 560]} drift={4} seed={2}>
        <SyncWide f={f} arms={{ r: { to: point, pose: pointing ? "point" : "open", rot: pointing ? 0 : -70 } }} raoTurn={turn} raoGaze={[0.8, 0.5]} arjunNod={nod} nehaLook={nehaLook} />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 8. CU Rao to the room, thumbs up: "Someone note it down!" */
const NoteShot: React.FC = () => {
  const f = useT() + SCENES.note.from;
  const arm = springPt(f, [600, 1040], [{ at: BEAT.thumb - 4, to: [565, 470] }], 10, 170);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.note), [1, 1.04], clamp)} focus={[960, 500]} drift={3} seed={4}>
        <Depth blur={7}>
          <OfficeWallL />
        </Depth>
        <Actor
          x={640}
          y={100}
          s={1.02}
          f={f}
          who={RAO}
          shadow
          torso={160}
          expr="smile"
          viseme={vis("rao-note", f)}
          tilt={talkTilt(["rao-note"], f, 2, 2.2)}
          turn={0.15 * Math.sin((f - SCENES.note.from) / 14)}
          gaze={[0.3 * Math.sin((f - SCENES.note.from) / 14), 0.1]}
          blink={blinkK(f, 6)}
          arms={f >= BEAT.thumb - 6 ? { r: { to: arm, pose: "thumb", upright: true, rot: 90 } } : {}}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.16} />
    </Frame>
  );
};

/* 9. The side-eye chain, as three columns: Arjun looks at Neha, Neha at Meera, Meera at us. */
const Eyes: React.FC = () => {
  const f = useT() + SCENES.eyes.from;
  const col = (x: number, fill: string, at: number, child: React.ReactNode, k: number) => {
    const s = springAt(f, 0, [{ at, to: 1 }], 16, 170);
    return (
      <g key={k} transform={`translate(${(1 - s) * LW} 0)`}>
        <clipPath id={`eyecol${k}`}>
          <rect x={x} y={0} width={640} height={LH} />
        </clipPath>
        <rect x={x} y={0} width={640} height={LH} fill={fill} />
        <g clipPath={`url(#eyecol${k})`}>{child}</g>
      </g>
    );
  };
  const glance = (at: number, from: [number, number], to: [number, number]): [number, number] => {
    const k = springAt(f, 0, [{ at, to: 1 }], 18, 260);
    return [from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k];
  };
  return (
    <Frame bg={["#0F172A", "#0F172A"]}>
      {col(0, "#E0E7FF", BEAT.bandA, <Actor x={35} y={170} s={0.95} f={f} who={ARJUN} expr="sideeye" turn={0.25} gaze={glance(BEAT.bandA + 10, [0, 0], [1, 0.1])} blink={blinkK(f, 2)} />, 0)}
      {col(640, "#FEF3C7", BEAT.bandN, <Actor x={675} y={170} s={0.95} f={f} who={NEHA} expr="sideeye" turn={0.2} gaze={glance(BEAT.bandN + 10, [0, 0], [1, 0.1])} blink={blinkK(f, 7)} />, 1)}
      {col(
        1280,
        "#FDE7C7",
        BEAT.bandM,
        <g transform={`translate(1600 760) scale(${interpolate(f, [BEAT.bandM + 8, BEAT.bandM + 20], [1, 1.06], clamp)}) translate(-1600 -760)`}>
          <Actor x={1315} y={180} s={0.95} f={f} who="meera" expr="smug" turn={0} gaze={glance(BEAT.bandM + 14, [-0.6, 0], [0, 0])} sway={Math.sin(f / 20) * 0.4} />
        </g>,
        2,
      )}
      <path d={`M640 0 L640 ${LH} M1280 0 L1280 ${LH}`} stroke="#0F172A" strokeWidth={14} />
    </Frame>
  );
};

/* 10. On the table, Meera's phone has been recording all along. "Nobody wrote it down. Except Srutam." */
const TableShot: React.FC = () => {
  const f = useT() + SCENES.table.from;
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.table), [1, 1.08], { ...clamp, easing: easeInOut })} focus={[960, 540]} drift={2} seed={8}>
        <rect width={LW} height={LH} fill="#8B6B52" />
        {Array.from({ length: 6 }, (_, i) => (
          <path key={i} d={`M-40 ${90 + i * 190} C600 ${70 + i * 190} 1300 ${120 + i * 190} 1960 ${100 + i * 190}`} stroke="#6B4F3A" strokeWidth={5} fill="none" opacity={0.35} />
        ))}
        <g transform="translate(470 160)">
          <path d="M0 0 L120 0 L106 140 L14 140 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={5} />
          <ellipse cx={60} cy={4} rx={56} ry={10} fill="#7C4A21" />
        </g>
        <rect x={1240} y={130} width={330} height={430} rx={10} fill="#FFFDF8" stroke={LINE} strokeWidth={4} transform="rotate(8 1405 345)" />
        {[0, 1, 2, 3, 4].map((i) => (
          <path key={i} d={`M1290 ${210 + i * 60} L${1500 - (i % 2) * 60} ${210 + i * 60}`} stroke="#94A3B8" strokeWidth={6} transform="rotate(8 1405 345)" />
        ))}
        <Phone x={765} y={100} w={390} rot={-4} glow="#EF4444">
          <DockOverApp w={390} recording label="Team chat" />
        </Phone>
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

/** A big copy of an Insights card, beside the phone, as it lands. */
const BigCard: React.FC<{ x: number; y: number; kind: string; color: string; text: string; meta?: string; k: number }> = ({ x, y, kind, color, text, meta, k }) => (
  <g transform={`translate(${x + (1 - k) * 120} ${y}) scale(${0.9 + 0.1 * k})`} opacity={Math.min(1, k * 1.4)}>
    <rect x={0} y={0} width={760} height={meta ? 196 : 160} rx={30} fill="#FFFFFF" />
    <rect x={36} y={30} width={kind.length * 20 + 48} height={46} rx={23} fill={color} opacity={0.16} />
    <text x={60} y={63} fontFamily="Inter" fontWeight={800} fontSize={26} letterSpacing={2} fill={color}>
      {kind}
    </text>
    <text x={40} y={128} fontFamily="Inter" fontWeight={700} fontSize={44} fill="#0F172A">
      {text}
    </text>
    {meta && (
      <text x={40} y={174} fontFamily="Inter" fontWeight={500} fontSize={30} fill="#64748B">
        {meta}
      </text>
    )}
  </g>
);

const ITEMS = [
  { kind: "DECISION", text: "Launch on the 14th", color: "#0D9488" },
  { kind: "NEXT STEP", text: "Arjun sends the pricing deck", meta: "By Thursday", color: "#2563EB" },
  { kind: "REMINDER", text: "Pricing deck", meta: "Thursday, 10:00 am", color: "#7C3AED" },
];

/* 11. Decision. Next step. Reminder. Before lunch. The phone, and each card large beside it. */
const Cards: React.FC = () => {
  const f = useT() + SCENES.cards.from;
  const appear = [BEAT.cardDecision, BEAT.cardStep, BEAT.cardReminder].map((at) => springAt(f, 0, [{ at, to: 1 }], 13, 160));
  const sticker = springAt(f, 0, [{ at: BEAT.lunch, to: 1 }], 12, 170);
  return (
    <Frame bg={["#0F172A", "#1E293B"]} night>
      <Camera f={f} zoom={interpolate(f, span(SCENES.cards), [1, 1.03], { ...clamp, easing: easeInOut })} focus={[960, 520]} drift={3} seed={9}>
        <Phone x={260} y={50} w={430} glow="#3B82F6">
          <InsightsScreen
            w={430}
            note="Tuesday sync"
            meta="Tue, 11:00 am · 32:14"
            appear={appear}
            items={ITEMS.map(({ kind, text, meta }) => ({ kind: kind as "DECISION", text, meta }))}
          />
        </Phone>
        {ITEMS.map((it, i) => (
          <BigCard key={it.kind} x={900} y={110 + i * 236} kind={it.kind} color={it.color} text={it.text} meta={it.meta} k={appear[i]} />
        ))}
        <g transform={`translate(1580 860) rotate(${8 - sticker * 4}) scale(${sticker})`} opacity={sticker > 0.02 ? 1 : 0}>
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

/* 12. One week later: the calendar page tears off and falls; the title beside it. */
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
      <g transform="translate(620 545) rotate(-3) scale(0.9)">
        <Page day="19" />
        <g transform={`translate(${fall[0]} ${fall[1]}) rotate(${rot} -330 -380)`}>
          <Page day="12" />
        </g>
      </g>
      <g opacity={title} transform={`translate(${(1 - title) * 30} 0)`}>
        <text x={1390} y={580} textAnchor="middle" fill="#FDE68A" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={112}>
          One week later
        </text>
      </g>
    </Frame>
  );
};

/* 13. Back to the outburst: Rao glares from one to the other; "Neha did!" "Arjun did!" */
const Blame: React.FC = () => {
  const f = useT() + SCENES.blame.from;
  const a = springAt(f, 0, [{ at: BEAT.pointA, to: 1 }], 11, 160);
  const n = springAt(f, 0, [{ at: BEAT.pointN, to: 1 }], 11, 160);
  const arjunHand = springPt(f, reach("r", 92, 0.9, BROAD), [{ at: BEAT.pointA, to: reach("r", -20, 0.6, BROAD) }], 11, 160);
  const nehaHand = springPt(f, reach("l", 88, 0.9), [{ at: BEAT.pointN, to: reach("l", 200, 0.6) }], 11, 160);
  const glare = springAt(f, -0.35, [{ at: BEAT.pointA + 2, to: -0.45 }, { at: BEAT.pointN + 2, to: 0.45 }], 14, 150);
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
      <Camera f={f} zoom={interpolate(f, span(SCENES.blame), [1, 1.04], { ...clamp, easing: easeInOut })} focus={[960, 520]} drift={3} seed={13}>
        <Depth blur={5}>
          <OfficeWallL board={false} />
        </Depth>
        <Depth blur={2}>
          <Actor x={780} y={90} s={0.6} f={f} who={RAO} shadow torso={600} expr="angry" turn={glare} gaze={[glare * 2.2, 0.4]} blink={blinkK(f, 2)} />
        </Depth>
        <TableL y={930} props={false} />
        <Actor
          x={150}
          y={200}
          s={0.86}
          f={f}
          who={ARJUN}
          shadow
          torso={240}
          expr="worried"
          viseme={vis("arjun-her", f)}
          turn={0.2 + 0.35 * a}
          tilt={3 * a}
          gaze={[0.3 + 0.6 * a, -0.1]}
          blink={blinkK(f, 4)}
          arms={a > 0.03 ? { r: { to: arjunHand, pose: a > 0.3 ? "point" : "fist", rot: 72 * a } } : {}}
        />
        <Actor
          x={1250}
          y={200}
          s={0.86}
          f={f}
          who={NEHA}
          shadow
          torso={240}
          expr="angry"
          viseme={vis("neha-him", f, "A")}
          turn={-0.2 - 0.35 * n}
          tilt={-3 * n}
          gaze={[-0.3 - 0.6 * n, -0.1]}
          blink={blinkK(f, 9)}
          arms={n > 0.03 ? { l: { to: nehaHand, pose: n > 0.3 ? "point" : "fist", rot: -55 * n } } : {}}
        />
        {drop(BEAT.pointA + 8, 470, 330)}
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

/* 14. Meera, calmly, to the room as she lifts her phone: "Relax. Let's ask Srutam." */
const Raise: React.FC = () => {
  const f = useT() + SCENES.raise.from;
  const raise = springAt(f, 0, [{ at: BEAT.lift, to: 1 }], 14, 110);
  const hand: Pt = [560, 420 + (1 - raise) * 440];
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.raise), [1, 1.04], clamp)} focus={[700, 520]} drift={3} seed={17}>
        <Depth blur={7}>
          <OfficeWallL board={false} />
          <Actor x={1180} y={140} s={0.62} f={f} who={RAO} torso={600} expr="angry" turn={-0.4} gaze={[-0.8, 0.2]} />
          <TableL y={930} props={false} />
        </Depth>
        <Actor
          x={330}
          y={100}
          s={1}
          f={f}
          who="meera"
          shadow
          torso={120}
          expr="smug"
          viseme={vis("m-ask", f)}
          turn={0.35}
          tilt={talkTilt(["m-ask"], f, 0, 1.4)}
          sway={follow((g) => talkTilt(["m-ask"], g, 0, 1.4), f, 4, 0.3)}
          gaze={[0.8, -0.2]}
          blink={blinkK(f, 1)}
          arms={{ r: { to: hand, pose: "phone", upright: true } }}
        />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

const Q = "When did we agree on the 14th?";
const A = "In Tuesday's sync: “No, no. The 14th. Final.”";
const TRANSCRIPT = "...So, do we launch on the 14th? ... Sir, the 21st would be safer... No, no. The 14th. Final. ... Arjun. Pricing deck, by Thursday...";

/** Behind Meera's phone: the room, waiting (out of focus). */
const Waiting: React.FC<{ f: number; blur?: number }> = ({ f, blur = 9 }) => (
  <Depth blur={blur}>
    <OfficeWallL board={false} />
    <Actor x={260} y={140} s={0.6} f={f} who={RAO} torso={600} expr="angry" turn={0.4} gaze={[0.8, 0.4]} />
    <Actor x={-40} y={420} s={0.62} f={f} who={ARJUN} expr="worried" turn={0.4} gaze={[0.8, 0]} />
    <Actor x={560} y={420} s={0.62} f={f} who={NEHA} expr="neutral" turn={0.3} gaze={[0.8, 0]} />
    <TableL y={900} props={false} />
  </Depth>
);

/* 15. Meera's phone, close, the room waiting behind: she types (jump cut mid-sentence),
   sends, Srutam thinks, then quotes the line and cites the note; she taps the note. */
const TypeShot: React.FC = () => {
  const f = useT() + SCENES.type.from;
  const PX = 1080;
  const PY = 50;
  const PW = 440;
  const ph = PW * (2400 / 1080);
  const cut = BEAT.typeStart + 22;
  const t = typed(Q, f < cut ? f : f + 200, BEAT.typeStart, 9);
  const sent = f >= BEAT.send;
  const kb = sent ? interpolate(f, [BEAT.send, BEAT.send + 8], [1, 0], clamp) : 1;
  const words = A.split(" ");
  const shown = Math.round(interpolate(f, [BEAT.answer, BEAT.answerEnd], [0, words.length], clamp));
  const th = thumbs(sent ? undefined : t.key, t.press, PW);
  const thumbR: Pt | null = f >= BEAT.send - 4 && f < BEAT.send + 4 ? [PW - 46, ph - 214] : th.r;
  const point: Pt | null = f >= BEAT.tapRef - 10 && f < BEAT.tapRef + 10 ? [PW * 0.32, ph * 0.66 + (f < BEAT.tapRef ? (BEAT.tapRef - f) * 9 : 0)] : null;
  const tapK = interpolate(f, [BEAT.tapRef, BEAT.tapRef + 12], [0, 1], clamp);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.type), [1, 1.02], clamp)} focus={[1300, 520]} drift={2} seed={18}>
        <Waiting f={f} />
        <HandsOnPhone x={PX} y={PY} w={PW} glow={f >= BEAT.send ? "#3B82F6" : undefined} thumbL={th.l} thumbR={thumbR} point={point}>
          <AskFlow
            w={PW}
            sent={sent ? Q : undefined}
            thinking={sent && f < BEAT.answer}
            answer={shown > 0 ? words.slice(0, shown).join(" ") : undefined}
            refs={f >= BEAT.answerEnd ? ["Tuesday sync"] : []}
            pressedRef={f >= BEAT.tapRef && f < BEAT.tapRef + 8}
            input={sent ? "" : t.text}
            caret={!sent && Math.floor(f / 8) % 2 === 0}
            keyboard={kb}
            keyDown={t.key}
            sendDown={f >= BEAT.send - 2 && f < BEAT.send + 3}
          />
        </HandsOnPhone>
        {tapK > 0 && tapK < 1 && <circle cx={PX + PW * 0.32} cy={PY + ph * 0.66} r={16 + tapK * 70} fill="none" stroke="#FFFFFF" strokeWidth={7} opacity={1 - tapK} />}
      </Camera>
      {f >= cut && f < cut + 4 && <rect width={LW} height={LH} fill="#FFFFFF" opacity={0.15} />}
    </Frame>
  );
};

/* 16. The note opens. She taps the waveform near the moment, then play. */
const DetailShot: React.FC = () => {
  const f = useT() + SCENES.detail.from;
  const PX = 1080;
  const PY = 50;
  const PW = 440;
  const k = PW / 470;
  const slide = interpolate(f, [SCENES.detail.from, SCENES.detail.from + 8], [1, 0], { ...clamp, easing: easeOut });
  const seeked = f >= BEAT.seek;
  const waveX = (24 + 20 + 70 + 16) * k;
  const waveW = PW - waveX - 120 * k;
  const waveY = (76 + 20 + 18 + 34 + 4 + 18 + 24 + 22 + 35) * k;
  const seekX = waveX + waveW * 0.4;
  const playX = (24 + 20 + 35) * k;
  const point: Pt | null = f < BEAT.seek - 10 ? null : f < BEAT.playBtn - 5 ? [seekX, waveY + 8] : f < BEAT.playBtn + 8 ? [playX, waveY + 8] : null;
  const tapA = interpolate(f, [BEAT.seek, BEAT.seek + 10], [0, 1], clamp);
  const tapB = interpolate(f, [BEAT.playBtn, BEAT.playBtn + 10], [0, 1], clamp);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} drift={2} seed={19} focus={[1300, 520]}>
        <Waiting f={f} />
        <HandsOnPhone x={PX} y={PY} w={PW} glow="#3B82F6" point={point}>
          <div style={{ position: "absolute", inset: 0, transform: `translateX(${slide * 100}%)` }}>
            <NoteDetail
              w={PW}
              title="Tuesday sync"
              meta="Tue, 11:00 am · 32:14"
              progress={seeked ? 0.4 : 0}
              playing={f >= BEAT.playBtn}
              time={seeked ? "12:52" : "0:00"}
              total="32:14"
              tap={tapA}
              transcript={TRANSCRIPT}
            />
          </div>
        </HandsOnPhone>
        {tapB > 0 && tapB < 1 && <circle cx={PX + playX} cy={PY + waveY} r={16 + tapB * 64} fill="none" stroke="#FFFFFF" strokeWidth={7} opacity={1 - tapB} />}
      </Camera>
    </Frame>
  );
};

/* 17. It plays, and the room hears Rao himself. Focus pulls from the phone to his face. */
const Play: React.FC = () => {
  const f = useT() + SCENES.play.from;
  const p0 = BEAT.play;
  const rack = interpolate(f, [p0 + 6, p0 + 24], [0, 1], { ...clamp, easing: easeInOut });
  const progress = interpolate(f, [p0, p0 + 85], [0.4, 0.402], clamp);
  const secs = 52 + Math.floor((f - p0) / 30);
  const sink = springAt(f, 1, [{ at: BEAT.realise, to: 0.95 }], 16, 80);
  const raoExpr = f < p0 + 10 ? "angry" : f < BEAT.realise ? "surprised" : "worried";
  const look = springAt(f, 0, [{ at: p0 + 20, to: 1 }], 14, 140);
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.play), [1.0, 1.06], { ...clamp, easing: easeInOut })} focus={[1000, 480]} drift={3} seed={21}>
        <Depth blur={3 + 2 * (1 - rack)}>
          <OfficeWallL board={false} />
        </Depth>
        <Depth blur={5 * (1 - rack)}>
          <Actor x={420} y={300} s={0.7} f={f} who={ARJUN} shadow torso={300} expr={look > 0.5 ? "smug" : "worried"} turn={0.5 * look} gaze={[0.9 * look, -0.2]} blink={blinkK(f, 4)} />
          <Actor x={1300} y={300} s={0.7} f={f} who={NEHA} shadow torso={300} expr={look > 0.5 ? "smug" : "neutral"} turn={-0.5 * look} gaze={[-0.9 * look, -0.2]} blink={blinkK(f, 9)} />
          <Actor x={830} y={170} s={0.8} f={f} who={RAO} shadow torso={320} expr={raoExpr} squash={sink} turn={f > BEAT.realise ? -0.15 : 0} tilt={f > BEAT.realise ? -2 : 0} gaze={f < BEAT.realise ? [-0.2, 0.9] : [-0.6, 0.5]} blink={f > BEAT.realise ? blinkK(f, 2) : 0} />
          {f > BEAT.realise + 6 && <Sweat x={1180} y={330 + ((f - BEAT.realise - 6) % 24) * 4} s={1} />}
          <TableL y={900} props={false} />
        </Depth>
        <Depth blur={4 * rack}>
          <Phone x={90} y={430} w={280} rot={-6} glow="#3B82F6">
            <NoteDetail w={280} title="Tuesday sync" meta="Tue, 11:00 am · 32:14" progress={progress} playing time={`12:${secs}`} total="32:14" transcript="...No, no. The 14th. Final. ..." />
          </Phone>
        </Depth>
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
    </Frame>
  );
};

/* 18. Rao owns it: "Okay... so the 14th is final, then." The room exhales and laughs. */
const Payoff: React.FC = () => {
  const f = useT() + SCENES.payoff.from;
  const rub = springPt(f, [130, 900], [{ at: BEAT.rub, to: [150, 300] }], 13, 120);
  const rubbing: Pt = [rub[0] + (f > BEAT.rub + 10 ? Math.sin(f / 3) * 12 : 0), rub[1]];
  const laugh = f >= BEAT.laugh;
  const bob = (seed: number) => (laugh ? Math.abs(Math.sin((f + seed) / 5)) * 8 * Math.min(1, (f - BEAT.laugh) / 10) : 0);
  const raoLook = talking(["rao-sheepish"], f) ? 0.35 * Math.sin((f - SCENES.payoff.from) / 18) : 0;
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} zoom={interpolate(f, [SCENES.payoff.from, BEAT.laugh - 4, SCENES.end.from], [1.06, 1.06, 1.0], { ...clamp, easing: easeInOut })} focus={[960, 500]} drift={3} seed={23}>
        <Depth blur={5}>
          <OfficeWallL board={false} />
        </Depth>
        <g transform={`translate(0 ${-bob(0)})`}>
          <Actor x={180} y={300} s={0.74} f={f} who={ARJUN} shadow torso={300} expr={laugh ? "laugh" : "smug"} turn={0.45} tilt={laugh ? -3 : 0} gaze={[0.8, -0.2]} blink={blinkK(f, 4)} />
        </g>
        <g transform={`translate(0 ${-bob(9)})`}>
          <Actor x={1300} y={300} s={0.74} f={f} who={NEHA} shadow torso={300} expr={laugh ? "laugh" : "smug"} turn={-0.45} tilt={laugh ? 3 : 0} gaze={[-0.8, -0.2]} blink={blinkK(f, 9)} />
        </g>
        <Actor
          x={700}
          y={150}
          s={0.86}
          f={f}
          who={RAO}
          shadow
          torso={320}
          expr={laugh ? "laugh" : "sheepish"}
          viseme={vis("rao-sheepish", f)}
          turn={raoLook}
          tilt={talkTilt(["rao-sheepish"], f, -4, 1.4)}
          gaze={laugh ? [0, 0] : [raoLook * 2, 0.3]}
          blink={blinkK(f, 6)}
          arms={{ l: { to: rubbing, pose: "palm", rot: 25 } }}
        />
        <TableL y={930} />
      </Camera>
      <Grade warm="#FDE68A" cool="#1E3A8A" from="left" amount={0.14} />
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

/* 19. Everyone laughs. "Every meeting, minuted." */
const End: React.FC = () => {
  const f = useT() + SCENES.end.from;
  const bob = (seed: number) => Math.abs(Math.sin((f + seed) / 5)) * 8;
  const band = interpolate(f, [BEAT.band, BEAT.band + 20], [0, 1], { ...clamp, easing: easeOut });
  return (
    <Frame bg={OFFICE}>
      <Camera f={f} drift={3} seed={19}>
        <g transform={`translate(0 ${-band * 150})`}>
          <Depth blur={5}>
            <OfficeWallL board={false} />
          </Depth>
          <g transform={`translate(0 ${-bob(0)})`}>
            <Actor x={260} y={300} s={0.72} f={f} who={ARJUN} shadow torso={300} expr="laugh" turn={0.3} tilt={-3} />
          </g>
          <g transform={`translate(0 ${-bob(9)})`}>
            <Actor x={1230} y={300} s={0.72} f={f} who={NEHA} shadow torso={300} expr="laugh" turn={-0.3} tilt={3} />
          </g>
          <g transform={`translate(0 ${-bob(4) * 0.6})`}>
            <Actor x={720} y={230} s={0.8} f={f} who={RAO} shadow torso={300} expr="laugh" tilt={-4} />
          </g>
          <TableL y={930} />
        </g>
      </Camera>
      <g transform={`translate(0 ${(1 - band) * 300})`}>
        <EndBandL tagline="Every meeting, minuted." />
      </g>
    </Frame>
  );
};

const LIST = [
  ["1 Cold open", SCENES.open, Open],
  ["2 One week earlier", SCENES.freeze, Freeze],
  ["3 Recording starts", SCENES.dock, DockStart],
  ["4 Board", SCENES.board, BoardShot],
  ["5 Arjun: the 21st", SCENES.arjun, ArjunShot],
  ["6 Rao: the 14th, final", SCENES.final, FinalShot],
  ["7 Pricing deck", SCENES.deck, DeckShot],
  ["8 Note it down", SCENES.note, NoteShot],
  ["9 Side-eye", SCENES.eyes, Eyes],
  ["10 Still recording", SCENES.table, TableShot],
  ["11 Cards", SCENES.cards, Cards],
  ["12 One week later", SCENES.week, Week],
  ["13 Blame", SCENES.blame, Blame],
  ["14 Let's ask", SCENES.raise, Raise],
  ["15 Type and ask", SCENES.type, TypeShot],
  ["16 Open the note", SCENES.detail, DetailShot],
  ["17 Playback", SCENES.play, Play],
  ["18 Payoff", SCENES.payoff, Payoff],
  ["19 End", SCENES.end, End],
] as const;

export const WhoLandscape: React.FC = () => (
  <Landscape>
    {LIST.map(([name, s, Comp]) => (
      <Sequence key={name} name={name} from={s.from * K} durationInFrames={s.duration * K}>
        <Comp />
      </Sequence>
    ))}
    <LCaptions cues={CAPTIONS} k={K} />
    <Soundtrack music="meeting/music-wide.wav" musicVolume={0.42} lines={VO} sfx={SFX} k={K} sfxGain={0.6} duck={0.35} />
  </Landscape>
);

