// "Knock Knock", restaged for 1920 x 1080. Same art, copy and beats as the
// vertical scenes (src/knock/KnockVideo.tsx), every element re-laid out across
// the width: the front door in its hallway, Meera on the street with the promise
// beside her, Ma and Meera split side by side, each phone beside the face looking
// at it, and the video call ending as scene card + lettering.
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut } from "../../anim";
import { MeeraSketch, type SketchExpr } from "../../sketch/MeeraSketch";
import { Bubble, Burst, Frame, GripHand, InsightsScreen, LINE, LockNotifs, Phone, Sfx } from "../../storyboards/Kit";
import { MA } from "../../storyboards/knock";
import { Person } from "../../storyboards/People";
import { about, blinking, fade, pop, shake, talkAt } from "../../stories/Story";
import { LH, LW } from "../kit";
import { EndSplit, END_X, Lettering } from "../nani/Props";
import { LIPSYNC } from "./lipsync";
import { DateChip, FrontDoor, LSofa, LStreet, Popcorn, Sconce } from "./Props";
import { BEAT, SCENES, VO } from "./timeline";

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
export const Hook: React.FC = () => {
  const f = useCurrentFrame() + SCENES.hook.from;
  const dx = shake(f, [BEAT.knock1, BEAT.knock2], 12);
  const k1 = pop(f, BEAT.knock1, 8);
  const k2 = pop(f, BEAT.knock2 + 1, 8);
  const dim = fade(f, BEAT.title - 6, 12);
  return (
    <Frame bg={["#120A08", "#2B1A12"]} night>
      <g transform={about(960, 540, zoom(f, 0, SCENES.hook.duration, 1, 1.06))}>
        <Sconce x={330} y={330} />
        <Sconce x={1590} y={330} />
        <FrontDoor cx={960} y={90} h={900} dx={dx} />
        <g opacity={1 - dim * 0.75}>
          <g transform={about(960, 480, k1)} opacity={k1 > 0 ? 1 : 0}>
            <Burst x={960} y={480} r={300} color="#FBBF24" n={12} />
            <Sfx x={760} y={430} text="knock" size={190} rot={-10} />
          </g>
          <g transform={about(1180, 650, k2)} opacity={k2 > 0 ? 1 : 0}>
            <Sfx x={1180} y={680} text="knock!" size={190} rot={6} />
          </g>
        </g>
      </g>
      <rect width={LW} height={LH} fill="#000" opacity={dim * 0.55} />
      <g opacity={dim} transform={`translate(0 ${(1 - dim) * 30})`}>
        <text x={960} y={500} textAnchor="middle" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={104} fill="#FDE68A">
          <tspan x={960}>The promise</tspan>
          <tspan x={960} dy={124}>
            you forgot.
          </tspan>
        </text>
      </g>
      <rect width={LW} height={LH} fill="#000" opacity={1 - fade(f, 0, 8)} />
    </Frame>
  );
};

/** Meera on the phone: the phone at her ear and the hand holding it, in her own box (from the vertical layout). */
const OnCall: React.FC<{ x: number; y: number; s: number; children: React.ReactNode }> = ({ x, y, s, children }) => {
  const k = s / 1.45;
  return (
    <g>
      {children}
      <g transform={`translate(${x} ${y}) scale(${k}) translate(-110 -720)`}>
        <g transform="translate(720 980) rotate(16)">
          <rect x={0} y={0} width={120} height={250} rx={22} fill="#0B0F1A" stroke="#3A4154" strokeWidth={4} />
        </g>
        <GripHand x={760} y={1260} s={0.9} rot={-14} />
      </g>
    </g>
  );
};

/* 2. "Okay Ma, I'll call you on Sunday. Promise!" */
export const PromiseScene: React.FC = () => {
  const f = useCurrentFrame() + SCENES.promise.from;
  const t0 = SCENES.promise.from;
  const b = pop(f, BEAT.bubble, 10);
  const auto = interpolate(f, [t0, t0 + SCENES.promise.duration], [-420, 1960], clamp);
  const pan = interpolate(f, [t0, t0 + SCENES.promise.duration], [0, 160], clamp);
  const M = { x: 200, y: 190, s: 1.15 };
  return (
    <Frame bg={["#F59E0B", "#7C3AED"]}>
      <g transform={about(640, 560, zoom(f, t0, SCENES.promise.duration, 1, 1.05))}>
        <LStreet pan={pan} auto={auto} />
        <OnCall {...M}>
          <Meera f={f} {...M} expr="laugh" talk={talk("k2-promise", f)} turn={0.2} />
        </OnCall>
      </g>
      <g transform={about(1300, 320, b)} opacity={b > 0 ? 1 : 0}>
        <Bubble x={900} y={130} w={820} lines={["Okay Ma, I'll call you", "on Sunday. Promise!"]} tail={[720, 470]} size={64} />
      </g>
    </Frame>
  );
};

/* The split between Ma's room (left) and Meera's (right). */
const SPLIT_TOP = 1010;
const SPLIT_BOTTOM = 900;

/* 3. Sunday night. Ma waits by a silent phone; Meera binges. Side by side. */
export const Life: React.FC = () => {
  const f = useCurrentFrame() + SCENES.life.from;
  const t0 = SCENES.life.from;
  const think = pop(f, BEAT.maThinks, 12);
  const tv = 0.12 + 0.08 * Math.sin(f * 0.9) + 0.05 * Math.sin(f * 2.3);
  const glance = f > 330 && f < 370 ? [-0.8, 0.8] : [-0.3, 0.2];
  const MA_S = 1.2;
  return (
    <Frame bg={["#1E1B4B", "#0F172A"]} night>
      <defs>
        <clipPath id="lifeLeft">
          <path d={`M0 0 L${SPLIT_TOP} 0 L${SPLIT_BOTTOM} ${LH} L0 ${LH} Z`} />
        </clipPath>
      </defs>
      {/* Meera's side: TV glow, the sofa, popcorn */}
      <rect x={0} y={0} width={LW} height={LH} fill="#60A5FA" opacity={tv} />
      <g transform={about(1480, 560, zoom(f, t0, SCENES.life.duration, 1, 1.04))}>
        <LSofa y={700} x0={SPLIT_BOTTOM - 60} />
        <Meera f={f} x={1180} y={290} s={1.0} expr="laugh" turn={-0.3} seed={11} />
        <Popcorn x={1790} y={858} s={0.9} rot={Math.sin(f / 9) * 2} />
      </g>
      {/* Ma's side */}
      <g clipPath="url(#lifeLeft)">
        <rect width={LW} height={LH} fill="#FDE7C7" />
        <g transform={about(520, 560, zoom(f, t0, SCENES.life.duration, 1, 1.05))}>
          <Person {...MA} x={560 - 300 * MA_S} y={LH - 800 * MA_S + 10} s={MA_S} expr="worried" glasses gaze={glance as [number, number]} />
          <g>
            <rect x={20} y={880} width={300} height={40} fill="#A9744D" stroke={LINE} strokeWidth={5} />
            <rect x={50} y={920} width={24} height={200} fill="#8B5E3C" stroke={LINE} strokeWidth={4} />
            <rect x={266} y={920} width={24} height={200} fill="#8B5E3C" stroke={LINE} strokeWidth={4} />
            <g transform="translate(110 790) rotate(-8)">
              <rect width={110} height={110} rx={18} fill="#0B0F1A" stroke="#3A4154" strokeWidth={4} />
              <rect x={12} y={12} width={86} height={86} rx={10} fill="#1F2937" />
            </g>
          </g>
        </g>
        <g transform={about(230, 200, think)} opacity={think > 0 ? 1 : 0}>
          <Bubble x={50} y={80} w={360} lines={["Sunday…", "no call?"]} tail={[380, 330]} size={48} think />
        </g>
      </g>
      <path d={`M${SPLIT_TOP} 0 L${SPLIT_BOTTOM} ${LH}`} stroke="#FFFDF8" strokeWidth={14} />
      <DateChip cx={1440} y={40} text="Sunday, 9:40 pm" opacity={fade(f, 262, 10)} />
    </Frame>
  );
};

/* 4. Say it once: Start on Srutam's notification, "Call Ma, Sunday at 7." */
export const Once: React.FC = () => {
  const f = useCurrentFrame() + SCENES.once.from;
  const t0 = SCENES.once.from;
  const recording = f >= BEAT.tapStart + 4;
  const secs = Math.max(0, Math.floor((f - BEAT.tapStart) / 30));
  const showCard = f >= BEAT.card;
  const card = pop(f, BEAT.card, 12);
  const say = pop(f, BEAT.sayIt, 10);
  const phoneIn = interpolate(f, [t0, t0 + 20], [300, 0], { ...clamp, easing: easeOut });
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <Meera f={f} x={170} y={200} s={1.12} expr="smug" talk={talk("k4-once", f)} turn={0.3} gaze={[0.6, 0.3]} />
      <g transform={`translate(0 ${phoneIn})`}>
        <Phone x={1290} y={80} w={400} rot={6}>
          {showCard ? (
            <div style={{ position: "absolute", inset: 0, transform: `scale(${0.94 + 0.06 * card})`, opacity: Math.min(1, card * 1.5) }}>
              <InsightsScreen w={400} note="Call Ma" meta="Today, 8:12 pm · 0:04" items={[{ kind: "REMINDER", text: "Call Ma", meta: "Sunday, 7:00 pm" }]} />
            </div>
          ) : (
            <LockNotifs
              w={400}
              date="Wednesday, 6 March"
              time="8:12"
              notifs={[
                recording
                  ? { title: "Recording", text: `0:0${secs}`, recording: true, ongoing: true, actions: ["Save", "Pause"] }
                  : { title: "Srutam", text: "Ready to record", ongoing: true, actions: ["Start"], pressed: f >= BEAT.tapStart - 3 ? 0 : undefined },
              ]}
            />
          )}
        </Phone>
        <GripHand x={1396} y={1000} s={1.0} rot={4} />
      </g>
      <g transform={about(900, 200, say)} opacity={say > 0 && f < 536 ? 1 : 0}>
        <Bubble x={690} y={90} w={480} lines={["Call Ma,", "Sunday at 7."]} tail={[664, 640]} size={66} />
      </g>
    </Frame>
  );
};

/* 5. Sunday, 6:45 pm. Knock knock: "Call Ma. Starts in 15 minutes." */
export const Knock: React.FC = () => {
  const f = useCurrentFrame() + SCENES.knock.from;
  const t0 = SCENES.knock.from;
  const on = f >= BEAT.notify;
  const hit = pop(f, BEAT.notify, 10);
  const dx = shake(f, [BEAT.notify, BEAT.notify + 3], 9);
  const glowPulse = on ? 0.6 + 0.4 * Math.sin((f - BEAT.notify) / 5) : 0;
  const expr: SketchExpr = f < BEAT.notify ? "blank" : f < BEAT.laugh ? "surprised" : "laugh";
  return (
    <Frame bg={["#1E1B4B", "#312E81"]} night>
      <rect width={LW} height={LH} fill="#FBBF24" opacity={on ? 0.08 * glowPulse : 0} />
      <g transform={about(1000, 560, zoom(f, t0, SCENES.knock.duration, 1, 1.04))}>
        <LSofa y={720} />
        <Meera f={f} x={200} y={240} s={1.1} expr={expr} talk={talk("k5-knocks", f)} turn={0.35} gaze={on ? [0.9, 0.2] : [-0.4, -0.3]} seed={5} />
        <g transform={about(1450, 540, hit)} opacity={on ? 1 : 0}>
          <Burst x={1450} y={540} r={470} color="#FBBF24" n={14} />
        </g>
        <g transform={`translate(${dx} 0)`}>
          <Phone x={1240} y={96} w={400} rot={-4} glow={on ? "#FBBF24" : undefined}>
            <LockNotifs
              w={400}
              date="Sunday, 10 March"
              time="6:45"
              notifs={on ? [{ title: "Call Ma", text: "Starts in 15 minutes.", reminder: true }] : []}
            />
          </Phone>
        </g>
      </g>
      <g transform={about(640, 180, hit)} opacity={on ? 1 : 0}>
        <Sfx x={640} y={215} text="knock knock!" size={140} rot={-6} />
      </g>
      <DateChip cx={640} y={40} text="Sunday, 6:45 pm" opacity={fade(f, 575, 10) * (1 - fade(f, 598, 6))} />
    </Frame>
  );
};

/** The video call: Meera beside the phone, with Ma on it. */
const CallScene: React.FC<{ f: number }> = ({ f }) => {
  const maTalk = talk("ma-remembered", f);
  const b = pop(f, vo("ma-remembered").at, 10);
  return (
    <g>
      <rect width={LW} height={LH} fill="#FDE7C7" />
      <rect y={LH * 0.62} width={LW} height={LH * 0.38} fill="#F3D2A8" opacity={0.6} />
      <Meera f={f} x={170} y={200} s={1.12} expr="laugh" turn={0.3} seed={3} />
      <Phone x={1250} y={64} w={430} rot={5}>
        <div style={{ position: "absolute", inset: 0, background: "#FDE7C7" }}>
          <svg viewBox="60 120 480 690" width="100%" height="100%" preserveAspectRatio="xMidYMax slice" style={{ position: "absolute", inset: 0 }}>
            <Person {...MA} x={0} y={0} s={1} expr={maTalk > 0.15 ? "talk" : "laugh"} glasses />
          </svg>
        </div>
      </Phone>
      <g transform={about(940, 230, b)} opacity={b > 0 ? 1 : 0}>
        <Bubble x={690} y={120} w={500} lines={["Oh! You", "remembered!"]} tail={[1226, 470]} size={64} />
      </g>
    </g>
  );
};

/* 6. "Oh! You remembered!" Then: Say it once. Srutam knocks. */
export const Payoff: React.FC = () => {
  const f = useCurrentFrame() + SCENES.payoff.from;
  const band = interpolate(f, [BEAT.endCard, BEAT.endCard + 22], [0, 1], { ...clamp, easing: easeInOut });
  const logo = pop(f, BEAT.logoKnock, 10);
  const words = fade(f, BEAT.endCard + 12, 14);
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <EndSplit
        k={band}
        letteringK={words}
        scene={<CallScene f={f} />}
        lettering={
          <g>
            <g transform={about(END_X, 520, 0.92 + 0.08 * logo)}>
              <Lettering tagline={["Say it once.", "Srutam knocks."]} y={440} />
            </g>
            <g opacity={Math.min(1, logo * 1.5)} transform={about(END_X, 770, 0.7 + 0.3 * logo)}>
              <Sfx x={END_X} y={800} text="knock knock" size={84} rot={-4} />
            </g>
          </g>
        }
      />
    </Frame>
  );
};
