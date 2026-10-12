// "Nani's Laddoos" in 16:9: tilt down from the jars to Nani, the moonlit window
// beside Meera far from home, Ask Srutam up close, and the video call as a real
// two-up call before the brand.
import { CAPTIONS, SCENES, TOTAL } from "../nani/timeline";
import { NaniVideo } from "../nani/NaniVideo";
import { bandEnd } from "./ends";
import { Wide, type Shot } from "./Wide";

const SHOTS: Shot[] = [
  // The shelf of gur, besan and ghee, then down to Nani at the stove.
  { at: 0, crop: [540, 560, 1080], to: [540, 1180, 1080] },
  { at: 100, crop: [540, 1180, 1080] },
  // The notification shade: Srutam, ready to record.
  { at: SCENES.listen.from, crop: [560, 760, 1080] },
  // Diwali: the window, moon and diyas beside Meera alone.
  { at: SCENES.diwali.from, split: [[600, 450, 800], [540, 1350, 860]] },
  // The question and Nani's answer, riding down to the cited note.
  { at: SCENES.ask.from, crop: [540, 680, 1080], to: [540, 900, 1080] },
  // Her voice on the note, Meera beside it.
  { at: SCENES.voice.from, crop: [540, 1220, 1080] },
  // The video call, two-up: Meera with her laddoos, Nani in the phone.
  { at: SCENES.call.from, split: [[270, 1080, 680], [740, 600, 640]] },
  bandEnd(1132, [600, 600, 1000]),
];

export const NaniWide: React.FC = () => <Wide V={NaniVideo} shots={SHOTS} cues={CAPTIONS} total={TOTAL} />;
