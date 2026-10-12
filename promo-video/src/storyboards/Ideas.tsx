// The story-idea storyboards, one sheet per idea.
import { IdeaBoard } from "./Board";
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
