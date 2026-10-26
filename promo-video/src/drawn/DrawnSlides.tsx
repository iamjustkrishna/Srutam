// "Who was doing slides 8-12?", AI-illustrated cut. Each story beat is one Canva
// illustration (public/drawn/sNN.jpg, prompts and ids in SHOTS.md) moved with a slow
// camera; banners, phone screens, lettering, voices and sound are layered on top here.
import { AbsoluteFill, Img, interpolate, Sequence, staticFile } from "remotion";
import { clamp, easeInOut, easeOut } from "../anim";
import { springAt } from "../stories/physics";
import { Captions, Soundtrack } from "../stories/Story";
import { K, useT } from "../stories/time";
import { typed } from "../stories/typing";
import { AskFlow, ChatScreen, EndBand, H, Phone, W } from "../storyboards/Kit";
import { BEAT, CAPTIONS, SFX, SHOTS, TOTAL, VO } from "./timeline";

type Span = { from: number; duration: number };
type Move = { s: [number, number]; x: [number, number]; y: [number, number] };

/** Story frame within the whole video (Sequences restart Remotion's clock). */
const useF = (sh: Span) => useT() + sh.from;

/** An illustration filling the frame, pushed and panned across its shot (focus in 0..1). */
const Art: React.FC<{ src: string; sh: Span; move: Move; f: number; shake?: number; filter?: string }> = ({ src, sh, move, f, shake = 0, filter }) => {
  const p = interpolate(f, [sh.from, sh.from + sh.duration], [0, 1], { ...clamp, easing: easeInOut });
  const s = move.s[0] + (move.s[1] - move.s[0]) * p;
  const fx = move.x[0] + (move.x[1] - move.x[0]) * p;
  const fy = move.y[0] + (move.y[1] - move.y[0]) * p;
  // A breath of handheld drift, plus an optional decaying shake on impact.
  const hit = shake * Math.exp(-(f - sh.from) / 6);
  const dx = Math.sin(f / 37) * 4 + Math.sin(f * 2.3) * hit;
  const dy = Math.cos(f / 43) * 4 + Math.cos(f * 2.9) * hit;
  return (
    <AbsoluteFill style={{ overflow: "hidden", background: "#05060C" }}>
      <Img
        src={staticFile(src)}
        style={{
          position: "absolute",
          width: W,
          height: H,
          objectFit: "cover",
          transformOrigin: `${fx * 100}% ${fy * 100}%`,
          transform: `translate(${dx}px, ${dy}px) scale(${s})`,
          filter,
        }}
      />
    </AbsoluteFill>
  );
};

/** Time of night, top centre. */
const Chip: React.FC<{ f: number; sh: Span; text: string }> = ({ f, sh, text }) => {
  const o = interpolate(f, [sh.from + 2, sh.from + 10, sh.from + sh.duration - 8, sh.from + sh.duration], [0, 1, 1, 0], clamp);
  return (
    <div style={{ position: "absolute", top: 80, width: W, display: "flex", justifyContent: "center", opacity: o }}>
      <div style={{ background: "rgba(10,14,30,0.78)", color: "#F8FAFC", fontFamily: "Inter", fontWeight: 800, fontSize: 44, padding: "16px 36px", borderRadius: 26 }}>{text}</div>
    </div>
  );
};

/** A phone notification sliding down from the top. */
const Banner: React.FC<{ f: number; at: number; until: number; who: string; color: string; text: string; slot?: number; top?: number }> = ({ f, at, until, who, color, text, slot = 0, top = 210 }) => {
  const k = springAt(f, 0, [{ at, to: 1 }, { at: until, to: 0 }], 16, 190);
  if (k < 0.01) return null;
  return (
    <div
      style={{
        position: "absolute",
        left: 50,
        right: 50,
        top: top + slot * 190,
        transform: `translateY(${(1 - k) * -320}px)`,
        opacity: Math.min(1, k * 1.4),
        background: "rgba(250,250,252,0.96)",
        borderRadius: 40,
        padding: "26px 32px",
        display: "flex",
        gap: 24,
        alignItems: "center",
        boxShadow: "0 18px 50px rgba(0,0,0,0.35)",
        fontFamily: "Inter",
      }}
    >
      <div style={{ width: 92, height: 92, borderRadius: 99, background: color, color: "#fff", fontWeight: 800, fontSize: 44, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>{who[0]}</div>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 30, fontWeight: 700, color: "#475569" }}>{who} · DBMS project 📊</div>
        <div style={{ fontSize: 46, fontWeight: 700, color: "#0F172A", marginTop: 4 }}>{text}</div>
      </div>
    </div>
  );
};

/** Big hand-lettered words, punched in. */
const Lettering: React.FC<{ f: number; at: number; text: string; color: string; y: number; size?: number; rot?: number }> = ({ f, at, text, color, y, size = 150, rot = -4 }) => {
  const k = springAt(f, 0, [{ at, to: 1 }], 10, 220);
  if (k < 0.01) return null;
  return (
    <div style={{ position: "absolute", top: y, width: W, textAlign: "center", transform: `rotate(${rot}deg) scale(${0.4 + 0.6 * k})`, opacity: Math.min(1, k * 2) }}>
      <span style={{ fontFamily: "Kalam", fontWeight: 700, fontSize: size, color, WebkitTextStroke: "6px #111827", paintOrder: "stroke fill", textShadow: "0 10px 30px rgba(0,0,0,0.5)" }}>{text}</span>
    </div>
  );
};

const Night: React.FC<{ amount?: number }> = ({ amount = 0.35 }) => (
  <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 45%, rgba(0,0,0,0) 45%, rgba(4,6,20,1) 100%)", opacity: amount }} />
);

/* 1. 11 p.m. Intro done, slides 1-7 done, everyone off to sleep. */
const Shot1: React.FC = () => {
  const f = useF(SHOTS.s1);
  return (
    <AbsoluteFill>
      <Art src="drawn/s01.jpg" sh={SHOTS.s1} f={f} move={{ s: [1.04, 1.14], x: [0.5, 0.45], y: [0.4, 0.38] }} />
      <Night amount={0.25} />
      <Banner f={f} at={BEAT.banner11[0]} until={88} who="Meera" color="#D97706" text="intro done ✅" top={1300} />
      <Banner f={f} at={BEAT.banner11[1]} until={88} who="Riya" color="#DB2777" text="1–7 done ✅ night all 😴" slot={1} top={1300} />
      <Chip f={f} sh={SHOTS.s1} text="11:02 PM · PPT at 9 AM" />
    </AbsoluteFill>
  );
};

/* 2. 1:43 a.m. The message that wakes her. */
const Shot2: React.FC = () => {
  const f = useF(SHOTS.s2);
  return (
    <AbsoluteFill>
      <Art src="drawn/s02.jpg" sh={SHOTS.s2} f={f} shake={14} move={{ s: [1.18, 1.32], x: [0.5, 0.48], y: [0.42, 0.4] }} />
      <Night amount={0.3} />
      {f < SHOTS.s2.from + 3 && <AbsoluteFill style={{ background: "#fff", opacity: 0.5 }} />}
      <Banner f={f} at={BEAT.banner143} until={136} who="Riya" color="#DB2777" text="wait… where are slides 8–12??" />
      <Chip f={f} sh={SHOTS.s2} text="1:43 AM" />
    </AbsoluteFill>
  );
};

/* 3a. Riya, furious. 3b. Kabir, guilty. */
const Shot3a: React.FC = () => {
  const f = useF(SHOTS.s3a);
  return (
    <AbsoluteFill>
      <Art src="drawn/s03a.jpg" sh={SHOTS.s3a} f={f} shake={6} move={{ s: [1.08, 1.26], x: [0.5, 0.5], y: [0.42, 0.36] }} />
      <Night amount={0.25} />
    </AbsoluteFill>
  );
};

const Shot3b: React.FC = () => {
  const f = useF(SHOTS.s3b);
  return (
    <AbsoluteFill>
      <Art src="drawn/s03b.jpg" sh={SHOTS.s3b} f={f} move={{ s: [1.05, 1.22], x: [0.5, 0.5], y: [0.4, 0.38] }} />
      <Night amount={0.25} />
      <Banner f={f} at={BEAT.bannerKabir} until={392} who="Kabir" color="#16A34A" text="not me bro 💀" />
    </AbsoluteFill>
  );
};

/* 4. The spiral: we're going to FAIL. */
const Shot4: React.FC = () => {
  const f = useF(SHOTS.s4);
  const pulse = 0.18 + 0.12 * Math.sin((f - SHOTS.s4.from) / 3);
  return (
    <AbsoluteFill>
      <Art src="drawn/s04.jpg" sh={SHOTS.s4} f={f} shake={18} move={{ s: [1.0, 1.2], x: [0.5, 0.42], y: [0.3, 0.26] }} />
      <AbsoluteFill style={{ background: "radial-gradient(circle at 42% 26%, rgba(255,40,40,0.9), rgba(120,0,0,0) 55%)", opacity: pulse, mixBlendMode: "screen" }} />
      <Lettering f={f} at={SHOTS.s4.from + 10} text="WE'RE GOING TO FAIL" color="#F87171" y={1430} size={112} />
    </AbsoluteFill>
  );
};

/* 5. 2:30 a.m. Doing it alone. 7. She remembers. */
const Shot5: React.FC = () => {
  const f = useF(SHOTS.s5);
  return (
    <AbsoluteFill>
      <Art src="drawn/s05.jpg" sh={SHOTS.s5} f={f} move={{ s: [1.0, 1.1], x: [0.5, 0.55], y: [0.55, 0.5] }} />
      <Night amount={0.35} />
      <Chip f={f} sh={SHOTS.s5} text="2:30 AM · 0 of 5 slides" />
    </AbsoluteFill>
  );
};

const Shot7: React.FC = () => {
  const f = useF(SHOTS.s7);
  const punch = springAt(f, 1.08, [{ at: SHOTS.s7.from + 14, to: 1.24 }], 12, 160);
  return (
    <AbsoluteFill>
      <Art src="drawn/s07.jpg" sh={SHOTS.s7} f={f} move={{ s: [punch, punch], x: [0.48, 0.48], y: [0.36, 0.36] }} />
      <Night amount={0.2} />
      <Lettering f={f} at={SHOTS.s7.from + 16} text="!!" color="#FDE047" y={140} size={190} rot={8} />
    </AbsoluteFill>
  );
};

/** A phone held up over a blurred, darkened illustration. */
const PhoneOver: React.FC<{ f: number; src: string; sh: Span; children: React.ReactNode; glow?: string }> = ({ f, src, sh, children, glow }) => {
  const rise = springAt(f, 0, [{ at: sh.from, to: 1 }], 15, 120);
  const PW = 700;
  return (
    <AbsoluteFill>
      <Art src={src} sh={sh} f={f} move={{ s: [1.3, 1.36], x: [0.5, 0.5], y: [0.4, 0.4] }} filter="blur(14px) brightness(0.55)" />
      <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`} style={{ position: "absolute" }}>
        <g transform={`translate(0 ${(1 - rise) * 700 + Math.sin(f / 22) * 5})`}>
          <Phone x={(W - PW) / 2} y={190} w={PW} rot={-1.2} glow={glow}>
            {children}
          </Phone>
        </g>
      </svg>
    </AbsoluteFill>
  );
};

const Q = "Who's doing slides 8 to 12?";
const A = "Kabir. In Tuesday's group meeting: “And Kabir, you'll do eight to twelve.” … “Done. Easy.”";

/* 8. She asks Srutam; it quotes the line and cites the meeting. */
const ShotAsk: React.FC = () => {
  const f = useF(SHOTS.ask);
  const t = typed(Q, f, BEAT.typeStart, 18);
  const sent = f >= BEAT.send;
  const words = A.split(" ");
  const shown = Math.round(interpolate(f, [BEAT.answer, BEAT.answerEnd], [0, words.length], clamp));
  return (
    <AbsoluteFill>
      <PhoneOver f={f} src="drawn/s07.jpg" sh={SHOTS.ask} glow={sent ? "#3B82F6" : undefined}>
        <AskFlow
          w={700}
          sent={sent ? Q : undefined}
          thinking={sent && f < BEAT.answer}
          answer={shown > 0 ? words.slice(0, shown).join(" ") : undefined}
          refs={f >= BEAT.answerEnd ? ["Group meeting · Tue"] : []}
          pressedRef={f >= BEAT.tapRef && f < BEAT.tapRef + 8}
          input={sent ? "" : t.text}
          caret={!sent && Math.floor(f / 8) % 2 === 0}
          keyboard={sent ? interpolate(f, [BEAT.send, BEAT.send + 8], [1, 0], clamp) : 1}
          keyDown={t.key}
          sendDown={f >= BEAT.send - 2 && f < BEAT.send + 3}
        />
      </PhoneOver>
      <Chip f={f} sh={SHOTS.ask} text="2:34 AM" />
    </AbsoluteFill>
  );
};

/* 9. The canteen, last Tuesday: the recording plays. */
const Shot6: React.FC = () => {
  const f = useF(SHOTS.s6);
  const p = interpolate(f, [BEAT.play, BEAT.play + 127], [0, 1], clamp);
  const s = Math.floor((p * 42) % 60);
  return (
    <AbsoluteFill>
      <Art src="drawn/s06.jpg" sh={SHOTS.s6} f={f} move={{ s: [1.02, 1.22], x: [0.5, 0.62], y: [0.55, 0.45] }} filter="sepia(0.28) saturate(1.05)" />
      <AbsoluteFill style={{ background: "radial-gradient(ellipse at center, rgba(0,0,0,0) 55%, rgba(40,20,0,0.55) 100%)" }} />
      <div style={{ position: "absolute", left: 60, top: 1620, display: "flex", alignItems: "center", gap: 18, background: "rgba(10,14,30,0.75)", padding: "16px 28px", borderRadius: 24, fontFamily: "Inter", fontWeight: 800, fontSize: 38, color: "#fff" }}>
        <div style={{ width: 26, height: 26, borderRadius: 99, background: "#EF4444", opacity: Math.floor(f / 12) % 2 ? 1 : 0.35 }} />
        ▶ Tue · canteen · 0:{String(s).padStart(2, "0")}
      </div>
      <div style={{ position: "absolute", left: 60, right: 60, top: 1720, height: 10, borderRadius: 9, background: "rgba(255,255,255,0.3)" }}>
        <div style={{ width: `${p * 100}%`, height: "100%", borderRadius: 9, background: "#60A5FA" }} />
      </div>
    </AbsoluteFill>
  );
};

/* 10. Shared to the group. */
const CHAT = [
  { from: "Me", text: "🎧 Group meeting · Tue · 0:42\n“And Kabir, you'll do eight to twelve.” “Done. Easy.”", me: true },
  { from: "Riya", text: "KABIR.", color: "#DB2777" },
  { from: "Kabir", text: "on it 😭", color: "#16A34A" },
];
const ShotShare: React.FC = () => {
  const f = useF(SHOTS.share);
  const appear = BEAT.replies.map((at) => springAt(f, 0, [{ at, to: 1 }], 14, 170));
  return (
    <AbsoluteFill>
      <PhoneOver f={f} src="drawn/s03b.jpg" sh={SHOTS.share}>
        <ChatScreen w={700} title="DBMS project 📊" members="Riya, Kabir, you" messages={CHAT} appear={appear} />
      </PhoneOver>
    </AbsoluteFill>
  );
};

/* 11. 4 a.m., Kabir and the chai tower. */
const Shot8: React.FC = () => {
  const f = useF(SHOTS.s8);
  return (
    <AbsoluteFill>
      <Art src="drawn/s08.jpg" sh={SHOTS.s8} f={f} move={{ s: [1.02, 1.16], x: [0.5, 0.6], y: [0.5, 0.42] }} />
      <Night amount={0.25} />
      <Chip f={f} sh={SHOTS.s8} text="4:07 AM · slides 8–12 ✅" />
    </AbsoluteFill>
  );
};

/* 12. 9 a.m. Kabir presents, and starts recording. Then the band. */
const Shot9: React.FC = () => {
  const f = useF(SHOTS.s9);
  const band = interpolate(f, [BEAT.band, BEAT.band + 14], [0, 1], { ...clamp, easing: easeOut });
  return (
    <AbsoluteFill>
      <Art src="drawn/s09.jpg" sh={SHOTS.s9} f={f} move={{ s: [1.0, 1.3], x: [0.5, 0.62], y: [0.4, 0.55] }} />
      <Chip f={f} sh={{ from: SHOTS.s9.from, duration: 50 }} text="9:00 AM" />
      <Lettering f={f} at={1200} text="REC ●" color="#EF4444" y={1330} size={120} rot={-6} />
      <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`} style={{ position: "absolute" }}>
        <g transform={`translate(0 ${(1 - band) * 420})`}>
          <EndBand tagline="Listen once. Ask anytime." />
        </g>
      </svg>
    </AbsoluteFill>
  );
};

const LIST = [
  ["1 11 p.m.", SHOTS.s1, Shot1],
  ["2 1:43 a.m.", SHOTS.s2, Shot2],
  ["3a Riya", SHOTS.s3a, Shot3a],
  ["3b Kabir", SHOTS.s3b, Shot3b],
  ["4 Nightmare", SHOTS.s4, Shot4],
  ["5 2:30 a.m.", SHOTS.s5, Shot5],
  ["7 She remembers", SHOTS.s7, Shot7],
  ["8 Ask Srutam", SHOTS.ask, ShotAsk],
  ["6 Canteen", SHOTS.s6, Shot6],
  ["10 Share", SHOTS.share, ShotShare],
  ["8 Kabir 4 a.m.", SHOTS.s8, Shot8],
  ["9 Presentation", SHOTS.s9, Shot9],
] as const;

export { TOTAL };

export const DrawnSlides: React.FC = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    {LIST.map(([name, s, Comp]) => (
      <Sequence key={name} name={name} from={s.from * K} durationInFrames={s.duration * K}>
        <Comp />
      </Sequence>
    ))}
    <Captions cues={CAPTIONS} k={K} />
    <Soundtrack music="drawn/music.wav" musicVolume={0.42} lines={VO} sfx={SFX} k={K} sfxGain={0.6} duck={0.35} />
  </AbsoluteFill>
);
