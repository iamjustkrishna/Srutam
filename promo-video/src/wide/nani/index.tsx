// "Nani's Laddoos", native 16:9 with English voices: the vertical's scenes
// restaged for 1920 x 1080 (./Scenes.tsx) on the landscape timeline (./timeline.ts),
// with its own soundtrack (same music bed and effects, Nani's English lines).
import { Sequence } from "remotion";
import { Scene, Soundtrack } from "../../stories/Story";
import { Landscape, LCaptions } from "../kit";
import { Ask, Call, Diwali, Kitchen, Listen, Voice } from "./Scenes";
import { CAPTIONS, SCENES, SFX, TOTAL as T, VO } from "./timeline";

/** Length in timeline frames (30 fps); rendered at 30 fps. */
export const TOTAL = T;
export const RENDER_K = 1;

const SEQ: [string, keyof typeof SCENES, React.FC, number][] = [
  ["1 Kitchen", "kitchen", Kitchen, 0],
  ["2 Let it listen", "listen", Listen, 6],
  ["3 Diwali", "diwali", Diwali, 8],
  ["4 Ask", "ask", Ask, 6],
  ["5 Her voice", "voice", Voice, 8],
  ["6 Call", "call", Call, 8],
];

/** Keep captions in the left part of the frame, clear of the phone on the right, once one is on screen. */
const lane = (f: number): [number, number] | undefined =>
  f >= SCENES.call.from ? [100, 780] : f >= SCENES.voice.from ? [70, 620] : f >= SCENES.listen.from ? [80, 820] : undefined;

export const NaniLandscape: React.FC = () => (
  <Landscape>
    {SEQ.map(([name, key, Comp, fadeIn]) => (
      <Sequence key={key} name={name} from={SCENES[key].from} durationInFrames={SCENES[key].duration}>
        <Scene fadeIn={fadeIn}>
          <Comp />
        </Scene>
      </Sequence>
    ))}
    <LCaptions cues={CAPTIONS} lane={lane} />
    <Soundtrack music="nani/music.wav" lines={VO} sfx={SFX} />
  </Landscape>
);
