import { Audio } from "@remotion/media";
import { AbsoluteFill, Sequence, staticFile } from "remotion";
import {
  End,
  Later,
  Morning1,
  Morning2,
  Night1,
  Night2,
  Remember,
  Rewind,
  Save,
} from "./Scenes";
import { Captions, type CaptionCue } from "../stories/Story";
import { SCENES, VO } from "./timeline";

// Caption chunks (absolute frames), timed to the voiceover's word timings.
// The whisper is shown as the golden words drifting into the phone instead.
export const CAPTIONS: CaptionCue[] = [
  { from: 6, to: 64, text: "2:13 a.m." },
  { from: 70, to: 152, text: "The best idea I've ever had." },
  { from: 170, to: 230, text: "I'll remember it in the morning." },
  { from: 272, to: 345, text: "I did not remember it in the morning." },
  { from: 352, to: 414, text: "Okay. Let's try that again." },
  { from: 726, to: 820, text: "Tap. Say it. Back to sleep.", top: true },
  {
    from: 832,
    to: 904,
    text: "And in the morning, it's all there.",
    top: true,
  },
  { from: 906, to: 964, text: "The idea, the next step...", top: true },
  { from: 965, to: 1000, text: "...even a reminder.", top: true },
];


const SFX: { file: string; at: number; volume?: number; dur?: number }[] = [
  { file: "bedtime/ding.wav", at: 86, volume: 0.6 },
  { file: "bedtime/alarm.wav", at: 258, volume: 0.45 },
  { file: "sfx/pop.wav", at: 290, volume: 0.7 },
  { file: "bedtime/rewind.wav", at: 345, volume: 0.55, dur: 75 },
  { file: "sfx/tap.wav", at: 455, volume: 0.7 },
  { file: "sfx/whoosh.wav", at: 470, volume: 0.5 },
  { file: "sfx/tap.wav", at: 730, volume: 0.7 },
  { file: "sfx/knock.wav", at: 760, volume: 1 }, // the app's knock-knock
  { file: "sfx/shimmer.wav", at: 908, volume: 0.5 },
  { file: "sfx/pop.wav", at: 933, volume: 0.5 },
  { file: "sfx/knock.wav", at: 952, volume: 1 },
  { file: "bedtime/ding.wav", at: 1088, volume: 0.5 },
];

const inVo = (f: number) =>
  VO.some((v) => f >= v.at - 4 && f <= v.at + v.frames + 4);

export const BedtimeVideo: React.FC = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    <Sequence
      name="1 2:13 a.m."
      from={SCENES.night1.from}
      durationInFrames={SCENES.night1.duration}
    >
      <Night1 />
    </Sequence>
    <Sequence
      name="2 I'll remember it"
      from={SCENES.remember.from}
      durationInFrames={SCENES.remember.duration}
    >
      <Remember />
    </Sequence>
    <Sequence
      name="3 Forgot"
      from={SCENES.morning1.from}
      durationInFrames={SCENES.morning1.duration}
    >
      <Morning1 />
    </Sequence>
    <Sequence
      name="4 Rewind"
      from={SCENES.rewind.from}
      durationInFrames={SCENES.rewind.duration}
    >
      <Rewind />
    </Sequence>
    <Sequence
      name="5 Take two"
      from={SCENES.night2.from}
      durationInFrames={SCENES.night2.duration}
    >
      <Night2 />
    </Sequence>
    <Sequence
      name="6 Save, sleep"
      from={SCENES.save.from}
      durationInFrames={SCENES.save.duration}
    >
      <Save />
    </Sequence>
    <Sequence
      name="7 It's all there"
      from={SCENES.morning2.from}
      durationInFrames={SCENES.morning2.duration}
    >
      <Morning2 />
    </Sequence>
    <Sequence
      name="8 Three months later"
      from={SCENES.later.from}
      durationInFrames={SCENES.later.duration}
    >
      <Later />
    </Sequence>
    <Sequence
      name="9 End"
      from={SCENES.end.from}
      durationInFrames={SCENES.end.duration}
    >
      <End />
    </Sequence>

    <Captions cues={CAPTIONS} />

    <Audio
      src={staticFile("bedtime/music.wav")}
      volume={(f) => (inVo(f) ? 0.32 : 0.6)}
    />
    {[
      [SCENES.night1.from, SCENES.remember.from + SCENES.remember.duration],
      [SCENES.night2.from, SCENES.save.from + SCENES.save.duration],
    ].map(([a, b]) => (
      <Sequence
        key={a}
        name="Crickets"
        from={a}
        durationInFrames={b - a}
        layout="none"
      >
        <Audio src={staticFile("bedtime/crickets.wav")} loop volume={0.25} />
      </Sequence>
    ))}
    {[SCENES.morning1, SCENES.morning2].map((s) => (
      <Sequence
        key={s.from}
        name="Birds"
        from={s.from + 4}
        durationInFrames={s.duration - 4}
        layout="none"
      >
        <Audio src={staticFile("bedtime/birds.wav")} loop volume={0.22} />
      </Sequence>
    ))}
    {VO.map((v) => (
      <Sequence
        key={v.file}
        name={`VO ${v.file}`}
        from={v.at}
        durationInFrames={v.frames + 8}
        layout="none"
      >
        <Audio src={staticFile(`voiceover/bedtime/${v.file}.mp3`)} />
      </Sequence>
    ))}
    {SFX.map((s, i) => (
      <Sequence
        key={i}
        name={`SFX ${s.file}`}
        from={s.at}
        durationInFrames={s.dur ?? 60}
        layout="none"
      >
        <Audio src={staticFile(s.file)} volume={s.volume ?? 0.6} />
      </Sequence>
    ))}
  </AbsoluteFill>
);
