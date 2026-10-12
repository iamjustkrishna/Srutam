// "Nani's Laddoos": the recipe that only ever lived in Nani's voice, recorded
// once in her kitchen and asked back on Diwali, 1,400 km away.
import { AbsoluteFill, interpolate, Sequence, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut } from "../anim";
import { MeeraSketch, type SketchExpr } from "../sketch/MeeraSketch";
import {
  AskScreen,
  Bubble,
  Diya,
  EndBand,
  Frame,
  GripHand,
  H,
  Kadhai,
  Laddoos,
  LINE,
  Phone,
  PlaybackScreen,
  QuickSettings,
  Table,
  W,
  Window,
} from "../storyboards/Kit";
import { NANI, Shelf } from "../storyboards/nani";
import { Person, type PExpr } from "../storyboards/People";
import { about, blinking, Captions, fade, pop, Scene, Soundtrack, talkAt } from "../stories/Story";
import { LIPSYNC } from "./lipsync";
import { BEAT, CAPTIONS, SCENES, SFX, VO } from "./timeline";

const vo = (file: string) => VO.find((v) => v.file === file)!;
const talk = (file: string, f: number) => talkAt(LIPSYNC, file, vo(file).at, f);
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

/* 1. Nani's kitchen: "Measure? Arre beta, you go by the smell!" */
const Kitchen: React.FC = () => {
  const f = useCurrentFrame() + SCENES.kitchen.from;
  return (
    <Frame bg={["#FCD9A8", "#E9A86B"]}>
      <g transform={about(540, 1100, zoom(f, 0, 240, 1, 1.08))}>
        <Window x={600} y={200} w={360} h={380} sky={["#FEF9C3", "#FDE68A"]} />
        <Shelf />
        <Nani f={f} x={150} y={700} s={1.3} line="nani-smell" />
        <Table y={1650} color="#7C5134" top="#A9744D" />
        <Steam f={f} x={560} y={1640} s={1.2} />
        <Kadhai x={560} y={1690} s={1.3} steam={false} />
      </g>
      <rect width={W} height={H} fill="#000" opacity={1 - fade(f, 0, 10)} />
    </Frame>
  );
};

/* 2. "So one Sunday, I let my phone listen." Quick Settings, Voice Record. */
const Listen: React.FC = () => {
  const f = useCurrentFrame() + SCENES.listen.from;
  const shade = interpolate(f, [BEAT.shade, BEAT.shade + 16], [-100, 0], { ...clamp, easing: easeOut });
  const ripple = interpolate(f, [BEAT.tile, BEAT.tile + 14], [0, 1], clamp);
  return (
    <Frame bg={["#FCD9A8", "#E9A86B"]}>
      <g opacity={0.45}>
        <Nani f={f} x={560} y={560} s={0.9} rest={Math.sin(f / 6) > 0 ? "talk" : "smile"} />
        <Steam f={f} x={760} y={1230} s={0.7} />
        <Kadhai x={760} y={1260} s={0.8} steam={false} />
      </g>
      <g transform={about(480, 1100, zoom(f, 240, 140, 1, 1.04))}>
        <Phone x={170} y={420} w={620} rot={-4}>
          <div style={{ position: "absolute", inset: 0, background: "#1E2A5A" }} />
          <div style={{ position: "absolute", inset: 0, transform: `translateY(${shade}%)` }}>
            <QuickSettings w={620} recording={f >= BEAT.tile + 3} />
          </div>
        </Phone>
        {ripple > 0 && ripple < 1 && (
          <circle cx={377} cy={900} r={40 + ripple * 90} fill="none" stroke="#FFFFFF" strokeWidth={10} opacity={1 - ripple} />
        )}
        <GripHand x={470} y={1780} s={1.4} rot={2} />
      </g>
    </Frame>
  );
};

/* 3. Diwali, 1,400 km away: "...and how much gur goes in?!" */
const Diwali: React.FC = () => {
  const f = useCurrentFrame() + SCENES.diwali.from;
  const think = pop(f, BEAT.gur, 12);
  const bursts = [
    [420, 300, 430, "#F472B6"],
    [480, 760, 520, "#FBBF24"],
    [530, 420, 360, "#60A5FA"],
    [560, 640, 470, "#34D399"],
  ] as const;
  return (
    <Frame bg={["#1E1B4B", "#312E81"]} night>
      <g transform={about(540, 1100, zoom(f, 380, 220, 1, 1.07))}>
        <Window x={120} y={220} w={840} h={620} sky={["#0B1033", "#1E3A8A"]} moon />
        {bursts.map(([at, x, y, c]) => {
          const k = interpolate(f, [at, at + 24], [0, 1], clamp);
          return k > 0 && k < 1 ? (
            <g key={at} opacity={1 - k}>
              {Array.from({ length: 12 }, (_, i) => {
                const a = (i / 12) * Math.PI * 2;
                return <circle key={i} cx={x + Math.cos(a) * k * 110} cy={y + Math.sin(a) * k * 110} r={7} fill={c} />;
              })}
            </g>
          ) : null;
        })}
        {Array.from({ length: 20 }, (_, i) => (
          <circle
            key={i}
            cx={160 + (i % 10) * 82}
            cy={380 + Math.floor(i / 10) * 260 + (i % 3) * 30}
            r={9}
            fill="#FCD34D"
            opacity={0.5 + 0.4 * Math.sin(f / 8 + i)}
          />
        ))}
        <rect x={80} y={840} width={920} height={30} fill="#A9744D" stroke={LINE} strokeWidth={5} />
        <Diyas f={f} y={840} xs={[240, 540, 840]} />
        <Meera f={f} x={90} y={980} s={1.25} expr={f < BEAT.gur ? "blank" : "worried"} turn={0.15} gaze={f < BEAT.gur ? [-0.4, -0.5] : [0.3, 0.8]} seed={9} />
        <Table y={1720} color="#5B3A22" top="#7C5134" />
        <Kadhai x={760} y={1760} s={1.1} steam={false} />
      </g>
      <g transform={about(740, 1030, think)} opacity={think > 0 ? 1 : 0}>
        <Bubble x={470} y={930} w={540} lines={["How much", "gur?!"]} tail={[330, 1160]} size={62} think />
      </g>
    </Frame>
  );
};

const Q = "How much jaggery did Nani put in the laddoos?";
const A = "About one fistful of gur for every bowl of besan, “and a little love.”";

/* 4. Ask Srutam AI. */
const Ask: React.FC = () => {
  const f = useCurrentFrame() + SCENES.ask.from;
  const qn = Math.round(interpolate(f, [BEAT.qStart, BEAT.qEnd], [0, Q.length], clamp));
  const an = Math.round(interpolate(f, [BEAT.aStart, BEAT.aEnd], [0, A.length], clamp));
  const lift = interpolate(f, [600, 618], [300, 0], { ...clamp, easing: easeOut });
  return (
    <Frame bg={["#1E1B4B", "#0B0F26"]} night>
      <g transform={`translate(0 ${lift}) ${about(500, 1000, zoom(f, 600, 170, 1, 1.04))}`}>
        <Phone x={150} y={250} w={700} rot={-2} glow="#3B82F6">
          <AskScreen
            w={700}
            q={Q.slice(0, qn)}
            a={A.slice(0, an)}
            refs={f >= BEAT.refs ? ["Nani's kitchen · 14 min"] : []}
            pressed={f >= BEAT.tapRef && f < BEAT.tapRef + 8}
          />
        </Phone>
        <GripHand x={500} y={1820} s={1.4} rot={-2} />
      </g>
    </Frame>
  );
};

/* 5. Nani's own voice: "...aur thoda sa pyaar. Bas." */
const Voice: React.FC = () => {
  const f = useCurrentFrame() + SCENES.voice.from;
  const progress = interpolate(f, [800, 997], [0.02, 1], clamp);
  const expr: SketchExpr = f < 812 ? "surprised" : f < 870 ? "blank" : "laugh";
  const tear = interpolate(f, [BEAT.pyaar, BEAT.pyaar + 50], [0, 1], clamp);
  const warm = fade(f, BEAT.pyaar - 20, 40);
  return (
    <Frame bg={["#3B2A5E", "#1E1B4B"]} night>
      <rect width={W} height={H} fill="#FBBF24" opacity={0.08 + 0.1 * warm} />
      <g transform={about(540, 1200, zoom(f, 770, 240, 1, 1.08))}>
        <Diyas f={f} y={420} xs={[160, 920]} s={0.7} />
        <Meera f={f} x={-30} y={760} s={1.25} expr={expr} turn={0.25} gaze={[0.8, 0.4]} seed={2} />
        {tear > 0 && (
          <path
            d="M300 1260 C312 1282 312 1298 300 1304 C288 1298 288 1282 300 1260 Z"
            transform={`translate(0 ${tear * 40})`}
            opacity={Math.min(1, tear * 3) * (1 - Math.max(0, tear - 0.8) * 5)}
            fill="#BFDBFE"
            stroke={LINE}
            strokeWidth={3}
          />
        )}
        <Table y={1640} color="#5B3A22" top="#7C5134" />
        <Steam f={f} x={340} y={1650} />
        <Kadhai x={340} y={1690} s={1.0} steam={false} />
        <Phone x={680} y={980} w={330} rot={-6} glow={f >= 800 && f < 1000 ? "#FBBF24" : undefined}>
          <PlaybackScreen w={330} title="Nani's kitchen" meta="Sun, 3 Mar · 14:02" quote="…aur thoda sa pyaar. Bas." progress={progress} />
        </Phone>
      </g>
    </Frame>
  );
};

/* 6. Video call: "Arre wah! Just like mine!" Keep the recipe. Keep the voice. */
const Call: React.FC = () => {
  const f = useCurrentFrame() + SCENES.call.from;
  const band = interpolate(f, [BEAT.band, BEAT.band + 18], [0, 1], { ...clamp, easing: easeOut });
  const plate = interpolate(f, [1010, 1030], [200, 0], { ...clamp, easing: easeOut });
  return (
    <Frame bg={["#FDE7C7", "#F3D2A8"]}>
      <g transform={`translate(0 ${-band * 200})`}>
        <Phone x={470} y={170} w={540} rot={4}>
          <div style={{ position: "absolute", inset: 0, background: "#FCD9A8" }}>
            <svg viewBox="60 170 480 640" width="100%" height="100%" preserveAspectRatio="xMidYMax slice" style={{ position: "absolute", inset: 0 }}>
              <Person {...NANI} x={0} y={0} s={1} expr={talk("nani-mine", f) > 0.15 ? "talk" : "laugh"} />
            </svg>
          </div>
        </Phone>
        <Meera f={f} x={-110} y={620} s={1.05} expr="laugh" turn={0.3} seed={4} />
        <g transform={`translate(0 ${plate})`}>
          <Laddoos x={330} y={1380} s={0.9} />
        </g>
      </g>
      <g transform={`translate(0 ${(1 - band) * 420})`}>
        <EndBand tagline="Keep the recipe. Keep the voice." />
      </g>
    </Frame>
  );
};

const SCENE_LIST = [
  ["1 Kitchen", SCENES.kitchen, Kitchen, 0],
  ["2 Let it listen", SCENES.listen, Listen, 6],
  ["3 Diwali", SCENES.diwali, Diwali, 8],
  ["4 Ask", SCENES.ask, Ask, 6],
  ["5 Her voice", SCENES.voice, Voice, 8],
  ["6 Call", SCENES.call, Call, 8],
] as const;

export const NaniVideo: React.FC = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    {SCENE_LIST.map(([name, s, Comp, fadeIn]) => (
      <Sequence key={name} name={name} from={s.from} durationInFrames={s.duration}>
        <Scene fadeIn={fadeIn}>
          <Comp />
        </Scene>
      </Sequence>
    ))}
    <Captions cues={CAPTIONS} />
    <Soundtrack music="nani/music.wav" lines={VO} sfx={SFX} />
  </AbsoluteFill>
);
