// "Nani's Laddoos", restaged for 1920 x 1080. Same art, copy and beats as the
// vertical scenes (src/nani/NaniVideo.tsx), every element re-laid out across the
// width: Nani's whole kitchen with the shelf of jars beside her, the Diwali window
// beside Meera, each phone beside the face looking at it, and the video call as
// Meera and the phone side by side, ending as scene card + lettering.
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut } from "../../anim";
import { MeeraSketch, type SketchExpr } from "../../sketch/MeeraSketch";
import {
  AskScreen,
  Bubble,
  Diya,
  Frame,
  GripHand,
  Kadhai,
  Laddoos,
  LINE,
  NotificationShade,
  Phone,
  PlaybackScreen,
  Window,
} from "../../storyboards/Kit";
import { NANI } from "../../storyboards/nani";
import { Person, type PExpr } from "../../storyboards/People";
import { about, blinking, fade, pop, talkAt } from "../../stories/Story";
import { LH, LW } from "../kit";
import { LIPSYNC } from "./lipsync";
import { EndSplit, Ladles, Lettering, LShelf, LTable } from "./Props";
import { BEAT, SCENES, VO } from "./timeline";

const vo = (file: string) => VO.find((v) => v.file === file)!;
const talk = (file: string, f: number) => talkAt(LIPSYNC, file, vo(file).at, f);
const zoom = (f: number, from: number, dur: number, a = 1, b = 1.06) =>
  interpolate(f, [from, from + dur], [a, b], { ...clamp, easing: easeInOut });

const KITCHEN_BG: [string, string] = ["#FCD9A8", "#E9A86B"];

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

/** Nani, with her mouth following her real voice. */
const Nani: React.FC<{ f: number; x: number; y: number; s: number; line?: string; rest?: PExpr }> = ({ f, x, y, s, line, rest = "laugh" }) => {
  const t = line ? talk(line, f) : 0;
  return (
    <g transform={`translate(0 ${Math.sin(f / 26) * 3})`}>
      <Person {...NANI} x={x} y={y} s={s} expr={t > 0.15 ? "talk" : rest} />
    </g>
  );
};

/** Steam wisps rising off the kadhai. */
const Steam: React.FC<{ f: number; x: number; y: number; s?: number }> = ({ f, x, y, s = 1 }) => (
  <g transform={`translate(${x} ${y}) scale(${s})`}>
    {[0, 1, 2].map((i) => {
      const k = ((f + i * 23) % 70) / 70;
      return (
        <path
          key={i}
          d={`M${-70 + i * 70} 0 q-26 -50 0 -100 q26 -50 0 -100`}
          transform={`translate(${Math.sin((f + i * 30) / 14) * 10} ${-k * 120})`}
          stroke="#FFFFFF"
          strokeWidth={12}
          fill="none"
          strokeLinecap="round"
          opacity={0.55 * Math.sin(k * Math.PI)}
        />
      );
    })}
  </g>
);

/** Flickering diya flames. */
const Diyas: React.FC<{ f: number; y: number; xs: number[]; s?: number }> = ({ f, y, xs, s = 0.9 }) => (
  <g>
    {xs.map((x, i) => (
      <g key={x} transform={about(x, y - 40, 1 + 0.06 * Math.sin(f * 0.7 + i * 2))}>
        <Diya x={x} y={y} s={s} />
      </g>
    ))}
  </g>
);

/** Nani's kitchen wall: window on the left, ladles, the shelf of jars on the right. */
const KitchenWall: React.FC = () => (
  <g>
    <Window x={150} y={100} w={340} h={350} sky={["#FEF9C3", "#FDE68A"]} />
    <Ladles x={170} y={500} s={0.9} />
    <LShelf x0={1290} y={430} xs={[1410, 1580, 1740]} />
  </g>
);

/* Where Nani stands in her kitchen (both kitchen shots). */
const NANI_S = 1.35;
const NANI_X = 860 - 300 * NANI_S; // face centred at x 860
const NANI_Y = 150 - 167 * NANI_S; // hair top at y 150
const COUNTER = 820;
const KADHAI: [number, number] = [1210, 872];

/* 1. Nani's kitchen: "Measure? Oh, sweetheart... you go by the smell!" */
export const Kitchen: React.FC = () => {
  const f = useCurrentFrame() + SCENES.kitchen.from;
  return (
    <Frame bg={KITCHEN_BG}>
      <g transform={about(860, 380, zoom(f, 0, SCENES.kitchen.duration, 1, 1.05))}>
        <KitchenWall />
        <Nani f={f} x={NANI_X} y={NANI_Y} s={NANI_S} line="nani-smell" />
        <LTable y={COUNTER} color="#7C5134" top="#A9744D" />
        <Steam f={f} x={KADHAI[0]} y={KADHAI[1] - 20} s={0.9} />
        <Kadhai x={KADHAI[0]} y={KADHAI[1]} s={1.1} steam={false} />
      </g>
      <rect width={LW} height={LH} fill="#000" opacity={1 - fade(f, 0, 10)} />
    </Frame>
  );
};

/* 2. "So one Sunday, I let Srutam listen." Nani keeps talking behind; the phone comes up beside her. */
export const Listen: React.FC = () => {
  const f = useCurrentFrame() + SCENES.listen.from;
  const shade = interpolate(f, [BEAT.shade, BEAT.shade + 16], [-100, 0], { ...clamp, easing: easeOut });
  const lift = interpolate(f, [SCENES.listen.from, SCENES.listen.from + 14], [140, 0], { ...clamp, easing: easeOut });
  return (
    <Frame bg={KITCHEN_BG}>
      <g opacity={0.5}>
        <KitchenWall />
        <Nani f={f} x={NANI_X - 170} y={NANI_Y} s={NANI_S} rest={Math.sin(f / 6) > 0 ? "talk" : "smile"} />
        <LTable y={COUNTER} color="#7C5134" top="#A9744D" />
        <Steam f={f} x={KADHAI[0] - 170} y={KADHAI[1] - 20} s={0.9} />
        <Kadhai x={KADHAI[0] - 170} y={KADHAI[1]} s={1.1} steam={false} />
      </g>
      <g transform={`translate(0 ${lift}) ${about(1450, 500, zoom(f, SCENES.listen.from, SCENES.listen.duration, 1, 1.03))}`}>
        <Phone x={1300} y={56} w={400} rot={4}>
          <div style={{ position: "absolute", inset: 0, background: "#1E2A5A" }} />
          <div style={{ position: "absolute", inset: 0, transform: `translateY(${shade}%)` }}>
            <NotificationShade
              w={400}
              recording={f >= BEAT.start + 3}
              pressed={f >= BEAT.start - 3}
              secs={Math.max(0, Math.floor((f - BEAT.start) / 30))}
            />
          </div>
        </Phone>
        <GripHand x={1438} y={972} s={1.0} rot={4} />
      </g>
    </Frame>
  );
};

/** A small framed photo of Nani on Meera's wall, far from her kitchen. */
const NaniPhoto: React.FC<{ x: number; y: number; w: number; h: number; rot?: number }> = ({ x, y, w, h, rot = 0 }) => (
  <g transform={`rotate(${rot} ${x + w / 2} ${y + h / 2})`}>
    <path d={`M${x + w / 2} ${y - 50} L${x + 20} ${y} M${x + w / 2} ${y - 50} L${x + w - 20} ${y}`} stroke="#94A3B8" strokeWidth={3} />
    <circle cx={x + w / 2} cy={y - 50} r={6} fill="#94A3B8" />
    <rect x={x - 16} y={y - 16} width={w + 32} height={h + 32} rx={6} fill="#7C5134" stroke={LINE} strokeWidth={5} />
    <svg x={x} y={y} width={w} height={h} viewBox="120 130 360 330" preserveAspectRatio="xMidYMin slice">
      <rect x={0} y={0} width={600} height={800} fill="#FCD9A8" />
      <Person {...NANI} x={0} y={0} s={1} expr="smile" />
    </svg>
    <rect x={x} y={y} width={w} height={h} fill="none" stroke={LINE} strokeWidth={3} />
  </g>
);

const BURSTS = [
  [420, 1140, 170, "#F472B6"],
  [480, 1560, 250, "#FBBF24"],
  [530, 1280, 120, "#60A5FA"],
  [560, 1480, 200, "#34D399"],
] as const;

/* 3. Diwali, 1,400 km away: the window beside Meera. "...and how much gur goes in?!" */
export const Diwali: React.FC = () => {
  const f = useCurrentFrame() + SCENES.diwali.from;
  const think = pop(f, BEAT.gur, 12);
  return (
    <Frame bg={["#1E1B4B", "#312E81"]} night>
      <g transform={about(900, 560, zoom(f, SCENES.diwali.from, SCENES.diwali.duration, 1, 1.05))}>
        <Window x={900} y={60} w={900} h={540} sky={["#0B1033", "#1E3A8A"]} moon />
        {BURSTS.map(([at, x, y, c]) => {
          const t0 = at - 380 + SCENES.diwali.from;
          const k = interpolate(f, [t0, t0 + 24], [0, 1], clamp);
          return k > 0 && k < 1 ? (
            <g key={at} opacity={1 - k}>
              {Array.from({ length: 12 }, (_, i) => {
                const a = (i / 12) * Math.PI * 2;
                return <circle key={i} cx={x + Math.cos(a) * k * 100} cy={y + Math.sin(a) * k * 100} r={7} fill={c} />;
              })}
            </g>
          ) : null;
        })}
        {Array.from({ length: 20 }, (_, i) => (
          <circle
            key={i}
            cx={950 + (i % 10) * 88}
            cy={200 + Math.floor(i / 10) * 230 + (i % 3) * 26}
            r={9}
            fill="#FCD34D"
            opacity={0.5 + 0.4 * Math.sin(f / 8 + i)}
          />
        ))}
        <rect x={860} y={600} width={980} height={30} fill="#A9744D" stroke={LINE} strokeWidth={5} />
        <Diyas f={f} y={600} xs={[1040, 1350, 1660]} />
        <Meera
          f={f}
          x={150}
          y={190}
          s={1.12}
          expr={f < BEAT.gur ? "blank" : "worried"}
          turn={0.15}
          gaze={f < BEAT.gur ? [0.7, -0.5] : [0.4, 0.8]}
          seed={9}
        />
        <NaniPhoto x={1500} y={650} w={230} h={210} rot={3} />
        <LTable y={900} color="#5B3A22" top="#7C5134" />
        <Kadhai x={1250} y={960} s={1.0} steam={false} />
      </g>
      <g transform={about(1060, 740, think)} opacity={think > 0 ? 1 : 0}>
        <Bubble x={860} y={650} w={460} lines={["How much", "gur?!"]} tail={[640, 520]} size={62} think />
      </g>
    </Frame>
  );
};

const Q = "How much jaggery did Nani put in the laddoos?";
const A = "About one fistful of gur for every bowl of besan, “and a little love.”";

/* 4. Ask Srutam AI: Meera asks, the phone beside her answers. */
export const Ask: React.FC = () => {
  const f = useCurrentFrame() + SCENES.ask.from;
  const t0 = SCENES.ask.from;
  const qn = Math.round(interpolate(f, [BEAT.qStart, BEAT.qEnd], [0, Q.length], clamp));
  const an = Math.round(interpolate(f, [BEAT.aStart, BEAT.aEnd], [0, A.length], clamp));
  const lift = interpolate(f, [t0, t0 + 18], [300, 0], { ...clamp, easing: easeOut });
  const glow = fade(f, t0 + 6, 14);
  const expr: SketchExpr = f < BEAT.aStart + 10 ? "blank" : f < BEAT.refs ? "surprised" : "laugh";
  return (
    <Frame bg={["#1E1B4B", "#0B0F26"]} night>
      {Array.from({ length: 14 }, (_, i) => (
        <circle
          key={i}
          cx={60 + i * 140}
          cy={120 + (i % 4) * 46}
          r={8}
          fill="#FCD34D"
          opacity={0.25 + 0.2 * Math.sin(f / 9 + i)}
        />
      ))}
      <g transform={about(960, 560, zoom(f, t0, SCENES.ask.duration, 1, 1.04))}>
        <defs>
          <radialGradient id="askGlow">
            <stop offset="0%" stopColor="#3B82F6" stopOpacity={0.32} />
            <stop offset="100%" stopColor="#3B82F6" stopOpacity={0} />
          </radialGradient>
        </defs>
        <ellipse cx={1250} cy={540} rx={760} ry={620} fill="url(#askGlow)" opacity={glow} />
        <Meera f={f} x={240} y={200} s={1.1} expr={expr} talk={talk("n4-ask", f)} turn={0.3} gaze={[0.9, 0.35]} seed={6} />
        <g transform={`translate(0 ${lift})`}>
          <Phone x={1200} y={96} w={400} rot={-3} glow="#3B82F6">
            <AskScreen
              w={400}
              q={Q.slice(0, qn)}
              a={A.slice(0, an)}
              refs={f >= BEAT.refs ? ["Nani's kitchen · 14 min"] : []}
              pressed={f >= BEAT.tapRef && f < BEAT.tapRef + 8}
            />
          </Phone>
          <GripHand x={1446} y={990} s={1.0} rot={-3} />
        </g>
      </g>
    </Frame>
  );
};

/** A happy tear, in Meera's own 600 x 800 box (k: 0 -> 1 as it rolls down). */
const Tear: React.FC<{ x: number; y: number; s: number; k: number }> = ({ x, y, s, k }) =>
  k > 0 ? (
    <g transform={`translate(${x + 264 * s} ${y + 400 * s}) scale(${s / 1.25})`}>
      <path
        d="M0 0 C12 22 12 38 0 44 C-12 38 -12 22 0 0 Z"
        transform={`translate(0 ${k * 40})`}
        opacity={Math.min(1, k * 3) * (1 - Math.max(0, k - 0.8) * 5)}
        fill="#BFDBFE"
        stroke={LINE}
        strokeWidth={3}
      />
    </g>
  ) : null;

/* 5. Nani's own voice from the phone beside Meera: "...and a little love. That's it." */
export const Voice: React.FC = () => {
  const f = useCurrentFrame() + SCENES.voice.from;
  const t0 = BEAT.playFrom;
  const progress = interpolate(f, [t0, t0 + vo("nani-recipe").frames], [0.02, 1], clamp);
  const expr: SketchExpr = f < t0 + 12 ? "surprised" : f < t0 + 70 ? "blank" : "laugh";
  const tear = interpolate(f, [BEAT.pyaar, BEAT.pyaar + 50], [0, 1], clamp);
  const warm = fade(f, BEAT.pyaar - 20, 40);
  const playing = f >= t0 && f < t0 + vo("nani-recipe").frames;
  const M = { x: 190, y: 160, s: 1.15 };
  return (
    <Frame bg={["#3B2A5E", "#1E1B4B"]} night>
      <rect width={LW} height={LH} fill="#FBBF24" opacity={0.08 + 0.1 * warm} />
      <g transform={about(900, 560, zoom(f, SCENES.voice.from, SCENES.voice.duration, 1, 1.05))}>
        <Meera f={f} x={M.x} y={M.y} s={M.s} expr={expr} turn={0.25} gaze={[0.8, 0.4]} seed={2} />
        <Tear {...M} k={tear} />
        <LTable y={890} color="#5B3A22" top="#7C5134" />
        <Diyas f={f} y={905} xs={[110, 1010, 1820]} s={0.8} />
        <Steam f={f} x={790} y={920} s={0.75} />
        <Kadhai x={790} y={960} s={0.95} steam={false} />
        <Phone x={1300} y={100} w={390} rot={-5} glow={playing ? "#FBBF24" : undefined}>
          <PlaybackScreen w={390} title="Nani's kitchen" meta="Sun, 3 Mar · 14:02" quote="…and a little love. That's it." progress={progress} />
        </Phone>
      </g>
    </Frame>
  );
};

/** The video call itself: Meera and the plate of laddoos beside the phone with Nani on it. */
const CallScene: React.FC<{ f: number }> = ({ f }) => {
  const plate = interpolate(f, [SCENES.call.from, SCENES.call.from + 20], [220, 0], { ...clamp, easing: easeOut });
  return (
    <g>
      <rect width={LW} height={LH} fill="#FDE7C7" />
      <rect y={LH * 0.6} width={LW} height={LH * 0.4} fill="#F3D2A8" opacity={0.6} />
      <Meera f={f} x={140} y={190} s={1.12} expr="laugh" turn={0.3} seed={4} />
      <g transform={`translate(0 ${plate})`}>
        <Laddoos x={860} y={850} s={0.95} />
      </g>
      <Phone x={1230} y={64} w={430} rot={4}>
        <div style={{ position: "absolute", inset: 0, background: "#FCD9A8" }}>
          <svg viewBox="60 120 480 690" width="100%" height="100%" preserveAspectRatio="xMidYMax slice" style={{ position: "absolute", inset: 0 }}>
            <Person {...NANI} x={0} y={0} s={1} expr={talk("nani-mine", f) > 0.15 ? "talk" : "laugh"} />
          </svg>
        </div>
      </Phone>
    </g>
  );
};

/* 6. Video call: "Oh, wow! Just like mine!" Then: Keep the recipe. Keep the voice. */
export const Call: React.FC = () => {
  const f = useCurrentFrame() + SCENES.call.from;
  const band = interpolate(f, [BEAT.band, BEAT.band + 22], [0, 1], { ...clamp, easing: easeInOut });
  const words = fade(f, BEAT.band + 18, 16);
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <EndSplit k={band} letteringK={words} scene={<CallScene f={f} />} lettering={<Lettering tagline={["Keep the recipe.", "Keep the voice."]} />} />
    </Frame>
  );
};
