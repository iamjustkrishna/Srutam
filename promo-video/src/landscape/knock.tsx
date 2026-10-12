// "Knock Knock" in 16:9: the door, the promise bubble beside Meera on the phone,
// Ma waiting beside Meera busy (the vertical's diagonal split, side by side), the
// phone beside her face, the reminder's knock, the call two-up and the end card.
import { CAPTIONS, BEAT, SCENES, TOTAL } from "../knock/timeline";
import { KnockVideo } from "../knock/KnockVideo";
import { BAND } from "./ends";
import { Wide, type Shot } from "./Wide";

const SHOTS: Shot[] = [
  { at: 0, crop: [540, 880, 1080], to: [540, 900, 1000] },
  // "Haan Ma, I'll call you on Sunday. Promise!"
  { at: SCENES.promise.from, split: [[540, 560, 960], [540, 1250, 860]] },
  // Sunday, 9:40 pm: Ma waiting by the phone, Meera busy.
  { at: SCENES.life.from, split: [[540, 480, 860], [540, 1350, 860]] },
  // "Now, I just say it once": her face beside the phone, then her words above it.
  { at: SCENES.once.from, split: [[300, 1380, 600], [760, 1150, 700]] },
  {
    at: BEAT.sayIt - 2,
    panels: [
      { box: [0, 0, 760, 1080], win: [300, 1380, 600] },
      { box: [768, 0, 1152, 330], win: [330, 315, 720] },
      { box: [768, 338, 1152, 742], win: [760, 1060, 700] },
    ],
  },
  // The bubble is gone: the note lands in Insights.
  { at: BEAT.card, split: [[300, 1380, 600], [760, 1150, 700]] },
  // The reminder knocks: the phone with its notification beside her face.
  { at: SCENES.knock.from, split: [[540, 640, 1000], [320, 1400, 700]] },
  // "Arre! You remembered!": the call two-up.
  { at: SCENES.payoff.from, split: [[280, 900, 600], [830, 620, 560]] },
  // End card: the call on the left, the lettering on the right.
  {
    at: BEAT.endCard + 20,
    bg: BAND,
    panels: [
      { box: [0, 0, 1000, 1080], win: [450, 380, 704] },
      { box: [1020, 120, 860, 840], win: [540, 1270, 760] },
    ],
  },
];

export const KnockWide: React.FC = () => <Wide V={KnockVideo} shots={SHOTS} cues={CAPTIONS} total={TOTAL} />;
