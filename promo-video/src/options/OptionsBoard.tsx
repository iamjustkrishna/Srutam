import { AbsoluteFill } from "remotion";
import { rand } from "../anim";
import { FONT } from "../theme";
import { Shru } from "../spirit/Shru";
import { Dot, Echo, Gaja, Koyal, Nada } from "./Mascots";
import { Glowfly } from "./shapes";

const INK = "#1A1433";

const ShruCard: React.FC = () => (
  <g>
    <Shru x={50} y={80} s={1.0} pose="sit" />
    <Glowfly x={72} y={150} r={12} />
  </g>
);

type Option = {
  letter: string;
  name: string;
  kind: string;
  pitch: string;
  personality: string;
  motion: string;
  why: string;
  bg: [string, string];
  swatches: string[];
  Art: React.FC;
  tag?: string;
};

const OPTIONS: Option[] = [
  {
    letter: "A",
    name: "Shru",
    kind: "the fox spirit",
    pitch: "Lives in the floating dock and leaps out the moment you tap.",
    personality: "Brave, quick, loyal",
    motion: "Anime leaps, tails crack like sound waves",
    why: "Agile = instant capture. Foxes hear everything.",
    bg: ["#24306B", "#0E0A24"],
    swatches: ["#3B82F6", "#67E8F9", "#FBBF24"],
    Art: ShruCard,
    tag: "CURRENT",
  },
  {
    letter: "B",
    name: "Gaja",
    kind: "the baby elephant",
    pitch: "An elephant never forgets, and neither will you.",
    personality: "Gentle, wise, a little clumsy",
    motion: "Ears ripple like speakers, trunk scoops the thought",
    why: "Memory is the whole product. Huge ears = listening. Deeply Indian.",
    bg: ["#3B2A6A", "#120C26"],
    swatches: ["#A5B4FC", "#BE123C", "#FBBF24"],
    Art: Gaja,
  },
  {
    letter: "C",
    name: "Koyal",
    kind: "the koel bird",
    pitch: "Sings your ideas back to you.",
    personality: "Cheerful, chatty, musical",
    motion: "Wingbeats are equalizer bars; swoops and dives",
    why: "The koel's call is India's most familiar sound. Voice-first.",
    bg: ["#173B5C", "#0A1424"],
    swatches: ["#1E1B4B", "#22D3EE", "#EF4444"],
    Art: () => (
      <g transform="translate(40 40) scale(0.86)">
        <Koyal />
      </g>
    ),
  },
  {
    letter: "D",
    name: "Nāda",
    kind: "the sound dragon",
    pitch: "Nāda: Sanskrit for the first sound. Its body is a waveform.",
    personality: "Mystical, playful, protective",
    motion: "Body ripples to the voiceover; coils around thoughts",
    why: "Most striking in motion: it literally animates to the audio.",
    bg: ["#134E4A", "#0B0F24"],
    swatches: ["#2DD4BF", "#6366F1", "#FBBF24"],
    Art: () => (
      <g transform="translate(60 30) scale(0.88)">
        <Nada />
      </g>
    ),
  },
  {
    letter: "E",
    name: "Dot",
    kind: "the recorder bot",
    pitch: "A tiny recorder with a big heart: the red record button.",
    personality: "Earnest, tidy, a bit nerdy",
    motion: "Hovers, blinks, jars every thought",
    why: "Most literal and techy. Easiest to tie to the app's UI.",
    bg: ["#1E3A5F", "#0B1220"],
    swatches: ["#F8FAFC", "#2563EB", "#EF4444"],
    Art: Dot,
  },
  {
    letter: "F",
    name: "Echo",
    kind: "the headphone cat",
    pitch: "Hears what you almost forgot.",
    personality: "Cool, curious, mischievous",
    motion: "Pounces, swats, tail squiggles like a wave",
    why: "Cats rule the internet. Highest meme and sticker potential.",
    bg: ["#3B1E5C", "#120C26"],
    swatches: ["#334155", "#2563EB", "#FBBF24"],
    Art: Echo,
  },
];

const Card: React.FC<{ o: Option }> = ({ o }) => (
  <div
    style={{
      width: 770,
      borderRadius: 32,
      overflow: "hidden",
      background: "#FFFFFF",
      boxShadow: "0 20px 50px rgba(26,20,51,0.18)",
    }}
  >
    <div style={{ position: "relative" }}>
      <svg
        viewBox="0 0 600 545"
        width={770}
        height={700}
        style={{ display: "block" }}
      >
        <defs>
          <radialGradient id={`bg${o.letter}`} cx="45%" cy="45%" r="75%">
            <stop offset="0%" stopColor={o.bg[0]} />
            <stop offset="100%" stopColor={o.bg[1]} />
          </radialGradient>
        </defs>
        <rect width={600} height={545} fill={`url(#bg${o.letter})`} />
        {Array.from({ length: 40 }, (_, i) => (
          <circle
            key={i}
            cx={rand(i + o.letter.charCodeAt(0)) * 600}
            cy={rand(i * 3 + 7) * 545}
            r={0.8 + rand(i * 5) * 2}
            fill="#FFFFFF"
            opacity={0.25 + rand(i * 7) * 0.5}
          />
        ))}
        <o.Art />
      </svg>
      <div
        style={{
          position: "absolute",
          left: 22,
          top: 22,
          width: 64,
          height: 64,
          borderRadius: 18,
          background: "#FFFFFF",
          color: INK,
          fontSize: 38,
          fontWeight: 900,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        {o.letter}
      </div>
      {o.tag && (
        <div
          style={{
            position: "absolute",
            right: 22,
            top: 30,
            padding: "8px 16px",
            borderRadius: 999,
            background: "#FBBF24",
            color: INK,
            fontSize: 20,
            fontWeight: 800,
            letterSpacing: 2,
          }}
        >
          {o.tag}
        </div>
      )}
    </div>
    <div style={{ padding: "26px 34px 32px" }}>
      <div style={{ display: "flex", alignItems: "baseline", gap: 14 }}>
        <div style={{ fontFamily: FONT.serif, fontSize: 64, lineHeight: 1 }}>
          {o.name}
        </div>
        <div
          style={{
            fontFamily: FONT.serif,
            fontStyle: "italic",
            fontSize: 32,
            color: "#2563EB",
          }}
        >
          {o.kind}
        </div>
      </div>
      <div
        style={{
          fontSize: 28,
          fontWeight: 600,
          marginTop: 14,
          lineHeight: 1.3,
        }}
      >
        {o.pitch}
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "150px 1fr",
          rowGap: 8,
          marginTop: 18,
          fontSize: 22,
          lineHeight: 1.35,
        }}
      >
        {[
          ["Personality", o.personality],
          ["Motion", o.motion],
          ["Why it fits", o.why],
        ].map(([k, v]) => (
          <div key={k} style={{ display: "contents" }}>
            <div
              style={{ fontWeight: 800, color: "#7C6FA8", letterSpacing: 1 }}
            >
              {k}
            </div>
            <div style={{ color: "#3B3460" }}>{v}</div>
          </div>
        ))}
      </div>
      <div style={{ display: "flex", gap: 10, marginTop: 20 }}>
        {o.swatches.map((c) => (
          <div
            key={c}
            style={{
              width: 54,
              height: 30,
              borderRadius: 8,
              background: c,
              border: "2px solid rgba(26,20,51,0.25)",
            }}
          />
        ))}
      </div>
    </div>
  </div>
);

export const OptionsBoard: React.FC = () => (
  <AbsoluteFill
    style={{
      background: "#F6F1EA",
      padding: 60,
      fontFamily: FONT.sans,
      color: INK,
    }}
  >
    <div
      style={{
        height: 180,
        display: "flex",
        justifyContent: "space-between",
        alignItems: "flex-end",
      }}
    >
      <div>
        <div
          style={{
            fontSize: 26,
            fontWeight: 700,
            letterSpacing: 8,
            color: "#7C6FA8",
          }}
        >
          SRUTAM · HERO OPTIONS
        </div>
        <div style={{ fontFamily: FONT.serif, fontSize: 100, lineHeight: 1.1 }}>
          Who{" "}
          <span style={{ fontStyle: "italic", color: "#2563EB" }}>
            catches the thought?
          </span>
        </div>
      </div>
      <div
        style={{
          fontSize: 25,
          color: "#4B4470",
          maxWidth: 820,
          textAlign: "right",
          lineHeight: 1.4,
        }}
      >
        Same world, same story: a golden firefly fades in seconds and a creature
        that lives in Srutam's floating dock saves it. Pick the hero, or mix and
        match.
      </div>
    </div>
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(3, 770px)",
        gap: 35,
        marginTop: 40,
      }}
    >
      {OPTIONS.map((o) => (
        <Card key={o.letter} o={o} />
      ))}
    </div>
  </AbsoluteFill>
);
