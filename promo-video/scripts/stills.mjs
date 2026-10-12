// Render review stills: node scripts/stills.mjs [--id=Composition] [frame ...]
import { bundle } from "@remotion/bundler";
import { renderStill, selectComposition } from "@remotion/renderer";
import { mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const idArg = process.argv.find((a) => a.startsWith("--id="));
const id = idArg ? idArg.slice(5) : "SparkStory30";
const frames = process.argv.slice(2).filter((a) => !a.startsWith("--")).map(Number);
const serveUrl = await bundle({ entryPoint: join(root, "src/index.ts") });
const browserExecutable = process.env.REMOTION_BROWSER_EXECUTABLE ?? null;
const composition = await selectComposition({ serveUrl, id, browserExecutable });
mkdirSync(join(root, "out/stills"), { recursive: true });
for (const frame of frames.length ? frames : [0, 60, 150, 240, 300, 420, 540, 700, 780, 860]) {
  const output = join(root, `out/stills/${id}-${String(frame).padStart(4, "0")}.png`);
  await renderStill({ serveUrl, composition, frame, output, scale: 0.5, browserExecutable });
  console.log(output);
}
