// Render review stills of the animatic: node scripts/stills.mjs [frame ...]
import { bundle } from "@remotion/bundler";
import { renderStill, selectComposition } from "@remotion/renderer";
import { mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const frames = process.argv.slice(2).map(Number);
const serveUrl = await bundle({ entryPoint: join(root, "src/index.ts") });
const browserExecutable = process.env.REMOTION_BROWSER_EXECUTABLE ?? null;
const composition = await selectComposition({ serveUrl, id: "SparkStory30", browserExecutable });
mkdirSync(join(root, "out/stills"), { recursive: true });
for (const frame of frames.length ? frames : [0, 60, 150, 240, 300, 420, 540, 700, 780, 860]) {
  const output = join(root, `out/stills/f${String(frame).padStart(3, "0")}.png`);
  await renderStill({ serveUrl, composition, frame, output, scale: 0.5, browserExecutable });
  console.log(output);
}
