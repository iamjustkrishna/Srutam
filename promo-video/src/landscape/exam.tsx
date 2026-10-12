// "2 a.m. Before the Exam" in 16:9: her face over the 2:07 clock, the blackboard
// tilting down to the sleeping class, Ask Srutam and Note Details up close, the
// door beside Meera, the all-nighter's clock and faces beside the Insights, and in
// the hall her face beside the question paper.
import { BEAT, CAPTIONS, SCENES, TOTAL } from "../exam/timeline";
import { ExamVideo } from "../exam/ExamVideo";
import { K } from "../stories/time";
import { bandEnd } from "./ends";
import { Wide, type Shot } from "./Wide";

const SHOTS: Shot[] = [
  // 2:07 am: ride the pull-out from the clock, then her face, the clock, the moon.
  { at: 0, crop: [540, 960, 1080], to: [540, 1090, 1080] },
  {
    at: 80,
    cap: [1130, 30],
    panels: [
      { box: [0, 0, 1100, 1080], win: [420, 1000, 660], to: [430, 990, 620] },
      { box: [1108, 0, 812, 536], win: [445, 1355, 420] },
      { box: [1108, 544, 812, 536], win: [200, 340, 500] },
    ],
  },
  // The group chat, as the messages land.
  { at: SCENES.chat.from, crop: [540, 1350, 1080] },
  { at: SCENES.guilt.from, crop: [540, 930, 1080] },
  // Monday 9:00: Start on Srutam's notification, the lecture on the calendar below.
  { at: SCENES.start.from, crop: [600, 600, 1080] },
  // The blackboard, tilting down to the sleeping class.
  { at: SCENES.lecture.from, crop: [540, 400, 1080], to: [540, 1150, 1080] },
  { at: SCENES.insert.from, crop: [540, 640, 1080] },
  // Typing the question, then the answer and its cited note.
  { at: SCENES.type.from, crop: [540, 1380, 1080] },
  { at: BEAT.send, blend: 12, crop: [540, 1080, 1080] },
  { at: SCENES.detail.from, crop: [560, 740, 1080] },
  { at: SCENES.listen.from, crop: [540, 1050, 1080] },
  // Sharing it with the group, riding down to the replies.
  { at: SCENES.share.from, crop: [540, 1100, 1080], to: [540, 1250, 1080] },
  // A knock: Meera beside the door as Kabir and Riya come in.
  { at: SCENES.door.from, split: [[220, 1400, 560], [700, 900, 760]] },
  // The all-nighter: the clock and three faces beside the Insights.
  { at: SCENES.lapse.from, split: [[540, 560, 960], [830, 1500, 560]] },
  { at: SCENES.remind.from, crop: [540, 620, 1080] },
  // The exam: her face beside the question paper.
  { at: SCENES.hall.from, split: [[540, 800, 760], [540, 1580, 760]] },
  { at: SCENES.end.from, crop: [540, 1050, 1080] },
  bandEnd(BEAT.band + 18, [540, 1000, 960]),
];

export const ExamWide: React.FC = () => <Wide V={ExamVideo} shots={SHOTS} cues={CAPTIONS} k={K} total={TOTAL} />;
