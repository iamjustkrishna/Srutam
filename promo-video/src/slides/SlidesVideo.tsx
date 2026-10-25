// "Who was doing slides 8-12?": 1:43 a.m., presentation at nine, and the group chat finds
// nobody did slides 8-12 ("not me bro"). Last Tuesday in the canteen Meera recorded the
// group meeting; Riya assigned the parts out loud ("And Kabir, you'll do eight to
// twelve") and wrote them in her planner. Meera asks Srutam AI, the answer quotes that
// line by its words and plays it, she drops it in the group, and Kabir is on it till
// 4 a.m. Rendered at 60 fps (see stories/time.ts).
import { useId } from "react";
import { AbsoluteFill, interpolate, Sequence } from "remotion";
import { clamp, easeInOut, easeOut } from "../anim";
import { Kulhad } from "../bedtime/Props";
import { KABIR, RIYA } from "../cast/cast";
import { Actor, blinkK, reach, type Pt } from "../cast/Rig";
import { Camera, Depth, Grade } from "../stories/Camera";
import { HandsOnPhone } from "../stories/Hands";
import { damped, springAt, springPt } from "../stories/physics";
import { Captions, Soundtrack } from "../stories/Story";
import { DrawnStep, K, useT } from "../stories/time";
import { thumbs, typed } from "../stories/typing";
import { voice } from "../stories/voice";
import { AskFlow, ChatScreen, EndBand, Frame, GripHand, H, Lamp, LINE, NoteDetail, Phone, W, Window } from "../storyboards/Kit";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";
import { VISEMES } from "./visemes";

const { vis, talkTilt, emph } = voice(VO, VISEMES);
const span = (sc: { from: number; duration: number }): [number, number] => [sc.from, sc.from + sc.duration];
const NIGHT: [string, string] = ["#0B1033", "#1E1B4B"];

/** A dark time chip at the top of the frame. */
const TimeChip: React.FC<{ f: number; text: string; keys: [number, number, number, number] }> = ({ f, text, keys }) => (
  <g opacity={interpolate(f, keys, [0, 1, 1, 0], clamp)}>
    <rect x={W / 2 - 250} y={70} width={500} height={92} rx={24} fill="#0F172A" opacity={0.82} />
    <text x={W / 2} y={132} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={46} fill="#FFFFFF">
      {text}
    </text>
  </g>
);

/** Meera's hostel room at night: a window, a bunk, sticky notes, clothes on the chair. */
const HostelRoom: React.FC<{ f: number }> = ({ f }) => (
  <g>
    <rect width={W} height={H} fill="#2A2560" />
    <rect y={1480} width={W} height={H - 1480} fill="#1F1B47" />
    <Window x={90} y={200} w={380} h={480} sky={["#0B1033", "#1E1B4B"]} moon />
    {[0, 1, 2, 3, 4, 5].map((i) => (
      <rect key={i} x={600 + (i % 3) * 140} y={240 + Math.floor(i / 3) * 170} width={100} height={124} rx={6} fill={["#F472B6", "#60A5FA", "#FBBF24", "#34D399", "#A78BFA", "#FB923C"][i]} opacity={0.42} transform={`rotate(${i * 5 - 9} ${650 + (i % 3) * 140} 300)`} />
    ))}
    {/* the top bunk's ladder and rail */}
    <rect x={0} y={760} width={W} height={26} fill="#4C3B8F" />
    {[120, 260].map((x) => (
      <rect key={x} x={x} y={786} width={14} height={700} fill="#4C3B8F" />
    ))}
    {/* the extension board with too many chargers */}
    <g transform="translate(820 1440)">
      <rect x={-90} y={-24} width={180} height={48} rx={8} fill="#E5E7EB" />
      {[-55, -18, 19, 56].map((x, i) => (
        <path key={i} d={`M${x} -24 C${x} -90 ${x + 40} -110 ${x + 60} -170`} stroke={["#111827", "#F8FAFC", "#111827", "#3B82F6"][i]} strokeWidth={6} fill="none" />
      ))}
    </g>
    {[0, 1, 2, 3].map((i) => (
      <circle key={i} cx={160 + i * 70} cy={260 + (i % 2) * 70} r={3} fill="#FEF3C7" opacity={0.5 + 0.5 * Math.sin(f / 9 + i * 2)} />
    ))}
  </g>
);

/** Two hands holding a phone up in front of us. */
const HeldPhone: React.FC<{ f: number; y: number; w?: number; children: React.ReactNode }> = ({ f, y, w = 680, children }) => {
  const x = 540 - w / 2;
  const h = w * (2400 / 1080);
  return (
    <g transform={`translate(0 ${Math.sin(f / 22) * 4})`}>
      <Phone x={x} y={y} w={w} rot={-1.5}>
        <div style={{ position: "absolute", left: 0, top: 0, width: w, height: h - 330 }}>{children}</div>
      </Phone>
      <GripHand x={x + 20} y={y + h - 120} s={1.5} rot={-12} />
      <GripHand x={x + w - 20} y={y + h - 120} s={1.5} rot={12} />
    </g>
  );
};

const GROUP = { title: "DBMS project 📊", members: "Riya, Kabir, you" };
const CHAT = [
  { from: "Riya", text: "WHO was doing slides 8–12??", color: "#DB2777" },
  { from: "Riya", text: "presentation is at 9 😭", color: "#DB2777" },
  { from: "Me", text: "not me", me: true },
  { from: "Riya", text: "not me", color: "#DB2777" },
  { from: "Kabir", text: "not me bro 💀", color: "#16A34A" },
];

/* 1. 1:43 a.m.: the group chat lights up. Nobody did slides 8-12. Kabir: "not me bro". */
const ChatShot: React.FC = () => {
  const f = useT() + SCENES.chat.from;
  const appear = BEAT.msg.map((at) => springAt(f, 0, [{ at, to: 1 }], 14, 170));
  const lift = springAt(f, 0, [{ at: 0, to: 1 }], 14, 110);
  if (f >= BEAT.msg[4] + 16) {
    // ECU: her eyes narrow at "not me bro".
    const push = interpolate(f, [BEAT.msg[4] + 16, SCENES.chat.duration], [2.6, 3.0], clamp);
    return (
      <Frame bg={NIGHT} night boil={2.4} paper={0.14}>
        <Camera f={f} zoom={push} focus={[470, 900]} center={[540, 960]} drift={2} seed={31}>
          <HostelRoom f={f} />
          <Actor x={40} y={380} s={1.45} f={f} who="meera" expr="smug" gaze={[0.7, 0.35]} tilt={-4} blink={0.45} />
          <ellipse cx={760} cy={1100} rx={420} ry={460} fill="#BFDBFE" opacity={0.2} style={{ mixBlendMode: "screen" }} />
        </Camera>
        <Grade warm="#93C5FD" cool="#0B1033" from="right" amount={0.2} />
      </Frame>
    );
  }
  return (
    <Frame bg={NIGHT} night boil={1.2} paper={0.12}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.chat), [1, 1.06], clamp)} focus={[540, 1000]} drift={3} seed={3}>
        <Depth blur={9}>
          <HostelRoom f={f} />
          <Lamp x={760} y={880} />
        </Depth>
        <HeldPhone f={f} y={190 + (1 - lift) * 500}>
          <ChatScreen w={680} title={GROUP.title} members={GROUP.members} messages={CHAT} appear={appear} typing={f > BEAT.kabirTyping && f < BEAT.msg[4] ? "Kabir" : f > 60 && f < BEAT.msg[2] ? "Riya" : undefined} />
        </HeldPhone>
      </Camera>
      <TimeChip f={f} text="1:43 a.m. · Presentation at 9" keys={[4, 14, 180, 195]} />
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
    </Frame>
  );
};

/** Flashback look: a warm wash and a soft vignette. */
const Flashback: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <g>
    {children}
    <rect width={W} height={H} fill="#B45309" opacity={0.14} style={{ mixBlendMode: "multiply" }} />
  </g>
);

/** The college canteen: green tiles, a price board, a ceiling fan, a tube light. */
const Canteen: React.FC<{ f: number }> = ({ f }) => (
  <g>
    <rect width={W} height={H} fill="#F3E7C9" />
    <rect y={760} width={W} height={H - 760} fill="#CFE3D2" />
    {Array.from({ length: 12 }, (_, i) => (
      <path key={i} d={`M${i * 96} 760 V${H}`} stroke="#B7CFBB" strokeWidth={3} />
    ))}
    {Array.from({ length: 10 }, (_, i) => (
      <path key={i} d={`M0 ${760 + i * 96} H${W}`} stroke="#B7CFBB" strokeWidth={3} />
    ))}
    {/* the price board */}
    <g transform="translate(560 220)">
      <rect x={-300} y={-60} width={600} height={400} rx={14} fill="#1F3B2D" stroke="#7C5A3A" strokeWidth={12} />
      <text x={0} y={10} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={54} fill="#FDE68A">
        CANTEEN
      </text>
      <text fontFamily="Kalam" fontWeight={700} fontSize={42} fill="#F1F5F9">
        <tspan x={-240} y={90}>Maggi .......... ₹40</tspan>
        <tspan x={-240} y={150}>Chai ............ ₹10</tspan>
        <tspan x={-240} y={210}>Samosa ........ ₹15</tspan>
        <tspan x={-240} y={270}>Cold coffee .. ₹50</tspan>
      </text>
    </g>
    {/* tube light and a ceiling fan */}
    <rect x={100} y={60} width={300} height={22} rx={10} fill="#FFFFFF" />
    <ellipse cx={250} cy={92} rx={200} ry={30} fill="#FFFFFF" opacity={0.25} />
    <g transform={`translate(900 70) rotate(${(f * 9) % 360})`}>
      {[0, 120, 240].map((a) => (
        <rect key={a} x={-12} y={0} width={24} height={170} rx={10} fill="#8B7355" transform={`rotate(${a})`} />
      ))}
      <circle r={22} fill="#6B5843" />
    </g>
    {/* other students, far away */}
    {[[80, 640], [960, 660]].map(([x, y], i) => (
      <g key={i} opacity={0.5}>
        <circle cx={x} cy={y} r={50} fill="#7C6A55" />
        <rect x={x - 70} y={y + 40} width={140} height={160} rx={50} fill={i ? "#60A5FA" : "#F59E0B"} />
      </g>
    ))}
  </g>
);

/** A steel bowl of Maggi with a fork. */
const Maggi: React.FC<{ x: number; y: number; f: number }> = ({ x, y, f }) => (
  <g transform={`translate(${x} ${y})`}>
    <path d="M-110 -20 Q0 70 110 -20 Z" fill="#CBD5E1" stroke={LINE} strokeWidth={5} />
    <ellipse cx={0} cy={-20} rx={110} ry={26} fill="#E2E8F0" stroke={LINE} strokeWidth={5} />
    {Array.from({ length: 7 }, (_, i) => (
      <path key={i} d={`M${-80 + i * 24} -24 q10 -14 20 0 t20 0`} stroke="#F59E0B" strokeWidth={6} fill="none" />
    ))}
    {[0, 1].map((i) => (
      <path key={i} d={`M${-30 + i * 40} -60 q14 -30 0 -60`} stroke="#FFFFFF" strokeWidth={5} fill="none" opacity={0.4 + 0.3 * Math.sin(f / 8 + i)} />
    ))}
  </g>
);

/** Riya's planner on the table: the parts get written in as she says them. */
const Planner: React.FC<{ f: number }> = ({ f }) => {
  const row = (at: number, text: string, y: number, hot = false) => {
    const k = interpolate(f, [at, at + 12], [0, 1], clamp);
    if (k <= 0) return null;
    return (
      <text x={-210} y={y} fontFamily="Kalam" fontWeight={700} fontSize={hot ? 48 : 42} fill={hot ? "#B91C1C" : "#1E3A8A"}>
        {text.slice(0, Math.round(text.length * k))}
      </text>
    );
  };
  const under = interpolate(f, [BEAT.eightTwelve + 14, BEAT.eightTwelve + 26], [0, 1], clamp);
  return (
    <g transform="translate(300 1395) rotate(-6) scale(0.82)">
      <rect x={-250} y={-150} width={500} height={340} rx={10} fill="#FFFDF8" stroke={LINE} strokeWidth={5} />
      <path d="M-250 -150 V190" stroke="#EF4444" strokeWidth={4} opacity={0.6} />
      <text x={-210} y={-95} fontFamily="Kalam" fontWeight={700} fontSize={36} fill="#64748B">
        DBMS ppt
      </text>
      {row(BEAT.intro, "Intro – Meera", -30)}
      {row(BEAT.oneToSeven, "1–7 – Riya", 40)}
      {row(BEAT.eightTwelve, "8–12 – KABIR", 115, true)}
      {under > 0 && <path d={`M-210 132 h${310 * under}`} stroke="#B91C1C" strokeWidth={5} />}
      {f > BEAT.eightTwelve + 22 && (
        <ellipse cx={-50} cy={100} rx={200} ry={56} fill="none" stroke="#DC2626" strokeWidth={7} strokeDasharray={1400} strokeDashoffset={1400 * (1 - interpolate(f, [BEAT.eightTwelve + 22, BEAT.eightTwelve + 32], [0, 1], clamp))} transform="rotate(-4 -50 100)" />
      )}
    </g>
  );
};

/** The flashback's cuts: a wide, then close-ups on the line that matters. */
const flashShot = (f: number): { zoom: number; focus: Pt; center: Pt } => {
  if (f < BEAT.kabirName) return { zoom: interpolate(f, [SCENES.flash.from, BEAT.kabirName], [1.02, 1.1], { ...clamp, easing: easeInOut }), focus: [540, 1100], center: [540, 1100] };
  if (f < BEAT.eightTwelve) return { zoom: 1.9, focus: [300, 980], center: [540, 960] }; // CU Riya, pen up: "And Kabir..."
  if (f < 470) return { zoom: interpolate(f, [BEAT.eightTwelve, 470], [2.5, 2.8], clamp), focus: [330, 1450], center: [540, 960] }; // ECU planner
  return { zoom: interpolate(f, [470, SCENES.ask.from], [1.9, 2.1], clamp), focus: [880, 990], center: [540, 900] }; // CU Kabir
};

/* 2. Last Tuesday, the canteen: Meera taps record ("Nobody remembers anything anyway"),
   Riya assigns the parts out loud and writes them down, Kabir eats Maggi: "Done. Easy." */
const FlashShot: React.FC = () => {
  const f = useT() + SCENES.flash.from;
  const tableY = 1300;
  // Meera reaches to the phone in the middle of the table and taps record.
  const meeraArm = springPt(f, [300, 900], [{ at: BEAT.recTap - 10, to: [380, 1020] }, { at: BEAT.recTap + 10, to: [320, 900] }], 14, 160);
  const rec = f >= BEAT.recTap;
  const dot = 0.5 + 0.5 * Math.sin(f / 6);
  // Riya points her pen at Kabir on his name.
  const riyaArm = springPt(f, reach("r", 80, 0.8), [{ at: BEAT.kabirName - 4, to: reach("r", 20, 0.98) }, { at: BEAT.easy + 10, to: reach("r", 80, 0.8) }], 14, 150);
  const kabirLook = springAt(f, 0, [{ at: BEAT.kabirLooks, to: 1 }], 14, 150);
  const thumb = springPt(f, [440, 980], [{ at: BEAT.easy - 2, to: [470, 560] }], 13, 170);
  const chip = interpolate(f, [SCENES.flash.from + 4, SCENES.flash.from + 14, SCENES.flash.from + 70, SCENES.flash.from + 84], [0, 1, 1, 0], clamp);
  return (
    <Frame bg={["#F3E7C9", "#E8D7B0"]} boil={2.4} paper={0.16}>
      <Flashback>
        <Camera f={f} {...flashShot(f)} drift={3} seed={6}>
          <Depth blur={4}>
            <Canteen f={f} />
          </Depth>
          {/* Riya, left, with her planner */}
          <Actor
            x={-90}
            y={640}
            s={0.72}
            f={f}
            who={RIYA}
            shadow
            expr="talk"
            viseme={vis("s-assign", f)}
            tilt={talkTilt(["s-assign"], f, 2, 1.4)}
            turn={f > BEAT.kabirName - 6 ? 0.4 : 0.15}
            gaze={f > BEAT.kabirName - 6 && f < BEAT.easy + 20 ? [0.9, 0] : [0.3, 0.6]}
            blink={blinkK(f, 3)}
            arms={{ r: { to: [riyaArm[0], riyaArm[1] - emph(["s-assign"], f) * 20], pose: f > BEAT.kabirName - 6 && f < BEAT.easy + 10 ? "point" : "grip", rot: 10 } }}
          />
          {/* Kabir, right: Maggi, headphones, half listening */}
          <Actor
            x={640}
            y={660}
            s={0.72}
            f={f}
            who={KABIR}
            shadow
            expr={f < BEAT.kabirLooks ? "neutral" : f < BEAT.easy + 20 ? "smile" : "laugh"}
            viseme={vis("s-easy", f, f < BEAT.kabirLooks && Math.floor(f / 7) % 3 === 0 ? "B" : "X")}
            tilt={-6 + kabirLook * 6 + talkTilt(["s-easy"], f, 0, 1.2)}
            turn={-0.2 - kabirLook * 0.25}
            gaze={f < BEAT.kabirLooks ? [-0.2, 0.9] : [-0.9, 0]}
            blink={blinkK(f, 5)}
            arms={f > BEAT.easy - 8 ? { l: { to: thumb, pose: "thumb", upright: true } } : { l: { to: [380, 980], pose: "grip", rot: -40 } }}
          />
          {/* Meera, centre back */}
          <Actor
            x={300}
            y={520}
            s={0.62}
            f={f}
            who="meera"
            shadow
            expr={f < BEAT.kabirName ? "smug" : "laugh"}
            viseme={vis("s-record", f)}
            tilt={talkTilt(["s-record"], f, 0, 1.2)}
            gaze={f > BEAT.kabirName ? [0.8, 0.1] : [0, 0.6]}
            blink={blinkK(f, 1)}
            arms={f < BEAT.recTap + 30 ? { r: { to: meeraArm, pose: "point" } } : {}}
          />
          {/* the table */}
          <rect x={-20} y={tableY} width={W + 40} height={H - tableY + 20} fill="#9A7B5B" />
          <rect x={-20} y={tableY} width={W + 40} height={34} fill="#B8956F" stroke={LINE} strokeWidth={5} />
          <Maggi x={880} y={tableY + 90} f={f} />
          <Kulhad x={700} y={tableY + 70} s={0.75} t={f} />
          {/* the phone, lying on the table, recording */}
          <g transform={`translate(560 ${tableY + 70})`}>
            <path d="M-80 -20 L80 -20 L96 20 L-96 20 Z" fill="#0B0F1A" stroke="#3A4154" strokeWidth={3} />
            <path d="M-72 -15 L72 -15 L84 15 L-84 15 Z" fill={rec ? "#1E2A5A" : "#111827"} />
            {rec && <circle cx={-46} cy={0} r={8} fill="#EF4444" opacity={0.4 + 0.6 * dot} />}
            {rec && <circle cx={-46} cy={0} r={20} fill="#EF4444" opacity={0.2 * dot} />}
          </g>
          <Planner f={f} />
        </Camera>
      </Flashback>
      <g opacity={chip}>
        <rect x={W / 2 - 260} y={70} width={520} height={92} rx={24} fill="#78350F" opacity={0.85} />
        <text x={W / 2} y={132} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={44} fill="#FEF3C7">
          Last Tuesday · Canteen
        </text>
      </g>
      <Grade warm="#FDE68A" cool="#78350F" from="top" amount={0.16} />
    </Frame>
  );
};

const Q = "Who's doing slides 8 to 12?";
const A = "Kabir. In Tuesday's group meeting: “And Kabir, you'll do eight to twelve.” … “Done. Easy.”";

/* 3. Back at 1:43 a.m.: she asks Srutam AI; the answer quotes the line and cites the meeting. */
const AskShot: React.FC = () => {
  const f = useT() + SCENES.ask.from;
  const PW = 680;
  const ph = PW * (2400 / 1080);
  const cut = BEAT.typeStart + 18;
  const t = typed(Q, f < cut ? f : f + 160, BEAT.typeStart, 9);
  const sent = f >= BEAT.send;
  const kb = sent ? interpolate(f, [BEAT.send, BEAT.send + 8], [1, 0], clamp) : 1;
  const words = A.split(" ");
  const shown = Math.round(interpolate(f, [BEAT.answer, BEAT.answerEnd], [0, words.length], clamp));
  const th = thumbs(sent ? undefined : t.key, t.press, PW);
  const thumbR: Pt | null = f >= BEAT.send - 4 && f < BEAT.send + 4 ? [PW - 70, ph - 330] : th.r;
  const point: Pt | null = f >= BEAT.tapRef - 10 && f < BEAT.tapRef + 10 ? [PW * 0.3, ph * 0.7 + (f < BEAT.tapRef ? (BEAT.tapRef - f) * 14 : 0)] : null;
  return (
    <Frame bg={NIGHT} night boil={1.2} paper={0.12}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.ask), [1, 1.04], clamp)} focus={[540, 900]} drift={2} seed={14}>
        <Depth blur={9}>
          <HostelRoom f={f} />
        </Depth>
        <HandsOnPhone x={200} y={170} w={PW} glow={sent ? "#3B82F6" : undefined} thumbL={th.l} thumbR={thumbR} point={point}>
          <AskFlow
            w={PW}
            sent={sent ? Q : undefined}
            thinking={sent && f < BEAT.answer}
            answer={shown > 0 ? words.slice(0, shown).join(" ") : undefined}
            refs={f >= BEAT.answerEnd ? ["Group meeting · Tue"] : []}
            pressedRef={f >= BEAT.tapRef && f < BEAT.tapRef + 8}
            input={sent ? "" : t.text}
            caret={!sent && Math.floor(f / 8) % 2 === 0}
            keyboard={kb}
            keyDown={t.key}
            sendDown={f >= BEAT.send - 2 && f < BEAT.send + 3}
          />
        </HandsOnPhone>
      </Camera>
      {f >= cut && f < cut + 4 && <rect width={W} height={H} fill="#FFFFFF" opacity={0.12} />}
      <TimeChip f={f} text="1:46 a.m." keys={[SCENES.ask.from, SCENES.ask.from + 8, SCENES.ask.from + 120, SCENES.ask.from + 130]} />
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
    </Frame>
  );
};

/* 4. The meeting opens at the moment and plays: "And Kabir, you'll do eight to twelve." "Done. Easy." */
const PlayShot: React.FC = () => {
  const f = useT() + SCENES.play.from;
  const PW = 680;
  const slide = interpolate(f, [SCENES.play.from, SCENES.play.from + 8], [1, 0], { ...clamp, easing: easeOut });
  const progress = 0.46 + interpolate(f, [BEAT.play, BEAT.play + 127], [0, 0.004], clamp);
  if (f >= 742) {
    // CU Meera as Kabir's own "Done. Easy." comes out of the speaker.
    const PH: Pt = [40 + 545 * 1.45 + 40, 380 + 430 * 1.45 - 120];
    return (
      <Frame bg={NIGHT} night boil={2.4} paper={0.14}>
        <Camera f={f} zoom={interpolate(f, [742, SCENES.share.from], [1.5, 1.7], clamp)} focus={[520, 960]} center={[540, 900]} drift={2} seed={33}>
          <HostelRoom f={f} />
          <Actor
            x={40}
            y={380}
            s={1.45}
            f={f}
            who="meera"
            shadow
            screen={<rect x={-38} y={-84} width={76} height={164} rx={9} fill="#0C1225" />}
            expr={f < BEAT.playEasy ? "smug" : "laugh"}
            tilt={f < BEAT.playEasy ? 3 : 6 + Math.sin(f / 5) * 2}
            gaze={[0.7, 0.2]}
            blink={blinkK(f, 1)}
            arms={{ r: { to: [545, 430], pose: "phone", upright: true } }}
          />
          {f < BEAT.play + 127 &&
            [0, 1, 2].map((i) => {
              const k = ((f - BEAT.play + i * 12) % 36) / 36;
              return <path key={i} d={`M${PH[0] + 60 + k * 110} ${PH[1] - 80 - k * 50} q${36 + k * 36} ${80 + k * 40} 0 ${160 + k * 100}`} stroke="#93C5FD" strokeWidth={8} fill="none" opacity={(1 - k) * 0.55} strokeLinecap="round" />;
            })}
          <ellipse cx={PH[0] - 120} cy={PH[1] - 60} rx={420} ry={460} fill="#BFDBFE" opacity={0.18} style={{ mixBlendMode: "screen" }} />
        </Camera>
        <Grade warm="#93C5FD" cool="#1E1B4B" from="right" amount={0.16} />
      </Frame>
    );
  }
  return (
    <Frame bg={NIGHT} night boil={1.2} paper={0.12}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.play), [1, 1.05], clamp)} focus={[540, 800]} drift={2} seed={15}>
        <Depth blur={9}>
          <HostelRoom f={f} />
        </Depth>
        <HandsOnPhone x={200} y={330} w={PW} glow="#3B82F6">
          <div style={{ position: "absolute", inset: 0, transform: `translateX(${slide * 100}%)` }}>
            <NoteDetail
              w={PW}
              title="Group meeting"
              meta="Tue, 12:30 pm · 26:52"
              progress={progress}
              playing={f >= BEAT.play}
              time={f >= BEAT.playEasy ? "12:44" : "12:41"}
              total="26:52"
              tap={interpolate(f, [SCENES.play.from + 2, SCENES.play.from + 12], [0, 1], clamp)}
              transcript="…Okay. Meera, intro. I'll do one to seven. And Kabir, you'll do eight to twelve. … Done. Easy. …"
            />
          </div>
        </HandsOnPhone>
        {f >= BEAT.play &&
          f < BEAT.play + 127 &&
          [0, 1, 2].map((i) => {
            const k = ((f - BEAT.play + i * 12) % 36) / 36;
            return <path key={i} d={`M${880 + k * 90} ${720 - k * 40} q${30 + k * 30} ${70 + k * 30} 0 ${140 + k * 60}`} stroke="#93C5FD" strokeWidth={7} fill="none" opacity={(1 - k) * 0.5} strokeLinecap="round" />;
          })}
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
    </Frame>
  );
};

/* 5. She drops it in the group. Riya: 💀💀💀. Kabir is typing… "on it 😭". */
const ShareShot: React.FC = () => {
  const f = useT() + SCENES.share.from;
  const msgs = [
    ...CHAT.slice(2),
    { from: "Me", text: "Srutam: “And Kabir, you'll do eight to twelve.” … “Done. Easy.” 🎧 Group meeting · Tue 12:41", me: true },
    { from: "Riya", text: "💀💀💀", color: "#DB2777" },
    { from: "Kabir", text: "on it 😭", color: "#16A34A" },
  ];
  const appear = [1, 1, 1, ...[BEAT.mine, ...BEAT.replies].map((at) => springAt(f, 0, [{ at, to: 1 }], 14, 170))];
  return (
    <Frame bg={NIGHT} night boil={1.2} paper={0.12}>
      <Camera f={f} zoom={interpolate(f, span(SCENES.share), [1, 1.05], clamp)} focus={[540, 1000]} drift={3} seed={18}>
        <Depth blur={9}>
          <HostelRoom f={f} />
        </Depth>
        <HeldPhone f={f} y={190}>
          <ChatScreen w={680} title={GROUP.title} members={GROUP.members} messages={msgs} appear={appear} typing={f > BEAT.kabirTyping2 && f < BEAT.replies[1] ? "Kabir" : undefined} />
        </HeldPhone>
      </Camera>
      <Grade warm="#93C5FD" cool="#0B1033" from="top" amount={0.14} />
    </Frame>
  );
};

/** A cutting-chai glass, empty but for a ring at the bottom. */
const ChaiGlass: React.FC<{ x: number; y: number }> = ({ x, y }) => (
  <g transform={`translate(${x} ${y})`}>
    <path d="M-34 -90 L34 -90 L26 0 L-26 0 Z" fill="#E0F2FE" opacity={0.7} stroke={LINE} strokeWidth={4} />
    <path d="M-28 -14 L28 -14 L26 0 L-26 0 Z" fill="#B45309" opacity={0.7} />
  </g>
);

/* 6. 4:10 a.m., Kabir's room: laptop glow, three empty chai glasses. "Who records a canteen?!" Band. */
const KabirShot: React.FC = () => {
  const f = useT() + SCENES.kabir.from;
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const yell = f >= BEAT.canteen - 4;
  const band = interpolate(f, [BEAT.band, BEAT.band + 20], [0, 1], { ...clamp, easing: easeOut });
  const deskY = 1340;
  const typingBob = !yell ? Math.sin(f * 1.3) * 3 : 0;
  return (
    <Frame bg={["#111827", "#0B1033"]} night boil={2.4} paper={0.16}>
      <Camera f={f} {...(yell && f < 1042 ? { zoom: 2.25, focus: [535, 760] as Pt, center: [540, 820] as Pt } : { zoom: interpolate(f, span(SCENES.kabir), [1, 1.06], clamp), focus: [540, 1000] as Pt })} drift={3} seed={20} shake={yell && f < BEAT.canteen + 10 ? [damped(f, BEAT.canteen, 6, 5, 6), 0] : [0, 0]}>
        <Depth blur={4}>
          <rect width={W} height={H} fill="#1F2937" />
          {/* a band poster and the timetable */}
          <rect x={90} y={220} width={300} height={420} fill="#7C3AED" opacity={0.55} />
          <text x={240} y={460} textAnchor="middle" fontFamily="Playfair Display" fontWeight={700} fontSize={64} fill="#FDE68A" opacity={0.8}>
            LIVE
          </text>
          <rect x={640} y={260} width={340} height={260} fill="#F8FAFC" opacity={0.75} />
          {[0, 1, 2, 3].map((i) => (
            <path key={i} d={`M660 ${310 + i * 50} H960`} stroke="#94A3B8" strokeWidth={4} />
          ))}
          {/* the clock */}
          <g transform="translate(215 760)">
            <rect x={-150} y={-62} width={300} height={92} rx={18} fill="#0F172A" stroke={LINE} strokeWidth={5} />
            <text x={0} y={8} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={62} fill="#EF4444">
              4:10
            </text>
          </g>
        </Depth>
        <defs>
          <radialGradient id={`lap${id}`}>
            <stop offset="0%" stopColor="#BFDBFE" stopOpacity={0.45} />
            <stop offset="100%" stopColor="#BFDBFE" stopOpacity={0} />
          </radialGradient>
        </defs>
        <g transform={`translate(0 ${typingBob})`}>
          <Actor
            x={190}
            y={330}
            s={1.15}
            f={f}
            who={KABIR}
            shadow
            expr={yell ? "angry" : "sleepy"}
            viseme={vis("s-canteen", f)}
            tilt={yell ? talkTilt(["s-canteen"], f, 3, 3) : -4}
            gaze={yell ? [0, 0] : [0, 0.9]}
            blink={yell ? 0 : blinkK(f, 5)}
            squash={yell ? 1 + 0.02 * Math.sin(f) : 1}
          />
        </g>
        <ellipse cx={540} cy={1120} rx={460} ry={360} fill={`url(#lap${id})`} style={{ mixBlendMode: "screen" }} />
        <rect x={-20} y={deskY} width={W + 40} height={H - deskY + 20} fill="#3F2A1D" />
        <rect x={-20} y={deskY} width={W + 40} height={36} fill="#5B3A22" stroke={LINE} strokeWidth={5} />
        {/* the back of his laptop, stickered */}
        <g transform={`translate(540 ${deskY + 10})`}>
          <path d="M-300 0 L300 0 L276 -300 L-276 -300 Z" fill="#9CA3AF" stroke={LINE} strokeWidth={6} />
          <circle cx={-150} cy={-200} r={46} fill="#22C55E" />
          <text x={-150} y={-186} textAnchor="middle" fontFamily="Inter" fontWeight={900} fontSize={36} fill="#FFFFFF">
            {"</>"}
          </text>
          <rect x={60} y={-250} width={170} height={70} rx={14} fill="#F59E0B" transform="rotate(8 145 -215)" />
          <text x={145} y={-203} textAnchor="middle" fontFamily="Inter" fontWeight={900} fontSize={34} fill="#111827" transform="rotate(8 145 -215)">
            404
          </text>
          <text x={-40} y={-80} textAnchor="middle" fontFamily="Inter" fontWeight={900} fontSize={30} fill="#E5E7EB" transform="rotate(-10 -40 -80)">
            DEADLINES? 😎
          </text>
        </g>
        {/* the night so far: a tower of empty chai glasses */}
        {Array.from({ length: 11 }, (_, i) => {
          const row = [0, 0, 0, 0, 1, 1, 1, 2, 2, 3, 4][i];
          const col = [0, 1, 2, 3, 0, 1, 2, 0, 1, 0, 0][i];
          const n = [4, 3, 2, 1, 1][row];
          return <ChaiGlass key={i} x={150 + (col - (n - 1) / 2) * 62 + Math.sin(i * 3.1) * 4} y={deskY + 40 - row * 92} />;
        })}
        <ChaiGlass x={900} y={deskY + 100} />
      </Camera>
      {yell && f < 1042 && (
        <g transform={`translate(${540 + Math.sin(f * 2.3) * 8} ${1460 + Math.cos(f * 1.7) * 6}) rotate(-6) scale(${interpolate(f, [BEAT.canteen - 4, BEAT.canteen + 4], [0.6, 1], { ...clamp, easing: easeOut })})`}>
          {["WHO RECORDS", "A CANTEEN?!"].map((l, i) => (
            <text key={l} x={0} y={i * 150} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={150} fill="#FFFDF8" stroke="#1F1512" strokeWidth={14} paintOrder="stroke" letterSpacing={2}>
              {l}
            </text>
          ))}
        </g>
      )}
      <g transform={`translate(0 ${(1 - band) * 420})`}>
        <EndBand tagline="Listen once. Ask anytime." />
      </g>
    </Frame>
  );
};

const LIST = [
  ["1 1:43 a.m. chat", SCENES.chat, ChatShot],
  ["2 Canteen flashback", SCENES.flash, FlashShot],
  ["3 Ask Srutam", SCENES.ask, AskShot],
  ["4 Playback", SCENES.play, PlayShot],
  ["5 Share", SCENES.share, ShareShot],
  ["6 Kabir, 4:10 a.m.", SCENES.kabir, KabirShot],
] as const;

export const SlidesVideo: React.FC = () => {
  const f = useT();
  const sepia = interpolate(f, [SCENES.flash.from, SCENES.flash.from + 12, SCENES.ask.from - 10, SCENES.ask.from], [0, 0.45, 0.45, 0], clamp);
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <AbsoluteFill style={{ filter: sepia > 0 ? `sepia(${sepia})` : undefined }}>
        <DrawnStep.Provider value={2}>
          {LIST.map(([name, s, Comp]) => (
            <Sequence key={name} name={name} from={s.from * K} durationInFrames={s.duration * K}>
              <Comp />
            </Sequence>
          ))}
        </DrawnStep.Provider>
      </AbsoluteFill>
      <Captions cues={CAPTIONS} k={K} />
      <Soundtrack music="slides/music.wav" musicVolume={0.42} lines={VO} sfx={SFX} k={K} sfxGain={0.6} duck={0.35} />
    </AbsoluteFill>
  );
};
