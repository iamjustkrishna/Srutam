// Shared building blocks for the story videos (Knock Knock, Nani's Laddoos):
// captions, voice / SFX / music tracks with ducking, pop-ins and lip-sync.
import { Audio } from "@remotion/media";
import { AbsoluteFill, interpolate, Sequence, staticFile, useCurrentFrame } from "remotion";
import { clamp, easeOut, springy } from "../anim";
import { FONT } from "../theme";

export type Line = { file: string; at: number; frames: number };
export type Cue = { file: string; at: number; volume?: number; dur?: number; loop?: boolean };
export type CaptionCue = { from: number; to: number; text: string; top?: boolean };

/** 0 -> 1 with a little overshoot, starting at `at`. */
export const pop = (f: number, at: number, len = 12) =>
  interpolate(f, [at, at + len], [0, 1], { ...clamp, easing: springy });

export const fade = (f: number, at: number, len = 10) =>
  interpolate(f, [at, at + len], [0, 1], { ...clamp, easing: easeOut });

/** Scale a group about (cx, cy). */
export const about = (cx: number, cy: number, s: number) =>
  `translate(${cx} ${cy}) scale(${s}) translate(${-cx} ${-cy})`;

/** Blink for 4 frames roughly every 2.7 s. */
export const blinking = (f: number, seed = 0) => (f + seed) % 82 > 77;

/** Mouth openness for a voice line at frame f (0 when it isn't playing). */
export const talkAt = (sync: Record<string, number[]>, file: string, at: number, f: number) =>
  sync[file]?.[Math.round(f - at)] ?? 0;

/** A decaying shake after each impact frame. */
export const shake = (f: number, hits: number[], amp = 10) =>
  hits.reduce((acc, h) => {
    const d = f - h;
    return d >= 0 && d < 10 ? acc + Math.sin(d * 2.6) * amp * (1 - d / 10) : acc;
  }, 0);

/** A scene that fades up from its first frame. */
export const Scene: React.FC<{ children: React.ReactNode; fadeIn?: number }> = ({ children, fadeIn = 8 }) => {
  const f = useCurrentFrame();
  return <AbsoluteFill style={{ opacity: fadeIn ? fade(f, 0, fadeIn) : 1 }}>{children}</AbsoluteFill>;
};

export const Captions: React.FC<{ cues: CaptionCue[]; k?: number }> = ({ cues, k: rate = 1 }) => {
  const f = useCurrentFrame() / rate;
  const c = cues.find((x) => f >= x.from - 2 && f <= x.to + 4);
  if (!c) return null;
  const k = interpolate(f, [c.from - 2, c.from + 5, c.to, c.to + 4], [0, 1, 1, 0], clamp);
  return (
    <div
      style={{
        position: "absolute",
        left: 70,
        right: 70,
        top: c.top ? 130 : undefined,
        bottom: c.top ? undefined : 200,
        display: "flex",
        justifyContent: "center",
        opacity: k,
        translate: `0 ${interpolate(k, [0, 1], [18, 0], { ...clamp, easing: easeOut })}px`,
      }}
    >
      <div
        style={{
          padding: "18px 34px",
          borderRadius: 30,
          background: "rgba(26,20,51,0.82)",
          color: "#FFFFFF",
          fontFamily: FONT.sans,
          fontWeight: 800,
          fontSize: 52,
          textAlign: "center",
          lineHeight: 1.2,
        }}
      >
        {c.text}
      </div>
    </div>
  );
};

/**
 * Voice lines, sound effects and a music bed that ducks under the voices.
 * `k` is render frames per timeline frame (2 for 60 fps renders of 30 fps
 * timelines); `sfxGain` scales every effect; `duck` is the music level under voice.
 */
export const Soundtrack: React.FC<{
  music: string;
  musicVolume?: number;
  lines: (Line & { src: string })[];
  sfx: Cue[];
  k?: number;
  sfxGain?: number;
  duck?: number;
}> = ({ music, musicVolume = 0.6, lines, sfx, k = 1, sfxGain = 1, duck = 0.5 }) => {
  // Music eases down under each line and back up after it (no hard steps).
  const level = (t: number) => {
    let d = 0;
    for (const l of lines) {
      const a = interpolate(t, [l.at - 8, l.at - 2, l.at + l.frames + 2, l.at + l.frames + 12], [0, 1, 1, 0], clamp);
      d = Math.max(d, a);
    }
    return musicVolume * (1 - d * (1 - duck));
  };
  return (
    <>
      <Audio src={staticFile(music)} volume={(f) => level(f / k)} />
      {lines.map((l) => (
        <Sequence key={l.file} name={`VO ${l.file}`} from={Math.round(l.at * k)} durationInFrames={Math.round((l.frames + 8) * k)} layout="none">
          <Audio src={staticFile(l.src)} />
        </Sequence>
      ))}
      {sfx.map((s, i) => (
        <Sequence key={i} name={`SFX ${s.file}`} from={Math.round(s.at * k)} durationInFrames={Math.round((s.dur ?? 60) * k)} layout="none">
          <Audio src={staticFile(s.file)} volume={(s.volume ?? 0.6) * sfxGain} loop={s.loop} />
        </Sequence>
      ))}
    </>
  );
};
