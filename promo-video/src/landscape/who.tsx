// "Who Said That?" in 16:9: medium close-ups at full width, the board tilting down
// to the team, Arjun and Neha's side-eye as two panels, the calendar beside
// "One week later", Ask Srutam and Note Details up close, the band on the right.
import { K } from "../stories/time";
import { BEAT, CAPTIONS, SCENES, TOTAL } from "../who/timeline";
import { WhoVideo } from "../who/WhoVideo";
import { bandEnd } from "./ends";
import { chip, Wide, type Shot } from "./Wide";

const SHOTS: Shot[] = [
  // Cold open on Rao's outburst, then the freeze.
  { at: 0, crop: [540, 960, 1080], to: [540, 980, 1000] },
  { at: SCENES.freeze.from, crop: [540, 980, 1000] },
  // Tuesday, 11:00 am: Meera taps Srutam's dock under the table.
  { at: SCENES.dock.from, crop: [600, 860, 1080], over: chip("Tuesday, 11:00 am", [144, 152, 192, 200]) },
  // Rao at the board as he asks the room.
  { at: SCENES.board.from, crop: [540, 720, 1080], to: [540, 760, 1040] },
  { at: SCENES.arjun.from, crop: [540, 1050, 1080] },
  { at: SCENES.final.from, crop: [540, 1010, 1080] },
  // "Arjun, pricing deck by Thursday": Rao and his point.
  { at: SCENES.deck.from, crop: [540, 900, 1080], to: [540, 940, 1000] },
  { at: SCENES.note.from, crop: [540, 1130, 1080] },
  // Arjun and Neha look at each other: side by side.
  { at: SCENES.eyes.from, split: [[600, 320, 560], [670, 960, 560]] },
  { at: SCENES.table.from, crop: [600, 900, 1080] },
  // Insights: ride down the cards as they land.
  { at: SCENES.cards.from, crop: [500, 560, 1000], to: [500, 860, 1000] },
  // The calendar beside "One week later".
  { at: SCENES.week.from, split: [[620, 720, 960], [540, 1600, 760]] },
  { at: SCENES.blame.from, crop: [540, 820, 1080] },
  { at: SCENES.raise.from, crop: [540, 950, 1080] },
  // Typing, then the question and Srutam's answer.
  { at: SCENES.type.from, crop: [540, 1380, 1080] },
  { at: BEAT.send, blend: 12, crop: [540, 1290, 1080] },
  { at: SCENES.detail.from, crop: [560, 670, 1080] },
  { at: SCENES.play.from, crop: [540, 1130, 1080] },
  { at: SCENES.payoff.from, crop: [540, 1150, 1080] },
  bandEnd(BEAT.band + 18, [540, 1000, 960]),
];

export const WhoWide: React.FC = () => <Wide V={WhoVideo} shots={SHOTS} cues={CAPTIONS} k={K} total={TOTAL} />;
