import { bundle } from "@remotion/bundler";
import { renderStill, selectComposition } from "@remotion/renderer";
import { mkdirSync } from "node:fs";
const root = process.cwd();
const serveUrl = await bundle({ entryPoint: `${root}/src/index.ts` });
const browserExecutable = process.env.REMOTION_BROWSER_EXECUTABLE;
mkdirSync(`${root}/out/ideas`, { recursive: true });
const names = { IdeaKnock: "idea-1-knock-knock", IdeaNani: "idea-2-nanis-laddoos", IdeaMeeting: "idea-3-who-said-that", IdeaExam: "idea-4-before-the-exam", IdeaMeetingV2: "idea-3-who-said-that-v2", IdeaExamV2: "idea-4-before-the-exam-v2", CastSheet: "cast-sheet" };
const only = process.argv.slice(2);
for (const [id, file] of Object.entries(names)) {
  if (only.length && !only.includes(id)) continue;
  const composition = await selectComposition({ serveUrl, id, browserExecutable });
  await renderStill({ serveUrl, composition, output: `${root}/out/ideas/${file}.png`, browserExecutable });
  console.log(file);
}
