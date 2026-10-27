// Export a story video as an editable project: cue sheet + separate audio stems + shot list.
//
//   node scripts/render/export-edit.mjs court            # -> out/court-edit/
//
// The timeline (src/<name>/timeline.ts) is the single source of truth: SHOTS, VO, SFX, CAPTIONS.
// Writes cues.csv (every voice/SFX/caption with start, length, file), shots.csv, and the stems
// voices.wav / sfx.wav / music.wav, all full length on the same clock, so a DAW or NLE can
// line them up under the per-shot clips from render-shots.sh. Stems are un-ducked, un-normalised.
import { build } from "esbuild";
import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync, existsSync } from "node:fs";
import { pathToFileURL } from "node:url";

const name = process.argv[2] ?? "court";
const out = `out/${name}-edit`;
mkdirSync(out, { recursive: true });
const bundle = `${out}/.timeline.mjs`;
await build({ entryPoints: [`src/${name}/timeline.ts`], bundle: true, format: "esm", outfile: bundle, logLevel: "error" });
const T = await import(pathToFileURL(`${process.cwd()}/${bundle}`).href);

const fps = 30, sec = (f) => (f / fps).toFixed(3);
const rows = [["track", "start_s", "length_s", "file_or_text", "volume"]];
for (const v of T.VO) rows.push(["voice", sec(v.at), sec(v.frames), v.src, 1]);
for (const c of T.SFX) rows.push(["sfx", sec(c.at), c.dur ? sec(c.dur) : "", `public/${c.file}`, c.volume ?? 1]);
for (const c of T.CAPTIONS) rows.push(["caption", sec(c.from), sec(c.to - c.from), JSON.stringify(c.text), ""]);
writeFileSync(`${out}/cues.csv`, rows.map((r) => r.join(",")).join("\n") + "\n");
writeFileSync(`${out}/shots.csv`, ["shot,from_s,length_s,from_frame,frames", ...Object.entries(T.SHOTS).map(([k, s]) => `${k},${sec(s.from)},${sec(s.duration)},${s.from},${s.duration}`)].join("\n") + "\n");

const total = T.TOTAL / fps;
const stem = (file, items) => {
  if (!items.length) return;
  const args = ["-loglevel", "error", "-y"];
  items.forEach((i) => args.push("-i", i.path));
  const f = items.map((i, n) => `[${n}:a]${i.dur ? `atrim=0:${i.dur},` : ""}volume=${i.vol},adelay=${Math.round(i.at * 1000)}:all=1[a${n}]`);
  f.push(`${items.map((_, n) => `[a${n}]`).join("")}amix=inputs=${items.length}:normalize=0:duration=longest,apad=whole_dur=${total}[o]`);
  args.push("-filter_complex", f.join(";"), "-map", "[o]", "-t", String(total), `${out}/${file}`);
  execFileSync("ffmpeg", args);
  console.log(`${out}/${file}`);
};
stem("voices.wav", T.VO.map((v) => ({ path: `public/${v.src}`, at: v.at / fps, vol: 1 })));
stem("sfx.wav", T.SFX.map((c) => ({ path: `public/${c.file}`, at: c.at / fps, vol: c.volume ?? 1, dur: c.dur ? c.dur / fps : 0 })));
if (existsSync(`public/${name}/music.wav`)) execFileSync("ffmpeg", ["-loglevel", "error", "-y", "-i", `public/${name}/music.wav`, "-t", String(total), `${out}/music.wav`]);
console.log(`${out}/cues.csv, shots.csv`);
