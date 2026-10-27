// "The Brain Court", episode 1. Each shot is a Canva illustration (public/court/, see
// SCRIPT.md) moved with a camera. Characters act by swapping between matched pose variants
// ("b" images): whole frames for big actions (the gavel), and a soft circle around the mouth,
// driven by the voice's loudness, for lip-sync. Where there is no mouth variant, the mouth
// area of the same picture is stretched open on loud syllables.
import { AbsoluteFill, Img, interpolate, Sequence, staticFile } from "remotion";
import { clamp, easeInOut, easeOut } from "../anim";
import { springAt } from "../stories/physics";
import { Captions, Soundtrack } from "../stories/Story";
import { K, useT } from "../stories/time";
import { H, W } from "../storyboards/Kit";
import { LIPSYNC } from "./lipsync";
import { SrutamPhone, PhoneBody } from "./SrutamPhone";
import { BEAT, CAPTIONS, SFX, SHOTS, TOTAL, VO } from "./timeline";

type Span = { from: number; duration: number };
type Pt = [number, number];
/** Image pixels (944 x 1680) to frame pixels. */
const S = W / 944;
const art = (name: string) => staticFile(`court/${name}.jpg`);

/** Story frame within the whole video. */
const useF = (sh: Span) => useT() + sh.from;

/** Voice loudness (0..1) of a line at story frame f, held on twos like drawn animation. */
const level = (file: string, f: number) => {
  const line = VO.find((v) => v.file === file)!;
  const i = Math.floor((f - line.at) / 2) * 2;
  return LIPSYNC[file]?.[i] ?? 0;
};

/** Mouth open on this frame: loud syllables open it; during long loud stretches it still
 *  closes for one drawing in three, so shouting reads as speech, not a frozen open mouth. */
const talking = (file: string, f: number) => {
  const line = VO.find((v) => v.file === file)!;
  const d = Math.floor((f - line.at) / 2);
  return level(file, f) > 0.45 && d % 3 !== 2;
};

/** A mouth swap: the "open" picture shown inside a soft ellipse around the mouth. */
type MouthSwap = { open: string; at: Pt; r: Pt; file: string };
/** A mouth stretch: the mouth area of the same picture, scaled open on loud syllables. */
type MouthStretch = { at: Pt; r: Pt; file: string };

const mask = (at: Pt, r: Pt) =>
  `radial-gradient(ellipse ${r[0] * S}px ${r[1] * S}px at ${at[0] * S}px ${at[1] * S}px, #000 55%, transparent 100%)`;

const layer: React.CSSProperties = { position: "absolute", left: 0, top: 0, width: W, height: H };

/** A shot: picture, camera (push, pan, drift, shake), and acting layers in the picture's own space. */
const Shot: React.FC<{
  f: number;
  sh: Span;
  img: string;
  zoom?: [number, number];
  focus?: [Pt, Pt];
  shake?: number;
  shakeAt?: number;
  rumble?: number;
  swap?: MouthSwap;
  stretch?: MouthStretch;
  filter?: string;
  ease?: (t: number) => number;
  children?: React.ReactNode;
}> = ({ f, sh, img, zoom = [1.02, 1.1], focus = [[472, 840], [472, 840]], shake = 0, shakeAt, rumble = 0, swap, stretch, filter, ease = easeInOut, children }) => {
  const p = interpolate(f, [sh.from, sh.from + sh.duration], [0, 1], { ...clamp, easing: ease });
  const z = zoom[0] + (zoom[1] - zoom[0]) * p;
  const fx = focus[0][0] + (focus[1][0] - focus[0][0]) * p;
  const fy = focus[0][1] + (focus[1][1] - focus[0][1]) * p;
  const since = f - (shakeAt ?? sh.from);
  const hit = since >= 0 ? shake * Math.exp(-since / 5) : 0;
  const amp = hit + rumble;
  const dx = Math.sin(f / 37) * 3 + Math.sin(f * 2.3) * amp;
  const dy = Math.cos(f / 43) * 3 + Math.cos(f * 2.9) * amp;
  const open = swap ? talking(swap.file, f) : false;
  const st = stretch ? level(stretch.file, f) : 0;
  return (
    <AbsoluteFill style={{ overflow: "hidden", background: "#0b0610" }}>
      <div style={{ ...layer, transformOrigin: `${fx * S}px ${fy * S}px`, transform: `translate(${dx}px, ${dy}px) scale(${z})`, filter }}>
        <Img src={art(img)} style={layer} />
        {swap && <Img src={art(swap.open)} style={{ ...layer, opacity: open ? 1 : 0, WebkitMaskImage: mask(swap.at, swap.r), maskImage: mask(swap.at, swap.r) }} />}
        {stretch && st > 0.1 && (
          <Img
            src={art(img)}
            style={{
              ...layer,
              transformOrigin: `${stretch.at[0] * S}px ${(stretch.at[1] - stretch.r[1] * 0.4) * S}px`,
              transform: `scale(${1 + 0.05 * st}, ${1 + 0.32 * st})`,
              WebkitMaskImage: mask(stretch.at, stretch.r),
              maskImage: mask(stretch.at, stretch.r),
            }}
          />
        )}
        {children}
      </div>
    </AbsoluteFill>
  );
};

/** Paper sheets drifting down (Memory's leaky filing cabinet, the spiral). */
const Papers: React.FC<{ f: number; from: number; n?: number; seed?: number; speed?: number }> = ({ f, from, n = 9, seed = 1, speed = 1 }) => (
  <>
    {Array.from({ length: n }, (_, i) => {
      const r = (k: number) => {
        const x = Math.sin((i + 1) * 12.9898 * (seed + k) + seed * 78.233) * 43758.5453;
        return x - Math.floor(x);
      };
      const t = (f - from) * speed + r(1) * 140;
      const y = ((t * 4.5) % 2300) - 300;
      const x = r(2) * W + Math.sin(t / 14 + r(3) * 6) * 60;
      const rot = Math.sin(t / 11 + r(4) * 6) * 50;
      return (
        <div
          key={i}
          style={{ position: "absolute", left: x, top: y, width: 70, height: 90, background: "#FBF4E4", border: "2px solid #6b4a3a", borderRadius: 4, transform: `rotate(${rot}deg) scaleX(${Math.cos(t / 9 + i)})`, boxShadow: "0 6px 14px rgba(0,0,0,0.25)" }}
        >
          <div style={{ margin: "16px 10px", height: 4, background: "#c9b9a0", boxShadow: "0 12px 0 #c9b9a0, 0 24px 0 #c9b9a0, 0 36px 0 #c9b9a0" }} />
        </div>
      );
    })}
  </>
);

/** Dust puff and speed lines where the gavel lands. */
const Impact: React.FC<{ f: number; at: number; x: number; y: number }> = ({ f, at, x, y }) => {
  const t = f - at;
  if (t < 0 || t > 18) return null;
  const k = t / 18;
  return (
    <div style={{ position: "absolute", left: x, top: y, width: 0, height: 0 }}>
      {Array.from({ length: 8 }, (_, i) => {
        const a = (i / 8) * Math.PI * 2;
        const d = 60 + k * 260;
        return <div key={i} style={{ position: "absolute", left: Math.cos(a) * d - 40, top: Math.sin(a) * d * 0.45 - 25, width: 80 * (1 - k * 0.5), height: 50 * (1 - k * 0.5), borderRadius: 99, background: "#efe3d3", opacity: 0.85 * (1 - k) }} />;
      })}
    </div>
  );
};

/** Big hand-lettered words, punched in. */
const Lettering: React.FC<{ f: number; at: number; text: string; color: string; y: number; size?: number; rot?: number; until?: number }> = ({ f, at, text, color, y, size = 150, rot = -4, until }) => {
  const k = springAt(f, 0, [{ at, to: 1 }, ...(until ? [{ at: until, to: 0 }] : [])], 10, 220);
  if (k < 0.01) return null;
  return (
    <div style={{ position: "absolute", top: y, width: W, textAlign: "center", transform: `rotate(${rot}deg) scale(${0.4 + 0.6 * k})`, opacity: Math.min(1, k * 2) }}>
      <span style={{ fontFamily: "Kalam", fontWeight: 700, fontSize: size, color, WebkitTextStroke: "7px #1a1020", paintOrder: "stroke fill", textShadow: "0 10px 30px rgba(0,0,0,0.45)" }}>{text}</span>
    </div>
  );
};

const Chip: React.FC<{ f: number; sh: Span; text: string }> = ({ f, sh, text }) => {
  const o = interpolate(f, [sh.from + 2, sh.from + 10, sh.from + sh.duration - 8, sh.from + sh.duration], [0, 1, 1, 0], clamp);
  return (
    <div style={{ position: "absolute", top: 90, width: W, display: "flex", justifyContent: "center", opacity: o }}>
      <div style={{ background: "rgba(10,14,30,0.78)", color: "#F8FAFC", fontFamily: "Inter", fontWeight: 800, fontSize: 44, padding: "16px 36px", borderRadius: 26 }}>{text}</div>
    </div>
  );
};

const Flash: React.FC<{ f: number; at: number; len?: number; color?: string; peak?: number }> = ({ f, at, len = 8, color = "#fff", peak = 0.8 }) => {
  const o = interpolate(f, [at, at + 1, at + len], [0, peak, 0], clamp);
  return o > 0 ? <AbsoluteFill style={{ background: color, opacity: o }} /> : null;
};

/* 1. Hook: the gavel comes down. "ORDER! Who lost the deadline?!" */
const ShotGavel: React.FC = () => {
  const f = useF(SHOTS.gavel);
  const down = f >= BEAT.slam;
  return (
    <AbsoluteFill>
      <Shot f={f} sh={SHOTS.gavel} img={down ? "c01" : "c01b"} zoom={[1.12, 1.02]} focus={[[520, 760], [480, 800]]} shake={22} shakeAt={BEAT.slam} ease={easeOut}>
        <Impact f={f} at={BEAT.slam} x={560 * S} y={1440 * S} />
      </Shot>
      <Flash f={f} at={BEAT.slam} len={6} peak={0.55} />
    </AbsoluteFill>
  );
};

/* 2. Aarav, 9:04 p.m., frozen. Then the camera pushes into his forehead. */
const ShotAarav: React.FC = () => {
  const f = useF(SHOTS.aarav);
  const push = interpolate(f, [BEAT.push, SHOTS.aarav.from + SHOTS.aarav.duration], [0, 1], { ...clamp, easing: (t) => t * t * t });
  return (
    <AbsoluteFill>
      {push > 0 ? (
        <AbsoluteFill style={{ transformOrigin: `${470 * S}px ${560 * S}px`, transform: `scale(${1.1 + push * 6})` }}>
          <Img src={art("c02")} style={layer} />
        </AbsoluteFill>
      ) : (
        <Shot f={f} sh={{ from: SHOTS.aarav.from, duration: BEAT.push - SHOTS.aarav.from }} img="c02" zoom={[1.0, 1.1]} focus={[[480, 760], [470, 560]]} />
      )}
      <AbsoluteFill style={{ background: "radial-gradient(circle at 50% 30%, #ffd1e6 0%, #ff8fbf 60%, #b44d82 100%)", opacity: push }} />
      <Chip f={f} sh={{ from: SHOTS.aarav.from, duration: 50 }} text="9:04 PM" />
    </AbsoluteFill>
  );
};

/* 3. Inside his head: the court. */
const ShotWide: React.FC = () => {
  const f = useF(SHOTS.wide);
  return (
    <AbsoluteFill>
      <Shot f={f} sh={SHOTS.wide} img="c03" zoom={[1.6, 1.02]} focus={[[472, 500], [472, 820]]} ease={easeOut} />
      <AbsoluteFill style={{ background: "#ff9fca", opacity: interpolate(f, [SHOTS.wide.from, SHOTS.wide.from + 10], [1, 0], clamp) }} />
    </AbsoluteFill>
  );
};

/* 4. Anxiety accuses Memory. */
const ShotAnxiety: React.FC = () => {
  const f = useF(SHOTS.anxiety);
  return (
    <Shot f={f} sh={SHOTS.anxiety} img="c04b" zoom={[1.0, 1.14]} focus={[[472, 900], [450, 640]]} rumble={1.2} swap={{ open: "c04", at: [445, 591], r: [70, 80], file: "c-accuse" }}>
      <Papers f={f} from={SHOTS.anxiety.from} n={5} seed={4} speed={0.7} />
    </Shot>
  );
};

/* 5. Memory's defence. */
const ShotMemory: React.FC = () => {
  const f = useF(SHOTS.memory);
  return (
    <Shot f={f} sh={SHOTS.memory} img="c05" zoom={[1.0, 1.18]} focus={[[472, 900], [420, 640]]} stretch={{ at: [410, 595], r: [85, 48], file: "c-defend" }}>
      <Papers f={f} from={SHOTS.memory.from} n={8} seed={2} />
    </Shot>
  );
};

/* 6. The judge rubs his temples, then: "Call the witnesses." */
const ShotJudge: React.FC = () => {
  const f = useF(SHOTS.judge);
  const up = f >= 600;
  return (
    <Shot
      f={f}
      sh={SHOTS.judge}
      img={up ? "c06b" : "c06"}
      zoom={[1.05, 1.12]}
      focus={[[472, 760], [472, 740]]}
      stretch={up ? { at: [472, 770], r: [55, 32], file: "c-witnesses" } : undefined}
    />
  );
};

/* 7. Overconfidence: "Bro. I told him 'I'll remember it.'" */
const ShotOverconf: React.FC = () => {
  const f = useF(SHOTS.overconf);
  return <Shot f={f} sh={SHOTS.overconf} img="c07" zoom={[1.0, 1.16]} focus={[[400, 1000], [340, 860]]} swap={{ open: "c07b", at: [335, 857], r: [110, 70], file: "c-bro" }} />;
};

/* 8. Sleep: "Was it… Friday? Or…" and dozes off. */
const ShotSleep: React.FC = () => {
  const f = useF(SHOTS.sleep);
  const dozing = f >= 920;
  return (
    <Shot
      f={f}
      sh={SHOTS.sleep}
      img={dozing ? "c08" : "c08b"}
      zoom={[1.04, 1.16]}
      focus={[[540, 820], [540, 760]]}
      stretch={dozing ? undefined : { at: [478, 830], r: [60, 40], file: "c-friday" }}
    />
  );
};

/* 9. The spiral. */
const ShotSpiral: React.FC = () => {
  const f = useF(SHOTS.spiral);
  const t = f - SHOTS.spiral.from;
  const red = 0.15 + 0.15 * Math.max(0, Math.sin(t / 3));
  return (
    <AbsoluteFill style={{ transform: `rotate(${Math.sin(t / 9) * 2.5}deg) scale(1.06)` }}>
      <Shot f={f} sh={SHOTS.spiral} img="c09" zoom={[1.0, 1.25]} focus={[[472, 900], [472, 560]]} rumble={3 + t / 25} stretch={{ at: [480, 420], r: [70, 60], file: "c-spiral" }}>
        <Papers f={f} from={SHOTS.spiral.from} n={10} seed={7} speed={1.6} />
      </Shot>
      <AbsoluteFill style={{ background: "radial-gradient(circle at 70% 10%, rgba(255,30,30,0.9), rgba(120,0,0,0.2) 70%)", opacity: red, mixBlendMode: "screen" }} />
    </AbsoluteFill>
  );
};

/* 10. The doors burst open: "Objection." */
const ShotDoors: React.FC = () => {
  const f = useF(SHOTS.doors);
  return (
    <AbsoluteFill>
      <Shot f={f} sh={SHOTS.doors} img="c10" zoom={[1.0, 1.15]} focus={[[472, 840], [472, 640]]} shake={10} />
      <Flash f={f} at={SHOTS.doors.from} len={12} color="#BFDBFE" peak={0.9} />
      <Lettering f={f} at={1100} text="OBJECTION!" color="#60A5FA" y={1180} size={150} rot={-5} />
    </AbsoluteFill>
  );
};

/** Sound-wave rings rippling out of Srutam, louder when it speaks. */
const Rings: React.FC<{ f: number; x: number; y: number; file?: string }> = ({ f, x, y, file }) => {
  const v = file ? level(file, f) : 0;
  return (
    <>
      {[0, 1, 2].map((i) => {
        const k = ((f / 40 + i / 3) % 1 + 1) % 1;
        return <div key={i} style={{ position: "absolute", left: x - 150 - k * 260, top: y - 150 - k * 260, width: 300 + k * 520, height: 300 + k * 520, borderRadius: 999, border: `${6 + v * 10}px solid rgba(96,165,250,${0.6 * (1 - k)})` }} />;
      })}
    </>
  );
};

/* 11. "I have the recording." */
const ShotSrutam: React.FC = () => {
  const f = useF(SHOTS.srutam);
  return (
    <Shot f={f} sh={SHOTS.srutam} img="c11" zoom={[1.0, 1.12]} focus={[[472, 900], [470, 860]]} swap={{ open: "c11b", at: [466, 935], r: [60, 40], file: "c-recording" }}>
      <Rings f={f} x={466 * S} y={860 * S} file="c-recording" />
    </Shot>
  );
};

/* 12. The real app: Aarav's question, the answer with transcript evidence, and the recording playing. */
const ShotProof: React.FC = () => {
  const f = useF(SHOTS.proof);
  const k = springAt(f, 0, [{ at: SHOTS.proof.from + 2, to: 1 }], 15, 130);
  const PW = 760;
  return (
    <AbsoluteFill>
      <Shot f={f} sh={SHOTS.proof} img="c11" zoom={[1.15, 1.2]} focus={[[470, 860], [470, 860]]} filter="blur(12px) brightness(0.55)">
        <Rings f={f} x={466 * S} y={860 * S} />
      </Shot>
      <div style={{ position: "absolute", left: (W - PW) / 2, top: 140, transform: `translateY(${(1 - k) * 900}px) rotate(${-1.5 + Math.sin(f / 30) * 0.4}deg)` }}>
        <PhoneBody w={PW}>
          <SrutamPhone f={f} w={PW * 0.94} ask={BEAT.ask} answer={BEAT.answer} play={BEAT.play} playLen={111} />
        </PhoneBody>
      </div>
    </AbsoluteFill>
  );
};

/* 13. Reactions. */
const ShotReact: React.FC = () => {
  const f = useF(SHOTS.react);
  const bounce = springAt(f, 1.12, [{ at: SHOTS.react.from + 4, to: 1.0 }], 8, 260);
  return <Shot f={f} sh={SHOTS.react} img="c13" zoom={[bounce, bounce]} focus={[[620, 800], [620, 800]]} />;
};

/* 14. "Case closed. Next time… record it." Gavel down. */
const ShotClosed: React.FC = () => {
  const f = useF(SHOTS.closed);
  const down = f >= BEAT.slam2;
  return (
    <AbsoluteFill>
      <Shot f={f} sh={SHOTS.closed} img={down ? "c14b" : "c14"} zoom={[1.0, 1.1]} focus={[[472, 820], [472, 760]]} shake={down ? 14 : 0} shakeAt={BEAT.slam2} stretch={down ? undefined : { at: [537, 740], r: [70, 36], file: "c-closed" }}>
        <Impact f={f} at={BEAT.slam2} x={330 * S} y={1060 * S} />
      </Shot>
      <Lettering f={f} at={BEAT.slam2 + 1} text="CASE CLOSED" color="#F472B6" y={1260} size={130} rot={-6} />
    </AbsoluteFill>
  );
};

/* 15. Back outside: Aarav exhales and replies. */
const ShotRelief: React.FC = () => {
  const f = useF(SHOTS.relief);
  const k = springAt(f, 0, [{ at: BEAT.sent, to: 1 }], 16, 190);
  return (
    <AbsoluteFill>
      <Shot f={f} sh={SHOTS.relief} img="c15" zoom={[1.08, 1.0]} focus={[[460, 700], [460, 760]]} ease={easeOut} />
      <div style={{ position: "absolute", left: 50, right: 50, top: 170, transform: `translateY(${(1 - k) * -360}px)`, opacity: Math.min(1, k * 1.4), background: "rgba(250,250,252,0.96)", borderRadius: 40, padding: "26px 32px", display: "flex", gap: 24, alignItems: "center", boxShadow: "0 18px 50px rgba(0,0,0,0.35)", fontFamily: "Inter" }}>
        <div style={{ width: 92, height: 92, borderRadius: 99, background: "#2563EB", color: "#fff", fontWeight: 800, fontSize: 44, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>A</div>
        <div>
          <div style={{ fontSize: 30, fontWeight: 700, color: "#475569" }}>You → Priya (Manager)</div>
          <div style={{ fontSize: 46, fontWeight: 700, color: "#0F172A", marginTop: 4 }}>On it! Final deck by 10 AM ✅</div>
        </div>
      </div>
    </AbsoluteFill>
  );
};

/* Call to action. */
const ShotCta: React.FC = () => {
  const f = useT();
  const k = (at: number) => interpolate(f, [at, at + 10], [0, 1], { ...clamp, easing: easeOut });
  const line = (at: number): React.CSSProperties => ({ opacity: k(at), transform: `translateY(${(1 - k(at)) * 40}px)` });
  return (
    <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 30%, #1E3A8A 0%, #050714 70%)", alignItems: "center", justifyContent: "center", textAlign: "center", padding: 80 }}>
      <Img src={staticFile("court/srutam-logo.png")} style={{ ...line(0), width: 300, height: 300, marginBottom: -20, filter: `drop-shadow(0 0 ${30 + 30 * level("c-cta", f)}px rgba(96,165,250,0.7))` }} />
      <div style={{ ...line(4), fontFamily: "Playfair Display", fontSize: 150, color: "#F8FAFC" }}>Srutam</div>
      <div style={{ ...line(8), fontFamily: "Inter", fontWeight: 700, fontSize: 56, color: "#E2E8F0", marginTop: 40, lineHeight: 1.3 }}>Record your meetings and lectures.</div>
      <div style={{ ...line(16), fontFamily: "Inter", fontWeight: 700, fontSize: 56, color: "#93C5FD", marginTop: 18, lineHeight: 1.3 }}>Ask later. It finds who said what.</div>
      <div style={{ ...line(28), marginTop: 90, transform: `scale(${0.9 + 0.1 * k(28) + 0.02 * Math.sin(f / 5)})`, background: "#F8FAFC", color: "#0B1033", fontFamily: "Inter", fontWeight: 800, fontSize: 54, padding: "30px 60px", borderRadius: 999 }}>▶ Get it on Google Play</div>
      <div style={{ ...line(36), fontFamily: "Inter", fontWeight: 600, fontSize: 42, color: "#CBD5E1", marginTop: 44 }}>srutam.iamjustkrishna.space</div>
      <div style={{ ...line(42), fontFamily: "Inter", fontWeight: 600, fontSize: 42, color: "#94A3B8", marginTop: 12 }}>Instagram @srutam.app</div>
    </AbsoluteFill>
  );
};

const LIST = [
  ["1 Gavel", SHOTS.gavel, ShotGavel],
  ["2 Aarav", SHOTS.aarav, ShotAarav],
  ["3 Court", SHOTS.wide, ShotWide],
  ["4 Anxiety", SHOTS.anxiety, ShotAnxiety],
  ["5 Memory", SHOTS.memory, ShotMemory],
  ["6 Judge", SHOTS.judge, ShotJudge],
  ["7 Overconfidence", SHOTS.overconf, ShotOverconf],
  ["8 Sleep", SHOTS.sleep, ShotSleep],
  ["9 Spiral", SHOTS.spiral, ShotSpiral],
  ["10 Objection", SHOTS.doors, ShotDoors],
  ["11 Srutam", SHOTS.srutam, ShotSrutam],
  ["12 Proof", SHOTS.proof, ShotProof],
  ["13 Reactions", SHOTS.react, ShotReact],
  ["14 Case closed", SHOTS.closed, ShotClosed],
  ["15 Relief", SHOTS.relief, ShotRelief],
  ["CTA", SHOTS.cta, ShotCta],
] as const;

export { TOTAL };

export const BrainCourt: React.FC = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    {LIST.map(([name, s, Comp]) => (
      <Sequence key={name} name={name} from={s.from * K} durationInFrames={s.duration * K}>
        <Comp />
      </Sequence>
    ))}
    <Captions cues={CAPTIONS} k={K} />
    <Soundtrack music="court/music.wav" musicVolume={0.4} lines={VO} sfx={SFX} k={K} sfxGain={0.7} duck={0.35} />
  </AbsoluteFill>
);
