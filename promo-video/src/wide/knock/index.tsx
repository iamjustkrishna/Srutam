// "Knock Knock", native 16:9 with English voices: the vertical's scenes restaged
// for 1920 x 1080 (./Scenes.tsx) on the landscape timeline (./timeline.ts), with
// its own soundtrack (same music bed and effects, Meera's and Ma's English lines).
import { Sequence } from "remotion";
import { Scene, Soundtrack } from "../../stories/Story";
import { Landscape, LCaptions } from "../kit";
import { Hook, Knock, Life, Once, Payoff, PromiseScene } from "./Scenes";
import { CAPTIONS, SCENES, SFX, TOTAL as T, VO } from "./timeline";

/** Length in timeline frames (30 fps); rendered at 30 fps. */
export const TOTAL = T;
export const RENDER_K = 1;

const SEQ: [string, keyof typeof SCENES, React.FC, number][] = [
  ["1 Hook", "hook", Hook, 0],
  ["2 Promise", "promise", PromiseScene, 6],
  ["3 Life happens", "life", Life, 6],
  ["4 Say it once", "once", Once, 6],
  ["5 Knock", "knock", Knock, 6],
  ["6 Payoff", "payoff", Payoff, 6],
];

/** Captions sit on Meera's side: right of the split, then left of the phone. */
const lane = (f: number): [number, number] | undefined =>
  f >= SCENES.once.from ? [80, 820] : f >= SCENES.life.from ? [960, 70] : undefined;

export const KnockLandscape: React.FC = () => (
  <Landscape>
    {SEQ.map(([name, key, Comp, fadeIn]) => (
      <Sequence key={key} name={name} from={SCENES[key].from} durationInFrames={SCENES[key].duration}>
        <Scene fadeIn={fadeIn}>
          <Comp />
        </Scene>
      </Sequence>
    ))}
    <LCaptions cues={CAPTIONS} lane={lane} />
    <Soundtrack music="knock/music.wav" lines={VO} sfx={SFX} />
  </Landscape>
);
