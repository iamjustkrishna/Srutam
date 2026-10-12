// The story-idea storyboards, one sheet per idea.
import { IdeaBoard } from "./Board";
import { EXAM2 } from "./exam2";
import { MEETING2 } from "./meeting2";
import { ProBoard } from "./ProBoard";
import { EXAM } from "./exam";
import { KNOCK } from "./knock";
import { MEETING } from "./meeting";
import { NANI_IDEA } from "./nani";

export const IDEAS = [
  ["IdeaKnock", KNOCK],
  ["IdeaNani", NANI_IDEA],
  ["IdeaMeeting", MEETING],
  ["IdeaExam", EXAM],
] as const;

export const IDEA_BOARDS = IDEAS.map(
  ([id, idea]) => [id, () => <IdeaBoard idea={idea} />] as const,
);

// Storyboard v2: the rigged cast, ten panels and full camera / action notation.
export const PRO_BOARDS = (
  [
    ["IdeaMeetingV2", MEETING2],
    ["IdeaExamV2", EXAM2],
  ] as const
).map(([id, idea]) => [id, () => <ProBoard idea={idea} />] as const);
