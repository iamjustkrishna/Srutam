// "The 2 a.m. Idea", native 16:9: the vertical's scenes restaged for 1920 x 1080
// (./Scenes.tsx), on exactly the vertical's timeline so its finished audio track
// muxes under this picture (no Audio here).
import { Sequence } from "remotion";
import { CAPTIONS } from "../../bedtime/BedtimeVideo";
import { SCENES, TOTAL as T } from "../../bedtime/timeline";
import { Landscape, LCaptions } from "../kit";
import { End, Later, Morning1, Morning2, Night1, Night2, Remember, Rewind, Save } from "./Scenes";

/** Length in timeline frames (30 fps); rendered at 30 fps. */
export const TOTAL = T;
export const RENDER_K = 1;

const SEQ: [string, keyof typeof SCENES, React.FC][] = [
  ["1 2:13 a.m.", "night1", Night1],
  ["2 I'll remember it", "remember", Remember],
  ["3 Forgot", "morning1", Morning1],
  ["4 Rewind", "rewind", Rewind],
  ["5 Take two", "night2", Night2],
  ["6 Save, sleep", "save", Save],
  ["7 It's all there", "morning2", Morning2],
  ["8 Three months later", "later", Later],
  ["9 End", "end", End],
];

/** Top captions over the phone scenes sit over the left (Meera's) side, clear of the phone. */
const lane = (f: number): [number, number] | undefined =>
  f >= SCENES.save.from && f < SCENES.later.from ? [60, 820] : undefined;

export const BedtimeLandscape: React.FC = () => (
  <Landscape>
    {SEQ.map(([name, key, Scene]) => (
      <Sequence key={key} name={name} from={SCENES[key].from} durationInFrames={SCENES[key].duration}>
        <Scene />
      </Sequence>
    ))}
    <LCaptions cues={CAPTIONS} lane={lane} />
  </Landscape>
);
