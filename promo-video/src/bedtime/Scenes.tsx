// The nine scenes of "The 2 a.m. Idea". Each renders a 1080x1920 SVG and
// animates from useCurrentFrame() (frame 0 = the start of its Sequence).
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut, rand, springy } from "../anim";
import { Hand, PhoneUI } from "../painted/Figures";
import { MeeraSketch, type SketchExpr } from "../sketch/MeeraSketch";
import { LOGO } from "../spirit/logoData";
import {
  Bedroom,
  Bulb,
  Kulhad,
  Quilt,
  Rooftop,
  ThoughtBubble,
  Zzz,
} from "./Props";
import { LockScreen, MorningScreen, type LockState } from "./Screens";
import { BEAT, H, SCENES, W } from "./timeline";

const Stage: React.FC<{
  children: React.ReactNode;
  zoom?: number;
  focus?: [number, number];
  /** Rendered on top, outside the zoom (titles, HUD). */
  overlay?: React.ReactNode;
}> = ({ children, zoom = 1, focus = [540, 800], overlay }) => (
  <AbsoluteFill style={{ overflow: "hidden" }}>
    <svg
      viewBox={`0 0 ${W} ${H}`}
      width={W}
      height={H}
      style={{
        transformOrigin: `${focus[0]}px ${focus[1]}px`,
        scale: `${zoom}`,
      }}
    >
      <defs>
        <filter id="paperGrain">
          <feTurbulence
            type="fractalNoise"
            baseFrequency={0.85}
            numOctaves={2}
            seed={9}
          />
          <feColorMatrix type="saturate" values="0" />
        </filter>
      </defs>
      {children}
      <rect
        width={W}
        height={H}
        filter="url(#paperGrain)"
        opacity={0.07}
        style={{ mixBlendMode: "overlay" }}
      />
    </svg>
    {overlay && (
      <svg
        viewBox={`0 0 ${W} ${H}`}
        width={W}
        height={H}
        style={{ position: "absolute", inset: 0 }}
      >
        {overlay}
      </svg>
    )}
  </AbsoluteFill>
);

/** Blink for 4 frames roughly every 2.7 s (offset per scene). */
const blinking = (f: number, seed = 0) => (f + seed) % 82 > 77;

/** Night tint over the characters (moonlit blue). */
const NightTint: React.FC<{ amount?: number }> = ({ amount = 0.45 }) => (
  <rect
    width={W}
    height={H}
    fill="#1B2350"
    opacity={amount}
    style={{ mixBlendMode: "multiply" }}
  />
);

const LyingMeera: React.FC<{
  f: number;
  expr: SketchExpr;
  gaze?: [number, number];
  talk?: number;
  s?: number;
  y?: number;
}> = ({ f, expr, gaze = [0, 0], talk = 0, s = 1.45, y = 268 }) => (
  <g transform={`translate(0 ${Math.sin(f / 22) * 3})`}>
    <MeeraSketch
      x={540 - 300 * s}
      y={y}
      s={s}
      expr={expr}
      gaze={gaze}
      talk={talk}
      blink={expr !== "sleep" && blinking(f)}
      sway={Math.sin(f / 30) * 0.6}
    />
  </g>
);

/* 1. 2:13 a.m. Eyes snap open: the best idea. */
export const Night1: React.FC = () => {
  const f = useCurrentFrame();
  const open = f >= BEAT.eyesOpen;
  const bulbIn = interpolate(f, [BEAT.bulbOn - 4, BEAT.bulbOn + 10], [0, 1], {
    ...clamp,
    easing: springy,
  });
  return (
    <Stage
      zoom={interpolate(f, [0, SCENES.night1.duration], [1.18, 1.28], clamp)}
      focus={[540, 640]}
      overlay={
        <>
          <text
            x={90}
            y={240}
            fontFamily="Inter"
            fontWeight={200}
            fontSize={150}
            fill="#E9E4FF"
            opacity={interpolate(f, [14, 24], [0, 1], clamp)}
            style={{ filter: "drop-shadow(0 0 16px rgba(167,139,250,0.8))" }}
          >
            2:13
          </text>
          <text
            x={98}
            y={300}
            fontFamily="Inter"
            fontWeight={600}
            fontSize={44}
            letterSpacing={8}
            fill="#C4B5FD"
            opacity={interpolate(f, [18, 28], [0, 1], clamp)}
          >
            A.M.
          </text>
        </>
      }
    >
      <Bedroom morning={0} />
      <LyingMeera f={f} expr={open ? "surprised" : "sleep"} gaze={[0, -0.7]} />
      <Quilt morning={0} breathe={Math.sin(f / 22)} />
      <NightTint />
      <g
        opacity={interpolate(
          f,
          [BEAT.eyesOpen, BEAT.eyesOpen + 4, 40, 48],
          [0, 1, 1, 0],
          clamp,
        )}
      >
        <text
          x={830}
          y={560}
          fontFamily="Kalam"
          fontWeight={700}
          fontSize={130}
          fill="#FDE68A"
        >
          !
        </text>
      </g>
      <Bulb x={540} y={330} s={bulbIn * 1.1} glow={bulbIn} />
    </Stage>
  );
};

/* 2. "I'll remember it in the morning." Back to sleep. */
export const Remember: React.FC = () => {
  const f = useCurrentFrame();
  const asleep = f >= BEAT.sleep - SCENES.remember.from;
  const bubble = interpolate(f, [2, 10, 50, 58], [0, 1, 1, 0], clamp);
  const dawn = interpolate(f, [70, 90], [0, 1], clamp);
  return (
    <Stage zoom={1.25} focus={[540, 640]}>
      <Bedroom morning={0} />
      <LyingMeera
        f={f + 165}
        expr={asleep ? "sleep" : "smug"}
        gaze={[0.4, -0.2]}
      />
      <Quilt morning={0} breathe={Math.sin(f / 22)} />
      <NightTint />
      <Bulb
        x={540}
        y={330}
        s={1.1}
        glow={interpolate(f, [40, 90], [1, 0.25], clamp)}
      />
      <ThoughtBubble
        x={170}
        y={210}
        w={560}
        lines={["I'll remember it", "in the morning."]}
        size={54}
        tail={[500, 600]}
        opacity={bubble}
      />
      {asleep && <Zzz x={760} y={620} t={f} />}
      <rect width={W} height={H} fill="#FFF4D6" opacity={dawn} />
    </Stage>
  );
};

/** Meera sitting up in bed. */
const SittingMeera: React.FC<{
  f: number;
  expr: SketchExpr;
  gaze?: [number, number];
  seed?: number;
}> = ({ f, expr, gaze = [0, 0], seed = 0 }) => (
  <g transform={`translate(0 ${Math.sin(f / 26) * 3})`}>
    <MeeraSketch
      x={540 - 300 * 1.45}
      y={300}
      s={1.45}
      expr={expr}
      gaze={gaze}
      blink={!["sleep", "laugh"].includes(expr) && blinking(f, seed)}
      sway={Math.sin(f / 30) * 0.6}
    />
  </g>
);

/* 3. Morning. Alarm. "...I did not remember it in the morning." */
export const Morning1: React.FC = () => {
  const f = useCurrentFrame();
  const flash = interpolate(f, [0, 10], [1, 0], clamp);
  const pop = interpolate(
    f,
    [
      BEAT.bulbPop - SCENES.morning1.from,
      BEAT.bulbPop - SCENES.morning1.from + 14,
    ],
    [0, 1],
    clamp,
  );
  const buzz = f >= 3 && f < 34 ? Math.sin(f * 2.4) * 6 : 0;
  return (
    <Stage
      zoom={interpolate(f, [0, 90], [1.0, 1.06], clamp)}
      focus={[540, 700]}
    >
      <Bedroom morning={1} />
      <SittingMeera f={f} expr="blank" />
      <Quilt morning={1} y={1300} />
      {/* Alarm on the nightstand */}
      <g transform={`translate(${30 + buzz} 700) rotate(${buzz})`}>
        <rect
          x={0}
          y={0}
          width={80}
          height={150}
          rx={14}
          fill="#0F172A"
          stroke="#4A2E28"
          strokeWidth={4}
        />
        <text
          x={40}
          y={92}
          textAnchor="middle"
          fontFamily="Inter"
          fontWeight={700}
          fontSize={30}
          fill="#FDE68A"
        >
          7:30
        </text>
        {f < 34 &&
          [0, 1, 2].map((i) => (
            <path
              key={i}
              d={`M${96 + i * 14} ${40} q12 30 0 60`}
              stroke="#4A2E28"
              strokeWidth={5}
              fill="none"
              strokeLinecap="round"
            />
          ))}
      </g>
      <Bulb x={540} y={250} s={1.0} glow={0.2} pop={pop} />
      <ThoughtBubble
        x={330}
        y={90}
        w={420}
        lines={["?"]}
        size={110}
        tail={[520, 420]}
        opacity={interpolate(f, [44, 52], [0, 1], clamp)}
      />
      <rect width={W} height={H} fill="#FFFFFF" opacity={flash} />
    </Stage>
  );
};

/* 4. VHS rewind back to 2:13 a.m. */
export const Rewind: React.FC = () => {
  const f = useCurrentFrame();
  const k = f / SCENES.rewind.duration;
  const night = k > 0.45;
  const jx = (rand(Math.floor(f / 2)) - 0.5) * 24;
  const mins = Math.round(
    interpolate(k, [0, 0.9], [7 * 60 + 30, 2 * 60 + 13], clamp),
  );
  const clockText = `${Math.floor(mins / 60)}:${String(mins % 60).padStart(2, "0")}`;
  const expr: SketchExpr = k < 0.3 ? "blank" : k < 0.65 ? "sleep" : "surprised";
  return (
    <Stage zoom={1.04}>
      <g transform={`translate(${jx} 0)`}>
        <Bedroom morning={night ? 0 : 1} />
        {night ? (
          <LyingMeera f={300 - f * 4} expr={expr} gaze={[0, -0.7]} />
        ) : (
          <SittingMeera f={300 - f * 4} expr={expr} />
        )}
        <Quilt morning={night ? 0 : 1} y={night ? 1060 : 1300} />
        {night && <NightTint />}
      </g>
      {/* RGB split, scanlines, tracking bar */}
      <rect
        width={W}
        height={H}
        fill="#FF00AA"
        opacity={0.08}
        style={{ mixBlendMode: "screen" }}
        transform={`translate(${8 + jx} 0)`}
      />
      <rect
        width={W}
        height={H}
        fill="#00E5FF"
        opacity={0.08}
        style={{ mixBlendMode: "screen" }}
        transform={`translate(${-8 - jx} 0)`}
      />
      {Array.from({ length: 96 }, (_, i) => (
        <rect
          key={i}
          x={0}
          y={i * 20}
          width={W}
          height={4}
          fill="#000000"
          opacity={0.18}
        />
      ))}
      <rect
        x={0}
        y={((f * 37) % (H + 200)) - 100}
        width={W}
        height={70}
        fill="#FFFFFF"
        opacity={0.18}
      />
      <g
        fontFamily="Inter"
        fontWeight={800}
        fill="#FFFFFF"
        stroke="#0B0718"
        strokeWidth={8}
        paintOrder="stroke"
        style={{ filter: "drop-shadow(0 0 6px rgba(255,255,255,0.7))" }}
      >
        <text x={70} y={170} fontSize={70} opacity={f % 16 < 11 ? 1 : 0.2}>
          ◀◀ REWIND
        </text>
        <text x={1010} y={170} textAnchor="end" fontSize={70}>
          {clockText}
        </text>
      </g>
    </Stage>
  );
};

/* 5. 2:13 a.m., take two: grab the phone, tap Start, whisper it. */
const WHISPER = [
  { text: "Rooftop café", at: 0.08 },
  { text: "for book lovers", at: 1.62 },
  { text: "swap a book,", at: 3.08 },
  { text: "get a chai", at: 4.08 },
  { text: "Call Priya", at: 5.24 },
  { text: "about her terrace", at: 5.92 },
  { text: "Saturday", at: 7.48 },
];
const SPEAKING: [number, number][] = [
  [0.08, 2.5],
  [3.08, 4.8],
  [5.24, 6.9],
  [7.48, 8.3],
];

const PHONE = { x: 230, w: 620 };

/** Soft light from the phone screen, and the idea landing in it. */
const Lights: React.FC = () => (
  <defs>
    <radialGradient id="phoneLight">
      <stop offset="0%" stopColor="#7DD3FC" stopOpacity={0.28} />
      <stop offset="100%" stopColor="#7DD3FC" stopOpacity={0} />
    </radialGradient>
    <radialGradient id="ideaLight">
      <stop offset="0%" stopColor="#FDE68A" stopOpacity={0.7} />
      <stop offset="100%" stopColor="#FBBF24" stopOpacity={0} />
    </radialGradient>
  </defs>
);

export const Night2: React.FC = () => {
  const f = useCurrentFrame();
  const abs = f + SCENES.night2.from;
  const phoneY = interpolate(
    abs,
    [BEAT.grabPhone - 10, BEAT.grabPhone + 18],
    [1960, 1110],
    { ...clamp, easing: easeOut },
  );
  const state: LockState = abs < BEAT.tapStart ? "idle" : "recording";
  const vo = (abs - 462) / 30;
  const talking = SPEAKING.some(([a, b]) => vo >= a && vo <= b);
  const talk = talking ? 0.4 + 0.6 * Math.abs(Math.sin(abs * 0.8)) : 0;
  const bulbT = interpolate(
    abs,
    [BEAT.bulbIntoPhone, BEAT.bulbIntoPhone + 28],
    [0, 1],
    { ...clamp, easing: easeInOut },
  );
  const glow = interpolate(
    abs,
    [BEAT.bulbIntoPhone + 24, BEAT.bulbIntoPhone + 34, BEAT.bulbIntoPhone + 70],
    [0, 1, 0.25],
    clamp,
  );
  return (
    <Stage zoom={1.0}>
      <Lights />
      <Bedroom morning={0} />
      <LyingMeera
        f={abs}
        expr={abs < BEAT.grabPhone + 20 ? "surprised" : "drowsy"}
        gaze={abs < BEAT.grabPhone + 20 ? [0, -0.7] : [0, 0.8]}
        talk={talk}
        y={180}
      />
      <Quilt morning={0} y={980} breathe={Math.sin(abs / 22)} />
      <NightTint />
      {bulbT < 1 && (
        <Bulb
          x={540 + (540 - 540) * bulbT}
          y={240 + (phoneY + 420 - 240) * bulbT}
          s={1.1 * (1 - 0.7 * bulbT)}
          glow={1}
        />
      )}
      {/* Phone glow on her face */}
      <ellipse
        cx={540}
        cy={760}
        rx={420}
        ry={300}
        fill="url(#phoneLight)"
        opacity={phoneY < 1500 ? 1 : 0}
      />
      <PhoneUI x={PHONE.x} y={phoneY} w={PHONE.w} dark>
        <LockScreen
          w={PHONE.w}
          state={state}
          seconds={Math.max(0, (abs - BEAT.tapStart) / 30)}
          level={talking ? 0.9 : 0.15}
          pressed={
            abs >= BEAT.tapStart - 4 && abs < BEAT.tapStart + 4 ? "start" : null
          }
        />
      </PhoneUI>
      <circle
        cx={540}
        cy={phoneY + 420}
        r={320}
        fill="url(#ideaLight)"
        opacity={glow}
      />
      <Hand x={PHONE.x - 40} y={phoneY + 760} w={PHONE.w + 80} />
      {/* Tap ripple on Start */}
      {abs >= BEAT.tapStart && abs < BEAT.tapStart + 14 && (
        <circle
          cx={PHONE.x + 540}
          cy={phoneY + 395}
          r={20 + (abs - BEAT.tapStart) * 6}
          fill="none"
          stroke="#FFFFFF"
          strokeWidth={6}
          opacity={1 - (abs - BEAT.tapStart) / 14}
        />
      )}
      {/* Her whispered words drift down into the phone */}
      {WHISPER.map((w, i) => {
        const t = (vo - w.at) / 1.6;
        if (t < 0 || t > 1) return null;
        return (
          <text
            key={i}
            x={540 + (i % 2 ? 90 : -90)}
            y={720 + t * 260}
            textAnchor="middle"
            fontFamily="Kalam"
            fontWeight={700}
            fontSize={56}
            fill="#FDE68A"
            opacity={Math.sin(t * Math.PI)}
            style={{ filter: "drop-shadow(0 0 10px rgba(251,191,36,0.9))" }}
          >
            {w.text}
          </text>
        );
      })}
    </Stage>
  );
};

/* 6. Save. "Tap. Say it. Back to sleep." */
export const Save: React.FC = () => {
  const f = useCurrentFrame();
  const abs = f + SCENES.save.from;
  const state: LockState =
    abs < BEAT.tapSave ? "recording" : abs < BEAT.saved ? "saving" : "saved";
  const phoneY = interpolate(
    abs,
    [BEAT.backToSleep, BEAT.backToSleep + 22],
    [1110, 1960],
    { ...clamp, easing: easeInOut },
  );
  const asleep = abs >= BEAT.backToSleep;
  const dawn = interpolate(f, [86, 100], [0, 1], clamp);
  return (
    <Stage>
      <Bedroom morning={0} />
      <LyingMeera
        f={abs}
        expr={asleep ? "sleep" : "drowsy"}
        gaze={[0, 0.8]}
        y={180}
      />
      <Quilt morning={0} y={980} breathe={Math.sin(abs / 22)} />
      <NightTint />
      <PhoneUI x={PHONE.x} y={phoneY} w={PHONE.w} dark>
        <LockScreen
          w={PHONE.w}
          state={state}
          seconds={8}
          level={0.1}
          pressed={
            abs >= BEAT.tapSave - 4 && abs < BEAT.tapSave + 2 ? "save" : null
          }
        />
      </PhoneUI>
      <Hand x={PHONE.x - 40} y={phoneY + 760} w={PHONE.w + 80} />
      {abs >= BEAT.tapSave && abs < BEAT.tapSave + 14 && (
        <circle
          cx={PHONE.x + 560}
          cy={phoneY + 395}
          r={20 + (abs - BEAT.tapSave) * 6}
          fill="none"
          stroke="#FFFFFF"
          strokeWidth={6}
          opacity={1 - (abs - BEAT.tapSave) / 14}
        />
      )}
      {asleep && <Zzz x={760} y={520} t={f} />}
      <rect width={W} height={H} fill="#FFF4D6" opacity={dawn} />
    </Stage>
  );
};

/* 7. Morning: it's all there. */
export const Morning2: React.FC = () => {
  const f = useCurrentFrame();
  const abs = f + SCENES.morning2.from;
  const show = (beat: number) =>
    interpolate(abs, [beat - 6, beat + 8], [0, 1], {
      ...clamp,
      easing: easeOut,
    });
  const phoneY = interpolate(f, [0, 30], [1960, 860], {
    ...clamp,
    easing: easeOut,
  });
  const expr: SketchExpr =
    abs < BEAT.noteIn ? "blank" : abs < BEAT.ideaCard ? "surprised" : "laugh";
  return (
    <Stage>
      <Bedroom morning={1} />
      {[0, 1, 2].map((i) => (
        <polygon
          key={i}
          points={`${700 + i * 90},120 ${760 + i * 90},120 ${200 + i * 120},1920 ${60 + i * 120},1920`}
          fill="#FFF4D6"
          opacity={0.12}
        />
      ))}
      <g transform="translate(0 -120)">
        <SittingMeera f={abs} expr={expr} gaze={[0, 0.6]} seed={30} />
      </g>
      <Quilt morning={1} y={1240} />
      <PhoneUI x={PHONE.x} y={phoneY} w={PHONE.w}>
        <MorningScreen
          w={PHONE.w}
          note={show(BEAT.noteIn)}
          idea={show(BEAT.ideaCard)}
          step={show(BEAT.stepCard)}
          reminder={show(BEAT.reminderCard)}
        />
      </PhoneUI>
      <Hand x={PHONE.x - 40} y={phoneY + 760} w={PHONE.w + 80} />
      <rect
        width={W}
        height={H}
        fill="#FFF4D6"
        opacity={interpolate(f, [0, 12], [1, 0], clamp)}
      />
    </Stage>
  );
};

/* 8. Three months later: Chai & Chapters. */
export const Later: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <Stage
      zoom={interpolate(f, [0, 85], [1.08, 1.0], { ...clamp, easing: easeOut })}
      focus={[540, 1000]}
    >
      <Rooftop t={f} />
      <MeeraSketch
        x={20}
        y={700}
        s={1.35}
        expr={f < 40 ? "smug" : "laugh"}
        turn={0.2}
        gaze={[0.5, 0]}
        blink={f > 20 && f < 24}
        sway={Math.sin(f / 18) * 0.9}
      />
      <Kulhad x={560} y={1640} s={1.3} t={f} />
      <text
        x={540}
        y={230}
        textAnchor="middle"
        fontFamily="Kalam"
        fontWeight={700}
        fontSize={96}
        fill="#FFFFFF"
        opacity={interpolate(f, [4, 14], [0, 1], clamp)}
        style={{ filter: "drop-shadow(0 4px 10px rgba(30,10,40,0.6))" }}
      >
        3 months later...
      </text>
    </Stage>
  );
};

/* 9. Brand. */
export const End: React.FC = () => {
  const f = useCurrentFrame();
  const pop = (a: number) =>
    interpolate(f, [a, a + 12], [0, 1], { ...clamp, easing: springy });
  return (
    <Stage>
      <rect width={W} height={H} fill="#F4EBDD" />
      <circle cx={540} cy={640} r={420} fill="#FDE68A" opacity={0.35} />
      <Bulb x={540} y={560} s={1.5 * pop(2)} glow={1} />
      <image
        href={LOGO}
        x={540 - 90}
        y={880}
        width={180}
        height={180}
        opacity={pop(10)}
      />
      <text
        x={540}
        y={1210}
        textAnchor="middle"
        fontFamily="Playfair Display"
        fontSize={170}
        fill="#1A1433"
        opacity={pop(16)}
      >
        Srutam
      </text>
      <text
        x={540}
        y={1310}
        textAnchor="middle"
        fontFamily="Kalam"
        fontWeight={700}
        fontSize={64}
        fill="#2563EB"
        opacity={pop(26)}
      >
        Catch it before it's gone.
      </text>
      <g opacity={pop(40)}>
        <rect
          x={540 - 260}
          y={1420}
          width={520}
          height={110}
          rx={55}
          fill="#1A1433"
        />
        <text
          x={540}
          y={1490}
          textAnchor="middle"
          fontFamily="Inter"
          fontWeight={800}
          fontSize={42}
          fill="#FFFFFF"
        >
          Available on Android
        </text>
      </g>
    </Stage>
  );
};
