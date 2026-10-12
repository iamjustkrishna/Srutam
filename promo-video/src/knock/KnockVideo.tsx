// "Knock Knock": we promise out loud and forget; Srutam knocks at the right
// moment. The app's own knock-knock reminder sound is the hook and the payoff.
import { AbsoluteFill, interpolate, Sequence, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut } from "../anim";
import { MeeraSketch, type SketchExpr } from "../sketch/MeeraSketch";
import {
  Bubble,
  Burst,
  Caption,
  Door,
  EndCard,
  Frame,
  GripHand,
  H,
  InsightsScreen,
  LINE,
  LockNotifs,
  Phone,
  Sfx,
  W,
} from "../storyboards/Kit";
import { MA, Sofa, Street } from "../storyboards/knock";
import { Person } from "../storyboards/People";
import { about, blinking, Captions, fade, pop, Scene, shake, Soundtrack, talkAt } from "../stories/Story";
import { LIPSYNC } from "./lipsync";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";

const vo = (file: string) => VO.find((v) => v.file === file)!;
const talk = (file: string, f: number) => talkAt(LIPSYNC, file, vo(file).at, f);

/** Slow push-in over a scene. */
const zoom = (f: number, from: number, dur: number, a = 1, b = 1.06) =>
  interpolate(f, [from, from + dur], [a, b], { ...clamp, easing: easeInOut });

const Meera: React.FC<{
  f: number;
  x: number;
  y: number;
  s: number;
  expr: SketchExpr;
  talk?: number;
  turn?: number;
  gaze?: [number, number];
  seed?: number;
}> = ({ f, x, y, s, expr, talk: t = 0, turn = 0, gaze = [0, 0], seed = 0 }) => (
  <g transform={`translate(0 ${Math.sin((f + seed) / 24) * 4})`}>
    <MeeraSketch
      x={x}
      y={y}
      s={s}
      expr={expr}
      talk={t}
      turn={turn}
      gaze={gaze}
      blink={!["sleep", "laugh"].includes(expr) && blinking(f, seed)}
      sway={Math.sin((f + seed) / 30) * 0.6}
    />
  </g>
);

/* 1. A door. Knock knock. "Who's there?" ... The promise you forgot. */
const Hook: React.FC = () => {
  const f = useCurrentFrame() + SCENES.hook.from;
  const dx = shake(f, [BEAT.knock1, BEAT.knock2], 12);
  const k1 = pop(f, BEAT.knock1, 8);
  const k2 = pop(f, BEAT.knock2 + 1, 8);
  const dim = fade(f, BEAT.title - 6, 12);
  return (
    <Frame bg={["#120A08", "#2B1A12"]} night>
      <g transform={about(540, 1000, zoom(f, 0, 105, 1, 1.08))}>
        <g transform={`translate(${dx} 0)`}>
          <Door x={150} y={240} w={780} h={1700} />
        </g>
        <g opacity={1 - dim * 0.75}>
          <g transform={about(540, 900, k1)} opacity={k1 > 0 ? 1 : 0}>
            <Burst x={540} y={900} r={240} color="#FBBF24" n={12} />
            <Sfx x={540} y={860} text="knock" size={190} rot={-10} />
          </g>
          <g transform={about(560, 1040, k2)} opacity={k2 > 0 ? 1 : 0}>
            <Sfx x={560} y={1060} text="knock!" size={190} rot={6} />
          </g>
        </g>
      </g>
      <rect width={W} height={H} fill="#000" opacity={dim * 0.55} />
      <g opacity={dim} transform={`translate(0 ${(1 - dim) * 30})`}>
        <text x={540} y={880} textAnchor="middle" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={92} fill="#FDE68A">
          <tspan x={540}>The promise</tspan>
          <tspan x={540} dy={112}>
            you forgot.
          </tspan>
        </text>
      </g>
      <rect width={W} height={H} fill="#000" opacity={1 - fade(f, 0, 8)} />
    </Frame>
  );
};

/* 2. "Haan Ma, I'll call you on Sunday. Promise!" */
const PromiseScene: React.FC = () => {
  const f = useCurrentFrame() + SCENES.promise.from;
  const b = pop(f, BEAT.bubble, 10);
  const autoX = interpolate(f, [105, 255], [-520, 260], clamp);
  return (
    <Frame bg={["#F59E0B", "#7C3AED"]}>
      <g transform={about(540, 1100, zoom(f, 105, 150, 1, 1.05))}>
        <g transform={`translate(${autoX + 520} 0)`}>
          <Street />
        </g>
        <Meera f={f} x={110} y={720} s={1.45} expr="laugh" talk={talk("k2-promise", f)} turn={0.2} />
        <g transform="translate(720 980) rotate(16)">
          <rect x={0} y={0} width={120} height={250} rx={22} fill="#0B0F1A" stroke="#3A4154" strokeWidth={4} />
        </g>
        <GripHand x={760} y={1260} s={0.9} rot={-14} />
      </g>
      <g transform={about(540, 560, b)} opacity={b > 0 ? 1 : 0}>
        <Bubble x={90} y={260} w={900} lines={["Haan Ma, I'll call you", "on Sunday. Promise!"]} tail={[560, 760]} size={64} />
      </g>
    </Frame>
  );
};

/* 3. Sunday night. Ma waits; Meera binges. */
const Life: React.FC = () => {
  const f = useCurrentFrame() + SCENES.life.from;
  const think = pop(f, BEAT.maThinks, 12);
  const tv = 0.12 + 0.08 * Math.sin(f * 0.9) + 0.05 * Math.sin(f * 2.3);
  const glance = f > 330 && f < 370 ? [-0.8, 0.8] : [-0.3, 0.2];
  return (
    <Frame bg={["#1E1B4B", "#0F172A"]} night>
      <defs>
        <clipPath id="lifeTop">
          <path d={`M0 0 L${W} 0 L${W} 860 L0 1000 Z`} />
        </clipPath>
      </defs>
      <g clipPath="url(#lifeTop)">
        <rect width={W} height={1000} fill="#FDE7C7" />
        <g transform={about(540, 500, zoom(f, 255, 150, 1, 1.05))}>
          <Person {...MA} x={330} y={180} s={1.05} expr="worried" glasses gaze={glance as [number, number]} />
          <rect x={40} y={800} width={300} height={40} fill="#A9744D" stroke={LINE} strokeWidth={5} />
          <g transform="translate(110 690) rotate(-8)">
            <rect width={110} height={110} rx={18} fill="#0B0F1A" stroke="#3A4154" strokeWidth={4} />
            <rect x={12} y={12} width={86} height={86} rx={10} fill="#1F2937" />
          </g>
        </g>
        <g transform={about(220, 330, think)} opacity={think > 0 ? 1 : 0}>
          <Bubble x={40} y={150} w={360} lines={["Sunday…", "no call?"]} tail={[330, 420]} size={48} think />
        </g>
      </g>
      <path d={`M0 1000 L${W} 860`} stroke="#FFFDF8" strokeWidth={14} />
      <rect x={0} y={1000} width={W} height={920} fill="#60A5FA" opacity={tv} />
      <Sofa y={1500} />
      <Meera f={f} x={250} y={1030} s={1.0} expr="laugh" turn={-0.3} seed={11} />
      <g transform={`translate(800 1720) rotate(${Math.sin(f / 9) * 2})`}>
        <path d="M-120 0 C-110 90 110 90 120 0 Z" fill="#DC2626" stroke={LINE} strokeWidth={5} />
        {Array.from({ length: 9 }, (_, i) => (
          <circle key={i} cx={-90 + i * 22} cy={-10 - (i % 3) * 16} r={20} fill="#FEF3C7" stroke={LINE} strokeWidth={2} />
        ))}
      </g>
      <g opacity={fade(f, 262, 10)}>
        <Caption text="Sunday, 9:40 pm" y={1040} />
      </g>
    </Frame>
  );
};

/* 4. Say it once: Start on Srutam's notification, "Call Ma, Sunday at 7." */
const Once: React.FC = () => {
  const f = useCurrentFrame() + SCENES.once.from;
  const recording = f >= BEAT.tapStart + 4;
  const secs = Math.max(0, Math.floor((f - BEAT.tapStart) / 30));
  const showCard = f >= BEAT.card;
  const card = pop(f, BEAT.card, 12);
  const say = pop(f, BEAT.sayIt, 10);
  const phoneIn = interpolate(f, [405, 425], [260, 0], { ...clamp, easing: easeOut });
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <Meera f={f} x={-170} y={880} s={1.3} expr="smug" talk={talk("k4-once", f)} turn={0.3} gaze={[0.6, 0.3]} />
      <g transform={`translate(0 ${phoneIn})`}>
        <Phone x={520} y={600} w={520} rot={6}>
          {showCard ? (
            <div style={{ position: "absolute", inset: 0, transform: `scale(${0.94 + 0.06 * card})`, opacity: Math.min(1, card * 1.5) }}>
              <InsightsScreen w={520} note="Call Ma" meta="Today, 8:12 pm · 0:04" items={[{ kind: "REMINDER", text: "Call Ma", meta: "Sunday, 7:00 pm" }]} />
            </div>
          ) : (
            <LockNotifs
              w={520}
              date="Wednesday, 6 March"
              time="8:12"
              notifs={[
                recording
                  ? { title: "Recording", text: `0:0${secs}`, recording: true, actions: ["Pause", "Save"] }
                  : { title: "Srutam", text: "Ready to record", actions: ["Start"], pressed: f >= BEAT.tapStart - 3 ? 0 : undefined },
              ]}
            />
          )}
        </Phone>
        <GripHand x={790} y={1800} s={1.2} rot={-4} />
      </g>
      <g transform={about(330, 400, say)} opacity={say > 0 && f < 536 ? 1 : 0}>
        <Bubble x={40} y={200} w={600} lines={["Call Ma,", "Sunday at 7."]} tail={[230, 1000]} size={66} />
      </g>
    </Frame>
  );
};

/* 5. Sunday, 6:45 pm. Knock knock: "Call Ma. Starts in 15 minutes." */
const Knock: React.FC = () => {
  const f = useCurrentFrame() + SCENES.knock.from;
  const on = f >= BEAT.notify;
  const hit = pop(f, BEAT.notify, 10);
  const dx = shake(f, [BEAT.notify, BEAT.notify + 3], 9);
  const glowPulse = on ? 0.6 + 0.4 * Math.sin((f - BEAT.notify) / 5) : 0;
  const expr: SketchExpr = f < BEAT.notify ? "blank" : f < BEAT.laugh ? "surprised" : "laugh";
  return (
    <Frame bg={["#1E1B4B", "#312E81"]} night>
      <rect width={W} height={H} fill="#FBBF24" opacity={on ? 0.08 * glowPulse : 0} />
      <g transform={about(700, 1100, zoom(f, 570, 180, 1, 1.07))}>
        <Sofa y={1500} />
        <Meera f={f} x={-60} y={1000} s={1.15} expr={expr} talk={talk("k5-knocks", f)} turn={0.35} gaze={on ? [0.9, 0.2] : [-0.4, -0.3]} seed={5} />
        <g transform={`translate(${dx} 0)`}>
          <Phone x={470} y={560} w={560} rot={-4} glow={on ? "#FBBF24" : undefined}>
            <LockNotifs
              w={560}
              date="Sunday, 10 March"
              time="6:45"
              notifs={on ? [{ title: "Call Ma", text: "Starts in 15 minutes.", reminder: true }] : []}
            />
          </Phone>
        </g>
        <g transform={about(750, 1180, hit)} opacity={on ? 1 : 0}>
          <Burst x={750} y={1180} r={700} color="#FBBF24" n={14} />
        </g>
      </g>
      <g transform={about(540, 360, hit)} opacity={on ? 1 : 0}>
        <Sfx x={540} y={380} text="knock knock!" size={150} rot={-6} />
      </g>
      <g opacity={fade(f, 575, 10) * (1 - fade(f, 598, 6))}>
        <Caption text="Sunday, 6:45 pm" y={140} />
      </g>
    </Frame>
  );
};

/* 6. "Arre! You remembered!" Then: Say it once. Srutam knocks. */
const Payoff: React.FC = () => {
  const f = useCurrentFrame() + SCENES.payoff.from;
  const maTalk = talk("ma-remembered", f);
  const b = pop(f, vo("ma-remembered").at, 10);
  const band = interpolate(f, [BEAT.endCard, BEAT.endCard + 18], [0, 1], { ...clamp, easing: easeOut });
  const logo = pop(f, BEAT.logoKnock, 10);
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <g transform={`translate(0 ${-band * 420})`}>
        <Meera f={f} x={-30} y={420} s={1.0} expr="laugh" turn={0.3} seed={3} />
        <Phone x={600} y={200} w={380} rot={5}>
          <div style={{ position: "absolute", inset: 0, background: "#FDE7C7" }}>
            <svg viewBox="60 170 480 640" width="100%" height="100%" preserveAspectRatio="xMidYMax slice" style={{ position: "absolute", inset: 0 }}>
              <Person {...MA} x={0} y={0} s={1} expr={maTalk > 0.15 ? "talk" : "laugh"} glasses />
            </svg>
          </div>
        </Phone>
        <g transform={about(790, 160, b)} opacity={b > 0 ? 1 : 0}>
          <Bubble x={500} y={40} w={560} lines={["Arre! You", "remembered!"]} tail={[760, 260]} size={54} />
        </g>
      </g>
      <g transform={`translate(0 ${(1 - band) * 1300})`}>
        <rect x={0} y={760} width={W} height={1200} fill="#050714" />
        <g transform={`translate(0 220) ${about(540, 860, 0.9 + 0.1 * logo)}`}>
          <EndCard tagline={["Say it once.", "Srutam knocks."]} />
        </g>
        <g opacity={logo}>
          <Sfx x={540} y={1640} text="knock knock" size={86} rot={-4} />
        </g>
      </g>
    </Frame>
  );
};

const SCENE_LIST = [
  ["1 Hook", SCENES.hook, Hook, 0],
  ["2 Promise", SCENES.promise, PromiseScene, 6],
  ["3 Life happens", SCENES.life, Life, 6],
  ["4 Say it once", SCENES.once, Once, 6],
  ["5 Knock", SCENES.knock, Knock, 6],
  ["6 Payoff", SCENES.payoff, Payoff, 6],
] as const;

export const KnockVideo: React.FC = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    {SCENE_LIST.map(([name, s, Comp, fadeIn]) => (
      <Sequence key={name} name={name} from={s.from} durationInFrames={s.duration}>
        <Scene fadeIn={fadeIn}>
          <Comp />
        </Scene>
      </Sequence>
    ))}
    <Captions cues={CAPTIONS} />
    <Soundtrack music="knock/music.wav" lines={VO} sfx={SFX} />
  </AbsoluteFill>
);
