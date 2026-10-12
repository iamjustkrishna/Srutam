import { Audio } from "@remotion/media";
import { AbsoluteFill, Sequence, staticFile } from "remotion";
import { Caption } from "./components/Overlays";
import { Catch } from "./scenes/Catch";
import { Connect } from "./scenes/Connect";
import { End } from "./scenes/End";
import { Hook } from "./scenes/Hook";
import { Insights } from "./scenes/Insights";
import { Lost } from "./scenes/Lost";
import { CATCH, SCENES, VO } from "./timeline";

// Seconds of each VO file (public/voiceover/durations.json), in frames.
const VO_FRAMES: Record<string, number> = {
  "v1-hook": 107,
  "v2-lost": 46,
  "v3-tap": 60,
  "v4-caught": 97,
  "v5-insights": 116,
  "v6-connect": 87,
  "v7-end": 83,
};

// Burned-in captions (the hook and end lines are already on screen).
const CAPTIONS: Record<string, string> = {
  "v2-lost": "Most never make it.",
  "v3-tap": "Tap, tap. From any app.",
  "v4-caught": "Caught. And transcribed, right on your phone.",
  "v5-insights": "Srutam pulls out your next steps, ideas, and decisions.",
  "v6-connect": "Every thought connects. Just ask.",
};

const C0 = SCENES.catch.from;
const SFX: { file: string; at: number; volume?: number }[] = [
  // Countdown ticks
  { file: "tick", at: 2 },
  { file: "tick", at: 37 },
  { file: "tick", at: 72 },
  // Three ideas die
  { file: "ash", at: SCENES.lost.from + 28 },
  { file: "ash", at: SCENES.lost.from + 44 },
  { file: "ash", at: SCENES.lost.from + 60 },
  // Last second: faster ticks, then tap, tap, the dive and the catch
  { file: "tick", at: C0 + 2 },
  { file: "tick", at: C0 + 14 },
  { file: "tick", at: C0 + 24 },
  { file: "tick", at: C0 + 32 },
  { file: "tap", at: C0 + CATCH.tap1 },
  { file: "tap", at: C0 + CATCH.tap2 },
  { file: "whoosh", at: C0 + CATCH.dive },
  { file: "pop", at: C0 + CATCH.dive + 30 },
  { file: "tap", at: C0 + CATCH.stop },
  { file: "knock", at: C0 + CATCH.saved, volume: 1 }, // the app's knock-knock
  { file: "whoosh", at: C0 + 240 },
  // Insights land
  { file: "pop", at: SCENES.insights.from + 56 },
  { file: "pop", at: SCENES.insights.from + 85 },
  { file: "pop", at: SCENES.insights.from + 106 },
  { file: "pop", at: SCENES.insights.from + 126 },
  // Night and the constellation
  { file: "whoosh", at: SCENES.connect.from - 4 },
  { file: "shimmer", at: SCENES.connect.from + 30 },
  { file: "whoosh", at: SCENES.connect.from + 62 },
  { file: "pop", at: SCENES.connect.from + 104 },
  // Brand
  { file: "shimmer", at: SCENES.end.from + 6 },
];

const inVo = (f: number) =>
  VO.some((v) => f >= v.at - 4 && f <= v.at + VO_FRAMES[v.file] + 4);

export const SparkStory: React.FC = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    <Sequence
      name="1 Hook"
      from={SCENES.hook.from}
      durationInFrames={SCENES.hook.duration}
    >
      <Hook />
    </Sequence>
    <Sequence
      name="2 Lost"
      from={SCENES.lost.from}
      durationInFrames={SCENES.lost.duration}
    >
      <Lost />
    </Sequence>
    <Sequence
      name="3 Catch"
      from={SCENES.catch.from}
      durationInFrames={SCENES.catch.duration}
    >
      <Catch />
    </Sequence>
    <Sequence
      name="4 Insights"
      from={SCENES.insights.from}
      durationInFrames={SCENES.insights.duration}
    >
      <Insights />
    </Sequence>
    <Sequence
      name="5 Connect"
      from={SCENES.connect.from}
      durationInFrames={SCENES.connect.duration}
    >
      <Connect />
    </Sequence>
    <Sequence
      name="6 End"
      from={SCENES.end.from}
      durationInFrames={SCENES.end.duration}
    >
      <End />
    </Sequence>

    {VO.map((v, i) => {
      if (!CAPTIONS[v.file]) return null;
      // End each caption before the next line starts so two never overlap.
      const next = VO[i + 1]?.at ?? Infinity;
      const to = Math.min(v.at + VO_FRAMES[v.file], next - 10);
      return (
        <Caption key={v.file} text={CAPTIONS[v.file]} from={v.at} to={to} />
      );
    })}

    <Audio
      src={staticFile("music/bed.wav")}
      volume={(f) => (inVo(f) ? 0.35 : 0.6)}
    />
    {VO.map((v) => (
      <Sequence
        key={v.file}
        name={`VO ${v.file}`}
        from={v.at}
        durationInFrames={VO_FRAMES[v.file] + 6}
        layout="none"
      >
        <Audio src={staticFile(`voiceover/${v.file}.mp3`)} />
      </Sequence>
    ))}
    {SFX.map((s, i) => (
      <Sequence
        key={i}
        name={`SFX ${s.file}`}
        from={s.at}
        durationInFrames={60}
        layout="none"
      >
        <Audio src={staticFile(`sfx/${s.file}.wav`)} volume={s.volume ?? 0.6} />
      </Sequence>
    ))}
  </AbsoluteFill>
);
