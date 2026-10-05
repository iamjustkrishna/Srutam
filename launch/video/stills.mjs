import {bundle} from '@remotion/bundler';
import {renderStill, selectComposition} from '@remotion/renderer';
import path from 'node:path';
const [comp, outdir, ...times] = process.argv.slice(2);
const serveUrl = await bundle({entryPoint: path.resolve('src/index.ts')});
import fs from 'node:fs';
const SANDBOX = '/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell';
const browserExecutable = fs.existsSync(SANDBOX) ? SANDBOX : undefined;
const composition = await selectComposition({serveUrl, id: comp, browserExecutable});
console.log('duration s', composition.durationInFrames / 30);
for (const t of times) {
  const frame = t.startsWith('f') ? +t.slice(1) : Math.round(+t * 30);
  await renderStill({serveUrl, composition, frame: Math.min(frame, composition.durationInFrames - 1), output: `${outdir}/${comp}_${String(frame).padStart(5,'0')}.jpeg`, imageFormat: 'jpeg', jpegQuality: 80, browserExecutable});
}
