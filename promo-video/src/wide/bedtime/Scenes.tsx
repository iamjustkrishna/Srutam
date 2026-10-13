// "The 2 a.m. Idea", restaged for 1920 x 1080. Same art, copy and beats as the
// vertical scenes (src/bedtime/Scenes.tsx); every element re-laid out across
// the width: Meera on one side, the bulb / thought bubble / phone on the other.
import { useId } from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut, rand, springy } from "../../anim";
import { Bulb, Kulhad, ThoughtBubble, Zzz } from "../../bedtime/Props";
import { LockScreen, MorningScreen, type LockState } from "../../bedtime/Screens";
import { BEAT, SCENES } from "../../bedtime/timeline";
import { Hand, PhoneUI } from "../../painted/Figures";
import { MeeraSketch, type SketchExpr } from "../../sketch/MeeraSketch";
import { LOGO } from "../../spirit/logoData";
import { LH, LW } from "../kit";
import { AlarmClock, CafeTable, LBedroom, LQuilt, LRooftop } from "./Props";

/** The landscape stage: a 1920 x 1080 SVG with paper grain, zoomed about `focus`. */
const LStage: React.FC<{
  children: React.ReactNode;
  zoom?: number;
  focus?: [number, number];
  /** Rendered on top, outside the zoom (titles, HUD). */
  overlay?: React.ReactNode;
}> = ({ children, zoom = 1, focus = [LW / 2, LH / 2], overlay }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      <svg
        viewBox={`0 0 ${LW} ${LH}`}
        width={LW}
        height={LH}
        style={{ transformOrigin: `${focus[0]}px ${focus[1]}px`, scale: `${zoom}` }}
      >
        <defs>
          <filter id={`grain${id}`}>
            <feTurbulence type="fractalNoise" baseFrequency={0.85} numOctaves={2} seed={9} />
            <feColorMatrix type="saturate" values="0" />
          </filter>
        </defs>
        {children}
        <rect width={LW} height={LH} filter={`url(#grain${id})`} opacity={0.07} style={{ mixBlendMode: "overlay" }} />
      </svg>
      {overlay && (
        <svg viewBox={`0 0 ${LW} ${LH}`} width={LW} height={LH} style={{ position: "absolute", inset: 0 }}>
          {overlay}
        </svg>
      )}
    </AbsoluteFill>
  );
};

/** Blink for 4 frames roughly every 2.7 s (offset per scene). */
const blinking = (f: number, seed = 0) => (f + seed) % 82 > 77;

/** Night tint over the characters (moonlit blue). */
const NightTint: React.FC<{ amount?: number }> = ({ amount = 0.45 }) => (
  <rect width={LW} height={LH} fill="#1B2350" opacity={amount} style={{ mixBlendMode: "multiply" }} />
);

/** Meera's scale in the bedroom, and where things sit. */
const MS = 1.05;
const SOLO = 960; // bed / head centre when she has the frame to herself
const DUO = 600; // bed / head centre when the phone shares the frame
const CLOCK: [number, number] = [SOLO - 880 + 120, 430 + 150 - 6]; // on the nightstand

const LyingMeera: React.FC<{
  f: number;
  expr: SketchExpr;
  cx: number;
  y: number;
  gaze?: [number, number];
  talk?: number;
}> = ({ f, expr, cx, y, gaze = [0, 0], talk = 0 }) => (
  <g transform={`translate(0 ${Math.sin(f / 22) * 3})`}>
    <MeeraSketch
      x={cx - 300 * MS}
      y={y}
      s={MS}
      expr={expr}
      gaze={gaze}
      talk={talk}
      blink={expr !== "sleep" && blinking(f)}
      sway={Math.sin(f / 30) * 0.6}
    />
  </g>
);

const SittingMeera: React.FC<{
  f: number;
  expr: SketchExpr;
  cx: number;
  y: number;
  gaze?: [number, number];
  seed?: number;
}> = ({ f, expr, cx, y, gaze = [0, 0], seed = 0 }) => (
  <g transform={`translate(0 ${Math.sin(f / 26) * 3})`}>
    <MeeraSketch
      x={cx - 300 * MS}
      y={y}
      s={MS}
      expr={expr}
      gaze={gaze}
      blink={!["sleep", "laugh"].includes(expr) && blinking(f, seed)}
      sway={Math.sin(f / 30) * 0.6}
    />
  </g>
);

// Night 1 / Remember: lying, head centred; the bulb up to her right.
const N1 = { y: 148, quilt: 745, bulb: [1330, 290] as [number, number] };

/* 1. 2:13 a.m. Eyes snap open: the best idea. */
export const Night1: React.FC = () => {
  const f = useCurrentFrame();
  const open = f >= BEAT.eyesOpen;
  const bulbIn = interpolate(f, [BEAT.bulbOn - 4, BEAT.bulbOn + 10], [0, 1], { ...clamp, easing: springy });
  return (
    <LStage
      zoom={interpolate(f, [0, SCENES.night1.duration], [1.0, 1.06], clamp)}
      focus={[SOLO, 470]}
      overlay={
        <>
          <text
            x={100}
            y={250}
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
            x={108}
            y={312}
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
      <LBedroom morning={0} bx={SOLO} />
      <LyingMeera f={f} cx={SOLO} y={N1.y} expr={open ? "surprised" : "sleep"} gaze={[0, -0.7]} />
      <LQuilt morning={0} bx={SOLO} y={N1.quilt} breathe={Math.sin(f / 22)} />
      <NightTint />
      <AlarmClock x={CLOCK[0]} y={CLOCK[1]} text="2:13" night />
      <g opacity={interpolate(f, [BEAT.eyesOpen, BEAT.eyesOpen + 4, 40, 48], [0, 1, 1, 0], clamp)}>
        <text x={SOLO + 205} y={390} fontFamily="Kalam" fontWeight={700} fontSize={110} fill="#FDE68A">
          !
        </text>
      </g>
      <Bulb x={N1.bulb[0]} y={N1.bulb[1]} s={bulbIn * 1.15} glow={bulbIn} />
    </LStage>
  );
};

/* 2. "I'll remember it in the morning." Back to sleep. */
export const Remember: React.FC = () => {
  const f = useCurrentFrame();
  const asleep = f >= BEAT.sleep - SCENES.remember.from;
  const bubble = interpolate(f, [2, 10, 50, 58], [0, 1, 1, 0], clamp);
  const dawn = interpolate(f, [70, 90], [0, 1], clamp);
  return (
    <LStage zoom={1.06} focus={[SOLO, 470]}>
      <LBedroom morning={0} bx={SOLO} />
      <LyingMeera f={f + 165} cx={SOLO} y={N1.y} expr={asleep ? "sleep" : "smug"} gaze={[-0.4, -0.2]} />
      <LQuilt morning={0} bx={SOLO} y={N1.quilt} breathe={Math.sin(f / 22)} />
      <NightTint />
      <AlarmClock x={CLOCK[0]} y={CLOCK[1]} text="2:14" night />
      <Bulb x={N1.bulb[0]} y={N1.bulb[1]} s={1.15} glow={interpolate(f, [40, 90], [1, 0.25], clamp)} />
      <ThoughtBubble
        x={190}
        y={140}
        w={500}
        lines={["I'll remember it", "in the morning."]}
        size={52}
        tail={[820, 380]}
        opacity={bubble}
      />
      {asleep && <Zzz x={SOLO + 170} y={410} t={f} />}
      <rect width={LW} height={LH} fill="#FFF4D6" opacity={dawn} />
    </LStage>
  );
};

// Morning 1: sitting up, head centred.
const M1 = { y: 171, quilt: 900 };

/* 3. Morning. Alarm. "...I did not remember it in the morning." */
export const Morning1: React.FC = () => {
  const f = useCurrentFrame();
  const flash = interpolate(f, [0, 10], [1, 0], clamp);
  const pop = interpolate(f, [BEAT.bulbPop - SCENES.morning1.from, BEAT.bulbPop - SCENES.morning1.from + 14], [0, 1], clamp);
  const buzz = f >= 3 && f < 34 ? Math.sin(f * 2.4) * 6 : 0;
  return (
    <LStage zoom={interpolate(f, [0, 90], [1.0, 1.05], clamp)} focus={[SOLO, 470]}>
      <LBedroom morning={1} bx={SOLO} />
      <SittingMeera f={f} cx={SOLO} y={M1.y} expr="blank" />
      <LQuilt morning={1} bx={SOLO} y={M1.quilt} />
      <AlarmClock x={CLOCK[0]} y={CLOCK[1]} text="7:30" buzz={buzz} ringing={f < 34} />
      <Bulb x={1320} y={270} s={1.0} glow={0.2} pop={pop} />
      <ThoughtBubble
        x={1230}
        y={120}
        w={340}
        lines={["?"]}
        size={110}
        tail={[1120, 450]}
        opacity={interpolate(f, [44, 52], [0, 1], clamp)}
      />
      <rect width={LW} height={LH} fill="#FFFFFF" opacity={flash} />
    </LStage>
  );
};

/* 4. VHS rewind back to 2:13 a.m. */
export const Rewind: React.FC = () => {
  const f = useCurrentFrame();
  const k = f / SCENES.rewind.duration;
  const night = k > 0.45;
  const jx = (rand(Math.floor(f / 2)) - 0.5) * 24;
  const mins = Math.round(interpolate(k, [0, 0.9], [7 * 60 + 30, 2 * 60 + 13], clamp));
  const clockText = `${Math.floor(mins / 60)}:${String(mins % 60).padStart(2, "0")}`;
  const expr: SketchExpr = k < 0.3 ? "blank" : k < 0.65 ? "sleep" : "surprised";
  return (
    <LStage zoom={1.03} focus={[SOLO, 470]}>
      <g transform={`translate(${jx} 0)`}>
        <LBedroom morning={night ? 0 : 1} bx={SOLO} />
        {night ? (
          <LyingMeera f={300 - f * 4} cx={SOLO} y={N1.y} expr={expr} gaze={[0, -0.7]} />
        ) : (
          <SittingMeera f={300 - f * 4} cx={SOLO} y={M1.y} expr={expr} />
        )}
        <LQuilt morning={night ? 0 : 1} bx={SOLO} y={night ? N1.quilt : M1.quilt} />
        {night && <NightTint />}
        <AlarmClock x={CLOCK[0]} y={CLOCK[1]} text={clockText} night={night} />
      </g>
      {/* RGB split, scanlines, tracking bar */}
      <rect width={LW} height={LH} fill="#FF00AA" opacity={0.08} style={{ mixBlendMode: "screen" }} transform={`translate(${8 + jx} 0)`} />
      <rect width={LW} height={LH} fill="#00E5FF" opacity={0.08} style={{ mixBlendMode: "screen" }} transform={`translate(${-8 - jx} 0)`} />
      {Array.from({ length: Math.ceil(LH / 20) }, (_, i) => (
        <rect key={i} x={-100} y={i * 20} width={LW + 200} height={4} fill="#000000" opacity={0.18} />
      ))}
      <rect x={-100} y={((f * 23) % (LH + 200)) - 100} width={LW + 200} height={60} fill="#FFFFFF" opacity={0.18} />
      <g
        fontFamily="Inter"
        fontWeight={800}
        fill="#FFFFFF"
        stroke="#0B0718"
        strokeWidth={8}
        paintOrder="stroke"
        style={{ filter: "drop-shadow(0 0 6px rgba(255,255,255,0.7))" }}
      >
        <text x={110} y={150} fontSize={76} opacity={f % 16 < 11 ? 1 : 0.2}>
          ◀◀ REWIND
        </text>
        <text x={LW - 110} y={150} textAnchor="end" fontSize={76}>
          {clockText}
        </text>
      </g>
    </LStage>
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

// Night 2 / Save: Meera lying on the left, the phone held up on the right.
const N2 = { y: 100, quilt: 700 };
const PHONE = { x: 1220, w: 540, y: 150, down: 1140 };
/** Where the vertical's phone-local points land: the vertical phone is 620 wide. */
const pk = PHONE.w / 620;
/** Hands at the phone's lower edge (thumb left, fingers right). */
const PhoneHand: React.FC<{ x: number; y: number; w: number }> = ({ x, y, w }) => {
  const k = (w + 80) / 400;
  return <Hand x={x - 40} y={y + w * 2.22 * 0.62 - 180 * k} w={w + 80} />;
};

/** Soft light from the phone screen, and the idea landing in it. */
const Lights: React.FC = () => (
  <defs>
    <radialGradient id="lPhoneLight">
      <stop offset="0%" stopColor="#7DD3FC" stopOpacity={0.28} />
      <stop offset="100%" stopColor="#7DD3FC" stopOpacity={0} />
    </radialGradient>
    <radialGradient id="lIdeaLight">
      <stop offset="0%" stopColor="#FDE68A" stopOpacity={0.7} />
      <stop offset="100%" stopColor="#FBBF24" stopOpacity={0} />
    </radialGradient>
  </defs>
);

export const Night2: React.FC = () => {
  const f = useCurrentFrame();
  const abs = f + SCENES.night2.from;
  const phoneY = interpolate(abs, [BEAT.grabPhone - 10, BEAT.grabPhone + 18], [PHONE.down, PHONE.y], { ...clamp, easing: easeOut });
  const state: LockState = abs < BEAT.tapStart ? "idle" : "recording";
  const vo = (abs - 462) / 30;
  const talking = SPEAKING.some(([a, b]) => vo >= a && vo <= b);
  const talk = talking ? 0.4 + 0.6 * Math.abs(Math.sin(abs * 0.8)) : 0;
  const bulbT = interpolate(abs, [BEAT.bulbIntoPhone, BEAT.bulbIntoPhone + 28], [0, 1], { ...clamp, easing: easeInOut });
  const glow = interpolate(abs, [BEAT.bulbIntoPhone + 24, BEAT.bulbIntoPhone + 34, BEAT.bulbIntoPhone + 70], [0, 1, 0.25], clamp);
  const target: [number, number] = [PHONE.x + PHONE.w / 2, phoneY + 400 * pk];
  const from: [number, number] = [DUO + 400, 270];
  return (
    <LStage>
      <Lights />
      <LBedroom morning={0} bx={DUO} wx={110} />
      <LyingMeera
        f={abs}
        cx={DUO}
        y={N2.y}
        expr={abs < BEAT.grabPhone + 20 ? "surprised" : "drowsy"}
        gaze={abs < BEAT.grabPhone + 20 ? [0, -0.7] : [0.9, 0.4]}
        talk={talk}
      />
      <LQuilt morning={0} bx={DUO} y={N2.quilt} breathe={Math.sin(abs / 22)} />
      <NightTint />
      {bulbT < 1 && (
        <Bulb
          x={from[0] + (target[0] - from[0]) * bulbT}
          y={from[1] + (target[1] - from[1]) * bulbT - Math.sin(bulbT * Math.PI) * 120}
          s={1.1 * (1 - 0.7 * bulbT)}
          glow={1}
        />
      )}
      {/* Phone glow on her face */}
      <ellipse cx={DUO + 120} cy={440} rx={460} ry={320} fill="url(#lPhoneLight)" opacity={phoneY < 700 ? 1 : 0} />
      <PhoneUI x={PHONE.x} y={phoneY} w={PHONE.w} dark>
        <LockScreen
          w={PHONE.w}
          state={state}
          seconds={Math.max(0, (abs - BEAT.tapStart) / 30)}
          level={talking ? 0.9 : 0.15}
          pressed={abs >= BEAT.tapStart - 4 && abs < BEAT.tapStart + 4 ? "start" : null}
        />
      </PhoneUI>
      <circle cx={target[0]} cy={target[1]} r={320} fill="url(#lIdeaLight)" opacity={glow} />
      <PhoneHand x={PHONE.x} y={phoneY} w={PHONE.w} />
      {/* Tap ripple on Start */}
      {abs >= BEAT.tapStart && abs < BEAT.tapStart + 14 && (
        <circle
          cx={PHONE.x + 540 * pk}
          cy={phoneY + 395 * pk}
          r={20 + (abs - BEAT.tapStart) * 6}
          fill="none"
          stroke="#FFFFFF"
          strokeWidth={6}
          opacity={1 - (abs - BEAT.tapStart) / 14}
        />
      )}
      {/* Her whispered words drift across into the phone */}
      {WHISPER.map((w, i) => {
        const t = (vo - w.at) / 1.6;
        if (t < 0 || t > 1) return null;
        return (
          <text
            key={i}
            x={900 + t * 330}
            y={(i % 2 ? 600 : 520) - t * 60 - Math.sin(t * Math.PI) * 40}
            textAnchor="middle"
            fontFamily="Kalam"
            fontWeight={700}
            fontSize={54}
            fill="#FDE68A"
            opacity={Math.sin(t * Math.PI)}
            style={{ filter: "drop-shadow(0 0 10px rgba(251,191,36,0.9))" }}
          >
            {w.text}
          </text>
        );
      })}
    </LStage>
  );
};

/* 6. Save. "Tap. Say it. Back to sleep." */
export const Save: React.FC = () => {
  const f = useCurrentFrame();
  const abs = f + SCENES.save.from;
  const state: LockState = abs < BEAT.tapSave ? "recording" : abs < BEAT.saved ? "saving" : "saved";
  const phoneY = interpolate(abs, [BEAT.backToSleep, BEAT.backToSleep + 22], [PHONE.y, PHONE.down], { ...clamp, easing: easeInOut });
  const asleep = abs >= BEAT.backToSleep;
  const dawn = interpolate(f, [86, 100], [0, 1], clamp);
  return (
    <LStage>
      <Lights />
      <LBedroom morning={0} bx={DUO} wx={110} />
      <LyingMeera f={abs} cx={DUO} y={N2.y} expr={asleep ? "sleep" : "drowsy"} gaze={[0.9, 0.4]} />
      <LQuilt morning={0} bx={DUO} y={N2.quilt} breathe={Math.sin(abs / 22)} />
      <NightTint />
      <ellipse cx={DUO + 120} cy={440} rx={460} ry={320} fill="url(#lPhoneLight)" opacity={phoneY < 700 ? 1 : 0} />
      <PhoneUI x={PHONE.x} y={phoneY} w={PHONE.w} dark>
        <LockScreen
          w={PHONE.w}
          state={state}
          seconds={8}
          level={0.1}
          pressed={abs >= BEAT.tapSave - 4 && abs < BEAT.tapSave + 2 ? "save" : null}
        />
      </PhoneUI>
      <PhoneHand x={PHONE.x} y={phoneY} w={PHONE.w} />
      {abs >= BEAT.tapSave && abs < BEAT.tapSave + 14 && (
        <circle
          cx={PHONE.x + 560 * pk}
          cy={phoneY + 395 * pk}
          r={20 + (abs - BEAT.tapSave) * 6}
          fill="none"
          stroke="#FFFFFF"
          strokeWidth={6}
          opacity={1 - (abs - BEAT.tapSave) / 14}
        />
      )}
      {asleep && <Zzz x={DUO + 170} y={360} t={f} />}
      <rect width={LW} height={LH} fill="#FFF4D6" opacity={dawn} />
    </LStage>
  );
};

/* 7. Morning: it's all there. */
const M2 = { y: 150, quilt: 880 };
const PHONE2 = { x: 1210, w: 560, y: 150 };

export const Morning2: React.FC = () => {
  const f = useCurrentFrame();
  const abs = f + SCENES.morning2.from;
  const show = (beat: number) => interpolate(abs, [beat - 6, beat + 8], [0, 1], { ...clamp, easing: easeOut });
  const phoneY = interpolate(f, [0, 30], [PHONE.down, PHONE2.y], { ...clamp, easing: easeOut });
  const expr: SketchExpr = abs < BEAT.noteIn ? "blank" : abs < BEAT.ideaCard ? "surprised" : "laugh";
  return (
    <LStage>
      <LBedroom morning={1} bx={DUO} wx={110} />
      {[0, 1, 2].map((i) => (
        <polygon
          key={i}
          points={`${240 + i * 90},90 ${300 + i * 90},90 ${980 + i * 150},${LH} ${820 + i * 150},${LH}`}
          fill="#FFF4D6"
          opacity={0.12}
        />
      ))}
      <SittingMeera f={abs} cx={DUO} y={M2.y} expr={expr} gaze={[0.9, 0.3]} seed={30} />
      <LQuilt morning={1} bx={DUO} y={M2.quilt} />
      <PhoneUI x={PHONE2.x} y={phoneY} w={PHONE2.w}>
        <MorningScreen
          w={PHONE2.w}
          note={show(BEAT.noteIn)}
          idea={show(BEAT.ideaCard)}
          step={show(BEAT.stepCard)}
          reminder={show(BEAT.reminderCard)}
        />
      </PhoneUI>
      <PhoneHand x={PHONE2.x} y={phoneY + 60} w={PHONE2.w} />
      <rect width={LW} height={LH} fill="#FFF4D6" opacity={interpolate(f, [0, 12], [1, 0], clamp)} />
    </LStage>
  );
};

/* 8. Three months later: Chai & Chapters. */
export const Later: React.FC = () => {
  const f = useCurrentFrame();
  const cx = 860;
  return (
    <LStage
      zoom={interpolate(f, [0, 85], [1.06, 1.0], { ...clamp, easing: easeOut })}
      focus={[cx, 640]}
      overlay={
        <text
          x={LW / 2}
          y={150}
          textAnchor="middle"
          fontFamily="Kalam"
          fontWeight={700}
          fontSize={100}
          fill="#FFFFFF"
          opacity={interpolate(f, [4, 14], [0, 1], clamp)}
          style={{ filter: "drop-shadow(0 4px 10px rgba(30,10,40,0.6))" }}
        >
          3 months later...
        </text>
      }
    >
      <LRooftop t={f} />
      <MeeraSketch
        x={cx - 300 * MS}
        y={250}
        s={MS}
        expr={f < 40 ? "smug" : "laugh"}
        turn={0.2}
        gaze={[0.5, 0]}
        blink={f > 20 && f < 24}
        sway={Math.sin(f / 18) * 0.9}
      />
      <CafeTable x0={520} x1={1300} y={968} />
      <Kulhad x={1120} y={968 - 58 * 1.15} s={1.15} t={f} />
    </LStage>
  );
};

/* 9. Brand. */
export const End: React.FC = () => {
  const f = useCurrentFrame();
  const pop = (a: number) => interpolate(f, [a, a + 12], [0, 1], { ...clamp, easing: springy });
  const bx = 600;
  const tx = 1270;
  return (
    <LStage>
      <rect width={LW} height={LH} fill="#F4EBDD" />
      <circle cx={bx} cy={540} r={400} fill="#FDE68A" opacity={0.35} />
      <Bulb x={bx} y={520} s={1.6 * pop(2)} glow={1} />
      <image href={LOGO} x={tx - 85} y={200} width={170} height={170} opacity={pop(10)} />
      <text x={tx} y={575} textAnchor="middle" fontFamily="Playfair Display" fontSize={180} fill="#1A1433" opacity={pop(16)}>
        Srutam
      </text>
      <text x={tx} y={672} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={66} fill="#2563EB" opacity={pop(26)}>
        Catch it before it's gone.
      </text>
      <g opacity={pop(40)}>
        <rect x={tx - 270} y={750} width={540} height={114} rx={57} fill="#1A1433" />
        <text x={tx} y={822} textAnchor="middle" fontFamily="Inter" fontWeight={800} fontSize={44} fill="#FFFFFF">
          Available on Android
        </text>
      </g>
    </LStage>
  );
};
