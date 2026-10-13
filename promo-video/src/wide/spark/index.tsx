// "Spark", restaged natively for 16:9 (1920 x 1080). Same scenes, art, copy and
// timing as the vertical cut (src/SparkStory.tsx), so its finished audio track
// lines up under this picture; every shot is re-laid-out for the wide frame.
import { interpolate, Sequence } from "remotion";
import { clamp, easeInOut } from "../../anim";
import { SPARK_CAPTIONS } from "../../SparkStory";
import { SCENES, TOTAL as T } from "../../timeline";
import { Landscape, LCaptions } from "../kit";
import { Catch } from "./Catch";
import { Connect } from "./Connect";
import { End } from "./End";
import { Hook } from "./Hook";
import { Insights } from "./Insights";
import { Lost } from "./Lost";

/** Length in timeline frames (30 fps); rendered at 30 fps. */
export const TOTAL = T;
export const RENDER_K = 1;

/**
 * Caption lanes: centred over the Lost panels, in the right-hand column while the
 * phone holds the left (Catch, Insights), and sliding left under the Mesh just
 * before the AI phone rises on the right (Connect).
 */
const lane = (f: number): [number, number] => {
  // "Tap, tap" plays during the punch-in on the dock: keep it clear of the phone
  if (f >= SCENES.catch.from && f < SCENES.catch.from + 82) return [1280, 60];
  if (f >= SCENES.catch.from && f < SCENES.insights.from) return [1110, 90];
  if (f >= SCENES.insights.from && f < SCENES.connect.from) return [1060, 140];
  if (f >= SCENES.connect.from) {
    const t = interpolate(f - SCENES.connect.from, [50, 64], [0, 1], {
      ...clamp,
      easing: easeInOut,
    });
    return [200 - 400 * t, 200 + 400 * t];
  }
  return [200, 200];
};

const SHOTS = [
  ["1 Hook", SCENES.hook, Hook],
  ["2 Lost", SCENES.lost, Lost],
  ["3 Catch", SCENES.catch, Catch],
  ["4 Insights", SCENES.insights, Insights],
  ["5 Connect", SCENES.connect, Connect],
  ["6 End", SCENES.end, End],
] as const;

export const SparkLandscape: React.FC = () => (
  <Landscape>
    {SHOTS.map(([name, s, Scene]) => (
      <Sequence key={name} name={name} from={s.from} durationInFrames={s.duration}>
        <Scene />
      </Sequence>
    ))}
    <LCaptions cues={SPARK_CAPTIONS} lane={lane} />
  </Landscape>
);
